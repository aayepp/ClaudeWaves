package com.lastwave.app.ui.update

import org.junit.Assert.assertEquals
import org.junit.Test

class ReleaseNotesTextTest {

    @Test
    fun `renders headings bullets and inline markup as readable text`() {
        val notes = """

            ## What's new
            - **Bold** change with `code`
              - nested [link](https://example.test/x)
            ![shot](https://example.test/shot.png)


            ---
            Plain line
        """.trimIndent()

        assertEquals(
            "What's new\n" +
                "•  Bold change with code\n" +
                "    •  nested link\n" +
                "\n" +
                "―――\n" +
                "Plain line",
            releaseNotesText(notes).text,
        )
    }

    @Test
    fun `unknown markup stays as text`() {
        assertEquals("a *b* c", releaseNotesText("a *b* c").text)
    }

    @Test
    fun `strips markdown table alignment rows and github alert tags`() {
        val rawNotice = """
            ### Download Package (`v5.2.9`)
            | Package | Supported Android | Description |
            | :--- | :--- | :--- |
            | **`ClaudeWaves.apk`** | **Android 9.0 – 17** | **Universal Release.** Signed release APK. |
            > [!NOTE]
            > **Existing users:** Open **Settings → Check for Updates**.
        """.trimIndent()

        assertEquals(
            "Download Package (v5.2.9)\n" +
                "•  ClaudeWaves.apk  •  Android 9.0 – 17  •  Universal Release. Signed release APK.\n" +
                "Existing users: Open Settings → Check for Updates.",
            releaseNotesText(rawNotice).text,
        )
    }
}
