package com.lastwave.app.ui.shell

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.graphicsLayer
import com.lastwave.app.ui.player.kick
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import com.lastwave.app.ui.theme.inkHalo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/** A tab's icon: its animated glyph when it has one, else the plain vector. */
@Composable
private fun TabIcon(item: LiquidNavigationItem, play: () -> Float, tint: Color, modifier: Modifier) {
    val glyph = item.glyph
    if (glyph != null) AnimatedTabGlyph(glyph, play, tint, modifier)
    else Icon(item.icon, contentDescription = null, tint = tint, modifier = modifier)
}

/** The glyph over a crisp contrast halo copy of itself in [halo], zero GPU blur passes per hop frame. */
@Composable
private fun HaloTabIcon(item: LiquidNavigationItem, play: () -> Float, tint: Color, halo: Color, modifier: Modifier) {
    Box(modifier) {
        TabIcon(
            item,
            play,
            halo.copy(alpha = halo.alpha * 0.72f),
            Modifier.matchParentSize().graphicsLayer {
                scaleX = 1.08f
                scaleY = 1.08f
                translationY = 0.75.dp.toPx()
            },
        )
        TabIcon(item, play, tint, Modifier.matchParentSize())
    }
}

/** Shared, stationary geometry for the visible rail and the lens's captured glyph plane. */
@Composable
internal fun NavigationGlyphs(
    items: List<LiquidNavigationItem>,
    selectedIndex: Int,
    captionIndex: Int,
    compact: Boolean,
    ink: (index: Int) -> Color,
    captured: Boolean,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    slotWeights: List<Float>? = null,
) {
    Row(modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
        items.forEachIndexed { index, item ->
            val tint = ink(index)
            val halo = inkHalo(tint)
            // A small hop as a tab becomes selected. Keyed on the selection, not the tap,
            // so the optical copy and the touch layer hop together.
            val hop = remember(index) { Animatable(0f) }
            val isSelected = selectedIndex == index
            var wasSelected by remember(index) { mutableStateOf(isSelected) }
            LaunchedEffect(isSelected) {
                if (isSelected && !wasSelected) hop.kick()
                wasSelected = isSelected
            }
            val iconMotion = Modifier.graphicsLayer {
                val s = 1f + 0.18f * hop.value
                scaleX = s
                scaleY = s
                translationY = -3.dp.toPx() * hop.value
            }
            val inputModifier = if (captured) {
                // The optical copy must neither receive touches nor duplicate tab semantics.
                Modifier.clearAndSetSemantics { }
            } else {
                Modifier
                    .testTag("liquid_navigation_tab_$index")
                    .then(if (compact) Modifier.semantics { contentDescription = item.label } else Modifier)
                    .selectable(
                        selected = selectedIndex == index,
                        onClick = { onSelect(index) },
                        role = Role.Tab,
                        interactionSource = remember(index) { MutableInteractionSource() },
                        indication = null,
                    )
            }
            // Clear Text itself as well: unmerged semantic queries must not find the hidden copy.
            val textSemantics = if (captured) Modifier.clearAndSetSemantics { } else Modifier
            val slotWeight = slotWeights?.getOrNull(index)?.coerceAtLeast(0.5f) ?: 1f
            Box(
                modifier = Modifier
                    .weight(slotWeight)
                    .fillMaxHeight()
                    .then(inputModifier)
                    .padding(horizontal = 4.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (compact) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        HaloTabIcon(item, { hop.value }, tint, halo, Modifier.size(24.dp).then(iconMotion))
                        if (captionIndex == index) {
                            Text(
                                text = item.label,
                                modifier = Modifier.weight(1f, fill = false).padding(start = 6.dp).then(textSemantics),
                                color = tint,
                                style = MaterialTheme.typography.labelMedium.copy(shadow = Shadow(halo, blurRadius = 8f)),
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        HaloTabIcon(item, { hop.value }, tint, halo, Modifier.size(23.dp).then(iconMotion))
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = item.label,
                            modifier = textSemantics,
                            color = tint,
                            style = MaterialTheme.typography.labelSmall.copy(shadow = Shadow(halo, blurRadius = 8f)),
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}
