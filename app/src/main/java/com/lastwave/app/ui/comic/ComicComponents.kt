@file:Suppress("FunctionName")

package com.lastwave.app.ui.comic

import com.lastwave.app.ui.theme.LocalComicTheme
import com.lastwave.app.ui.theme.LocalIsDarkTheme
import com.lastwave.app.ui.theme.LocalSakugaFx
import com.lastwave.app.ui.theme.LocalSakugaPalette
import com.lastwave.app.ui.theme.LocalSakugaTheme
import com.lastwave.app.ui.theme.SakugaDefaultLightPalette
import android.os.SystemClock
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import kotlin.math.sqrt
import kotlinx.coroutines.launch
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.IntOffset
import androidx.compose.material3.Slider as M3Slider
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sign
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonElevation
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardElevation
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.lastwave.app.ui.theme.comicInk
import com.lastwave.app.ui.theme.comicPanel
import com.lastwave.app.ui.theme.comicPress
import androidx.compose.ui.graphics.graphicsLayer
import com.lastwave.app.ui.theme.glassCardBorder
import com.lastwave.app.ui.theme.glassCardColor
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.CheckboxColors
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ChipColors
import androidx.compose.material3.ChipElevation
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.MenuItemColors
import androidx.compose.material3.RadioButtonColors
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.SelectableChipColors
import androidx.compose.material3.SelectableChipElevation
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import com.lastwave.app.ui.theme.comicOutline
import androidx.compose.material3.AssistChip as M3AssistChip
import androidx.compose.material3.Checkbox as M3Checkbox
import androidx.compose.material3.DropdownMenuItem as M3DropdownMenuItem
import androidx.compose.material3.FilledIconButton as M3FilledIconButton
import androidx.compose.material3.FilterChip as M3FilterChip
import androidx.compose.material3.RadioButton as M3RadioButton
import androidx.compose.material3.Switch as M3Switch
import androidx.compose.material3.TextButton as M3TextButton
import androidx.compose.ui.draw.clip as uiClip
import androidx.compose.material3.AlertDialog as M3AlertDialog
import androidx.compose.material3.Button as M3Button
import androidx.compose.material3.Card as M3Card
import androidx.compose.material3.FilledTonalButton as M3FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton as M3FilledTonalIconButton
import androidx.compose.material3.IconButton as M3IconButton
import androidx.compose.material3.OutlinedButton as M3OutlinedButton
import androidx.compose.material3.Surface as M3Surface

/*
 * Drop-in stand-ins for the Material 3 components the app is built from.
 * Screens import these instead of the Material ones, so the Comic Pop theme
 * reaches every card, button and surface from one place. With any other theme
 * each of them is exactly the Material component it wraps.
 */

/** The app's faint hairline borders give way to the ink outline under Comic Pop. */
@Composable
private fun BorderStroke?.unlessComic(): BorderStroke? = if (LocalComicTheme.current) null else this

/** Under Comic Pop, ink frames replace blurred Material elevation shadows (which allocate Vulkan stencils). */
@Composable
private fun Dp.unlessComicElevation(): Dp = if (LocalComicTheme.current) 0.dp else this

/** A see-through card is a layout wrapper, not a panel: nothing to ink. */
@Composable
private fun Modifier.comicCard(shape: Shape, colors: CardColors): Modifier =
    if (colors.containerColor.alpha < 0.05f) this else comicPanel(shape, translucent = colors.containerColor.alpha < 1f)

/**
 * Buttons sit on a shorter shadow and slide onto it while held. An outlined or
 * text button has no fill, so its shadow is cut out rather than showing through
 * as a pink slab under the label.
 */
@Composable
private fun Modifier.comicButton(shape: Shape, source: MutableInteractionSource, enabled: Boolean, colors: ButtonColors): Modifier {
    if (!enabled) return this
    val held by source.collectIsPressedAsState()
    val isSakuga = LocalSakugaTheme.current
    val pushed by animateFloatAsState(if (held) 1f else 0f, spring(stiffness = 900f), label = "comicButtonPush")
    val sy by animateFloatAsState(
        targetValue = if (isSakuga && held) 0.80f else 1f,
        animationSpec = if (held) spring(stiffness = 1100f) else spring(dampingRatio = 0.36f, stiffness = 520f),
        label = "sakugaBtnSquash",
    )
    val sx = if (isSakuga) 1f / sqrt(sy.coerceIn(0.65f, 1.35f)) else 1f
    return comicPress(source)
        .comicPanel(shape, offset = 3.dp, translucent = colors.containerColor.alpha < 1f, pressed = { pushed })
        .let { m ->
            if (isSakuga) m.graphicsLayer { scaleX = sx; scaleY = sy } else m
        }
}

@Composable
private fun CardElevation.unlessComicCardElevation(): CardElevation =
    if (LocalComicTheme.current) CardDefaults.cardElevation(0.dp, 0.dp, 0.dp, 0.dp, 0.dp, 0.dp) else this

/** Every card is glass when Liquid Glass is on, so no screen is left with opaque boxes:
 *  a see-through fill and a lit rim over the page wash or background image. A card
 *  with no fill stays invisible. No-op with glass off (Comic Pop included). */
@Composable
private fun CardColors.glass(): CardColors =
    copy(containerColor = glassCardColor(containerColor), disabledContainerColor = glassCardColor(disabledContainerColor))

@Composable
private fun glassBorder(colors: CardColors, border: BorderStroke?): BorderStroke? =
    border ?: if (colors.containerColor.alpha < 0.05f) null else glassCardBorder()

@Composable
fun Card(
    modifier: Modifier = Modifier,
    shape: Shape = CardDefaults.shape,
    colors: CardColors = CardDefaults.cardColors(),
    elevation: CardElevation = CardDefaults.cardElevation(),
    border: BorderStroke? = null,
    content: @Composable ColumnScope.() -> Unit,
) = M3Card(modifier.comicCard(shape, colors), shape, colors.glass(), elevation.unlessComicCardElevation(), glassBorder(colors, border), content)

@Composable
fun Card(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = CardDefaults.shape,
    colors: CardColors = CardDefaults.cardColors(),
    elevation: CardElevation = CardDefaults.cardElevation(),
    border: BorderStroke? = null,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    M3Card(onClick, modifier.comicPress(source).comicCard(shape, colors), enabled, shape, colors.glass(), elevation.unlessComicCardElevation(), glassBorder(colors, border), source, content)
}

@Composable
fun Button(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.shape,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    elevation: ButtonElevation? = ButtonDefaults.buttonElevation(),
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit,
) {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val isSakuga = LocalSakugaTheme.current
    val fx = LocalSakugaFx.current
    var bounds by remember { mutableStateOf<Rect?>(null) }
    var invertFlash by remember { mutableStateOf(false) }
    val held by source.collectIsPressedAsState()
    LaunchedEffect(held, isSakuga) {
        if (isSakuga && held) {
            bounds?.let { b -> fx?.emitButtonPress(b.center, b.width, b.height) }
        }
    }
    LaunchedEffect(invertFlash) {
        if (invertFlash) {
            delay(95L)
            invertFlash = false
        }
    }
    val effectiveColors = if (isSakuga && invertFlash) {
        ButtonDefaults.buttonColors(
            containerColor = colors.contentColor,
            contentColor = colors.containerColor,
        )
    } else colors
    val tilt by animateFloatAsState(
        targetValue = if (isSakuga && invertFlash) -3.2f else 0f,
        animationSpec = spring(dampingRatio = 0.45f, stiffness = 680f),
        label = "sakugaBtnTilt",
    )
    M3Button(
        onClick = {
            if (isSakuga) {
                invertFlash = true
                bounds?.let { b -> fx?.emitPrimaryImpact(b.center, b.width, b.height) }
            }
            onClick()
        },
        modifier = modifier
            .onGloballyPositioned { if (it.isAttached) bounds = it.boundsInRoot() }
            .then(if (isSakuga) Modifier.graphicsLayer { rotationZ = tilt } else Modifier)
            .comicButton(shape, source, enabled, colors),
        enabled = enabled, shape = shape, colors = effectiveColors, elevation = elevation, border = border,
        contentPadding = contentPadding, interactionSource = source, content = content,
    )
}

@Composable
fun FilledTonalButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.filledTonalShape,
    colors: ButtonColors = ButtonDefaults.filledTonalButtonColors(),
    elevation: ButtonElevation? = ButtonDefaults.filledTonalButtonElevation(),
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit,
) {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val isSakuga = LocalSakugaTheme.current
    val fx = LocalSakugaFx.current
    var bounds by remember { mutableStateOf<Rect?>(null) }
    M3FilledTonalButton(
        onClick = {
            if (isSakuga) bounds?.let { b -> fx?.emitPrimaryImpact(b.center, b.width, b.height, withDonSfx = false) }
            onClick()
        },
        modifier = modifier
            .onGloballyPositioned { if (it.isAttached) bounds = it.boundsInRoot() }
            .comicButton(shape, source, enabled, colors),
        enabled = enabled, shape = shape, colors = colors, elevation = elevation, border = border,
        contentPadding = contentPadding, interactionSource = source, content = content,
    )
}

@Composable
fun OutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.outlinedShape,
    colors: ButtonColors = ButtonDefaults.outlinedButtonColors(),
    elevation: ButtonElevation? = null,
    border: BorderStroke? = ButtonDefaults.outlinedButtonBorder(enabled),
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit,
) {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val isSakuga = LocalSakugaTheme.current
    val fx = LocalSakugaFx.current
    var bounds by remember { mutableStateOf<Rect?>(null) }
    M3OutlinedButton(
        onClick = {
            if (isSakuga) bounds?.let { b -> fx?.emitSecondaryTap(b.topRight) }
            onClick()
        },
        modifier = modifier
            .onGloballyPositioned { if (it.isAttached) bounds = it.boundsInRoot() }
            .comicButton(shape, source, enabled, colors),
        enabled = enabled, shape = shape, colors = colors, elevation = elevation, border = border,
        contentPadding = contentPadding, interactionSource = source, content = content,
    )
}

/**
 * The icon inside a pressed button shrinks while held and springs back past its size
 * on release, so every icon button in the app answers a tap the same way. The icon
 * moves, not the button, so its touch target and outline stay put.
 */
@Composable
private fun PressBounce(source: MutableInteractionSource, content: @Composable () -> Unit) {
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (pressed) 0.82f else 1f,
        spring(dampingRatio = if (pressed) 0.8f else 0.4f, stiffness = 600f),
        label = "iconPressBounce",
    )
    androidx.compose.foundation.layout.Box(
        Modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
        },
        contentAlignment = androidx.compose.ui.Alignment.Center,
    ) { content() }
}

@Composable
fun IconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: IconButtonColors = IconButtonDefaults.iconButtonColors(),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit,
) {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val isSakuga = LocalSakugaTheme.current
    val sakugaFx = LocalSakugaFx.current
    val density = LocalDensity.current
    var btnBounds by remember { mutableStateOf<Rect?>(null) }
    M3IconButton(
        onClick = {
            if (isSakuga) {
                btnBounds?.let { b ->
                    val pad = with(density) { 4.dp.toPx() }
                    sakugaFx?.emitSecondaryTap(Offset(b.right - pad, b.top + pad))
                }
            }
            onClick()
        },
        modifier = modifier
            .let { m -> if (isSakuga) m.onGloballyPositioned { if (it.isAttached) btnBounds = it.boundsInRoot() } else m }
            .comicPress(source),
        enabled = enabled,
        colors = colors,
        interactionSource = source,
        content = { PressBounce(source, content) },
    )
}

@Composable
fun FilledTonalIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = IconButtonDefaults.filledShape,
    colors: IconButtonColors = IconButtonDefaults.filledTonalIconButtonColors(),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit,
) {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val isSakuga = LocalSakugaTheme.current
    val sakugaFx = LocalSakugaFx.current
    val density = LocalDensity.current
    var btnBounds by remember { mutableStateOf<Rect?>(null) }
    M3FilledTonalIconButton(
        onClick = {
            if (isSakuga) {
                btnBounds?.let { b ->
                    val pad = with(density) { 4.dp.toPx() }
                    sakugaFx?.emitSecondaryTap(Offset(b.right - pad, b.top + pad))
                }
            }
            onClick()
        },
        modifier = modifier
            .let { m -> if (isSakuga) m.onGloballyPositioned { if (it.isAttached) btnBounds = it.boundsInRoot() } else m }
            .comicPress(source)
            .comicPanel(shape, offset = 2.dp),
        enabled = enabled,
        shape = shape,
        colors = colors,
        interactionSource = source,
    ) {
        PressBounce(source, content)
    }
}

@Composable
fun Surface(
    modifier: Modifier = Modifier,
    shape: Shape = RectangleShape,
    color: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = contentColorFor(color),
    tonalElevation: Dp = 0.dp,
    shadowElevation: Dp = 0.dp,
    border: BorderStroke? = null,
    content: @Composable () -> Unit,
) = M3Surface(
    modifier.comicInk(shape, color),
    shape, color, contentColor, tonalElevation, shadowElevation.unlessComicElevation(), border.unlessComic(), content,
)

@Composable
fun Surface(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = RectangleShape,
    color: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = contentColorFor(color),
    tonalElevation: Dp = 0.dp,
    shadowElevation: Dp = 0.dp,
    border: BorderStroke? = null,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit,
) {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    M3Surface(
        onClick,
        modifier.comicInk(shape, color).comicPress(source),
        enabled, shape, color, contentColor, tonalElevation, shadowElevation.unlessComicElevation(), border.unlessComic(), source, content,
    )
}

@Composable
fun Surface(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = RectangleShape,
    color: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = contentColorFor(color),
    tonalElevation: Dp = 0.dp,
    shadowElevation: Dp = 0.dp,
    border: BorderStroke? = null,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit,
) {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    M3Surface(
        selected, onClick,
        modifier.comicInk(shape, color).comicPress(source),
        enabled, shape, color, contentColor, tonalElevation, shadowElevation.unlessComicElevation(), border.unlessComic(), source, content,
    )
}

@Composable
fun Surface(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = RectangleShape,
    color: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = contentColorFor(color),
    tonalElevation: Dp = 0.dp,
    shadowElevation: Dp = 0.dp,
    border: BorderStroke? = null,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit,
) {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    M3Surface(
        checked, onCheckedChange,
        modifier.comicInk(shape, color).comicPress(source),
        enabled, shape, color, contentColor, tonalElevation, shadowElevation.unlessComicElevation(), border.unlessComic(), source, content,
    )
}

/** Dialogs get the outline only: a dialog window clips anything drawn outside it. */
@Composable
fun AlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: @Composable (() -> Unit)? = null,
    icon: @Composable (() -> Unit)? = null,
    title: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
    shape: Shape = AlertDialogDefaults.shape,
    containerColor: Color = AlertDialogDefaults.containerColor,
    iconContentColor: Color = AlertDialogDefaults.iconContentColor,
    titleContentColor: Color = AlertDialogDefaults.titleContentColor,
    textContentColor: Color = AlertDialogDefaults.textContentColor,
    tonalElevation: Dp = AlertDialogDefaults.TonalElevation,
    properties: DialogProperties = DialogProperties(),
) = M3AlertDialog(
    onDismissRequest, confirmButton, modifier.comicPanel(shape, offset = 0.dp), dismissButton, icon, title, text,
    shape, containerColor, iconContentColor, titleContentColor, textContentColor, tonalElevation, properties,
)

/** Plain clip, for a shape that only bounds a press ripple and encloses nothing drawn. */
fun Modifier.clipOnly(shape: Shape): Modifier = uiClip(shape)

/** `Modifier.clip` that also inks the clipped edge under Comic Pop; otherwise the plain clip. */
fun Modifier.clip(shape: Shape): Modifier = comicOutline(shape).uiClip(shape)

@Composable
fun TextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.textShape,
    colors: ButtonColors = ButtonDefaults.textButtonColors(),
    elevation: ButtonElevation? = null,
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.TextButtonContentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit,
) {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val isSakuga = LocalSakugaTheme.current
    val sakugaFx = LocalSakugaFx.current
    val density = LocalDensity.current
    var btnBounds by remember { mutableStateOf<Rect?>(null) }
    M3TextButton(
        onClick = {
            if (isSakuga) {
                btnBounds?.let { b ->
                    val pad = with(density) { 4.dp.toPx() }
                    sakugaFx?.emitSecondaryTap(Offset(b.right - pad, b.top + pad))
                }
            }
            onClick()
        },
        modifier = modifier
            .let { m -> if (isSakuga) m.onGloballyPositioned { if (it.isAttached) btnBounds = it.boundsInRoot() } else m }
            .comicPress(source),
        enabled = enabled,
        shape = shape,
        colors = colors,
        elevation = elevation,
        border = border,
        contentPadding = contentPadding,
        interactionSource = source,
        content = content,
    )
}

@Composable
fun FilledIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = IconButtonDefaults.filledShape,
    colors: IconButtonColors = IconButtonDefaults.filledIconButtonColors(),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit,
) {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val isSakuga = LocalSakugaTheme.current
    val sakugaFx = LocalSakugaFx.current
    val density = LocalDensity.current
    var btnBounds by remember { mutableStateOf<Rect?>(null) }
    M3FilledIconButton(
        onClick = {
            if (isSakuga) {
                btnBounds?.let { b ->
                    val pad = with(density) { 4.dp.toPx() }
                    sakugaFx?.emitSecondaryTap(Offset(b.right - pad, b.top + pad))
                }
            }
            onClick()
        },
        modifier = modifier
            .let { m -> if (isSakuga) m.onGloballyPositioned { if (it.isAttached) btnBounds = it.boundsInRoot() } else m }
            .comicPress(source)
            .comicPanel(shape, offset = 2.dp),
        enabled = enabled,
        shape = shape,
        colors = colors,
        interactionSource = source,
        content = { PressBounce(source, content) },
    )
}

@Composable
fun FilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    shape: Shape = FilterChipDefaults.shape,
    colors: SelectableChipColors = FilterChipDefaults.filterChipColors(),
    elevation: SelectableChipElevation? = FilterChipDefaults.filterChipElevation(),
    border: BorderStroke? = FilterChipDefaults.filterChipBorder(enabled, selected),
    interactionSource: MutableInteractionSource? = null,
) {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val isSakuga = LocalSakugaTheme.current
    val sakugaFx = LocalSakugaFx.current
    val density = LocalDensity.current
    var chipBounds by remember { mutableStateOf<Rect?>(null) }
    M3FilterChip(
        selected = selected,
        onClick = {
            if (isSakuga) {
                chipBounds?.let { b ->
                    val pad = with(density) { 4.dp.toPx() }
                    sakugaFx?.emitChipTap(b.center, Offset(b.right - pad, b.top + pad))
                }
            }
            onClick()
        },
        label = label,
        modifier = modifier
            .let { m -> if (isSakuga) m.onGloballyPositioned { if (it.isAttached) chipBounds = it.boundsInRoot() } else m }
            .comicPress(source)
            .comicPanel(
                shape, offset = 2.dp, stroke = 2.dp,
                // An unselected chip is see-through; without the cut-out its shadow fills it pink at night.
                translucent = !selected,
            ),
        enabled = enabled, leadingIcon = leadingIcon, trailingIcon = trailingIcon, shape = shape, colors = colors,
        elevation = elevation, border = border, interactionSource = source,
    )
}

@Composable
fun AssistChip(
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    shape: Shape = AssistChipDefaults.shape,
    colors: ChipColors = AssistChipDefaults.assistChipColors(),
    elevation: ChipElevation? = AssistChipDefaults.assistChipElevation(),
    border: BorderStroke? = AssistChipDefaults.assistChipBorder(enabled),
    interactionSource: MutableInteractionSource? = null,
) {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val isSakuga = LocalSakugaTheme.current
    val sakugaFx = LocalSakugaFx.current
    val density = LocalDensity.current
    var chipBounds by remember { mutableStateOf<Rect?>(null) }
    M3AssistChip(
        onClick = {
            if (isSakuga) {
                chipBounds?.let { b ->
                    val pad = with(density) { 4.dp.toPx() }
                    sakugaFx?.emitChipTap(b.center, Offset(b.right - pad, b.top + pad))
                }
            }
            onClick()
        },
        label = label,
        modifier = modifier
            .let { m -> if (isSakuga) m.onGloballyPositioned { if (it.isAttached) chipBounds = it.boundsInRoot() } else m }
            .comicPress(source)
            .comicPanel(shape, offset = 2.dp, stroke = 2.dp, translucent = colors.containerColor.alpha < 1f),
        enabled = enabled, leadingIcon = leadingIcon, trailingIcon = trailingIcon, shape = shape, colors = colors,
        elevation = elevation, border = border, interactionSource = source,
    )
}

@Composable
fun Switch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    thumbContent: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
    colors: SwitchColors = SwitchDefaults.colors(),
    interactionSource: MutableInteractionSource? = null,
) {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    when {
        LocalSakugaTheme.current -> SakugaSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = modifier,
            thumbContent = thumbContent,
            enabled = enabled,
            interactionSource = source,
        )
        LocalComicTheme.current -> M3Switch(
            checked = checked, onCheckedChange = onCheckedChange, modifier = modifier.comicPress(source),
            thumbContent = thumbContent, enabled = enabled, colors = colors, interactionSource = source,
        )
        else -> LiquidGlassSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = modifier,
            thumbContent = thumbContent,
            enabled = enabled,
            interactionSource = source,
        )
    }
}

@Composable
private fun SakugaSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    thumbContent: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource,
) {
    val pal = LocalSakugaPalette.current
    val fx = LocalSakugaFx.current
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val trackColor by animateColorAsState(
        targetValue = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
        animationSpec = spring(stiffness = 780f, dampingRatio = 0.85f),
        label = "sakugaSwitchBg",
    )
    val pos by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.56f, stiffness = 580f),
        label = "sakugaSwitchPos",
    )
    val smear = remember { Animatable(0f) }
    var initialized by remember { mutableStateOf(false) }
    LaunchedEffect(checked) {
        if (!initialized) {
            initialized = true
            return@LaunchedEffect
        }
        smear.snapTo(if (checked) 1f else -1f)
        smear.animateTo(0f, spring(dampingRatio = 0.48f, stiffness = 520f))
    }
    var bounds by remember { mutableStateOf<Rect?>(null) }

    Box(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .onGloballyPositioned { if (it.isAttached) bounds = it.boundsInRoot() }
            .alpha(if (enabled) 1f else 0.45f)
            .size(width = 54.dp, height = 30.dp)
            .semantics {
                role = Role.Switch
                toggleableState = if (checked) ToggleableState.On else ToggleableState.Off
                if (enabled && onCheckedChange != null) {
                    onClick { onCheckedChange(!checked); true }
                }
            }
            .then(
                if (enabled && onCheckedChange != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = {
                            val next = !checked
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            bounds?.let { b ->
                                val targetFrac = if (next) 1f else 0f
                                val thumbCenterX = b.left + b.height / 2f + targetFrac * (b.width - b.height)
                                fx?.emitToggleFlip(Offset(thumbCenterX, b.center.y), next)
                            }
                            scope.launch {
                                smear.snapTo(if (next) 1f else -1f)
                                smear.animateTo(0f, spring(dampingRatio = 0.48f, stiffness = 520f))
                            }
                            onCheckedChange(next)
                        },
                    )
                } else Modifier,
            ),
        contentAlignment = Alignment.CenterStart,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val h = size.height
            val w = size.width
            val r = CornerRadius(h / 2f, h / 2f)
            val off = 2.dp.toPx()
            val strokePx = 2.dp.toPx()
            drawRoundRect(
                color = pal.shadow,
                topLeft = Offset(off, off),
                size = Size(w, h),
                cornerRadius = r,
            )
            drawRoundRect(
                color = trackColor,
                size = Size(w, h),
                cornerRadius = r,
            )
            drawRoundRect(
                color = pal.ink,
                size = Size(w, h),
                cornerRadius = r,
                style = Stroke(strokePx),
            )
            val pad = 4.dp.toPx()
            val knobSize = h - pad * 2f
            val travel = (w - pad * 2f - knobSize).coerceAtLeast(0f)
            val xLeft = pad + pos.coerceIn(0f, 1f) * travel
            val stretch = 7.dp.toPx() * abs(smear.value)
            val pillLeft = (xLeft - if (smear.value > 0f) stretch else 0f).coerceAtLeast(pad)
            val pillW = (knobSize + stretch).coerceAtMost(w - pad * 2f)
            drawRoundRect(
                color = pal.shadow.copy(alpha = 0.5f),
                topLeft = Offset(pillLeft + 1.dp.toPx(), pad + 1.dp.toPx()),
                size = Size(pillW, knobSize),
                cornerRadius = CornerRadius(knobSize / 2f, knobSize / 2f),
            )
            drawRoundRect(
                color = Color.White,
                topLeft = Offset(pillLeft, pad),
                size = Size(pillW, knobSize),
                cornerRadius = CornerRadius(knobSize / 2f, knobSize / 2f),
            )
            drawRoundRect(
                color = pal.ink,
                topLeft = Offset(pillLeft, pad),
                size = Size(pillW, knobSize),
                cornerRadius = CornerRadius(knobSize / 2f, knobSize / 2f),
                style = Stroke(strokePx),
            )
        }
        if (thumbContent != null) {
            Box(
                modifier = Modifier
                    .offset {
                        val hPx = 30.dp.toPx()
                        val pad = 4.dp.toPx()
                        val knobSize = hPx - pad * 2f
                        val travel = (54.dp.toPx() - pad * 2f - knobSize).coerceAtLeast(0f)
                        IntOffset((pad + pos.coerceIn(0f, 1f) * travel).roundToInt(), 0)
                    }
                    .size(22.dp),
                contentAlignment = Alignment.Center,
            ) {
                thumbContent()
            }
        }
    }
}

/**
 * iOS 26-inspired Liquid Glass toggle switch for normal Light / Dark / AMOLED themes.
 * Supports both tap and horizontal drag with rubber-banding: touching lifts the knob
 * beyond the track into a clear glass lens, and releasing settles it back into a white
 * pill with a volume-preserving jelly wobble.
 */
@Composable
private fun LiquidGlassSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    thumbContent: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource,
) {
    val isDark = LocalIsDarkTheme.current
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current

    val trackWidth = 56.dp
    val trackHeight = 32.dp
    val inset = 3.dp
    val thumbRestWidth = 28.dp
    val thumbHeight = trackHeight - inset * 2
    val travelPx = with(density) { (trackWidth - inset * 2 - thumbRestWidth).toPx() }

    val latestChecked by rememberUpdatedState(checked)
    val latestOnChange by rememberUpdatedState(onCheckedChange)
    val latestEnabled by rememberUpdatedState(enabled)

    var isPressed by remember { mutableStateOf(false) }
    var isDragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(if (checked) 1f else 0f) }
    var rubberOvershoot by remember { mutableFloatStateOf(0f) }
    var isMorphing by remember { mutableStateOf(false) }
    var initialized by remember { mutableStateOf(false) }

    LaunchedEffect(checked) {
        if (!initialized) {
            initialized = true
            dragFraction = if (checked) 1f else 0f
            return@LaunchedEffect
        }
        if (!isDragging) {
            dragFraction = if (checked) 1f else 0f
            isMorphing = true
            delay(180L)
            isMorphing = false
        }
    }

    val lifted = (isPressed || isDragging || isMorphing) && enabled
    val targetFraction = if (isDragging) dragFraction else if (checked) 1f else 0f

    val animatedFraction by animateFloatAsState(
        targetValue = targetFraction,
        animationSpec = if (isDragging) {
            spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 2400f)
        } else {
            spring(dampingRatio = 0.68f, stiffness = 520f)
        },
        label = "liquidSwitchFraction",
    )
    // Lift inflates the knob beyond the track edges into a clear refracting lens;
    // release settles back with a short jelly wobble.
    val liftProgress by animateFloatAsState(
        targetValue = if (lifted) 1f else 0f,
        animationSpec = if (lifted) {
            spring(dampingRatio = 0.74f, stiffness = 780f)
        } else {
            spring(dampingRatio = 0.54f, stiffness = 520f)
        },
        label = "liquidSwitchLift",
    )
    // Glass clarity follows lift up, then falls cleanly without flashing back on bounce.
    val glassClarity by animateFloatAsState(
        targetValue = if (lifted) 1f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 680f),
        label = "liquidSwitchGlass",
    )
    val rubberAnimated by animateFloatAsState(
        targetValue = if (isDragging) rubberOvershoot else 0f,
        animationSpec = spring(dampingRatio = 0.62f, stiffness = 540f),
        label = "liquidSwitchRubber",
    )

    val offTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = if (isDark) 0.14f else 0.12f)
    val onTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = if (isDark) 0.88f else 0.94f)
    val trackColor = lerp(offTrackColor, onTrackColor, animatedFraction.coerceIn(0f, 1f))
    val isVisualOn = animatedFraction >= 0.5f

    val rimTopColor = Color.White.copy(alpha = when {
        isVisualOn && isDark -> 0.44f
        isVisualOn -> 0.64f
        isDark -> 0.22f
        else -> 0.55f
    })
    val rimBottomColor = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.10f)

    val gestureModifier = Modifier
        .semantics(mergeDescendants = true) {
            role = Role.Switch
            toggleableState = ToggleableState(checked)
            if (enabled && onCheckedChange != null) {
                onClick {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onCheckedChange(!checked)
                    true
                }
            }
        }
        .pointerInput(travelPx) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                if (!latestEnabled || latestOnChange == null) return@awaitEachGesture
                isPressed = true
                var dragged = false
                var totalDx = 0f
                val startFraction = if (latestChecked) 1f else 0f
                dragFraction = startFraction
                rubberOvershoot = 0f
                var lastCrossedOn = latestChecked

                while (true) {
                    val event = awaitPointerEvent()
                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                    if (!change.pressed) break
                    val dx = change.positionChange().x
                    if (dx != 0f) {
                        totalDx += dx
                        if (!dragged && abs(totalDx) > viewConfiguration.touchSlop * 0.45f) {
                            dragged = true
                            isDragging = true
                        }
                        if (dragged && travelPx > 1f) {
                            change.consume()
                            val raw = startFraction + totalDx / travelPx
                            dragFraction = raw.coerceIn(0f, 1f)
                            val over = when {
                                raw < 0f -> raw
                                raw > 1f -> raw - 1f
                                else -> 0f
                            }
                            rubberOvershoot = sign(over) * (abs(over) / (1f + abs(over) * 2.2f)) * 0.18f
                            val nowOn = dragFraction >= 0.5f
                            if (nowOn != lastCrossedOn) {
                                lastCrossedOn = nowOn
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                        }
                    }
                }

                val targetNext = if (dragged) dragFraction >= 0.5f else !latestChecked
                isPressed = false
                isDragging = false
                rubberOvershoot = 0f
                if (targetNext != latestChecked) {
                    if (!dragged) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    latestOnChange?.invoke(targetNext)
                }
            }
        }

    // Outer container does NOT clip so the lifted glass lens can visibly inflate past the track rim.
    Box(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .alpha(if (enabled) 1f else 0.42f)
            .then(gestureModifier),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier.size(width = trackWidth, height = trackHeight),
            contentAlignment = Alignment.CenterStart,
        ) {
            // Translucent liquid glass track with specular rim + subtle inner gloss
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val stretchX = 1f + abs(rubberAnimated) * 0.35f
                        val stretchY = 1f - abs(rubberAnimated) * 0.12f
                        scaleX = stretchX
                        scaleY = stretchY
                        translationX = rubberAnimated * 16f
                    }
                    .uiClip(CircleShape)
                    .background(trackColor)
                    .background(
                        Brush.verticalGradient(
                            0.0f to Color.White.copy(alpha = if (isVisualOn) 0.24f else if (isDark) 0.10f else 0.22f),
                            0.55f to Color.Transparent,
                            1.0f to Color.Black.copy(alpha = if (isDark) 0.16f else 0.06f),
                        ),
                    )
                    .border(
                        width = 1.dp,
                        brush = Brush.verticalGradient(listOf(rimTopColor, rimBottomColor)),
                        shape = CircleShape,
                    ),
            )

            // Lifted iOS 26 Liquid Glass lens knob (drawn as sibling so it expands cleanly past track bounds)
            val safeLift = liftProgress.coerceAtLeast(-0.18f)
            val lensStretch = abs(rubberAnimated) * 0.45f
            val knobScaleX = 1f + safeLift * 0.26f + lensStretch
            val knobScaleY = 1f + safeLift * 0.20f - lensStretch * 0.35f
            val dropletAlpha = (0.97f - glassClarity.coerceIn(0f, 1f) * 0.72f).coerceIn(0.20f, 0.98f)
            val borderAlpha = 0.76f + glassClarity.coerceIn(0f, 1f) * 0.22f
            val baseOffsetPx = with(density) { inset.toPx() } +
                animatedFraction.coerceIn(0f, 1f) * travelPx +
                rubberAnimated * 22f

            Box(
                modifier = Modifier
                    .offset { IntOffset(baseOffsetPx.roundToInt(), 0) }
                    .size(width = thumbRestWidth, height = thumbHeight)
                    .graphicsLayer {
                        scaleX = knobScaleX
                        scaleY = knobScaleY
                    }
                    .shadow(
                        elevation = (3f + safeLift.coerceAtLeast(0f) * 5f).dp,
                        shape = CircleShape,
                        ambientColor = Color.Black.copy(alpha = 0.28f),
                        spotColor = Color.Black.copy(alpha = 0.32f),
                    )
                    .uiClip(CircleShape)
                    // Subtle refracted track tint glowing inside the lifted clear lens
                    .background(trackColor.copy(alpha = glassClarity.coerceIn(0f, 1f) * 0.38f))
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = (dropletAlpha + 0.16f).coerceAtMost(1f)),
                                Color.White.copy(alpha = dropletAlpha),
                                Color.White.copy(alpha = (dropletAlpha * 0.65f).coerceAtLeast(0.14f)),
                            ),
                            center = Offset(16f, 10f),
                            radius = 58f,
                        ),
                    )
                    .border(
                        width = (0.9f + glassClarity.coerceIn(0f, 1f) * 0.6f).dp,
                        brush = Brush.verticalGradient(
                            listOf(
                                Color.White.copy(alpha = borderAlpha),
                                Color.White.copy(alpha = borderAlpha * 0.32f),
                                Color.White.copy(alpha = borderAlpha * 0.65f),
                            ),
                        ),
                        shape = CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                thumbContent?.invoke()
            }
        }
    }
}

/**
 * Theme-aware Slider: Comic Pop keeps M3 Slider, while normal Light / Dark / AMOLED themes
 * render an iOS 26-inspired Liquid Glass Slider whose thumb inflates into a clear glass lens
 * while dragging or tapping.
 */
@Composable
fun Slider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null,
    colors: SliderColors = SliderDefaults.colors(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    if (LocalSakugaTheme.current) {
        SakugaSlider(
            value = value,
            onValueChange = onValueChange,
            modifier = modifier,
            enabled = enabled,
            valueRange = valueRange,
            steps = steps,
            onValueChangeFinished = onValueChangeFinished,
        )
        return
    }
    if (LocalComicTheme.current) {
        M3Slider(
            value = value,
            onValueChange = onValueChange,
            modifier = modifier,
            enabled = enabled,
            valueRange = valueRange,
            steps = steps,
            onValueChangeFinished = onValueChangeFinished,
            colors = colors,
            interactionSource = interactionSource,
        )
        return
    }

    val isDark = LocalIsDarkTheme.current
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current

    val span = (valueRange.endInclusive - valueRange.start).coerceAtLeast(1e-6f)
    val fraction = ((value - valueRange.start) / span).coerceIn(0f, 1f)

    val latestOnValueChange by rememberUpdatedState(onValueChange)
    val latestOnFinished by rememberUpdatedState(onValueChangeFinished)
    val latestEnabled by rememberUpdatedState(enabled)
    val latestRange by rememberUpdatedState(valueRange)
    val latestSteps by rememberUpdatedState(steps)

    var trackWidthPx by remember { mutableIntStateOf(1) }
    var isDragging by remember { mutableStateOf(false) }
    var rubberOvershoot by remember { mutableFloatStateOf(0f) }

    val thumbRestW = 28.dp
    val thumbRestH = 19.dp
    val thumbRestWPx = with(density) { thumbRestW.toPx() }
    val usableTravelPx = (trackWidthPx - thumbRestWPx).coerceAtLeast(1f)

    val liftProgress by animateFloatAsState(
        targetValue = if (isDragging && enabled) 1f else 0f,
        animationSpec = if (isDragging) {
            spring(dampingRatio = 0.74f, stiffness = 780f)
        } else {
            spring(dampingRatio = 0.56f, stiffness = 540f)
        },
        label = "liquidSliderLift",
    )
    val glassClarity by animateFloatAsState(
        targetValue = if (isDragging && enabled) 1f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 680f),
        label = "liquidSliderGlass",
    )
    val rubberAnimated by animateFloatAsState(
        targetValue = if (isDragging) rubberOvershoot else 0f,
        animationSpec = spring(dampingRatio = 0.62f, stiffness = 540f),
        label = "liquidSliderRubber",
    )

    fun quantize(rawFraction: Float): Float {
        val clamped = rawFraction.coerceIn(0f, 1f)
        val snapped = if (latestSteps > 0) {
            val intervals = latestSteps + 1
            (clamped * intervals).roundToInt().toFloat() / intervals
        } else clamped
        return latestRange.start + snapped * (latestRange.endInclusive - latestRange.start)
    }

    val trackOffColor = MaterialTheme.colorScheme.onSurface.copy(alpha = if (isDark) 0.14f else 0.12f)
    val activeColor = colors.activeTrackColor.takeIf { it != Color.Unspecified && it != Color.Transparent }
        ?: MaterialTheme.colorScheme.primary

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(36.dp)
            .alpha(if (enabled) 1f else 0.42f)
            .onSizeChanged { if (it.width > 0) trackWidthPx = it.width }
            .pointerInput(usableTravelPx, thumbRestWPx) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    if (!latestEnabled) return@awaitEachGesture
                    isDragging = true
                    var lastEmitted = quantize((down.position.x - thumbRestWPx / 2f) / usableTravelPx)
                    latestOnValueChange(lastEmitted)
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)

                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) break
                        change.consume()
                        val raw = (change.position.x - thumbRestWPx / 2f) / usableTravelPx
                        val over = when {
                            raw < 0f -> raw
                            raw > 1f -> raw - 1f
                            else -> 0f
                        }
                        rubberOvershoot = sign(over) * (abs(over) / (1f + abs(over) * 2.5f)) * 0.16f
                        val next = quantize(raw)
                        if (abs(next - lastEmitted) > 1e-4f) {
                            if (latestSteps > 0) {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                            lastEmitted = next
                            latestOnValueChange(next)
                        }
                    }

                    isDragging = false
                    rubberOvershoot = 0f
                    latestOnFinished?.invoke()
                }
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        // Translucent pill track + active liquid fill
        val trackHeight = (6f + liftProgress.coerceAtLeast(0f) * 1.5f).dp
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(trackHeight)
                .uiClip(CircleShape)
                .background(trackOffColor)
                .border(
                    width = 0.8.dp,
                    color = Color.White.copy(alpha = if (isDark) 0.16f else 0.45f),
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.CenterStart,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction.coerceIn(0f, 1f))
                    .background(activeColor)
                    .background(
                        Brush.verticalGradient(
                            0.0f to Color.White.copy(alpha = 0.28f),
                            1.0f to Color.Transparent,
                        ),
                    ),
            )
        }

        // Lifted iOS 26 Liquid Glass lens thumb
        val safeLift = liftProgress.coerceAtLeast(-0.15f)
        val thumbScaleX = 1f + safeLift * 0.30f + abs(rubberAnimated) * 0.35f
        val thumbScaleY = 1f + safeLift * 0.28f - abs(rubberAnimated) * 0.18f
        val lensAlpha = (0.97f - glassClarity.coerceIn(0f, 1f) * 0.70f).coerceIn(0.22f, 0.98f)
        val borderAlpha = 0.78f + glassClarity.coerceIn(0f, 1f) * 0.20f
        val offsetPx = fraction * usableTravelPx + rubberAnimated * 18f

        Box(
            modifier = Modifier
                .offset { IntOffset(offsetPx.roundToInt(), 0) }
                .size(width = thumbRestW, height = thumbRestH)
                .graphicsLayer {
                    scaleX = thumbScaleX
                    scaleY = thumbScaleY
                }
                .shadow(
                    elevation = (3f + safeLift.coerceAtLeast(0f) * 5f).dp,
                    shape = CircleShape,
                    ambientColor = Color.Black.copy(alpha = 0.26f),
                    spotColor = Color.Black.copy(alpha = 0.30f),
                )
                .uiClip(CircleShape)
                .background(activeColor.copy(alpha = glassClarity.coerceIn(0f, 1f) * 0.24f))
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = (lensAlpha + 0.15f).coerceAtMost(1f)),
                            Color.White.copy(alpha = lensAlpha),
                            Color.White.copy(alpha = (lensAlpha * 0.68f).coerceAtLeast(0.16f)),
                        ),
                        center = Offset(18f, 8f),
                        radius = 56f,
                    ),
                )
                .border(
                    width = (0.9f + glassClarity.coerceIn(0f, 1f) * 0.6f).dp,
                    brush = Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = borderAlpha),
                            Color.White.copy(alpha = borderAlpha * 0.30f),
                            Color.White.copy(alpha = borderAlpha * 0.62f),
                        ),
                    ),
                    shape = CircleShape,
                ),
        )
    }
}

@Composable
fun DropdownMenuItem(
    text: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    enabled: Boolean = true,
    colors: MenuItemColors = MenuDefaults.itemColors(),
    contentPadding: PaddingValues = MenuDefaults.DropdownMenuItemContentPadding,
    interactionSource: MutableInteractionSource? = null,
) {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    M3DropdownMenuItem(
        text = text, onClick = onClick, modifier = modifier.comicPress(source), leadingIcon = leadingIcon,
        trailingIcon = trailingIcon, enabled = enabled, colors = colors, contentPadding = contentPadding,
        interactionSource = source,
    )
}

@Composable
fun RadioButton(
    selected: Boolean,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: RadioButtonColors = RadioButtonDefaults.colors(),
    interactionSource: MutableInteractionSource? = null,
) {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    M3RadioButton(
        selected = selected, onClick = onClick, modifier = modifier.comicPress(source), enabled = enabled,
        colors = colors, interactionSource = source,
    )
}

@Composable
fun Checkbox(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: CheckboxColors = CheckboxDefaults.colors(),
    interactionSource: MutableInteractionSource? = null,
) {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val isSakuga = LocalSakugaTheme.current
    val sakugaFx = LocalSakugaFx.current
    var boxBounds by remember { mutableStateOf<Rect?>(null) }
    val popAnim = remember { Animatable(1f) }
    var initialized by remember { mutableStateOf(false) }
    LaunchedEffect(checked, isSakuga) {
        if (!initialized) {
            initialized = true
            return@LaunchedEffect
        }
        if (isSakuga) {
            popAnim.snapTo(1.22f)
            popAnim.animateTo(1f, spring(dampingRatio = 0.42f, stiffness = 620f))
        }
    }
    M3Checkbox(
        checked = checked,
        onCheckedChange = if (onCheckedChange == null) null else { next ->
            if (isSakuga) {
                boxBounds?.let { b -> sakugaFx?.emitCheckboxChange(b.center, next) }
            }
            onCheckedChange(next)
        },
        modifier = modifier
            .let { m ->
                if (isSakuga) {
                    m.onGloballyPositioned { if (it.isAttached) boxBounds = it.boundsInRoot() }
                        .graphicsLayer { scaleX = popAnim.value; scaleY = 2f - popAnim.value }
                } else m
            }
            .comicPress(source),
        enabled = enabled,
        colors = colors,
        interactionSource = source,
    )
}

@Composable
private fun SakugaSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null,
) {
    val pal = LocalSakugaPalette.current
    val fx = LocalSakugaFx.current
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current
    val span = (valueRange.endInclusive - valueRange.start).coerceAtLeast(1e-6f)
    val fraction = ((value - valueRange.start) / span).coerceIn(0f, 1f)

    val latestOnChange by rememberUpdatedState(onValueChange)
    val latestOnFinished by rememberUpdatedState(onValueChangeFinished)
    val latestEnabled by rememberUpdatedState(enabled)
    val latestRange by rememberUpdatedState(valueRange)
    val latestSteps by rememberUpdatedState(steps)

    var trackWidthPx by remember { mutableIntStateOf(1) }
    var bounds by remember { mutableStateOf<Rect?>(null) }
    var prevFrac by remember { mutableStateOf<Float?>(null) }
    var prevNanos by remember { mutableLongStateOf(0L) }
    var rawVel by remember { mutableFloatStateOf(0f) }
    var lastDir by remember { mutableFloatStateOf(0f) }
    val smoothVel by animateFloatAsState(
        targetValue = rawVel,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 700f),
        label = "sakugaSliderVel",
    )
    val tailPath = remember { Path() }
    val primaryColor = MaterialTheme.colorScheme.primary
    val trackBg = MaterialTheme.colorScheme.surfaceContainerHighest

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(34.dp)
            .alpha(if (enabled) 1f else 0.45f)
            .onSizeChanged { trackWidthPx = it.width.coerceAtLeast(1) }
            .onGloballyPositioned { if (it.isAttached) bounds = it.boundsInRoot() }
            .pointerInput(Unit) {
                val thumbPadPx = with(density) { 12.dp.toPx() }
                fun fractionForX(x: Float): Float {
                    val usable = (trackWidthPx.toFloat() - thumbPadPx * 2f).coerceAtLeast(1f)
                    val f = ((x - thumbPadPx) / usable).coerceIn(0f, 1f)
                    val s = latestSteps
                    return if (s > 0) (f * (s + 1)).roundToInt().toFloat() / (s + 1) else f
                }
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    if (!latestEnabled) return@awaitEachGesture
                    down.consume()
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    val initialFrac = fractionForX(down.position.x)
                    prevFrac = initialFrac
                    prevNanos = SystemClock.elapsedRealtimeNanos()
                    rawVel = 0f
                    lastDir = 0f
                    latestOnChange(latestRange.start + initialFrac * (latestRange.endInclusive - latestRange.start))

                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) break
                        if (change.positionChange() != Offset.Zero) {
                            change.consume()
                            val newFrac = fractionForX(change.position.x)
                            val now = SystemClock.elapsedRealtimeNanos()
                            val old = prevFrac
                            if (old != null) {
                                val dtSec = ((now - prevNanos) / 1_000_000_000f).coerceAtLeast(0.004f)
                                val delta = newFrac - old
                                if (abs(delta) > 0.0005f) {
                                    val inst = (delta / dtSec / 2.2f).coerceIn(-1f, 1f)
                                    rawVel = (rawVel * 0.45f + inst * 0.55f).coerceIn(-1f, 1f)
                                    lastDir = sign(rawVel)
                                    if (abs(rawVel) > 0.14f) {
                                        bounds?.let { b ->
                                            val pad = with(density) { 12.dp.toPx() }
                                            val usable = (b.width - pad * 2f).coerceAtLeast(1f)
                                            val cx = b.left + pad + newFrac * usable
                                            fx?.emitSliderCloud(Offset(cx, b.center.y), sign(rawVel), abs(rawVel))
                                        }
                                    }
                                }
                            }
                            prevFrac = newFrac
                            prevNanos = now
                            latestOnChange(latestRange.start + newFrac * (latestRange.endInclusive - latestRange.start))
                        }
                    }

                    val relFrac = prevFrac ?: fraction
                    if (abs(rawVel) > 0.12f || lastDir != 0f) {
                        bounds?.let { b ->
                            val pad = with(density) { 12.dp.toPx() }
                            val usable = (b.width - pad * 2f).coerceAtLeast(1f)
                            val cx = b.left + pad + relFrac * usable
                            fx?.emitSliderRelease(Offset(cx, b.center.y), if (lastDir != 0f) lastDir else 1f)
                        }
                    }
                    prevFrac = null
                    rawVel = 0f
                    lastDir = 0f
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    latestOnFinished?.invoke()
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val pad = 12.dp.toPx()
            val trackH = 8.dp.toPx()
            val usable = (size.width - pad * 2f).coerceAtLeast(1f)
            val cy = size.height / 2f
            val corner = CornerRadius(trackH / 2f, trackH / 2f)
            val strokePx = 2.dp.toPx()

            drawRoundRect(
                color = pal.shadow,
                topLeft = Offset(pad + 1.5.dp.toPx(), cy - trackH / 2f + 1.5.dp.toPx()),
                size = Size(usable, trackH),
                cornerRadius = corner,
            )
            drawRoundRect(
                color = trackBg,
                topLeft = Offset(pad, cy - trackH / 2f),
                size = Size(usable, trackH),
                cornerRadius = corner,
            )
            val fillW = (fraction * usable).coerceIn(0f, usable)
            if (fillW > 0f) {
                drawRoundRect(
                    color = primaryColor,
                    topLeft = Offset(pad, cy - trackH / 2f),
                    size = Size(fillW, trackH),
                    cornerRadius = corner,
                )
            }
            drawRoundRect(
                color = pal.ink,
                topLeft = Offset(pad, cy - trackH / 2f),
                size = Size(usable, trackH),
                cornerRadius = corner,
                style = Stroke(strokePx),
            )

            val r = 9.5.dp.toPx()
            val thumbCenter = Offset(pad + fillW, cy)
            val tailLen = (abs(smoothVel) * 16.dp.toPx()).coerceAtMost(20.dp.toPx())
            if (tailLen < 1.2f) {
                drawCircle(color = pal.shadow.copy(alpha = 0.55f), radius = r, center = thumbCenter + Offset(1.5.dp.toPx(), 1.5.dp.toPx()))
                drawCircle(color = Color.White, radius = r, center = thumbCenter)
                drawCircle(color = pal.ink, radius = r, center = thumbCenter, style = Stroke(strokePx))
            } else {
                val dir = if (smoothVel >= 0f) 1f else -1f
                val tipX = thumbCenter.x - dir * (r + tailLen)
                tailPath.reset()
                tailPath.moveTo(tipX, cy)
                tailPath.quadraticTo(thumbCenter.x - dir * r * 0.55f, cy - r, thumbCenter.x, cy - r)
                tailPath.arcTo(
                    rect = Rect(thumbCenter.x - r, cy - r, thumbCenter.x + r, cy + r),
                    startAngleDegrees = -90f,
                    sweepAngleDegrees = dir * 180f,
                    forceMoveTo = false,
                )
                tailPath.quadraticTo(thumbCenter.x - dir * r * 0.55f, cy + r, tipX, cy)
                tailPath.close()
                drawPath(path = tailPath, color = Color.White)
                drawPath(path = tailPath, color = pal.ink, style = Stroke(width = strokePx, cap = StrokeCap.Round))
            }
        }
    }
}
