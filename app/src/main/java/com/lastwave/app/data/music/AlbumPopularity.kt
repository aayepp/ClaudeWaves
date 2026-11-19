package com.lastwave.app.data.music

/**
 * YouTube Music album rows carry a localized play count ("1.2M plays", "1,2 jt kali diputar",
 * "120万回再生"). Only the order within one album matters, so the unit just has to be known;
 * an unknown unit returns null and the album shows no stars rather than wrong ones.
 */
internal fun parsePlayCount(text: String): Long? {
    val match = Regex("""^(\d[\d.,   ]*)\s*(\S*)""").find(text.trim()) ?: return null
    val digits = match.groupValues[1].trim().replace(" ", "").replace(" ", "").replace(" ", "")
    val token = match.groupValues[2].lowercase().trimEnd('.')
    val cjk = token.firstOrNull()?.let { CJK_UNITS[it] }
    val multiplier = cjk ?: UNITS[token] ?: 1L
    if (multiplier == 1L) {
        // No unit: separators are thousands, so "1,234 plays" is 1234. A bare
        // number with no noun after it is not a play count.
        if (token.isEmpty()) return null
        return digits.filter(Char::isDigit).toLongOrNull()
    }
    val value = digits.replace(',', '.').toDoubleOrNull() ?: return null
    return (value * multiplier).toLong()
}

/** Up to three tracks with at least half the top track's plays; none for singles or flat albums. */
internal fun popularVideoIds(plays: Map<String, Long>): Set<String> {
    if (plays.size < 3) return emptySet()
    val top = plays.values.max()
    if (top <= 0) return emptySet()
    return plays.entries
        .filter { it.value * 2 >= top }
        .sortedByDescending { it.value }
        .take(3)
        .takeIf { it.size < plays.size }
        ?.mapTo(mutableSetOf()) { it.key }
        .orEmpty()
}

private val UNITS = mapOf(
    "k" to 1_000L, "rb" to 1_000L, "ribu" to 1_000L, "mil" to 1_000L, "tsd" to 1_000L, "тыс" to 1_000L,
    "m" to 1_000_000L, "jt" to 1_000_000L, "juta" to 1_000_000L, "mio" to 1_000_000L, "mln" to 1_000_000L,
    "млн" to 1_000_000L, "mn" to 1_000_000L, "tr" to 1_000_000L, "mi" to 1_000_000L,
    "b" to 1_000_000_000L, "bn" to 1_000_000_000L, "md" to 1_000_000_000L, "mrd" to 1_000_000_000L,
    "млрд" to 1_000_000_000L, "bi" to 1_000_000_000L,
)

private val CJK_UNITS = mapOf(
    '千' to 1_000L, '천' to 1_000L, '万' to 10_000L, '萬' to 10_000L, '만' to 10_000L,
    '亿' to 100_000_000L, '億' to 100_000_000L, '억' to 100_000_000L,
)

/** Album rows put the play count in their last flex column (after title and artist). */
internal fun albumPopularVideoIds(root: kotlinx.serialization.json.JsonElement): Set<String> {
    val rows = mutableListOf<kotlinx.serialization.json.JsonObject>()
    collectObjects(root, "musicResponsiveListItemRenderer", rows)
    val plays = rows.mapNotNull { row ->
        val videoId = row.obj("playlistItemData")?.string("videoId") ?: return@mapNotNull null
        val columns = row.array("flexColumns")?.takeIf { it.size >= 3 } ?: return@mapNotNull null
        val text = columns.last().obj("musicResponsiveListItemFlexColumnRenderer")?.obj("text")
            ?.array("runs")?.joinToString("") { it.string("text").orEmpty() }
            ?: return@mapNotNull null
        parsePlayCount(text)?.let { videoId to it }
    }.toMap()
    return popularVideoIds(plays)
}
