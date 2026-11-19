package com.lastwave.app.data.music

import com.lastwave.app.data.lyrics.LyricLine

/** One slice of YouTube's "Most replayed" heatmap. */
data class HeatMarker(val startMs: Long, val intensity: Double)

/**
 * Start of the most replayed moment: a short lead-in before the peak slice, never
 * reaching back past the valley that precedes it. YouTube's curve sits at 0.6-0.8 for
 * most of a song, so walking back to "60% of the peak" covered nearly all of it and
 * every song started a few seconds in. The opening tenth is skipped: every play counts
 * there, so it reads hot whatever the song.
 */
fun bestPartFromHeatmap(markers: List<HeatMarker>): Long? {
    if (markers.size < 10) return null
    val body = markers.drop(markers.size / 10)
    val peak = body.indices.maxByOrNull { body[it].intensity } ?: return null
    var foot = peak
    while (foot > 0 && body[foot - 1].intensity <= body[foot].intensity + SLOPE_NOISE) foot--
    return body[maxOf(foot, peak - LEAD_IN_SLICES)].startMs
}

/** Wobble in YouTube's smoothed curve that still counts as the same rising slope. */
private const val SLOPE_NOISE = 0.005

/** ~4 s on a typical 100-slice curve: enough to hear the chorus arrive. */
private const val LEAD_IN_SLICES = 2

/**
 * Where each chorus starts, with a short pre-roll so its first syllable is heard.
 * Apple's TTML labels its blocks ("Chorus", "Hook", "Refrain"): the song's real
 * structure. Without labels, the hook is the most repeated line of three words or
 * more ("oh", "yeah yeah" repeat everywhere); when the chorus opens on another line,
 * back up over lines sung exactly as often as the hook. They belong to the same block,
 * while a pre-chorus usually skips the last round and so stops the walk.
 */
fun chorusStarts(lines: List<LyricLine>): List<Long> {
    val sorted = lines.filter { it.text.isNotBlank() }.sortedBy { it.timeMs }
    val labeled = sorted.filterIndexed { i, line -> line.isChorus() && (i == 0 || !sorted[i - 1].isChorus()) }
    if (labeled.isNotEmpty()) return labeled.map { preRoll(it.timeMs) }
    val keys = sorted.map { lyricKey(it.text) }
    val counts = keys.groupingBy { it }.eachCount()
    val top = keys.filter { it.split(' ').size >= 3 }.maxOfOrNull { counts.getValue(it) }?.takeIf { it >= 2 }
        ?: return emptyList()
    val hook = keys.first { it.split(' ').size >= 3 && counts[it] == top }
    val hookAt = keys.indices.filter { keys[it] == hook }
    // Only lines that always lead into the hook open a chorus. A chorus's closing line is
    // sung as often but not always followed by the hook: walking over it in a double
    // chorus merged the second chorus into the first.
    val leadsIn = keys.indices.groupBy { keys[it] }
        .filterValues { at -> at.all { j -> hookAt.any { it > j && it - j <= MAX_LEAD_LINES } } }.keys
    return hookAt
        .map { i ->
            var start = i
            while (start > 0 && i - start < MAX_LEAD_LINES && keys[start - 1] != hook &&
                counts[keys[start - 1]] == top && keys[start - 1] in leadsIn
            ) start--
            sorted[start].timeMs to (start < i)
        }
        // The hook twice inside one chorus is still one chorus; one with its own opening
        // line is a new chorus however close (a double chorus).
        .fold(emptyList<Long>()) { starts, (t, opens) ->
            if (starts.isEmpty() || opens || t - starts.last() > CHORUS_WINDOW_MS) starts + t else starts
        }
        .map(::preRoll)
}

/** Lyric text for comparing lines: ad-libs "(yeah)", tags "[x2]" and punctuation dropped. */
private fun lyricKey(text: String): String = text.lowercase()
    .replace(Regex("""\([^)]*\)|\[[^]]*]"""), " ")
    .replace(Regex("""[^\p{L}\p{N} ]"""), "")
    .trim()
    .replace(Regex("""\s+"""), " ")

private fun LyricLine.isChorus(): Boolean {
    val part = songPart?.lowercase() ?: return false
    return part.startsWith("chorus") || part.startsWith("hook") || part.startsWith("refrain")
}

private fun preRoll(ms: Long) = (ms - CHORUS_PRE_ROLL_MS).coerceAtLeast(0L)

/**
 * The best part is a chorus: the one YouTube listeners replay most (mean heat over its
 * first [CHORUS_WINDOW_MS]), else the first. With no chorus known, the heatmap peak,
 * moved back to the lyric line sung there so it opens on a phrase, not mid-word.
 */
fun pickBestPart(choruses: List<Long>, heatmap: List<HeatMarker>, lineStarts: List<Long> = emptyList()): Long? {
    if (choruses.isEmpty()) {
        val peak = bestPartFromHeatmap(heatmap) ?: return null
        return lineStarts.lastOrNull { it in (peak - LINE_SNAP_MS)..peak }?.let(::preRoll) ?: peak
    }
    if (heatmap.size < 10) return choruses.first()
    // The opening tenth reads hot whatever the song, so it carries no signal.
    val body = heatmap.drop(heatmap.size / 10)
    return choruses.maxBy { start ->
        body.filter { it.startMs >= start && it.startMs < start + CHORUS_WINDOW_MS }
            .map { it.intensity }.average().takeUnless { it.isNaN() } ?: 0.0
    }
}

/** Span that counts as "this chorus" when scoring it and when merging repeats. */
private const val CHORUS_WINDOW_MS = 20_000L

/** Lyric stamps mark the vocal onset; starting exactly there clips the pickup. */
private const val CHORUS_PRE_ROLL_MS = 600L

/** A chorus opens at most this many lines before its hook. */
private const val MAX_LEAD_LINES = 4

/** How far back a heatmap peak may move to reach the start of a sung line. */
private const val LINE_SNAP_MS = 6_000L
