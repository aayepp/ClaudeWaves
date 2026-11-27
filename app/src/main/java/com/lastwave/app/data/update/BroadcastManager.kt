package com.lastwave.app.data.update

import android.content.Context
import android.net.Uri
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.CacheControl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLDecoder
import java.net.URLEncoder
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/** [emoji] marks the type in the notification shade, which cannot show a vector icon. */
enum class BroadcastType(val key: String, val label: String, val emoji: String) {
    INFO("info", "Info", "\uD83D\uDCE2"),
    WARNING("warning", "Amaran", "\u26A0\uFE0F"),
    MAINTENANCE("maintenance", "Selenggara", "\uD83D\uDD27"),
    UPDATE("update", "Update", "\uD83C\uDD95");

    companion object {
        fun fromKey(raw: String?): BroadcastType =
            entries.firstOrNull { it.key.equals(raw?.trim(), ignoreCase = true) } ?: INFO
    }
}

enum class BroadcastDisplayMode(val key: String, val label: String) {
    POPUP("popup", "Popup Sahaja"),
    TICKER("ticker", "Top Home Bergerak"),
    BOTH("both", "Kedua-dua");

    companion object {
        fun fromKey(raw: String?): BroadcastDisplayMode =
            entries.firstOrNull { it.key.equals(raw?.trim(), ignoreCase = true) } ?: POPUP
    }
}

data class BroadcastMessage(
    val id: String,
    val issueNumber: Int,
    val title: String,
    val message: String,
    val type: BroadcastType,
    val publishedAt: String,
    val imageUrl: String? = null,
    val actionLabel: String?,
    val actionUrl: String?,
    val notify: Boolean,
    val displayMode: BroadcastDisplayMode = BroadcastDisplayMode.POPUP,
)

data class BroadcastUiState(
    val active: BroadcastMessage? = null,
    val showDialog: Boolean = false,
    val showTicker: Boolean = false,
)

sealed interface DevLockState {
    data class Open(
        val remainingAttempts: Int,
        val isFinalChanceAfterCooldown: Boolean,
    ) : DevLockState

    data class Cooldown24h(val unlockAtWallMs: Long) : DevLockState
    data object PermanentlyLocked : DevLockState
}

/**
 * Polls owner-authored GitHub issues labeled `broadcast` and manages
 * optional local-PAT admin actions (never ships any token in APK).
 */
@Singleton
class BroadcastManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(BroadcastUiState())
    val state: StateFlow<BroadcastUiState> = _state.asStateFlow()

    private val _devShortcutVisible = MutableStateFlow(computeDevShortcutVisible())
    val devShortcutVisible: StateFlow<Boolean> = _devShortcutVisible.asStateFlow()

    init {
        checkForBroadcast()
    }

    fun checkForBroadcast() {
        scope.launch {
            val latest = try {
                fetchLatestValidBroadcast()
            } catch (e: Exception) {
                currentCoroutineContext().ensureActive()
                return@launch
            }
            if (latest == null) {
                _state.value = BroadcastUiState()
                return@launch
            }
            val dismissedId = prefs.getString(KEY_DISMISSED_ID, null)
            val visible = latest.id != dismissedId
            _state.value = BroadcastUiState(
                active = latest,
                showDialog = visible && latest.displayMode != BroadcastDisplayMode.TICKER,
                showTicker = visible && latest.displayMode != BroadcastDisplayMode.POPUP,
            )
        }
    }

    suspend fun messageToNotify(): BroadcastMessage? {
        val latest = try {
            fetchLatestValidBroadcast()
        } catch (e: Exception) {
            currentCoroutineContext().ensureActive()
            return null
        } ?: return null

        val dismissedId = prefs.getString(KEY_DISMISSED_ID, null)
        if (latest.id == dismissedId) return null

        _state.value = _state.value.copy(
            active = latest,
            showTicker = latest.displayMode != BroadcastDisplayMode.POPUP,
        )
        if (!latest.notify) return null
        val notifiedId = prefs.getString(KEY_NOTIFIED_ID, null)
        if (latest.id == notifiedId) return null

        prefs.edit().putString(KEY_NOTIFIED_ID, latest.id).apply()
        return latest
    }

    fun showDialog() {
        val current = _state.value.active ?: return
        val dismissedId = prefs.getString(KEY_DISMISSED_ID, null)
        if (current.id == dismissedId) return
        _state.value = _state.value.copy(
            active = current,
            showDialog = current.displayMode != BroadcastDisplayMode.TICKER,
            showTicker = current.displayMode != BroadcastDisplayMode.POPUP,
        )
    }

    fun openDialogFromTicker() {
        val current = _state.value.active ?: return
        _state.value = _state.value.copy(active = current, showDialog = true)
    }

    fun dismiss(id: String) {
        val current = _state.value.active
        if (current != null && current.displayMode != BroadcastDisplayMode.POPUP) {
            _state.value = _state.value.copy(showDialog = false)
            return
        }
        prefs.edit().putString(KEY_DISMISSED_ID, id).apply()
        _state.value = _state.value.copy(showDialog = false, showTicker = false)
    }

    fun dismissTicker(id: String) {
        prefs.edit().putString(KEY_DISMISSED_ID, id).apply()
        _state.value = _state.value.copy(showTicker = false, showDialog = false)
    }

    fun hasOwnerToken(): Boolean = !readOwnerToken().isNullOrBlank()

    fun savedOwnerLogin(): String? = prefs.getString(KEY_OWNER_LOGIN, null)

    private fun computeDevShortcutVisible(): Boolean =
        hasOwnerToken() && !prefs.getBoolean(KEY_DEV_SHORTCUT_HIDDEN, false)

    fun hideDeveloperShortcut() {
        prefs.edit().putBoolean(KEY_DEV_SHORTCUT_HIDDEN, true).apply()
        _devShortcutVisible.value = false
    }

    fun revealDeveloperShortcutIfAuthenticated() {
        if (!hasOwnerToken()) return
        prefs.edit().remove(KEY_DEV_SHORTCUT_HIDDEN).apply()
        _devShortcutVisible.value = true
    }

    fun getDevLockState(nowMs: Long = System.currentTimeMillis()): DevLockState {
        if (hasOwnerToken()) {
            return DevLockState.Open(
                remainingAttempts = MAX_INITIAL_ATTEMPTS,
                isFinalChanceAfterCooldown = false,
            )
        }
        val stage = prefs.getInt(KEY_DEV_LOCK_STAGE, 0)
        if (stage >= 2) return DevLockState.PermanentlyLocked
        if (stage == 1) {
            val untilMs = prefs.getLong(KEY_DEV_LOCK_UNTIL_MS, 0L)
            return if (nowMs < untilMs) {
                DevLockState.Cooldown24h(untilMs)
            } else {
                DevLockState.Open(
                    remainingAttempts = 1,
                    isFinalChanceAfterCooldown = true,
                )
            }
        }
        val failed = prefs.getInt(KEY_DEV_FAILED_ATTEMPTS, 0)
        val remaining = (MAX_INITIAL_ATTEMPTS - failed).coerceIn(1, MAX_INITIAL_ATTEMPTS)
        return DevLockState.Open(
            remainingAttempts = remaining,
            isFinalChanceAfterCooldown = false,
        )
    }

    fun isDevAccessPermanentlyLocked(): Boolean =
        !hasOwnerToken() && prefs.getInt(KEY_DEV_LOCK_STAGE, 0) >= 2

    private fun recordFailedDevLoginAttempt(nowMs: Long = System.currentTimeMillis()): String {
        val stage = prefs.getInt(KEY_DEV_LOCK_STAGE, 0)
        return if (stage >= 1) {
            prefs.edit()
                .putInt(KEY_DEV_LOCK_STAGE, 2)
                .putInt(KEY_DEV_FAILED_ATTEMPTS, 0)
                .putLong(KEY_DEV_LOCK_UNTIL_MS, 0L)
                .apply()
            "Percubaan gagal selepas tempoh kunci. Akses developer dikunci secara kekal."
        } else {
            val newFailed = prefs.getInt(KEY_DEV_FAILED_ATTEMPTS, 0) + 1
            if (newFailed >= MAX_INITIAL_ATTEMPTS) {
                prefs.edit()
                    .putInt(KEY_DEV_LOCK_STAGE, 1)
                    .putInt(KEY_DEV_FAILED_ATTEMPTS, 0)
                    .putLong(KEY_DEV_LOCK_UNTIL_MS, nowMs + COOLDOWN_DURATION_MS)
                    .apply()
                "3 percubaan gagal. Halaman login dikunci selama 24 jam."
            } else {
                prefs.edit()
                    .putInt(KEY_DEV_FAILED_ATTEMPTS, newFailed)
                    .apply()
                val left = MAX_INITIAL_ATTEMPTS - newFailed
                "Token tidak sah (baki $left percubaan)"
            }
        }
    }

    private fun resetDevLockout() {
        prefs.edit()
            .remove(KEY_DEV_LOCK_STAGE)
            .remove(KEY_DEV_FAILED_ATTEMPTS)
            .remove(KEY_DEV_LOCK_UNTIL_MS)
            .apply()
    }

    fun clearOwnerToken() {
        prefs.edit()
            .remove(KEY_OWNER_TOKEN)
            .remove(KEY_OWNER_LOGIN)
            .remove(KEY_DEV_SHORTCUT_HIDDEN)
            .apply()
        _devShortcutVisible.value = false
    }

    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(KEYSTORE_ALIAS, null) as? SecretKey)?.let { return it }
        val spec = KeyGenParameterSpec.Builder(
            KEYSTORE_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setRandomizedEncryptionRequired(true)
            .build()
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE).run {
            init(spec)
            generateKey()
        }
    }

    private fun encryptToken(plainToken: String): String {
        val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())
        val iv = cipher.iv
        val ciphertext = cipher.doFinal(plainToken.toByteArray(Charsets.UTF_8))
        val combined = ByteArray(iv.size + ciphertext.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(ciphertext, 0, combined, iv.size, ciphertext.size)
        return ENC_PREFIX + Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    internal fun readOwnerToken(): String? {
        val stored = prefs.getString(KEY_OWNER_TOKEN, null)?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        if (!stored.startsWith(ENC_PREFIX)) {
            runCatching { encryptToken(stored) }.getOrNull()?.let { encrypted ->
                prefs.edit().putString(KEY_OWNER_TOKEN, encrypted).apply()
            }
            return stored
        }
        return runCatching {
            val payload = Base64.decode(stored.removePrefix(ENC_PREFIX), Base64.NO_WRAP)
            if (payload.size <= GCM_IV_BYTES) return@runCatching null
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            val key = keyStore.getKey(KEYSTORE_ALIAS, null) as? SecretKey ?: return@runCatching null
            val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, payload, 0, GCM_IV_BYTES))
            String(cipher.doFinal(payload, GCM_IV_BYTES, payload.size - GCM_IV_BYTES), Charsets.UTF_8)
        }.getOrNull()?.takeIf { it.isNotBlank() }
    }

    suspend fun saveAndVerifyOwnerToken(rawToken: String): Result<String> = withContext(Dispatchers.IO) {
        when (getDevLockState()) {
            DevLockState.PermanentlyLocked ->
                return@withContext Result.failure(IllegalStateException("Akses developer dikunci secara kekal"))
            is DevLockState.Cooldown24h ->
                return@withContext Result.failure(IllegalStateException("Halaman login dikunci selama 24 jam"))
            is DevLockState.Open -> Unit
        }
        val token = rawToken.trim()
        if (token.isEmpty()) return@withContext Result.failure(IllegalArgumentException("Token kosong"))
        try {
            val request = githubRequestBuilder("https://api.github.com/user")
                .header("Authorization", "Bearer $token")
                .cacheControl(CacheControl.FORCE_NETWORK)
                .get()
                .build()
            okHttpClient.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    if (response.code == 401 || response.code == 403 || response.code == 404) {
                        val lockMsg = recordFailedDevLoginAttempt()
                        return@withContext Result.failure(IllegalStateException(lockMsg))
                    }
                    return@withContext Result.failure(IllegalStateException(extractGithubError(response.code, bodyStr)))
                }
                val login = JSONObject(bodyStr).optString("login").trim()
                if (!login.equals(OWNER_LOGIN, ignoreCase = true)) {
                    val lockMsg = recordFailedDevLoginAttempt()
                    return@withContext Result.failure(IllegalStateException("Akaun bukan $OWNER_LOGIN. $lockMsg"))
                }
                val encryptedToken = encryptToken(token)
                prefs.edit()
                    .putString(KEY_OWNER_TOKEN, encryptedToken)
                    .putString(KEY_OWNER_LOGIN, login)
                    .remove(KEY_DEV_SHORTCUT_HIDDEN)
                    .apply()
                resetDevLockout()
                _devShortcutVisible.value = true
                Result.success(login)
            }
        } catch (e: Exception) {
            currentCoroutineContext().ensureActive()
            Result.failure(e)
        }
    }

    suspend fun fetchOpenBroadcasts(): Result<List<BroadcastMessage>> = withContext(Dispatchers.IO) {
        try {
            val token = readOwnerToken()
            val builder = githubRequestBuilder(ISSUES_ENDPOINT)
                .cacheControl(CacheControl.FORCE_NETWORK)
                .get()
            if (token != null) builder.header("Authorization", "Bearer $token")
            okHttpClient.newCall(builder.build()).execute().use { response ->
                val bodyStr = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return@withContext Result.failure(IllegalStateException(extractGithubError(response.code, bodyStr)))
                }
                Result.success(parseIssueList(bodyStr))
            }
        } catch (e: Exception) {
            currentCoroutineContext().ensureActive()
            Result.failure(e)
        }
    }

    suspend fun sendBroadcast(
        title: String,
        message: String,
        type: BroadcastType,
        imageUrl: String? = null,
        actionLabel: String?,
        actionUrl: String?,
        notify: Boolean,
        displayMode: BroadcastDisplayMode = BroadcastDisplayMode.POPUP,
        closePrevious: Boolean = true,
    ): Result<BroadcastMessage> = withContext(Dispatchers.IO) {
        val token = readOwnerToken()
            ?: return@withContext Result.failure(IllegalStateException("Tiada token admin disimpan"))
        val cleanTitle = title.trim()
        val cleanMessage = message.trim()
        if (cleanTitle.isEmpty() || cleanMessage.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("Tajuk & mesej wajib diisi"))
        }
        val cleanImageUrl = sanitizeHttpUrl(imageUrl)
        if (!imageUrl.isNullOrBlank() && cleanImageUrl == null) {
            return@withContext Result.failure(IllegalArgumentException("URL gambar mesti bermula dengan http:// atau https://"))
        }
        val cleanUrl = sanitizeHttpUrl(actionUrl)
        if (!actionUrl.isNullOrBlank() && cleanUrl == null) {
            return@withContext Result.failure(IllegalArgumentException("URL mesti bermula dengan http:// atau https://"))
        }
        try {
            if (closePrevious) {
                fetchOpenBroadcasts().getOrNull().orEmpty().forEach { existing ->
                    closeBroadcast(existing.issueNumber)
                }
            }
            val payload = JSONObject().apply {
                put("title", cleanTitle)
                put("body", buildIssueBody(cleanMessage, type, cleanImageUrl, actionLabel, cleanUrl, notify, displayMode))
                put("labels", JSONArray().put(BROADCAST_LABEL))
            }
            val request = githubRequestBuilder("https://api.github.com/repos/$REPO/issues")
                .header("Authorization", "Bearer $token")
                .cacheControl(CacheControl.FORCE_NETWORK)
                .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()
            okHttpClient.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return@withContext Result.failure(IllegalStateException(extractGithubError(response.code, bodyStr)))
                }
                invalidateCache()
                val created = parseIssue(JSONObject(bodyStr))
                    ?: return@withContext Result.failure(IllegalStateException("Gagal baca respons issue baru"))
                Result.success(created)
            }
        } catch (e: Exception) {
            currentCoroutineContext().ensureActive()
            Result.failure(e)
        }
    }

    suspend fun closeBroadcast(issueNumber: Int): Result<Unit> = withContext(Dispatchers.IO) {
        val token = readOwnerToken()
            ?: return@withContext Result.failure(IllegalStateException("Tiada token admin disimpan"))
        try {
            val payload = JSONObject().apply { put("state", "closed") }
            val request = githubRequestBuilder("https://api.github.com/repos/$REPO/issues/$issueNumber")
                .header("Authorization", "Bearer $token")
                .cacheControl(CacheControl.FORCE_NETWORK)
                .patch(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()
            okHttpClient.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return@withContext Result.failure(IllegalStateException(extractGithubError(response.code, bodyStr)))
                }
                invalidateCache()
                val current = _state.value.active
                if (current != null && current.issueNumber == issueNumber) {
                    _state.value = BroadcastUiState()
                }
                Result.success(Unit)
            }
        } catch (e: Exception) {
            currentCoroutineContext().ensureActive()
            Result.failure(e)
        }
    }

    private suspend fun fetchLatestValidBroadcast(): BroadcastMessage? = withContext(Dispatchers.IO) {
        val cachedJson = prefs.getString(KEY_CACHED_JSON, null).orEmpty()
        val cachedEtag = prefs.getString(KEY_CACHED_ETAG, null)?.takeIf { it.isNotBlank() && cachedJson.isNotEmpty() }
        val builder = githubRequestBuilder(ISSUES_ENDPOINT)
            .cacheControl(CacheControl.FORCE_NETWORK)
            .get()
        if (cachedEtag != null) builder.header("If-None-Match", cachedEtag)
        okHttpClient.newCall(builder.build()).execute().use { response ->
            val rawJson = when (response.code) {
                304 -> cachedJson
                in 200..299 -> {
                    val bodyStr = response.body?.string().orEmpty()
                    val etag = response.header("ETag").orEmpty()
                    prefs.edit()
                        .putString(KEY_CACHED_JSON, bodyStr)
                        .putString(KEY_CACHED_ETAG, etag)
                        .apply()
                    bodyStr
                }
                else -> return@withContext null
            }
            parseIssueList(rawJson).firstOrNull()
        }
    }

    private fun invalidateCache() {
        prefs.edit()
            .remove(KEY_CACHED_ETAG)
            .remove(KEY_CACHED_JSON)
            .apply()
    }

    private fun githubRequestBuilder(url: String): Request.Builder = Request.Builder()
        .url(url)
        .header("Accept", "application/vnd.github+json")
        .header("X-GitHub-Api-Version", GITHUB_API_VERSION)
        .header("User-Agent", USER_AGENT)

    private fun parseIssueList(rawJson: String): List<BroadcastMessage> {
        if (rawJson.isBlank()) return emptyList()
        val array = runCatching { JSONArray(rawJson) }.getOrNull() ?: return emptyList()
        val out = ArrayList<BroadcastMessage>(array.length())
        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: continue
            parseIssue(item)?.let(out::add)
        }
        return out
    }

    private fun parseIssue(item: JSONObject): BroadcastMessage? {
        if (item.has("pull_request")) return null
        val login = item.optJSONObject("user")?.optString("login").orEmpty()
        val association = item.optString("author_association").orEmpty()
        if (!login.equals(OWNER_LOGIN, ignoreCase = true) ||
            !association.equals("OWNER", ignoreCase = true)
        ) return null

        val number = item.optInt("number", 0)
        if (number <= 0) return null
        val title = item.optString("title").trim()
        if (title.isEmpty()) return null

        val parsed = parseIssueBody(item.optString("body"))
        return BroadcastMessage(
            id = "issue-$number",
            issueNumber = number,
            title = title,
            message = parsed.message,
            type = parsed.type,
            publishedAt = item.optString("created_at").trim(),
            imageUrl = parsed.imageUrl,
            actionLabel = parsed.actionLabel,
            actionUrl = parsed.actionUrl,
            notify = parsed.notify,
            displayMode = parsed.displayMode,
        )
    }

    private data class ParsedBody(
        val type: BroadcastType,
        val notify: Boolean,
        val imageUrl: String?,
        val actionLabel: String?,
        val actionUrl: String?,
        val displayMode: BroadcastDisplayMode,
        val message: String,
    )

    private fun parseIssueBody(rawBody: String): ParsedBody {
        val match = META_COMMENT_REGEX.find(rawBody)
        var type = BroadcastType.INFO
        var notify = true
        var imageUrl: String? = null
        var actionLabel: String? = null
        var actionUrl: String? = null
        var displayMode = BroadcastDisplayMode.POPUP

        if (match != null) {
            val tokens = match.groupValues[1].trim().split(WHITESPACE_REGEX)
            for (token in tokens) {
                val eq = token.indexOf('=')
                if (eq <= 0) continue
                val key = token.substring(0, eq).trim()
                val rawValue = token.substring(eq + 1).trim()
                when (key) {
                    "type" -> type = BroadcastType.fromKey(rawValue)
                    "notify" -> notify = rawValue.toBooleanStrictOrNull() ?: true
                    "imageUrl" -> imageUrl = sanitizeHttpUrl(urlDecode(rawValue))
                    "actionLabel" -> actionLabel = urlDecode(rawValue)?.trim()?.ifBlank { null }
                    "actionUrl" -> actionUrl = sanitizeHttpUrl(urlDecode(rawValue))
                    "display" -> displayMode = BroadcastDisplayMode.fromKey(rawValue)
                }
            }
        }
        val cleanBody = if (match != null) {
            rawBody.removeRange(match.range).trim()
        } else {
            rawBody.trim()
        }
        return ParsedBody(
            type = type,
            notify = notify,
            imageUrl = imageUrl,
            actionLabel = actionLabel,
            actionUrl = actionUrl,
            displayMode = displayMode,
            message = cleanBody,
        )
    }

    private fun buildIssueBody(
        message: String,
        type: BroadcastType,
        imageUrl: String?,
        actionLabel: String?,
        actionUrl: String?,
        notify: Boolean,
        displayMode: BroadcastDisplayMode,
    ): String {
        val meta = ArrayList<String>(6).apply {
            add("type=${type.key}")
            add("notify=$notify")
            add("display=${displayMode.key}")
            if (!imageUrl.isNullOrBlank()) add("imageUrl=${urlEncode(imageUrl)}")
            val label = actionLabel?.trim()?.takeIf { it.isNotEmpty() }
            if (label != null) add("actionLabel=${urlEncode(label)}")
            if (!actionUrl.isNullOrBlank()) add("actionUrl=${urlEncode(actionUrl)}")
        }
        return "<!-- cw-broadcast ${meta.joinToString(" ")} -->\n$message"
    }

    fun getDevLanguageCode(): String = prefs.getString(KEY_DEV_LANGUAGE, "ms") ?: "ms"

    fun setDevLanguageCode(code: String) {
        prefs.edit().putString(KEY_DEV_LANGUAGE, if (code == "en") "en" else "ms").apply()
    }

    private fun extractGithubError(code: Int, bodyStr: String): String {
        val apiMsg = runCatching { JSONObject(bodyStr).optString("message") }.getOrNull().orEmpty().trim()
        return if (apiMsg.isNotEmpty()) "HTTP $code: $apiMsg" else "HTTP $code"
    }

    companion object {
        private const val OWNER_LOGIN = "aayepp"
        private const val REPO = "aayepp/ClaudeWaves"
        private const val BROADCAST_LABEL = "broadcast"
        private const val PREFS_NAME = "lastwave_broadcast"
        private const val GITHUB_API_VERSION = "2022-11-28"
        private const val USER_AGENT = "ClaudeWaves-Android"
        private const val ISSUES_ENDPOINT =
            "https://api.github.com/repos/$REPO/issues?labels=$BROADCAST_LABEL&state=open&creator=$OWNER_LOGIN&per_page=5&sort=created&direction=desc"

        private const val KEY_DISMISSED_ID = "dismissed_id"
        private const val KEY_NOTIFIED_ID = "notified_id"
        private const val KEY_CACHED_ETAG = "cached_etag"
        private const val KEY_CACHED_JSON = "cached_json"
        private const val KEY_OWNER_TOKEN = "owner_token"
        private const val KEY_OWNER_LOGIN = "owner_login"
        private const val KEY_DEV_SHORTCUT_HIDDEN = "dev_shortcut_hidden"
        private const val KEY_DEV_LOCK_STAGE = "dev_lock_stage"
        private const val KEY_DEV_FAILED_ATTEMPTS = "dev_failed_attempts"
        private const val KEY_DEV_LOCK_UNTIL_MS = "dev_lock_until_ms"
        private const val KEY_DEV_LANGUAGE = "dev_console_language"
        private const val MAX_INITIAL_ATTEMPTS = 3
        private const val COOLDOWN_DURATION_MS = 24L * 60L * 60L * 1000L
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEYSTORE_ALIAS = "claudewaves_broadcast_pat_v1"
        private const val AES_GCM_TRANSFORMATION = "AES/GCM/NoPadding"
        private const val ENC_PREFIX = "enc_v1:"
        private const val GCM_IV_BYTES = 12
        private const val GCM_TAG_BITS = 128

        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
        private val WHITESPACE_REGEX = Regex("\\s+")
        private val META_COMMENT_REGEX = Regex("""^\s*<!--\s*cw-broadcast\s+([^>]*?)-->\s*""")

        private fun urlEncode(value: String): String =
            URLEncoder.encode(value, "UTF-8")

        private fun urlDecode(value: String): String? =
            runCatching { URLDecoder.decode(value, "UTF-8") }.getOrNull()

        private fun sanitizeHttpUrl(raw: String?): String? {
            val trimmed = raw?.trim().orEmpty()
            if (trimmed.isEmpty()) return null
            if (!trimmed.startsWith("https://", ignoreCase = true) &&
                !trimmed.startsWith("http://", ignoreCase = true)
            ) return null
            val validHost = runCatching { !Uri.parse(trimmed).host.isNullOrBlank() }.getOrDefault(false)
            return trimmed.takeIf { validHost }
        }
    }
}

private val MONTH_NAMES = arrayOf(
    "Jan", "Feb", "Mac", "Apr", "Mei", "Jun",
    "Jul", "Ogos", "Sep", "Okt", "Nov", "Dis",
)

/** Formats ISO-8601 date (`YYYY-MM-DD...`) safely on minSdk 24 without `java.time`. */
internal fun formatBroadcastDate(isoDate: String): String {
    val datePart = isoDate.substringBefore('T').trim()
    val parts = datePart.split('-')
    if (parts.size != 3) return datePart
    val year = parts[0].toIntOrNull() ?: return datePart
    val month = parts[1].toIntOrNull() ?: return datePart
    val day = parts[2].toIntOrNull() ?: return datePart
    val monthName = MONTH_NAMES.getOrNull(month - 1) ?: return datePart
    return "$day $monthName $year"
}
