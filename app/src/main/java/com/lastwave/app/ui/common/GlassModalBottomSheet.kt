package com.lastwave.app.ui.common

import android.os.Build
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.SheetState
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.compose.ui.window.DialogWindowProvider
import com.lastwave.app.ui.theme.LiquidGlassIntensity
import com.lastwave.app.ui.theme.LocalIsDarkTheme
import com.lastwave.app.ui.theme.LocalLiquidGlass
import androidx.compose.material3.ModalBottomSheet as M3ModalBottomSheet

/**
 * The app's bottom sheet: Material's, plus liquid glass when it is on.
 *
 * A sheet lives in its own window, so the in-app backdrop glass cannot see the
 * screen behind it. Android 12+ blurs behind a window in the compositor
 * instead (no app GPU work) and the sheet's own surface turns slightly
 * translucent; the blur thins as the user's glass intensity rises. Without
 * cross-window blur (Battery Saver, some devices) the sheet stays opaque.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModalBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(),
    sheetMaxWidth: Dp = BottomSheetDefaults.SheetMaxWidth,
    sheetGesturesEnabled: Boolean = true,
    shape: Shape = BottomSheetDefaults.ExpandedShape,
    containerColor: Color = BottomSheetDefaults.ContainerColor,
    contentColor: Color = contentColorFor(containerColor),
    tonalElevation: Dp = 0.dp,
    scrimColor: Color = BottomSheetDefaults.ScrimColor,
    dragHandle: @Composable (() -> Unit)? = { BottomSheetDefaults.DragHandle() },
    contentWindowInsets: @Composable () -> WindowInsets = { BottomSheetDefaults.windowInsets },
    properties: ModalBottomSheetProperties = ModalBottomSheetProperties(),
    glass: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    val blur = glass && LocalLiquidGlass.current && crossWindowBlurAvailable()
    val isDark = LocalIsDarkTheme.current
    val intensity = LiquidGlassIntensity.value
    val resolvedDragHandle: (@Composable () -> Unit)? = when {
        dragHandle == null -> null
        blur -> {
            {
                Box(
                    Modifier
                        .padding(vertical = 12.dp)
                        .size(width = 38.dp, height = 4.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = if (isDark) 0.30f else 0.72f))
                        .border(
                            0.5.dp,
                            Color.White.copy(alpha = if (isDark) 0.45f else 0.90f),
                            CircleShape,
                        ),
                )
            }
        }
        else -> dragHandle
    }
    val sheetModifier = if (blur) {
        modifier.drawWithContent {
            drawContent()
            val outline = shape.createOutline(size, layoutDirection, this)
            // Soft upper sheen + crisp top specular rim along the sheet's rounded crest.
            drawRect(
                brush = Brush.verticalGradient(
                    0f to Color.White.copy(alpha = if (isDark) lerp(0.06f, 0.13f, intensity) else lerp(0.16f, 0.28f, intensity)),
                    0.18f to Color.Transparent,
                ),
            )
            drawOutline(
                outline = outline,
                brush = Brush.linearGradient(
                    0f to Color.White.copy(alpha = if (isDark) lerp(0.32f, 0.50f, intensity) else lerp(0.65f, 0.90f, intensity)),
                    0.28f to Color.White.copy(alpha = if (isDark) 0.08f else 0.22f),
                    0.55f to Color.Transparent,
                    start = Offset(0f, 0f),
                    end = Offset(size.width * 0.65f, size.height * 0.45f),
                ),
                style = Stroke(width = 1.dp.toPx()),
            )
        }
    } else {
        modifier
    }
    M3ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = sheetModifier,
        sheetState = sheetState,
        sheetMaxWidth = sheetMaxWidth,
        sheetGesturesEnabled = sheetGesturesEnabled,
        shape = shape,
        // Scales smoothly with Glass Clarity while keeping dense rows readable over busy screens.
        containerColor = if (blur) {
            containerColor.copy(alpha = containerColor.alpha * lerp(0.88f, 0.72f, intensity))
        } else {
            containerColor
        },
        contentColor = contentColor,
        tonalElevation = tonalElevation,
        scrimColor = scrimColor,
        dragHandle = resolvedDragHandle,
        contentWindowInsets = contentWindowInsets,
        properties = properties,
    ) {
        if (blur) WindowBehindBlur(radius = 36.dp * LiquidGlassIntensity.blur())
        content()
    }
}

@Composable
private fun crossWindowBlurAvailable(): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return false
    val wm = LocalContext.current.getSystemService(WindowManager::class.java)
    return wm?.isCrossWindowBlurEnabled == true
}

/** Blurs whatever is behind this composable's window (API 31+). */
@Composable
private fun WindowBehindBlur(radius: Dp) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
    val view = LocalView.current
    val px = with(LocalDensity.current) { radius.roundToPx() }
    DisposableEffect(view, px) {
        (view.parent as? DialogWindowProvider)?.window?.setBackgroundBlurRadius(px)
        onDispose { }
    }
}
