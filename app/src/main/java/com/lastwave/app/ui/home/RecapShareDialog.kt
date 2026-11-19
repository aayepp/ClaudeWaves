package com.lastwave.app.ui.home

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.lastwave.app.ui.comic.Button
import com.lastwave.app.ui.comic.FilterChip
import com.lastwave.app.ui.comic.IconButton
import com.lastwave.app.ui.comic.OutlinedButton
import com.lastwave.app.ui.common.EdgeToEdgeDialogWindow
import com.lastwave.app.util.FileExportHelper
import kotlinx.coroutines.launch

@Composable
fun RecapShareDialog(
    uiState: HomeUiState,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val exportHelper = remember(context) { FileExportHelper(context.applicationContext) }
    val primaryArgb = MaterialTheme.colorScheme.primary.toArgb()

    val swatchPalette = remember(primaryArgb) {
        listOf(
            primaryArgb,
            0xFF8B5CF6.toInt(), // Neon Violet
            0xFF06B6D4.toInt(), // Electric Cyan
            0xFFF43F5E.toInt(), // Sunset Coral
        ).distinct()
    }

    var selectedPeriodName by rememberSaveable { mutableStateOf(RecapPeriod.LAST_7_DAYS.name) }
    var selectedAccent by rememberSaveable { mutableIntStateOf(swatchPalette.first()) }
    val selectedPeriod = remember(selectedPeriodName) {
        RecapPeriod.entries.firstOrNull { it.name == selectedPeriodName } ?: RecapPeriod.LAST_7_DAYS
    }

    val recapData = remember(uiState, selectedPeriod) { buildRecapCardData(uiState, selectedPeriod) }
    var heroBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var renderedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isExporting by remember { mutableStateOf(false) }

    LaunchedEffect(recapData.heroArtworkUrl) {
        val url = recapData.heroArtworkUrl
        heroBitmap = if (url.isNullOrBlank()) {
            null
        } else {
            runCatching {
                val request = ImageRequest.Builder(context)
                    .data(url)
                    .allowHardware(false)
                    .size(512)
                    .build()
                val result = context.imageLoader.execute(request) as? SuccessResult
                (result?.drawable as? BitmapDrawable)?.bitmap
            }.getOrNull()
        }
    }

    LaunchedEffect(recapData, heroBitmap, selectedAccent) {
        renderedBitmap = RecapCardRenderer.render(
            context = context,
            data = recapData,
            heroArtwork = heroBitmap,
            accentColor = selectedAccent,
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        EdgeToEdgeDialogWindow()
        BackHandler(onBack = onDismiss)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF08090D))
                .systemBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.White)
                }
                Text(
                    text = "ClaudeWaves Recap",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
                Spacer(Modifier.size(48.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            ) {
                RecapPeriod.entries.forEach { period ->
                    FilterChip(
                        selected = period == selectedPeriod,
                        onClick = { selectedPeriodName = period.name },
                        label = { Text(period.chipLabel) },
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                val bmp = renderedBitmap
                if (bmp != null) {
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = "ClaudeWaves Recap Preview",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .aspectRatio(1080f / 1350f)
                            .clip(RoundedCornerShape(24.dp)),
                    )
                } else {
                    CircularProgressIndicator(color = Color(selectedAccent))
                }
            }

            Row(
                modifier = Modifier.padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                swatchPalette.forEach { argb ->
                    val selected = argb == selectedAccent
                    Box(
                        modifier = Modifier
                            .size(if (selected) 34.dp else 28.dp)
                            .clip(CircleShape)
                            .background(Color(argb))
                            .border(
                                width = if (selected) 3.dp else 1.dp,
                                color = if (selected) Color.White else Color.White.copy(alpha = 0.25f),
                                shape = CircleShape,
                            )
                            .clickable { selectedAccent = argb },
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = {
                        val bmp = renderedBitmap ?: return@OutlinedButton
                        if (isExporting) return@OutlinedButton
                        isExporting = true
                        scope.launch {
                            val res = exportHelper.saveImageToGallery(
                                bitmap = bmp,
                                title = selectedPeriod.name.lowercase(),
                                filenamePrefix = "recap",
                            )
                            isExporting = false
                            Toast.makeText(
                                context,
                                if (res.isSuccess) "Saved Recap to Pictures/ClaudeWaves" else "Could not save Recap",
                                Toast.LENGTH_SHORT,
                            ).show()
                        }
                    },
                    enabled = renderedBitmap != null && !isExporting,
                    modifier = Modifier.weight(1f).height(50.dp),
                ) {
                    Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(8.dp))
                    Text("Save")
                }

                Button(
                    onClick = {
                        val bmp = renderedBitmap ?: return@Button
                        if (isExporting) return@Button
                        isExporting = true
                        scope.launch {
                            val res = exportHelper.shareImage(
                                activityContext = context,
                                bitmap = bmp,
                                title = "recap_${selectedPeriod.name.lowercase()}",
                                chooserTitle = "Share ClaudeWaves Recap",
                                clipLabel = "ClaudeWaves Recap",
                            )
                            isExporting = false
                            if (res.isFailure) {
                                Toast.makeText(context, "Could not share Recap", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    enabled = renderedBitmap != null && !isExporting,
                    modifier = Modifier.weight(1f).height(50.dp),
                ) {
                    Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(8.dp))
                    Text("Share")
                }
            }
        }
    }
}
