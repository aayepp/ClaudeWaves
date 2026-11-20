package com.lastwave.app.data.music

import com.lastwave.app.data.lyrics.LyricLine
import kotlin.math.abs

/** One slice of YouTube's "Most replayed" heatmap. */
data class HeatMarker(val startMs: Long, val intensity: Double)

/** One chapter marker parsed from YouTube's /next response. */
data class ChapterMarker(val title: String, val startMs: Long)

/** One RMS energy sample from on-device audio analysis (fallback for instrumentals/local tracks). */
data class EnergySample(val startMs: Long, val rms: Double)

/** Combined YouTube watch signals retrieved in a single /next call. */
data class VideoBestPartSignals(
    val heatmap: List<HeatMarker> = emptyList(),
    val chapters: List<ChapterMarker> = emptyList(),
    val durationMs: Long? = null,
)

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
 * Extracts chorus / hook / drop timestamps from YouTube chapters across languages
 * (English, Malay/Indonesian "Reff", Japanese "サビ", Korean "후렴", Chinese "副歌").
 */
fun chapterChorusStarts(chapters: List<ChapterMarker>): List<Long> {
    if (chapters.isEmpty()) return emptyList()
    return chapters
        .filter { ch ->
            val t = ch.title.trim().lowercase()
            t.isNotEmpty() && !CHAPTER_EXCLUDE.containsMatchIn(t) && CHAPTER_BEST_PART.containsMatchIn(t)
        }
        .map { preRoll(it.startMs) }
        .distinct()
        .sorted()
}

private val CHAPTER_EXCLUDE = Regex("""\b(pre[-\s]?chorus|pre[-\s]?reff|intro|outro|verse|interlude|ending)\b""")
private val CHAPTER_BEST_PART = Regex(
    """\b(chorus|hook|refrain|reff|drop|climax|best\s*part)\b|サビ|후렴|副歌""",
    RegexOption.IGNORE_CASE,
)

/**
 * Where each chorus starts, with a short pre-roll so its first syllable is heard.
 * Priority:
 *  1. Apple TTML structural labels ("Chorus", "Hook", "Refrain").
 *  2. Repeated substantive lines (supports spaced languages >= 3 words AND CJK/Hangul/Kana/Thai >= 4 chars),
 *     ranked by distinct song sections (>= 20 s apart) so a real chorus beats a repetitive outro chant.
 *  3. Fuzzy near-duplicate grouping when singers vary a word between chorus rounds.
 */
fun chorusStarts(lines: List<LyricLine>): List<Long> {
    val sorted = lines.filter { it.text.isNotBlank() }.sortedBy { it.timeMs }
    if (sorted.isEmpty()) return emptyList()

    val labeled = sorted.filterIndexed { i, line -> line.isChorus() && (i == 0 || !sorted[i - 1].isChorus()) }
    if (labeled.isNotEmpty()) return labeled.map { preRoll(it.timeMs) }

    val keys = sorted.map { lyricKey(it.text) }
    val exact = detectChorusByKeys(sorted, keys)
    if (exact.isNotEmpty()) return exact

    // Fuzzy fallback: cluster near-identical lines (e.g. "Aku cinta kamu" vs "Ku cinta kamu").
    val canonical = canonicalizeFuzzyKeys(keys)
    return if (canonical != keys) detectChorusByKeys(sorted, canonical) else emptyList()
}

private fun detectChorusByKeys(sorted: List<LyricLine>, keys: List<String>): List<Long> {
    val counts = keys.groupingBy { it }.eachCount()
    val candidateKeys = keys.indices
        .filter { isSubstantiveHook(keys[it]) && counts.getValue(keys[it]) >= 2 }
        .map { keys[it] }
        .distinct()
    if (candidateKeys.isEmpty()) return emptyList()

    // Rank by number of distinct separated sections (>= CHORUS_WINDOW_MS apart) first, then total repeats,
    // so a chorus sung across 3 sections beats a chant repeated 6x back-to-back in the outro.
    fun sectionCount(key: String): Int {
        var sections = 0
        var lastMs = Long.MIN_VALUE / 4
        for (i in keys.indices) {
            if (keys[i] == key) {
                val t = sorted[i].timeMs
                if (sections == 0 || t - lastMs > CHORUS_WINDOW_MS) {
                    sections++
                    lastMs = t
                }
            }
        }
        return sections
    }

    val bestSections = candidateKeys.maxOf(::sectionCount)
    val bestCount = candidateKeys
        .filter { sectionCount(it) == bestSections }
        .maxOf { counts.getValue(it) }
    val hook = candidateKeys.first { sectionCount(it) == bestSections && counts.getValue(it) == bestCount }
    val hookCount = counts.getValue(hook)
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
                counts[keys[start - 1]] == hookCount && keys[start - 1] in leadsIn
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

/** True for lines substantial enough to anchor a chorus (>= 3 words, or >= 4 non-spaced CJK/Thai chars). */
private fun isSubstantiveHook(key: String): Boolean {
    if (key.isBlank()) return false
    if (key.split(' ').size >= 3) return true
    val compact = key.replace(" ", "")
    return compact.length >= 4 && compact.any(::isScriptWithoutSpaces)
}

private fun isScriptWithoutSpaces(ch: Char): Boolean {
    val block = Character.UnicodeBlock.of(ch)
    return block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS ||
        block == Character.UnicodeBlock.HIRAGANA ||
        block == Character.UnicodeBlock.KATAKANA ||
        block == Character.UnicodeBlock.HANGUL_SYLLABLES ||
        block == Character.UnicodeBlock.THAI
}

/** Clusters near-identical substantive lines so minor wording variations still match as one hook. */
private fun canonicalizeFuzzyKeys(keys: List<String>): List<String> {
    val representatives = mutableListOf<String>()
    return keys.map { key ->
        if (!isSubstantiveHook(key)) return@map key
        val match = representatives.firstOrNull { rep ->
            rep == key || isFuzzyLineMatch(rep, key)
        }
        if (match != null) {
            match
        } else {
            representatives.add(key)
            key
        }
    }
}

private fun isFuzzyLineMatch(a: String, b: String): Boolean {
    val lenDiff = abs(a.length - b.length)
    val maxLen = maxOf(a.length, b.length)
    if (maxLen == 0 || lenDiff > maxLen * 0.4) return false

    val tokensA = a.split(' ').filter { it.isNotEmpty() }.toSet()
    val tokensB = b.split(' ').filter { it.isNotEmpty() }.toSet()
    if (tokensA.size >= 3 && tokensB.size >= 3) {
        val inter = tokensA.intersect(tokensB).size
        val union = tokensA.union(tokensB).size
        if (union > 0 && inter.toDouble() / union >= FUZZY_TOKEN_JACCARD) return true
    }

    // Character bigram Dice coefficient (handles CJK & Malay/Indonesian affixes cleanly).
    val biA = charBigrams(a.replace(" ", ""))
    val biB = charBigrams(b.replace(" ", ""))
    if (biA.isEmpty() || biB.isEmpty()) return false
    val overlap = biA.intersect(biB).size
    return (2.0 * overlap) / (biA.size + biB.size) >= FUZZY_BIGRAM_DICE
}

private fun charBigrams(s: String): Set<String> {
    if (s.length < 2) return emptySet()
    val out = HashSet<String>(s.length)
    for (i in 0 until s.length - 1) out.add(s.substring(i, i + 2))
    return out
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
 * Finds the climax/drop moment from an on-device RMS energy curve (used when a track has
 * neither synced lyrics nor a YouTube heatmap — e.g. EDM, Phonk, instrumentals, local files).
 * Scores each window by its RMS level plus any sharp upward energy jump (beat drop),
 * then backs up slightly so the listener hears the drop arrive.
 */
fun bestPartFromEnergyCurve(samples: List<EnergySample>): Long? {
    if (samples.size < 4) return null
    val sorted = samples.sortedBy { it.startMs }
    val maxRms = sorted.maxOf { it.rms }
    if (maxRms <= 1e-4) return null

    var bestIdx = 0
    var bestScore = Double.NEGATIVE_INFINITY
    for (i in sorted.indices) {
        val normRms = sorted[i].rms / maxRms
        val prevRms = if (i > 0) sorted[i - 1].rms / maxRms else normRms
        val jump = (normRms - prevRms).coerceAtLeast(0.0)
        // Favor high sustained energy + strong transition into it (drop / chorus entry).
        val score = normRms * 0.72 + jump * 0.28
        if (score > bestScore) {
            bestScore = score
            bestIdx = i
        }
    }
    // Walk back 1 window if the preceding window is the immediate rising buildup (<= 70% of peak).
    val targetIdx = if (bestIdx > 0 && sorted[bestIdx - 1].rms < sorted[bestIdx].rms * 0.75) {
        bestIdx - 1
    } else {
        bestIdx
    }
    return sorted[targetIdx].startMs
}

/**
 * Picks the song's best part by combining all available signals:
 *  - Structural candidates: lyric choruses (Apple TTML / repeated / fuzzy), else YouTube chapters.
 *  - Crowd signal: YouTube "Most replayed" heatmap (ATV or duration-matched OMV counterpart).
 *    * Sharp viral standalone peaks (e.g. viral bridge / solo / drop far hotter than any chorus) win.
 *    * Weak/flat heatmaps fall back to the first chorus instead of picking curve noise.
 *  - Fallback: on-device audio energy climax ([audioPeakMs]) for instrumentals / EDM / local tracks.
 */
fun pickBestPart(
    choruses: List<Long>,
    heatmap: List<HeatMarker>,
    lineStarts: List<Long> = emptyList(),
    chapters: List<ChapterMarker> = emptyList(),
    audioPeakMs: Long? = null,
): Long? {
    val candidates = choruses.ifEmpty { chapterChorusStarts(chapters) }
    val hasUsableHeatmap = heatmap.size >= 10

    if (candidates.isEmpty()) {
        val peak = bestPartFromHeatmap(heatmap) ?: audioPeakMs ?: return null
        return snapToLyricLine(peak, lineStarts)
    }

    if (!hasUsableHeatmap) return candidates.first()

    // The opening tenth reads hot whatever the song, so it carries no signal.
    val body = heatmap.drop(heatmap.size / 10)
    val intensities = body.map { it.intensity }.sorted()
    val median = intensities[intensities.size / 2]
    val maxHeat = intensities.last()

    // Flat/noisy heatmap carries no real listener consensus; trust the first chorus.
    if (maxHeat - median < HEATMAP_MIN_CONTRAST) return candidates.first()

    fun windowHeat(startMs: Long): Double =
        body.filter { it.startMs >= startMs && it.startMs < startMs + CHORUS_WINDOW_MS }
            .map { it.intensity }
            .average()
            .takeUnless { it.isNaN() } ?: 0.0

    val bestChorus = candidates.maxBy(::windowHeat)
    val bestChorusHeat = windowHeat(bestChorus)

    // Viral standalone peak override: if listeners overwhelmingly replay a non-chorus moment
    // (bridge, breakdown, guitar solo, viral TikTok drop), honor that peak instead of a cooler chorus.
    val globalPeakStart = bestPartFromHeatmap(heatmap)
    if (globalPeakStart != null) {
        val outsideChoruses = candidates.none { abs(globalPeakStart - it) <= CHORUS_WINDOW_MS }
        val peakProminence = maxHeat - median
        val peakWindowHeat = windowHeat(globalPeakStart)
        if (outsideChoruses &&
            maxHeat >= VIRAL_PEAK_MIN_INTENSITY &&
            peakProminence >= VIRAL_PEAK_MIN_PROMINENCE &&
            peakWindowHeat >= bestChorusHeat + VIRAL_PEAK_MARGIN
        ) {
            return snapToLyricLine(globalPeakStart, lineStarts)
        }
    }

    return bestChorus
}

private fun snapToLyricLine(targetMs: Long, lineStarts: List<Long>): Long =
    lineStarts.lastOrNull { it in (targetMs - LINE_SNAP_MS)..targetMs }?.let(::preRoll)
        ?: lineStarts.firstOrNull { it in targetMs..(targetMs + LINE_SNAP_FORWARD_MS) }?.let(::preRoll)
        ?: targetMs

/** Span that counts as "this chorus" when scoring it and when merging repeats. */
private const val CHORUS_WINDOW_MS = 20_000L

/** Lyric stamps mark the vocal onset; starting exactly there clips the pickup. */
private const val CHORUS_PRE_ROLL_MS = 600L

/** A chorus opens at most this many lines before its hook. */
private const val MAX_LEAD_LINES = 4

/** How far back a heatmap peak may move to reach the start of a sung line. */
private const val LINE_SNAP_MS = 6_000L

/** How far forward a heatmap foot slice may snap to catch the exact opening line of a peak. */
private const val LINE_SNAP_FORWARD_MS = 2_500L

private const val FUZZY_TOKEN_JACCARD = 0.72
private const val FUZZY_BIGRAM_DICE = 0.78

/** Minimum peak-to-median contrast required before a heatmap overrides the first chorus. */
private const val HEATMAP_MIN_CONTRAST = 0.08

/** Thresholds for a standalone viral moment (bridge / solo / drop) to beat all choruses. */
private const val VIRAL_PEAK_MIN_INTENSITY = 0.85
private const val VIRAL_PEAK_MIN_PROMINENCE = 0.25
private const val VIRAL_PEAK_MARGIN = 0.18
