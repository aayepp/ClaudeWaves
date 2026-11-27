package com.lastwave.app.playback.karaoke

import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Thread-safe rolling stem cache at 44,100 Hz holding separated Instrumental + Vocal
 * stereo chunks (Int16 quantized to cut RAM in half). Supports arbitrary seek start
 * offsets and 1-shot decimated cross-correlation against ExoPlayer's dry PCM so
 * AAC/Opus encoder delay differences never cause timing drift.
 */
internal class AiStemBuffer {

    data class StemChunk(
        val startFrame44k: Long,
        val validFrames: Int,
        val instPcm: ShortArray,
        val vocalPcm: ShortArray,
    ) {
        val endFrame44k: Long get() = startFrame44k + validFrames
    }

    private val chunks = CopyOnWriteArrayList<StemChunk>()

    @Volatile
    private var ptsOffsetFrames44k: Long = 0L

    @Volatile
    private var alignmentLocked: Boolean = false

    fun isAlignmentLocked(): Boolean = alignmentLocked

    fun clear() {
        chunks.clear()
        ptsOffsetFrames44k = 0L
        alignmentLocked = false
    }

    fun hasCoverageAround(positionMs: Long): Boolean {
        val targetFrame = (positionMs.coerceAtLeast(0L) * MdxStftNative.MDX_SAMPLE_RATE) / 1_000L +
            ptsOffsetFrames44k
        return findChunk(targetFrame) != null
    }

    fun putChunk(
        startFrame44k: Long,
        origInterleaved: FloatArray,
        instInterleaved: FloatArray,
        validFrames: Int,
    ) {
        val safeFrames = validFrames.coerceIn(0, MdxStftNative.MDX_GEN_SIZE)
        if (safeFrames <= 0) return
        val samples = safeFrames * 2
        val inst = ShortArray(samples)
        val vocal = ShortArray(samples)
        for (i in 0 until samples) {
            val origSample = origInterleaved[i]
            val instSample = instInterleaved[i]
            val vocSample = origSample - instSample
            inst[i] = floatToShort(instSample)
            vocal[i] = floatToShort(vocSample)
        }
        val chunk = StemChunk(
            startFrame44k = startFrame44k.coerceAtLeast(0L),
            validFrames = safeFrames,
            instPcm = inst,
            vocalPcm = vocal,
        )
        chunks.removeAll { it.startFrame44k == chunk.startFrame44k }
        chunks.add(chunk)
        while (chunks.size > MAX_CHUNKS_IN_RAM) {
            chunks.removeAt(0)
        }
    }

    fun tryAlignAndRead(
        presentationTimeUs: Long,
        frameCount: Int,
        outputSampleRate: Int,
        removalIntensity: Float,
        outInterleavedFloat: FloatArray,
        refDryStereoFloat: FloatArray?,
        refDryFrames: Int,
    ): Boolean {
        if (frameCount <= 0 || outputSampleRate <= 0 || chunks.isEmpty()) return false

        val ptsFrame44k = (presentationTimeUs.coerceAtLeast(0L) * MdxStftNative.MDX_SAMPLE_RATE) / 1_000_000L
        if (!alignmentLocked && refDryStereoFloat != null && refDryFrames >= 256) {
            tryLockAlignment(ptsFrame44k, refDryStereoFloat, refDryFrames, outputSampleRate)
        }

        val alignedStart = ptsFrame44k + ptsOffsetFrames44k
        val ratio = MdxStftNative.MDX_SAMPLE_RATE.toDouble() / outputSampleRate.toDouble()
        val vocalKeep = (1f - removalIntensity).coerceIn(0f, 1f)

        val endOffset44k = ((frameCount - 1) * ratio).toLong() + 1L
        if (findChunk(alignedStart) == null || findChunk(alignedStart + endOffset44k) == null) {
            return false
        }

        // Drop chunks far behind playhead to keep footprint tight (~40s max behind)
        val minKeepFrame = alignedStart - MdxStftNative.MDX_GEN_SIZE * 2L
        if (chunks.size > MAX_CHUNKS_IN_RAM) {
            chunks.removeAll { it.endFrame44k < minKeepFrame }
        }

        var cachedChunk: StemChunk? = null
        for (f in 0 until frameCount) {
            val pos44k = alignedStart.toDouble() + f * ratio
            val f0 = pos44k.toLong()
            val frac = (pos44k - f0).toFloat()
            val f1 = f0 + 1L

            val c0 = cachedChunk?.takeIf { f0 >= it.startFrame44k && f0 < it.endFrame44k }
                ?: findChunk(f0)?.also { cachedChunk = it }
                ?: return false
            val l0 = (f0 - c0.startFrame44k).toInt()

            val c1 = cachedChunk?.takeIf { f1 >= it.startFrame44k && f1 < it.endFrame44k }
                ?: findChunk(f1)
                ?: c0
            val l1 = (f1 - c1.startFrame44k).toInt().coerceIn(0, c1.validFrames - 1)

            val off0 = l0 * 2
            val off1 = l1 * 2

            val instL0 = c0.instPcm[off0] * INV_SHORT
            val instR0 = c0.instPcm[off0 + 1] * INV_SHORT
            val vocL0 = c0.vocalPcm[off0] * INV_SHORT
            val vocR0 = c0.vocalPcm[off0 + 1] * INV_SHORT

            val instL1 = c1.instPcm[off1] * INV_SHORT
            val instR1 = c1.instPcm[off1 + 1] * INV_SHORT
            val vocL1 = c1.vocalPcm[off1] * INV_SHORT
            val vocR1 = c1.vocalPcm[off1 + 1] * INV_SHORT

            val l = (instL0 + (instL1 - instL0) * frac) +
                (vocL0 + (vocL1 - vocL0) * frac) * vocalKeep
            val r = (instR0 + (instR1 - instR0) * frac) +
                (vocR0 + (vocR1 - vocR0) * frac) * vocalKeep

            outInterleavedFloat[f * 2] = l.coerceIn(-1f, 1f)
            outInterleavedFloat[f * 2 + 1] = r.coerceIn(-1f, 1f)
        }
        return true
    }

    private fun findChunk(frame44k: Long): StemChunk? {
        if (frame44k < 0L) return null
        val snapshot = chunks
        for (i in snapshot.size - 1 downTo 0) {
            val c = snapshot[i]
            if (frame44k >= c.startFrame44k && frame44k < c.endFrame44k) {
                return c
            }
        }
        return null
    }

    private fun tryLockAlignment(
        ptsFrame44k: Long,
        refDryStereo: FloatArray,
        refFrames: Int,
        refSampleRate: Int,
    ) {
        val sampleCount = minOf(refFrames, 512)
        if (sampleCount < 128) return
        val ratio = MdxStftNative.MDX_SAMPLE_RATE.toDouble() / refSampleRate.toDouble()
        val refMono = FloatArray(sampleCount)
        var refEnergy = 0.0
        for (i in 0 until sampleCount) {
            val m = 0.5f * (refDryStereo[i * 2] + refDryStereo[i * 2 + 1])
            refMono[i] = m
            refEnergy += m * m
        }
        if (refEnergy / sampleCount < 1.0e-4) return

        var bestLag = 0
        var bestScore = -1.0
        var coarseLag = -ALIGN_SEARCH_RADIUS
        while (coarseLag <= ALIGN_SEARCH_RADIUS) {
            val s = correlationAtLag(ptsFrame44k, coarseLag.toLong(), refMono, sampleCount, ratio, 4)
            if (s > bestScore) {
                bestScore = s
                bestLag = coarseLag
            }
            coarseLag += 16
        }
        var fineBestLag = bestLag
        var fineBestScore = bestScore
        for (lag in (bestLag - 20)..(bestLag + 20) step 2) {
            val s = correlationAtLag(ptsFrame44k, lag.toLong(), refMono, sampleCount, ratio, 2)
            if (s > fineBestScore) {
                fineBestScore = s
                fineBestLag = lag
            }
        }
        for (lag in (fineBestLag - 2)..(fineBestLag + 2)) {
            val s = correlationAtLag(ptsFrame44k, lag.toLong(), refMono, sampleCount, ratio, 1)
            if (s > fineBestScore) {
                fineBestScore = s
                fineBestLag = lag
            }
        }

        if (fineBestScore >= 0.40) {
            ptsOffsetFrames44k = fineBestLag.toLong()
            alignmentLocked = true
        } else if (findChunk(ptsFrame44k) != null && abs(ptsFrame44k) > MdxStftNative.MDX_SAMPLE_RATE) {
            alignmentLocked = true
        }
    }

    private fun correlationAtLag(
        ptsFrame44k: Long,
        lag44k: Long,
        refMono: FloatArray,
        sampleCount: Int,
        ratio: Double,
        stride: Int,
    ): Double {
        var dot = 0.0
        var eRef = 0.0
        var eMix = 0.0
        val base = ptsFrame44k + lag44k
        var i = 0
        while (i < sampleCount) {
            val f = base + (i * ratio).roundToInt()
            val mixM = readReconstructedMono(f) ?: return -1.0
            val r = refMono[i].toDouble()
            dot += r * mixM
            eRef += r * r
            eMix += mixM * mixM
            i += stride
        }
        val denom = sqrt(eRef * eMix)
        return if (denom > 1.0e-9) dot / denom else -1.0
    }

    private fun readReconstructedMono(frame44k: Long): Double? {
        val c = findChunk(frame44k) ?: return null
        val local = (frame44k - c.startFrame44k).toInt()
        val off = local * 2
        val l = (c.instPcm[off].toInt() + c.vocalPcm[off].toInt()) * INV_SHORT
        val r = (c.instPcm[off + 1].toInt() + c.vocalPcm[off + 1].toInt()) * INV_SHORT
        return 0.5 * (l + r)
    }

    companion object {
        private const val INV_SHORT = 1f / 32767f
        private const val MAX_CHUNKS_IN_RAM = 16
        private const val ALIGN_SEARCH_RADIUS = 3072

        private fun floatToShort(value: Float): Short {
            val scaled = (value.coerceIn(-1f, 1f) * 32767f).roundToInt()
            return scaled.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
    }
}
