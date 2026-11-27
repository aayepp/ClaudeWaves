package com.lastwave.app.data.update

import android.content.Context
import android.os.Build
import androidx.core.content.pm.PackageInfoCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.CacheControl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

data class DailyActivePoint(
    val day: String,
    val dau: Int,
)

data class StatBucket(
    val label: String,
    val count: Int,
)

data class LiveUserStats(
    val online15m: Int,
    val dauToday: Int,
    val active24h: Int,
    val wau7d: Int,
    val mau30d: Int,
    val totalInstalls: Int,
    val newToday: Int,
    val new7d: Int,
    val dailyTrend14d: List<DailyActivePoint>,
    val byVersion: List<StatBucket>,
    val byAndroid: List<StatBucket>,
    val byCountry: List<StatBucket>,
)

data class ReleaseDownloadItem(
    val tag: String,
    val downloads: Long,
    val publishedAt: String,
)

data class GitHubReleaseStats(
    val totalDownloads: Long,
    val latestTag: String?,
    val latestDownloads: Long,
    val stars: Int,
    val forks: Int,
    val releases: List<ReleaseDownloadItem>,
)

data class DeveloperStatsSnapshot(
    val liveStats: LiveUserStats?,
    val liveError: String?,
    val githubStats: GitHubReleaseStats?,
    val githubError: String?,
    val fetchedAtMs: Long,
)

/**
 * Sends anonymous install heartbeats (random UUIDv4 + app version + Android API level;
 * zero PII) to the Cloudflare Worker backend and fetches live DAU/WAU/MAU plus
 * GitHub Releases download counts for the hidden developer panel.
 */
@Singleton
class AppStatsManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient,
    private val broadcastManager: BroadcastManager,
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val pingClient: OkHttpClient by lazy {
        okHttpClient.newBuilder()
            .callTimeout(6, TimeUnit.SECONDS)
            .build()
    }

    private fun getOrCreateInstallId(): String {
        val existing = prefs.getString(KEY_INSTALL_ID, null)?.trim()
        if (!existing.isNullOrBlank()) return existing
        val generated = UUID.randomUUID().toString()
        prefs.edit().putString(KEY_INSTALL_ID, generated).apply()
        return generated
    }

    suspend fun sendHeartbeatIfDue(force: Boolean = false): Boolean = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val lastPing = prefs.getLong(KEY_LAST_PING_MS, 0L)
        if (!force && lastPing > 0L && now - lastPing < PING_INTERVAL_MS) {
            return@withContext false
        }

        val pkgInfo = runCatching { context.packageManager.getPackageInfo(context.packageName, 0) }.getOrNull()
        val versionName = pkgInfo?.versionName?.trim()?.takeIf { it.isNotBlank() } ?: "unknown"
        val versionCode = pkgInfo?.let { PackageInfoCompat.getLongVersionCode(it) } ?: 0L
        val installId = getOrCreateInstallId()

        val payload = JSONObject().apply {
            put("id", installId)
            put("v", versionName.take(24))
            put("vc", versionCode)
            put("api", Build.VERSION.SDK_INT)
        }.toString()

        val request = Request.Builder()
            .url("$WORKER_BASE_URL/ping")
            .header("User-Agent", USER_AGENT)
            .cacheControl(CacheControl.FORCE_NETWORK)
            .post(payload.toRequestBody(JSON_MEDIA_TYPE))
            .build()

        try {
            pingClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    prefs.edit().putLong(KEY_LAST_PING_MS, now).apply()
                    true
                } else {
                    false
                }
            }
        } catch (e: Exception) {
            currentCoroutineContext().ensureActive()
            false
        }
    }

    suspend fun fetchDeveloperStats(): DeveloperStatsSnapshot = withContext(Dispatchers.IO) {
        val token = broadcastManager.readOwnerToken()
        coroutineScope {
            val liveDeferred = async { fetchLiveWorkerStats(token) }
            val githubDeferred = async { fetchGitHubStats(token) }
            val (liveStats, liveError) = liveDeferred.await()
            val (githubStats, githubError) = githubDeferred.await()
            DeveloperStatsSnapshot(
                liveStats = liveStats,
                liveError = liveError,
                githubStats = githubStats,
                githubError = githubError,
                fetchedAtMs = System.currentTimeMillis(),
            )
        }
    }

    private suspend fun fetchLiveWorkerStats(token: String?): Pair<LiveUserStats?, String?> {
        if (token.isNullOrBlank()) {
            return null to "Masukkan GitHub PAT (ikon kunci) untuk buka Live Active Users"
        }
        val request = Request.Builder()
            .url("$WORKER_BASE_URL/stats")
            .header("Authorization", "Bearer $token")
            .header("User-Agent", USER_AGENT)
            .cacheControl(CacheControl.FORCE_NETWORK)
            .get()
            .build()
        return try {
            pingClient.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    val msg = runCatching { JSONObject(body).optString("error") }.getOrNull().orEmpty()
                    return null to (msg.ifBlank { "Worker HTTP ${response.code}" })
                }
                parseLiveStats(body) to null
            }
        } catch (e: Exception) {
            currentCoroutineContext().ensureActive()
            null to (e.message ?: "Gagal sambung ke Cloudflare Worker")
        }
    }

    private suspend fun fetchGitHubStats(token: String?): Pair<GitHubReleaseStats?, String?> = try {
        coroutineScope {
            val releasesDeferred = async {
                val builder = Request.Builder()
                    .url(GITHUB_RELEASES_URL)
                    .header("Accept", "application/vnd.github+json")
                    .header("User-Agent", USER_AGENT)
                    .cacheControl(CacheControl.FORCE_NETWORK)
                    .get()
                if (!token.isNullOrBlank()) builder.header("Authorization", "Bearer $token")
                okHttpClient.newCall(builder.build()).execute().use { response ->
                    val body = response.body?.string().orEmpty()
                    if (!response.isSuccessful) {
                        error("GitHub Releases HTTP ${response.code}")
                    }
                    parseReleases(body)
                }
            }
            val repoDeferred = async {
                runCatching {
                    val builder = Request.Builder()
                        .url(GITHUB_REPO_URL)
                        .header("Accept", "application/vnd.github+json")
                        .header("User-Agent", USER_AGENT)
                        .cacheControl(CacheControl.FORCE_NETWORK)
                        .get()
                    if (!token.isNullOrBlank()) builder.header("Authorization", "Bearer $token")
                    okHttpClient.newCall(builder.build()).execute().use { response ->
                        if (!response.isSuccessful) return@use 0 to 0
                        val json = JSONObject(response.body?.string().orEmpty())
                        json.optInt("stargazers_count", 0) to json.optInt("forks_count", 0)
                    }
                }.getOrDefault(0 to 0)
            }
            val releases = releasesDeferred.await()
            val (stars, forks) = repoDeferred.await()
            val total = releases.sumOf { it.downloads }
            val latest = releases.firstOrNull()
            GitHubReleaseStats(
                totalDownloads = total,
                latestTag = latest?.tag,
                latestDownloads = latest?.downloads ?: 0L,
                stars = stars,
                forks = forks,
                releases = releases,
            ) to null
        }
    } catch (e: Exception) {
        currentCoroutineContext().ensureActive()
        null to (e.message ?: "Gagal muat turun statistik GitHub")
    }

    private fun parseLiveStats(body: String): LiveUserStats {
        val root = JSONObject(body)
        val trendArr = root.optJSONArray("dailyTrend14d") ?: JSONArray()
        val trend = buildList {
            for (i in 0 until trendArr.length()) {
                val obj = trendArr.optJSONObject(i) ?: continue
                add(
                    DailyActivePoint(
                        day = obj.optString("day", ""),
                        dau = obj.optInt("dau", 0),
                    ),
                )
            }
        }
        return LiveUserStats(
            online15m = root.optInt("online15m", 0),
            dauToday = root.optInt("dauToday", 0),
            active24h = root.optInt("active24h", 0),
            wau7d = root.optInt("wau7d", 0),
            mau30d = root.optInt("mau30d", 0),
            totalInstalls = root.optInt("totalInstalls", 0),
            newToday = root.optInt("newToday", 0),
            new7d = root.optInt("new7d", 0),
            dailyTrend14d = trend,
            byVersion = parseBuckets(root.optJSONArray("byVersion")),
            byAndroid = parseBuckets(root.optJSONArray("byAndroid")) { raw ->
                androidApiLabel(raw.toIntOrNull())
            },
            byCountry = parseBuckets(root.optJSONArray("byCountry")),
        )
    }

    private fun parseBuckets(
        arr: JSONArray?,
        transformLabel: (String) -> String = { it },
    ): List<StatBucket> {
        if (arr == null) return emptyList()
        return buildList {
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val label = obj.optString("label", "").trim()
                if (label.isNotBlank()) {
                    add(StatBucket(label = transformLabel(label), count = obj.optInt("count", 0)))
                }
            }
        }
    }

    private fun parseReleases(body: String): List<ReleaseDownloadItem> {
        val arr = JSONArray(body)
        return buildList {
            for (i in 0 until arr.length()) {
                val rel = arr.optJSONObject(i) ?: continue
                if (rel.optBoolean("draft", false)) continue
                val tag = rel.optString("tag_name", "").trim()
                if (tag.isBlank()) continue
                val assets = rel.optJSONArray("assets") ?: JSONArray()
                var apkDownloads = 0L
                var allDownloads = 0L
                for (j in 0 until assets.length()) {
                    val asset = assets.optJSONObject(j) ?: continue
                    val count = asset.optLong("download_count", 0L)
                    allDownloads += count
                    val name = asset.optString("name", "")
                    if (name.endsWith(".apk", ignoreCase = true)) {
                        apkDownloads += count
                    }
                }
                add(
                    ReleaseDownloadItem(
                        tag = tag,
                        downloads = if (apkDownloads > 0L) apkDownloads else allDownloads,
                        publishedAt = rel.optString("published_at", "").take(10),
                    ),
                )
            }
        }
    }

    private fun androidApiLabel(api: Int?): String = when (api) {
        36 -> "Android 16"
        35 -> "Android 15"
        34 -> "Android 14"
        33 -> "Android 13"
        32 -> "Android 12L"
        31 -> "Android 12"
        30 -> "Android 11"
        29 -> "Android 10"
        28 -> "Android 9"
        27 -> "Android 8.1"
        26 -> "Android 8.0"
        25 -> "Android 7.1"
        24 -> "Android 7.0"
        null -> "Unknown"
        else -> "API $api"
    }

    companion object {
        internal const val WORKER_BASE_URL = "https://claudewaves-stats.ayepcloudwaves.workers.dev"
        private const val GITHUB_RELEASES_URL = "https://api.github.com/repos/aayepp/ClaudeWaves/releases?per_page=25"
        private const val GITHUB_REPO_URL = "https://api.github.com/repos/aayepp/ClaudeWaves"
        private const val PREFS_NAME = "lastwave_stats_prefs"
        private const val KEY_INSTALL_ID = "install_id"
        private const val KEY_LAST_PING_MS = "last_ping_ms"
        private const val PING_INTERVAL_MS = 15L * 60L * 1000L
        private const val USER_AGENT = "ClaudeWaves-Android"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}
