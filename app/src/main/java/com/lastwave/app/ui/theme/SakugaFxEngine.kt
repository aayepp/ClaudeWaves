package com.lastwave.app.ui.theme

import android.os.SystemClock
import android.provider.Settings
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotateRad
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private const val TWO_PI = (PI * 2.0).toFloat()
private const val SPRING_MAX_SUBSTEP_SEC = 0.004f
private const val MAX_LIVE_EFFECTS = 96

val LocalSakugaTheme = staticCompositionLocalOf { false }
val LocalSakugaMangaSfx = staticCompositionLocalOf { true }
val LocalSakugaFx = staticCompositionLocalOf<SakugaFxController?> { null }
val LocalSakugaPalette = staticCompositionLocalOf { SakugaDefaultLightPalette }

data class SakugaPalette(
    val fill: Color,
    val shade: Color,
    val line: Color,
    val accent: Color,
    val ink: Color,
    val shadow: Color,
    val sfxFill: Color,
    val sfxEdge: Color,
)

val SakugaDefaultLightPalette = SakugaPalette(
    fill = Color.White,
    shade = Color(0xFFB8B3FF),
    line = Color(0xFF5A4FD8),
    accent = Color(0xFFFF5FA8),
    ink = Color(0xFF1D1A4A),
    shadow = Color(0xFFB8B3FF),
    sfxFill = Color(0xFF1D1A4A),
    sfxEdge = Color.White,
)

val SakugaDefaultDarkPalette = SakugaPalette(
    fill = Color(0xFFF4F3FF),
    shade = Color(0xFF8F8DF7),
    line = Color(0xFFA6A0FF),
    accent = Color(0xFFFF5FA8),
    ink = Color(0xFFECEAFF),
    shadow = Color(0xFF5A4FD8),
    sfxFill = Color(0xFFFF5FA8),
    sfxEdge = Color(0xFF14112B),
)

/**
 * Resolves Sakuga's two-tone cel + shadow + accent palette. When the user picks their own
 * accent preset, wallpaper Dynamic Color, Mono, or Color Wheel, primary shifts to that hue
 * across both Light and Dark while keeping high-contrast anime line work intact.
 */
fun deriveSakugaPalette(
    scheme: ColorScheme,
    isDark: Boolean,
    useOwnPalette: Boolean,
): SakugaPalette {
    if (useOwnPalette) {
        return if (isDark) SakugaDefaultDarkPalette else SakugaDefaultLightPalette
    }
    val pri = scheme.primary
    return if (isDark) {
        SakugaPalette(
            fill = Color(0xFFF6F5FF),
            shade = lerp(pri, Color.White, 0.25f),
            line = lerp(pri, Color.White, 0.42f),
            accent = lerp(pri, Color(0xFFFF5FA8), 0.35f),
            ink = scheme.onBackground,
            shadow = lerp(pri, Color(0xFF14112B), 0.28f),
            sfxFill = if (pri.luminance() > 0.32f) pri else lerp(pri, Color.White, 0.35f),
            sfxEdge = scheme.background,
        )
    } else {
        SakugaPalette(
            fill = Color.White,
            shade = lerp(pri, Color.White, 0.56f),
            line = lerp(pri, Color(0xFF1D1A4A), 0.30f),
            accent = lerp(pri, Color(0xFFFF5FA8), 0.35f),
            ink = scheme.onBackground,
            shadow = lerp(pri, Color.White, 0.48f),
            sfxFill = scheme.onBackground,
            sfxEdge = Color.White,
        )
    }
}

/**
 * Sub-stepped damped spring so overshoot, bounce, and settling are deterministic
 * across 60 Hz, 90 Hz, 120 Hz, 144 Hz, and 165 Hz displays.
 */
internal class SakugaSpring(
    var x: Float = 0f,
    var stiffness: Float = 300f,
    var damping: Float = 20f,
) {
    var v: Float = 0f
    var to: Float = x

    fun step(dtSec: Float): Float {
        if (dtSec <= 0f) return x
        val steps = ceil((dtSec / SPRING_MAX_SUBSTEP_SEC).toDouble()).toInt().coerceIn(1, 10)
        val h = dtSec / steps
        repeat(steps) {
            v += (-stiffness * (x - to) - damping * v) * h
            x += v * h
        }
        return x
    }
}

private fun clamp01(v: Float): Float = v.coerceIn(0f, 1f)
private fun easeOutCubic(t: Float): Float = 1f - (1f - t).let { it * it * it }
private fun easeInCubic(t: Float): Float = t * t * t
private fun easeOutBack(t: Float): Float =
    1f + 2.70158f * (t - 1f) * (t - 1f) * (t - 1f) + 1.70158f * (t - 1f) * (t - 1f)

private fun rnd(min: Float, max: Float): Float = min + Random.nextFloat() * (max - min)
private fun rnd(max: Float): Float = Random.nextFloat() * max

private sealed class SakugaParticle(val lifeSec: Float) {
    var elapsedSec: Float = 0f
    var delaySec: Float = 0f
    val started: Boolean get() = elapsedSec >= delaySec
    val progress: Float get() = clamp01((elapsedSec - delaySec) / lifeSec)
    val tActive: Float get() = (elapsedSec - delaySec).coerceAtLeast(0f)
    val expired: Boolean get() = (elapsedSec - delaySec) >= lifeSec
}

private data class PuffLobe(val angle: Float, val dist: Float, val radius: Float, val phase: Float, val speed: Float)

private class PuffParticle(
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float,
    val size: Float,
    val lobes: List<PuffLobe>,
    val drawArcs: Boolean,
    val deep: Float,
    val lx: Float,
    lifeSec: Float,
) : SakugaParticle(lifeSec)

private data class ShardSpec(val angle: Float, val speed: Float, val size: Float, val spin: Float, val rot0: Float)

private class ShardsParticle(
    val x: Float,
    val y: Float,
    val gravity: Float,
    val specs: List<ShardSpec>,
    lifeSec: Float,
) : SakugaParticle(lifeSec)

private class LinesParticle(
    val x: Float,
    val y: Float,
    val count: Int,
    val dir: Float,
    val length: Float,
    val spread: Float,
    val seeds: List<Triple<Float, Float, Float>>,
    lifeSec: Float,
) : SakugaParticle(lifeSec)

private class TicksParticle(
    val x: Float,
    val y: Float,
    val angles: FloatArray,
    val rStart: Float,
    val len: Float,
    lifeSec: Float,
) : SakugaParticle(lifeSec)

private class BurstParticle(
    val x: Float,
    val y: Float,
    val radius: Float,
    val spikeRadii: FloatArray,
    val rot0: Float,
    val pinAngles: FloatArray,
    val pinLengths: FloatArray,
    val pinWidths: FloatArray,
    lifeSec: Float,
) : SakugaParticle(lifeSec)

private class SlashParticle(
    val x: Float,
    val y: Float,
    val length: Float,
    val angle: Float,
    val thickness: Float,
    lifeSec: Float,
) : SakugaParticle(lifeSec)

private class DonutParticle(
    val x: Float,
    val y: Float,
    val maxR: Float,
    val vx: Float,
    val vy: Float,
    lifeSec: Float,
) : SakugaParticle(lifeSec)

private class WobParticle(
    val x: Float,
    val y: Float,
    val dir: Float,
    val radius: Float,
    lifeSec: Float,
) : SakugaParticle(lifeSec)

private class KiraParticle(
    val x: Float,
    val y: Float,
    val radius: Float,
    lifeSec: Float,
) : SakugaParticle(lifeSec)

private class SfxParticle(
    val word: SakugaWord,
    val x: Float,
    val y: Float,
    val targetWidthPx: Float,
    val rotRad: Float,
    val dotted: Boolean,
    val scaleSpring: SakugaSpring,
    lifeSec: Float,
) : SakugaParticle(lifeSec) {
    var lastT: Float = 0f
}

/**
 * Single overlay particle controller shared by the whole app window.
 * Sleeps completely (zero frame callbacks) when [particles] is empty.
 */
class SakugaFxController {
    internal var enabled: Boolean = false
    internal var mangaSfxEnabled: Boolean = true
    internal var reducedMotion: Boolean = false
    internal var density: Float = 2.5f
    internal var palette: SakugaPalette = SakugaDefaultLightPalette
    internal var hostOriginInRoot: Offset = Offset.Zero

    private val particles = ArrayList<SakugaParticle>(32)
    internal val hasActive = mutableStateOf(false)
    internal val frameTick = mutableLongStateOf(0L)

    private var lastFrameNanos: Long = 0L
    private var lastCloudMs: Long = 0L
    private var lastDustMs: Long = 0L
    private var lastTabMs: Long = 0L

    private val tmpUnionPath = Path()
    private val tmpStarPath = Path()
    private val tmpAuxPath = Path()

    private fun toOverlay(rootPoint: Offset): Offset = rootPoint - hostOriginInRoot
    private fun px(dp: Float): Float = dp * density

    private fun canEmit(): Boolean = enabled && !reducedMotion

    private fun enqueue(p: SakugaParticle, delaySec: Float = 0f) {
        if (!canEmit()) return
        p.delaySec = delaySec
        if (particles.size >= MAX_LIVE_EFFECTS) {
            particles.removeAt(0)
        }
        particles.add(p)
        if (!hasActive.value) {
            lastFrameNanos = 0L
            hasActive.value = true
        }
    }

    internal fun step(frameNanos: Long) {
        val dt = if (lastFrameNanos == 0L) {
            0.008f
        } else {
            ((frameNanos - lastFrameNanos) / 1_000_000_000f).coerceIn(0.001f, 0.034f)
        }
        lastFrameNanos = frameNanos

        var i = particles.lastIndex
        while (i >= 0) {
            val p = particles[i]
            p.elapsedSec += dt
            if (p.expired) {
                particles.removeAt(i)
            }
            i--
        }
        frameTick.longValue = frameNanos
        if (particles.isEmpty()) {
            hasActive.value = false
            lastFrameNanos = 0L
        }
    }

    fun emitButtonPress(centerRoot: Offset, widthPx: Float, heightPx: Float) {
        if (!canEmit()) return
        val c = toOverlay(centerRoot)
        addTicks(c.x, c.y - heightPx * 0.5f - px(6f), dir = -PI.toFloat() / 2f, n = 3, r = px(4f), len = px(14f), fan = 0.5f, life = 0.4f)
        addWob(c.x - widthPx * 0.5f - px(6f), c.y, dir = -1f, radius = px(10f), life = 0.28f)
        addWob(c.x + widthPx * 0.5f + px(6f), c.y, dir = 1f, radius = px(10f), life = 0.28f)
        addLines(c.x, c.y + heightPx * 0.5f + px(8f), n = 3, dir = 0f, len = px(16f), spread = px(2f), life = 0.24f)
    }

    fun emitPrimaryImpact(centerRoot: Offset, widthPx: Float, heightPx: Float, withDonSfx: Boolean = true) {
        if (!canEmit()) return
        val c = toOverlay(centerRoot)
        addBurst(c.x, c.y, radius = minOf(px(74f), maxOf(px(44f), widthPx * 0.42f)), spikes = 16, life = 0.44f)
        addShards(c.x, c.y, n = 10, speed = px(180f), size = px(7f), life = 0.85f, arc = TWO_PI, grav = px(240f))
        val hw = widthPx * 0.45f + px(6f)
        val hh = heightPx * 0.45f
        for ((dx, dy) in listOf(-1f to -0.5f, 1f to -1f, -1f to 0.9f, 1f to 1f)) {
            addPuff(
                x = c.x + dx * hw,
                y = c.y + dy * hh,
                n = 5,
                size = px(rnd(10f, 14f)),
                vx = dx * px(40f),
                vy = dy * px(26f) - px(6f),
                life = rnd(0.65f, 0.95f),
            )
        }
        if (withDonSfx) {
            addSfx(SakugaWord.DON, c.x + px(8f), c.y - heightPx * 0.5f - px(32f), sizeDp = 96f, rot = -0.12f, life = 0.92f)
        }
        addTicks(c.x - hw - px(16f), c.y - hh - px(12f), dir = -2.4f, n = 2, r = px(4f), len = px(18f), fan = 0.45f, life = 0.42f)
        addTicks(c.x + hw + px(16f), c.y - hh - px(8f), dir = -0.7f, n = 2, r = px(4f), len = px(18f), fan = 0.45f, life = 0.42f)
        addDonut(c.x + px(6f), c.y - heightPx * 0.5f - px(16f), maxR = px(24f), vx = px(18f), vy = -px(42f), life = 1.05f, delay = 0.14f)
        addWob(c.x - widthPx * 0.5f - px(8f), c.y, dir = -1f, radius = px(11f), life = 0.34f, delay = 0.16f)
        addWob(c.x + widthPx * 0.5f + px(8f), c.y, dir = 1f, radius = px(11f), life = 0.34f, delay = 0.16f)
    }

    fun emitSecondaryTap(topRightRoot: Offset) {
        if (!canEmit()) return
        val c = toOverlay(topRightRoot)
        addTicks(c.x - px(4f), c.y + px(4f), dir = -1.05f, n = 3, r = px(5f), len = px(16f), fan = 0.40f, life = 0.45f)
    }

    fun emitChipTap(centerRoot: Offset, topRightRoot: Offset) {
        if (!canEmit()) return
        val tr = toOverlay(topRightRoot)
        addTicks(tr.x - px(4f), tr.y + px(2f), dir = -1.0f, n = 3, r = px(4f), len = px(14f), fan = 0.42f, life = 0.42f)
        addPuff(tr.x + px(4f), tr.y, n = 3, size = px(7f), vx = px(12f), vy = -px(14f), life = 0.50f)
        addSfx(SakugaWord.KACHI, centerRoot.x - hostOriginInRoot.x, tr.y - px(22f), sizeDp = 58f, rot = 0.08f, life = 0.70f)
    }

    fun emitToggleFlip(knobCenterRoot: Offset, turnedOn: Boolean) {
        if (!canEmit()) return
        val c = toOverlay(knobCenterRoot)
        if (turnedOn) {
            addTicks(c.x + px(4f), c.y, dir = -0.35f, n = 5, r = px(6f), len = px(18f), fan = 1.4f, life = 0.45f)
            addPuff(c.x + px(8f), c.y - px(10f), n = 4, size = px(10f), vx = px(20f), vy = -px(18f), life = 0.65f)
            addPuff(c.x + px(12f), c.y + px(8f), n = 3, size = px(7f), vx = px(16f), vy = px(6f), life = 0.52f)
            addSfx(SakugaWord.KACHI, c.x - px(18f), c.y - px(28f), sizeDp = 66f, rot = 0.12f, life = 0.80f)
        } else {
            addPuff(c.x, c.y, n = 3, size = px(7f), vx = -px(12f), vy = -px(10f), life = 0.42f)
        }
    }

    fun emitCheckboxChange(centerRoot: Offset, checked: Boolean) {
        if (!canEmit()) return
        val c = toOverlay(centerRoot)
        if (!checked) {
            addPuff(c.x, c.y, n = 3, size = px(7f), vx = 0f, vy = -px(12f), life = 0.40f)
            return
        }
        addSlash(c.x + px(6f), c.y + px(2f), length = px(84f), angle = -0.42f, thickness = px(10f), life = 0.46f)
        addSlash(c.x - px(4f), c.y + px(10f), length = px(64f), angle = 2.72f, thickness = px(6f), life = 0.50f)
        addLines(c.x - px(22f), c.y + px(16f), n = 6, dir = 2.75f, len = px(44f), spread = px(10f), life = 0.36f)
        addShards(c.x, c.y, n = 6, speed = px(140f), size = px(5.5f), life = 0.68f, arc = TWO_PI, grav = px(200f))
        addKira(c.x + px(20f), c.y - px(18f), radius = px(9f), life = 0.45f, delay = 0.28f)
        addSfx(SakugaWord.ZUZA, c.x + px(18f), c.y - px(38f), sizeDp = 72f, rot = -0.12f, life = 0.85f, dotted = true)
    }

    fun emitSliderCloud(thumbCenterRoot: Offset, dirSign: Float, speedRatio: Float) {
        if (!canEmit()) return
        val nowMs = SystemClock.uptimeMillis()
        if (nowMs - lastCloudMs < 65L) return
        lastCloudMs = nowMs
        val c = toOverlay(thumbCenterRoot)
        val s = clamp01(speedRatio)
        addPuff(
            x = c.x - dirSign * px(10f),
            y = c.y - px(4f),
            n = 5,
            size = px(8f + s * 6f),
            vx = -dirSign * px(26f + s * 32f),
            vy = -px(14f),
            life = 0.62f,
            drawArcs = true,
        )
        addLines(
            x = c.x - dirSign * px(14f),
            y = c.y,
            n = 3,
            dir = if (dirSign > 0f) PI.toFloat() else 0f,
            len = px(24f + s * 18f),
            spread = px(8f),
            life = 0.28f,
        )
        if (Random.nextFloat() < 0.45f) {
            addShards(
                x = c.x - dirSign * px(8f),
                y = c.y,
                n = 2,
                speed = px(110f),
                size = px(4f),
                life = 0.45f,
                dir = if (dirSign > 0f) PI.toFloat() else 0f,
                arc = 1.2f,
                grav = px(140f),
            )
        }
    }

    fun emitSliderRelease(thumbCenterRoot: Offset, dirSign: Float) {
        if (!canEmit()) return
        val c = toOverlay(thumbCenterRoot)
        val angle = if (dirSign >= 0f) 0f else PI.toFloat()
        addSlash(c.x + dirSign * px(14f), c.y, length = px(42f), angle = angle, thickness = px(8f), life = 0.38f)
        addPuff(c.x - dirSign * px(10f), c.y - px(4f), n = 3, size = px(8f), vx = -dirSign * px(18f), vy = -px(10f), life = 0.50f)
        addPuff(c.x - dirSign * px(18f), c.y + px(4f), n = 3, size = px(6f), vx = -dirSign * px(12f), vy = -px(6f), life = 0.42f, delay = 0.06f)
        addSfx(SakugaWord.SHU, c.x, c.y - px(28f), sizeDp = 64f, rot = -0.10f * dirSign, life = 0.72f)
    }

    fun emitTypingFocus(topRightRoot: Offset) {
        if (!canEmit()) return
        val c = toOverlay(topRightRoot)
        addTicks(c.x - px(6f), c.y + px(4f), dir = -1.05f, n = 3, r = px(5f), len = px(16f), fan = 0.36f, life = 0.48f)
    }

    fun emitTypingDust(caretRoot: Offset) {
        if (!canEmit()) return
        val nowMs = SystemClock.uptimeMillis()
        if (nowMs - lastDustMs < 35L) return
        lastDustMs = nowMs
        val c = toOverlay(caretRoot)
        addPuff(
            x = c.x,
            y = c.y + px(4f),
            n = 2,
            size = px(rnd(4.5f, 6.5f)),
            vx = px(rnd(-10f, 10f)),
            vy = -px(22f),
            life = 0.38f,
        )
        if (Random.nextFloat() < 0.35f) {
            addShards(c.x, c.y, n = 2, speed = px(95f), size = px(3.2f), life = 0.46f, arc = TWO_PI, grav = px(150f))
        }
    }

    fun emitTabLand(tabCenterRoot: Offset, tabWidthPx: Float, tabHeightPx: Float, dirSign: Float) {
        if (!canEmit()) return
        val nowMs = SystemClock.uptimeMillis()
        if (nowMs - lastTabMs < 80L) return
        lastTabMs = nowMs
        val c = toOverlay(tabCenterRoot)
        addTicks(c.x, c.y - tabHeightPx * 0.5f - px(8f), dir = -PI.toFloat() / 2f, n = 3, r = px(4f), len = px(14f), fan = 0.5f, life = 0.46f)
        addPuff(c.x - dirSign * (tabWidthPx * 0.5f + px(8f)), c.y - px(4f), n = 4, size = px(8.5f), vx = -dirSign * px(18f), vy = -px(12f), life = 0.62f)
        addPuff(c.x - dirSign * (tabWidthPx * 0.5f + px(16f)), c.y + px(10f), n = 3, size = px(6f), vx = -dirSign * px(14f), vy = -px(4f), life = 0.50f, delay = 0.08f)
        addLines(c.x - dirSign * (tabWidthPx * 0.5f), c.y, n = 4, dir = if (dirSign > 0f) PI.toFloat() else 0f, len = px(28f), spread = px(9f), life = 0.32f)
    }

    fun emitToastLand(toastCenterRoot: Offset, widthPx: Float, heightPx: Float) {
        if (!canEmit()) return
        val c = toOverlay(toastCenterRoot)
        val hw = widthPx * 0.5f
        val hh = heightPx * 0.5f
        addLines(c.x - hw - px(4f), c.y, n = 8, dir = PI.toFloat(), len = px(56f), spread = px(14f), life = 0.36f)
        for (u in floatArrayOf(-0.46f, -0.28f, -0.10f)) {
            addPuff(c.x + widthPx * u, c.y + hh + px(2f), n = 4, size = px(rnd(7f, 11f)), vx = -px(rnd(10f, 26f)), vy = -px(8f), life = rnd(0.65f, 0.90f))
        }
        addPuff(c.x + hw + px(6f), c.y + px(8f), n = 3, size = px(7f), vx = px(18f), vy = -px(4f), life = 0.55f)
        addShards(c.x - hw, c.y + px(10f), n = 4, speed = px(130f), size = px(4f), life = 0.60f, dir = PI.toFloat() + 0.5f, arc = 1.6f, grav = px(180f))
        addTicks(c.x + px(26f), c.y + hh + px(10f), dir = 0.5f, n = 2, r = px(4f), len = px(14f), fan = 0.45f, life = 0.42f)
        addWob(c.x + hw + px(6f), c.y, dir = 1f, radius = px(10f), life = 0.32f)
        addSfx(SakugaWord.ZUZA, c.x - px(24f), c.y - hh - px(30f), sizeDp = 78f, rot = -0.10f, life = 0.88f, dotted = true)
    }

    fun emitToastDismiss(toastCenterRoot: Offset) {
        if (!canEmit()) return
        val c = toOverlay(toastCenterRoot)
        addPuff(c.x, c.y, n = 4, size = px(9f), vx = 0f, vy = -px(14f), life = 0.48f)
    }

    fun emitLikeBurst(heartCenterRoot: Offset) {
        if (!canEmit()) return
        val c = toOverlay(heartCenterRoot)
        addBurst(c.x, c.y, radius = px(38f), spikes = 13, life = 0.36f)
        addShards(c.x, c.y, n = 7, speed = px(145f), size = px(5f), life = 0.72f, arc = TWO_PI, grav = px(200f))
        addPuff(c.x - px(18f), c.y - px(10f), n = 4, size = px(8f), vx = -px(22f), vy = -px(14f), life = 0.60f)
        addPuff(c.x + px(18f), c.y + px(10f), n = 4, size = px(7f), vx = px(22f), vy = px(10f), life = 0.56f)
        addSfx(SakugaWord.DON, c.x, c.y - px(34f), sizeDp = 74f, rot = -0.14f, life = 0.85f)
    }

    private fun addPuff(
        x: Float,
        y: Float,
        n: Int,
        size: Float,
        vx: Float,
        vy: Float,
        life: Float,
        drawArcs: Boolean = size > px(6.5f),
        delay: Float = 0f,
    ) {
        val lobes = buildList(n + 1) {
            add(PuffLobe(0f, 0f, size, 0f, 1f))
            repeat(n) { i ->
                add(
                    PuffLobe(
                        angle = (i.toFloat() / n.coerceAtLeast(1)) * TWO_PI + rnd(0.6f),
                        dist = rnd(0.25f, 0.90f) * size,
                        radius = rnd(0.55f, 1.0f) * size,
                        phase = rnd(TWO_PI),
                        speed = rnd(0.6f, 1.3f),
                    ),
                )
            }
        }
        enqueue(
            PuffParticle(
                x = x,
                y = y,
                vx = vx,
                vy = vy,
                size = size,
                lobes = lobes,
                drawArcs = drawArcs,
                deep = 1.4f,
                lx = 0.5f,
                lifeSec = life,
            ),
            delaySec = delay,
        )
    }

    private fun addBurst(x: Float, y: Float, radius: Float, spikes: Int, life: Float) {
        val radii = FloatArray(spikes * 2) { i -> if (i % 2 == 1) rnd(0.34f, 0.50f) else rnd(0.75f, 1.15f) }
        val pinCount = 8
        val pAngles = FloatArray(pinCount) { i -> (i.toFloat() / pinCount) * TWO_PI + rnd(0.5f) }
        val pLengths = FloatArray(pinCount) { rnd(1.25f, 1.72f) }
        val pWidths = FloatArray(pinCount) { rnd(0.030f, 0.055f) }
        enqueue(
            BurstParticle(
                x = x,
                y = y,
                radius = radius,
                spikeRadii = radii,
                rot0 = rnd(TWO_PI),
                pinAngles = pAngles,
                pinLengths = pLengths,
                pinWidths = pWidths,
                lifeSec = life,
            ),
        )
    }

    private fun addShards(
        x: Float,
        y: Float,
        n: Int,
        speed: Float,
        size: Float,
        life: Float,
        dir: Float = -PI.toFloat() / 2f,
        arc: Float = PI.toFloat(),
        grav: Float = px(200f),
    ) {
        val specs = List(n) {
            ShardSpec(
                angle = dir + rnd(-arc * 0.5f, arc * 0.5f),
                speed = rnd(0.5f, 1.1f) * speed,
                size = rnd(0.6f, 1.25f) * size,
                spin = rnd(-8f, 8f),
                rot0 = rnd(TWO_PI),
            )
        }
        enqueue(ShardsParticle(x, y, grav, specs, life))
    }

    private fun addLines(x: Float, y: Float, n: Int, dir: Float, len: Float, spread: Float, life: Float) {
        val seeds = List(n) { Triple(rnd(-spread, spread), rnd(0.6f, 1.2f), rnd(-0.18f, 0.18f)) }
        enqueue(LinesParticle(x, y, n, dir, len, spread, seeds, life))
    }

    private fun addTicks(x: Float, y: Float, dir: Float, n: Int, r: Float, len: Float, fan: Float, life: Float) {
        val angles = FloatArray(n) { i ->
            if (n == 1) dir else dir + (-0.5f + i.toFloat() / (n - 1)) * fan
        }
        enqueue(TicksParticle(x, y, angles, r, len, life))
    }

    private fun addSlash(x: Float, y: Float, length: Float, angle: Float, thickness: Float, life: Float) {
        enqueue(SlashParticle(x, y, length, angle, thickness, life))
    }

    private fun addDonut(x: Float, y: Float, maxR: Float, vx: Float, vy: Float, life: Float, delay: Float = 0f) {
        enqueue(DonutParticle(x, y, maxR, vx, vy, life), delaySec = delay)
    }

    private fun addWob(x: Float, y: Float, dir: Float, radius: Float, life: Float, delay: Float = 0f) {
        enqueue(WobParticle(x, y, dir, radius, life), delaySec = delay)
    }

    private fun addKira(x: Float, y: Float, radius: Float, life: Float, delay: Float = 0f) {
        enqueue(KiraParticle(x, y, radius, life), delaySec = delay)
    }

    private fun addSfx(word: SakugaWord, x: Float, y: Float, sizeDp: Float, rot: Float, life: Float, dotted: Boolean = false) {
        if (!mangaSfxEnabled) return
        val spring = SakugaSpring(0.2f, stiffness = 520f, damping = 17f).apply { to = 1f }
        enqueue(SfxParticle(word, x, y, px(sizeDp), rot, dotted, spring, life))
    }

    internal fun DrawScope.renderAll() {
        val pal = palette
        for (i in 0 until particles.size) {
            val p = particles[i]
            if (!p.started || p.expired) continue
            val k = p.progress
            val t = p.tActive
            when (p) {
                is BurstParticle -> drawBurst(p, k, pal)
                is PuffParticle -> drawPuff(p, k, t, pal)
                is ShardsParticle -> drawShards(p, k, t, pal)
                is LinesParticle -> drawLines(p, k, pal)
                is TicksParticle -> drawTicks(p, k, pal)
                is SlashParticle -> drawSlash(p, k, pal)
                is DonutParticle -> drawDonut(p, k, pal)
                is WobParticle -> drawWob(p, k, pal)
                is KiraParticle -> drawKira(p, k, pal)
                is SfxParticle -> drawSfx(p, k, t, pal)
            }
        }
    }

    private fun DrawScope.drawBlob(
        cx: FloatArray,
        cy: FloatArray,
        cr: FloatArray,
        count: Int,
        lw: Float,
        lx: Float,
        deep: Float,
        arcs: Boolean,
        pal: SakugaPalette,
    ) {
        val u = tmpUnionPath
        u.reset()
        var firstR = 0f
        var any = false
        for (i in 0 until count) {
            val r = cr[i]
            if (r > 0.5f) {
                if (!any) {
                    firstR = r
                    any = true
                }
                u.addOval(Rect(cx[i] - r, cy[i] - r, cx[i] + r, cy[i] + r))
            }
        }
        if (!any) return
        drawPath(u, color = pal.line, style = Stroke(width = lw * 2f, join = StrokeJoin.Round, cap = StrokeCap.Round))
        clipPath(u) {
            drawRect(color = pal.shade)
            val offX = lx * firstR * 0.2f * deep
            val offY = -firstR * 0.26f * deep
            translate(left = offX, top = offY) {
                drawPath(u, color = pal.fill)
            }
            if (arcs) {
                for (i in 0 until count) {
                    val rr = cr[i]
                    if (rr > px(3f)) {
                        drawArc(
                            color = pal.line,
                            startAngle = -155f,
                            sweepAngle = 125f,
                            useCenter = false,
                            topLeft = Offset(cx[i] - rr, cy[i] - rr),
                            size = androidx.compose.ui.geometry.Size(rr * 2f, rr * 2f),
                            style = Stroke(width = lw * 0.75f, cap = StrokeCap.Round),
                        )
                    }
                }
            }
        }
    }

    private fun DrawScope.drawPuff(p: PuffParticle, k: Float, t: Float, pal: SakugaPalette) {
        val grow = easeOutBack(clamp01(t / 0.22f))
        val die = 1f - easeInCubic(clamp01((k - 0.55f) / 0.45f))
        if (die <= 0.01f) return
        val px = p.x + p.vx * easeOutCubic(k) * p.lifeSec
        val py = p.y + p.vy * easeOutCubic(k) * p.lifeSec
        val n = p.lobes.size
        val xs = FloatArray(n)
        val ys = FloatArray(n)
        val rs = FloatArray(n)
        val radialScale = 0.5f + easeOutCubic(k) * 0.9f
        for (i in 0 until n) {
            val l = p.lobes[i]
            val ang = l.angle + t * 0.9f * l.speed
            xs[i] = px + cos(ang) * l.dist * radialScale
            ys[i] = py + sin(ang) * l.dist * radialScale - easeOutCubic(k) * px(4f) * l.speed
            rs[i] = (l.radius * grow * die * (1f + sin(t * 9f + l.phase) * 0.05f)).coerceAtLeast(0f)
        }
        drawBlob(xs, ys, rs, n, lw = px(1.4f), lx = p.lx, deep = p.deep, arcs = p.drawArcs, pal = pal)
    }

    private fun DrawScope.drawBurst(p: BurstParticle, k: Float, pal: SakugaPalette) {
        val R = p.radius * easeOutCubic(clamp01(k / 0.30f))
        val hollow = 1f + easeInCubic(clamp01((k - 0.35f) / 0.65f)) * 1.3f
        val r0 = R * 0.6f * minOf(1.9f, hollow)
        val path = tmpAuxPath
        // Pink accent needles
        path.reset()
        for (i in p.pinAngles.indices) {
            val L = R * p.pinLengths[i]
            if (L <= r0) continue
            val a = p.pinAngles[i]
            val w = p.pinWidths[i]
            path.moveTo(p.x + cos(a - w) * r0, p.y + sin(a - w) * r0)
            path.lineTo(p.x + cos(a) * L, p.y + sin(a) * L)
            path.lineTo(p.x + cos(a + w) * r0, p.y + sin(a + w) * r0)
            path.close()
        }
        drawPath(path, color = pal.accent)

        if (hollow >= 1.92f) return
        fun buildStar(target: Path, rad: Float, turn: Float) {
            target.reset()
            val len = p.spikeRadii.size
            for (i in 0 until len) {
                val a = p.rot0 + turn + (i.toFloat() / len) * TWO_PI
                val mult = if (i % 2 == 1) minOf(0.95f, p.spikeRadii[i] * hollow) else p.spikeRadii[i]
                val rr = rad * mult
                val sx = p.x + cos(a) * rr
                val sy = p.y + sin(a) * rr
                if (i == 0) target.moveTo(sx, sy) else target.lineTo(sx, sy)
            }
            target.close()
        }
        buildStar(tmpStarPath, R * 1.15f, 0.19f)
        drawPath(tmpStarPath, color = pal.shade)
        drawPath(tmpStarPath, color = pal.line, style = Stroke(width = px(1.2f), join = StrokeJoin.Round))

        buildStar(tmpStarPath, R * 0.84f, 0f)
        drawPath(tmpStarPath, color = pal.line, style = Stroke(width = px(2.6f), join = StrokeJoin.Round))
        clipPath(tmpStarPath) {
            drawRect(color = pal.shade)
            translate(left = -px(2f), top = -px(3f)) {
                drawPath(tmpStarPath, color = pal.fill)
            }
        }
    }

    private fun DrawScope.drawShards(p: ShardsParticle, k: Float, t: Float, pal: SakugaPalette) {
        val sh = 1f - easeInCubic(clamp01((k - 0.35f) / 0.65f))
        if (sh <= 0.04f) return
        val path = tmpAuxPath
        for (s in p.specs) {
            val pxPos = p.x + cos(s.angle) * s.speed * t
            val pyPos = p.y + sin(s.angle) * s.speed * t + 0.5f * p.gravity * t * t
            val sz = s.size * sh
            withTransform({
                translate(pxPos, pyPos)
                rotateRad(s.rot0 + s.spin * t, Offset.Zero)
            }) {
                path.reset()
                path.moveTo(0f, -sz)
                path.lineTo(sz * 0.55f, 0f)
                path.lineTo(0f, sz * 0.8f)
                path.lineTo(-sz * 0.55f, 0f)
                path.close()
                drawPath(path, color = pal.fill)
                drawPath(path, color = pal.line, style = Stroke(width = px(1.2f), join = StrokeJoin.Round))
            }
        }
    }

    private fun DrawScope.drawLines(p: LinesParticle, k: Float, pal: SakugaPalette) {
        val nx = cos(p.dir)
        val ny = sin(p.dir)
        val pxPerp = -ny
        val pyPerp = nx
        val head = easeOutCubic(clamp01(k / 0.45f))
        val tail = easeInCubic(clamp01((k - 0.18f) / 0.82f))
        if (head <= tail) return
        for (i in 0 until p.count) {
            val (lat, lenMult, angOff) = p.seeds[i]
            val L = p.length * lenMult
            val dx = cos(p.dir + angOff)
            val dy = sin(p.dir + angOff)
            val bx = p.x + pxPerp * lat
            val by = p.y + pyPerp * lat
            val s = Offset(bx + dx * L * tail, by + dy * L * tail)
            val e = Offset(bx + dx * L * head, by + dy * L * head)
            drawLine(pal.line, s, e, strokeWidth = px(2.0f) * (1f - tail * 0.6f), cap = StrokeCap.Round)
        }
    }

    private fun DrawScope.drawTicks(p: TicksParticle, k: Float, pal: SakugaPalette) {
        val tm = easeOutCubic(k)
        val shrink = 1f - easeInCubic(clamp01((k - 0.42f) / 0.58f))
        if (shrink <= 0.04f) return
        val path = tmpAuxPath
        for (a in p.angles) {
            val dist = p.rStart + (p.len * 1.4f) * tm
            val L = p.len * shrink
            val hw = px(2.8f) * shrink
            val nx = cos(a)
            val ny = sin(a)
            val perpX = -ny
            val perpY = nx
            path.reset()
            path.moveTo(p.x + nx * (dist + L), p.y + ny * (dist + L))
            path.lineTo(p.x + nx * dist + perpX * hw, p.y + ny * dist + perpY * hw)
            path.lineTo(p.x + nx * dist - perpX * hw, p.y + ny * dist - perpY * hw)
            path.close()
            drawPath(path, color = pal.accent)
        }
    }

    private fun DrawScope.drawSlash(p: SlashParticle, k: Float, pal: SakugaPalette) {
        val shrink = 1f - easeInCubic(clamp01((k - 0.35f) / 0.65f))
        if (shrink <= 0.04f) return
        val advance = easeOutCubic(k) * px(22f)
        val halfL = p.length * 0.5f * shrink
        val thick = p.thickness * shrink
        val path = tmpAuxPath
        withTransform({
            translate(p.x + cos(p.angle) * advance, p.y + sin(p.angle) * advance)
            rotateRad(p.angle, Offset.Zero)
        }) {
            path.reset()
            path.moveTo(-halfL, 0f)
            path.quadraticTo(0f, -thick, halfL, 0f)
            path.quadraticTo(0f, -thick * 0.25f, -halfL, 0f)
            path.close()
            drawPath(path, color = pal.fill)
            drawPath(path, color = pal.line, style = Stroke(width = px(1.5f), join = StrokeJoin.Round))
        }
    }

    private fun DrawScope.drawDonut(p: DonutParticle, k: Float, pal: SakugaPalette) {
        val r = p.maxR * (0.25f + 0.75f * easeOutCubic(k))
        val strokeW = (px(4.5f) * (1f - easeInCubic(clamp01((k - 0.3f) / 0.7f)))).coerceAtLeast(0f)
        if (strokeW <= 0.3f) return
        val cx = p.x + p.vx * easeOutCubic(k) * p.lifeSec
        val cy = p.y + p.vy * easeOutCubic(k) * p.lifeSec
        drawCircle(color = pal.line, radius = r, center = Offset(cx, cy), style = Stroke(width = strokeW + px(2.2f)))
        drawCircle(color = pal.fill, radius = r, center = Offset(cx, cy), style = Stroke(width = strokeW))
    }

    private fun DrawScope.drawWob(p: WobParticle, k: Float, pal: SakugaPalette) {
        val sweepScale = 1f - easeInCubic(clamp01((k - 0.4f) / 0.6f))
        if (sweepScale <= 0.05f) return
        val rr = p.radius * (0.75f + 0.35f * easeOutCubic(k))
        val baseAngle = if (p.dir > 0f) -35f else 145f
        val strokeW = px(2.2f) * sweepScale
        drawArc(
            color = pal.line,
            startAngle = baseAngle,
            sweepAngle = 70f * sweepScale,
            useCenter = false,
            topLeft = Offset(p.x - rr, p.y - rr),
            size = androidx.compose.ui.geometry.Size(rr * 2f, rr * 2f),
            style = Stroke(width = strokeW, cap = StrokeCap.Round),
        )
    }

    private fun DrawScope.drawKira(p: KiraParticle, k: Float, pal: SakugaPalette) {
        val s = sin(k * PI.toFloat())
        val r = p.radius * s
        if (r <= 0.8f) return
        val w = r * 0.24f
        val path = tmpAuxPath
        path.reset()
        path.moveTo(p.x, p.y - r)
        path.quadraticTo(p.x + w, p.y - w, p.x + r, p.y)
        path.quadraticTo(p.x + w, p.y + w, p.x, p.y + r)
        path.quadraticTo(p.x - w, p.y + w, p.x - r, p.y)
        path.quadraticTo(p.x - w, p.y - w, p.x, p.y - r)
        path.close()
        drawPath(path, color = pal.fill)
        drawPath(path, color = pal.line, style = Stroke(width = px(1.4f), join = StrokeJoin.Round))
    }

    private fun DrawScope.drawSfx(p: SfxParticle, k: Float, t: Float, pal: SakugaPalette) {
        val scSpring = p.scaleSpring.step((t - p.lastT).coerceAtLeast(0.001f))
        p.lastT = t
        val die = 1f - easeInCubic(clamp01((k - 0.84f) / 0.16f))
        val sc = (scSpring * die).coerceAtLeast(0f)
        if (sc <= 0.03f) return
        val k0 = (p.targetWidthPx / p.word.width) * sc
        val jx = sin(t * 31f) * px(0.8f)
        val jy = cos(t * 27f) * px(0.8f)
        val wobbleRot = p.rotRad + sin(t * 6f) * 0.03f
        val path = p.word.toComposePath()
        val strokeW = p.word.width * 0.068f
        withTransform({
            translate(p.x + jx, p.y + jy)
            rotateRad(wobbleRot, Offset.Zero)
            scale(k0, k0, Offset.Zero)
            translate(-p.word.width * 0.5f, -p.word.height * 0.5f)
        }) {
            drawPath(
                path = path,
                color = pal.sfxEdge,
                style = Stroke(width = strokeW, join = StrokeJoin.Round, cap = StrokeCap.Round),
            )
            if (p.dotted) {
                drawPath(path = path, color = Color.White)
                clipPath(path) {
                    val step = p.word.width * 0.055f
                    val dotR = p.word.width * 0.013f
                    var row = 0
                    var yy = 0f
                    while (yy <= p.word.height + step) {
                        var xx = if (row % 2 == 0) 0f else step * 0.5f
                        while (xx <= p.word.width + step) {
                            drawCircle(color = pal.sfxFill, radius = dotR, center = Offset(xx, yy))
                            xx += step
                        }
                        yy += step * 0.85f
                        row++
                    }
                }
            } else {
                drawPath(path = path, color = pal.sfxFill)
            }
        }
    }
}

/**
 * Full-window Canvas layer anchored above app content (including PlayerHost, bottom sheets,
 * and toasts) matching Sakuga VFX's shared single-canvas clock architecture.
 */
@Composable
fun SakugaFxOverlay(
    controller: SakugaFxController,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val density = LocalDensity.current.density
    controller.density = density
    controller.reducedMotion = remember(context) {
        runCatching {
            Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
        }.getOrDefault(false)
    }

    LaunchedEffect(controller.hasActive.value) {
        if (!controller.hasActive.value) return@LaunchedEffect
        while (controller.hasActive.value) {
            withFrameNanos { nanos ->
                controller.step(nanos)
            }
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { coords ->
                if (coords.isAttached) {
                    controller.hostOriginInRoot = coords.positionInRoot()
                }
            },
    ) {
        controller.frameTick.longValue
        with(controller) { renderAll() }
    }
}
