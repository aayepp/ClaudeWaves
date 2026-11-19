package com.lastwave.app.ui.navigation

import android.content.Context
import io.mockk.mockk
import org.junit.Test

class LauncherShortcutsTest {

    @Test
    fun clear_doesNotThrowOnMockContext() {
        val context = mockk<Context>(relaxed = true)
        LauncherShortcuts.clear(context)
    }
}
