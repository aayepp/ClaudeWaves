package com.lastwave.app.ui.settings

import android.annotation.SuppressLint
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.JsResult
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lastwave.app.ui.common.ExpressiveHeader
import com.lastwave.app.ui.common.adaptiveContentWidth
import com.lastwave.app.ui.theme.pageBackground

private const val DISCORD_LOGIN_URL = "https://discord.com/login"
private const val MAX_TOKEN_ATTEMPTS = 12
private const val TOKEN_RETRY_MS = 500L

private val WEBVIEW_VERSION_REGEX = Regex("""Version/\d+(\.\d+)*\s*""")
private const val FALLBACK_UA =
    "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/129.0.0.0 Mobile Safari/537.36"

/**
 * Discord removes `window.localStorage` on the main window once booted;
 * a fresh same-origin iframe restores an untouched handle to read `localStorage.token`.
 */
private const val HARVEST_TOKEN_JS = """
    (function () {
      try {
        var frame = document.createElement('iframe');
        document.body.appendChild(frame);
        var token = frame.contentWindow.localStorage.token;
        document.body.removeChild(frame);
        alert(token ? token.slice(1, -1) : '');
      } catch (e) {
        alert('');
      }
    })()
"""

private fun cleanDiscordUserAgent(raw: String?): String {
    val base = raw?.takeIf { it.isNotBlank() } ?: return FALLBACK_UA
    return base
        .replace("; wv", "")
        .replace(WEBVIEW_VERSION_REGEX, "")
        .trim()
        .ifBlank { FALLBACK_UA }
}

private fun isSignedInDiscordUrl(url: String?): Boolean {
    if (url.isNullOrBlank()) return false
    return url.contains("discord.com/app") || url.contains("discord.com/channels/")
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun DiscordLoginScreen(
    onBack: () -> Unit,
    onConnected: () -> Unit,
    viewModel: DiscordSettingsViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val signingIn by viewModel.signingIn.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    var webView by remember { mutableStateOf<WebView?>(null) }
    var canGoBack by remember { mutableStateOf(false) }
    var pageLoading by remember { mutableStateOf(true) }

    BackHandler(enabled = canGoBack) {
        webView?.goBack()
    }

    DisposableEffect(Unit) {
        onDispose {
            runCatching {
                webView?.stopLoading()
                webView?.destroy()
            }
            webView = null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pageBackground(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .adaptiveContentWidth(maxWidth = 760.dp),
        ) {
            ExpressiveHeader(
                title = "Sign in to Discord",
                subtitle = "Connect your profile for Rich Presence",
                containerColor = Color.Transparent,
                onBack = onBack,
            )

            if (pageLoading || signingIn) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            error?.let { msg ->
                Text(
                    text = msg,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding(),
            ) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        WebView(ctx).apply {
                            // Explicit MATCH_PARENT prevents Chromium force_zero_layout_height
                            // from collapsing Discord's height:100% login DOM to an empty page.
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT,
                            )
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.databaseEnabled = true
                            settings.userAgentString = cleanDiscordUserAgent(settings.userAgentString)
                            CookieManager.getInstance().setAcceptCookie(true)
                            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

                            var captured = false
                            var attempts = 0

                            fun harvestToken() {
                                if (captured || attempts >= MAX_TOKEN_ATTEMPTS) return
                                attempts++
                                evaluateJavascript(HARVEST_TOKEN_JS, null)
                            }

                            webViewClient = object : WebViewClient() {
                                @Deprecated("Deprecated in Java")
                                override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                                    if (isSignedInDiscordUrl(url)) harvestToken()
                                    return false
                                }

                                override fun doUpdateVisitedHistory(
                                    view: WebView?,
                                    url: String?,
                                    isReload: Boolean,
                                ) {
                                    if (isSignedInDiscordUrl(url)) harvestToken()
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    pageLoading = false
                                    canGoBack = view?.canGoBack() == true
                                    if (isSignedInDiscordUrl(url)) harvestToken()
                                }

                                override fun onRenderProcessGone(
                                    view: WebView?,
                                    detail: RenderProcessGoneDetail?,
                                ): Boolean {
                                    runCatching {
                                        (view?.parent as? ViewGroup)?.removeView(view)
                                        view?.destroy()
                                    }
                                    webView = null
                                    return true
                                }
                            }

                            webChromeClient = object : WebChromeClient() {
                                override fun onJsAlert(
                                    view: WebView,
                                    url: String,
                                    message: String,
                                    result: JsResult,
                                ): Boolean {
                                    result.confirm()
                                    if (captured) return true
                                    val token = message.trim().takeUnless {
                                        it.isBlank() || it == "null" || it == "undefined"
                                    }
                                    if (token == null) {
                                        view.postDelayed({ harvestToken() }, TOKEN_RETRY_MS)
                                        return true
                                    }
                                    captured = true
                                    view.visibility = View.GONE
                                    viewModel.signInWithToken(token) {
                                        Toast.makeText(context, "Connected to Discord", Toast.LENGTH_SHORT).show()
                                        onConnected()
                                    }
                                    return true
                                }
                            }

                            webView = this
                            loadUrl(DISCORD_LOGIN_URL)
                        }
                    },
                )
                if (signingIn) {
                    Text(
                        text = "Connecting your Discord profile…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
            }
        }
    }
}
