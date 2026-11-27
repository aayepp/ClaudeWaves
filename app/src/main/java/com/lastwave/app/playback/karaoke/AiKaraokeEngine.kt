@file:OptIn(androidx.media3.common.util.UnstableApi::class)

package com.lastwave.app.playback.karaoke

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.SeekParameters
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import com.google.android.gms.tasks.Task
import com.google.android.gms.tflite.client.TfLiteInitializationOptions
import com.google.android.gms.tflite.gpu.support.TfLiteGpu
import com.google.android.gms.tflite.java.TfLite
import com.lastwave.app.playback.FfmpegCompat
import com.lastwave.app.playback.accurateAudioMediaCodecSelector
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
import kotlinx.coroutines.suspendCancellableCoroutine
import org.tensorflow.lite.InterpreterApi
import org.tensorflow.lite.gpu.GpuDelegateFactory
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Coordinates background GPU LiteRT/TFLite UVR-MDX-NET-Voc_FT vocal separation while
 * Instant C++ STFT runs synchronously in [com.lastwave.app.playback.NativeProcessingAudioSink].
 * Decodes ahead via off-air ExoPlayer + [KaraokePcmCaptureSink], computes 7680-pt STFT/iSTFT
 * in C++20, and subtracts only the predicted vocal stem (`orig - intensity * vocal`).
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
        val mediaItem: MediaItem,
        val durationMs: Long,
        val mediaSourceFactoryProvider: () -> MediaSource.Factory,
    )

    private val _status = MutableStateFlow<KaraokeStatus>(KaraokeStatus.Off)
    val status: StateFlow<KaraokeStatus> = _status.asStateFlow()

    internal val stemBuffer = AiStemBuffer()

    private val aiDispatcher = Dispatchers.Default.limitedParallelism(1)
    private val sessionLock = Any()
    private var tfliteInitialized = false
    private var gpuSupported = false
    private var interpreter: InterpreterApi? = null

    private var activeEnabled = false
    private var activeSource: TrackAudioSource? = null
    private var workerJob: Job? = null
    private var idleCloseJob: Job? = null

    @Volatile
    private var playheadUs: Long = 0L

    @Volatile
    private var consecutiveAiMisses: Int = 0

    fun onKaraokeStateChanged(enabled: Boolean, source: TrackAudioSource?, currentPositionMs: Long) {
        activeEnabled = enabled
        consecutiveAiMisses = 0
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
        consecutiveAiMisses = 0
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
        consecutiveAiMisses = 0
        playheadUs = positionMs.coerceAtLeast(0L) * 1_000L
        val source = activeSource ?: return
        if (!stemBuffer.hasCoverageAround(positionMs)) {
            stemBuffer.resetAlignment()
            startPipeline(source, positionMs)
        }
    }

    fun retryModelDownload(source: TrackAudioSource?, currentPositionMs: Long) {
        if (!activeEnabled) return
        val target = source ?: activeSource ?: return
        consecutiveAiMisses = 0
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
        if (ok) {
            consecutiveAiMisses = 0
            if (_status.value != KaraokeStatus.AiActive) {
                _status.value = KaraokeStatus.AiActive
            }
        } else if (_status.value == KaraokeStatus.AiActive) {
            consecutiveAiMisses++
            if (consecutiveAiMisses >= AI_MISS_REVERT_THRESHOLD) {
                _status.value = if (workerJob?.isActive == true) {
                    KaraokeStatus.PreparingAi
                } else {
                    KaraokeStatus.InstantStft
                }
            }
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
                closeInterpreter()
            }
        }
    }

    private suspend fun runSeparationPipeline(source: TrackAudioSource, startPosMs: Long) {
        val modelFile = modelManager.existingModelFile() ?: run {
            _status.value = KaraokeStatus.DownloadingModel(0, 0, 64)
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
        val activeInterpreter = runCatching { ensureInterpreter(modelFile) }.getOrElse { err ->
            Log.w(TAG, "Play Services TFLite unavailable; continuing Instant C++ STFT", err)
            if (activeEnabled) _status.value = KaraokeStatus.InstantStft
            return
        }

        val sink = KaraokePcmCaptureSink(
            playheadUsProvider = { playheadUs },
            maxAheadUs = MAX_AHEAD_US,
        )
        var decoderThread: HandlerThread? = null
        var decoderHandler: Handler? = null
        var exoPlayer: ExoPlayer? = null

        try {
            var currentGeneration = sink.prepareForSeek(startPosMs.coerceAtLeast(0L) * 1_000L)
            val thread = HandlerThread("AiKaraokeDecoder").apply { start() }
            decoderThread = thread
            val handler = Handler(thread.looper)
            decoderHandler = handler

            val readyLatch = CountDownLatch(1)
            handler.post {
                runCatching {
                    val renderersFactory = object : DefaultRenderersFactory(context) {
                        override fun buildAudioSink(
                            context: Context,
                            enableFloatOutput: Boolean,
                            enableAudioTrackPlaybackParams: Boolean,
                        ): AudioSink = sink
                    }.apply {
                        setExtensionRendererMode(
                            if (FfmpegCompat.isUsable) DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER
                            else DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF,
                        )
                        setEnableDecoderFallback(true)
                        setMediaCodecSelector(accurateAudioMediaCodecSelector)
                    }
                    val trackSelector = DefaultTrackSelector(context).apply {
                        parameters = buildUponParameters()
                            .setTrackTypeDisabled(C.TRACK_TYPE_VIDEO, true)
                            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                            .build()
                    }
                    val loadControl = DefaultLoadControl.Builder()
                        .setBufferDurationsMs(15_000, 30_000, 1_000, 1_000)
                        .build()

                    val player = ExoPlayer.Builder(context, renderersFactory)
                        .setLooper(thread.looper)
                        .setTrackSelector(trackSelector)
                        .setMediaSourceFactory(source.mediaSourceFactoryProvider())
                        .setLoadControl(loadControl)
                        .setAudioAttributes(AudioAttributes.DEFAULT, false)
                        .setHandleAudioBecomingNoisy(false)
                        .setSeekParameters(SeekParameters.EXACT)
                        .build()
                    player.addListener(
                        object : Player.Listener {
                            override fun onPlayerError(error: PlaybackException) {
                                sink.signalError(error)
                            }

                            override fun onPlaybackStateChanged(playbackState: Int) {
                                if (playbackState == Player.STATE_ENDED) {
                                    sink.signalEnded()
                                }
                            }
                        },
                    )
                    player.volume = 1f
                    player.setMediaItem(source.mediaItem, startPosMs.coerceAtLeast(0L))
                    player.prepare()
                    player.playWhenReady = true
                    exoPlayer = player
                }.onFailure { err ->
                    sink.signalError(err)
                }
                readyLatch.countDown()
            }
            readyLatch.await(3_000L, TimeUnit.MILLISECONDS)

            var outputDone = false
            val inputTensorBuf = ByteBuffer
                .allocateDirect(MdxStftNative.MDX_TENSOR_FLOATS * Float.SIZE_BYTES)
                .order(ByteOrder.nativeOrder())
            val outputTensorBuf = ByteBuffer
                .allocateDirect(MdxStftNative.MDX_TENSOR_FLOATS * Float.SIZE_BYTES)
                .order(ByteOrder.nativeOrder())
            val chunkInput = FloatArray(MdxStftNative.MDX_CHUNK_SIZE * 2)
            val vocalOut = FloatArray(MdxStftNative.MDX_GEN_SIZE * 2)
            val centerOrig = FloatArray(MdxStftNative.MDX_GEN_SIZE * 2)
            val prevTrimTail = FloatArray(MdxStftNative.MDX_TRIM * 2)

            // Accumulate 44.1kHz stereo frames: first chunk waits for GEN + TRIM frames so it has right context
            val accumCap = MdxStftNative.MDX_GEN_SIZE + MdxStftNative.MDX_TRIM + 16_384
            val accumStereo = FloatArray(accumCap * 2)
            var accumFrames = 0
            var chunkStartFrame44k = -1L
            var hasEmittedFirstChunk = false
            var resamplePhase = 0.0

            while (!outputDone) {
                currentCoroutineContext().ensureActive()
                if (!activeEnabled) return

                sink.terminalError?.let { err ->
                    Log.w(TAG, "Off-air karaoke decoder failed; continuing Instant C++ STFT", err)
                    if (activeEnabled && _status.value !is KaraokeStatus.AiActive) {
                        _status.value = KaraokeStatus.InstantStft
                    }
                    return
                }

                val block = sink.pollBlock(50L)
                if (block == null) {
                    if (sink.eosReached && sink.isQueueEmpty()) {
                        outputDone = true
                    } else {
                        continue
                    }
                } else {
                    if (block.generation != currentGeneration) continue
                    if (block.isEos) {
                        outputDone = true
                    } else {
                        val inFrames = block.frameCount
                        val sampleRate = block.sampleRate
                        if (inFrames > 0 && sampleRate > 0) {
                            if (chunkStartFrame44k < 0L) {
                                chunkStartFrame44k =
                                    (block.songTimeUs.coerceAtLeast(0L) * MdxStftNative.MDX_SAMPLE_RATE) / 1_000_000L
                            }
                            val decodedScratch = block.stereoFloat
                            val step = sampleRate.toDouble() / MdxStftNative.MDX_SAMPLE_RATE.toDouble()
                            if (sampleRate == MdxStftNative.MDX_SAMPLE_RATE) {
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
                }

                // Process ready full chunk(s) whenever GEN_SIZE + right TRIM frames are accumulated
                val neededForChunk = MdxStftNative.MDX_GEN_SIZE + MdxStftNative.MDX_TRIM
                while (accumFrames >= neededForChunk || (outputDone && accumFrames > 0)) {
                    currentCoroutineContext().ensureActive()
                    if (!activeEnabled) return

                    // Throttle inference to stay ~35s ahead of playhead (saves battery & GPU/CPU)
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

                    // Build chunkInput = [prevTrimTail (3840)] + [center (122368)] + [rightTrim (3840)]
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
                        activeInterpreter.run(inputTensorBuf, outputTensorBuf)
                        outputTensorBuf.rewind()
                        val istftOk = MdxStftNative.nativeComputeIstft(
                            outputTensorBuf,
                            vocalOut,
                            MdxStftNative.MDX_COMPENSATE,
                        )
                        if (istftOk) {
                            System.arraycopy(accumStereo, 0, centerOrig, 0, validGenFrames * 2)
                            stemBuffer.putChunk(chunkStartFrame44k, centerOrig, vocalOut, validGenFrames)
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

                    // Catch up if device fell > 2s behind real-time playhead
                    val nextStartUs = (chunkStartFrame44k * 1_000_000L) / MdxStftNative.MDX_SAMPLE_RATE
                    val behindUs = playheadUs - nextStartUs
                    if (!outputDone && behindUs > CATCHUP_BEHIND_US) {
                        val jumpMs = ((playheadUs + CATCHUP_JUMP_LEAD_US).coerceAtLeast(0L)) / 1_000L
                        val seekGen = AtomicInteger(currentGeneration)
                        val seekLatch = CountDownLatch(1)
                        val posted = decoderHandler?.post {
                            seekGen.set(sink.prepareForSeek(jumpMs * 1_000L))
                            exoPlayer?.seekTo(jumpMs)
                            seekLatch.countDown()
                        } == true
                        if (posted) {
                            seekLatch.await(1_000L, TimeUnit.MILLISECONDS)
                        }
                        currentGeneration = seekGen.get()
                        accumFrames = 0
                        chunkStartFrame44k = -1L
                        hasEmittedFirstChunk = false
                        resamplePhase = 0.0
                        outputDone = false
                        break
                    }
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
            sink.close()
            val handler = decoderHandler
            val thread = decoderThread
            if (handler != null) {
                val releaseLatch = CountDownLatch(1)
                val posted = handler.post {
                    runCatching { exoPlayer?.stop() }
                    runCatching { exoPlayer?.release() }
                    exoPlayer = null
                    releaseLatch.countDown()
                    thread?.quitSafely()
                }
                if (posted) {
                    releaseLatch.await(1_000L, TimeUnit.MILLISECONDS)
                } else {
                    thread?.quitSafely()
                }
            } else {
                thread?.quitSafely()
            }
        }
    }

    private suspend fun ensureInterpreter(modelFile: File): InterpreterApi {
        synchronized(sessionLock) {
            interpreter?.let { return it }
        }
        if (!tfliteInitialized) {
            val gpuAvail = runCatching { TfLiteGpu.isGpuDelegateAvailable(context).awaitTask() }.getOrDefault(false)
            val initOk = if (gpuAvail) {
                runCatching {
                    val opts = TfLiteInitializationOptions.builder()
                        .setEnableGpuDelegateSupport(true)
                        .build()
                    TfLite.initialize(context, opts).awaitTask()
                    true
                }.getOrDefault(false)
            } else {
                false
            }
            if (!initOk) {
                val cpuOpts = TfLiteInitializationOptions.builder()
                    .setEnableGpuDelegateSupport(false)
                    .build()
                TfLite.initialize(context, cpuOpts).awaitTask()
            }
            tfliteInitialized = true
            gpuSupported = gpuAvail && initOk
        }

        synchronized(sessionLock) {
            interpreter?.let { return it }
            val gpuCacheDir = File(context.cacheDir, "tflite_gpu_cache").apply { mkdirs() }
            val created = if (gpuSupported) {
                runCatching {
                    val gpuDelegateOpts = GpuDelegateFactory.Options()
                        .setPrecisionLossAllowed(true)
                        .setInferencePreference(GpuDelegateFactory.Options.INFERENCE_PREFERENCE_SUSTAINED_SPEED)
                        .setSerializationParams(gpuCacheDir.absolutePath, "uvr_mdx_voc_ft_t128_v1")
                    val options = InterpreterApi.Options()
                        .setRuntime(InterpreterApi.Options.TfLiteRuntime.FROM_SYSTEM_ONLY)
                        .addDelegateFactory(GpuDelegateFactory(gpuDelegateOpts))
                    InterpreterApi.create(modelFile, options)
                }.getOrElse { gpuErr ->
                    Log.w(TAG, "GPU delegate failed; falling back to Play Services XNNPACK CPU", gpuErr)
                    createCpuInterpreter(modelFile)
                }
            } else {
                createCpuInterpreter(modelFile)
            }
            interpreter = created
            return created
        }
    }

    private fun createCpuInterpreter(modelFile: File): InterpreterApi {
        val cores = Runtime.getRuntime().availableProcessors().coerceAtLeast(2)
        val options = InterpreterApi.Options()
            .setRuntime(InterpreterApi.Options.TfLiteRuntime.FROM_SYSTEM_ONLY)
            .setUseXNNPACK(true)
            .setNumThreads(minOf(4, maxOf(2, cores / 2)))
        return InterpreterApi.create(modelFile, options)
    }

    private fun closeInterpreter() {
        synchronized(sessionLock) {
            runCatching { interpreter?.close() }
            interpreter = null
        }
    }

    private suspend fun <T> Task<T>.awaitTask(): T = suspendCancellableCoroutine { cont ->
        addOnSuccessListener { result ->
            if (cont.isActive) cont.resume(result)
        }
        addOnFailureListener { error ->
            if (cont.isActive) cont.resumeWithException(error)
        }
        addOnCanceledListener {
            if (cont.isActive) cont.cancel()
        }
    }

    companion object {
        private const val TAG = "AiKaraokeEngine"
        private const val MAX_AHEAD_US = 35_000_000L
        private const val CATCHUP_BEHIND_US = 2_000_000L
        private const val CATCHUP_JUMP_LEAD_US = 3_000_000L
        private const val AI_MISS_REVERT_THRESHOLD = 20
    }
}
