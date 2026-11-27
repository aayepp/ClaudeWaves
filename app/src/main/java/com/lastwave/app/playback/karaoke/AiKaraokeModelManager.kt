package com.lastwave.app.playback.karaoke

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages the on-device UVR-MDX-NET-Inst_HQ_4 ONNX vocal separation model (~56 MB),
 * verifying file length and SHA-256 checksum on download.
 */
@Singleton
class AiKaraokeModelManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient,
) {
    private val downloadMutex = Mutex()
    private val modelDir: File by lazy { File(context.filesDir, "karaoke") }
    private val downloadClient: OkHttpClient by lazy {
        okHttpClient.newBuilder()
            .connectTimeout(25, TimeUnit.SECONDS)
            .readTimeout(45, TimeUnit.SECONDS)
            .callTimeout(10, TimeUnit.MINUTES)
            .build()
    }

    fun existingModelFile(): File? {
        val target = File(modelDir, MODEL_FILE_NAME)
        return target.takeIf { it.isFile && it.length() == MODEL_EXPECTED_BYTES }
    }

    suspend fun ensureModelDownloaded(
        onProgress: (progressPct: Int, downloadedMb: Int, totalMb: Int) -> Unit,
    ): Result<File> = downloadMutex.withLock {
        existingModelFile()?.let { return Result.success(it) }
        return withContext(Dispatchers.IO) {
            modelDir.mkdirs()
            val partFile = File(modelDir, "$MODEL_FILE_NAME.part")
            val finalFile = File(modelDir, MODEL_FILE_NAME)
            try {
                partFile.delete()
                val req = Request.Builder().url(MODEL_URL).get().build()
                downloadClient.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) {
                        return@withContext Result.failure(
                            IllegalStateException("HTTP ${resp.code}"),
                        )
                    }
                    val body = resp.body
                        ?: return@withContext Result.failure(IllegalStateException("Empty response body"))
                    val totalBytes = body.contentLength().takeIf { it > 0L } ?: MODEL_EXPECTED_BYTES
                    val totalMb = (totalBytes / BYTES_PER_MB).toInt().coerceAtLeast(1)
                    val digest = MessageDigest.getInstance("SHA-256")
                    val buf = ByteArray(64 * 1024)
                    var downloaded = 0L
                    var lastPct = -1

                    FileOutputStream(partFile).use { fos ->
                        body.byteStream().use { input ->
                            while (true) {
                                currentCoroutineContext().ensureActive()
                                val n = input.read(buf)
                                if (n < 0) break
                                fos.write(buf, 0, n)
                                digest.update(buf, 0, n)
                                downloaded += n
                                val pct = ((downloaded * 100L) / totalBytes).toInt().coerceIn(0, 100)
                                if (pct != lastPct) {
                                    lastPct = pct
                                    onProgress(pct, (downloaded / BYTES_PER_MB).toInt(), totalMb)
                                }
                            }
                        }
                    }

                    val actualSha = digest.digest().joinToString("") { "%02x".format(it) }
                    if (!actualSha.equals(MODEL_SHA256, ignoreCase = true)) {
                        partFile.delete()
                        return@withContext Result.failure(
                            IllegalStateException("Model checksum mismatch"),
                        )
                    }
                    if (finalFile.exists()) finalFile.delete()
                    if (!partFile.renameTo(finalFile)) {
                        partFile.copyTo(finalFile, overwrite = true)
                        partFile.delete()
                    }
                    Result.success(finalFile)
                }
            } catch (c: CancellationException) {
                partFile.delete()
                throw c
            } catch (t: Throwable) {
                currentCoroutineContext().ensureActive()
                partFile.delete()
                Result.failure(t)
            }
        }
    }

    companion object {
        private const val BYTES_PER_MB = 1_048_576L
        const val MODEL_FILE_NAME = "UVR-MDX-NET-Inst_HQ_4.onnx"
        const val MODEL_URL =
            "https://github.com/TRvlvr/model_repo/releases/download/all_public_uvr_models/UVR-MDX-NET-Inst_HQ_4.onnx"
        const val MODEL_EXPECTED_BYTES = 59_074_342L
        const val MODEL_SHA256 = "3c4b5b9b05090fdf238f38ba5046813982d50e2a652e9cb3324ea79720c3c9c8"
    }
}
