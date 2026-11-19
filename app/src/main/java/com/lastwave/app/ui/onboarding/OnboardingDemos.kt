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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import kotlin.math.abs
import kotlin.math.sin

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

/** Quick tour: a finger swipes a song aside, then long-presses it. */
@Composable
internal fun QuickTourDemo() {
    val t = demoClock(5000)
    val scheme = MaterialTheme.colorScheme
    val ring = scheme.primary
    DemoFrame {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        val swipe = window(t.value, 0.05f, 0.28f) - window(t.value, 0.32f, 0.5f)
                        val press = window(t.value, 0.6f, 0.75f) * (1f - window(t.value, 0.88f, 0.98f))
                        translationX = swipe * 64.dp.toPx()
                        scaleX = 1f + 0.04f * press
                        scaleY = scaleX
                    }
                    .clip(RoundedCornerShape(16.dp))
                    .background(scheme.surfaceContainerHighest)
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconBubble(Icons.Filled.MusicNote, scheme.primaryContainer, scheme.onPrimaryContainer, size = 40.dp)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Song title", style = MaterialTheme.typography.labelLarge, color = scheme.onSurface)
                    Caption("Artist")
                }
            }
            Canvas(Modifier.fillMaxSize()) {
                val press = window(t.value, 0.6f, 0.75f) * (1f - window(t.value, 0.88f, 0.98f))
                if (press > 0f) drawCircle(ring.copy(alpha = 0.25f * press), 28.dp.toPx() * press, center)
            }
            Icon(
                Icons.Filled.TouchApp,
                null,
                tint = scheme.onSurface,
                modifier = Modifier.size(30.dp).offset(x = 6.dp, y = 18.dp).graphicsLayer {
                    val swipe = window(t.value, 0.05f, 0.28f) - window(t.value, 0.32f, 0.5f)
                    translationX = swipe * 64.dp.toPx()
                },
            )
        }
    }
}
