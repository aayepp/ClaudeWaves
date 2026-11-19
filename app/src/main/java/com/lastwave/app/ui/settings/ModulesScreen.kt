package com.lastwave.app.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.ui.text.style.TextOverflow
import com.lastwave.app.ui.comic.Surface
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Extension
import com.lastwave.app.ui.comic.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import com.lastwave.app.ui.comic.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import com.lastwave.app.ui.comic.Switch
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.WindowInsets
import com.lastwave.app.ui.common.ExpressiveHeader
import com.lastwave.app.ui.common.GlassHeaderLayout
import com.lastwave.app.ui.common.rememberLazyListReorder
import com.lastwave.app.ui.common.reorderHandle
import com.lastwave.app.ui.common.safeDrawingBottomPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lastwave.app.R
import com.lastwave.app.data.plugin.InstalledProviderModule
import com.lastwave.app.ui.player.MiniPlayerAwareSnackbarHost

import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.zIndex
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.ui.platform.LocalUriHandler
import com.lastwave.app.data.local.AddonWebsite
import com.lastwave.app.ui.comic.AlertDialog
import androidx.compose.material3.OutlinedTextField
import com.lastwave.app.ui.comic.TextButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.lastwave.app.data.addon.AddonHealth

/**
 * Settings -> Provider Modules & Addons.
 * Configure external HTTP Addons or install .lwp packages, toggle, remove.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModulesScreen(
    onBack: () -> Unit = {},
    viewModel: ModulesViewModel = hiltViewModel(),
) {
    val modules by viewModel.modules.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val notice by viewModel.notice.collectAsStateWithLifecycle()
    val preferModules by viewModel.preferModules.collectAsStateWithLifecycle()
    val httpAddons by viewModel.httpAddons.collectAsStateWithLifecycle()
    val addonHealthMap by viewModel.addonHealthMap.collectAsStateWithLifecycle()
    val checkingIds by viewModel.checkingIds.collectAsStateWithLifecycle()
    val addonSites by viewModel.addonWebsites.collectAsStateWithLifecycle()

    var nowMs by remember { androidx.compose.runtime.mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(30_000L)
            nowMs = System.currentTimeMillis()
        }
    }

    LaunchedEffect(httpAddons.map { it.id to it.url }) {
        // Only auto-probe addons not already checked during app launch.
        val missing = httpAddons.any { it.url.isNotBlank() && it.id !in addonHealthMap }
        if (missing) viewModel.testAllAddons(silent = true)
    }
    val uriHandler = LocalUriHandler.current
    // No browser installed makes openUri throw; nothing useful to do but stay put.
    val openSite: (AddonWebsite) -> Unit = { site -> runCatching { uriHandler.openUri(site.url) } }
    var showSiteDialog by remember { mutableStateOf(false) }
    // Local copy the drag reorders live; saved once, when the finger lifts.
    val addonRows = remember(httpAddons) { httpAddons.toMutableStateList() }
    val listState = rememberLazyListState()
    val reorder = rememberLazyListReorder(
        listState = listState,
        canDropOn = { key -> addonRows.any { it.id == key } },
        onMove = { from, to ->
            val i = addonRows.indexOfFirst { it.id == from }
            val j = addonRows.indexOfFirst { it.id == to }
            if (i >= 0 && j >= 0) addonRows.add(j, addonRows.removeAt(i))
        },
        onEnd = { viewModel.reorderAddons(addonRows.map { it.id }) },
    )

    var showAddonDialog by remember { mutableStateOf(false) }
    var editingAddonId by remember { mutableStateOf<String?>(null) }
    var addonInputText by remember { mutableStateOf("") }
    val snackbar = remember { SnackbarHostState() }

    // "*/*": providers misreport .lwp as octet-stream; real validation
    // happens in ModuleManager.install after the file is read.
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) viewModel.install(uri)
    }

    LaunchedEffect(notice) {
        notice?.let {
            snackbar.showSnackbar(it)
            viewModel.clearNotice()
        }
    }

    if (showSiteDialog) {
        var siteName by remember { mutableStateOf("") }
        var siteUrl by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showSiteDialog = false },
            title = { Text(stringResource(R.string.mod_site_add)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = siteName,
                        onValueChange = { siteName = it },
                        label = { Text(stringResource(R.string.mod_site_name)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = siteUrl,
                        onValueChange = { siteUrl = it },
                        label = { Text(stringResource(R.string.mod_site_url)) },
                        placeholder = { Text("https://example.com") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (siteUrl.isNotBlank()) {
                            viewModel.addWebsite(siteName, siteUrl)
                            showSiteDialog = false
                        }
                    },
                ) { Text(stringResource(R.string.common_save)) }
            },
            dismissButton = {
                TextButton(onClick = { showSiteDialog = false }) { Text(stringResource(R.string.common_cancel)) }
            },
        )
    }

    if (showAddonDialog) {
        AlertDialog(
            onDismissRequest = { showAddonDialog = false },
            title = { Text(if (editingAddonId == null) "Add Streaming Addon" else "Edit Streaming Addon") },
            text = {
                Column {
                    Text(
                        "Enter the URL of your personal Addon service (e.g. http://10.0.2.2:8787/a/<token>/ or custom host):",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = addonInputText,
                        onValueChange = { addonInputText = it },
                        label = { Text(stringResource(R.string.mod_addon_url)) },
                        placeholder = { Text("https://example.com/a/...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val toSave = addonInputText.trim()
                        if (toSave.isNotBlank()) {
                            viewModel.saveAddon(editingAddonId, toSave)
                            showAddonDialog = false
                        }
                    },
                ) {
                    Text(if (editingAddonId == null) "Connect" else "Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddonDialog = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
    }

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        // The glass header handles the status bar; content scrolls under it.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { MiniPlayerAwareSnackbarHost(snackbar) },
    ) { _ ->
        GlassHeaderLayout(
            Modifier.fillMaxSize(),
            header = { headerModifier, headerBackdrop ->
                ExpressiveHeader(
                    modifier = headerModifier,
                    backdrop = headerBackdrop,
                    title = stringResource(R.string.settings_modules_title),
                    onBack = onBack,
                )
            },
        ) { headerInset ->
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp + headerInset, bottom = 16.dp + safeDrawingBottomPadding()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // --- Section 1: HTTP Addons ---
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        "Streaming Addons",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    if (httpAddons.isNotEmpty()) {
                        TextButton(
                            onClick = { viewModel.testAllAddons(silent = false) },
                            enabled = checkingIds.isEmpty(),
                        ) {
                            Icon(
                                Icons.Filled.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(if (checkingIds.isNotEmpty()) "Checking..." else "Check All")
                        }
                    }
                }
                Text(
                    "Take requests directly from personal or community Addon services. Enabled addons query concurrently; drag to set priority, the top addon wins when it finds the song.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            items(addonRows, key = { it.id }) { addon ->
                val health = addonHealthMap[addon.id]
                val isDragging = reorder.draggingKey == addon.id
                Card(
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        // The dragged card follows the finger itself; animating its slot
                        // too would move it twice and make it jitter.
                        .animateItem(placementSpec = if (isDragging) null else spring(stiffness = Spring.StiffnessMediumLow, visibilityThreshold = IntOffset.VisibilityThreshold))
                        .fillMaxWidth()
                        .zIndex(if (isDragging) 1f else 0f)
                        .graphicsLayer { translationY = if (isDragging) reorder.offset else 0f },
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDragging) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    ),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (addonRows.size > 1) {
                                // Position is resolve priority: drag ≡ to rank addons.
                                Icon(
                                    Icons.Filled.DragHandle,
                                    contentDescription = stringResource(R.string.mod_drag_cd),
                                    modifier = Modifier
                                        .size(48.dp)
                                        .reorderHandle(reorder, addon.id)
                                        .padding(10.dp),
                                )
                            } else {
                                Icon(
                                    Icons.Filled.Cloud,
                                    contentDescription = null,
                                    tint = if (health is AddonHealth.Unreachable) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp),
                                )
                            }
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    addon.name.ifBlank { "HTTP Addon" },
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium,
                                )
                                val masked = if (addon.url.length > 36) addon.url.take(24) + "..." + addon.url.takeLast(8) else addon.url
                                Text(
                                    masked,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Switch(
                                checked = addon.enabled,
                                onCheckedChange = { viewModel.toggleAddon(addon.id, it) },
                            )
                        }

                        Spacer(Modifier.height(8.dp))

                        val isChecking = addon.id in checkingIds
                        val isExpired = health is AddonHealth.Rejected ||
                            (health is AddonHealth.Unreachable &&
                                (health.reason.contains("401") || health.reason.contains("403") || health.reason.contains("410")))
                        val badgeLabel = when {
                            isChecking -> "Checking..."
                            health is AddonHealth.Ok -> "Online"
                            isExpired -> "Expired"
                            health is AddonHealth.Unreachable -> "Offline"
                            addon.enabled -> "Enabled"
                            else -> "Disabled"
                        }
                        val onlineGreen = androidx.compose.ui.graphics.Color(0xFF22C55E)
                        val dotColor = when {
                            isChecking -> MaterialTheme.colorScheme.onSurfaceVariant
                            health is AddonHealth.Ok -> onlineGreen
                            health is AddonHealth.Unreachable || health is AddonHealth.Rejected -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        }
                        val pillBg = when {
                            isChecking -> MaterialTheme.colorScheme.surfaceContainerHighest
                            health is AddonHealth.Ok -> onlineGreen.copy(alpha = 0.16f)
                            health is AddonHealth.Unreachable || health is AddonHealth.Rejected -> MaterialTheme.colorScheme.errorContainer
                            else -> MaterialTheme.colorScheme.surfaceContainerHighest
                        }
                        val pillTextColor = when {
                            isChecking -> MaterialTheme.colorScheme.onSurfaceVariant
                            health is AddonHealth.Ok -> onlineGreen
                            health is AddonHealth.Unreachable || health is AddonHealth.Rejected -> MaterialTheme.colorScheme.onErrorContainer
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                        val detailReason = when (health) {
                            is AddonHealth.Ok -> health.info
                            is AddonHealth.Unreachable -> health.reason
                            is AddonHealth.Rejected -> health.reason
                            null -> null
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Surface(
                                        shape = androidx.compose.foundation.shape.CircleShape,
                                        color = pillBg,
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        ) {
                                            if (isChecking) {
                                                androidx.compose.material3.CircularProgressIndicator(
                                                    modifier = Modifier.size(10.dp),
                                                    strokeWidth = 1.6.dp,
                                                    color = dotColor,
                                                )
                                            } else {
                                                Box(
                                                    modifier = Modifier
                                                        .size(8.dp)
                                                        .background(dotColor, androidx.compose.foundation.shape.CircleShape),
                                                )
                                            }
                                            Text(
                                                text = badgeLabel,
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = pillTextColor,
                                            )
                                        }
                                    }
                                    val expiresAtMs = (health as? AddonHealth.Ok)?.expiresAtMs
                                    if (expiresAtMs != null && !isChecking) {
                                        val remainingMs = expiresAtMs - nowMs
                                        val amber = androidx.compose.ui.graphics.Color(0xFFF59E0B)
                                        val (timerLabel, timerTextColor, timerBgColor) = when {
                                            remainingMs <= 0L -> Triple(
                                                "Expired",
                                                MaterialTheme.colorScheme.onErrorContainer,
                                                MaterialTheme.colorScheme.errorContainer,
                                            )
                                            remainingMs < 3_600_000L -> Triple(
                                                formatAddonRemaining(remainingMs),
                                                MaterialTheme.colorScheme.onErrorContainer,
                                                MaterialTheme.colorScheme.errorContainer,
                                            )
                                            remainingMs < 6 * 3_600_000L -> Triple(
                                                formatAddonRemaining(remainingMs),
                                                amber,
                                                amber.copy(alpha = 0.16f),
                                            )
                                            else -> Triple(
                                                formatAddonRemaining(remainingMs),
                                                onlineGreen,
                                                onlineGreen.copy(alpha = 0.14f),
                                            )
                                        }
                                        Surface(
                                            shape = androidx.compose.foundation.shape.CircleShape,
                                            color = timerBgColor,
                                        ) {
                                            Text(
                                                text = timerLabel,
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = timerTextColor,
                                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                                            )
                                        }
                                    }
                                }
                                if (!detailReason.isNullOrBlank()) {
                                    Text(
                                        text = detailReason,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { viewModel.testAddon(addon.id) },
                                    enabled = !busy && !isChecking,
                                ) {
                                    Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.mod_test_cd))
                                }
                                IconButton(
                                    onClick = {
                                        editingAddonId = addon.id
                                        addonInputText = addon.url
                                        showAddonDialog = true
                                    },
                                ) {
                                    Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.mod_edit_cd))
                                }
                                IconButton(
                                    onClick = { viewModel.removeAddon(addon.id) },
                                ) {
                                    Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.mod_remove_cd))
                                }
                            }
                        }
                        // A dead addon usually means its link expired: point at where a new one comes from.
                        if ((health is AddonHealth.Unreachable || health is AddonHealth.Rejected) && addonSites.isNotEmpty()) {
                            Box {
                                var pickSite by remember { mutableStateOf(false) }
                                TextButton(onClick = { if (addonSites.size == 1) openSite(addonSites.first()) else pickSite = true }) {
                                    Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(stringResource(R.string.mod_site_get_new))
                                }
                                DropdownMenu(expanded = pickSite, onDismissRequest = { pickSite = false }) {
                                    addonSites.forEach { site ->
                                        DropdownMenuItem(
                                            text = { Text(site.name) },
                                            onClick = { pickSite = false; openSite(site) },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Card(
                    onClick = {
                        editingAddonId = null
                        addonInputText = ""
                        showAddonDialog = true
                    },
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (httpAddons.isEmpty()) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(if (httpAddons.isEmpty()) Icons.Filled.Cloud else Icons.Filled.Add, contentDescription = null)
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                "Add Streaming Addon",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                if (httpAddons.isEmpty()) "Connect to your personal or custom Addon server URL" else "Add another concurrent streaming addon server",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }

            // --- Addon websites: where the user renews addon links ---
            item {
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.mod_sites_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    stringResource(R.string.mod_sites_sub),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            items(addonSites, key = { "site_" + it.url }) { site ->
                Card(
                    onClick = { openSite(site) },
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(start = 18.dp, top = 6.dp, bottom = 6.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Filled.Language, contentDescription = null)
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text(site.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                site.url,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        IconButton(onClick = { viewModel.removeWebsite(site.url) }) {
                            Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.mod_site_remove_cd))
                        }
                    }
                }
            }
            item {
                Card(
                    onClick = { showSiteDialog = true },
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Spacer(Modifier.width(16.dp))
                        Text(stringResource(R.string.mod_site_add), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }


            // --- Section 2: Local .lwp Packages ---
            item {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.settings_modules_query_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            stringResource(R.string.settings_modules_query_sub),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(checked = preferModules, onCheckedChange = viewModel::setPreferModules)
                }
            }

            item {
                Card(
                    onClick = { if (!busy) picker.launch(arrayOf("*/*")) },
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (busy) {
                            CircularProgressIndicator(modifier = Modifier.size(22.dp))
                        } else {
                            Icon(Icons.Filled.Add, contentDescription = null)
                        }
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                stringResource(R.string.settings_modules_add),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                stringResource(R.string.settings_modules_add_sub),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }

            if (modules.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.settings_modules_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 4.dp),
                    )
                }
            }
            items(modules, key = { it.manifest.id }) { module ->
                ModuleRow(
                    module = module,
                    onToggle = { viewModel.setEnabled(module.manifest.id, it) },
                    onRemove = { viewModel.remove(module.manifest.id) },
                )
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
        }
    }
}

@Composable
private fun ModuleRow(
    module: InstalledProviderModule,
    onToggle: (Boolean) -> Unit,
    onRemove: () -> Unit,
) {
    val manifest = module.manifest
    Card(
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.Extension, contentDescription = null)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(manifest.name.ifBlank { manifest.id }, fontWeight = FontWeight.Bold)
                Text(
                    "${manifest.version.ifBlank { "?" }} • ${manifest.capabilities.take(4).joinToString(", ")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = module.enabled, onCheckedChange = onToggle)
            IconButton(onClick = onRemove) {
                Icon(Icons.Filled.Delete, contentDescription = null)
            }
        }
    }
}

private fun formatAddonRemaining(remainingMs: Long): String {
    val totalMinutes = (remainingMs / 60_000L).coerceAtLeast(1L)
    val days = totalMinutes / (60L * 24L)
    val hours = (totalMinutes % (60L * 24L)) / 60L
    val mins = totalMinutes % 60L
    return when {
        days > 0L -> "${days}d ${hours}h left"
        hours > 0L -> "${hours}h ${mins}m left"
        else -> "${mins}m left"
    }
}
