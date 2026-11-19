package com.lastwave.app.ui.navigation

import android.content.Context
import android.os.Build
import androidx.core.content.pm.ShortcutManagerCompat

/**
 * Clears any previously-registered dynamic launcher long-press shortcuts so
 * holding the app icon on the home screen stays clean.
 */
object LauncherShortcuts {
    fun clear(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) return
        val appContext = context.applicationContext
        runCatching { ShortcutManagerCompat.removeAllDynamicShortcuts(appContext) }
    }
}
