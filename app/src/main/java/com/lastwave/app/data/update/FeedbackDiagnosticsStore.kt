package com.lastwave.app.data.update

import android.content.Context
import com.lastwave.app.diagnostics.AppLog
import com.lastwave.app.diagnostics.CrashRecorder
import com.lastwave.app.diagnostics.CrashType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

data class FeedbackDraft(
    val categoryKey: String = FeedbackCategory.BUG.wireKey,
    val bugAreaKey: String = "Playback",
    val bugFreqKey: String = "Always",
    val title: String = "",
    val body: String = "",
    val contact: String = "",
    val includeDevice: Boolean = true,
    val includeDiagnostics: Boolean = true,
)

data class SubmittedFeedbackReceipt(
    val id: Long,
    val categoryWire: String,
    val title: String,
    val submittedAtMs: Long,
)

data class PendingCrashReport(
    val fileName: String,
    val crashType: String,
    val headline: String,
    val compactTrace: String,
    val recordedAtMs: Long,
)

data class SettingsDeepLink(
    val tab: String? = null,
    val openFeedback: Boolean = false,
    val timestampMs: Long = System.currentTimeMillis(),
)

/**
 * Local storage and sanitization helper for Feedback drafts, local ticket receipts,
 * opt-in diagnostic snapshots, post-crash prompts, and internal Settings deep links.
 */
@Singleton
class FeedbackDiagnosticsStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _pendingSettingsDeepLink = MutableStateFlow<SettingsDeepLink?>(null)
    val pendingSettingsDeepLink: StateFlow<SettingsDeepLink?> = _pendingSettingsDeepLink.asStateFlow()

    fun requestSettingsDeepLink(tab: String?, openFeedback: Boolean) {
        _pendingSettingsDeepLink.value = SettingsDeepLink(
            tab = tab?.trim()?.lowercase()?.ifBlank { null },
            openFeedback = openFeedback,
        )
    }

    fun consumeSettingsDeepLink() {
        _pendingSettingsDeepLink.value = null
    }

    fun loadDraft(): FeedbackDraft = FeedbackDraft(
        categoryKey = prefs.getString(KEY_DRAFT_CATEGORY, null) ?: FeedbackCategory.BUG.wireKey,
        bugAreaKey = prefs.getString(KEY_DRAFT_AREA, null) ?: "Playback",
        bugFreqKey = prefs.getString(KEY_DRAFT_FREQ, null) ?: "Always",
        title = prefs.getString(KEY_DRAFT_TITLE, null).orEmpty(),
        body = prefs.getString(KEY_DRAFT_BODY, null).orEmpty(),
        contact = prefs.getString(KEY_DRAFT_CONTACT, null).orEmpty(),
        includeDevice = prefs.getBoolean(KEY_DRAFT_INCLUDE_DEVICE, true),
        includeDiagnostics = prefs.getBoolean(KEY_DRAFT_INCLUDE_DIAG, true),
    )

    fun saveDraft(draft: FeedbackDraft) {
        prefs.edit()
            .putString(KEY_DRAFT_CATEGORY, draft.categoryKey)
            .putString(KEY_DRAFT_AREA, draft.bugAreaKey)
            .putString(KEY_DRAFT_FREQ, draft.bugFreqKey)
            .putString(KEY_DRAFT_TITLE, draft.title.take(120))
            .putString(KEY_DRAFT_BODY, draft.body.take(2000))
            .putString(KEY_DRAFT_CONTACT, draft.contact.take(80))
            .putBoolean(KEY_DRAFT_INCLUDE_DEVICE, draft.includeDevice)
            .putBoolean(KEY_DRAFT_INCLUDE_DIAG, draft.includeDiagnostics)
            .apply()
    }

    fun clearDraft() {
        prefs.edit()
            .remove(KEY_DRAFT_CATEGORY)
            .remove(KEY_DRAFT_AREA)
            .remove(KEY_DRAFT_FREQ)
            .remove(KEY_DRAFT_TITLE)
            .remove(KEY_DRAFT_BODY)
            .remove(KEY_DRAFT_CONTACT)
            .remove(KEY_DRAFT_INCLUDE_DEVICE)
            .remove(KEY_DRAFT_INCLUDE_DIAG)
            .apply()
    }

    fun recordSubmission(id: Long, category: FeedbackCategory, title: String) {
        val current = getRecentSubmissions().toMutableList()
        current.add(
            0,
            SubmittedFeedbackReceipt(
                id = id,
                categoryWire = category.wireKey,
                title = title.trim().take(120),
                submittedAtMs = System.currentTimeMillis(),
            ),
        )
        val trimmed = current.distinctBy { it.id to it.title }.take(MAX_RECEIPTS)
        val arr = JSONArray()
        trimmed.forEach { item ->
            arr.put(
                JSONObject().apply {
                    put("id", item.id)
                    put("cat", item.categoryWire)
                    put("title", item.title)
                    put("ts", item.submittedAtMs)
                },
            )
        }
        prefs.edit().putString(KEY_RECENT_RECEIPTS, arr.toString()).apply()
    }

    fun getRecentSubmissions(): List<SubmittedFeedbackReceipt> {
        val raw = prefs.getString(KEY_RECENT_RECEIPTS, null).orEmpty()
        if (raw.isBlank()) return emptyList()
        val arr = runCatching { JSONArray(raw) }.getOrNull() ?: return emptyList()
        return buildList {
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val title = obj.optString("title", "").trim()
                if (title.isEmpty()) continue
                add(
                    SubmittedFeedbackReceipt(
                        id = obj.optLong("id", 0L),
                        categoryWire = obj.optString("cat", FeedbackCategory.FEEDBACK.wireKey),
                        title = title,
                        submittedAtMs = obj.optLong("ts", 0L),
                    ),
                )
            }
        }
    }

    /**
     * Checks whether the app recorded a real crash (exception, native crash, ANR, or
     * excessive resource use) within the last 48 hours that has not yet been offered
     * for one-tap bug reporting.
     */
    fun findUnpromptedRecentCrash(nowMs: Long = System.currentTimeMillis()): PendingCrashReport? {
        val crashFiles = runCatching { CrashRecorder.crashFiles(context) }.getOrNull().orEmpty()
        val alreadyPrompted = prefs.getString(KEY_LAST_PROMPTED_CRASH, null)

        for (file in crashFiles) {
            val ageMs = nowMs - file.lastModified()
            if (ageMs > RECENT_CRASH_MAX_AGE_MS) break
            if (ageMs < 0L) continue
            if (alreadyPrompted == file.name) return null

            val rawText = runCatching { file.readText(Charsets.UTF_8) }.getOrNull().orEmpty()
            if (rawText.isBlank()) continue

            val lines = rawText.lines()
            val crashType = parseCrashType(lines) ?: continue
            if (crashType.lowercase() !in PROMPTABLE_CRASH_TYPES) continue

            val exceptionIndex = lines.indexOfFirst { it.trim().equals("--- exception ---", ignoreCase = true) }
            val detailIndex = lines.indexOfFirst { it.trim().equals("--- detail ---", ignoreCase = true) }
            val startIndex = when {
                exceptionIndex >= 0 -> exceptionIndex + 1
                detailIndex >= 0 -> detailIndex + 1
                else -> 0
            }
            val traceLines = lines.drop(startIndex)
                .takeWhile { line ->
                    val t = line.trim()
                    !t.equals("--- other threads ---", ignoreCase = true) &&
                        !t.startsWith("--- log lead-up", ignoreCase = true) &&
                        !t.equals("--- end ---", ignoreCase = true)
                }
                .map { sanitizeLine(it) }
                .filter { it.isNotBlank() }
                .take(10)

            val headline = traceLines.firstOrNull()?.take(90)?.ifBlank { crashType } ?: crashType
            val compactTrace = traceLines.joinToString("\n").take(600)

            return PendingCrashReport(
                fileName = file.name,
                crashType = crashType,
                headline = headline,
                compactTrace = compactTrace,
                recordedAtMs = file.lastModified(),
            )
        }
        return null
    }

    private fun parseCrashType(lines: List<String>): String? {
        for (line in lines) {
            if (line.startsWith("Crash type", ignoreCase = true)) {
                val parsed = line.substringAfter(':', "").trim()
                if (parsed.isNotEmpty()) return parsed
            }
        }
        return null
    }

    fun markCrashPromptHandled(fileName: String) {
        prefs.edit().putString(KEY_LAST_PROMPTED_CRASH, fileName).apply()
    }

    /**
     * Builds a compact, redacted diagnostics block (runtime flags + latest real crash header +
     * recent warnings/errors) capped safely at [MAX_SNAPSHOT_CHARS] so it fits inside `/feedback`.
     */
    fun buildCompactDiagnosticsSnapshot(extraSummary: String = ""): String {
        val sb = StringBuilder()
        if (extraSummary.isNotBlank()) {
            sb.appendLine(sanitizeLine(extraSummary))
        }
        val now = System.currentTimeMillis()
        val crashFiles = runCatching { CrashRecorder.crashFiles(context) }.getOrNull().orEmpty()
        val unknownMarkerLabel = CrashType.UNKNOWN_TERMINATION.label
        for (crashFile in crashFiles) {
            if (now - crashFile.lastModified() > CRASH_INCLUDE_MAX_AGE_MS) break
            val crashLines = runCatching { crashFile.readText(Charsets.UTF_8).lines() }.getOrNull().orEmpty()
            val parsedType = parseCrashType(crashLines)
            if (parsedType != null && parsedType.equals(unknownMarkerLabel, ignoreCase = true)) continue
            val typeLine = crashLines.firstOrNull { it.startsWith("Crash type", ignoreCase = true) }?.trim()
            val excIdx = crashLines.indexOfFirst { it.trim().equals("--- exception ---", ignoreCase = true) }
            val excSnippet = if (excIdx >= 0) {
                crashLines.drop(excIdx + 1).filter { it.isNotBlank() }.take(5)
            } else {
                emptyList()
            }
            if (typeLine != null || excSnippet.isNotEmpty()) {
                sb.appendLine("Crash: ${sanitizeLine(typeLine ?: crashFile.name)}")
                excSnippet.forEach { sb.appendLine(sanitizeLine(it).take(140)) }
            }
            break
        }

        val tailEntries = runCatching { AppLog.tail?.snapshot(20).orEmpty() }.getOrDefault(emptyList())
        if (tailEntries.isNotEmpty()) {
            val formatted = tailEntries.map { sanitizeLine(it.toString()) }
            val severe = formatted.filter { it.contains(" W/") || it.contains(" E/") || it.contains("WARN") || it.contains("ERROR") }
            val picked = (severe.takeLast(4) + formatted.takeLast(4)).distinct().take(6)
            if (picked.isNotEmpty()) {
                sb.appendLine("Logs:")
                picked.forEach { sb.appendLine(it.take(120)) }
            }
        }
        return sb.toString().trim().take(MAX_SNAPSHOT_CHARS)
    }

    companion object {
        private const val PREFS_NAME = "lastwave_feedback_local"
        private const val KEY_DRAFT_CATEGORY = "draft_category"
        private const val KEY_DRAFT_AREA = "draft_area"
        private const val KEY_DRAFT_FREQ = "draft_freq"
        private const val KEY_DRAFT_TITLE = "draft_title"
        private const val KEY_DRAFT_BODY = "draft_body"
        private const val KEY_DRAFT_CONTACT = "draft_contact"
        private const val KEY_DRAFT_INCLUDE_DEVICE = "draft_include_device"
        private const val KEY_DRAFT_INCLUDE_DIAG = "draft_include_diag"
        private const val KEY_RECENT_RECEIPTS = "recent_receipts"
        private const val KEY_LAST_PROMPTED_CRASH = "last_prompted_crash"
        private const val MAX_RECEIPTS = 10
        private const val MAX_SNAPSHOT_CHARS = 750
        private const val RECENT_CRASH_MAX_AGE_MS = 48L * 60L * 60L * 1000L
        private const val CRASH_INCLUDE_MAX_AGE_MS = 7L * 24L * 60L * 60L * 1000L

        private val PROMPTABLE_CRASH_TYPES = setOf(
            CrashType.UNCAUGHT_EXCEPTION.label.lowercase(),
            CrashType.NATIVE_CRASH.label.lowercase(),
            CrashType.ANR.label.lowercase(),
            CrashType.EXCESSIVE_RESOURCE_USE.label.lowercase(),
        )

        private val PAT_REGEX = Regex("""(github_pat_|ghp_|gho_|ghu_|ghs_)[A-Za-z0-9_]+""")
        private val BEARER_REGEX = Regex("""(?i)\b(Bearer|Basic)\s+[A-Za-z0-9\-._~+/]+=*""")
        private val SECRET_KV_REGEX = Regex("""(?i)\b(token|cookie|authorization|password|secret|session|api[_-]?key)\s*[:=]\s*\S+""")
        private val QUERY_SECRET_REGEX = Regex("""\?[^\s)\]]+""")

        internal fun sanitizeLine(raw: String): String = raw
            .replace(PAT_REGEX, "[REDACTED_TOKEN]")
            .replace(BEARER_REGEX, "$1 [REDACTED]")
            .replace(SECRET_KV_REGEX, "$1=[REDACTED]")
            .replace(QUERY_SECRET_REGEX, "?[REDACTED]")
            .trim()
    }
}
