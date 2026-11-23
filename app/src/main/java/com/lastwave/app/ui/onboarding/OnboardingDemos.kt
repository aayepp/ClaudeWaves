package com.lastwave.app.ui.onboarding

import android.provider.Settings
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.sp
import com.lastwave.app.ui.theme.ComicInk
import com.lastwave.app.ui.theme.LocalComicTheme
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lastwave.app.ui.theme.comicPanel
import kotlin.math.PI

/** True for the tutorial page on screen; off-screen pages hold a still frame. */
internal val LocalOnboardingPageActive = compositionLocalOf { true }

/**
 * A 0..1 loop for a page's demo. Still (at [still]) while the page is off screen or the
 * system's animations are off: with a zero animator scale a tween ends at once, and the
 * loop would spin.
 */
@Composable
private fun demoClock(periodMs: Int, still: Float = 0.62f): State<Float> {
    val active = LocalOnboardingPageActive.current
    val context = LocalContext.current
    val animationsOff = remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
    val clock = remember { Animatable(still) }
    LaunchedEffect(active, animationsOff) {
        if (!active || animationsOff) {
            clock.snapTo(still)
            return@LaunchedEffect
        }
        clock.snapTo(0f)
        while (true) {
            clock.animateTo(1f, tween(periodMs, easing = LinearEasing))
            clock.snapTo(0f)
        }
    }
    return clock.asState()
}

/** Eased 0..1 progress of [t] through [from]..[to]. */
private fun window(t: Float, from: Float, to: Float): Float =
    FastOutSlowInEasing.transform(((t - from) / (to - from)).coerceIn(0f, 1f))

/** In over [inFrom]..[inTo], held, out over the last tenth: one beat of a loop. */
private fun beat(t: Float, inFrom: Float, inTo: Float): Float = window(t, inFrom, inTo) * (1f - window(t, 0.9f, 1f))

private val DemoShape = RoundedCornerShape(20.dp)
private val DemoHeight = 124.dp

@Composable
private fun DemoFrame(content: @Composable () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(DemoHeight)
            .comicPanel(DemoShape)
            .clip(DemoShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(16.dp),
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
private fun IconBubble(icon: ImageVector, container: Color, tint: Color, size: Dp = 44.dp, modifier: Modifier = Modifier) {
    Box(modifier.size(size).clip(CircleShape).background(container), contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(size * 0.5f))
    }
}

@Composable
private fun Caption(text: String, color: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Text(text, style = MaterialTheme.typography.labelMedium, color = color, maxLines = 1, overflow = TextOverflow.Ellipsis)
}

/** Welcome: three feature tiles rise in one after another. */
@Composable
internal fun WelcomeDemo() {
    val t = demoClock(5000)
    val scheme = MaterialTheme.colorScheme
    val features = listOf(Icons.Filled.HighQuality to "Hi-Res", Icons.Filled.Lyrics to "Lyrics", Icons.Filled.CloudDownload to "Offline")
    DemoFrame {
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            features.forEachIndexed { i, (icon, label) ->
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .graphicsLayer {
                            val k = beat(t.value, 0.05f + i * 0.12f, 0.3f + i * 0.12f)
                            alpha = k
                            translationY = (1f - k) * 16.dp.toPx()
                        }
                        .clip(RoundedCornerShape(16.dp))
                        .background(scheme.surfaceContainerHighest),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    IconBubble(icon, scheme.primaryContainer, scheme.onPrimaryContainer, size = 40.dp)
                    Spacer(Modifier.height(8.dp))
                    Caption(label, scheme.onSurface)
                }
            }
        }
    }
}

/** Theme: the ring steps along the swatches and a sample card takes each accent. */
@Composable
internal fun ThemeDemo() {
    val t = demoClock(6000)
    val scheme = MaterialTheme.colorScheme
    val swatches = listOf(scheme.primary, scheme.tertiary, scheme.secondary, scheme.error)
    val index by remember { derivedStateOf { (t.value * swatches.size).toInt().coerceIn(0, swatches.lastIndex) } }
    val accent by animateColorAsState(swatches[index], tween(500), label = "demoAccent")
    DemoFrame {
        Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                swatches.forEachIndexed { i, c ->
                    Box(
                        Modifier
                            .size(32.dp)
                            .border(2.dp, if (i == index) scheme.onSurface else Color.Transparent, CircleShape)
                            .padding(4.dp)
                            .clip(CircleShape)
                            .background(c),
                    )
                }
            }
            Row(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(scheme.surfaceContainerHighest)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconBubble(Icons.Filled.MusicNote, accent, scheme.surface, size = 36.dp)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("Now Playing", style = MaterialTheme.typography.labelLarge, color = scheme.onSurface, maxLines = 1)
                    Spacer(Modifier.height(6.dp))
                    Box(Modifier.fillMaxWidth(0.8f).height(4.dp).clip(CircleShape).background(accent))
                }
            }
        }
    }
}

/** Profile: the avatar settles in, then a name types out. */
@Composable
internal fun ProfileDemo() {
    val t = demoClock(5000)
    val scheme = MaterialTheme.colorScheme
    val name = "Your name"
    val typed by remember { derivedStateOf { (window(t.value, 0.3f, 0.7f) * name.length).toInt() } }
    val caretOn by remember { derivedStateOf { (t.value * 10).toInt() % 2 == 0 } }
    DemoFrame {
        Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            IconBubble(
                Icons.Filled.Person,
                scheme.primaryContainer,
                scheme.onPrimaryContainer,
                size = 64.dp,
                modifier = Modifier.graphicsLayer {
                    val k = beat(t.value, 0.02f, 0.22f)
                    alpha = k
                    scaleX = 0.8f + 0.2f * k
                    scaleY = scaleX
                },
            )
            Spacer(Modifier.width(16.dp))
            Row(
                Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(scheme.surfaceContainerHighest)
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(name.take(typed), style = MaterialTheme.typography.bodyLarge, color = scheme.onSurface, maxLines = 1)
                Box(Modifier.padding(start = 2.dp).width(2.dp).height(20.dp).background(if (caretOn) scheme.primary else Color.Transparent))
            }
        }
    }
}

/** Accounts: YouTube and Spotify join the app, a dot riding each link. */
@Composable
internal fun AccountsDemo() {
    val t = demoClock(3600)
    val scheme = MaterialTheme.colorScheme
    val outline = scheme.outlineVariant
    val dot = scheme.primary
    DemoFrame {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val bubble = 40.dp
            Canvas(Modifier.fillMaxSize()) {
                val r = bubble.toPx() / 2f
                val app = Offset(size.width - r, size.height / 2f)
                listOf(Offset(r, r), Offset(r, size.height - r)).forEachIndexed { i, s ->
                    drawLine(outline, s, app, 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 6.dp.toPx()), -t.value * 14.dp.toPx() * 4f))
                    val k = (t.value + i * 0.5f) % 1f
                    drawCircle(dot.copy(alpha = sin(k * PI).toFloat()), 4.dp.toPx(), s + (app - s) * k)
                }
            }
            Row(Modifier.align(Alignment.TopStart), verticalAlignment = Alignment.CenterVertically) {
                IconBubble(Icons.Filled.PlayArrow, scheme.errorContainer, scheme.onErrorContainer, size = bubble)
                Spacer(Modifier.width(8.dp))
                Caption("YouTube")
            }
            Row(Modifier.align(Alignment.BottomStart), verticalAlignment = Alignment.CenterVertically) {
                IconBubble(Icons.Filled.MusicNote, scheme.tertiaryContainer, scheme.onTertiaryContainer, size = bubble)
                Spacer(Modifier.width(8.dp))
                Caption("Spotify")
            }
            IconBubble(
                Icons.Filled.GraphicEq,
                scheme.primary,
                scheme.onPrimary,
                size = bubble,
                modifier = Modifier.align(Alignment.CenterEnd).graphicsLayer {
                    val s = 1f + 0.06f * sin(t.value * 2 * PI).toFloat()
                    scaleX = s
                    scaleY = s
                },
            )
        }
    }
}

/** Addon: tap the website, copy the generated link, and it lands in the field. */
@Composable
internal fun AddonDemo() {
    val t = demoClock(5600, still = 0.92f)
    val scheme = MaterialTheme.colorScheme
    val link = "https://addon…/my-link"
    val pasted by remember { derivedStateOf { t.value >= 0.72f && t.value < 0.97f } }
    DemoFrame {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val w = constraints.maxWidth.toFloat()
            val h = constraints.maxHeight.toFloat()
            // The website.
            Row(
                Modifier
                    .align(Alignment.TopStart)
                    .graphicsLayer {
                        val tap = window(t.value, 0.2f, 0.3f)
                        val s = 1f - 0.06f * sin(tap * PI).toFloat()
                        scaleX = s
                        scaleY = s
                    }
                    .clip(CircleShape)
                    .background(scheme.secondaryContainer)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.Language, null, tint = scheme.onSecondaryContainer, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Palv", style = MaterialTheme.typography.labelLarge, color = scheme.onSecondaryContainer)
            }
            // The addon field.
            Row(
                Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .height(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(scheme.surfaceContainerHighest)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.Link, null, tint = scheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    if (pasted) link else "Addon URL",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (pasted) scheme.onSurface else scheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    Icons.Filled.CheckCircle,
                    null,
                    tint = scheme.primary,
                    modifier = Modifier.size(20.dp).graphicsLayer {
                        val k = beat(t.value, 0.74f, 0.82f)
                        alpha = k
                        scaleX = k
                        scaleY = k
                    },
                )
            }
            // The copied link flying from the website down into the field.
            Row(
                Modifier
                    .graphicsLayer {
                        val fly = window(t.value, 0.36f, 0.68f)
                        alpha = if (t.value in 0.3f..0.7f) 1f else 0f
                        translationX = w * 0.32f * (1f - fly)
                        translationY = h * 0.08f + (h * 0.62f) * fly - 18.dp.toPx() * sin(fly * PI).toFloat()
                    }
                    .clip(CircleShape)
                    .background(scheme.primary)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.ContentCopy, null, tint = scheme.onPrimary, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text("Copy link", style = MaterialTheme.typography.labelMedium, color = scheme.onPrimary)
            }
            // The finger that taps the website.
            Icon(
                Icons.Filled.TouchApp,
                null,
                tint = scheme.onSurface,
                modifier = Modifier.size(28.dp).graphicsLayer {
                    val reach = window(t.value, 0f, 0.2f)
                    alpha = 1f - window(t.value, 0.32f, 0.42f)
                    translationX = w * 0.55f + (24.dp.toPx() - w * 0.55f) * reach
                    translationY = h * 0.55f + (20.dp.toPx() - h * 0.55f) * reach
                },
            )
        }
    }
}

/** Audio: a moving waveform beside a Hi-Res badge. */
@Composable
internal fun AudioDemo() {
    val t = demoClock(3000)
    val scheme = MaterialTheme.colorScheme
    val from = scheme.primary
    val to = scheme.tertiary
    DemoFrame {
        Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Canvas(Modifier.weight(1f).fillMaxSize()) {
                val bars = 20
                val gap = 4.dp.toPx()
                val bw = (size.width - (bars - 1) * gap) / bars
                repeat(bars) { i ->
                    val wave = 0.2f + 0.8f * abs(sin(t.value * 2 * PI + i * 0.42).toFloat())
                    val bh = size.height * wave
                    drawRoundRect(lerp(from, to, i / bars.toFloat()), Offset(i * (bw + gap), (size.height - bh) / 2f), Size(bw, bh), CornerRadius(bw / 2f))
                }
            }
            Row(
                Modifier
                    .clip(CircleShape)
                    .background(scheme.primary)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.HighQuality, null, tint = scheme.onPrimary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("HI-RES", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = scheme.onPrimary)
            }
        }
    }
}

/** Permissions: songs come out of the music folder into your list. */
@Composable
internal fun PermissionsDemo() {
    val t = demoClock(5000)
    val scheme = MaterialTheme.colorScheme
    DemoFrame {
        Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            IconBubble(Icons.Filled.Folder, scheme.primaryContainer, scheme.onPrimaryContainer, size = 56.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(3) { i ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .graphicsLayer {
                                val k = beat(t.value, 0.08f + i * 0.14f, 0.3f + i * 0.14f)
                                alpha = k
                                translationX = -(1f - k) * 40.dp.toPx()
                            }
                            .clip(RoundedCornerShape(10.dp))
                            .background(scheme.surfaceContainerHighest)
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Filled.MusicNote, null, tint = scheme.tertiary, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Caption("Song ${i + 1}", scheme.onSurface)
                    }
                }
            }
        }
    }
}

/** Quick-tour gesture demos replaced by the dedicated mini-phone tutorials below. */

@Composable
private fun MiniPhoneTutorialFrame(
    glowColor: Color,
    content: @Composable androidx.compose.foundation.layout.BoxScope.() -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val comic = LocalComicTheme.current
    val outerShape = RoundedCornerShape(30.dp)
    val innerShape = RoundedCornerShape(24.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .width(214.dp)
                .height(310.dp)
                .comicPanel(outerShape, offset = 3.dp)
                .clip(outerShape)
                .background(
                    Brush.radialGradient(
                        listOf(glowColor.copy(alpha = 0.22f), scheme.surfaceContainerHigh),
                    ),
                )
                .border(
                    width = if (comic) 2.dp else 1.2.dp,
                    color = if (comic) ComicInk else scheme.outlineVariant.copy(alpha = 0.45f),
                    shape = outerShape,
                )
                .padding(7.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(innerShape)
                    .background(scheme.surfaceContainerLowest),
            ) {
                content()
                // Compact top speaker / camera island
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 6.dp)
                        .width(46.dp)
                        .height(9.dp)
                        .clip(CircleShape)
                        .background(if (comic) ComicInk else Color.Black.copy(alpha = 0.72f)),
                )
            }
        }
    }
}

@Composable
private fun TouchDotIndicator(
    alpha: Float,
    pressed: Boolean = true,
    modifier: Modifier = Modifier,
) {
    if (alpha <= 0.02f) return
    val accent = MaterialTheme.colorScheme.primary
    Box(
        modifier = modifier
            .size(if (pressed) 30.dp else 24.dp)
            .graphicsLayer { this.alpha = alpha },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(accent.copy(alpha = if (pressed) 0.30f else 0.18f)),
        )
        Box(
            Modifier
                .size(14.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.88f))
                .border(1.5.dp, accent, CircleShape),
        )
    }
}

/**
 * Tutorial #1: Liquid Ripple shockwave burst -> 3D Cover-Flow swipe + floor sheen -> Synced Video toggle.
 */
@Composable
internal fun PlayerGesturesTutorialDemo() {
    val t = demoClock(5200, still = 0.42f)
    val scheme = MaterialTheme.colorScheme
    val density = LocalDensity.current
    val coverColors = listOf(scheme.tertiary, scheme.primary, scheme.secondary)

    MiniPhoneTutorialFrame(glowColor = scheme.primary) {
        val v = t.value
        val rippleP = (v / 0.32f).coerceIn(0f, 1f)
        val swipeFrac = window(v, 0.28f, 0.72f)
        val coverCursor = 1f + sin(swipeFrac * PI).toFloat() * 0.92f
        val videoBadgeOn = v >= 0.74f && v <= 0.95f

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            scheme.primary.copy(alpha = 0.24f),
                            scheme.surfaceContainerLowest,
                        ),
                    ),
                )
                .padding(horizontal = 12.dp, vertical = 22.dp),
        ) {
            // Center 3D Cover-Flow Stack + Liquid Ripple
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 24.dp)
                    .size(134.dp),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    if (rippleP > 0.02f && rippleP < 0.98f) {
                        val expand = 22.dp.toPx() * rippleP
                        val a = (1f - rippleP) * (1f - rippleP) * 0.75f
                        drawRoundRect(
                            color = scheme.primary.copy(alpha = a),
                            topLeft = Offset(-expand, -expand),
                            size = Size(size.width + expand * 2f, size.height + expand * 2f),
                            cornerRadius = CornerRadius(18.dp.toPx() + expand * 0.35f),
                            style = Stroke(width = 2.5.dp.toPx() * (1f - 0.5f * rippleP)),
                        )
                    }
                }
                val order = listOf(0, 1, 2).sortedByDescending { abs(it - coverCursor) }
                order.forEach { idx ->
                    val d = idx - coverCursor
                    if (abs(d) <= 1.25f) {
                        val ad = abs(d)
                        val sc = 1f - 0.24f * ad.coerceAtMost(1f)
                        Box(
                            modifier = Modifier
                                .size(104.dp)
                                .graphicsLayer {
                                    cameraDistance = 12f * this.density
                                    translationX = d * 68.dp.toPx()
                                    translationY = ad * 6.dp.toPx()
                                    rotationY = (-d * 22f).coerceIn(-26f, 26f)
                                    scaleX = sc
                                    scaleY = sc
                                    alpha = 1f - 0.45f * ad.coerceAtMost(1f)
                                }
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(coverColors[idx], coverColors[(idx + 1) % coverColors.size]),
                                    ),
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Filled.MusicNote, null, tint = Color.White.copy(alpha = 0.9f), modifier = Modifier.size(34.dp))
                        }
                    }
                }
            }

            // Soft mirrored floor sheen below cover
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 152.dp)
                    .width(96.dp)
                    .height(14.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(scheme.primary.copy(alpha = 0.26f), Color.Transparent),
                        ),
                    ),
            )

            // Minimal controls + Synced Video chip
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(Modifier.fillMaxWidth().height(3.dp).clip(CircleShape).background(scheme.outlineVariant.copy(alpha = 0.35f))) {
                    Box(Modifier.fillMaxWidth(0.35f + 0.4f * v).height(3.dp).clip(CircleShape).background(scheme.primary))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Filled.SmartDisplay,
                        null,
                        tint = if (videoBadgeOn) scheme.primary else scheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp),
                    )
                    Icon(Icons.AutoMirrored.Filled.QueueMusic, null, tint = scheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                    Icon(Icons.Filled.Lyrics, null, tint = scheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                    Icon(Icons.Filled.CloudDownload, null, tint = scheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                }
                Box(
                    modifier = Modifier
                        .graphicsLayer { alpha = if (videoBadgeOn) 1f else 0.4f }
                        .clip(CircleShape)
                        .background(if (videoBadgeOn) scheme.primaryContainer else scheme.surfaceContainerHighest)
                        .padding(horizontal = 10.dp, vertical = 3.dp),
                ) {
                    Text(
                        text = if (videoBadgeOn) "SYNCED VIDEO ON" else "Swipe cover ↔",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (videoBadgeOn) scheme.onPrimaryContainer else scheme.onSurfaceVariant,
                    )
                }
            }

            // Finger swipe indicator over the artwork
            val fingerAlpha = beat(v, 0.26f, 0.35f) * (1f - window(v, 0.68f, 0.76f))
            TouchDotIndicator(
                alpha = fingerAlpha,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 84.dp)
                    .graphicsLayer { translationX = -cos(swipeFrac * PI).toFloat() * 34.dp.toPx() },
            )
        }
    }
}

/**
 * Tutorial #2: Interactive 7-day Stats bar filter tap + Hold-Home expandable pill shifting MiniPlayer.
 */
@Composable
internal fun DockStatsTutorialDemo() {
    val t = demoClock(5200, still = 0.66f)
    val scheme = MaterialTheme.colorScheme
    val v = t.value
    val satSelected = v >= 0.18f
    val holdPhase = beat(v, 0.44f, 0.56f) * (1f - window(v, 0.86f, 0.96f))
    val slide2nd = window(v, 0.60f, 0.78f)
    val bars = listOf(0.45f, 0.42f, 0.38f, 0.28f, 0.56f, 0.88f, 0.30f)

    MiniPhoneTutorialFrame(glowColor = scheme.tertiary) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(scheme.surfaceContainerLowest)
                .padding(horizontal = 12.dp, vertical = 22.dp),
        ) {
            // Upper stage: 7-day Stats Bar Chart + Filtered Track rows
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = if (satSelected) "Stats · Saturday (18 plays)" else "Stats · 7-Day Activity",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (satSelected) scheme.primary else scheme.onSurface,
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    bars.forEachIndexed { i, frac ->
                        val active = (i == 5 && satSelected)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize()
                                .padding(top = ((1f - frac) * 48).dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    if (active) scheme.primary
                                    else scheme.outlineVariant.copy(alpha = 0.45f),
                                ),
                        )
                    }
                }
                repeat(3) { r ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (satSelected) scheme.primaryContainer.copy(alpha = 0.42f)
                                else scheme.surfaceContainerHigh,
                            )
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Box(Modifier.size(18.dp).clip(RoundedCornerShape(5.dp)).background(scheme.primary.copy(alpha = 0.65f)))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Box(Modifier.fillMaxWidth(0.7f - r * 0.1f).height(5.dp).clip(CircleShape).background(scheme.onSurface.copy(alpha = 0.7f)))
                            Box(Modifier.fillMaxWidth(0.45f).height(4.dp).clip(CircleShape).background(scheme.onSurfaceVariant.copy(alpha = 0.45f)))
                        }
                    }
                }
            }

            // Bottom Expandable Hold-Home Pill + sliding MiniPlayer
            val pillW = 34.dp + (40.dp * holdPhase)
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(bottom = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(
                    modifier = Modifier
                        .width(pillW)
                        .height(34.dp)
                        .clip(CircleShape)
                        .background(scheme.surfaceContainerHighest)
                        .border(1.dp, scheme.primary.copy(alpha = 0.45f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    if (holdPhase < 0.25f) {
                        Icon(Icons.Filled.Home, null, tint = scheme.primary, modifier = Modifier.size(16.dp))
                    } else {
                        Row(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Filled.Home,
                                null,
                                tint = if (slide2nd < 0.5f) scheme.primary else scheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp),
                            )
                            Icon(
                                Icons.Filled.CloudDownload,
                                null,
                                tint = if (slide2nd >= 0.5f) scheme.primary else scheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp),
                            )
                        }
                    }
                }
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .height(34.dp)
                        .clip(CircleShape)
                        .background(scheme.surfaceContainerHigh)
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Box(Modifier.size(20.dp).clip(RoundedCornerShape(6.dp)).background(scheme.primary))
                    Box(Modifier.weight(1f).height(5.dp).clip(CircleShape).background(scheme.onSurface.copy(alpha = 0.65f)))
                    Icon(Icons.Filled.PlayArrow, null, tint = scheme.onSurface, modifier = Modifier.size(15.dp))
                }
            }

            // Finger tapping Sat bar (0.05..0.38) then holding & sliding Home pill (0.42..0.90)
            if (v in 0.04f..0.38f) {
                TouchDotIndicator(
                    alpha = beat(v, 0.04f, 0.14f) * (1f - window(v, 0.30f, 0.38f)),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 54.dp, end = 28.dp),
                )
            } else if (v >= 0.42f) {
                val fa = beat(v, 0.42f, 0.52f) * (1f - window(v, 0.84f, 0.94f))
                TouchDotIndicator(
                    alpha = fa,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 6.dp, bottom = 4.dp)
                        .graphicsLayer { translationX = slide2nd * 34.dp.toPx() },
                )
            }
        }
    }
}

/**
 * Tutorial #3: Playlist long-press Frosted Quick-Peek Card + Queue drag reorder + Jam live pill.
 */
@Composable
internal fun QuickPeekJamTutorialDemo() {
    val t = demoClock(5400, still = 0.45f)
    val scheme = MaterialTheme.colorScheme
    val v = t.value
    val showQuickPeek = v < 0.55f
    val cardPop = beat(v, 0.12f, 0.22f) * (1f - window(v, 0.46f, 0.54f))
    val dragLift = beat(v, 0.60f, 0.68f) * (1f - window(v, 0.88f, 0.96f))
    val dragSwap = window(v, 0.68f, 0.86f)

    MiniPhoneTutorialFrame(glowColor = scheme.secondary) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(scheme.surfaceContainerLowest)
                .padding(horizontal = 12.dp, vertical = 22.dp),
        ) {
            if (showQuickPeek) {
                // Background Library Grid + Quick-Peek Overlay
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Library", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = scheme.onSurface)
                        Box(
                            Modifier
                                .clip(CircleShape)
                                .background(scheme.primary.copy(alpha = 0.18f))
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                Icon(Icons.Filled.People, null, tint = scheme.primary, modifier = Modifier.size(10.dp))
                                Text("Jam", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = scheme.primary)
                            }
                        }
                    }
                    repeat(2) { row ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            repeat(2) { col ->
                                val target = (row == 0 && col == 0)
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(scheme.surfaceContainerHigh)
                                        .padding(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Box(
                                        Modifier
                                            .fillMaxWidth()
                                            .height(54.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (target) scheme.primary else scheme.secondaryContainer),
                                    )
                                    Box(Modifier.fillMaxWidth(0.75f).height(5.dp).clip(CircleShape).background(scheme.onSurface.copy(alpha = 0.6f)))
                                }
                            }
                        }
                    }
                }

                if (cardPop > 0.04f) {
                    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f * cardPop)))
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .fillMaxWidth(0.92f)
                            .graphicsLayer {
                                scaleX = 0.85f + 0.15f * cardPop
                                scaleY = scaleX
                                alpha = cardPop
                            }
                            .clip(RoundedCornerShape(16.dp))
                            .background(scheme.surfaceContainerHigh)
                            .border(1.dp, scheme.primary.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(scheme.primary))
                            Column {
                                Text("Late Night Chill", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = scheme.onSurface)
                                Text("Quick Peek · 5 tracks", fontSize = 8.sp, color = scheme.primary)
                            }
                        }
                        repeat(3) { r ->
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .height(14.dp)
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(scheme.surfaceContainerHighest),
                            )
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(22.dp)
                                .clip(CircleShape)
                                .background(scheme.primary),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("Play All", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = scheme.onPrimary)
                        }
                    }
                }

                if (v < 0.25f) {
                    TouchDotIndicator(
                        alpha = beat(v, 0.04f, 0.14f),
                        modifier = Modifier.align(Alignment.TopStart).padding(start = 32.dp, top = 46.dp),
                    )
                }
            } else {
                // Phase B: Queue drag reorder with ≡ handle + Jam live header
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(CircleShape)
                            .background(scheme.primaryContainer.copy(alpha = 0.6f))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(Icons.Filled.People, null, tint = scheme.primary, modifier = Modifier.size(12.dp))
                        Text("Listen Together Jam · Reorder Queue", fontSize = 8.5.sp, fontWeight = FontWeight.Bold, color = scheme.onPrimaryContainer)
                    }

                    val rowStep = 38.dp
                    Box(Modifier.fillMaxWidth().height(156.dp)) {
                        listOf(0, 1, 2).sortedBy { if (it == 2) 10 else it }.forEach { idx ->
                            val yDp = when (idx) {
                                0 -> rowStep * dragSwap
                                1 -> rowStep * (1f + dragSwap)
                                else -> rowStep * (2f * (1f - dragSwap))
                            }
                            val lifted = idx == 2 && dragLift > 0.05f
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(32.dp)
                                    .offset { IntOffset(0, with(density) { yDp.roundToPx() }) }
                                    .graphicsLayer {
                                        val sc = if (lifted) 1f + 0.05f * dragLift else 1f
                                        scaleX = sc
                                        scaleY = sc
                                    }
                                    .clip(RoundedCornerShape(9.dp))
                                    .background(if (lifted) scheme.primaryContainer else scheme.surfaceContainerHigh)
                                    .padding(horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Box(Modifier.size(18.dp).clip(RoundedCornerShape(5.dp)).background(if (idx == 2) scheme.primary else scheme.tertiary))
                                Box(Modifier.weight(1f).height(6.dp).clip(CircleShape).background(scheme.onSurface.copy(alpha = 0.65f)))
                                Icon(Icons.Filled.DragHandle, null, tint = if (lifted) scheme.primary else scheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                            }
                        }

                        if (dragLift > 0.05f) {
                            val handleY = rowStep * (2f * (1f - dragSwap))
                            TouchDotIndicator(
                                alpha = dragLift,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(end = 2.dp, top = 2.dp)
                                    .offset { IntOffset(0, with(density) { handleY.roundToPx() }) },
                            )
                        }
                    }
                }
            }
        }
    }
}

