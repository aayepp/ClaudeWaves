package com.lastwave.app.ui.settings

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imeAnimationTarget
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lastwave.app.ui.comic.Button
import com.lastwave.app.ui.comic.Card
import com.lastwave.app.ui.comic.FilledTonalIconButton
import com.lastwave.app.ui.common.ExpressiveHeader
import com.lastwave.app.ui.common.adaptiveContentWidth
import com.lastwave.app.ui.theme.pageBackground
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

private const val DISCORD_LOGIN_LOG_TAG = "DiscordLoginScreen"
private const val DISCORD_LOGIN_URL = "https://discord.com/login"
private const val TOKEN_POLL_MS = 1_000L

private val WEBVIEW_VERSION_REGEX = Regex("""Version/\d+(\.\d+)*\s*""")
private const val FALLBACK_UA =
    "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/129.0.0.0 Mobile Safari/537.36"

/**
 * Discord removes `window.localStorage` on the main window once booted;
 * a fresh same-origin iframe restores an untouched handle to read `localStorage.token`.
 * Returns the raw token directly to `evaluateJavascript` (never hijacks `window.alert`).
 */
private const val HARVEST_TOKEN_JS = """
    (function () {
      try {
        var frame = document.createElement('iframe');
        document.body.appendChild(frame);
        var raw = frame.contentWindow && frame.contentWindow.localStorage
          ? frame.contentWindow.localStorage.token
          : null;
        document.body.removeChild(frame);
        if (raw) {
          var clean = String(raw).replace(/^"|"$/g, '').trim();
          if (clean.length > 20) return clean;
        }
      } catch (e) {}
      try {
        if (window.webpackChunkdiscord_app) {
          var mods = [];
          window.webpackChunkdiscord_app.push([[Math.random()], {}, function (req) {
            for (var id in req.c) { mods.push(req.c[id]); }
          }]);
          for (var i = 0; i < mods.length; i++) {
            var exp = mods[i] && mods[i].exports;
            if (exp && exp.default && typeof exp.default.getToken === 'function') {
              var t = exp.default.getToken();
              if (t && String(t).length > 20) return String(t);
            }
          }
        }
      } catch (e) {}
      return '';
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

private fun decodeJsStringResult(raw: String?): String? {
    val trimmed = raw?.trim().orEmpty()
    if (trimmed.isEmpty() || trimmed == "null" || trimmed == "undefined") return null
    val decoded = runCatching {
        Json.parseToJsonElement(trimmed).jsonPrimitive.contentOrNull
    }.getOrNull() ?: trimmed.removeSurrounding("\"")
    return decoded.trim().removeSurrounding("\"").takeIf {
        it.length > 20 && it != "null" && it != "undefined"
    }
}

private fun safelyDestroyWebView(view: WebView?) {
    if (view == null) return
    runCatching {
        view.stopLoading()
        view.webViewClient = WebViewClient()
        view.webChromeClient = null
        view.loadUrl("about:blank")
        // Always detach before destroy(): destroying an attached WebView can abort Chromium.
        (view.parent as? ViewGroup)?.removeView(view)
        view.destroy()
    }
}

@OptIn(ExperimentalLayoutApi::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun DiscordLoginScreen(
    onBack: () -> Unit,
    onConnected: () -> Unit,
    viewModel: DiscordSettingsViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val appContext = remember(context) { context.applicationContext }
    val signingIn by viewModel.signingIn.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    var webView by remember { mutableStateOf<WebView?>(null) }
    var canGoBack by remember { mutableStateOf(false) }
    var pageLoading by remember { mutableStateOf(true) }
    var webViewError by remember { mutableStateOf<String?>(null) }
    var signedInUrlReached by remember { mutableStateOf(false) }
    var captured by remember { mutableStateOf(false) }
    var showTokenDialog by rememberSaveable { mutableStateOf(false) }
    var openGuideInitially by rememberSaveable { mutableStateOf(false) }

    BackHandler(enabled = canGoBack && webViewError == null) {
        runCatching { webView?.goBack() }
    }

    // Match SpotifyLoginScreen's keyboard handling so ROMs honouring `adjustResize`
    // do not subtract keyboard height twice and shake/crash focus.
    val density = LocalDensity.current
    val imeTargetPx = WindowInsets.imeAnimationTarget.getBottom(density)
    var restingHeightPx by remember { mutableIntStateOf(0) }
    var restingWidthPx by remember { mutableIntStateOf(0) }
    var currentHeightPx by remember { mutableIntStateOf(0) }
    var keyboardClearancePx by remember { mutableIntStateOf(0) }
    LaunchedEffect(imeTargetPx, currentHeightPx, restingHeightPx) {
        val alreadyResizedPx = (restingHeightPx - currentHeightPx).coerceAtLeast(0)
        val wanted = (imeTargetPx - alreadyResizedPx).coerceAtLeast(0)
        if (wanted != keyboardClearancePx) {
            delay(if (wanted == 0) 0L else 90L)
            keyboardClearancePx = wanted
        }
    }

    // Lifecycle-scoped token polling: stops automatically if user backs out (no leaked postDelayed).
    LaunchedEffect(webView, signedInUrlReached, captured, signingIn) {
        val wv = webView ?: return@LaunchedEffect
        if (!signedInUrlReached || captured || signingIn) return@LaunchedEffect
        repeat(20) {
            if (captured || signingIn) return@LaunchedEffect
            runCatching {
                wv.evaluateJavascript(HARVEST_TOKEN_JS) { raw ->
                    if (captured || signingIn) return@evaluateJavascript
                    val token = decodeJsStringResult(raw) ?: return@evaluateJavascript
                    captured = true
                    wv.visibility = View.GONE
                    viewModel.signInWithToken(token) {
                        Toast.makeText(appContext, "Connected to Discord", Toast.LENGTH_SHORT).show()
                        onConnected()
                    }
                }
            }
            delay(TOKEN_POLL_MS)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            val target = webView
            webView = null
            safelyDestroyWebView(target)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pageBackground()
            .onSizeChanged { size ->
                if (size.width != restingWidthPx) {
                    restingWidthPx = size.width
                    restingHeightPx = size.height
                } else if (size.height > restingHeightPx) {
                    restingHeightPx = size.height
                }
                currentHeightPx = size.height
            },
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .adaptiveContentWidth(maxWidth = 760.dp)
                .navigationBarsPadding()
                .padding(bottom = with(density) { keyboardClearancePx.toDp() }),
        ) {
            ExpressiveHeader(
                title = "Sign in to Discord",
                subtitle = "Connect your profile for Rich Presence",
                containerColor = Color.Transparent,
                onBack = onBack,
                actions = {
                    if (webViewError == null) {
                        FilledTonalIconButton(
                            onClick = {
                                captured = false
                                signedInUrlReached = false
                                pageLoading = true
                                runCatching { webView?.loadUrl(DISCORD_LOGIN_URL) }
                            },
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                            ),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = "Reload login page",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Spacer(Modifier.width(6.dp))
                    }
                    FilledTonalIconButton(
                        onClick = {
                            viewModel.clearError()
                            openGuideInitially = true
                            showTokenDialog = true
                        },
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        ),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Key,
                            contentDescription = "Paste Discord token",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
            )

            error?.let { msg ->
                Text(
                    text = msg,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        var createdWebView: WebView? = null
                        try {
                            WebView(ctx).also {
                                createdWebView = it
                                webView = it
                            }.apply {
                                // Explicit MATCH_PARENT prevents Chromium force_zero_layout_height
                                // from collapsing Discord's height:100% login DOM to an empty page.
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                )
                                overScrollMode = View.OVER_SCROLL_NEVER
                                settings.apply {
                                    javaScriptEnabled = true
                                    domStorageEnabled = true
                                    mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                    cacheMode = WebSettings.LOAD_DEFAULT
                                    setSupportMultipleWindows(false)
                                    userAgentString = cleanDiscordUserAgent(userAgentString)
                                }
                                runCatching {
                                    val cm = CookieManager.getInstance()
                                    cm.setAcceptCookie(true)
                                    cm.setAcceptThirdPartyCookies(this, true)
                                }

                                webChromeClient = object : WebChromeClient() {
                                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                        pageLoading = newProgress < 100
                                    }
                                }

                                webViewClient = object : WebViewClient() {
                                    override fun shouldOverrideUrlLoading(
                                        view: WebView?,
                                        request: WebResourceRequest?,
                                    ): Boolean {
                                        val url = request?.url?.toString()
                                        if (isSignedInDiscordUrl(url)) signedInUrlReached = true
                                        val scheme = request?.url?.scheme?.lowercase()
                                        // Block discord://, intent://, market:// so WebView stays on web login.
                                        if (scheme != null && scheme != "http" && scheme != "https") {
                                            return true
                                        }
                                        return false
                                    }

                                    @Deprecated("Deprecated in Java")
                                    override fun shouldOverrideUrlLoading(
                                        view: WebView?,
                                        url: String?,
                                    ): Boolean {
                                        if (isSignedInDiscordUrl(url)) signedInUrlReached = true
                                        val lower = url?.lowercase().orEmpty()
                                        if (lower.isNotEmpty() && !lower.startsWith("http://") && !lower.startsWith("https://")) {
                                            return true
                                        }
                                        return false
                                    }

                                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                        if (isSignedInDiscordUrl(url)) signedInUrlReached = true
                                    }

                                    override fun doUpdateVisitedHistory(
                                        view: WebView?,
                                        url: String?,
                                        isReload: Boolean,
                                    ) {
                                        if (isSignedInDiscordUrl(url)) signedInUrlReached = true
                                    }

                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        pageLoading = false
                                        canGoBack = runCatching { view?.canGoBack() == true }.getOrDefault(false)
                                        val currentUrl = runCatching { view?.url }.getOrNull() ?: url
                                        if (isSignedInDiscordUrl(currentUrl)) signedInUrlReached = true
                                    }

                                    override fun onReceivedError(
                                        view: WebView?,
                                        request: WebResourceRequest?,
                                        error: WebResourceError?,
                                    ) {
                                        if (request?.isForMainFrame == true) {
                                            pageLoading = false
                                            Log.w(
                                                DISCORD_LOGIN_LOG_TAG,
                                                "Main frame error: ${error?.errorCode} ${error?.description}",
                                            )
                                        }
                                    }

                                    override fun onRenderProcessGone(
                                        view: WebView?,
                                        detail: RenderProcessGoneDetail?,
                                    ): Boolean {
                                        webView = null
                                        safelyDestroyWebView(view)
                                        pageLoading = false
                                        webViewError = "Discord login WebView closed unexpectedly. You can sign in using your token instead."
                                        return true
                                    }
                                }

                                loadUrl(DISCORD_LOGIN_URL)
                            }
                        } catch (err: Exception) {
                            Log.e(DISCORD_LOGIN_LOG_TAG, "WebView init failed", err)
                            safelyDestroyWebView(createdWebView)
                            webView = null
                            pageLoading = false
                            webViewError = "Android System WebView is unavailable on this device. Tap below to connect using your Discord token instead."
                            FrameLayout(ctx)
                        } catch (err: LinkageError) {
                            Log.e(DISCORD_LOGIN_LOG_TAG, "WebView linkage error", err)
                            safelyDestroyWebView(createdWebView)
                            webView = null
                            pageLoading = false
                            webViewError = "This ROM's WebView is incompatible with Discord login. Tap below to connect using your Discord token instead."
                            FrameLayout(ctx)
                        }
                    },
                )

                // Float progress bar over WebView so its bounds never jump during typing.
                if ((pageLoading || signingIn) && webViewError == null) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth(),
                    )
                }

                if (signingIn) {
                    Text(
                        text = "Connecting your Discord profile…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.Center),
                    )
                }

                webViewError?.let { errMsg ->
                    Card(
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp),
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text(
                                text = errMsg,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                            )
                            Button(
                                onClick = {
                                    viewModel.clearError()
                                    openGuideInitially = true
                                    showTokenDialog = true
                                },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Icon(Icons.Filled.Key, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Use Discord Token Instead")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showTokenDialog) {
        DiscordTokenDialog(
            signingIn = signingIn,
            error = error,
            initialGuideExpanded = openGuideInitially,
            onClearError = viewModel::clearError,
            onDismiss = { if (!signingIn) showTokenDialog = false },
            onSubmit = { token ->
                viewModel.signInWithToken(token) {
                    showTokenDialog = false
                    Toast.makeText(appContext, "Connected to Discord", Toast.LENGTH_SHORT).show()
                    onConnected()
                }
            },
        )
    }
}
