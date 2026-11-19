package com.lastwave.app.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import com.lastwave.app.ui.comic.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import com.lastwave.app.ui.comic.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.lastwave.app.ui.theme.Backdrop
import com.lastwave.app.ui.theme.LocalIsDarkTheme
import com.lastwave.app.ui.theme.LocalLiquidGlass
import com.lastwave.app.ui.theme.isLiquidGlassBackdropSupported
import com.lastwave.app.ui.theme.headerLiquidGlass

/** Square, full-bleed bar. Corner-based (not RectangleShape) because the glass lens needs one. */
private val HeaderShape = RoundedCornerShape(0.dp)

/**
 * The unified top container for all major screens.
 * Uses exact pure Kyant liquid glass chrome matching the navigation bar and player bar.
 * Window-inset aware: content pads for the status bar while the glass extends behind it.
 */
@Composable
fun ExpressiveHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    containerColor: Color = Color.Unspecified,
    backdrop: Backdrop? = null,
    /** Extra row drawn inside the same glass pane, under the title row
     *  (e.g. Home's chip strip), full width with no side padding. */
    bottomContent: (@Composable ColumnScope.() -> Unit)? = null,
    /** Optional custom brand/title slot replacing [title] + [subtitle] when provided. */
    leadingContent: (@Composable RowScope.() -> Unit)? = null,
    /** Optional centered slot (e.g. landscape search pill) between [leadingContent] and [actions]. */
    middleContent: (@Composable RowScope.() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val liquidGlass = LocalLiquidGlass.current
    val isGlass = liquidGlass && isLiquidGlassBackdropSupported()
    val isDark = LocalIsDarkTheme.current
    val backgroundColor = MaterialTheme.colorScheme.background
    val isCustomColor = containerColor != Color.Unspecified
    val isTransparent = isCustomColor && containerColor == Color.Transparent
    // Glass only over the caller's own scrolling content. A header-sized backdrop of its
    // own (plus an ink wash) ended on a hard line at the header's bottom edge; a header
    // with nothing under it passes Color.Transparent and sits on the page seamlessly.
    val effectiveBackdrop = backdrop
    val useGlass = isGlass && !isCustomColor && effectiveBackdrop != null

    val resolvedContainerColor = when {
        isCustomColor -> containerColor
        useGlass -> Color.Transparent
        else -> backgroundColor
    }

    Box(modifier.fillMaxWidth().zIndex(1f)) {
        Surface(
            shape = if (isTransparent) RectangleShape else HeaderShape,
            color = resolvedContainerColor,
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (useGlass && effectiveBackdrop != null) {
                        Modifier.headerLiquidGlass(
                            backdrop = effectiveBackdrop,
                            shape = HeaderShape,
                        )
                    } else {
                        Modifier
                    }
                ),
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(
                        WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
                    )
                    .padding(bottom = if (bottomContent != null) 2.dp else 6.dp),
            ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 2.dp, bottom = if (bottomContent != null) 2.dp else 0.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (onBack != null) {
                        FilledTonalIconButton(
                            onClick = onBack,
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = if (useGlass) Color.White.copy(alpha = if (isDark) 0.12f else 0.20f)
                                else MaterialTheme.colorScheme.surfaceContainerHighest,
                                contentColor = if (useGlass) (if (isDark) Color.White else Color.Black)
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                        ) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Back", modifier = Modifier.size(28.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                    }

                    if (leadingContent != null) {
                        leadingContent()
                    } else {
                        Column(Modifier.weight(1f)) {
                            Text(
                                title,
                                style = if (onBack != null) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (useGlass) (if (isDark) Color.White else Color.Black)
                                else MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis,
                            )
                            subtitle?.let {
                                Text(
                                    it,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (useGlass) (if (isDark) Color.White.copy(alpha = 0.7f) else Color.Black.copy(alpha = 0.7f))
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }

                    if (middleContent != null) {
                        Spacer(Modifier.width(12.dp))
                        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                            middleContent()
                        }
                        Spacer(Modifier.width(12.dp))
                    } else if (leadingContent != null) {
                        Spacer(Modifier.weight(1f))
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        actions()
                    }
                }
            }
            bottomContent?.invoke(this)
            }
        }
    }
}

/**
 * A trailing action icon for [ExpressiveHeader] styled with glass-compatible translucent tonal styling.
 */
@Composable
fun HeaderActionIcon(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
) {
    val isGlass = LocalLiquidGlass.current && isLiquidGlassBackdropSupported()
    val isDark = LocalIsDarkTheme.current
    FilledTonalIconButton(
        onClick = onClick,
        colors = IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = if (isGlass) Color.White.copy(alpha = if (isDark) 0.12f else 0.20f)
            else MaterialTheme.colorScheme.surfaceContainerHighest,
            contentColor = if (isGlass) (if (isDark) Color.White else Color.Black)
            else MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    ) {
        Icon(icon, contentDescription = contentDescription)
    }
}
