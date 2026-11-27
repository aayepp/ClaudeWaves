@file:OptIn(androidx.media3.common.util.UnstableApi::class)

package com.lastwave.app.playback.karaoke

import androidx.media3.common.AudioAttributes
import androidx.media3.common.AuxEffectInfo
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.PlaybackParameters
import androidx.media3.exoplayer.audio.AudioSink
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeUnit

/**
 * Non-realtime streaming PCM capture AudioSink for [AiKaraokeEngine].
 *
 * Accepts MediaCodec / FFmpeg decoded PCM (16/24/32-bit int or float) and reports
 * position from written samples so ExoPlayer decodes faster than real-time while
 * respecting [maxAheadUs] backpressure.
 */
internal class KaraokePcmCaptureSink(
    private val playheadUsProvider: () -> Long,
    private val maxAheadUs: Long = 35_000_000L,
) : AudioSink {

    class PcmBlock(
        val stereoFloat: FloatArray,
        val frameCount: Int,
        val sampleRate: Int,
        val songTimeUs: Long,
        val generation: Int,
        val isEos: Boolean = false,
    )

    private val queue = ArrayBlockingQueue<PcmBlock>(32)

    @Volatile
    var terminalError: Throwable? = null
        private set

    @Volatile
    var eosReached: Boolean = false
        private set

    @Volatile
    private var closed = false

    @Volatile
    private var generation = 0

    @Volatile
    private var pendingSeekBaseUs = 0L

    private var sampleRate = 0
    private var channelCount = 0
    private var pcmEncoding = C.ENCODING_INVALID
    private var bytesPerFrame = 0
    private var seekBaseUs = 0L
    private var startMediaTimeUs = C.TIME_UNSET
    private var framesWritten = 0L
    private var ended = false
    private var configured = false

    private var playbackParameters = PlaybackParameters.DEFAULT
    private var skipSilenceEnabled = false
    private var audioAttributes = AudioAttributes.DEFAULT
    private var listener: AudioSink.Listener? = null

    fun prepareForSeek(targetSongTimeUs: Long): Int {
        val nextGen = ++generation
        pendingSeekBaseUs = targetSongTimeUs.coerceAtLeast(0L)
        eosReached = false
        ended = false
        queue.clear()
        return nextGen
    }

    fun signalError(error: Throwable) {
        terminalError = error
        eosReached = true
    }

    fun signalEnded() {
        eosReached = true
        queue.offer(
            PcmBlock(
                stereoFloat = EMPTY_FLOAT,
                frameCount = 0,
                sampleRate = sampleRate.coerceAtLeast(44_100),
                songTimeUs = 0L,
                generation = generation,
                isEos = true,
            ),
        )
    }

    fun close() {
        closed = true
        eosReached = true
        queue.clear()
    }

    fun pollBlock(timeoutMs: Long): PcmBlock? =
        queue.poll(timeoutMs, TimeUnit.MILLISECONDS)

    fun isQueueEmpty(): Boolean = queue.isEmpty()

    override fun setListener(listener: AudioSink.Listener) {
        this.listener = listener
    }

    override fun supportsFormat(format: Format): Boolean = isPcm(format)

    override fun getFormatSupport(format: Format): Int =
        if (isPcm(format)) AudioSink.SINK_FORMAT_SUPPORTED_DIRECTLY
        else AudioSink.SINK_FORMAT_UNSUPPORTED

    override fun getCurrentPositionUs(sourceEnded: Boolean): Long {
        val base = startMediaTimeUs
        val rate = sampleRate
        if (base == C.TIME_UNSET || rate <= 0) return AudioSink.CURRENT_POSITION_NOT_SET
        return base + (framesWritten * 1_000_000L) / rate
    }

    override fun configure(
        format: Format,
        specifiedBufferSize: Int,
        outputChannels: IntArray?,
    ) {
        if (!isPcm(format)) {
            throw AudioSink.ConfigurationException("Karaoke capture sink requires PCM", format)
        }
        sampleRate = format.sampleRate
        channelCount = format.channelCount
        pcmEncoding = format.pcmEncoding
        bytesPerFrame = channelCount * bytesPerSample(pcmEncoding)
        seekBaseUs = pendingSeekBaseUs
        startMediaTimeUs = C.TIME_UNSET
        framesWritten = 0L
        ended = false
        configured = true
    }

    override fun play() = Unit

    override fun handleDiscontinuity() {
        startMediaTimeUs = C.TIME_UNSET
        framesWritten = 0L
    }

    override fun handleBuffer(
        buffer: ByteBuffer,
        presentationTimeUs: Long,
        encodedAccessUnitCount: Int,
    ): Boolean {
        if (!configured || closed || bytesPerFrame <= 0 || sampleRate <= 0) return false

        val blockSongTimeUs = seekBaseUs + (framesWritten * 1_000_000L) / sampleRate
        if (queue.remainingCapacity() == 0 || blockSongTimeUs > playheadUsProvider() + maxAheadUs) {
            return false
        }

        if (startMediaTimeUs == C.TIME_UNSET) {
            startMediaTimeUs = presentationTimeUs
        }

        val frameCount = buffer.remaining() / bytesPerFrame
        if (frameCount <= 0) {
            buffer.position(buffer.limit())
            return true
        }

        val stereo = FloatArray(frameCount * 2)
        decodeToStereoFloat(buffer, frameCount, channelCount, pcmEncoding, stereo)
        queue.offer(
            PcmBlock(
                stereoFloat = stereo,
                frameCount = frameCount,
                sampleRate = sampleRate,
                songTimeUs = blockSongTimeUs,
                generation = generation,
                isEos = false,
            ),
        )
        framesWritten += frameCount
        return true
    }

    override fun playToEndOfStream() {
        if (!ended) {
            ended = true
            signalEnded()
        }
    }

    override fun isEnded(): Boolean = ended

    override fun hasPendingData(): Boolean = false

    override fun setPlaybackParameters(playbackParameters: PlaybackParameters) {
        this.playbackParameters = playbackParameters
    }

    override fun getPlaybackParameters(): PlaybackParameters = playbackParameters

    override fun setSkipSilenceEnabled(skipSilenceEnabled: Boolean) {
        this.skipSilenceEnabled = skipSilenceEnabled
    }

    override fun getSkipSilenceEnabled(): Boolean = skipSilenceEnabled

    override fun setAudioAttributes(audioAttributes: AudioAttributes) {
        this.audioAttributes = audioAttributes
    }

    override fun getAudioAttributes(): AudioAttributes = audioAttributes

    override fun setAudioSessionId(audioSessionId: Int) = Unit

    override fun setAuxEffectInfo(auxEffectInfo: AuxEffectInfo) = Unit

    override fun enableTunnelingV21() = Unit

    override fun disableTunneling() = Unit

    override fun setVolume(volume: Float) = Unit

    override fun pause() = Unit

    override fun flush() {
        seekBaseUs = pendingSeekBaseUs
        startMediaTimeUs = C.TIME_UNSET
        framesWritten = 0L
        ended = false
    }

    override fun reset() {
        configured = false
        ended = false
        startMediaTimeUs = C.TIME_UNSET
        framesWritten = 0L
    }

    private fun isPcm(format: Format): Boolean =
        format.sampleRate > 0 &&
            format.channelCount > 0 &&
            bytesPerSample(format.pcmEncoding) > 0

    private fun bytesPerSample(encoding: Int): Int = when (encoding) {
        C.ENCODING_PCM_16BIT -> 2
        C.ENCODING_PCM_24BIT -> 3
        C.ENCODING_PCM_32BIT, C.ENCODING_PCM_FLOAT -> 4
        else -> 0
    }

    private fun decodeToStereoFloat(
        buffer: ByteBuffer,
        frameCount: Int,
        channels: Int,
        encoding: Int,
        outStereo: FloatArray,
    ) {
        val origOrder = buffer.order()
        buffer.order(ByteOrder.LITTLE_ENDIAN)
        var pos = buffer.position()
        val bps = bytesPerSample(encoding)
        try {
            for (f in 0 until frameCount) {
                val s0 = readSampleFloat(buffer, pos, encoding)
                if (channels == 1) {
                    outStereo[f * 2] = s0
                    outStereo[f * 2 + 1] = s0
                } else {
                    val s1 = readSampleFloat(buffer, pos + bps, encoding)
                    if (channels == 2) {
                        outStereo[f * 2] = s0
                        outStereo[f * 2 + 1] = s1
                    } else if (channels >= 6) {
                        val s2 = readSampleFloat(buffer, pos + bps * 2, encoding)
                        val s4 = readSampleFloat(buffer, pos + bps * 4, encoding)
                        val s5 = readSampleFloat(buffer, pos + bps * 5, encoding)
                        outStereo[f * 2] = (s0 + 0.707f * s2 + 0.5f * s4).coerceIn(-1f, 1f)
                        outStereo[f * 2 + 1] = (s1 + 0.707f * s2 + 0.5f * s5).coerceIn(-1f, 1f)
                    } else {
                        val s2 = readSampleFloat(buffer, pos + bps * 2, encoding)
                        outStereo[f * 2] = (s0 + 0.707f * s2).coerceIn(-1f, 1f)
                        outStereo[f * 2 + 1] = (s1 + 0.707f * s2).coerceIn(-1f, 1f)
                    }
                }
                pos += channels * bps
            }
            buffer.position(pos)
        } finally {
            buffer.order(origOrder)
        }
    }

    private fun readSampleFloat(buffer: ByteBuffer, offset: Int, encoding: Int): Float = when (encoding) {
        C.ENCODING_PCM_16BIT -> buffer.getShort(offset) * INV_16
        C.ENCODING_PCM_24BIT -> {
            val b0 = buffer.get(offset).toInt() and 0xFF
            val b1 = buffer.get(offset + 1).toInt() and 0xFF
            val b2 = buffer.get(offset + 2).toInt()
            ((b2 shl 16) or (b1 shl 8) or b0) * INV_24
        }
        C.ENCODING_PCM_32BIT -> buffer.getInt(offset) * INV_32
        C.ENCODING_PCM_FLOAT -> buffer.getFloat(offset)
        else -> 0f
    }

    companion object {
        private val EMPTY_FLOAT = FloatArray(0)
        private const val INV_16 = 1f / 32768f
        private const val INV_24 = 1f / 8388608f
        private const val INV_32 = 1f / 2147483648f
    }
}
