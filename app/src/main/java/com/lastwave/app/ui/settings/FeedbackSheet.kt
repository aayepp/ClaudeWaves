package com.lastwave.app.ui.settings

import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.lastwave.app.R
import com.lastwave.app.data.update.FeedbackCategory
import com.lastwave.app.data.update.FeedbackDraft
import com.lastwave.app.data.update.PendingCrashReport
import com.lastwave.app.data.update.SubmittedFeedbackReceipt
import com.lastwave.app.ui.comic.Button
import com.lastwave.app.ui.comic.FilterChip
import com.lastwave.app.ui.comic.Surface
import com.lastwave.app.ui.comic.Switch
import com.lastwave.app.ui.comic.TextButton
import com.lastwave.app.ui.theme.LocalComicTheme
import com.lastwave.app.ui.theme.LocalSakugaTheme
import com.lastwave.app.ui.theme.sakugaTypingInput
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class BugChipOption(
    val wireKey: String,
    val labelRes: Int,
)

private val BUG_AREAS = listOf(
    BugChipOption("Playback", R.string.feedback_area_playback),
    BugChipOption("MiniPlayer", R.string.feedback_area_miniplayer),
    BugChipOption("Lyrics", R.string.feedback_area_lyrics),
    BugChipOption("Downloads", R.string.feedback_area_downloads),
    BugChipOption("USB DAC", R.string.feedback_area_usb_dac),
    BugChipOption("Search", R.string.feedback_area_search),
    BugChipOption("UI / Theme", R.string.feedback_area_ui_theme),
    BugChipOption("Other", R.string.feedback_area_other),
)

private val BUG_FREQUENCIES = listOf(
    BugChipOption("Always", R.string.feedback_freq_always),
    BugChipOption("Sometimes", R.string.feedback_freq_sometimes),
    BugChipOption("Once", R.string.feedback_freq_once),
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun FeedbackBottomSheet(
    onDismiss: () -> Unit,
    initialDraft: FeedbackDraft? = null,
    prefillCrash: PendingCrashReport? = null,
    recentSubmissions: List<SubmittedFeedbackReceipt> = emptyList(),
    onDraftChange: (FeedbackDraft) -> Unit = {},
    onClearDraft: () -> Unit = {},
    onSubmit: suspend (
        category: FeedbackCategory,
        title: String,
        body: String,
        contact: String,
        includeDeviceInfo: Boolean,
        includeDiagnostics: Boolean,
        screenshots: List<Uri>,
    ) -> Result<Long>,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isFlatCel = LocalComicTheme.current || LocalSakugaTheme.current

    val crashTitleSeed = prefillCrash?.let { "Crash: ${it.headline}".take(120) }
    val crashBodySeed = prefillCrash?.let {
        "${it.crashType}\n${it.compactTrace}".trim().take(2000)
    }

    var categoryKey by rememberSaveable {
        mutableStateOf(
            if (prefillCrash != null) FeedbackCategory.BUG.wireKey
            else initialDraft?.categoryKey ?: FeedbackCategory.BUG.wireKey,
        )
    }
    var bugAreaKey by rememberSaveable {
        mutableStateOf(initialDraft?.bugAreaKey ?: BUG_AREAS.first().wireKey)
    }
    var bugFreqKey by rememberSaveable {
        mutableStateOf(initialDraft?.bugFreqKey ?: BUG_FREQUENCIES.first().wireKey)
    }
    var title by rememberSaveable { mutableStateOf(crashTitleSeed ?: initialDraft?.title.orEmpty()) }
    var body by rememberSaveable { mutableStateOf(crashBodySeed ?: initialDraft?.body.orEmpty()) }
    var contact by rememberSaveable { mutableStateOf(initialDraft?.contact.orEmpty()) }
    var includeDevice by rememberSaveable { mutableStateOf(initialDraft?.includeDevice ?: true) }
    var includeDiagnostics by rememberSaveable {
        mutableStateOf(if (prefillCrash != null) true else initialDraft?.includeDiagnostics ?: true)
    }
    var showRecentHistory by rememberSaveable { mutableStateOf(false) }
    var pickedUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var isSending by rememberSaveable { mutableStateOf(false) }
    var sentSuccess by rememberSaveable { mutableStateOf(false) }
    var errorMsg by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(
        categoryKey,
        bugAreaKey,
        bugFreqKey,
        title,
        body,
        contact,
        includeDevice,
        includeDiagnostics,
        sentSuccess,
    ) {
        if (prefillCrash == null && !sentSuccess) {
            onDraftChange(
                FeedbackDraft(
                    categoryKey = categoryKey,
                    bugAreaKey = bugAreaKey,
                    bugFreqKey = bugFreqKey,
                    title = title,
                    body = body,
                    contact = contact,
                    includeDevice = includeDevice,
                    includeDiagnostics = includeDiagnostics,
                ),
            )
        }
    }

    val shortErr = stringResource(R.string.feedback_error_short)
    val rateErr = stringResource(R.string.feedback_error_rate_limit)
    val failErr = stringResource(R.string.feedback_error_failed)

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 5),
    ) { selected ->
        if (selected.isNotEmpty()) {
            pickedUris = (pickedUris + selected).distinct().take(5)
        }
    }

    val versionName = remember(context) {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0"
        }.getOrDefault("1.0")
    }
    val deviceSummary = remember(versionName) {
        "v$versionName · Android ${Build.VERSION.RELEASE} · ${Build.MODEL}"
    }
    val category = FeedbackCategory.fromWire(categoryKey)
    val isBugMode = category == FeedbackCategory.BUG
    val inputStyle = LocalTextStyle.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = stringResource(R.string.feedback_sheet_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = if (isBugMode) {
                            stringResource(R.string.feedback_bug_sub)
                        } else {
                            stringResource(R.string.feedback_sheet_sub)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if ((title.isNotBlank() || body.isNotBlank()) && !isSending && !sentSuccess && prefillCrash == null) {
                    TextButton(
                        onClick = {
                            title = ""
                            body = ""
                            contact = ""
                            pickedUris = emptyList()
                            errorMsg = null
                            onClearDraft()
                        },
                    ) {
                        Text(
                            text = stringResource(R.string.feedback_clear_draft),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val options = listOf(
                    Triple(FeedbackCategory.BUG, R.string.feedback_cat_bug, Icons.Filled.BugReport),
                    Triple(FeedbackCategory.SUGGESTION, R.string.feedback_cat_suggestion, Icons.Filled.Lightbulb),
                    Triple(FeedbackCategory.FEEDBACK, R.string.feedback_cat_general, Icons.Filled.ChatBubbleOutline),
                )
                options.forEach { (cat, labelRes, icon) ->
                    val selected = category == cat
                    FilterChip(
                        selected = selected,
                        onClick = {
                            if (!isSending) {
                                categoryKey = cat.wireKey
                                errorMsg = null
                            }
                        },
                        label = {
                            Text(
                                text = stringResource(labelRes),
                                maxLines = 1,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            )
                        },
                        leadingIcon = {
                            Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        shape = CircleShape,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ),
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            if (isBugMode) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = stringResource(R.string.feedback_bug_area_label),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        BUG_AREAS.forEach { area ->
                            val selected = bugAreaKey == area.wireKey
                            FilterChip(
                                selected = selected,
                                onClick = { if (!isSending && !sentSuccess) bugAreaKey = area.wireKey },
                                label = {
                                    Text(
                                        text = stringResource(area.labelRes),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    )
                                },
                                shape = CircleShape,
                            )
                        }
                    }
                }
            }

            OutlinedTextField(
                value = title,
                onValueChange = {
                    if (it.length <= 120) title = it
                    errorMsg = null
                },
                label = {
                    Text(
                        stringResource(
                            if (isBugMode) R.string.feedback_bug_title_label else R.string.feedback_title_label,
                        ),
                    )
                },
                placeholder = {
                    Text(
                        stringResource(
                            if (isBugMode) R.string.feedback_bug_title_hint else R.string.feedback_title_hint,
                        ),
                    )
                },
                singleLine = true,
                enabled = !isSending && !sentSuccess,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .sakugaTypingInput(text = title, textStyle = inputStyle, leadingPadding = 16.dp),
            )

            OutlinedTextField(
                value = body,
                onValueChange = {
                    if (it.length <= 2000) body = it
                    errorMsg = null
                },
                label = {
                    Text(
                        stringResource(
                            if (isBugMode) R.string.feedback_bug_steps_label else R.string.feedback_body_label,
                        ),
                    )
                },
                placeholder = {
                    Text(
                        stringResource(
                            if (isBugMode) R.string.feedback_bug_steps_hint else R.string.feedback_body_hint,
                        ),
                    )
                },
                minLines = if (isBugMode) 4 else 3,
                maxLines = 7,
                enabled = !isSending && !sentSuccess,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .sakugaTypingInput(text = body, textStyle = inputStyle, leadingPadding = 16.dp),
            )

            if (isBugMode) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = stringResource(R.string.feedback_bug_freq_label),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        BUG_FREQUENCIES.forEach { freq ->
                            val selected = bugFreqKey == freq.wireKey
                            FilterChip(
                                selected = selected,
                                onClick = { if (!isSending && !sentSuccess) bugFreqKey = freq.wireKey },
                                label = {
                                    Text(
                                        text = stringResource(freq.labelRes),
                                        maxLines = 1,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    )
                                },
                                shape = CircleShape,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }

            // Screenshot attachment card (optional up to 5 images for bug/UI context)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = if (isFlatCel) 1f else 0.45f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.32f)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.feedback_screenshots_title),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = if (pickedUris.isEmpty()) {
                                    stringResource(R.string.feedback_screenshots_empty)
                                } else {
                                    stringResource(R.string.feedback_screenshots_selected, pickedUris.size)
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (pickedUris.size < 5 && !isSending && !sentSuccess) {
                            FilterChip(
                                selected = pickedUris.isNotEmpty(),
                                onClick = {
                                    photoPicker.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Filled.AddPhotoAlternate,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                    )
                                },
                                label = {
                                    Text(
                                        text = stringResource(
                                            if (pickedUris.isEmpty()) {
                                                R.string.feedback_screenshots_pick
                                            } else {
                                                R.string.feedback_screenshots_add
                                            },
                                        ),
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                },
                                shape = CircleShape,
                            )
                        }
                    }

                    if (pickedUris.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            pickedUris.forEach { uri ->
                                Box(
                                    modifier = Modifier
                                        .size(width = 76.dp, height = 76.dp)
                                        .clip(RoundedCornerShape(12.dp)),
                                ) {
                                    AsyncImage(
                                        model = uri,
                                        contentDescription = stringResource(R.string.feedback_screenshots_title),
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                    if (!isSending && !sentSuccess) {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(4.dp)
                                                .size(20.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.72f))
                                                .clickable { pickedUris = pickedUris.filterNot { it == uri } },
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Close,
                                                contentDescription = stringResource(R.string.feedback_screenshots_remove),
                                                tint = MaterialTheme.colorScheme.onPrimary,
                                                modifier = Modifier.size(13.dp),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            OutlinedTextField(
                value = contact,
                onValueChange = { if (it.length <= 80) contact = it },
                label = { Text(stringResource(R.string.feedback_contact_label)) },
                placeholder = { Text(stringResource(R.string.feedback_contact_hint)) },
                singleLine = true,
                enabled = !isSending && !sentSuccess,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .sakugaTypingInput(text = contact, textStyle = inputStyle, leadingPadding = 16.dp),
            )

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = if (isFlatCel) 1f else 0.55f),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.feedback_include_device),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = deviceSummary,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = includeDevice,
                        onCheckedChange = { if (!isSending && !sentSuccess) includeDevice = it },
                    )
                }
            }

            if (isBugMode) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = if (isFlatCel) 1f else 0.55f),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.feedback_include_diagnostics),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = stringResource(R.string.feedback_include_diagnostics_sub),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = includeDiagnostics,
                            onCheckedChange = { if (!isSending && !sentSuccess) includeDiagnostics = it },
                        )
                    }
                }
            }

            errorMsg?.let { msg ->
                Text(
                    text = msg,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Medium,
                )
            }

            if (sentSuccess) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        Text(
                            text = stringResource(R.string.feedback_sent_ok),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
            } else {
                Button(
                    onClick = {
                        if (title.trim().length < 3 || body.trim().length < 5) {
                            errorMsg = shortErr
                            return@Button
                        }
                        val formattedBody = if (isBugMode) {
                            "[Area: $bugAreaKey] [Freq: $bugFreqKey]\n\nSteps to Reproduce:\n${body.trim()}"
                        } else {
                            body.trim()
                        }
                        scope.launch {
                            isSending = true
                            errorMsg = null
                            val result = onSubmit(
                                category,
                                title,
                                formattedBody,
                                contact,
                                includeDevice,
                                isBugMode && includeDiagnostics,
                                pickedUris,
                            )
                            isSending = false
                            result.onSuccess {
                                sentSuccess = true
                                delay(1100L)
                                onDismiss()
                            }.onFailure { err ->
                                errorMsg = when (err.message) {
                                    "FEEDBACK_SHORT" -> shortErr
                                    "FEEDBACK_RATE_LIMIT" -> rateErr
                                    else -> failErr
                                }
                            }
                        }
                    },
                    enabled = !isSending,
                    shape = CircleShape,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                ) {
                    if (isSending) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.feedback_sending), fontWeight = FontWeight.Bold)
                    } else {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = stringResource(
                                if (isBugMode) R.string.feedback_send_bug else R.string.feedback_send,
                            ),
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }

            if (recentSubmissions.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    TextButton(
                        onClick = { showRecentHistory = !showRecentHistory },
                        modifier = Modifier.align(Alignment.Start),
                    ) {
                        Text(
                            text = stringResource(R.string.feedback_recent_title, recentSubmissions.size),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    AnimatedVisibility(visible = showRecentHistory) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            recentSubmissions.take(5).forEach { receipt ->
                                val dateStr = remember(receipt.submittedAtMs) {
                                    SimpleDateFormat("d MMM HH:mm", Locale.getDefault())
                                        .format(Date(receipt.submittedAtMs))
                                }
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = if (isFlatCel) 1f else 0.4f),
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            text = if (receipt.id > 0L) {
                                                stringResource(R.string.feedback_recent_ticket, receipt.id.toInt(), receipt.title)
                                            } else {
                                                receipt.title
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f).padding(end = 8.dp),
                                        )
                                        Text(
                                            text = dateStr,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
