package com.lastwave.app.playback

import android.content.Context
import android.net.Uri
import com.lastwave.app.playback.dsd.DsdNativeDecimator
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.io.SequenceInputStream
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import kotlin.math.pow

data class LocalAudioInspection(
    val mimeType: String,
    val badge: String,
    val isLossless: Boolean,
    val bitDepth: Int? = null,
    val samplingRateKHz: Double? = null,
    val bitrateKbps: Int? = null,
    val durationMs: Long? = null,
)

/**
 * Container-level audio stream properties parsed straight from file headers.
 * Works uniformly across Android API levels and OEMs without depending on
 * `MediaMetadataRetriever` API 31+ keys.
 */
data class AudioHeaderInfo(
    val containerLabel: String,
    val sampleRateHz: Int? = null,
    val bitDepth: Int? = null,
    val channels: Int? = null,
    val bitrateKbps: Int? = null,
    val durationMs: Long? = null,
    val isLossless: Boolean = false,
    val isDsd: Boolean = false,
    val dsdBadge: String? = null,
    val dsdOutputRateHz: Int? = null,
)

/**
 * Lightweight header parser covering FLAC, WAV/RF64, AIFF/AIFC, DSF, DFF (DSDIFF),
 * and MP4/M4A/ALAC atoms. All reads are bounded to tiny header prefixes so probing
 * local or SAF `content://` tracks stays fast on every device.
 */
object AudioHeaderInspector {

    fun inspect(context: Context, urlOrUri: String, mimeHint: String? = null): AudioHeaderInfo? {
        return runCatching {
            val lower = urlOrUri.lowercase()
            val hint = mimeHint ?: lower
            if (urlOrUri.startsWith("content://")) {
                val uri = Uri.parse(urlOrUri)
                val pfd = runCatching { context.contentResolver.openFileDescriptor(uri, "r") }.getOrNull()
                if (pfd != null) {
                    pfd.use { fd ->
                        val size = fd.statSize
                        FileInputStream(fd.fileDescriptor).use { fis ->
                            val ch = fis.channel
                            val rewindable = size >= 12L && runCatching { ch.position(0); true }.getOrDefault(false)
                            if (rewindable) {
                                return@runCatching if (isMp4Channel(ch, size)) {
                                    inspectMp4Reader(ChannelMp4Reader(ch, size))
                                } else {
                                    ch.position(0)
                                    inspectStream(fis, hint)
                                }
                            }
                        }
                    }
                }
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    inspectStream(stream, hint)
                }
            } else {
                val path = urlOrUri.removePrefix("file://")
                val file = File(path).takeIf { it.isFile && it.canRead() } ?: return@runCatching null
                FileInputStream(file).use { fis ->
                    val ch = fis.channel
                    val size = file.length()
                    if (isMp4Channel(ch, size)) {
                        inspectMp4Reader(ChannelMp4Reader(ch, size))
                    } else {
                        ch.position(0)
                        inspectStream(fis, hint)
                    }
                }
            }
        }.getOrNull()
    }

    /**
     * Unified local audio probe used by both stream resolution and notification/UI
     * badge publishing. Combines container-header facts with `MediaMetadataRetriever`
     * without assuming bit depths on unknown containers.
     */
    fun inspectLocalTrackQuality(
        context: Context,
        targetUrl: String,
        fallbackMime: String? = null,
        knownBitrate: Int? = null,
        knownDurationMs: Long? = null,
    ): LocalAudioInspection {
        val uri = if (targetUrl.startsWith("/") && !targetUrl.startsWith("file://")) {
            Uri.fromFile(File(targetUrl))
        } else {
            Uri.parse(targetUrl)
        }
        val decodedLower = (runCatching { Uri.decode(uri.path ?: targetUrl) }.getOrNull() ?: targetUrl).lowercase()
        val extMime = com.lastwave.app.data.generate.LocalAudioFormats.mimeFor(targetUrl)
        var effectiveMime = fallbackMime?.takeIf(String::isNotBlank)
            ?: extMime
            ?: if (targetUrl.startsWith("content://")) {
                runCatching { context.contentResolver.getType(uri) }.getOrNull()
            } else {
                null
            }
            ?: "audio/flac"

        var bitrateKbps: Int? = knownBitrate?.takeIf { it > 0 }
        var bitDepth: Int? = null
        var samplingRateKHz: Double? = null
        var fileDurationMs: Long? = null

        val header = inspect(context, targetUrl, effectiveMime)

        if (header?.isDsd != true) {
            val retriever = android.media.MediaMetadataRetriever()
            try {
                if (targetUrl.startsWith("content://")) {
                    retriever.setDataSource(context, uri)
                } else {
                    retriever.setDataSource(uri.path ?: targetUrl.removePrefix("file://"))
                }
                retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_MIMETYPE)
                    ?.lowercase()?.takeIf { it.isNotBlank() }?.let { rMime ->
                        if (fallbackMime.isNullOrBlank() && extMime == null) {
                            effectiveMime = rMime
                        }
                    }
                fileDurationMs = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)
                    ?.toLongOrNull()?.takeIf { it > 0L }
                if (bitrateKbps == null) {
                    bitrateKbps = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_BITRATE)
                        ?.toIntOrNull()?.let { it / 1000 }?.takeIf { it > 0 }
                }
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                    samplingRateKHz = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_SAMPLERATE)
                        ?.toDoubleOrNull()?.let { it / 1000.0 }?.takeIf { it > 0.0 }
                    bitDepth = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_BITS_PER_SAMPLE)
                        ?.toIntOrNull()?.takeIf { it > 0 }
                }
            } catch (_: Exception) {
                // Header probe below still identifies DSD, AIFF, WAV, FLAC, ALAC cleanly.
            } finally {
                runCatching { retriever.release() }
            }
        }

        if (header != null) {
            if (header.isDsd) {
                val outRateKHz = (header.dsdOutputRateHz ?: 176_400) / 1000.0
                return LocalAudioInspection(
                    mimeType = effectiveMime,
                    badge = header.dsdBadge ?: "DSD64",
                    isLossless = true,
                    bitDepth = 1,
                    samplingRateKHz = outRateKHz,
                    bitrateKbps = bitrateKbps,
                    durationMs = header.durationMs ?: fileDurationMs ?: knownDurationMs?.takeIf { it > 0L },
                )
            }
            if (header.containerLabel == "ALAC") {
                effectiveMime = "audio/alac"
            }
            // Prefer container header facts on lossless codecs because Android MediaMetadataRetriever
            // often omits BITS_PER_SAMPLE or reports 16-bit on 24-bit ALAC files.
            val headerDepth = header.bitDepth?.takeIf { it > 0 }
            if (bitDepth == null || (header.isLossless && headerDepth != null)) {
                if (headerDepth != null) bitDepth = headerDepth
            }
            val headerRateKHz = header.sampleRateHz?.takeIf { it > 0 }?.div(1000.0)
            if (samplingRateKHz == null || (header.isLossless && headerRateKHz != null)) {
                if (headerRateKHz != null) samplingRateKHz = headerRateKHz
            }
            if (bitrateKbps == null) bitrateKbps = header.bitrateKbps?.takeIf { it > 0 }
            if (fileDurationMs == null) fileDurationMs = header.durationMs?.takeIf { it > 0L }
        }

        val m = effectiveMime.lowercase()
        val isAlac = header?.containerLabel == "ALAC" || m.contains("alac") || decodedLower.endsWith(".alac")
        val isAiff = header?.containerLabel == "AIFF" || m.contains("aiff") ||
            decodedLower.endsWith(".aiff") || decodedLower.endsWith(".aif") || decodedLower.endsWith(".aifc")
        val isWav = header?.containerLabel == "WAV" || m.contains("wav") ||
            decodedLower.endsWith(".wav") || decodedLower.endsWith(".wave")
        val isFlac = header?.containerLabel == "FLAC" || m.contains("flac") || decodedLower.endsWith(".flac")
        val isLossless = (header?.isLossless == true) || isFlac || isWav || isAiff || isAlac

        val containerName = when {
            isAiff -> "AIFF"
            isWav -> "WAV"
            isAlac -> "ALAC"
            else -> "FLAC"
        }
        val resolvedBadge = when {
            header?.containerLabel == "DOLBY" || m.contains("eac3") || m.contains("ec-3") ||
                m.contains("ac3") || m.contains("ac-3") ||
                decodedLower.endsWith(".ac3") || decodedLower.endsWith(".ec3") || decodedLower.endsWith(".eac3") -> "DOLBY"
            header?.containerLabel == "DOLBY AC-4" || m.contains("ac4") || decodedLower.endsWith(".ac4") -> "DOLBY AC-4"
            isLossless -> {
                val d = bitDepth?.takeIf { it > 0 }
                val r = samplingRateKHz?.takeIf { it > 0.0 }
                when {
                    isAlac && d != null && r != null -> "ALAC $d/${formatSampleRateKHz(r)}kHz"
                    d != null && r != null -> "$d/${formatSampleRateKHz(r)}kHz"
                    r != null -> "${formatSampleRateKHz(r)}kHz $containerName"
                    (d ?: 0) > 16 -> "HI-RES $containerName"
                    else -> containerName
                }
            }
            header?.containerLabel == "OPUS" || m.contains("opus") -> "OPUS"
            m.contains("ogg") || decodedLower.endsWith(".ogg") || decodedLower.endsWith(".oga") -> "OGG"
            header?.containerLabel == "AAC" || m.contains("mp4") || m.contains("m4a") || m.contains("aac") ||
                decodedLower.endsWith(".m4a") || decodedLower.endsWith(".aac") -> "AAC"
            m.contains("mp3") || m.contains("mpeg") || decodedLower.endsWith(".mp3") || decodedLower.endsWith(".mp2") -> "MP3"
            m.contains("matroska") || decodedLower.endsWith(".mka") || decodedLower.endsWith(".webm") -> "MKA"
            else -> "AUDIO"
        }

        return LocalAudioInspection(
            mimeType = effectiveMime,
            badge = resolvedBadge,
            isLossless = isLossless,
            bitDepth = if (isLossless) bitDepth?.takeIf { it > 0 } else null,
            samplingRateKHz = samplingRateKHz?.takeIf { it > 0.0 },
            bitrateKbps = bitrateKbps,
            durationMs = fileDurationMs ?: knownDurationMs?.takeIf { it > 0L },
        )
    }

    fun inspectStream(input: InputStream, hint: String = ""): AudioHeaderInfo? {
        val probe = ByteArray(16 * 1024)
        var filled = 0
        while (filled < probe.size) {
            val n = input.read(probe, filled, probe.size - filled)
            if (n <= 0) break
            filled += n
        }
        if (filled < 12) return null

        val head = probe.copyOf(filled)
        val fourCc = String(head, 0, 4, Charsets.US_ASCII)

        // 1. DSF ('DSD ')
        if (fourCc == "DSD " && head.size >= 80) {
            return parseDsfHeader(head)
        }

        // 2. DFF / DSDIFF ('FRM8' ... 'DSD ')
        if (fourCc == "FRM8" && head.size >= 16 &&
            String(head, 12, 4, Charsets.US_ASCII) == "DSD "
        ) {
            return parseDffHeader(head)
        }

        // 3. AIFF / AIFC ('FORM' ... 'AIFF' / 'AIFC')
        if (fourCc == "FORM" && head.size >= 16) {
            val sub = String(head, 8, 4, Charsets.US_ASCII)
            if (sub == "AIFF" || sub == "AIFC") {
                return parseAiffHeader(head, isCompressed = sub == "AIFC")
            }
        }

        // 4. WAV ('RIFF' ... 'WAVE')
        if ((fourCc == "RIFF" || fourCc == "RF64") && head.size >= 36 &&
            String(head, 8, 4, Charsets.US_ASCII) == "WAVE"
        ) {
            return parseWavHeader(head)
        }

        // 5. FLAC (including optional leading ID3v2)
        if (fourCc == "fLaC" || (head[0] == 'I'.code.toByte() && head[1] == 'D'.code.toByte() && head[2] == '3'.code.toByte() && hint.contains("flac"))) {
            val combined = SequenceInputStream(ByteArrayInputStream(head), input)
            val info = readFlacStreamInfo(combined)
            if (info != null) {
                val sr = info.sampleRateHz ?: 0
                val ts = info.totalSamples ?: 0L
                val durMs = if (sr > 0 && ts > 0L) (ts * 1000L) / sr else null
                return AudioHeaderInfo(
                    containerLabel = "FLAC",
                    sampleRateHz = info.sampleRateHz,
                    bitDepth = info.bitDepth,
                    channels = info.channels,
                    durationMs = durMs,
                    isLossless = true,
                )
            }
        }

        // 6. MP4 / M4A / ALAC ('ftyp' at offset 4) — walk atoms beyond the 16 KB prefix
        // because moov often sits after mdat or embedded cover art in udta/meta.
        if (head.size >= 24 && String(head, 4, 4, Charsets.US_ASCII) == "ftyp") {
            val combined = SequenceInputStream(ByteArrayInputStream(head), input)
            return inspectMp4Reader(StreamMp4Reader(combined))
        }

        return null
    }

    internal fun parseDsfHeader(buf: ByteArray): AudioHeaderInfo? {
        if (buf.size < 80) return null
        if (String(buf, 0, 4, Charsets.US_ASCII) != "DSD ") return null
        val fmtOffset = leU64(buf, 4).toInt().coerceIn(28, 256)
        if (buf.size < fmtOffset + 52) return null
        if (String(buf, fmtOffset, 4, Charsets.US_ASCII) != "fmt ") return null
        val formatId = leU32(buf, fmtOffset + 16).toInt()
        if (formatId != 0) return null
        val channels = leU32(buf, fmtOffset + 24).toInt().coerceIn(1, 8)
        val dsdRateHz = leU32(buf, fmtOffset + 28).toInt().takeIf { it > 0 } ?: return null
        val sampleCount = leU64(buf, fmtOffset + 36)
        val durationMs = if (sampleCount > 0L) (sampleCount * 1000L) / dsdRateHz else null
        val badge = DsdNativeDecimator.dsdBadgeForRate(dsdRateHz)
        val factor = DsdNativeDecimator.chooseDecimationFactor(dsdRateHz)
        return AudioHeaderInfo(
            containerLabel = badge,
            sampleRateHz = dsdRateHz,
            bitDepth = 1,
            channels = channels,
            durationMs = durationMs,
            isLossless = true,
            isDsd = true,
            dsdBadge = badge,
            dsdOutputRateHz = (dsdRateHz / factor).coerceAtLeast(44_100),
        )
    }

    internal fun parseDffHeader(buf: ByteArray): AudioHeaderInfo? {
        if (buf.size < 16 || String(buf, 0, 4, Charsets.US_ASCII) != "FRM8") return null
        if (String(buf, 12, 4, Charsets.US_ASCII) != "DSD ") return null
        var pos = 16
        var dsdRateHz: Int? = null
        var channels: Int? = null
        var isUncompressed = true
        var dsdDataBytes: Long? = null

        while (pos + 12 <= buf.size) {
            val id = String(buf, pos, 4, Charsets.US_ASCII)
            val size = beU64(buf, pos + 4)
            val bodyStart = pos + 12
            if (size < 0L) break
            if (id == "PROP" && bodyStart + 4 <= buf.size) {
                if (String(buf, bodyStart, 4, Charsets.US_ASCII) == "SND ") {
                    var sub = bodyStart + 4
                    val propEnd = minOf(buf.size.toLong(), bodyStart.toLong() + size).toInt()
                    while (sub + 12 <= propEnd) {
                        val subId = String(buf, sub, 4, Charsets.US_ASCII)
                        val subSize = beU64(buf, sub + 4)
                        val subBody = sub + 12
                        when {
                            subId == "FS  " && subBody + 4 <= buf.size ->
                                dsdRateHz = beU32(buf, subBody).toInt()
                            subId == "CHNL" && subBody + 2 <= buf.size ->
                                channels = beU16(buf, subBody)
                            subId == "CMPR" && subBody + 4 <= buf.size -> {
                                val cmpr = String(buf, subBody, 4, Charsets.US_ASCII)
                                isUncompressed = (cmpr == "DSD ")
                            }
                        }
                        if (subSize <= 0L) break
                        val step = subBody + subSize + (subSize and 1L)
                        if (step > buf.size) break
                        sub = step.toInt()
                    }
                }
            } else if (id == "DSD ") {
                dsdDataBytes = size
                break
            }
            val next = bodyStart.toLong() + size + (size and 1L)
            if (next <= pos || next > Int.MAX_VALUE) break
            if (next >= buf.size) break
            pos = next.toInt()
        }

        if (!isUncompressed || dsdRateHz == null || dsdRateHz <= 0) return null
        val ch = (channels ?: 2).coerceIn(1, 8)
        val badge = DsdNativeDecimator.dsdBadgeForRate(dsdRateHz)
        val factor = DsdNativeDecimator.chooseDecimationFactor(dsdRateHz)
        val durationMs = dsdDataBytes?.let { bytes ->
            // bytes / ch = 8 DSD bit-periods
            val sampleBits = (bytes / ch) * 8L
            (sampleBits * 1000L) / dsdRateHz
        }
        return AudioHeaderInfo(
            containerLabel = badge,
            sampleRateHz = dsdRateHz,
            bitDepth = 1,
            channels = ch,
            durationMs = durationMs,
            isLossless = true,
            isDsd = true,
            dsdBadge = badge,
            dsdOutputRateHz = (dsdRateHz / factor).coerceAtLeast(44_100),
        )
    }

    internal fun parseAiffHeader(buf: ByteArray, isCompressed: Boolean): AudioHeaderInfo? {
        if (buf.size < 16) return null
        var pos = 12
        while (pos + 8 <= buf.size) {
            val id = String(buf, pos, 4, Charsets.US_ASCII)
            val size = beU32(buf, pos + 4)
            val body = pos + 8
            if (id == "COMM" && body + 18 <= buf.size) {
                val channels = beU16(buf, body).coerceIn(1, 16)
                val frames = beU32(buf, body + 2)
                val bitsPerSample = beU16(buf, body + 6)
                val sampleRateHz = decodeIeeeExtended80(buf, body + 8).toInt().takeIf { it > 0 } ?: return null
                if (isCompressed && body + 22 <= buf.size) {
                    val comp = String(buf, body + 18, 4, Charsets.US_ASCII)
                    val ok = comp == "NONE" || comp == "sowt" || comp == "twos" ||
                        comp.equals("fl32", ignoreCase = true) || comp.equals("fl64", ignoreCase = true)
                    if (!ok) return null
                }
                val durMs = if (frames > 0L) (frames * 1000L) / sampleRateHz else null
                val kbps = ((sampleRateHz.toLong() * channels * bitsPerSample) / 1000L).toInt().takeIf { it > 0 }
                return AudioHeaderInfo(
                    containerLabel = "AIFF",
                    sampleRateHz = sampleRateHz,
                    bitDepth = bitsPerSample.takeIf { it > 0 },
                    channels = channels,
                    bitrateKbps = kbps,
                    durationMs = durMs,
                    isLossless = true,
                )
            }
            if (size < 0L) break
            val next = body.toLong() + size + (size and 1L)
            if (next <= pos || next > buf.size) break
            pos = next.toInt()
        }
        return null
    }

    internal fun parseWavHeader(buf: ByteArray): AudioHeaderInfo? {
        if (buf.size < 36) return null
        var pos = 12
        var channels: Int? = null
        var sampleRateHz: Int? = null
        var bitDepth: Int? = null
        var audioFormat: Int? = null
        var dataBytes: Long? = null
        var byteRate: Long? = null

        while (pos + 8 <= buf.size) {
            val id = String(buf, pos, 4, Charsets.US_ASCII)
            val size = leU32(buf, pos + 4)
            val body = pos + 8
            if (id == "fmt " && body + 16 <= buf.size) {
                audioFormat = leU16(buf, body)
                channels = leU16(buf, body + 2)
                sampleRateHz = leU32(buf, body + 4).toInt()
                byteRate = leU32(buf, body + 8)
                bitDepth = leU16(buf, body + 14)
                // WAVE_FORMAT_EXTENSIBLE (0xFFFE) can specify validBitsPerSample at body+18
                if (audioFormat == 0xFFFE && size >= 40 && body + 26 <= buf.size) {
                    val validBits = leU16(buf, body + 18)
                    if (validBits in 8..64) bitDepth = validBits
                    audioFormat = leU16(buf, body + 24)
                }
            } else if (id == "data") {
                dataBytes = size
                break
            }
            if (size < 0L) break
            val next = body.toLong() + size + (size and 1L)
            if (next <= pos || next > buf.size) break
            pos = next.toInt()
        }

        val sr = sampleRateHz?.takeIf { it > 0 } ?: return null
        val isPcmLossless = audioFormat == 1 || audioFormat == 3
        val ch = (channels ?: 2).coerceAtLeast(1)
        val durMs = if (dataBytes != null && byteRate != null && byteRate > 0L) {
            (dataBytes * 1000L) / byteRate
        } else null
        val kbps = byteRate?.let { ((it * 8L) / 1000L).toInt() }?.takeIf { it > 0 }
        return AudioHeaderInfo(
            containerLabel = "WAV",
            sampleRateHz = sr,
            bitDepth = bitDepth?.takeIf { it > 0 },
            channels = ch,
            bitrateKbps = kbps,
            durationMs = durMs,
            isLossless = isPcmLossless,
        )
    }

    internal fun parseMp4AlacHeader(buf: ByteArray): AudioHeaderInfo? =
        parseMp4AlacRange(buf, startIndex = 8, endIndex = buf.size)

    private fun parseMp4AlacRange(buf: ByteArray, startIndex: Int, endIndex: Int): AudioHeaderInfo? {
        // Scan buffered slice for the 24-byte ALACSpecificConfig inside an 'alac' FullBox atom.
        // Layout: [size(4)][alac(4)][version+flags(4)] +
        //   alacSpecificConfig: frameLength(4), compatibleVersion(1), bitDepth(1),
        //   pb(1), mb(1), kb(1), numChannels(1), maxRun(2), maxFrameBytes(4),
        //   avgBitRate(4), sampleRate(4) -> total 24 bytes after version+flags.
        val limit = minOf(endIndex, buf.size)
        var i = startIndex.coerceAtLeast(4)
        while (i + 32 <= limit) {
            if (buf[i] == 'a'.code.toByte() && buf[i + 1] == 'l'.code.toByte() &&
                buf[i + 2] == 'a'.code.toByte() && buf[i + 3] == 'c'.code.toByte()
            ) {
                parseAlacConfigAt(buf, i, limit)?.let { return it }
            }
            i++
        }
        return null
    }

    private fun parseAlacConfigAt(buf: ByteArray, typeOffset: Int, limit: Int): AudioHeaderInfo? {
        if (typeOffset < 4 || typeOffset + 32 > limit) return null
        val atomSize = beU32(buf, typeOffset - 4)
        if (atomSize !in 36..64) return null
        val version = buf[typeOffset + 4].toInt() and 0xFF
        val cfg = typeOffset + 8
        if (version != 0 || cfg + 24 > limit) return null
        val depth = buf[cfg + 5].toInt() and 0xFF
        val ch = buf[cfg + 9].toInt() and 0xFF
        val avgBitRate = beU32(buf, cfg + 16).toInt()
        val sr = beU32(buf, cfg + 20).toInt()
        if (depth !in 16..32 || ch !in 1..8 || sr !in 8_000..384_000) return null
        return AudioHeaderInfo(
            containerLabel = "ALAC",
            sampleRateHz = sr,
            bitDepth = depth,
            channels = ch,
            bitrateKbps = (avgBitRate / 1000).takeIf { it > 0 },
            isLossless = true,
        )
    }

    private interface Mp4Reader {
        val length: Long
        fun position(): Long
        fun seekOrSkipTo(targetPos: Long): Boolean
        fun readExact(dst: ByteArray, len: Int): Boolean
    }

    private class ChannelMp4Reader(
        private val ch: FileChannel,
        override val length: Long,
    ) : Mp4Reader {
        override fun position(): Long = ch.position()

        override fun seekOrSkipTo(targetPos: Long): Boolean {
            if (targetPos < 0L || targetPos > length) return false
            ch.position(targetPos)
            return true
        }

        override fun readExact(dst: ByteArray, len: Int): Boolean {
            val buf = ByteBuffer.wrap(dst, 0, len)
            while (buf.hasRemaining()) {
                if (ch.read(buf) <= 0) return false
            }
            return true
        }
    }

    private class StreamMp4Reader(private val stream: InputStream) : Mp4Reader {
        private var pos = 0L
        private val skipScratch = ByteArray(4096)
        override val length: Long = Long.MAX_VALUE

        override fun position(): Long = pos

        override fun seekOrSkipTo(targetPos: Long): Boolean {
            var remaining = targetPos - pos
            if (remaining < 0L) return false
            while (remaining > 0L) {
                val skipped = stream.skip(remaining)
                if (skipped > 0L) {
                    pos += skipped
                    remaining -= skipped
                } else {
                    val toRead = minOf(remaining, skipScratch.size.toLong()).toInt()
                    val n = stream.read(skipScratch, 0, toRead)
                    if (n <= 0) return false
                    pos += n
                    remaining -= n
                }
            }
            return true
        }

        override fun readExact(dst: ByteArray, len: Int): Boolean {
            var total = 0
            while (total < len) {
                val n = stream.read(dst, total, len - total)
                if (n <= 0) return false
                total += n
                pos += n
            }
            return true
        }
    }

    private class Mp4WalkState {
        var durationMs: Long? = null
        var lossless: AudioHeaderInfo? = null
        var lossy: AudioHeaderInfo? = null
    }

    private fun isMp4Channel(ch: FileChannel, size: Long): Boolean {
        if (size < 12L) return false
        ch.position(0)
        val head = ByteArray(12)
        val ok = ChannelMp4Reader(ch, size).readExact(head, 12)
        ch.position(0)
        return ok && String(head, 4, 4, Charsets.US_ASCII) == "ftyp"
    }

    private fun inspectMp4Reader(reader: Mp4Reader): AudioHeaderInfo? {
        val state = Mp4WalkState()
        walkMp4Container(reader, reader.length, depth = 0, state = state)
        val best = state.lossless ?: state.lossy ?: return null
        return if (best.durationMs == null && state.durationMs != null) {
            best.copy(durationMs = state.durationMs)
        } else {
            best
        }
    }

    private fun walkMp4Container(
        reader: Mp4Reader,
        limitPos: Long,
        depth: Int,
        state: Mp4WalkState,
    ) {
        if (depth > 8) return
        val hdr = ByteArray(8)
        var boxesSeen = 0
        while (reader.position() + 8L <= limitPos && boxesSeen < 512) {
            val boxStart = reader.position()
            if (!reader.readExact(hdr, 8)) return
            boxesSeen++
            val size32 = beU32(hdr, 0)
            val type = String(hdr, 4, 4, Charsets.US_ASCII)
            val headerLen: Long
            val boxSize: Long
            when (size32) {
                0L -> {
                    if (reader.length == Long.MAX_VALUE) return
                    boxSize = limitPos - boxStart
                    headerLen = 8L
                }
                1L -> {
                    val ext = ByteArray(8)
                    if (!reader.readExact(ext, 8)) return
                    boxSize = beU64(ext, 0)
                    headerLen = 16L
                }
                else -> {
                    boxSize = size32
                    headerLen = 8L
                }
            }
            if (boxSize < headerLen) return
            val nextPos = boxStart + boxSize
            val payloadSize = boxSize - headerLen
            val containerEnd = minOf(nextPos, limitPos)

            when (type) {
                // Handle synthetic top-level or standalone 'alac' config box directly
                "alac" -> {
                    if (payloadSize in 28L..64L) {
                        val payload = ByteArray(payloadSize.toInt())
                        if (reader.readExact(payload, payload.size)) {
                            val fullBox = ByteArray(8 + payload.size)
                            System.arraycopy(hdr, 0, fullBox, 0, 8)
                            System.arraycopy(payload, 0, fullBox, 8, payload.size)
                            parseAlacConfigAt(fullBox, typeOffset = 4, limit = fullBox.size)?.let {
                                state.lossless = it
                                return
                            }
                        }
                    }
                }
                "mvhd" -> {
                    if (payloadSize >= 20L) {
                        val cap = minOf(payloadSize, 64L).toInt()
                        val buf = ByteArray(cap)
                        if (reader.readExact(buf, cap)) {
                            parseMvhdDurationMs(buf)?.let { state.durationMs = it }
                        }
                    }
                }
                "moov", "trak", "mdia", "minf", "stbl" -> {
                    walkMp4Container(reader, containerEnd, depth + 1, state)
                    if (state.lossless != null && state.durationMs != null) return
                }
                "stsd" -> {
                    if (payloadSize >= 16L) {
                        val cap = minOf(payloadSize, 64L * 1024L).toInt()
                        val buf = ByteArray(cap)
                        if (reader.readExact(buf, cap)) {
                            parseStsdBox(buf)?.let { parsed ->
                                if (parsed.isLossless) {
                                    state.lossless = parsed
                                } else if (state.lossy == null) {
                                    state.lossy = parsed
                                }
                            }
                        }
                    }
                }
            }

            if (reader.position() != nextPos && !reader.seekOrSkipTo(nextPos)) return
        }
    }

    private fun parseMvhdDurationMs(buf: ByteArray): Long? {
        if (buf.size < 20) return null
        val version = buf[0].toInt() and 0xFF
        val timescale: Long
        val duration: Long
        when (version) {
            0 -> {
                timescale = beU32(buf, 12)
                duration = beU32(buf, 16)
                if (duration == 0xFFFF_FFFFL) return null
            }
            1 -> {
                if (buf.size < 32) return null
                timescale = beU32(buf, 20)
                duration = beU64(buf, 24)
            }
            else -> return null
        }
        if (timescale <= 0L || duration <= 0L) return null
        return (duration * 1000L) / timescale
    }

    private fun parseStsdBox(buf: ByteArray): AudioHeaderInfo? {
        if (buf.size < 16) return null
        val entryCount = minOf(beU32(buf, 4), 16L).toInt()
        var pos = 8
        var lossyFallback: AudioHeaderInfo? = null
        for (idx in 0 until entryCount) {
            if (pos + 8 > buf.size) break
            val entrySizeLong = beU32(buf, pos)
            if (entrySizeLong < 8L || pos + entrySizeLong > buf.size) break
            val entrySize = entrySizeLong.toInt()
            val entryEnd = pos + entrySize
            val codecId = String(buf, pos + 4, 4, Charsets.US_ASCII)
            val entryChannels = if (pos + 26 <= entryEnd) beU16(buf, pos + 24).takeIf { it in 1..16 } else null
            val entryBits = if (pos + 28 <= entryEnd) beU16(buf, pos + 26).takeIf { it in 8..32 } else null
            val entrySr = if (pos + 34 <= entryEnd) beU16(buf, pos + 32).takeIf { it in 8_000..65_535 } else null

            when (codecId) {
                "alac" -> {
                    val nested = parseMp4AlacRange(buf, startIndex = pos + 16, endIndex = entryEnd)
                    if (nested != null) return nested
                    return AudioHeaderInfo(
                        containerLabel = "ALAC",
                        sampleRateHz = entrySr,
                        bitDepth = entryBits,
                        channels = entryChannels,
                        isLossless = true,
                    )
                }
                "fLaC" -> {
                    val flacInfo = findMp4DfLaStreamInfo(buf, pos + 16, entryEnd)
                    return AudioHeaderInfo(
                        containerLabel = "FLAC",
                        sampleRateHz = flacInfo?.sampleRateHz ?: entrySr,
                        bitDepth = flacInfo?.bitDepth ?: entryBits,
                        channels = flacInfo?.channels ?: entryChannels,
                        isLossless = true,
                    )
                }
                "ec-3", "ac-3" -> if (lossyFallback == null) {
                    lossyFallback = AudioHeaderInfo("DOLBY", sampleRateHz = entrySr, channels = entryChannels, isLossless = false)
                }
                "ac-4" -> if (lossyFallback == null) {
                    lossyFallback = AudioHeaderInfo("DOLBY AC-4", sampleRateHz = entrySr, channels = entryChannels, isLossless = false)
                }
                "Opus" -> if (lossyFallback == null) {
                    lossyFallback = AudioHeaderInfo("OPUS", sampleRateHz = entrySr ?: 48_000, channels = entryChannels, isLossless = false)
                }
                "mp4a" -> if (lossyFallback == null) {
                    lossyFallback = AudioHeaderInfo("AAC", sampleRateHz = entrySr, channels = entryChannels, isLossless = false)
                }
            }
            pos = entryEnd
        }
        return lossyFallback
    }

    private fun findMp4DfLaStreamInfo(buf: ByteArray, start: Int, end: Int): FlacStreamInfo? {
        var i = start.coerceAtLeast(0)
        val limit = minOf(end, buf.size)
        while (i + 46 <= limit) {
            if (buf[i] == 'd'.code.toByte() && buf[i + 1] == 'f'.code.toByte() &&
                buf[i + 2] == 'L'.code.toByte() && buf[i + 3] == 'a'.code.toByte()
            ) {
                val blockType = buf[i + 8].toInt() and 0x7F
                if (blockType == 0) {
                    parseFlacStreamInfoBody(buf, i + 12)?.let { return it }
                }
            }
            i++
        }
        return null
    }

    /** Decodes 10-byte IEEE 754 standard extended float (big-endian), used in AIFF COMM chunks. */
    fun decodeIeeeExtended80(buf: ByteArray, offset: Int): Double {
        if (offset + 10 > buf.size) return 0.0
        val expHi = buf[offset].toInt() and 0xFF
        val expLo = buf[offset + 1].toInt() and 0xFF
        val sign = (expHi and 0x80) != 0
        val expon = ((expHi and 0x7F) shl 8) or expLo
        var hiMant = 0L
        for (i in 2..5) {
            hiMant = (hiMant shl 8) or (buf[offset + i].toLong() and 0xFFL)
        }
        var loMant = 0L
        for (i in 6..9) {
            loMant = (loMant shl 8) or (buf[offset + i].toLong() and 0xFFL)
        }
        if (expon == 0 && hiMant == 0L && loMant == 0L) return 0.0
        if (expon == 0x7FFF) return Double.NaN
        val e = expon - 16383
        val hi = hiMant.toDouble() * 2.0.pow(e - 31)
        val lo = loMant.toDouble() * 2.0.pow(e - 63)
        val value = hi + lo
        return if (sign) -value else value
    }

    fun leU16(b: ByteArray, o: Int): Int =
        (b[o].toInt() and 0xFF) or ((b[o + 1].toInt() and 0xFF) shl 8)

    fun leU32(b: ByteArray, o: Int): Long =
        (b[o].toLong() and 0xFFL) or
            ((b[o + 1].toLong() and 0xFFL) shl 8) or
            ((b[o + 2].toLong() and 0xFFL) shl 16) or
            ((b[o + 3].toLong() and 0xFFL) shl 24)

    fun leU64(b: ByteArray, o: Int): Long {
        var r = 0L
        for (i in 7 downTo 0) {
            r = (r shl 8) or (b[o + i].toLong() and 0xFFL)
        }
        return r
    }

    fun beU16(b: ByteArray, o: Int): Int =
        ((b[o].toInt() and 0xFF) shl 8) or (b[o + 1].toInt() and 0xFF)

    fun beU32(b: ByteArray, o: Int): Long =
        ((b[o].toLong() and 0xFFL) shl 24) or
            ((b[o + 1].toLong() and 0xFFL) shl 16) or
            ((b[o + 2].toLong() and 0xFFL) shl 8) or
            (b[o + 3].toLong() and 0xFFL)

    fun beU64(b: ByteArray, o: Int): Long {
        var r = 0L
        for (i in 0..7) {
            r = (r shl 8) or (b[o + i].toLong() and 0xFFL)
        }
        return r
    }
}
