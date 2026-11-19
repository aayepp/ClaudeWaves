@file:OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)

package com.lastwave.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.lastwave.app.R

/**
 * Inter, bundled as a variable font (res/font/inter.ttf, SIL OFL): axes wght
 * 100-900 and opsz 14-32. Each text style gets a family cut at its own optical
 * size, holding every weight step, so `.copy(fontWeight = ...)` at a call site
 * draws a real cut instead of a synthesised bold.
 *
 * The weight ladder is the Apple Music one BitChord uses: Bold titles, Semibold
 * section heads, Medium labels, Regular body, with large sizes tracked tight.
 */
private fun inter(opticalSize: Float, boost: Float = 0f): FontFamily = FontFamily(
    listOf(FontWeight.Normal, FontWeight.Medium, FontWeight.SemiBold, FontWeight.Bold, FontWeight.ExtraBold).map { weight ->
        Font(
            R.font.inter,
            weight = weight,
            variationSettings = FontVariation.Settings(
                FontVariation.weight((weight.weight + boost.toInt()).coerceAtMost(900)),
                FontVariation.Setting("opsz", opticalSize.coerceIn(14f, 32f)),
            ),
        )
    },
)

/** One fixed cut, for text that names its stroke on the wght axis directly (lyrics). */
private fun interCut(weight: Float, opticalSize: Float): FontFamily = FontFamily(
    Font(
        R.font.inter,
        weight = FontWeight.Bold,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(weight.toInt().coerceAtMost(900)),
            FontVariation.Setting("opsz", opticalSize.coerceIn(14f, 32f)),
        ),
    ),
)

/**
 * Settings "Use Bold Font": how much heavier (on the wght axis) every app-font
 * cut is drawn. The axis value, not the requested FontWeight, is what sets the
 * stroke here, so this is the knob that actually thickens the text.
 */
const val BOLD_FONT_BOOST = 120f

private val LyricsLeadFontFamily: FontFamily = interCut(weight = 700f, opticalSize = 32f)
private val LyricsBackingFontFamily: FontFamily = interCut(weight = 650f, opticalSize = 24f)
private val LyricsSubLineFontFamily: FontFamily = interCut(weight = 520f, opticalSize = 16f)
private val LyricsLeadBoldFontFamily: FontFamily = interCut(weight = 700f + BOLD_FONT_BOOST, opticalSize = 32f)
private val LyricsBackingBoldFontFamily: FontFamily = interCut(weight = 650f + BOLD_FONT_BOOST, opticalSize = 24f)
private val LyricsSubLineBoldFontFamily: FontFamily = interCut(weight = 520f + BOLD_FONT_BOOST, opticalSize = 16f)

fun lyricsLeadFontFamily(bold: Boolean): FontFamily = if (bold) LyricsLeadBoldFontFamily else LyricsLeadFontFamily
fun lyricsBackingFontFamily(bold: Boolean): FontFamily = if (bold) LyricsBackingBoldFontFamily else LyricsBackingFontFamily
fun lyricsSubLineFontFamily(bold: Boolean): FontFamily = if (bold) LyricsSubLineBoldFontFamily else LyricsSubLineFontFamily

// boost = 0 is exactly the tag 2.0 type scale; regular trims it, bold mode adds on top.
private fun lastWaveTypography(boost: Float) = Typography(
    displayLarge = TextStyle(fontFamily = inter(32f, boost), fontWeight = FontWeight.Bold, fontSize = 57.sp, lineHeight = 64.sp, letterSpacing = (-1.2).sp),
    displayMedium = TextStyle(fontFamily = inter(32f, boost), fontWeight = FontWeight.Bold, fontSize = 45.sp, lineHeight = 52.sp, letterSpacing = (-0.9).sp),
    displaySmall = TextStyle(fontFamily = inter(32f, boost), fontWeight = FontWeight.Bold, fontSize = 36.sp, lineHeight = 44.sp, letterSpacing = (-0.6).sp),
    headlineLarge = TextStyle(fontFamily = inter(32f, boost), fontWeight = FontWeight.Bold, fontSize = 32.sp, lineHeight = 40.sp, letterSpacing = (-0.5).sp),
    headlineMedium = TextStyle(fontFamily = inter(28f, boost), fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 36.sp, letterSpacing = (-0.4).sp),
    headlineSmall = TextStyle(fontFamily = inter(24f, boost), fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 32.sp, letterSpacing = (-0.3).sp),
    titleLarge = TextStyle(fontFamily = inter(22f, boost), fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp, letterSpacing = (-0.25).sp),
    titleMedium = TextStyle(fontFamily = inter(16f, boost), fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = (-0.1).sp),
    titleSmall = TextStyle(fontFamily = inter(14f, boost), fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontFamily = inter(16f, boost), fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = inter(15f, boost), fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 22.sp),
    bodySmall = TextStyle(fontFamily = inter(14f, boost), fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontFamily = inter(14f, boost), fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontFamily = inter(14f, boost), fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp),
    labelSmall = TextStyle(fontFamily = inter(14f, boost), fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 16.sp),
)

// Regular app font sits a little lighter than the 2.0 values (wght axis); bold mode is unchanged.
const val REGULAR_FONT_TRIM = -60f
val LastWaveTypography = lastWaveTypography(REGULAR_FONT_TRIM)
val LastWaveBoldTypography = lastWaveTypography(BOLD_FONT_BOOST)

/** Bangers (SIL OFL), one cut: registered at every weight so a Bold request is not faux-bolded. */
internal val Bangers = FontFamily(
    listOf(FontWeight.Normal, FontWeight.Medium, FontWeight.SemiBold, FontWeight.Bold, FontWeight.ExtraBold)
        .map { Font(R.font.bangers, it) },
)

private fun TextStyle.comic() = copy(fontFamily = Bangers, letterSpacing = 0.5.sp)

/** Comic Pop: display, headline and large-title roles set in Bangers; body text untouched. */
fun Typography.withComicHeadings(): Typography = copy(
    displayLarge = displayLarge.comic(),
    displayMedium = displayMedium.comic(),
    displaySmall = displaySmall.comic(),
    headlineLarge = headlineLarge.comic(),
    headlineMedium = headlineMedium.comic(),
    headlineSmall = headlineSmall.comic(),
    titleLarge = titleLarge.comic(),
)

/**
 * Aliased typography maintaining backward compatibility so both bold and regular states
 * accurately preserve the authentic typography architecture from the initial release (tag 2.0).
 */
val LastWaveThinTypography = LastWaveTypography

/**
 * The "Use Application Font" toggle's off-state: plain platform default
 * (FontFamily.Default), same sizes/weights as [LastWaveTypography] so
 * turning the toggle off changes ONLY the typeface, not the whole scale.
 */
val SystemTypography = Typography(
    displayLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 57.sp, lineHeight = 64.sp),
    displayMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 45.sp, lineHeight = 52.sp),
    displaySmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 36.sp, lineHeight = 44.sp),
    headlineLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold, fontSize = 32.sp, lineHeight = 40.sp),
    headlineMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 36.sp),
    headlineSmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, lineHeight = 32.sp),
    titleLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 22.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 24.sp),
    titleSmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 22.sp),
    bodySmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp),
    labelSmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 16.sp),
)

/**
 * The "Use Application Font" off-state when "Use Bold Font" is enabled:
 * thickens the platform default font family across all Material 3 tiers.
 */
val SystemBoldTypography = Typography(
    displayLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold, fontSize = 57.sp, lineHeight = 64.sp),
    displayMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold, fontSize = 45.sp, lineHeight = 52.sp),
    displaySmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold, fontSize = 36.sp, lineHeight = 44.sp),
    headlineLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.ExtraBold, fontSize = 32.sp, lineHeight = 40.sp),
    headlineMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.ExtraBold, fontSize = 28.sp, lineHeight = 36.sp),
    headlineSmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 32.sp),
    titleLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold, fontSize = 16.sp, lineHeight = 24.sp),
    titleSmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 15.sp, lineHeight = 22.sp),
    bodySmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold, fontSize = 14.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold, fontSize = 12.sp, lineHeight = 16.sp),
    labelSmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold, fontSize = 11.sp, lineHeight = 16.sp),
)
