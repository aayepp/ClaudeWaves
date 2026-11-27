@file:OptIn(androidx.media3.common.util.UnstableApi::class)

package com.lastwave.app.playback.karaoke

import android.media.MediaDataSource
import android.net.Uri
import androidx.media3.common.C
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import java.io.IOException

/**
 * Exposes a Media3 [DataSource] (cache + ChunkedRangeDataSource + signed headers) as an
 * Android [MediaDataSource] for [android.media.MediaExtractor]. Reuse sequential sockets
 * and reopen only on non-contiguous seeks.
 */
internal class Media3MediaDataSource(
    private val dataSourceFactory: DataSource.Factory,
    private val uri: Uri,
    private val cacheKey: String?,
    private val headers: Map<String, String>,
    private val knownLength: Long = C.LENGTH_UNSET.toLong(),
) : MediaDataSource() {

    private var activeSource: DataSource? = null
    private var cursorPos: Long = 0L
    private var resolvedSize: Long = knownLength
    private var closed = false

    @Synchronized
    override fun readAt(position: Long, buffer: ByteArray, offset: Int, size: Int): Int {
        if (closed || position < 0L) return -1
        if (size == 0) return 0
        if (resolvedSize > 0L && position >= resolvedSize) return -1

        val currentSource = activeSource
        val needReopen = currentSource == null || position != cursorPos
        if (needReopen) {
            closeActiveQuietly()
            val ds = dataSourceFactory.createDataSource()
            val spec = DataSpec.Builder()
                .setUri(uri)
                .setKey(cacheKey)
                .setPosition(position)
                .setHttpRequestHeaders(headers)
                .build()
            val openedLen = ds.open(spec)
            if (resolvedSize <= 0L && openedLen != C.LENGTH_UNSET.toLong()) {
                resolvedSize = position + openedLen
            }
            activeSource = ds
            cursorPos = position
        }

        val ds = activeSource ?: return -1
        var totalRead = 0
        while (totalRead < size) {
            val read = try {
                ds.read(buffer, offset + totalRead, size - totalRead)
            } catch (e: IOException) {
                closeActiveQuietly()
                if (totalRead > 0) return totalRead
                throw e
            }
            if (read == C.RESULT_END_OF_INPUT) {
                if (resolvedSize <= 0L) resolvedSize = cursorPos
                break
            }
            if (read <= 0) break
            totalRead += read
            cursorPos += read
        }
        return if (totalRead == 0) -1 else totalRead
    }

    @Synchronized
    override fun getSize(): Long {
        if (closed) return -1L
        if (resolvedSize > 0L) return resolvedSize
        if (activeSource == null) {
            runCatching {
                val ds = dataSourceFactory.createDataSource()
                val spec = DataSpec.Builder()
                    .setUri(uri)
                    .setKey(cacheKey)
                    .setPosition(0L)
                    .setHttpRequestHeaders(headers)
                    .build()
                val openedLen = ds.open(spec)
                if (openedLen != C.LENGTH_UNSET.toLong()) {
                    resolvedSize = openedLen
                }
                activeSource = ds
                cursorPos = 0L
            }
        }
        return if (resolvedSize > 0L) resolvedSize else -1L
    }

    @Synchronized
    override fun close() {
        closed = true
        closeActiveQuietly()
    }

    private fun closeActiveQuietly() {
        val ds = activeSource
        activeSource = null
        if (ds != null) {
            runCatching { ds.close() }
        }
    }
}
