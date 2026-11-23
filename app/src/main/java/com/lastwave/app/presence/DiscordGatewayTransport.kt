package com.lastwave.app.presence

import android.os.Build
import android.util.Base64
import android.util.Log
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

enum class DiscordConnectionStatus {
    SIGNED_OUT,
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    AUTH_FAILED,
}

data class DiscordUserProfile(
    val id: String,
    val username: String,
    val displayName: String,
    val avatarUrl: String?,
)

/**
 * Pure-OkHttp Discord Gateway v10 transport for mobile Rich Presence.
 *
 * Unlike [DiscordIpcTransport] (which relies on Discord Android's restricted
 * partner AIDL service), this connects directly to `wss://gateway.discord.gg`
 * using the user's own Discord session token — the same approach used by
 * BitChord, Metrolist, and Kizzy — so "Listening to ClaudeWaves" appears live
 * on Discord profiles even when the Discord app is closed.
 */
class DiscordGatewayTransport(
    private val okHttpClient: OkHttpClient,
    private val token: String,
    private val scope: CoroutineScope,
    private val initialStatus: String = "online",
) {
    private val wsClient: OkHttpClient = okHttpClient.newBuilder()
        .readTimeout(0L, TimeUnit.MILLISECONDS)
        .pingInterval(0L, TimeUnit.MILLISECONDS)
        .build()

    private val _status = MutableStateFlow(DiscordConnectionStatus.DISCONNECTED)
    val status: StateFlow<DiscordConnectionStatus> = _status.asStateFlow()

    @Volatile private var webSocket: WebSocket? = null
    @Volatile private var ready = false
    @Volatile private var closed = false
    @Volatile private var heartbeatJob: Job? = null
    @Volatile private var pendingPresenceFrame: String? = null
    private val lastSequence = AtomicInteger(-1)
    private val currentStatusRef = AtomicReference(initialStatus)

    val isReady: Boolean get() = !closed && ready && webSocket != null
    val isConnectingOrReady: Boolean
        get() = !closed && (ready || _status.value == DiscordConnectionStatus.CONNECTING) && webSocket != null

    fun connect() {
        if (closed) return
        if (webSocket != null && (_status.value == DiscordConnectionStatus.CONNECTING || ready)) return
        _status.value = DiscordConnectionStatus.CONNECTING
        val req = Request.Builder()
            .url(GATEWAY_URL)
            .header("User-Agent", DISCORD_USER_AGENT)
            .build()
        webSocket = wsClient.newWebSocket(req, listener)
    }

    fun sendPresence(frameJson: String, presenceStatus: String): Boolean {
        if (closed) return false
        currentStatusRef.set(presenceStatus)
        pendingPresenceFrame = frameJson
        val socket = webSocket
        if (ready && socket != null) {
            return runCatching { socket.send(frameJson) }.getOrDefault(false)
        }
        if (socket == null && _status.value != DiscordConnectionStatus.AUTH_FAILED) {
            connect()
        }
        return true
    }

    fun clearPresence(presenceStatus: String = currentStatusRef.get()): Boolean {
        val clearFrame = DiscordPresence.gatewayPresenceFrame(
            activity = null,
            status = presenceStatus,
            nowMs = System.currentTimeMillis(),
        )
        // Store clear frame as pending so any future reconnect never resurrects a stopped song.
        pendingPresenceFrame = clearFrame
        val socket = webSocket
        if (ready && socket != null) {
            return runCatching { socket.send(clearFrame) }.getOrDefault(false)
        }
        return false
    }

    fun close() {
        if (closed) return
        closed = true
        ready = false
        heartbeatJob?.cancel()
        heartbeatJob = null
        runCatching { webSocket?.close(1000, "Client closed") }
        webSocket = null
        _status.value = DiscordConnectionStatus.DISCONNECTED
    }

    private fun startHeartbeat(intervalMs: Long, socket: WebSocket) {
        heartbeatJob?.cancel()
        val safeInterval = intervalMs.coerceIn(5_000L, 120_000L)
        heartbeatJob = scope.launch(Dispatchers.IO) {
            // Discord recommends a short jitter before the first heartbeat.
            delay((safeInterval * 0.25).toLong().coerceAtLeast(1_000L))
            while (isActive && !closed && webSocket === socket) {
                val seq = lastSequence.get()
                val hb = buildJsonObject {
                    put("op", 1)
                    if (seq >= 0) put("d", seq) else put("d", JsonNull)
                }.toString()
                if (!runCatching { socket.send(hb) }.getOrDefault(false)) break
                delay(safeInterval)
            }
        }
    }

    private fun sendIdentify(socket: WebSocket) {
        val payload = buildJsonObject {
            put("op", 2)
            putJsonObject("d") {
                put("token", token)
                put("capabilities", 16381)
                put("compress", false)
                put("large_threshold", 100)
                putJsonObject("properties") {
                    put("os", "Android")
                    put("browser", "Discord Android")
                    put("device", Build.DEVICE.orEmpty().ifBlank { "Android" })
                }
                putJsonObject("presence") {
                    put("status", currentStatusRef.get())
                    put("since", 0)
                    putJsonArray("activities") {}
                    put("afk", false)
                }
            }
        }.toString()
        runCatching { socket.send(payload) }
    }

    private val listener = object : WebSocketListener() {
        override fun onMessage(webSocket: WebSocket, text: String) {
            if (closed || this@DiscordGatewayTransport.webSocket !== webSocket) return
            val root = runCatching { JSON.parseToJsonElement(text).jsonObject }.getOrNull() ?: return
            root["s"]?.jsonPrimitive?.intOrNull?.let { lastSequence.set(it) }
            val op = root["op"]?.jsonPrimitive?.intOrNull ?: return
            when (op) {
                10 -> { // HELLO
                    val interval = root["d"]?.jsonObject?.get("heartbeat_interval")
                        ?.jsonPrimitive?.longOrNull ?: 41_250L
                    startHeartbeat(interval, webSocket)
                    sendIdentify(webSocket)
                }
                0 -> { // DISPATCH
                    val eventType = root["t"]?.jsonPrimitive?.contentOrNull
                    if (eventType == "READY") {
                        ready = true
                        _status.value = DiscordConnectionStatus.CONNECTED
                        Log.d(TAG, "Discord Gateway READY")
                        pendingPresenceFrame?.let { frame ->
                            runCatching { webSocket.send(frame) }
                        }
                    }
                }
                7 -> { // RECONNECT requested by Discord
                    Log.d(TAG, "Discord Gateway requested reconnect")
                    dropSocket(webSocket, DiscordConnectionStatus.DISCONNECTED)
                }
                9 -> { // INVALID_SESSION
                    Log.w(TAG, "Discord Gateway invalid session")
                    dropSocket(webSocket, DiscordConnectionStatus.DISCONNECTED)
                }
            }
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            val nextStatus = if (code == 4004) {
                Log.w(TAG, "Discord Gateway auth failed (4004)")
                DiscordConnectionStatus.AUTH_FAILED
            } else {
                Log.d(TAG, "Discord Gateway closed: $code $reason")
                DiscordConnectionStatus.DISCONNECTED
            }
            dropSocket(webSocket, nextStatus)
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            val authFailed = response?.code == 401 || response?.code == 403
            Log.w(TAG, "Discord Gateway failure (${response?.code}): ${t.message}")
            dropSocket(
                webSocket,
                if (authFailed) DiscordConnectionStatus.AUTH_FAILED else DiscordConnectionStatus.DISCONNECTED,
            )
        }
    }

    private fun dropSocket(socket: WebSocket, nextStatus: DiscordConnectionStatus) {
        if (this.webSocket !== socket) return
        ready = false
        heartbeatJob?.cancel()
        heartbeatJob = null
        this.webSocket = null
        if (!closed) _status.value = nextStatus
    }

    companion object {
        private const val TAG = "DiscordGateway"
        private const val GATEWAY_URL = "wss://gateway.discord.gg/?v=10&encoding=json"
        private const val API_BASE = "https://discord.com/api/v9"
        private const val DISCORD_USER_AGENT = "Discord-Android/314013;RNA"
        private const val MAX_ASSET_CACHE = 128

        private val JSON = Json { ignoreUnknownKeys = true }
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
        private val assetCache = ConcurrentHashMap<String, String>()

        private val superPropertiesBase64: String by lazy {
            val json = buildJsonObject {
                put("os", "Android")
                put("browser", "Discord Android")
                put("device", Build.MODEL.orEmpty().ifBlank { "Android" })
                put("os_version", Build.VERSION.RELEASE.orEmpty().ifBlank { "14" })
                put("client_build_number", 314013)
                put("release_channel", "googleRelease")
            }.toString()
            Base64.encodeToString(json.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
        }

        /** Validates [token] against Discord's `/users/@me` and returns basic profile fields. */
        suspend fun fetchCurrentUser(
            okHttpClient: OkHttpClient,
            token: String,
        ): Result<DiscordUserProfile> = withContext(Dispatchers.IO) {
            runCatching {
                val clean = token.trim().removeSurrounding("\"")
                require(clean.isNotBlank()) { "Token is empty" }
                val req = Request.Builder()
                    .url("$API_BASE/users/@me")
                    .header("Authorization", clean)
                    .header("User-Agent", DISCORD_USER_AGENT)
                    .header("X-Super-Properties", superPropertiesBase64)
                    .get()
                    .build()
                okHttpClient.newCall(req).execute().use { resp ->
                    if (resp.code == 401 || resp.code == 403) {
                        error("Invalid or expired Discord token (HTTP ${resp.code})")
                    }
                    check(resp.isSuccessful) { "Discord rejected request (HTTP ${resp.code})" }
                    val body = resp.body?.string().orEmpty()
                    val obj = JSON.parseToJsonElement(body).jsonObject
                    val id = obj["id"]?.jsonPrimitive?.contentOrNull
                        ?: error("Discord response missing user id")
                    val username = obj["username"]?.jsonPrimitive?.contentOrNull
                        ?: error("Discord response missing username")
                    val globalName = obj["global_name"]?.jsonPrimitive?.contentOrNull
                        ?.takeIf { it.isNotBlank() && it != "null" }
                    val avatarHash = obj["avatar"]?.jsonPrimitive?.contentOrNull
                        ?.takeIf { it.isNotBlank() && it != "null" }
                    val avatarUrl = avatarHash?.let {
                        val ext = if (it.startsWith("a_")) "gif" else "png"
                        "https://cdn.discordapp.com/avatars/$id/$it.$ext?size=128"
                    }
                    DiscordUserProfile(
                        id = id,
                        username = username,
                        displayName = globalName ?: username,
                        avatarUrl = avatarUrl,
                    )
                }
            }
        }

        /**
         * Resolves an external http(s) artwork URL into Discord's `mp:external/...` asset path
         * via `/applications/{appId}/external-assets` so Gateway presence cards show album art.
         */
        suspend fun resolveExternalAsset(
            okHttpClient: OkHttpClient,
            token: String,
            imageUrl: String?,
        ): String? = withContext(Dispatchers.IO) {
            val url = imageUrl?.trim().orEmpty()
            if (url.isEmpty()) return@withContext null
            if (url.startsWith("mp:")) return@withContext url
            if (!url.startsWith("http://") && !url.startsWith("https://")) return@withContext null
            assetCache[url]?.let { return@withContext it }

            runCatching {
                val payload = buildJsonObject {
                    putJsonArray("urls") { add(JsonPrimitive(url)) }
                }.toString()
                val req = Request.Builder()
                    .url("$API_BASE/applications/${DiscordPresence.APPLICATION_ID}/external-assets")
                    .header("Authorization", token)
                    .header("User-Agent", DISCORD_USER_AGENT)
                    .header("X-Super-Properties", superPropertiesBase64)
                    .post(payload.toRequestBody(JSON_MEDIA_TYPE))
                    .build()
                okHttpClient.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) return@use null
                    val body = resp.body?.string().orEmpty()
                    val arr = JSON.parseToJsonElement(body).jsonArray
                    val path = arr.firstOrNull()?.jsonObject
                        ?.get("external_asset_path")?.jsonPrimitive?.contentOrNull
                        ?.takeIf { it.isNotBlank() }
                        ?: return@use null
                    val resolved = if (path.startsWith("mp:")) path else "mp:$path"
                    if (assetCache.size >= MAX_ASSET_CACHE) assetCache.clear()
                    assetCache[url] = resolved
                    resolved
                }
            }.getOrNull()
        }
    }
}
