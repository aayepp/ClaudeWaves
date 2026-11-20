package com.lastwave.app.playback

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import com.lastwave.app.data.music.EnergySample
import com.lastwave.app.data.music.bestPartFromEnergyCurve
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.nio.ByteOrder
import kotlin.math.sqrt

/**
 * Lightweight on-device audio energy scanner used ONLY as a Layer-3 fallback when a song
 * has neither synced lyrics choruses nor a YouTube heatmap/chapters (e.g., EDM, Phonk,
 * instrumentals, remixes, local files).
 *
 * Samples 18 brief (~280 ms) PCM windows across 15%–82% of the track via MediaExtractor.seekTo
 * + MediaCodec, staying fast (< 600 ms on local/cached streams, hard 2.5 s timeout).
 */
internal object BestPartAudioAnalyzer {
    private const val SAMPLE_WINDOWS = 18
    private const val START_FRACTION = 0.15
    private const val END_FRACTION = 0.82
    private const val WINDOW_PCM_FRAMES_TARGET = 12_000 // ~270 ms at 44.1 kHz
    private const val ANALYSIS_TIMEOUT_MS = 2_500L
    private const val DEQUEUE_TIMEOUT_US = 8_000L

    suspend fun analyzeBestPartMs(
        context: Context,
        sourceUrl: String?,
        durationHintMs: Long?,
    ): Long? {
        if (sourceUrl.isNullOrBlank()) return null
        return withTimeoutOrNull(ANALYSIS_TIMEOUT_MS) {
            withContext(Dispatchers.IO) {
                runCatching { scanEnergyCurve(context, sourceUrl, durationHintMs) }.getOrNull()
            }
        }
    }

    private fun scanEnergyCurve(
        context: Context,
        sourceUrl: String,
        durationHintMs: Long?,
    ): Long? {
        val extractor = MediaExtractor()
        var codec: MediaCodec? = null
        try {
            when {
                sourceUrl.startsWith("content://") -> extractor.setDataSource(context, Uri.parse(sourceUrl), null)
                sourceUrl.startsWith("file://") -> extractor.setDataSource(sourceUrl.removePrefix("file://"))
                else -> extractor.setDataSource(sourceUrl)
            }

            var audioTrack = -1
            var format: MediaFormat? = null
            for (i in 0 until extractor.trackCount) {
                val f = extractor.getTrackFormat(i)
                val mime = f.getString(MediaFormat.KEY_MIME).orEmpty()
                if (mime.startsWith("audio/")) {
                    audioTrack = i
                    format = f
                    break
                }
            }
            if (audioTrack < 0 || format == null) return null
            extractor.selectTrack(audioTrack)

            val trackDurationUs = if (format.containsKey(MediaFormat.KEY_DURATION)) {
                format.getLong(MediaFormat.KEY_DURATION)
            } else {
                (durationHintMs ?: 0L) * 1000L
            }
            if (trackDurationUs < 30_000_000L) return null // Skip clips < 30 s

            val mime = format.getString(MediaFormat.KEY_MIME) ?: return null
            codec = MediaCodec.createDecoderByType(mime).apply {
                configure(format, null, null, 0)
                start()
            }

            val startUs = (trackDurationUs * START_FRACTION).toLong()
            val endUs = (trackDurationUs * END_FRACTION).toLong()
            val stepUs = ((endUs - startUs) / SAMPLE_WINDOWS).coerceAtLeast(1_000_000L)

            val samples = ArrayList<EnergySample>(SAMPLE_WINDOWS)
            val bufferInfo = MediaCodec.BufferInfo()

            for (w in 0 until SAMPLE_WINDOWS) {
                val targetUs = startUs + w * stepUs
                codec.flush()
                extractor.seekTo(targetUs, MediaExtractor.SEEK_TO_CLOSEST_SYNC)

                var sumSq = 0.0
                var count = 0
                var inputPackets = 0
                var outputLoops = 0

                while (count < WINDOW_PCM_FRAMES_TARGET && outputLoops < 24) {
                    outputLoops++
                    if (inputPackets < 10) {
                        val inIdx = codec.dequeueInputBuffer(DEQUEUE_TIMEOUT_US)
                        if (inIdx >= 0) {
                            val inBuf = codec.getInputBuffer(inIdx)
                            val read = if (inBuf != null) extractor.readSampleData(inBuf, 0) else -1
                            if (read < 0) {
                                codec.queueInputBuffer(inIdx, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                                inputPackets = 10
                            } else {
                                codec.queueInputBuffer(inIdx, 0, read, extractor.sampleTime.coerceAtLeast(0L), 0)
                                extractor.advance()
                                inputPackets++
                            }
                        }
                    }

                    val outIdx = codec.dequeueOutputBuffer(bufferInfo, DEQUEUE_TIMEOUT_US)
                    if (outIdx >= 0) {
                        val outBuf = codec.getOutputBuffer(outIdx)
                        if (outBuf != null && bufferInfo.size > 1) {
                            outBuf.position(bufferInfo.offset)
                            outBuf.limit(bufferInfo.offset + bufferInfo.size)
                            val shorts = outBuf.order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
                            val n = shorts.remaining()
                            for (i in 0 until n) {
                                val v = shorts.get(i) / 32768.0
                                sumSq += v * v
                            }
                            count += n
                        }
                        codec.releaseOutputBuffer(outIdx, false)
                        if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) break
                    }
                }

                if (count > 0) {
                    samples.add(EnergySample(startMs = targetUs / 1000L, rms = sqrt(sumSq / count)))
                }
            }

            return bestPartFromEnergyCurve(samples)
        } finally {
            runCatching { codec?.stop() }
            runCatching { codec?.release() }
            runCatching { extractor.release() }
        }
    }
}
