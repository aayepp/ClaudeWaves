@file:OptIn(androidx.media3.common.util.UnstableApi::class)

package com.lastwave.app.playback.karaoke

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.util.Log
import androidx.media3.datasource.DataSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Collections
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

/**
 * Coordinates background on-device MDX-Net ONNX vocal separation while Instant C++ STFT
 * runs synchronously in [com.lastwave.app.playback.NativeProcessingAudioSink].
 * Decodes ahead up to 35 seconds past playhead via MediaExtractor + MediaCodec, computes
 * 5120-pt STFT/iSTFT in C++20, and seamlessly hands playback over to separated stems.
 */
@Singleton
class AiKaraokeEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val modelManager: AiKaraokeModelManager,
    private val applicationScope: CoroutineScope,
) {
    data class TrackAudioSource(
        val trackKey: String,
        val uri: Uri,
        val cacheKey: String?,
        val headers: Map<String, String>,
        val dataSourceFactory: DataSource.Factory,
        val durationMs: Long,
    )

    private val _status = MutableStateFlow<KaraokeStatus>(KaraokeStatus.Off)
    val status: StateFlow<KaraokeStatus> = _status.asStateFlow()

    internal val stemBuffer = AiStemBuffer()

    private val aiDispatcher = Dispatchers.Default.limitedParallelism(1)
    private val sessionLock = Any()
    private var ortEnv: OrtEnvironment? = null
    private var ortSession: OrtSession? = null
    private var inputName: String = "input"

    private var activeEnabled = false
    private var activeSource: TrackAudioSource? = null
    private var workerJob: Job? = null
    private var idleCloseJob: Job? = null

    @Volatile
    private var playheadUs: Long = 0L

    fun onKaraokeStateChanged(enabled: Boolean, source: TrackAudioSource?, currentPositionMs: Long) {
        activeEnabled = enabled
        if (!enabled) {
            workerJob?.cancel()
            workerJob = null
            activeSource = null
            stemBuffer.clear()
            _status.value = KaraokeStatus.Off
            scheduleIdleSessionRelease()
            return
        }
        idleCloseJob?.cancel()
        idleCloseJob = null
        playheadUs = currentPositionMs.coerceAtLeast(0L) * 1_000L
        activeSource = source
        if (source == null) {
            _status.value = KaraokeStatus.InstantStft
            return
        }
        startPipeline(source, currentPositionMs)
    }

    fun onTrackChanged(source: TrackAudioSource?, startPositionMs: Long) {
        if (!activeEnabled) return
        if (source == null) {
            workerJob?.cancel()
            activeSource = null
            stemBuffer.clear()
            _status.value = KaraokeStatus.InstantStft
            return
        }
        if (activeSource?.trackKey != source.trackKey || activeSource?.uri != source.uri) {
            stemBuffer.clear()
        }
        activeSource = source
        playheadUs = startPositionMs.coerceAtLeast(0L) * 1_000L
        startPipeline(source, startPositionMs)
    }

    fun onSeek(positionMs: Long) {
        if (!activeEnabled) return
        playheadUs = positionMs.coerceAtLeast(0L) * 1_000L
        val source = activeSource ?: return
        if (!stemBuffer.hasCoverageAround(positionMs)) {
            startPipeline(source, positionMs)
        }
    }

    fun retryModelDownload(source: TrackAudioSource?, currentPositionMs: Long) {
        if (!activeEnabled) return
        val target = source ?: activeSource ?: return
        activeSource = target
        playheadUs = currentPositionMs.coerceAtLeast(0L) * 1_000L
        startPipeline(target, currentPositionMs)
    }

    fun tryReadMixedStereoFloat(
        presentationTimeUs: Long,
        frameCount: Int,
        outputSampleRate: Int,
        removalIntensity: Float,
        outInterleavedFloat: FloatArray,
        refDryStereoFloat: FloatArray?,
        refDryFrames: Int,
    ): Boolean {
        playheadUs = presentationTimeUs.coerceAtLeast(0L)
        if (!activeEnabled) return false
        val ok = stemBuffer.tryAlignAndRead(
            presentationTimeUs = presentationTimeUs,
            frameCount = frameCount,
            outputSampleRate = outputSampleRate,
            removalIntensity = removalIntensity,
            outInterleavedFloat = outInterleavedFloat,
            refDryStereoFloat = refDryStereoFloat,
            refDryFrames = refDryFrames,
        )
        if (ok && _status.value != KaraokeStatus.AiActive) {
            _status.value = KaraokeStatus.AiActive
        }
        return ok
    }

    private fun startPipeline(source: TrackAudioSource, startPosMs: Long) {
        workerJob?.cancel()
        if (!MdxStftNative.isAvailable) {
            _status.value = KaraokeStatus.InstantStft
            return
        }
        _status.value = if (modelManager.existingModelFile() != null) {
            KaraokeStatus.PreparingAi
        } else {
            KaraokeStatus.InstantStft
        }
        workerJob = applicationScope.launch(aiDispatcher) {
            runSeparationPipeline(source, startPosMs)
        }
    }

    private fun scheduleIdleSessionRelease() {
        idleCloseJob?.cancel()
        idleCloseJob = applicationScope.launch(Dispatchers.IO) {
            delay(60_000L)
            if (!activeEnabled) {
                closeOrtSession()
            }
        }
    }

    private suspend fun runSeparationPipeline(source: TrackAudioSource, startPosMs: Long) {
        val modelFile = modelManager.existingModelFile() ?: run {
            _status.value = KaraokeStatus.DownloadingModel(0, 0, 56)
            val result = modelManager.ensureModelDownloaded { pct, dlMb, totMb ->
                if (activeEnabled) {
                    _status.value = KaraokeStatus.DownloadingModel(pct, dlMb, totMb)
                }
            }
            result.getOrElse { err ->
                if (activeEnabled) {
                    _status.value = KaraokeStatus.AiError(err.message ?: "Offline")
                }
                return
            }
        }
        currentCoroutineContext().ensureActive()
        if (!activeEnabled) return

        _status.value = KaraokeStatus.PreparingAi
        val session = runCatching { ensureOrtSession(modelFile) }.getOrElse { err ->
            Log.w(TAG, "ONNX session init failed; staying on C++ STFT", err)
            if (activeEnabled) _status.value = KaraokeStatus.AiError("ONNX init failed")
            return
        }
        val env = ortEnv ?: return

        var extractor: MediaExtractor? = null
        var codec: MediaCodec? = null
        var mediaDataSource: Media3MediaDataSource? = null

        try {
            extractor = MediaExtractor()
            val scheme = source.uri.scheme?.lowercase().orEmpty()
            if (scheme == "file" || scheme == "content") {
                extractor.setDataSource(context, source.uri, null)
            } else {
                val mds = Media3MediaDataSource(
                    dataSourceFactory = source.dataSourceFactory,
                    uri = source.uri,
                    cacheKey = source.cacheKey,
                    headers = source.headers,
                )
                mediaDataSource = mds
                extractor.setDataSource(mds)
            }

            val trackIndex = findAudioTrackIndex(extractor)
            if (trackIndex < 0) {
                if (activeEnabled) _status.value = KaraokeStatus.InstantStft
                return
            }
            extractor.selectTrack(trackIndex)
            val format = extractor.getTrackFormat(trackIndex)
            val mime = format.getString(MediaFormat.KEY_MIME).orEmpty()
            var sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            var channelCount = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            var pcmEncoding = AudioFormat.ENCODING_PCM_16BIT

            val seekTargetUs = ((startPosMs - 500L).coerceAtLeast(0L)) * 1_000L
            extractor.seekTo(seekTargetUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)

            codec = MediaCodec.createDecoderByType(mime)
            codec.configure(format, null, null, 0)
            codec.start()

            val bufferInfo = MediaCodec.BufferInfo()
            var inputDone = false
            var outputDone = false

            val inputTensorBuf = ByteBuffer
                .allocateDirect(MdxStftNative.MDX_TENSOR_FLOATS * Float.SIZE_BYTES)
                .order(ByteOrder.nativeOrder())
            val outputTensorBuf = ByteBuffer
                .allocateDirect(MdxStftNative.MDX_TENSOR_FLOATS * Float.SIZE_BYTES)
                .order(ByteOrder.nativeOrder())
            val chunkInput = FloatArray(MdxStftNative.MDX_CHUNK_SIZE * 2)
            val instOut = FloatArray(MdxStftNative.MDX_GEN_SIZE * 2)
            val centerOrig = FloatArray(MdxStftNative.MDX_GEN_SIZE * 2)
            val prevTrimTail = FloatArray(MdxStftNative.MDX_TRIM * 2)

            // Accumulate 44.1kHz stereo frames: first chunk waits for GEN + TRIM frames so it has right context
            val accumCap = MdxStftNative.MDX_GEN_SIZE + MdxStftNative.MDX_TRIM + 16_384
            val accumStereo = FloatArray(accumCap * 2)
            var accumFrames = 0
            var chunkStartFrame44k = -1L
            var hasEmittedFirstChunk = false
            var resamplePhase = 0.0
            var decodedScratch = FloatArray(0)

            while (!outputDone) {
                currentCoroutineContext().ensureActive()
                if (!activeEnabled) return

                // Feed MediaCodec input
                if (!inputDone) {
                    val inIdx = codec.dequeueInputBuffer(10_000L)
                    if (inIdx >= 0) {
                        val inBuf = codec.getInputBuffer(inIdx)
                        val sampleSize = if (inBuf != null) extractor.readSampleData(inBuf, 0) else -1
                        if (sampleSize < 0) {
                            codec.queueInputBuffer(inIdx, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inputDone = true
                        } else {
                            val pts = extractor.sampleTime.coerceAtLeast(0L)
                            codec.queueInputBuffer(inIdx, 0, sampleSize, pts, 0)
                            extractor.advance()
                        }
                    }
                }

                // Drain MediaCodec output
                val outIdx = codec.dequeueOutputBuffer(bufferInfo, 10_000L)
                if (outIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    val outFmt = codec.outputFormat
                    if (outFmt.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                        sampleRate = outFmt.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                    }
                    if (outFmt.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                        channelCount = outFmt.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                    }
                    pcmEncoding = if (outFmt.containsKey(MediaFormat.KEY_PCM_ENCODING)) {
                        outFmt.getInteger(MediaFormat.KEY_PCM_ENCODING)
                    } else {
                        AudioFormat.ENCODING_PCM_16BIT
                    }
                } else if (outIdx >= 0) {
                    val outBuf = codec.getOutputBuffer(outIdx)
                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        outputDone = true
                    }
                    if (outBuf != null && bufferInfo.size > 0 && sampleRate > 0 && channelCount >= 1) {
                        outBuf.position(bufferInfo.offset)
                        outBuf.limit(bufferInfo.offset + bufferInfo.size)
                        val bytesPerSample = if (pcmEncoding == AudioFormat.ENCODING_PCM_FLOAT) 4 else 2
                        val inFrames = bufferInfo.size / (bytesPerSample * channelCount)
                        if (inFrames > 0) {
                            if (chunkStartFrame44k < 0L) {
                                chunkStartFrame44k =
                                    (bufferInfo.presentationTimeUs.coerceAtLeast(0L) * MdxStftNative.MDX_SAMPLE_RATE) / 1_000_000L
                            }
                            if (decodedScratch.size < inFrames * 2) {
                                decodedScratch = FloatArray(inFrames * 2)
                            }
                            decodeBufferToStereoFloat(outBuf, inFrames, channelCount, pcmEncoding, decodedScratch)

                            // Resample / append directly into accumStereo at 44,100 Hz
                            val step = sampleRate.toDouble() / MdxStftNative.MDX_SAMPLE_RATE.toDouble()
                            if (abs(sampleRate - MdxStftNative.MDX_SAMPLE_RATE) == 0) {
                                val spaceLeft = accumCap - accumFrames
                                val copyFrames = minOf(inFrames, spaceLeft)
                                System.arraycopy(decodedScratch, 0, accumStereo, accumFrames * 2, copyFrames * 2)
                                accumFrames += copyFrames
                            } else {
                                while (resamplePhase < inFrames && accumFrames < accumCap) {
                                    val i0 = resamplePhase.toInt().coerceIn(0, inFrames - 1)
                                    val frac = (resamplePhase - i0).toFloat()
                                    val i1 = (i0 + 1).coerceAtMost(inFrames - 1)
                                    val l = decodedScratch[i0 * 2] + (decodedScratch[i1 * 2] - decodedScratch[i0 * 2]) * frac
                                    val r = decodedScratch[i0 * 2 + 1] + (decodedScratch[i1 * 2 + 1] - decodedScratch[i0 * 2 + 1]) * frac
                                    accumStereo[accumFrames * 2] = l
                                    accumStereo[accumFrames * 2 + 1] = r
                                    accumFrames++
                                    resamplePhase += step
                                }
                                resamplePhase -= inFrames.toDouble()
                                if (resamplePhase < 0.0) resamplePhase = 0.0
                            }
                        }
                    }
                    codec.releaseOutputBuffer(outIdx, false)
                }

                // Process ready full chunk(s) whenever GEN_SIZE + right TRIM frames are accumulated
                val neededForChunk = MdxStftNative.MDX_GEN_SIZE + MdxStftNative.MDX_TRIM
                while (accumFrames >= neededForChunk || (outputDone && accumFrames > 0)) {
                    currentCoroutineContext().ensureActive()
                    if (!activeEnabled) return

                    // Throttle inference to stay ~35s ahead of playhead (saves battery & CPU)
                    val chunkStartUs = (chunkStartFrame44k * 1_000_000L) / MdxStftNative.MDX_SAMPLE_RATE
                    while (chunkStartUs > playheadUs + MAX_AHEAD_US) {
                        currentCoroutineContext().ensureActive()
                        if (!activeEnabled) return
                        delay(200L)
                    }

                    val validGenFrames = if (accumFrames >= MdxStftNative.MDX_GEN_SIZE) {
                        MdxStftNative.MDX_GEN_SIZE
                    } else {
                        accumFrames
                    }

                    // Build chunkInput = [prevTrimTail (2560)] + [center (256000)] + [rightTrim (2560)]
                    if (hasEmittedFirstChunk) {
                        System.arraycopy(prevTrimTail, 0, chunkInput, 0, MdxStftNative.MDX_TRIM * 2)
                    } else {
                        java.util.Arrays.fill(chunkInput, 0, MdxStftNative.MDX_TRIM * 2, 0f)
                    }
                    val tailCopyCount = minOf(accumFrames, MdxStftNative.MDX_GEN_SIZE + MdxStftNative.MDX_TRIM)
                    System.arraycopy(accumStereo, 0, chunkInput, MdxStftNative.MDX_TRIM * 2, tailCopyCount * 2)
                    if (tailCopyCount < MdxStftNative.MDX_GEN_SIZE + MdxStftNative.MDX_TRIM) {
                        java.util.Arrays.fill(
                            chunkInput,
                            (MdxStftNative.MDX_TRIM + tailCopyCount) * 2,
                            MdxStftNative.MDX_CHUNK_SIZE * 2,
                            0f,
                        )
                    }

                    val stftOk = MdxStftNative.nativeComputeStft(chunkInput, inputTensorBuf)
                    if (stftOk) {
                        inputTensorBuf.rewind()
                        outputTensorBuf.clear()
                        runMdxChunk(env, session, inputTensorBuf, outputTensorBuf)
                        outputTensorBuf.rewind()
                        val istftOk = MdxStftNative.nativeComputeIstft(
                            outputTensorBuf,
                            instOut,
                            MdxStftNative.MDX_COMPENSATE,
                        )
                        if (istftOk) {
                            System.arraycopy(accumStereo, 0, centerOrig, 0, validGenFrames * 2)
                            stemBuffer.putChunk(chunkStartFrame44k, centerOrig, instOut, validGenFrames)
                            hasEmittedFirstChunk = true
                        }
                    }

                    // Save last MDX_TRIM frames of consumed center as left context for next chunk
                    if (validGenFrames >= MdxStftNative.MDX_TRIM) {
                        System.arraycopy(
                            accumStereo,
                            (validGenFrames - MdxStftNative.MDX_TRIM) * 2,
                            prevTrimTail,
                            0,
                            MdxStftNative.MDX_TRIM * 2,
                        )
                    }
                    val remaining = accumFrames - validGenFrames
                    if (remaining > 0) {
                        System.arraycopy(accumStereo, validGenFrames * 2, accumStereo, 0, remaining * 2)
                    }
                    accumFrames = remaining
                    chunkStartFrame44k += validGenFrames
                }
            }
        } catch (c: CancellationException) {
            throw c
        } catch (t: Throwable) {
            Log.w(TAG, "AI karaoke pipeline error; continuing Instant C++ STFT", t)
            if (activeEnabled && _status.value !is KaraokeStatus.AiActive) {
                _status.value = KaraokeStatus.InstantStft
            }
        } finally {
            runCatching { codec?.stop() }
            runCatching { codec?.release() }
            runCatching { extractor?.release() }
            runCatching { mediaDataSource?.close() }
        }
    }

    private fun runMdxChunk(
        env: OrtEnvironment,
        session: OrtSession,
        inputTensorBuf: ByteBuffer,
        outputTensorBuf: ByteBuffer,
    ) {
        val shape = longArrayOf(
            1L,
            4L,
            MdxStftNative.MDX_DIM_F.toLong(),
            MdxStftNative.MDX_DIM_T.toLong(),
        )
        OnnxTensor.createTensor(env, inputTensorBuf.asFloatBuffer(), shape).use { inputTensor ->
            session.run(Collections.singletonMap(inputName, inputTensor)).use { result ->
                val outValue = result[0] as? OnnxTensor ?: return
                val outFloats = outValue.floatBuffer ?: return
                outputTensorBuf.clear()
                outputTensorBuf.asFloatBuffer().put(outFloats)
            }
        }
    }

    private fun ensureOrtSession(modelFile: File): OrtSession {
        synchronized(sessionLock) {
            ortSession?.let { return it }
            val env = ortEnv ?: OrtEnvironment.getEnvironment().also { ortEnv = it }
            val session = OrtSession.SessionOptions().use { opts ->
                val cores = Runtime.getRuntime().availableProcessors().coerceAtLeast(2)
                opts.setIntraOpNumThreads(minOf(4, maxOf(2, cores / 2)))
                opts.setInterOpNumThreads(1)
                opts.setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT)
                runCatching { opts.addXnnpack(emptyMap()) }
                env.createSession(modelFile.absolutePath, opts)
            }
            inputName = session.inputNames.firstOrNull() ?: "input"
            ortSession = session
            return session
        }
    }

    private fun closeOrtSession() {
        synchronized(sessionLock) {
            runCatching { ortSession?.close() }
            ortSession = null
        }
    }

    private fun findAudioTrackIndex(extractor: MediaExtractor): Int {
        for (i in 0 until extractor.trackCount) {
            val mime = extractor.getTrackFormat(i).getString(MediaFormat.KEY_MIME)
            if (mime?.startsWith("audio/") == true) return i
        }
        return -1
    }

    private fun decodeBufferToStereoFloat(
        buffer: ByteBuffer,
        frames: Int,
        channels: Int,
        pcmEncoding: Int,
        outStereo: FloatArray,
    ) {
        val origOrder = buffer.order()
        buffer.order(ByteOrder.LITTLE_ENDIAN)
        try {
            if (pcmEncoding == AudioFormat.ENCODING_PCM_FLOAT) {
                for (f in 0 until frames) {
                    val l = buffer.getFloat()
                    val r = if (channels >= 2) buffer.getFloat() else l
                    for (extra in 2 until channels) buffer.getFloat()
                    outStereo[f * 2] = l
                    outStereo[f * 2 + 1] = r
                }
            } else {
                for (f in 0 until frames) {
                    val l = buffer.getShort() * INV_SHORT
                    val r = if (channels >= 2) buffer.getShort() * INV_SHORT else l
                    for (extra in 2 until channels) buffer.getShort()
                    outStereo[f * 2] = l
                    outStereo[f * 2 + 1] = r
                }
            }
        } finally {
            buffer.order(origOrder)
        }
    }

    companion object {
        private const val TAG = "AiKaraokeEngine"
        private const val MAX_AHEAD_US = 35_000_000L
        private const val INV_SHORT = 1f / 32768f
    }
}
