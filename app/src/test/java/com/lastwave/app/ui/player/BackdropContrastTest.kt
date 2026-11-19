package com.lastwave.app.ui.player

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BackdropContrastTest {

    @Test
    fun lightBackdropDarkensBrightColorsBelowMaxLuminance() {
        val yellowAdjusted = Color(0xFFFFD60A).ensureVisibleOnBackdrop(darkBackdrop = false)
        val whiteAdjusted = Color.White.ensureVisibleOnBackdrop(darkBackdrop = false)

        assertTrue(ColorUtils.calculateLuminance(yellowAdjusted.toArgb()) <= 0.36)
        assertTrue(ColorUtils.calculateLuminance(whiteAdjusted.toArgb()) <= 0.36)
    }

    @Test
    fun lightBackdropPreservesAlreadyDarkAccentColor() {
        val crimson = Color(0xFFD22018)
        assertEquals(crimson, crimson.ensureVisibleOnBackdrop(darkBackdrop = false))
    }

    @Test
    fun darkBackdropBrightensDarkColorAboveMinLuminance() {
        val deepNavy = Color(0xFF0A1433)
        val adjusted = deepNavy.ensureVisibleOnBackdrop(darkBackdrop = true, minLuminance = 0.38f)

        assertTrue(ColorUtils.calculateLuminance(adjusted.toArgb()) >= 0.38)
    }
}
