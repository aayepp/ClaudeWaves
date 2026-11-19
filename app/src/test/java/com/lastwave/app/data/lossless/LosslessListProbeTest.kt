package com.lastwave.app.data.lossless

import com.lastwave.app.data.addon.AddonTrack
import com.lastwave.app.data.addon.HybridMediaServer
import com.lastwave.app.data.local.HttpAddonConfig
import com.lastwave.app.data.local.SettingsPreferences
import com.lastwave.app.data.plugin.ModuleManager
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LosslessListProbeTest {

    private fun fakeApi(
        addonUrls: List<String> = listOf("https://addon.example"),
        httpHandler: (String) -> Pair<Int, String> = { 200 to """{"tracks":[]}""" },
    ): Pair<LosslessMusicApi, MutableList<String>> {
        val recordedUrls = mutableListOf<String>()
        val http = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val req = chain.request()
                val fullUrl = req.url.toString()
                recordedUrls += fullUrl
                val (code, json) = httpHandler(fullUrl)
                Response.Builder()
                    .request(req)
                    .protocol(Protocol.HTTP_1_1)
                    .code(code)
                    .message(if (code == 200) "OK" else "Error")
                    .body(json.toResponseBody())
                    .build()
            }
            .build()
        val settings = mockk<SettingsPreferences>()
        every { settings.httpAddons } returns MutableStateFlow(
            addonUrls.map { HttpAddonConfig(url = it, enabled = true) },
        )
        every { settings.addonUrl } returns MutableStateFlow(null)
        every { settings.addonEnabled } returns MutableStateFlow(false)
        val native = mockk<NativeSecrets>()
        every { native.signAddonRequest(any(), any()) } returns null
        val api = LosslessMusicApi(
            okHttpClient = http,
            moduleManager = mockk<ModuleManager>(),
            nativeSecrets = native,
            settingsPreferences = settings,
            hybridServer = mockk<HybridMediaServer>(relaxUnitFun = true),
        )
        return api to recordedUrls
    }

    @Test
    fun classifyAddonHitsDetectsHiResLosslessAtmosAndNone() {
        val (api, _) = fakeApi()

        val hiResTier = api.classifyAddonHitsForListTier(
            tracks = listOf(
                AddonTrack(
                    id = "1",
                    title = "Midnight City",
                    artist = "M83",
                    duration = 243.0,
                    audioQuality = "HI_RES_LOSSLESS",
                    rawBitDepth = JsonPrimitive(24),
                ),
            ),
            title = "Midnight City",
            artist = "M83",
            expectedDurationSeconds = 243,
        )
        assertEquals(LosslessListTier.HI_RES, hiResTier)

        val cdTier = api.classifyAddonHitsForListTier(
            tracks = listOf(
                AddonTrack(
                    id = "2",
                    title = "Midnight City",
                    artist = "M83",
                    duration = 243.0,
                    audioQuality = "LOSSLESS",
                    rawBitDepth = JsonPrimitive(16),
                ),
            ),
            title = "Midnight City",
            artist = "M83",
            expectedDurationSeconds = 243,
        )
        assertEquals(LosslessListTier.LOSSLESS, cdTier)

        val atmosTier = api.classifyAddonHitsForListTier(
            tracks = listOf(
                AddonTrack(
                    id = "3",
                    title = "Midnight City",
                    artist = "M83",
                    duration = 243.0,
                    atmos = true,
                    audioQuality = "DOLBY_ATMOS",
                ),
            ),
            title = "Midnight City",
            artist = "M83",
            expectedDurationSeconds = 243,
        )
        assertEquals(LosslessListTier.ATMOS, atmosTier)

        val lossyOnlyTier = api.classifyAddonHitsForListTier(
            tracks = listOf(
                AddonTrack(
                    id = "4",
                    title = "Midnight City",
                    artist = "M83",
                    duration = 243.0,
                    audioQuality = "HIGH",
                ),
            ),
            title = "Midnight City",
            artist = "M83",
            expectedDurationSeconds = 243,
        )
        assertEquals(LosslessListTier.NONE, lossyOnlyTier)
    }

    @Test
    fun stereoAndAtmosHitsKeepHiResTierAndAtmosMark() {
        val (api, _) = fakeApi()
        val hits = listOf(
            AddonTrack(
                id = "1", title = "Midnight City", artist = "M83", duration = 243.0,
                audioQuality = "HI_RES_LOSSLESS", rawBitDepth = JsonPrimitive(24),
            ),
            AddonTrack(
                id = "2", title = "Midnight City", artist = "M83", duration = 243.0,
                atmos = true, audioQuality = "DOLBY_ATMOS",
            ),
        )
        // The list tier is the stereo version, as before; the Atmos one is no longer lost.
        assertEquals(LosslessListTier.HI_RES, api.classifyAddonHitsForListTier(hits, "Midnight City", "M83", 243))
        assertTrue(api.addonHitsIncludeAtmos(hits, "Midnight City", "M83", 243))
        assertEquals(false, api.addonHitsIncludeAtmos(hits.take(1), "Midnight City", "M83", 243))
    }

    @Test
    fun probeTrackListTierUsesSearchOnlyAndNeverCallsStreamEndpoint() = runBlocking {
        val (api, recordedUrls) = fakeApi {
            200 to """
                {
                  "tracks": [
                    {
                      "id": "12345",
                      "title": "Nights",
                      "artist": "Frank Ocean",
                      "duration": 307,
                      "audioQuality": "HI_RES_LOSSLESS",
                      "bitDepth": 24
                    }
                  ]
                }
            """.trimIndent()
        }

        val tier = api.probeTrackListTier(
            title = "Nights",
            artist = "Frank Ocean",
            expectedDurationSeconds = 307,
        )

        assertEquals(LosslessListTier.HI_RES, tier?.tier)
        assertTrue("Expected search request", recordedUrls.any { it.contains("/search") })
        assertTrue("Probe must never invoke /stream", recordedUrls.none { it.contains("/stream") })
    }

    @Test
    fun probeTrackListTierReturnsNullOnTransientServerErrorSoCacheIsNotPoisoned() = runBlocking {
        val (api, _) = fakeApi { 500 to "" }

        val tier = api.probeTrackListTier(
            title = "Nights",
            artist = "Frank Ocean",
        )

        assertNull(tier)
    }

    @Test
    fun probeTrackListTierReturnsNoneWhenAddonSearchRespondsEmpty() = runBlocking {
        val (api, _) = fakeApi { 200 to """{"tracks":[]}""" }

        val tier = api.probeTrackListTier(
            title = "Rare Local Bootleg",
            artist = "Independent Artist",
        )

        assertEquals(LosslessListTier.NONE, tier?.tier)
    }

    @Test
    fun probeTrackListTierReturnsNullWhenNoAddonsConfigured() = runBlocking {
        val (api, recordedUrls) = fakeApi(addonUrls = emptyList())

        val tier = api.probeTrackListTier(title = "Nights", artist = "Frank Ocean")

        assertNull(tier)
        assertTrue(recordedUrls.isEmpty())
    }

    @Test
    fun resolveStreamCdOnlyTrackOnHiResSettingNeverFabricatesFake24BitHiRes() = runBlocking {
        val (api, _) = fakeApi { url ->
            when {
                url.contains("/search") -> 200 to """
                    {"tracks":[{"id":"101","title":"Nights","artist":"Frank Ocean","duration":307,"audioQuality":"LOSSLESS"}]}
                """.trimIndent()
                url.contains("/stream") -> 200 to """
                    {"url":"https://cdn.example/101.flac","format":"flac","codec":"flac","quality":"","sampleRate":44100}
                """.trimIndent()
                else -> 404 to "{}"
            }
        }

        val resolved = api.resolveStream(
            title = "Nights",
            artist = "Frank Ocean",
            expectedDurationSeconds = 307,
            preferredQuality = LosslessMusicApi.QUALITY_MAX_HI_RES,
        )

        requireNotNull(resolved)
        assertEquals(LosslessMusicApi.QUALITY_CD_LOSSLESS, resolved.formatId)
        assertTrue("Expected non-24-bit depth, was ${resolved.bitDepth}", resolved.bitDepth <= 16)
        val emitted = api.resolvedTrackTiers.first()
        assertEquals(LosslessListTier.LOSSLESS, emitted.tier)
        assertTrue(emitted.allowDowngrade)
    }

    @Test
    fun resolveStreamDowngradesMislabelledHiResSearchHitWhenStreamReturns16BitCd() = runBlocking {
        val (api, _) = fakeApi { url ->
            when {
                url.contains("/search") -> 200 to """
                    {"tracks":[{"id":"202","title":"Self Control","artist":"Frank Ocean","duration":249,"audioQuality":"HI_RES_LOSSLESS","bitDepth":24}]}
                """.trimIndent()
                url.contains("/stream") -> 200 to """
                    {"url":"https://cdn.example/202.flac","format":"flac","codec":"flac","quality":"LOSSLESS","bitDepth":16,"sampleRate":44100}
                """.trimIndent()
                else -> 404 to "{}"
            }
        }

        val resolved = api.resolveStream(
            title = "Self Control",
            artist = "Frank Ocean",
            expectedDurationSeconds = 249,
            preferredQuality = LosslessMusicApi.QUALITY_MAX_HI_RES,
        )

        requireNotNull(resolved)
        assertEquals(LosslessMusicApi.QUALITY_CD_LOSSLESS, resolved.formatId)
        assertEquals(16, resolved.bitDepth)
        val emitted = api.resolvedTrackTiers.first()
        assertEquals(LosslessListTier.LOSSLESS, emitted.tier)
        assertTrue(emitted.allowDowngrade)
    }

    @Test
    fun classifyHitsAndResolvedStreamHonorQualityFieldAnd96kHzSampleRate() = runBlocking {
        val (probeApi, _) = fakeApi()
        val hits = listOf(
            AddonTrack(id = "303", title = "Pink + White", artist = "Frank Ocean", quality = "HI_RES_LOSSLESS"),
            AddonTrack(id = "304", title = "Ivy", artist = "Frank Ocean", rawSampleRate = JsonPrimitive(96000)),
        )
        assertEquals(
            LosslessListTier.HI_RES,
            probeApi.classifyAddonHitsForListTier(listOf(hits[0]), "Pink + White", "Frank Ocean"),
        )
        assertEquals(
            LosslessListTier.HI_RES,
            probeApi.classifyAddonHitsForListTier(listOf(hits[1]), "Ivy", "Frank Ocean"),
        )

        val (api, _) = fakeApi { url ->
            when {
                url.contains("/search") -> 200 to """
                    {"tracks":[{"id":"404","title":"Ivy","artist":"Frank Ocean","duration":249,"audioQuality":"LOSSLESS"}]}
                """.trimIndent()
                url.contains("/stream") -> 200 to """
                    {"url":"https://cdn.example/404.flac","format":"flac","codec":"flac","quality":"LOSSLESS","sampleRate":96000}
                """.trimIndent()
                else -> 404 to "{}"
            }
        }
        val resolved = api.resolveStream(
            title = "Ivy",
            artist = "Frank Ocean",
            expectedDurationSeconds = 249,
            preferredQuality = LosslessMusicApi.QUALITY_MAX_HI_RES,
        )
        requireNotNull(resolved)
        assertEquals(96.0, resolved.samplingRate, 0.01)
        assertEquals(LosslessListTier.HI_RES, api.resolvedTrackTiers.first().tier)
    }

    @Test
    fun repositoryMergeTierAndDownloadBadgeClassificationWorkInBothDirections() {
        // Capped CD playback must not overwrite a verified HI_RES list badge.
        assertNull(
            LosslessAvailabilityRepository.mergeTierUpdate(
                existingTier = LosslessListTier.HI_RES,
                event = ResolvedTrackTierEvent("k", LosslessListTier.LOSSLESS, allowDowngrade = false),
            ),
        )
        // Uncapped Hi-Res resolution or measured playback must correct stale HI_RES -> LOSSLESS.
        assertEquals(
            LosslessListTier.LOSSLESS,
            LosslessAvailabilityRepository.mergeTierUpdate(
                existingTier = LosslessListTier.HI_RES,
                event = ResolvedTrackTierEvent("k", LosslessListTier.LOSSLESS, allowDowngrade = true),
            ),
        )
        // Upgrades and Atmos still propagate cleanly.
        assertEquals(
            LosslessListTier.HI_RES,
            LosslessAvailabilityRepository.mergeTierUpdate(
                existingTier = LosslessListTier.LOSSLESS,
                event = ResolvedTrackTierEvent("k", LosslessListTier.HI_RES, allowDowngrade = false),
            ),
        )

        assertEquals(LosslessListTier.HI_RES, LosslessAvailabilityRepository.classifyDownloadFormatBadge("96kHz FLAC"))
        assertEquals(LosslessListTier.HI_RES, LosslessAvailabilityRepository.classifyDownloadFormatBadge("24/44.1kHz"))
        assertEquals(LosslessListTier.HI_RES, LosslessAvailabilityRepository.classifyDownloadFormatBadge("DSD64"))
        assertEquals(LosslessListTier.ATMOS, LosslessAvailabilityRepository.classifyDownloadFormatBadge("DOLBY ATMOS"))
        assertEquals(LosslessListTier.LOSSLESS, LosslessAvailabilityRepository.classifyDownloadFormatBadge("16/44.1kHz"))
        assertEquals(LosslessListTier.LOSSLESS, LosslessAvailabilityRepository.classifyDownloadFormatBadge("FLAC"))
    }
}
