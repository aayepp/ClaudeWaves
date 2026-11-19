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
}
