package com.lastwave.app.data.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.lastwave.app.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

data class UpdateInfo(
    val isChecking: Boolean = false,
    val isUpdateAvailable: Boolean = false,
    val latestVersion: String = "",
    val currentVersion: String = "",
    val releaseNotes: String = "",
    val releaseUrl: String = "",
    val downloadUrl: String? = null,
    val downloadFileName: String? = null,
    val isDismissed: Boolean = false,
    val message: String? = null,
    /** Release title and publish date, shown in the update dialog. */
    val releaseTitle: String = "",
    val publishedAt: String = "",
    val downloadSizeBytes: Long = 0L,
    /** The in-app update dialog is open (on launch, or from Settings / the banner). */
    val showDialog: Boolean = false,
    val isDownloading: Boolean = false,
    val downloadProgress: Int? = null,
    val downloadedBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val downloadedApkPath: String? = null,
    val downloadError: String? = null,
)

data class ReleaseAsset(
    val name: String,
    val downloadUrl: String,
    val sizeBytes: Long = 0L,
)

enum class ApkVariant {
    UNCOMPRESSED,
    ANDROID7,
    UNIVERSAL,
    /** vivo Origin Island build (separate package); never mixed with the others. */
    VIVO_ORIGIN,
}

@Singleton
class AppUpdateManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val prefs = context.getSharedPreferences("lastwave_updates", Context.MODE_PRIVATE)
    private val downloadClient: OkHttpClient by lazy {
        // Bypass the small 8 MB metadata HTTP cache and allow longer streams for APK downloads.
        okHttpClient.newBuilder()
            .cache(null)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .callTimeout(10, TimeUnit.MINUTES)
            .build()
    }
    @Volatile private var downloadJob: Job? = null

    private val _updateInfo = MutableStateFlow(
        UpdateInfo(
            currentVersion = getCurrentVersion(),
        )
    )
    val updateInfo: StateFlow<UpdateInfo> = _updateInfo.asStateFlow()

    init {
        checkForUpdate(isSilent = true)
    }

    fun isUncompressedBuild(): Boolean {
        val pref = prefs.getString("preferred_variant", null)
        if (pref != null) {
            return pref.equals("uncompressed", ignoreCase = true) || pref.equals("raw", ignoreCase = true)
        }
        val buildType = runCatching { BuildConfig.BUILD_TYPE }.getOrDefault("")
        return buildType.equals("rawRelease", ignoreCase = true) ||
               buildType.contains("raw", ignoreCase = true) ||
               buildType.contains("uncompressed", ignoreCase = true)
    }

    fun getPreferredVariant(): String? = prefs.getString("preferred_variant", null)

    fun setPreferredVariant(variant: String?) {
        if (variant == null) {
            prefs.edit().remove("preferred_variant").apply()
        } else {
            prefs.edit().putString("preferred_variant", variant).apply()
        }
        checkForUpdate(isSilent = true)
    }

    fun getCurrentVersion(): String = try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "5.0.0"
    } catch (_: Exception) {
        "5.0.0"
    }

    fun checkForUpdate(isSilent: Boolean = false) {
        scope.launch {
            _updateInfo.update {
                it.copy(
                    isChecking = true,
                    message = if (!isSilent) "Checking for updates..." else it.message,
                )
            }
            try {
                val response = withContext(Dispatchers.IO) {
                    okHttpClient.newCall(latestReleaseRequest()).execute()
                }

                if (!response.isSuccessful) {
                    val code = response.code
                    _updateInfo.update {
                        it.copy(
                            isChecking = false,
                            // GitHub answers 404 while the repo has no release yet: nothing newer exists.
                            message = when {
                                isSilent -> null
                                code == 404 -> "You're on the latest version"
                                else -> "Could not check updates (HTTP $code)"
                            },
                        )
                    }
                    return@launch
                }

                val body = response.body?.string().orEmpty()
                val json = JSONObject(body)
                val tagName = json.optString("tag_name", "")
                val releaseUrl = json.optString("html_url", "https://github.com/$RELEASES_REPO/releases")
                val releaseNotes = json.optString("body", "")
                val releaseTitle = json.optString("name", "")
                val publishedAt = json.optString("published_at", "")

                val apkAssets = mutableListOf<ReleaseAsset>()
                val assets = json.optJSONArray("assets")
                if (assets != null) {
                    for (i in 0 until assets.length()) {
                        val asset = assets.optJSONObject(i) ?: continue
                        val name = asset.optString("name", "").trim()
                        val download = asset.optString("browser_download_url", "").trim()
                        if (name.endsWith(".apk", ignoreCase = true) && download.isNotBlank()) {
                            apkAssets.add(
                                ReleaseAsset(
                                    name = name,
                                    downloadUrl = download,
                                    sizeBytes = asset.optLong("size", 0L),
                                ),
                            )
                        }
                    }
                }

                val bestAsset = selectBestAsset(
                    assets = apkAssets,
                    isUncompressed = isUncompressedBuild(),
                    sdkInt = Build.VERSION.SDK_INT,
                    isVivoOrigin = BuildConfig.VIVO_ORIGIN,
                )
                val downloadUrl = bestAsset?.downloadUrl ?: releaseUrl
                val downloadFileName = bestAsset?.name

                val currentVersion = getCurrentVersion()
                val hasNewer = isNewerVersion(tagName, currentVersion)
                val cleanTag = tagName.removePrefix("v").removePrefix("V")
                val dismissedVersion = prefs.getString("dismissed_version", null)
                val isDismissed = dismissedVersion == cleanTag

                // Like the desktop app: a new, not-dismissed release opens the
                // update dialog by itself once per launch.
                val autoOpen = isSilent && hasNewer && !isDismissed && !autoDialogShown
                if (autoOpen) autoDialogShown = true
                val expectedAssetSize = bestAsset?.sizeBytes ?: 0L
                val cachedApk = if (hasNewer && bestAsset != null) {
                    cachedUpdateApkFile(cleanTag).takeIf {
                        it.isFile && it.length() > 1_000_000L &&
                            (expectedAssetSize <= 0L || it.length() == expectedAssetSize)
                    }
                } else {
                    null
                }
                _updateInfo.update {
                    val keepActiveDownload = it.isDownloading && it.latestVersion == cleanTag
                    it.copy(
                        isChecking = false,
                        isUpdateAvailable = hasNewer,
                        latestVersion = cleanTag,
                        currentVersion = currentVersion,
                        releaseNotes = releaseNotes,
                        releaseTitle = releaseTitle,
                        publishedAt = publishedAt,
                        downloadSizeBytes = expectedAssetSize,
                        showDialog = it.showDialog || autoOpen,
                        releaseUrl = releaseUrl,
                        downloadUrl = downloadUrl,
                        downloadFileName = downloadFileName,
                        isDismissed = isDismissed,
                        downloadedApkPath = if (keepActiveDownload) it.downloadedApkPath else cachedApk?.absolutePath,
                        downloadError = if (keepActiveDownload) it.downloadError else null,
                        message = if (!isSilent) {
                            if (hasNewer) "New version $cleanTag available!" else "You're on the latest version ($currentVersion)"
                        } else null,
                    )
                }
            } catch (e: Exception) {
                _updateInfo.update {
                    it.copy(
                        isChecking = false,
                        message = if (!isSilent) "Check failed: ${e.message ?: "Network error"}" else null,
                    )
                }
            }
        }
    }

    private fun latestReleaseRequest() = Request.Builder()
        .url("https://api.github.com/repos/$RELEASES_REPO/releases/latest")
        .header("Accept", "application/vnd.github.v3+json")
        .header("User-Agent", "ClaudeWaves-Android")
        .build()

    /**
     * For [UpdateCheckWorker]: the latest release's version if it is newer than this
     * build and the user has neither dismissed nor already been notified of it, else
     * null. Marks it notified, so each release is announced once.
     */
    suspend fun versionToAnnounce(): String? {
        val tag = withContext(Dispatchers.IO) {
            okHttpClient.newCall(latestReleaseRequest()).execute().use { response ->
                if (response.isSuccessful) JSONObject(response.body?.string().orEmpty()).optString("tag_name") else null
            }
        } ?: return null
        if (!isNewerVersion(tag, getCurrentVersion())) return null
        val version = tag.removePrefix("v").removePrefix("V")
        if (version == prefs.getString("dismissed_version", null) || version == prefs.getString("notified_version", null)) return null
        prefs.edit().putString("notified_version", version).apply()
        return version
    }

    fun dismissUpdate(version: String) {
        val clean = version.removePrefix("v").removePrefix("V")
        prefs.edit().putString("dismissed_version", clean).apply()
        _updateInfo.update { it.copy(isDismissed = true) }
    }

    /** Settings' "Update Ready" and the banner open the in-app dialog, not the browser. */
    fun openUpdate(context: Context) = showDialog()

    fun showDialog() = _updateInfo.update { it.copy(showDialog = true) }

    fun hideDialog() = _updateInfo.update { it.copy(showDialog = false) }

    /** The release page, for the dialog's GitHub button. */
    fun openReleasePage(context: Context) {
        val targetUrl = _updateInfo.value.releaseUrl.ifBlank {
            "https://github.com/$RELEASES_REPO/releases"
        }
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (_: Exception) { }
    }

    @Volatile private var autoDialogShown = false
    /**
     * Download the release APK inside the app with live progress and hand off
     * to Android's package installer once complete. Falls back to the release
     * page when the release has no direct `.apk` asset.
     */
    fun downloadAndInstallUpdate(context: Context) {
        val info = _updateInfo.value
        val targetUrl = info.downloadUrl?.takeIf { it.isNotBlank() }
            ?: return openReleasePage(context)
        val hasDirectApk = !info.downloadFileName.isNullOrBlank() ||
            targetUrl.endsWith(".apk", ignoreCase = true)
        if (!hasDirectApk) {
            openReleasePage(context)
            return
        }

        info.downloadedApkPath
            ?.let(::File)
            ?.takeIf { it.isFile && it.length() > 0L }
            ?.let { readyFile ->
                installApkFile(context, readyFile)
                return
            }

        if (info.isDownloading) return

        val appContext = context.applicationContext
        val versionTag = info.latestVersion.ifBlank { "latest" }
        val expectedBytes = info.downloadSizeBytes
        downloadJob = scope.launch {
            val targetFile = cachedUpdateApkFile(versionTag)
            val tempFile = File(targetFile.parentFile, "${targetFile.name}.part")
            try {
                cleanOldUpdateApks(keepName = targetFile.name)
                if (tempFile.exists()) tempFile.delete()

                _updateInfo.update {
                    it.copy(
                        isDownloading = true,
                        downloadProgress = if (expectedBytes > 0L) 0 else null,
                        downloadedBytes = 0L,
                        totalBytes = expectedBytes,
                        downloadedApkPath = null,
                        downloadError = null,
                    )
                }

                val request = Request.Builder()
                    .url(targetUrl)
                    .header("User-Agent", "ClaudeWaves-Android")
                    .build()

                downloadClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw IOException("HTTP ${response.code}")
                    }
                    val body = response.body ?: throw IOException("Empty response body")
                    val total = body.contentLength().takeIf { it > 0L } ?: expectedBytes

                    body.byteStream().use { input ->
                        FileOutputStream(tempFile).use { output ->
                            val buffer = ByteArray(64 * 1024)
                            var downloaded = 0L
                            var lastReportAt = 0L
                            var lastPercent = -1
                            while (true) {
                                currentCoroutineContext().ensureActive()
                                val read = input.read(buffer)
                                if (read == -1) break
                                output.write(buffer, 0, read)
                                downloaded += read
                                val now = System.currentTimeMillis()
                                val percent = if (total > 0L) {
                                    ((downloaded * 100L) / total).toInt().coerceIn(0, 99)
                                } else {
                                    null
                                }
                                if ((percent != lastPercent && now - lastReportAt >= 200L) ||
                                    (total > 0L && downloaded == total)
                                ) {
                                    lastPercent = percent ?: -1
                                    lastReportAt = now
                                    _updateInfo.update {
                                        it.copy(
                                            downloadedBytes = downloaded,
                                            totalBytes = total,
                                            downloadProgress = percent,
                                        )
                                    }
                                }
                            }
                            output.flush()
                        }
                    }
                }

                currentCoroutineContext().ensureActive()
                if (targetFile.exists()) targetFile.delete()
                if (!tempFile.renameTo(targetFile)) {
                    tempFile.copyTo(targetFile, overwrite = true)
                    tempFile.delete()
                }
                if (!targetFile.exists() || targetFile.length() <= 0L) {
                    throw IOException("Downloaded APK is empty")
                }

                _updateInfo.update {
                    it.copy(
                        isDownloading = false,
                        downloadProgress = 100,
                        downloadedBytes = targetFile.length(),
                        totalBytes = targetFile.length(),
                        downloadedApkPath = targetFile.absolutePath,
                        downloadError = null,
                    )
                }

                withContext(Dispatchers.Main) {
                    installApkFile(appContext, targetFile)
                }
            } catch (e: CancellationException) {
                runCatching { tempFile.delete() }
                _updateInfo.update {
                    it.copy(
                        isDownloading = false,
                        downloadProgress = null,
                        downloadedBytes = 0L,
                    )
                }
                throw e
            } catch (e: Exception) {
                tempFile.delete()
                _updateInfo.update {
                    it.copy(
                        isDownloading = false,
                        downloadProgress = null,
                        downloadedBytes = 0L,
                        downloadError = "Download failed: ${e.message ?: "Network error"}",
                    )
                }
            }
        }
    }

    fun cancelDownload() {
        downloadJob?.cancel()
        downloadJob = null
        _updateInfo.update {
            it.copy(
                isDownloading = false,
                downloadProgress = null,
                downloadedBytes = 0L,
                downloadError = null,
            )
        }
    }

    fun installApkFile(context: Context, apkFile: File) {
        if (!apkFile.exists() || apkFile.length() <= 0L) {
            _updateInfo.update {
                it.copy(
                    downloadedApkPath = null,
                    downloadError = "Downloaded APK was removed. Tap Update now to download again.",
                )
            }
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !context.packageManager.canRequestPackageInstalls()) {
            _updateInfo.update {
                it.copy(
                    downloadError = "Allow 'Install unknown apps' for ClaudeWaves, then tap Install Update.",
                )
            }
            val permIntent = Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}"),
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            try {
                context.startActivity(permIntent)
            } catch (_: Exception) { }
            return
        }

        _updateInfo.update { it.copy(downloadError = null) }
        try {
            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                apkFile,
            )
            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(installIntent)
        } catch (e: Exception) {
            _updateInfo.update {
                it.copy(downloadError = "Could not open installer: ${e.message ?: "Unknown error"}")
            }
        }
    }

    private fun cachedUpdateApkFile(version: String): File {
        val safeTag = version.replace(Regex("[^A-Za-z0-9._-]"), "_").ifBlank { "latest" }
        val sharedDir = File(context.cacheDir, "shared").apply { mkdirs() }
        return File(sharedDir, "ClaudeWaves-update-$safeTag.apk")
    }

    private fun cleanOldUpdateApks(keepName: String) {
        val sharedDir = File(context.cacheDir, "shared")
        sharedDir.listFiles()?.forEach { file ->
            if (file.name.startsWith("ClaudeWaves-update-") && file.name != keepName) {
                runCatching { file.delete() }
            }
        }
    }

    /** Browser fallback for opening the APK URL externally. */
    fun openDownload(context: Context) {
        val targetUrl = _updateInfo.value.downloadUrl?.takeIf { it.isNotBlank() }
            ?: return openReleasePage(context)
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (_: Exception) { }
    }

    fun isNewerVersion(remote: String, local: String): Boolean {
        if (remote.isBlank() || local.isBlank()) return false
        val cleanRemote = remote.removePrefix("v").removePrefix("V").substringBefore("-")
        val cleanLocal = local.removePrefix("v").removePrefix("V").substringBefore("-")
        if (cleanRemote == cleanLocal) return false

        val rParts = cleanRemote.split(".").mapNotNull { it.toIntOrNull() }
        val cParts = cleanLocal.split(".").mapNotNull { it.toIntOrNull() }

        val maxLen = maxOf(rParts.size, cParts.size)
        for (i in 0 until maxLen) {
            val r = rParts.getOrElse(i) { 0 }
            val c = cParts.getOrElse(i) { 0 }
            if (r > c) return true
            if (r < c) return false
        }
        return false
    }

    companion object {
        fun classifyAsset(name: String): ApkVariant {
            val lower = name.lowercase()
            return when {
                lower.contains("vivo") -> ApkVariant.VIVO_ORIGIN
                lower.contains("uncompressed") || lower.contains("-raw") || lower.contains("_raw") -> ApkVariant.UNCOMPRESSED
                lower.contains("android7") || lower.contains("legacy") -> ApkVariant.ANDROID7
                else -> ApkVariant.UNIVERSAL
            }
        }

        fun selectBestAsset(
            assets: List<ReleaseAsset>,
            isUncompressed: Boolean,
            sdkInt: Int,
            isVivoOrigin: Boolean = false,
        ): ReleaseAsset? {
            // The vivo build is a different package: installing the other APK
            // would add a second app instead of updating, so each build only
            // ever picks from its own pool.
            val apkAssets = assets.filter {
                it.name.endsWith(".apk", ignoreCase = true) && it.downloadUrl.isNotBlank() &&
                    (classifyAsset(it.name) == ApkVariant.VIVO_ORIGIN) == isVivoOrigin
            }
            if (apkAssets.isEmpty()) return null
            if (isVivoOrigin) return apkAssets.first()

            val target = when {
                sdkInt < 28 -> ApkVariant.ANDROID7
                isUncompressed -> ApkVariant.UNCOMPRESSED
                else -> ApkVariant.UNIVERSAL
            }

            // Direct match for target variant, preferring "universal" naming if present
            val matches = apkAssets.filter { classifyAsset(it.name) == target }
            if (matches.isNotEmpty()) {
                return matches.maxByOrNull { asset ->
                    val lower = asset.name.lowercase()
                    when (target) {
                        ApkVariant.UNIVERSAL -> if (lower.contains("universal")) 2 else 1
                        ApkVariant.UNCOMPRESSED -> if (lower.contains("universal")) 2 else 1
                        ApkVariant.ANDROID7 -> if (lower.contains("android7")) 2 else 1
                        ApkVariant.VIVO_ORIGIN -> 1
                    }
                }
            }

            // Fallback hierarchy if target variant was not present in this release:
            return when (target) {
                ApkVariant.UNCOMPRESSED -> {
                    apkAssets.firstOrNull { classifyAsset(it.name) == ApkVariant.UNIVERSAL }
                        ?: apkAssets.firstOrNull { classifyAsset(it.name) == ApkVariant.ANDROID7 }
                        ?: apkAssets.firstOrNull()
                }
                ApkVariant.UNIVERSAL -> {
                    apkAssets.firstOrNull { classifyAsset(it.name) == ApkVariant.UNCOMPRESSED }
                        ?: apkAssets.firstOrNull { classifyAsset(it.name) == ApkVariant.ANDROID7 }
                        ?: apkAssets.firstOrNull()
                }
                ApkVariant.ANDROID7 -> {
                    apkAssets.firstOrNull { classifyAsset(it.name) == ApkVariant.UNIVERSAL }
                        ?: apkAssets.firstOrNull()
                }
                ApkVariant.VIVO_ORIGIN -> apkAssets.firstOrNull()
            }
        }
    }
}

/**
 * Where this build's releases live. Upstream LastWave ships separately, so
 * in-app updates track the ClaudeWaves repository directly.
 */
private const val RELEASES_REPO = "aayepp/ClaudeWaves"
