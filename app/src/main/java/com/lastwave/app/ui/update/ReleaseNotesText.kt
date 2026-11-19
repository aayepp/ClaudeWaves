package com.lastwave.app.ui.update

import com.lastwave.app.ui.theme.LocalComicTheme
import com.lastwave.app.ui.theme.ComicAccent
import com.lastwave.app.ui.theme.ComicInk
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp

/**
 * GitHub release notes (Markdown) as styled text for the update dialog.
 *
 * Covers `#` headings, `-`/`*`/`+` bullets (nested by indent), `**bold**`,
 * `` `code` ``, `[links](url)` (tappable), horizontal rules, Markdown tables,
 * and GitHub `> [!NOTE]` callouts so raw table pipes or alert tags never leak
 * into the UI.
 */
internal fun releaseNotesText(markdown: String, style: ReleaseNotesStyle = ReleaseNotesStyle.Plain): AnnotatedString =
    buildAnnotatedString {
        val linkColor = style.link
        val rawLines = markdown.replace("\r\n", "\n").lines()
        val cleanedLines = sanitizeMarkdownLines(rawLines)
            .dropWhile { it.isBlank() }
            .dropLastWhile { it.isBlank() }

        var previousBlank = false
        cleanedLines.forEachIndexed { index, raw ->
            val line = raw.trimEnd()
            val trimmed = line.trimStart()
            if (trimmed.isEmpty()) {
                if (!previousBlank && index > 0) append('\n')
                previousBlank = true
                return@forEachIndexed
            }
            if (index > 0 && !previousBlank) append('\n')
            previousBlank = false
            when {
                HORIZONTAL_RULE.matches(trimmed) -> append("―――")
                trimmed.startsWith("#") && sectionLabel(trimmed, style.isDark) != null -> {
                    val (bg, ink) = sectionLabel(trimmed, style.isDark)!!
                    withStyle(SpanStyle(background = bg, color = ink, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, letterSpacing = 0.8.sp)) {
                        append(" ${trimmed.trimStart('#').trim().uppercase()} ")
                    }
                }
                trimmed.startsWith("#") -> {
                    val level = trimmed.takeWhile { it == '#' }.length
                    val headingColor = if (level >= 3 && linkColor != Color.Unspecified) linkColor else Color.Unspecified
                    withStyle(
                        SpanStyle(
                            color = headingColor,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = if (level <= 2) 16.sp else 14.sp,
                        ),
                    ) {
                        appendInline(trimmed.drop(level).trim(), style)
                    }
                }
                BULLET.containsMatchIn(trimmed) -> {
                    val depth = (line.length - trimmed.length) / 2
                    append("    ".repeat(depth.coerceAtMost(3)))
                    withStyle(SpanStyle(color = linkColor, fontWeight = FontWeight.Bold)) {
                        append("•  ")
                    }
                    appendInline(trimmed.drop(2).trim(), style)
                }
                trimmed.startsWith(">") -> {
                    val quoteBody = trimmed.dropWhile { it == '>' || it == ' ' }
                    if (quoteBody.isNotEmpty()) {
                        appendInline(quoteBody, style)
                    }
                }
                else -> appendInline(trimmed, style)
            }
        }
    }

private fun sanitizeMarkdownLines(lines: List<String>): List<String> {
    val result = mutableListOf<String>()
    for (raw in lines) {
        val trimmed = raw.trim()
        // Drop GitHub alert tags like `> [!NOTE]`, `> [!TIP]`, etc.
        if (GITHUB_ALERT_TAG.matches(trimmed)) continue
        // Drop Markdown table alignment rows like `| :--- | :--- |` and header row `| Package | Supported Android | Description |`
        if (TABLE_ALIGNMENT_ROW.matches(trimmed)) continue
        if (TABLE_HEADER_ROW.matches(trimmed)) continue
        // Convert Markdown table data rows `| col1 | col2 | col3 |` into clean bullet items
        if (trimmed.startsWith("|") && trimmed.endsWith("|") && trimmed.count { it == '|' } >= 3) {
            val cells = trimmed.trim('|').split('|').map { it.trim() }.filter { it.isNotEmpty() }
            if (cells.isNotEmpty()) {
                result.add("- " + cells.joinToString("  •  "))
                continue
            }
        }
        result.add(raw)
    }
    return result
}

private fun AnnotatedString.Builder.appendInline(text: String, style: ReleaseNotesStyle) {
    val linkColor = style.link
    var cursor = 0
    for (match in INLINE.findAll(text)) {
        append(text.substring(cursor, match.range.first))
        val (bold, boldAlt, code, label, url) = match.destructured
        when {
            match.value.startsWith("!") -> Unit
            bold.isNotEmpty() || boldAlt.isNotEmpty() -> {
                // Each change leads with a bold title; a glow that follows the letters makes it the thing you read.
                val glow = style.glow.takeIf { it != Color.Unspecified }?.let { Shadow(it, Offset.Zero, style.glowRadiusPx) }
                withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = style.highlightInk, shadow = glow)) {
                    append(bold.ifEmpty { boldAlt }.replace("`", ""))
                }
            }
            code.isNotEmpty() ->
                withStyle(SpanStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold)) { append(code) }
            label.isNotEmpty() ->
                withLink(
                    LinkAnnotation.Url(
                        url,
                        TextLinkStyles(SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline)),
                    ),
                ) { append(label) }
            else -> append(match.value)
        }
        cursor = match.range.last + 1
    }
    append(text.substring(cursor))
}

/** Colours for release notes, taken from the theme so every theme and mode reads well. */
@Immutable
internal data class ReleaseNotesStyle(
    val link: Color,
    /** Halo drawn around the bold change titles, following their letters. */
    val glow: Color,
    val glowRadiusPx: Float,
    val highlightInk: Color,
    val isDark: Boolean,
) {
    companion object {
        /** No colours: plain bold titles, used where no theme is at hand. */
        val Plain = ReleaseNotesStyle(Color.Unspecified, Color.Unspecified, 0f, Color.Unspecified, isDark = false)
    }
}

@Composable
internal fun releaseNotesStyle(): ReleaseNotesStyle {
    val c = MaterialTheme.colorScheme
    val isDark = c.surface.luminance() < 0.5f
    val comic = LocalComicTheme.current
    return ReleaseNotesStyle(
        link = c.primary,
        // Comic Pop glows in its yellow, a highlighter that follows the letters.
        glow = if (comic) ComicAccent else c.primary.copy(alpha = if (isDark) 0.85f else 0.6f),
        glowRadiusPx = with(LocalDensity.current) { (if (comic) 5.dp else 6.dp).toPx() },
        highlightInk = if (comic && !isDark) ComicInk else c.onPrimaryContainer,
        isDark = isDark,
    )
}

/**
 * Keep a Changelog section headings as coloured labels (background to ink),
 * so the kind of change reads before its text. Null for any other heading.
 */
private fun sectionLabel(heading: String, isDark: Boolean): Pair<Color, Color>? =
    when (heading.trimStart('#').trim().lowercase()) {
        "added" -> if (isDark) Color(0xFF1E4D2B) to Color(0xFF9BE6AF) else Color(0xFFD4F5DD) to Color(0xFF0B5A26)
        "changed" -> if (isDark) Color(0xFF5A3F0E) to Color(0xFFFFD08A) else Color(0xFFFFE8C2) to Color(0xFF7A4B00)
        "fixed" -> if (isDark) Color(0xFF1C3A66) to Color(0xFFA8C8FF) else Color(0xFFD6E6FF) to Color(0xFF0B3D86)
        "removed", "deprecated" -> if (isDark) Color(0xFF5C1B17) to Color(0xFFFFB4AB) else Color(0xFFFFDAD6) to Color(0xFF8C1D18)
        "security" -> if (isDark) Color(0xFF4A2A63) to Color(0xFFE2C1FF) else Color(0xFFF0DBFF) to Color(0xFF4F1F7A)
        else -> null
    }

private val BULLET = Regex("""^[-*+]\s""")
private val HORIZONTAL_RULE = Regex("""^([-*_])\1{2,}$""")
private val GITHUB_ALERT_TAG = Regex("""^>\s*\[!(NOTE|TIP|IMPORTANT|WARNING|CAUTION)]\s*$""", RegexOption.IGNORE_CASE)
private val TABLE_ALIGNMENT_ROW = Regex("""^\|[\s:|-]+\|$""")
private val TABLE_HEADER_ROW = Regex("""^\|\s*Package\s*\|\s*Supported Android\s*\|\s*Description\s*\|$""", RegexOption.IGNORE_CASE)
private val INLINE = Regex("""!\[[^\]]*]\([^)]*\)|\*\*(.+?)\*\*|__(.+?)__|`([^`]+)`|\[([^\]]+)]\(([^)\s]+)\)""")
