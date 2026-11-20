package com.lastwave.app.data.music

import com.google.common.truth.Truth.assertThat
import com.lastwave.app.data.lyrics.LyricLine
import org.junit.Test

class BestPartTest {
    private fun heatmap(vararg intensity: Double) =
        intensity.mapIndexed { i, v -> HeatMarker(startMs = i * 2_000L, intensity = v) }

    @Test
    fun heatmapStartsAtTheFootOfThePeakHill() {
        // Hot opening (ignored), then a hill rising at index 12 and peaking at 14.
        val values = DoubleArray(40) { 0.2 }.also {
            it[0] = 1.0
            it[12] = 0.7
            it[13] = 0.9
            it[14] = 1.0
            it[15] = 0.8
        }
        assertThat(bestPartFromHeatmap(heatmap(*values))).isEqualTo(12 * 2_000L)
    }

    @Test
    fun heatmapWithHighBaselineStartsAtTheChorusNotTheIntro() {
        // Shape of a real YouTube curve (one song, 100 slices of 2.04 s): hot opening,
        // a 0.6-0.7 plateau, a hill peaking at 63 s, then a slow fall.
        val curve = doubleArrayOf(
            1.00, 0.57, 0.48, 0.52, 0.57, 0.61, 0.64, 0.68, 0.70, 0.71,
            0.72, 0.70, 0.69, 0.69, 0.68, 0.69, 0.67, 0.68, 0.68, 0.68,
            0.70, 0.70, 0.71, 0.74, 0.75, 0.76, 0.78, 0.79, 0.80, 0.81,
            0.81, 0.82, 0.80, 0.76, 0.76, 0.74, 0.74, 0.73, 0.72, 0.72,
        ) + DoubleArray(60) { 0.62 - it * 0.01 }
        val markers = curve.mapIndexed { i, v -> HeatMarker(startMs = i * 2_040L, intensity = v) }
        // Peak at slice 31 (63 s): start two slices before it. Before the fix every
        // song started near 10 s, just after the skipped opening.
        assertThat(bestPartFromHeatmap(markers)).isEqualTo(29 * 2_040L)
    }

    @Test
    fun heatmapTooShortGivesNothing() {
        assertThat(bestPartFromHeatmap(heatmap(0.1, 0.9, 0.3))).isNull()
    }

    @Test
    fun appleChorusLabelsGiveEachChorusStart() {
        val lines = listOf(
            LyricLine(1_000, text = "Verse line one here", songPart = "Verse"),
            LyricLine(30_000, text = "Chorus opens up now", songPart = "Chorus"),
            LyricLine(34_000, text = "Chorus carries on", songPart = "Chorus"),
            LyricLine(50_000, text = "Second verse begins", songPart = "Verse"),
            LyricLine(80_000, text = "Chorus opens up now", songPart = "Chorus"),
        )
        // 600 ms pre-roll so the first syllable is heard.
        assertThat(chorusStarts(lines)).containsExactly(29_400L, 79_400L).inOrder()
    }

    @Test
    fun appleHookLabelCountsAsChorus() {
        val lines = listOf(
            LyricLine(1_000, text = "Verse line one here", songPart = "Verse"),
            LyricLine(30_000, text = "This is the hook", songPart = "Hook"),
            LyricLine(40_000, text = "Bridge comes in", songPart = "PreChorus"),
        )
        assertThat(chorusStarts(lines)).containsExactly(29_400L)
    }

    @Test
    fun chorusOpeningBeforeTheHookStartsAtItsFirstLine() {
        fun round(t: Long, withPre: Boolean) = buildList {
            if (withPre) add(LyricLine(t, text = "Building up before the drop"))
            add(LyricLine(t + 4_000, text = "Hey now"))
            add(LyricLine(t + 8_000, text = "Lights go down in the city"))
            add(LyricLine(t + 12_000, text = "We sing it loud tonight"))
        }
        // Verse, chorus, verse, then a double chorus.
        val lines = listOf(LyricLine(0, text = "A verse about the morning")) +
            round(20_000, withPre = true) + listOf(LyricLine(40_000, text = "Second verse of the song")) +
            round(60_000, withPre = true) + round(76_000, withPre = false)
        // The chorus opens on the short "Hey now" before the hook; the pre-chorus (2x) and
        // the previous chorus's last line stop the walk. Each starts 600 ms early.
        assertThat(chorusStarts(lines)).containsExactly(23_400L, 63_400L, 79_400L).inOrder()
    }

    @Test
    fun adLibsDoNotSplitARepeatedLine() {
        val lines = listOf(
            LyricLine(10_000, text = "Tonight we ride again (yeah!)"),
            LyricLine(50_000, text = "Tonight we ride again"),
            LyricLine(90_000, text = "[x2] tonight, we ride again"),
        )
        assertThat(chorusStarts(lines)).containsExactly(9_400L, 49_400L, 89_400L).inOrder()
    }

    @Test
    fun heatmapPeakMovesBackToTheLineSungThere() {
        val markers = (0 until 50).map { i -> HeatMarker(startMs = i * 4_000L, intensity = if (i == 30) 1.0 else 0.3) }
        // Peak slice 30 -> lead-in to slice 28 (112 s); the line sung from 110 s owns it.
        assertThat(pickBestPart(emptyList(), markers, listOf(90_000L, 110_000L, 118_000L))).isEqualTo(109_400L)
    }

    @Test
    fun withoutLabelsChorusIsWhereTheMostRepeatedLineIsSung() {
        val lines = listOf(
            LyricLine(1_000, text = "Walking down an empty street"),
            LyricLine(20_000, text = "We will rise again tonight!"),
            LyricLine(25_000, text = "Oh yeah"),
            LyricLine(28_000, text = "we will rise again tonight"),
            LyricLine(40_000, text = "Another verse is here"),
            LyricLine(60_000, text = "we will rise again tonight"),
            LyricLine(65_000, text = "Oh yeah"),
            LyricLine(66_000, text = "Oh yeah"),
        )
        // 20 s and 28 s are one chorus; 60 s is the next.
        assertThat(chorusStarts(lines)).containsExactly(19_400L, 59_400L).inOrder()
    }

    @Test
    fun lyricsWithoutRepeatsGiveNoChorus() {
        val lines = listOf(LyricLine(0, text = "one two three"), LyricLine(5_000, text = "four five six"))
        assertThat(chorusStarts(lines)).isEmpty()
    }

    @Test
    fun pickTheChorusListenersReplayMost() {
        // 50 slices of 4 s; the second chorus (120 s) is hotter than the first (40 s).
        val markers = (0 until 50).map { i ->
            HeatMarker(startMs = i * 4_000L, intensity = if (i in 30..34) 0.9 else if (i in 10..14) 0.6 else 0.3)
        }
        assertThat(pickBestPart(listOf(40_000L, 120_000L), markers)).isEqualTo(120_000L)
    }

    @Test
    fun pickFirstChorusWithoutHeatmapAndPeakWithoutChorus() {
        assertThat(pickBestPart(listOf(40_000L, 120_000L), emptyList())).isEqualTo(40_000L)
        assertThat(pickBestPart(emptyList(), emptyList())).isNull()
    }

    @Test
    fun cjkChorusWithoutSpacesIsDetected() {
        val lines = listOf(
            LyricLine(5_000, text = "静かな夜に歩いていた"),
            LyricLine(35_000, text = "君と見た星空を忘れない"),
            LyricLine(65_000, text = "新しい朝がやってくる"),
            LyricLine(95_000, text = "君と見た星空を忘れない"),
        )
        assertThat(chorusStarts(lines)).containsExactly(34_400L, 94_400L).inOrder()
    }

    @Test
    fun fuzzyChorusMatchHandlesMinorWordVariations() {
        val lines = listOf(
            LyricLine(5_000, text = "Malam sunyi berjalan sendiri"),
            LyricLine(40_000, text = "Aku cinta padamu selamanya kasih"),
            LyricLine(70_000, text = "Angin lalu membawa rindu"),
            LyricLine(105_000, text = "Ku cinta padamu selamanya kasih"),
        )
        assertThat(chorusStarts(lines)).containsExactly(39_400L, 104_400L).inOrder()
    }

    @Test
    fun realChorusAcrossSectionsBeatsRepetitiveOutroChant() {
        val lines = listOf(
            LyricLine(30_000, text = "Take me back to the summer lights"),
            LyricLine(85_000, text = "Take me back to the summer lights"),
            LyricLine(140_000, text = "Take me back to the summer lights"),
            // Outro chant repeated 5 times inside 12 seconds at the end.
            LyricLine(190_000, text = "Fade away into the dark now"),
            LyricLine(193_000, text = "Fade away into the dark now"),
            LyricLine(196_000, text = "Fade away into the dark now"),
            LyricLine(199_000, text = "Fade away into the dark now"),
            LyricLine(202_000, text = "Fade away into the dark now"),
        )
        assertThat(chorusStarts(lines)).containsExactly(29_400L, 84_400L, 139_400L).inOrder()
    }

    @Test
    fun viralStandaloneBridgePeakOverridesCoolerChoruses() {
        // 60 slices of 3 s (180 s). Choruses at 30 s and 90 s sit at 0.45;
        // a viral bridge rises at 129 s..132 s and spikes to 0.96 at 135 s..147 s.
        val markers = (0 until 60).map { i ->
            val intensity = when (i) {
                43 -> 0.55
                44 -> 0.78
                in 45..49 -> 0.96
                in 10..15, in 30..35 -> 0.45
                else -> 0.30
            }
            HeatMarker(startMs = i * 3_000L, intensity = intensity)
        }
        val result = pickBestPart(
            choruses = listOf(30_000L, 90_000L),
            heatmap = markers,
            lineStarts = listOf(30_000L, 90_000L, 128_000L),
        )
        // Peak at slice 45 (135 s) -> lead-in slice 43 (129 s) -> snapped to bridge line at 128 s minus 600 ms.
        assertThat(result).isEqualTo(127_400L)
    }

    @Test
    fun flatNoisyHeatmapFallsBackToFirstChorus() {
        val markers = (0 until 50).map { i ->
            HeatMarker(startMs = i * 4_000L, intensity = if (i in 30..34) 0.54 else 0.50)
        }
        assertThat(pickBestPart(listOf(40_000L, 120_000L), markers)).isEqualTo(40_000L)
    }

    @Test
    fun chapterMarkersProvideChorusWhenLyricsMissing() {
        val chapters = listOf(
            ChapterMarker("Intro", 0L),
            ChapterMarker("Verse 1", 15_000L),
            ChapterMarker("Pre-Chorus", 40_000L),
            ChapterMarker("Reff / Chorus", 52_000L),
            ChapterMarker("サビ", 118_000L),
        )
        assertThat(chapterChorusStarts(chapters)).containsExactly(51_400L, 117_400L).inOrder()
        assertThat(pickBestPart(emptyList(), emptyList(), chapters = chapters)).isEqualTo(51_400L)
    }

    @Test
    fun audioEnergyCurveDetectsDropClimaxAndBacksUpToArrival() {
        val samples = listOf(
            EnergySample(30_000L, 0.15),
            EnergySample(40_000L, 0.20),
            EnergySample(50_000L, 0.35), // buildup
            EnergySample(60_000L, 0.92), // drop!
            EnergySample(70_000L, 0.88),
            EnergySample(80_000L, 0.40),
        )
        // Peak at 60 s with buildup at 50 s (< 75% of peak) -> starts at 50 s to catch the drop arrival.
        assertThat(bestPartFromEnergyCurve(samples)).isEqualTo(50_000L)
        assertThat(pickBestPart(emptyList(), emptyList(), audioPeakMs = 50_000L)).isEqualTo(50_000L)
    }
}
