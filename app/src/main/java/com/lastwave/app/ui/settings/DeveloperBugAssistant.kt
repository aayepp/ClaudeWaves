package com.lastwave.app.ui.settings

import android.content.ClipData
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.FileProvider
import com.lastwave.app.data.update.FeedbackCategory
import com.lastwave.app.data.update.UserFeedbackItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.io.File
import java.util.concurrent.TimeUnit

internal enum class DevLanguage(
    val code: String,
    val shortLabel: String,
    val displayName: String,
) {
    BM("ms", "BM", "Bahasa Melayu"),
    EN("en", "EN", "English");

    companion object {
        fun fromCode(raw: String?): DevLanguage =
            if (raw.equals("en", ignoreCase = true)) EN else BM
    }
}

internal data class FeedbackTranslation(
    val sourceLang: String,
    val targetLang: DevLanguage,
    val translatedTitle: String,
    val translatedBody: String,
    val alreadyTargetLang: Boolean,
)

internal enum class BugPriority {
    HIGH,
    MEDIUM,
    LOW;

    fun label(lang: DevLanguage): String = when (this) {
        HIGH -> if (lang == DevLanguage.BM) "Keutamaan Tinggi" else "High Priority"
        MEDIUM -> if (lang == DevLanguage.BM) "Sederhana" else "Medium"
        LOW -> if (lang == DevLanguage.BM) "Ringan" else "Low"
    }
}

internal enum class BugSymptomKind {
    CRASH,
    AUDIO_FAIL,
    FREEZE_LAG,
    UI_GLITCH,
    PLAYBACK_CONTROL,
    LYRICS_CANVAS,
    DOWNLOAD_OFFLINE,
    GENERAL,
}

internal data class BugTriageDigest(
    val area: String,
    val frequency: String?,
    val priority: BugPriority,
    val plainExplanation: String,
    val likelyCause: String,
    val suspectFiles: List<String>,
    val steps: List<String>,
)

/**
 * Lightweight free Google Translate wrapper for Developer Inbox reports so user bug reports
 * can be auto-translated to Malay or English without bundled models or extra API keys.
 */
internal object FeedbackTranslator {
    private const val ENDPOINT = "https://translate.googleapis.com/translate_a/single"
    private const val MARKER = "\uE000CWSEP\uE001"
    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .callTimeout(10, TimeUnit.SECONDS)
            .build()
    }

    suspend fun translate(
        title: String,
        body: String,
        targetLang: DevLanguage,
    ): FeedbackTranslation? = withContext(Dispatchers.IO) {
        val combined = "${title.trim()}\n$MARKER\n${body.trim()}"
        if (combined.isBlank()) return@withContext null
        val form = FormBody.Builder()
            .add("client", "dict-chrome-ex")
            .add("sl", "auto")
            .add("tl", targetLang.code)
            .add("dt", "t")
            .add("q", combined)
            .build()
        val request = Request.Builder()
            .url(ENDPOINT)
            .header("User-Agent", "ClaudeWaves-Android")
            .header("Accept", "application/json")
            .post(form)
            .build()

        try {
            client.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext null
                val raw = resp.body?.string().orEmpty()
                if (raw.isBlank()) return@withContext null
                val root = JSONArray(raw)
                val segs = root.optJSONArray(0) ?: return@withContext null
                val fullOut = buildString {
                    for (i in 0 until segs.length()) {
                        val part = segs.optJSONArray(i)?.optString(0).orEmpty()
                        append(part)
                    }
                }
                val detectedLang = root.optString(2, "").trim().ifBlank { "auto" }
                val idx = fullOut.indexOf(MARKER)
                val (trTitle, trBody) = if (idx >= 0) {
                    fullOut.substring(0, idx).trim() to fullOut.substring(idx + MARKER.length).trim()
                } else {
                    val lines = fullOut.lines()
                    val first = lines.firstOrNull().orEmpty().trim()
                    val rest = lines.drop(1).joinToString("\n").trim()
                    first.ifBlank { title } to rest.ifBlank { fullOut.trim() }
                }
                val sameLang = detectedLang.equals(targetLang.code, ignoreCase = true) ||
                    (trTitle == title.trim() && trBody == body.trim())
                FeedbackTranslation(
                    sourceLang = detectedLang.uppercase(),
                    targetLang = targetLang,
                    translatedTitle = trTitle.ifBlank { title },
                    translatedBody = trBody.ifBlank { body },
                    alreadyTargetLang = sameLang,
                )
            }
        } catch (e: Exception) {
            currentCoroutineContext().ensureActive()
            null
        }
    }
}

/**
 * Deterministic bug analyzer producing a plain-language explanation, likely root cause,
 * suspect Kotlin/C++ files, priority, and copy-ready AI Agent fix prompt.
 */
internal object BugTriageAnalyzer {
    private val areaRegex = Regex("""\[Area:\s*([^\]]+)]""", RegexOption.IGNORE_CASE)
    private val freqRegex = Regex("""\[Freq:\s*([^\]]+)]""", RegexOption.IGNORE_CASE)
    private val exceptionRegex = Regex("""\b([A-Za-z0-9_]+(?:Exception|Error)|SIGSEGV|SIGABRT|ANR)\b""")

    fun analyze(
        item: UserFeedbackItem,
        displayTitle: String,
        displayBody: String,
        lang: DevLanguage,
    ): BugTriageDigest {
        val rawBody = item.body
        val taggedArea = areaRegex.find(rawBody)?.groupValues?.getOrNull(1)?.trim()
        val taggedFreq = freqRegex.find(rawBody)?.groupValues?.getOrNull(1)?.trim()
        val combinedLower = "${item.title} $rawBody $displayTitle $displayBody".lowercase()
        val exceptionHint = exceptionRegex.find(rawBody)?.groupValues?.getOrNull(1)
        val hasDiagnosticsCrash = rawBody.contains("--- Diagnostics ---", ignoreCase = true) &&
            (rawBody.contains("Crash:", ignoreCase = true) || exceptionHint != null)

        val (detectedArea, suspects) = detectModuleAndFiles(taggedArea, combinedLower)
        val symptom = detectSymptom(taggedArea, combinedLower)
        val priority = when {
            hasDiagnosticsCrash || exceptionHint != null ||
                symptom == BugSymptomKind.CRASH || symptom == BugSymptomKind.AUDIO_FAIL || symptom == BugSymptomKind.FREEZE_LAG ->
                BugPriority.HIGH
            taggedFreq?.containsAny("setiap", "always", "every") == true -> BugPriority.HIGH
            item.category != FeedbackCategory.BUG -> BugPriority.LOW
            symptom == BugSymptomKind.UI_GLITCH || symptom == BugSymptomKind.LYRICS_CANVAS -> BugPriority.MEDIUM
            else -> BugPriority.MEDIUM
        }

        val steps = extractSteps(displayBody)
        val plainExplanation = buildPlainExplanation(
            lang = lang,
            area = detectedArea,
            frequency = taggedFreq,
            symptom = symptom,
            cleanLead = displayTitle.trim().trimEnd('.'),
            stepCount = steps.size,
        )
        val baseCause = buildLikelyCause(lang, symptom, detectedArea)
        val likelyCause = if (exceptionHint != null) {
            if (lang == DevLanguage.BM) {
                "Dikesan ralat $exceptionHint dalam log diagnostik. $baseCause"
            } else {
                "Detected $exceptionHint in attached diagnostics. $baseCause"
            }
        } else {
            baseCause
        }

        return BugTriageDigest(
            area = detectedArea,
            frequency = taggedFreq,
            priority = priority,
            plainExplanation = plainExplanation,
            likelyCause = likelyCause,
            suspectFiles = suspects,
            steps = steps,
        )
    }

    fun buildAiAgentPrompt(
        item: UserFeedbackItem,
        translation: FeedbackTranslation?,
        digest: BugTriageDigest,
        lang: DevLanguage,
    ): String {
        val translatedTitle = translation?.translatedTitle ?: item.title
        val translatedBody = (translation?.translatedBody ?: item.body).trim()
        val includeOriginal = translation != null && !translation.alreadyTargetLang
        val meta = buildList {
            if (item.appVersion.isNotBlank()) add("v${item.appVersion}")
            if (item.androidApi > 0) add("Android API ${item.androidApi}")
            if (item.deviceModel.isNotBlank()) add(item.deviceModel)
            if (item.contact.isNotBlank()) add("User contact: ${item.contact}")
        }
        return buildString {
            appendLine("Please investigate and fix this user bug report in the ClaudeWaves Android repository:")
            appendLine()
            appendLine("## Bug Summary (${lang.shortLabel})")
            appendLine("- Module / Area: ${digest.area} (${digest.priority.label(DevLanguage.EN)})")
            digest.frequency?.let { appendLine("- Frequency: $it") }
            appendLine("- Plain Explanation: ${digest.plainExplanation}")
            appendLine("- Likely Root Cause: ${digest.likelyCause}")
            appendLine("- Suspect Files to Inspect First: ${digest.suspectFiles.joinToString(", ")}")
            appendLine()
            appendLine("## User Report")
            appendLine("- Category: ${item.category.wireKey}")
            appendLine("- Title: $translatedTitle")
            appendLine("- Details / Steps to Reproduce:")
            appendLine(translatedBody)
            if (includeOriginal) {
                appendLine()
                appendLine("### Original User Text (${translation.sourceLang})")
                appendLine("${item.title}")
                appendLine(item.body.trim())
            }
            if (meta.isNotEmpty()) {
                appendLine()
                appendLine("## Environment")
                appendLine("- Device: ${meta.joinToString(" | ")}")
            }
            if (item.screenshots.isNotEmpty()) {
                appendLine("- Attached Screenshots: ${item.screenshots.size} screenshot(s) attached in Developer Inbox")
            }
            appendLine()
            appendLine("## Instructions for AI Agent")
            appendLine("1. Read the suspect files above and trace the exact state/lifecycle or audio/UI path causing this issue.")
            appendLine("2. Explain the root cause clearly before applying a clean, minimal fix (do not regress adjacent themes/players).")
            appendLine("3. Run `./gradlew :app:compileDebugKotlin --no-daemon` to verify the fix compiles with zero errors.")
        }.trim()
    }

    private fun extractSteps(body: String): List<String> = body
        .lines()
        .map { it.trim() }
        .filter { line ->
            line.isNotBlank() &&
                !line.startsWith("[Area:", ignoreCase = true) &&
                !line.startsWith("Steps to Reproduce", ignoreCase = true) &&
                !line.startsWith("Langkah Ulang", ignoreCase = true)
        }
        .take(4)

    private fun detectSymptom(taggedArea: String?, text: String): BugSymptomKind {
        val tagLower = taggedArea?.lowercase().orEmpty()
        return when {
            text.containsAny("crash", "force close", "keluar sendiri", "tutup sendiri", "exception", "fatal", "anr") ->
                BugSymptomKind.CRASH
            text.containsAny("no sound", "silent", "senyap", "tak bunyi", "tiada bunyi", "audio cut", "usb", "dac", "mute", "distortion", "pecah", "crackling") ->
                BugSymptomKind.AUDIO_FAIL
            text.containsAny("freeze", "hang", "lag", "stutter", "sangkut", "slow", "lambat", "stuck", "beku", "sekat") ->
                BugSymptomKind.FREEZE_LAG
            text.containsAny("potong", "clip", "tenggelam", "outline", "glitch", "overlap", "bertindih", "layout", "screen", "hilang", "hidden", "bar") ->
                BugSymptomKind.UI_GLITCH
            tagLower.contains("lyric") || text.containsAny("lyric", "lirik", "canvas", "video", "translate", "terjemah") ->
                BugSymptomKind.LYRICS_CANVAS
            tagLower.contains("download") || text.containsAny("download", "muat turun", "offline", "cache") ->
                BugSymptomKind.DOWNLOAD_OFFLINE
            tagLower.contains("playback") || tagLower.contains("mini") || text.containsAny("play", "pause", "skip", "next", "queue", "shuffle", "repeat") ->
                BugSymptomKind.PLAYBACK_CONTROL
            else -> BugSymptomKind.GENERAL
        }
    }

    private fun buildPlainExplanation(
        lang: DevLanguage,
        area: String,
        frequency: String?,
        symptom: BugSymptomKind,
        cleanLead: String,
        stepCount: Int,
    ): String {
        val freqPart = when {
            frequency.isNullOrBlank() -> ""
            lang == DevLanguage.BM -> " (berlaku: $frequency)"
            else -> " (frequency: $frequency)"
        }
        val stepPart = when {
            stepCount <= 0 -> ""
            lang == DevLanguage.BM -> " Pengguna turut sertakan $stepCount langkah ulang."
            else -> " User provided $stepCount reproduction step(s)."
        }
        return if (lang == DevLanguage.BM) {
            val what = when (symptom) {
                BugSymptomKind.CRASH -> "App tiba-tiba crash/keluar sendiri masa guna bahagian $area"
                BugSymptomKind.AUDIO_FAIL -> "Bunyi tiada, terputus, atau routing audio/USB DAC bermasalah di $area"
                BugSymptomKind.FREEZE_LAG -> "Skrin atau animasi jadi lag/sangkut/beku di bahagian $area"
                BugSymptomKind.UI_GLITCH -> "Paparan UI di $area bertindih, terpotong, atau tak kemas"
                BugSymptomKind.LYRICS_CANVAS -> "Lirik, video Canvas, atau terjemahan tak selari di $area"
                BugSymptomKind.DOWNLOAD_OFFLINE -> "Lagu muat turun/offline gagal main atau sangkut di $area"
                BugSymptomKind.PLAYBACK_CONTROL -> "Kawalan lagu/queue tak respon seperti biasa di $area"
                BugSymptomKind.GENERAL -> "Ada tingkah laku pelik dilaporkan pada bahagian $area"
            }
            "$what — \"$cleanLead\"$freqPart.$stepPart"
        } else {
            val what = when (symptom) {
                BugSymptomKind.CRASH -> "The app unexpectedly crashes/closes while using $area"
                BugSymptomKind.AUDIO_FAIL -> "Audio drops out, fails to play, or USB DAC routing misbehaves in $area"
                BugSymptomKind.FREEZE_LAG -> "UI, playback, or animations stutter/freeze in $area"
                BugSymptomKind.UI_GLITCH -> "UI elements in $area look clipped, misaligned, or overlapping"
                BugSymptomKind.LYRICS_CANVAS -> "Lyrics, Canvas video, or translation is out of sync or broken in $area"
                BugSymptomKind.DOWNLOAD_OFFLINE -> "Downloaded/offline playback or caching fails in $area"
                BugSymptomKind.PLAYBACK_CONTROL -> "Playback or queue controls do not respond properly in $area"
                BugSymptomKind.GENERAL -> "Unexpected behavior reported in $area"
            }
            "$what — \"$cleanLead\"$freqPart.$stepPart"
        }
    }

    private fun buildLikelyCause(
        lang: DevLanguage,
        symptom: BugSymptomKind,
        area: String,
    ): String = if (lang == DevLanguage.BM) {
        when (symptom) {
            BugSymptomKind.CRASH -> "Kemungkinan ralat null/state tak sah, kebenaran belum sedia, atau crash dalam thread latar belakang modul $area."
            BugSymptomKind.AUDIO_FAIL -> "Kemungkinan fokus audio, sample rate/bit-depth USB DAC, atau sink ExoPlayer/C++ DSP gagal reset bila tukar lagu/output."
            BugSymptomKind.FREEZE_LAG -> "Kemungkinan kerja berat jalan atas main UI thread, recomposition Compose berulang, atau blur/readback GPU tersekat."
            BugSymptomKind.UI_GLITCH -> "Kemungkinan padding/insets skrin, lebar container, atau semakan tema Liquid Glass/Comic/Sakuga terlepas kes tertentu."
            BugSymptomKind.LYRICS_CANVAS -> "Kemungkinan parsing API/timestamp lirik atau player video Canvas tak bersih masa tukar lagu."
            BugSymptomKind.DOWNLOAD_OFFLINE -> "Kemungkinan cache lock, URL stream tamat tempoh, atau status fail offline tak kemas kini."
            BugSymptomKind.PLAYBACK_CONTROL -> "Kemungkinan state antara MediaSession/PlayerViewModel dan UI tak selari masa tukar queue."
            BugSymptomKind.GENERAL -> "Perlu semak aliran state UI & data terkini dalam fail modul $area yang disyaki."
        }
    } else {
        when (symptom) {
            BugSymptomKind.CRASH -> "Likely unhandled null/invalid state, lifecycle race, or uncaught exception inside $area."
            BugSymptomKind.AUDIO_FAIL -> "Likely audio focus loss, USB DAC sample-rate/PCM format negotiation, or ExoPlayer/DSP sink reset issue."
            BugSymptomKind.FREEZE_LAG -> "Likely heavy work on the main thread, Compose recomposition churn, or GPU backdrop/render stall."
            BugSymptomKind.UI_GLITCH -> "Likely missing WindowInsets/padding, overflow clipping, or theme-specific (Glass/Comic/Sakuga) styling mismatch."
            BugSymptomKind.LYRICS_CANVAS -> "Likely timestamp/parser mismatch or Canvas player lifecycle leak when switching tracks."
            BugSymptomKind.DOWNLOAD_OFFLINE -> "Likely ExoPlayer cache lock contention, expired stream URL, or stale offline database status."
            BugSymptomKind.PLAYBACK_CONTROL -> "Likely desync between MediaController/MusicPlayer state and queue/UI state."
            BugSymptomKind.GENERAL -> "Check recent state changes and edge-case guards in the suspect files for $area."
        }
    }

    private fun detectModuleAndFiles(taggedArea: String?, text: String): Pair<String, List<String>> {
        val tagLower = taggedArea?.lowercase().orEmpty()
        return when {
            tagLower.contains("usb") || text.containsAny("usb", "dac", "bit-perfect", "bitperfect", "exclusive") ->
                "Audio / USB DAC" to listOf("MusicPlayerUsb.kt", "DspProcessor.cpp")
            tagLower.contains("mini") || text.containsAny("miniplayer", "mini player", "droplet", "titisan", "percikan", "queue") ->
                "MiniPlayer & Queue" to listOf("MiniPlayer.kt", "MiniPlayerLiquidDrop.kt", "QueuePanel.kt")
            tagLower.contains("lyric") || text.containsAny("lyric", "lirik", "canvas", "video", "karaoke", "translate") ->
                "Lyrics & Canvas" to listOf("CanvasArtworkPlayer.kt", "FullPlayer.kt", "LyricsTranslation.kt")
            tagLower.contains("download") || text.containsAny("download", "muat turun", "offline", "cache") ->
                "Downloads & Offline" to listOf("TrackDownloadManager.kt", "DownloadsCards.kt")
            tagLower.contains("search") || text.containsAny("search", "carian", "cari") ->
                "Search" to listOf("SearchScreen.kt")
            tagLower.contains("ui") || text.containsAny("dock", "theme", "sakuga", "comic", "glass", "popup", "update", "landscape", "ui") ->
                "UI & Navigation" to listOf("MainShell.kt", "LiquidGlass.kt", "AppUpdateDialog.kt")
            else ->
                (taggedArea ?: "Playback") to listOf("MusicPlayer.kt", "FullPlayer.kt")
        }
    }

    private fun String.containsAny(vararg needles: String): Boolean = needles.any { contains(it) }
}

/** Decodes `data:image/...;base64,...` screenshots safely off the UI thread for Developer Inbox previews. */
internal suspend fun decodeDataUriBitmap(raw: String): ImageBitmap? = withContext(Dispatchers.IO) {
    if (!raw.startsWith("data:image/", ignoreCase = true)) return@withContext null
    val comma = raw.indexOf(',')
    if (comma <= 0 || comma >= raw.lastIndex) return@withContext null
    runCatching {
        val bytes = Base64.decode(raw.substring(comma + 1), Base64.DEFAULT)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
    }.getOrNull()
}

private val screenshotHttpClient by lazy {
    OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .callTimeout(20, TimeUnit.SECONDS)
        .build()
}

private suspend fun loadScreenshotBytes(source: String): ByteArray? = withContext(Dispatchers.IO) {
    if (source.startsWith("data:image/", ignoreCase = true)) {
        val comma = source.indexOf(',')
        if (comma <= 0 || comma >= source.lastIndex) return@withContext null
        return@withContext runCatching { Base64.decode(source.substring(comma + 1), Base64.DEFAULT) }.getOrNull()
    }
    if (source.startsWith("https://", ignoreCase = true) || source.startsWith("http://", ignoreCase = true)) {
        return@withContext runCatching {
            val req = Request.Builder().url(source).get().build()
            screenshotHttpClient.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) resp.body?.bytes() else null
            }
        }.getOrNull()
    }
    null
}

internal suspend fun saveScreenshotToGallery(context: Context, source: String): Result<String> = withContext(Dispatchers.IO) {
    val bytes = loadScreenshotBytes(source)
        ?: return@withContext Result.failure(IllegalStateException("Screenshot unreadable"))
    val fileName = "ClaudeWaves_Bug_${System.currentTimeMillis()}.jpg"
    runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/ClaudeWaves")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                ?: error("MediaStore insert failed")
            resolver.openOutputStream(uri)?.use { it.write(bytes) } ?: error("Cannot write image")
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            "Pictures/ClaudeWaves/$fileName"
        } else {
            val dir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES) ?: context.filesDir
            val outDir = File(dir, "ClaudeWaves").apply { mkdirs() }
            val outFile = File(outDir, fileName)
            outFile.writeBytes(bytes)
            outFile.absolutePath
        }
    }
}

private fun writeShareImageUri(context: Context, bytes: ByteArray, index: Int): Uri {
    val dir = File(context.cacheDir, "shared").apply { mkdirs() }
    val outFile = File(dir, "dev_bug_${System.currentTimeMillis()}_$index.jpg")
    outFile.writeBytes(bytes)
    return FileProvider.getUriForFile(context, "${context.packageName}.provider", outFile)
}

internal suspend fun shareScreenshot(context: Context, source: String): Result<Unit> = withContext(Dispatchers.IO) {
    val bytes = loadScreenshotBytes(source)
        ?: return@withContext Result.failure(IllegalStateException("Screenshot unreadable"))
    runCatching {
        val uri = writeShareImageUri(context, bytes, 0)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/jpeg"
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = ClipData.newUri(context.contentResolver, "Screenshot", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, "Share Screenshot").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(chooser)
    }
}

internal suspend fun shareBugPack(
    context: Context,
    promptText: String,
    screenshots: List<String>,
): Result<Unit> = withContext(Dispatchers.IO) {
    runCatching {
        val uris = ArrayList<Uri>()
        screenshots.take(5).forEachIndexed { idx, src ->
            loadScreenshotBytes(src)?.let { bytes ->
                uris += writeShareImageUri(context, bytes, idx)
            }
        }
        val intent = when (uris.size) {
            0 -> Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, promptText)
            }
            1 -> Intent(Intent.ACTION_SEND).apply {
                type = "image/jpeg"
                putExtra(Intent.EXTRA_TEXT, promptText)
                putExtra(Intent.EXTRA_STREAM, uris.first())
                clipData = ClipData.newUri(context.contentResolver, "Bug Screenshot", uris.first())
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            else -> Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "image/jpeg"
                putExtra(Intent.EXTRA_TEXT, promptText)
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                clipData = ClipData.newUri(context.contentResolver, "Bug Screenshots", uris.first()).apply {
                    for (i in 1 until uris.size) addItem(ClipData.Item(uris[i]))
                }
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }
        val chooser = Intent.createChooser(intent, "Share Bug Pack").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(chooser)
    }
}
