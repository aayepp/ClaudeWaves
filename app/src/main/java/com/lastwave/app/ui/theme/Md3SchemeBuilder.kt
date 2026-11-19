package com.lastwave.app.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Rebuilds the app's dark Material You scheme from a single source hue,
 * mirroring `_applyMaterialYouScheme()` / `_applyMonochromeScheme()` in the
 * legacy app.js (which itself hand-rolled dynamicDarkColorScheme()-style
 * tonal roles in CSS HSL space). This is intentionally NOT the same
 * algorithm as Android's real HCT-based dynamic color — using that here
 * would shift every color relationship in the app and count as a redesign.
 * This is a literal, faithful port so nothing has to be re-approved visually.
 */
object Md3SchemeBuilder {

    /** Chroma levels (CSS HSL %), match app.js constants exactly. */
    private const val CHROMA_PRIMARY = 30
    private const val CHROMA_SECONDARY = 14
    private const val CHROMA_TERTIARY = 22
    private const val CHROMA_NEUTRAL = 5
    private const val CHROMA_NEUTRAL_VARIANT = 8

    /** Extracts just the hue (0-360) from a hex color — the only component
     *  app.js's _hexToHsl() result that _applyMaterialYouScheme() actually uses. */
    fun hueOf(hex: String): Int {
        val clean = hex.removePrefix("#")
        if (clean.length < 6) return 0
        val rgb = clean.take(6).toIntOrNull(16) ?: return 0
        val r = ((rgb ushr 16) and 0xFF) / 255f
        val g = ((rgb ushr 8) and 0xFF) / 255f
        val b = (rgb and 0xFF) / 255f
        val max = maxOf(r, g, b)
        val min = minOf(r, g, b)
        if (max == min) return 0
        val d = max - min
        val h = when (max) {
            r -> ((g - b) / d + (if (g < b) 6f else 0f))
            g -> ((b - r) / d + 2f)
            else -> ((r - g) / d + 4f)
        } / 6f
        return (h * 360f).roundToInt().let { ((it % 360) + 360) % 360 }
    }

    /** HSL saturation (0-1) of a hex colour; 0 for anything unparseable. */
    private fun saturationOf(hex: String): Float {
        val rgb = hex.removePrefix("#").take(6).toIntOrNull(16) ?: return 0f
        val r = ((rgb ushr 16) and 0xFF) / 255f
        val g = ((rgb ushr 8) and 0xFF) / 255f
        val b = (rgb and 0xFF) / 255f
        val max = maxOf(r, g, b)
        val min = minOf(r, g, b)
        val span = 1f - abs(max + min - 1f)
        return if (span <= 0f) 0f else (max - min) / span
    }

    /** True for grey/black/white seeds: their hue is meaningless ([hueOf] gives 0, red). */
    fun isAchromatic(hex: String): Boolean = saturationOf(hex) < 0.1f

    /** Extra chroma a fully saturated cover adds on top of the fixed levels. */
    private const val MAX_ARTWORK_CHROMA_BOOST = 20

    /**
     * (dark, light) schemes for now-playing artwork. Fixed chroma and a
     * hue+60 tertiary made every cover the same pastel: the cover's own
     * saturation now sets how vivid the accents are, and its second colour,
     * when it has one, is the tertiary. Grey covers get the monochrome scheme.
     */
    fun buildArtworkSchemes(seed: String, second: String?, amoled: Boolean): Pair<ColorScheme, ColorScheme> {
        if (isAchromatic(seed)) return buildMonochromeDarkScheme(amoled) to buildMonochromeLightScheme()
        val h = hueOf(seed)
        val hT = second?.let(::hueOf) ?: ((h + 60) % 360)
        val boost = ((saturationOf(seed) * 100).roundToInt() - CHROMA_PRIMARY).coerceIn(0, MAX_ARTWORK_CHROMA_BOOST)
        // Light primaries sit at 40% lightness under white text: half the boost keeps that readable.
        return build(h, h, hT, h, amoled, chromaBoost = boost) to buildLight(h, h, hT, h, chromaBoost = boost / 2)
    }

    /** Full dynamic dark scheme, seeded from [hex]'s hue. Matches _applyMaterialYouScheme(). */
    @Suppress("UNUSED_PARAMETER")
    fun buildScheme(hex: String, amoled: Boolean, liquidGlass: Boolean = false): ColorScheme =
        buildDarkScheme(hex, amoled, liquidGlass)

    /** Dark scheme, seeded from [hex]'s hue. */
    fun buildDarkScheme(hex: String, amoled: Boolean, liquidGlass: Boolean = false): ColorScheme {
        val h = hueOf(hex)
        val hT = (h + 60) % 360
        return build(
            primaryHue = h,
            secondaryHue = h,
            tertiaryHue = hT,
            neutralHue = h,
            amoled = amoled,
        )
    }

    /** Full dynamic light scheme, seeded from [hex]'s hue. */
    @Suppress("UNUSED_PARAMETER")
    fun buildLightScheme(hex: String, liquidGlass: Boolean = false): ColorScheme {
        val h = hueOf(hex)
        val hT = (h + 60) % 360
        return buildLight(
            primaryHue = h,
            secondaryHue = h,
            tertiaryHue = hT,
            neutralHue = h,
        )
    }

    /** Pure grayscale dark scheme (all chroma = 0). Matches _applyMonochromeScheme(). */
    @Suppress("UNUSED_PARAMETER")
    fun buildMonochromeScheme(amoled: Boolean, liquidGlass: Boolean = false): ColorScheme =
        buildMonochromeDarkScheme(amoled, liquidGlass)

    /**
     * Monochrome dark scheme: black canvas, white accent, Apple's system greys
     * for the raised surfaces (the palette BitChord uses).
     */
    fun buildMonochromeDarkScheme(amoled: Boolean, @Suppress("UNUSED_PARAMETER") liquidGlass: Boolean = false): ColorScheme = darkColorScheme(
        primary = Color.White,
        onPrimary = Color.Black,
        primaryContainer = Color(0xFF2C2C2E),
        onPrimaryContainer = Color.White,
        secondary = Color(0xFFD1D1D6),
        onSecondary = Color.Black,
        secondaryContainer = Color(0xFF2C2C2E),
        onSecondaryContainer = Color.White,
        tertiary = Color(0xFFAEAEB2),
        onTertiary = Color.Black,
        tertiaryContainer = Color(0xFF3A3A3C),
        onTertiaryContainer = Color.White,
        error = Color(0xFFFF453A),
        onError = Color.White,
        errorContainer = Color(0xFF5C1512),
        onErrorContainer = Color(0xFFFFD9D6),
        background = Color.Black,
        onBackground = Color.White,
        surface = Color.Black,
        onSurface = Color.White,
        surfaceContainerLowest = Color.Black,
        surfaceContainerLow = if (amoled) Color.Black else Color(0xFF0D0D0F),
        surfaceContainer = if (amoled) Color(0xFF0D0D0F) else Color(0xFF1C1C1E),
        surfaceContainerHigh = if (amoled) Color(0xFF1C1C1E) else Color(0xFF2C2C2E),
        surfaceContainerHighest = if (amoled) Color(0xFF2C2C2E) else Color(0xFF3A3A3C),
        surfaceVariant = Color(0xFF1C1C1E),
        onSurfaceVariant = Color(0xFF8E8E93),
        outline = Color(0xFF48484A),
        outlineVariant = Color(0xFF2C2C2E),
        inverseSurface = Color.White,
        inverseOnSurface = Color.Black,
        inversePrimary = Color.Black,
        scrim = Color.Black,
    )

    /** Monochrome light scheme: white canvas, black accent, Apple's light greys. */
    @Suppress("UNUSED_PARAMETER")
    fun buildMonochromeLightScheme(liquidGlass: Boolean = false): ColorScheme = androidx.compose.material3.lightColorScheme(
        primary = Color.Black,
        onPrimary = Color.White,
        primaryContainer = Color(0xFFE5E5EA),
        onPrimaryContainer = Color.Black,
        secondary = Color(0xFF3A3A3C),
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFE5E5EA),
        onSecondaryContainer = Color.Black,
        tertiary = Color(0xFF636366),
        onTertiary = Color.White,
        tertiaryContainer = Color(0xFFD1D1D6),
        onTertiaryContainer = Color.Black,
        error = Color(0xFFFF3B30),
        onError = Color.White,
        errorContainer = Color(0xFFFFDAD6),
        onErrorContainer = Color(0xFF410002),
        background = Color.White,
        onBackground = Color.Black,
        surface = Color.White,
        onSurface = Color.Black,
        surfaceContainerLowest = Color.White,
        surfaceContainerLow = Color(0xFFF7F7F9),
        surfaceContainer = Color(0xFFF2F2F7),
        surfaceContainerHigh = Color(0xFFEBEBF0),
        surfaceContainerHighest = Color(0xFFE5E5EA),
        surfaceVariant = Color(0xFFF2F2F7),
        onSurfaceVariant = Color(0xFF6E6E73),
        outline = Color(0xFFC7C7CC),
        outlineVariant = Color(0xFFE5E5EA),
        inverseSurface = Color.Black,
        inverseOnSurface = Color.White,
        inversePrimary = Color.White,
        scrim = Color.Black,
    )

    /**
     * Comic Pop, dark: ink-purple page, cream line work, yellow / pink / cyan
     * spot colours. [accent] (the user's accent scheme) replaces the primary roles only.
     */
    fun buildComicDarkScheme(accent: ColorScheme? = null): ColorScheme = darkColorScheme(
        primary = Color(0xFFFFD60A),
        onPrimary = Color(0xFF16141F),
        primaryContainer = Color(0xFF3B2A8C),
        onPrimaryContainer = Color(0xFFFFF1CC),
        secondary = Color(0xFFFF4D8D),
        onSecondary = Color(0xFF16141F),
        secondaryContainer = Color(0xFFA3184F),
        onSecondaryContainer = Color(0xFFFFF1CC),
        tertiary = Color(0xFF3DD6F5),
        onTertiary = Color(0xFF16141F),
        tertiaryContainer = Color(0xFF0B6E86),
        onTertiaryContainer = Color(0xFFFFF1CC),
        error = Color(0xFFFF6B5E),
        onError = Color(0xFF16141F),
        errorContainer = Color(0xFF6B1B14),
        onErrorContainer = Color(0xFFFFDAD6),
        background = Color(0xFF16141F),
        onBackground = Color(0xFFFFF1CC),
        surface = Color(0xFF16141F),
        onSurface = Color(0xFFFFF1CC),
        surfaceContainerLowest = Color(0xFF100E18),
        surfaceContainerLow = Color(0xFF1E1B2A),
        surfaceContainer = Color(0xFF262335),
        surfaceContainerHigh = Color(0xFF2E2A40),
        surfaceContainerHighest = Color(0xFF38334D),
        // Not equal to any surfaceContainer role: contentColorFor() checks surfaceVariant
        // first, so a card on surfaceContainerHigh got the dim variant colour for its titles.
        surfaceVariant = Color(0xFF2F2B42),
        onSurfaceVariant = Color(0xFFCFC6E3),
        outline = Color(0xFFFFF1CC),
        outlineVariant = Color(0xFF6B6485),
        inverseSurface = Color(0xFFFFF6DC),
        inverseOnSurface = Color(0xFF111111),
        inversePrimary = Color(0xFFE0281E),
        scrim = Color.Black,
    ).withPrimaryOf(accent)

    /** Comic Pop, light: cream paper, black ink, red / yellow / cyan spot colours. */
    fun buildComicLightScheme(accent: ColorScheme? = null): ColorScheme = androidx.compose.material3.lightColorScheme(
        // Deep enough for small red text on cream to pass 4.5:1.
        primary = Color(0xFFD22018),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFFFD60A),
        onPrimaryContainer = Color(0xFF111111),
        secondary = Color(0xFF111111),
        onSecondary = Color(0xFFFFD60A),
        secondaryContainer = Color(0xFF2EC4F1),
        onSecondaryContainer = Color(0xFF111111),
        tertiary = Color(0xFF0B6FA4),
        onTertiary = Color.White,
        tertiaryContainer = Color(0xFFFFB3C7),
        onTertiaryContainer = Color(0xFF111111),
        error = Color(0xFFB3261E),
        onError = Color.White,
        errorContainer = Color(0xFFFFDAD6),
        onErrorContainer = Color(0xFF410002),
        background = Color(0xFFFFF6DC),
        onBackground = Color(0xFF111111),
        surface = Color(0xFFFFF6DC),
        onSurface = Color(0xFF111111),
        surfaceContainerLowest = Color.White,
        surfaceContainerLow = Color(0xFFFFFBEE),
        surfaceContainer = Color.White,
        surfaceContainerHigh = Color.White,
        surfaceContainerHighest = Color(0xFFFFEFC2),
        // Kept apart from surfaceContainerHighest for the same contentColorFor() reason as at night.
        surfaceVariant = Color(0xFFFFEEBF),
        onSurfaceVariant = Color(0xFF3D372C),
        outline = Color(0xFF111111),
        outlineVariant = Color(0xFF8A8170),
        inverseSurface = Color(0xFF111111),
        inverseOnSurface = Color(0xFFFFF6DC),
        inversePrimary = Color(0xFFFFD60A),
        scrim = Color.Black,
    ).withPrimaryOf(accent)

    /** Paper, ink and the other spot colours stay Comic Pop; only the accent role changes. */
    private fun ColorScheme.withPrimaryOf(accent: ColorScheme?): ColorScheme = if (accent == null) this else copy(
        primary = accent.primary,
        onPrimary = accent.onPrimary,
        primaryContainer = accent.primaryContainer,
        onPrimaryContainer = accent.onPrimaryContainer,
        inversePrimary = accent.inversePrimary,
        surfaceTint = accent.primary,
    )

    /**
     * LastWave Signature Dark Scheme built specifically around the official app logo colors:
     * Electric Lime (#C9FB00) and Deep Obsidian Teal (#012226).
     */
    fun buildLastWaveDarkScheme(amoled: Boolean, @Suppress("UNUSED_PARAMETER") liquidGlass: Boolean = false): ColorScheme {
        val brandLime = Color(0xFFC9FB00)
        val darkTeal = Color(0xFF012226)
        val deepTeal = Color(0xFF01181B)

        val backgroundCol = if (amoled) Color.Black else deepTeal
        val surfaceCol = if (amoled) Color.Black else darkTeal
        val surfaceLowCol = if (amoled) Color.Black else Color(0xFF011E22)
        val surfaceLowestCol = if (amoled) Color.Black else Color(0xFF001214)

        return darkColorScheme(
            primary = brandLime,
            onPrimary = darkTeal,
            primaryContainer = Color(0xFF063E45),
            onPrimaryContainer = Color(0xFFD6FF38),

            secondary = Color(0xFF38E5B7),
            onSecondary = darkTeal,
            secondaryContainer = Color(0xFF06474F),
            onSecondaryContainer = Color(0xFF67F8D8),

            tertiary = Color(0xFFA6FF38),
            onTertiary = Color(0xFF1B3800),
            tertiaryContainer = Color(0xFF0C464C),
            onTertiaryContainer = brandLime,

            error = Color(0xFFFFB4AB),
            errorContainer = Color(0xFF93000A),
            onErrorContainer = Color(0xFFFFDAD6),
            onError = Color(0xFF690005),

            background = backgroundCol,
            onBackground = Color(0xFFE4F8F8),
            surface = surfaceCol,
            onSurface = Color(0xFFE4F8F8),
            surfaceContainer = Color(0xFF02272C),
            surfaceContainerHigh = Color(0xFF04333A),
            surfaceContainerHighest = Color(0xFF073F47),
            surfaceContainerLow = surfaceLowCol,
            surfaceContainerLowest = surfaceLowestCol,
            surfaceVariant = Color(0xFF063B42),
            onSurfaceVariant = Color(0xFFA1CACE),

            outline = Color(0xFF23656E),
            outlineVariant = Color(0xFF0A444C),

            inverseSurface = Color(0xFFE4F8F8),
            inverseOnSurface = darkTeal,
            inversePrimary = Color(0xFF486300),
            scrim = Color.Black,
        )
    }

    /**
     * LastWave Signature Light Scheme built around the official app logo colors.
     */
    fun buildLastWaveLightScheme(@Suppress("UNUSED_PARAMETER") liquidGlass: Boolean = false): ColorScheme {
        val brandLime = Color(0xFFC9FB00)
        val darkTeal = Color(0xFF012226)

        return androidx.compose.material3.lightColorScheme(
            primary = Color(0xFF486300),
            onPrimary = Color.White,
            primaryContainer = Color(0xFFE0FE5B),
            onPrimaryContainer = darkTeal,

            secondary = Color(0xFF014D56),
            onSecondary = Color.White,
            secondaryContainer = Color(0xFF9FF2E2),
            onSecondaryContainer = darkTeal,

            tertiary = Color(0xFF2C6855),
            onTertiary = Color.White,
            tertiaryContainer = Color(0xFFB8F9A5),
            onTertiaryContainer = Color(0xFF0E3800),

            error = Color(0xFFBA1A1A),
            errorContainer = Color(0xFFFFDAD6),
            onErrorContainer = Color(0xFF410002),
            onError = Color.White,

            background = Color(0xFFF2FBF7),
            onBackground = darkTeal,
            surface = Color(0xFFF2FBF7),
            onSurface = darkTeal,
            surfaceContainer = Color(0xFFE0F1EC),
            surfaceContainerHigh = Color(0xFFD6EAE5),
            surfaceContainerHighest = Color(0xFFCCE4DE),
            surfaceContainerLow = Color(0xFFEAF6F2),
            surfaceContainerLowest = Color.White,
            surfaceVariant = Color(0xFFD6ECE8),
            onSurfaceVariant = Color(0xFF1E464B),

            outline = Color(0xFF5A8E89),
            outlineVariant = Color(0xFFB0D5D0),

            inverseSurface = darkTeal,
            inverseOnSurface = Color(0xFFE4F8F8),
            inversePrimary = brandLime,
            scrim = Color.Black,
        )
    }

    private fun build(
        primaryHue: Int,
        secondaryHue: Int,
        tertiaryHue: Int,
        neutralHue: Int,
        amoled: Boolean,
        chromaBoost: Int = 0,
    ): ColorScheme {
        val cP = CHROMA_PRIMARY + chromaBoost
        val cS = CHROMA_SECONDARY + chromaBoost / 2
        val cT = CHROMA_TERTIARY + chromaBoost / 2
        val cN = CHROMA_NEUTRAL
        val cNV = CHROMA_NEUTRAL_VARIANT

        val bgL = if (amoled) 0 else 6
        val sc1L = if (amoled) 4 else 11
        val sc2L = if (amoled) 8 else 16
        val sc3L = if (amoled) 12 else 20

        val backgroundCol = if (amoled) Color.Black else hsl(neutralHue, cN, bgL)
        val surfaceCol = if (amoled) Color.Black else hsl(neutralHue, cN, bgL)
        val surfaceLowCol = if (amoled) Color.Black else hsl(neutralHue, cN, bgL)
        val surfaceLowestCol = if (amoled) Color.Black else hsl(neutralHue, cN, (bgL - 2).coerceAtLeast(0))

        val scheme = darkColorScheme(
            primary = hsl(primaryHue, cP, 82),
            onPrimary = hsl(primaryHue, cP, 16),
            primaryContainer = hsl(primaryHue, cP, if (amoled) 24 else 30),
            onPrimaryContainer = hsl(primaryHue, (cP - 5).coerceAtLeast(0), 90),

            secondary = hsl(secondaryHue, cS, 80),
            onSecondary = hsl(secondaryHue, cS, 16),
            secondaryContainer = hsl(secondaryHue, cS, if (amoled) 22 else 28),
            onSecondaryContainer = hsl(secondaryHue, (cS - 4).coerceAtLeast(0), 90),

            tertiary = hsl(tertiaryHue, cT, 80),
            onTertiary = hsl(tertiaryHue, cT, 16),
            tertiaryContainer = hsl(tertiaryHue, cT, if (amoled) 22 else 28),
            onTertiaryContainer = hsl(tertiaryHue, (cT - 6).coerceAtLeast(0), 90),

            error = hsl(0, 45, 80),
            errorContainer = hsl(0, 30, 25),
            onErrorContainer = hsl(0, 25, 88),
            onError = hsl(0, 45, 16),

            background = backgroundCol,
            onBackground = hsl(neutralHue, cNV, 90),
            surface = surfaceCol,
            onSurface = hsl(neutralHue, cNV, 90),
            surfaceContainer = hsl(neutralHue, cN, sc1L),
            surfaceContainerHigh = hsl(neutralHue, cN, sc2L),
            surfaceContainerHighest = hsl(neutralHue, cN, sc3L),
            surfaceContainerLow = surfaceLowCol,
            surfaceContainerLowest = surfaceLowestCol,
            surfaceVariant = hsl(neutralHue, cNV, sc2L + 4),
            onSurfaceVariant = hsl(neutralHue, cNV, 80),

            outline = hsl(neutralHue, cNV, 60),
            outlineVariant = hsl(neutralHue, cNV, 28),

            inverseSurface = hsl(neutralHue, cNV, 90),
            inverseOnSurface = hsl(neutralHue, cNV, 20),
            inversePrimary = hsl(primaryHue, cP, 40),
            scrim = Color.Black,
        )

        return scheme
    }

    private fun buildLight(
        primaryHue: Int,
        secondaryHue: Int,
        tertiaryHue: Int,
        neutralHue: Int,
        chromaBoost: Int = 0,
    ): ColorScheme {
        val cP = CHROMA_PRIMARY + 15 + chromaBoost
        val cS = CHROMA_SECONDARY + 8 + chromaBoost / 2
        val cT = CHROMA_TERTIARY + 12 + chromaBoost / 2
        val cN = 4
        val cNV = 6

        return androidx.compose.material3.lightColorScheme(
            primary = hsl(primaryHue, cP, 40),
            onPrimary = Color.White,
            primaryContainer = hsl(primaryHue, cP, 90),
            onPrimaryContainer = hsl(primaryHue, cP, 10),

            secondary = hsl(secondaryHue, cS, 40),
            onSecondary = Color.White,
            secondaryContainer = hsl(secondaryHue, cS, 90),
            onSecondaryContainer = hsl(secondaryHue, cS, 10),

            tertiary = hsl(tertiaryHue, cT, 40),
            onTertiary = Color.White,
            tertiaryContainer = hsl(tertiaryHue, cT, 90),
            onTertiaryContainer = hsl(tertiaryHue, cT, 10),

            error = hsl(0, 70, 40),
            errorContainer = hsl(0, 70, 90),
            onErrorContainer = hsl(0, 70, 12),
            onError = Color.White,

            background = hsl(neutralHue, cN, 98),
            onBackground = hsl(neutralHue, cNV, 10),
            surface = hsl(neutralHue, cN, 98),
            onSurface = hsl(neutralHue, cNV, 10),
            surfaceContainerLowest = Color.White,
            surfaceContainerLow = hsl(neutralHue, cN, 96),
            surfaceContainer = hsl(neutralHue, cN, 94),
            surfaceContainerHigh = hsl(neutralHue, cN, 92),
            surfaceContainerHighest = hsl(neutralHue, cN, 90),
            surfaceVariant = hsl(neutralHue, cNV, 90),
            onSurfaceVariant = hsl(neutralHue, cNV, 30),

            outline = hsl(neutralHue, cNV, 50),
            outlineVariant = hsl(neutralHue, cNV, 80),

            inverseSurface = hsl(neutralHue, cNV, 20),
            inverseOnSurface = hsl(neutralHue, cNV, 95),
            inversePrimary = hsl(primaryHue, cP, 80),
            scrim = Color.Black,
        )
    }

    internal fun contrast(a: Color, b: Color): Double {
        val la = a.luminance() + 0.05
        val lb = b.luminance() + 0.05
        return maxOf(la, lb) / minOf(la, lb)
    }

    /**
     * HSL(h in 0-360, s in 0-100, l in 0-100) -> Compose Color.
     * Standard conversion — must match browsers' CSS hsl() exactly so every
     * token lines up with the values app.css previously rendered.
     */
    private fun hsl(h: Int, s: Int, l: Int): Color {
        val hf = ((h % 360) + 360) % 360 / 360f
        val sf = (s.coerceIn(0, 100)) / 100f
        val lf = (l.coerceIn(0, 100)) / 100f

        if (sf == 0f) return Color(lf, lf, lf)

        val q = if (lf < 0.5f) lf * (1 + sf) else lf + sf - lf * sf
        val p = 2 * lf - q

        fun hueToRgb(t: Float): Float {
            var tt = t
            if (tt < 0f) tt += 1f
            if (tt > 1f) tt -= 1f
            return when {
                tt < 1f / 6f -> p + (q - p) * 6f * tt
                tt < 1f / 2f -> q
                tt < 2f / 3f -> p + (q - p) * (2f / 3f - tt) * 6f
                else -> p
            }
        }

        val r = hueToRgb(hf + 1f / 3f)
        val g = hueToRgb(hf)
        val b = hueToRgb(hf - 1f / 3f)
        return Color(r, g, b)
    }
}

/** Kept for readability at call sites that only care about hue distance. */
internal fun hueDelta(a: Int, b: Int): Int = abs(a - b).let { minOf(it, 360 - it) }
