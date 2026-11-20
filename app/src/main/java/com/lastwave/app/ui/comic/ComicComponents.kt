@file:Suppress("FunctionName")

package com.lastwave.app.ui.comic

import com.lastwave.app.ui.theme.LocalComicTheme
import com.lastwave.app.ui.theme.LocalIsDarkTheme
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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import kotlinx.coroutines.delay
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
    val pushed by animateFloatAsState(if (held) 1f else 0f, spring(stiffness = 900f), label = "comicButtonPush")
    return comicPress(source).comicPanel(shape, offset = 3.dp, translucent = colors.containerColor.alpha < 1f, pressed = { pushed })
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
    M3Button(onClick, modifier.comicButton(shape, source, enabled, colors), enabled, shape, colors, elevation, border, contentPadding, source, content)
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
    M3FilledTonalButton(onClick, modifier.comicButton(shape, source, enabled, colors), enabled, shape, colors, elevation, border, contentPadding, source, content)
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
    M3OutlinedButton(onClick, modifier.comicButton(shape, source, enabled, colors), enabled, shape, colors, elevation, border, contentPadding, source, content)
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
    M3IconButton(
        onClick = onClick,
        modifier = modifier.comicPress(source),
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
    M3FilledTonalIconButton(onClick, modifier.comicPress(source).comicPanel(shape, offset = 2.dp), enabled, shape, colors, source) {
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
    M3TextButton(
        onClick = onClick, modifier = modifier.comicPress(source), enabled = enabled, shape = shape, colors = colors,
        elevation = elevation, border = border, contentPadding = contentPadding, interactionSource = source, content = content,
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
    M3FilledIconButton(
        onClick = onClick, modifier = modifier.comicPress(source).comicPanel(shape, offset = 2.dp), enabled = enabled,
        shape = shape, colors = colors, interactionSource = source, content = { PressBounce(source, content) },
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
    M3FilterChip(
        selected = selected, onClick = onClick, label = label,
        modifier = modifier.comicPress(source).comicPanel(
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
    M3AssistChip(
        onClick = onClick, label = label,
        modifier = modifier.comicPress(source).comicPanel(shape, offset = 2.dp, stroke = 2.dp, translucent = colors.containerColor.alpha < 1f),
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
    if (LocalComicTheme.current) {
        M3Switch(
            checked = checked, onCheckedChange = onCheckedChange, modifier = modifier.comicPress(source),
            thumbContent = thumbContent, enabled = enabled, colors = colors, interactionSource = source,
        )
    } else {
        LiquidGlassSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = modifier,
            thumbContent = thumbContent,
            enabled = enabled,
            interactionSource = source,
        )
    }
}

/**
 * Liquid Glass Droplet toggle switch for normal Light / Dark / AMOLED themes.
 * Pressing or toggling stretches the thumb into a translucent frosted glass droplet
 * with spring physics, specular rim lighting, and subtle haptic feedback.
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
    val isPressed by interactionSource.collectIsPressedAsState()

    var isMorphing by remember { mutableStateOf(false) }
    var initialized by remember { mutableStateOf(false) }
    LaunchedEffect(checked) {
        if (!initialized) {
            initialized = true
            return@LaunchedEffect
        }
        isMorphing = true
        delay(190L)
        isMorphing = false
    }

    val stretched = (isPressed || isMorphing) && enabled
    val trackWidth = 54.dp
    val trackHeight = 32.dp
    val inset = 3.dp
    val thumbHeight = trackHeight - inset * 2

    val thumbWidth by animateDpAsState(
        targetValue = if (stretched) 34.dp else 26.dp,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 850f),
        label = "liquidSwitchThumbWidth",
    )
    val thumbOffsetX by animateDpAsState(
        targetValue = if (checked) trackWidth - inset - thumbWidth else inset,
        animationSpec = spring(dampingRatio = 0.78f, stiffness = 560f),
        label = "liquidSwitchThumbOffset",
    )
    val dropletGlassAlpha by animateFloatAsState(
        targetValue = if (stretched) 0.36f else 0.96f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 700f),
        label = "liquidSwitchDropletAlpha",
    )
    val trackColor by animateColorAsState(
        targetValue = if (checked) {
            MaterialTheme.colorScheme.primary.copy(alpha = if (isDark) 0.88f else 0.94f)
        } else {
            MaterialTheme.colorScheme.onSurface.copy(alpha = if (isDark) 0.14f else 0.12f)
        },
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 620f),
        label = "liquidSwitchTrackColor",
    )
    val rimTopColor = Color.White.copy(alpha = when {
        checked && isDark -> 0.42f
        checked -> 0.62f
        isDark -> 0.22f
        else -> 0.55f
    })
    val rimBottomColor = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.10f)

    val toggleModifier = if (onCheckedChange != null) {
        Modifier.toggleable(
            value = checked,
            enabled = enabled,
            role = Role.Switch,
            interactionSource = interactionSource,
            indication = null,
            onValueChange = { next ->
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onCheckedChange(next)
            },
        )
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .alpha(if (enabled) 1f else 0.42f)
            .then(toggleModifier),
        contentAlignment = Alignment.Center,
    ) {
        // Translucent liquid glass track with top specular rim + soft inner gloss
        Box(
            modifier = Modifier
                .size(width = trackWidth, height = trackHeight)
                .uiClip(CircleShape)
                .background(trackColor)
                .background(
                    Brush.verticalGradient(
                        0.0f to Color.White.copy(alpha = if (checked) 0.24f else if (isDark) 0.10f else 0.22f),
                        0.55f to Color.Transparent,
                        1.0f to Color.Black.copy(alpha = if (isDark) 0.16f else 0.06f),
                    ),
                )
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(listOf(rimTopColor, rimBottomColor)),
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.CenterStart,
        ) {
            // Liquid Glass Droplet thumb
            val thumbBorderAlpha = if (stretched) 0.92f else 0.78f
            Box(
                modifier = Modifier
                    .offset(x = thumbOffsetX)
                    .size(width = thumbWidth, height = thumbHeight)
                    .shadow(
                        elevation = if (stretched) 6.dp else 3.dp,
                        shape = CircleShape,
                        ambientColor = Color.Black.copy(alpha = 0.30f),
                        spotColor = Color.Black.copy(alpha = 0.32f),
                    )
                    .uiClip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = (dropletGlassAlpha + 0.14f).coerceAtMost(1f)),
                                Color.White.copy(alpha = dropletGlassAlpha),
                                Color.White.copy(alpha = (dropletGlassAlpha * 0.72f).coerceAtLeast(0.18f)),
                            ),
                            center = Offset(14f, 10f),
                            radius = 52f,
                        ),
                    )
                    .border(
                        width = if (stretched) 1.3.dp else 0.9.dp,
                        brush = Brush.verticalGradient(
                            listOf(
                                Color.White.copy(alpha = thumbBorderAlpha),
                                Color.White.copy(alpha = thumbBorderAlpha * 0.35f),
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
    M3Checkbox(
        checked = checked, onCheckedChange = onCheckedChange, modifier = modifier.comicPress(source), enabled = enabled,
        colors = colors, interactionSource = source,
    )
}
