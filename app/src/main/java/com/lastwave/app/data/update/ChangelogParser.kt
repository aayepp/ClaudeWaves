package com.lastwave.app.data.update

/**
 * One release section parsed from bundled `CHANGELOG.md`.
 */
data class ChangelogEntry(
    val version: String,
    val date: String? = null,
    val bodyMarkdown: String,
)

private val VERSION_HEADING = Regex("""^##\s+\[?v?([^\]\s]+)\]?(?:\s*[-–—]\s*(.+))?\s*$""")

/**
 * Splits Keep-a-Changelog style Markdown (`## [x.y.z] - YYYY-MM-DD`) into
 * ordered [ChangelogEntry] items, newest first.
 */
internal fun parseChangelogMarkdown(markdown: String): List<ChangelogEntry> {
    if (markdown.isBlank()) return emptyList()
    val entries = mutableListOf<ChangelogEntry>()
    var currentVersion: String? = null
    var currentDate: String? = null
    val buffer = mutableListOf<String>()

    fun flush() {
        val version = currentVersion ?: return
        val body = buffer.joinToString("\n").trim()
        if (body.isNotEmpty()) {
            entries += ChangelogEntry(
                version = version,
                date = currentDate?.takeIf { it.isNotBlank() },
                bodyMarkdown = body,
            )
        }
        buffer.clear()
    }

    markdown.replace("\r\n", "\n").lineSequence().forEach { rawLine ->
        val match = VERSION_HEADING.matchEntire(rawLine.trimEnd())
        if (match != null) {
            flush()
            currentVersion = match.groupValues[1].trim()
            currentDate = match.groupValues.getOrNull(2)?.trim()?.ifBlank { null }
        } else if (currentVersion != null) {
            buffer += rawLine
        }
    }
    flush()
    return entries
}

internal fun findChangelogEntryForVersion(
    entries: List<ChangelogEntry>,
    versionName: String,
): ChangelogEntry? {
    val cleanTarget = versionName.trim().removePrefix("v").removePrefix("V")
    return entries.firstOrNull {
        it.version.trim().removePrefix("v").removePrefix("V").equals(cleanTarget, ignoreCase = true)
    } ?: entries.firstOrNull()
}
