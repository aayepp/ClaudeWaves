package com.lastwave.app.data.update

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ChangelogParserTest {

    @Test
    fun parseChangelogMarkdownExtractsEntriesWithDatesAcrossCrlf() {
        val md = listOf(
            "# Changelog",
            "",
            "## [5.2.2] - 2026-11-15",
            "",
            "### What's New",
            "* **Regional Charts:** Viral, weekly, and monthly charts.",
            "",
            "## [5.2.1] - 2026-11-13",
            "",
            "* Initial release.",
        ).joinToString("\r\n")

        val entries = parseChangelogMarkdown(md)

        assertThat(entries).hasSize(2)
        assertThat(entries[0].version).isEqualTo("5.2.2")
        assertThat(entries[0].date).isEqualTo("2026-11-15")
        assertThat(entries[0].bodyMarkdown).contains("Regional Charts")
        assertThat(entries[1].version).isEqualTo("5.2.1")
        assertThat(entries[1].date).isEqualTo("2026-11-13")
    }

    @Test
    fun findChangelogEntryForVersionMatchesCleanlyAndFallsBackToNewest() {
        val entries = listOf(
            ChangelogEntry("5.2.2", "2026-11-15", "Newest notes"),
            ChangelogEntry("5.2.1", "2026-11-13", "Older notes"),
        )

        assertThat(findChangelogEntryForVersion(entries, "v5.2.1")?.bodyMarkdown)
            .isEqualTo("Older notes")
        assertThat(findChangelogEntryForVersion(entries, "9.9.9")?.version)
            .isEqualTo("5.2.2")
    }
}
