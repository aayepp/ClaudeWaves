package com.lastwave.app.data.update

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AppUpdateManagerTest {

    private val sampleAssets = listOf(
        ReleaseAsset(
            name = "LastWave-v4.2.4-android7.apk",
            downloadUrl = "https://github.com/Clash-Projects/LastWave-Native/releases/download/4.2.4/LastWave-v4.2.4-android7.apk",
        ),
        ReleaseAsset(
            name = "LastWave-v4.2.4-universal-uncompressed.apk",
            downloadUrl = "https://github.com/Clash-Projects/LastWave-Native/releases/download/4.2.4/LastWave-v4.2.4-universal-uncompressed.apk",
        ),
        ReleaseAsset(
            name = "LastWave-v4.2.4-universal.apk",
            downloadUrl = "https://github.com/Clash-Projects/LastWave-Native/releases/download/4.2.4/LastWave-v4.2.4-universal.apk",
        ),
        ReleaseAsset(
            name = "SHA256SUMS.txt",
            downloadUrl = "https://github.com/Clash-Projects/LastWave-Native/releases/download/4.2.4/SHA256SUMS.txt",
        ),
    )

    @Test
    fun classifyAsset_recognizesVariantsCorrectly() {
        assertThat(AppUpdateManager.classifyAsset("LastWave-v4.2.4-android7.apk"))
            .isEqualTo(ApkVariant.ANDROID7)
        assertThat(AppUpdateManager.classifyAsset("LastWave-v4.2.4-legacy.apk"))
            .isEqualTo(ApkVariant.ANDROID7)

        assertThat(AppUpdateManager.classifyAsset("LastWave-v4.2.4-universal-uncompressed.apk"))
            .isEqualTo(ApkVariant.UNCOMPRESSED)
        assertThat(AppUpdateManager.classifyAsset("LastWave-v4.2.4-raw.apk"))
            .isEqualTo(ApkVariant.UNCOMPRESSED)
        assertThat(AppUpdateManager.classifyAsset("LastWave-v4.2.4-beta-raw.apk"))
            .isEqualTo(ApkVariant.UNCOMPRESSED)

        assertThat(AppUpdateManager.classifyAsset("LastWave-v4.2.4-universal.apk"))
            .isEqualTo(ApkVariant.UNIVERSAL)
        assertThat(AppUpdateManager.classifyAsset("LastWave-v4.2.4-release.apk"))
            .isEqualTo(ApkVariant.UNIVERSAL)
        assertThat(AppUpdateManager.classifyAsset("LastWave-v4.2.4-beta.apk"))
            .isEqualTo(ApkVariant.UNIVERSAL)
        assertThat(AppUpdateManager.classifyAsset("ClaudeWaves-v5.2.1-universal.apk"))
            .isEqualTo(ApkVariant.UNIVERSAL)
        assertThat(AppUpdateManager.classifyAsset("ClaudeWaves-v5.2.1-android7.apk"))
            .isEqualTo(ApkVariant.ANDROID7)
    }

    @Test
    fun selectBestAsset_standardAndroid9Plus_selectsUniversalApk() {
        val selectedAndroid9 = AppUpdateManager.selectBestAsset(
            assets = sampleAssets,
            isUncompressed = false,
            sdkInt = 28, // Android 9 Pie
        )
        assertThat(selectedAndroid9).isNotNull()
        assertThat(selectedAndroid9?.name).isEqualTo("LastWave-v4.2.4-universal.apk")

        val selected = AppUpdateManager.selectBestAsset(
            assets = sampleAssets,
            isUncompressed = false,
            sdkInt = 34, // Android 14
        )
        assertThat(selected).isNotNull()
        assertThat(selected?.name).isEqualTo("LastWave-v4.2.4-universal.apk")
        assertThat(selected?.downloadUrl).contains("LastWave-v4.2.4-universal.apk")
    }

    @Test
    fun selectBestAsset_uncompressedBuild_selectsUncompressedApk() {
        val selected = AppUpdateManager.selectBestAsset(
            assets = sampleAssets,
            isUncompressed = true,
            sdkInt = 34, // Android 14
        )
        assertThat(selected).isNotNull()
        assertThat(selected?.name).isEqualTo("LastWave-v4.2.4-universal-uncompressed.apk")
        assertThat(selected?.downloadUrl).contains("LastWave-v4.2.4-universal-uncompressed.apk")
    }

    @Test
    fun selectBestAsset_legacyAndroid_selectsAndroid7Apk() {
        val selected = AppUpdateManager.selectBestAsset(
            assets = sampleAssets,
            isUncompressed = false,
            sdkInt = 26, // Android 8.0 Oreo
        )
        assertThat(selected).isNotNull()
        assertThat(selected?.name).isEqualTo("LastWave-v4.2.4-android7.apk")
        assertThat(selected?.downloadUrl).contains("LastWave-v4.2.4-android7.apk")
    }

    @Test
    fun selectBestAsset_ignoresNonApkAssets() {
        val nonApkOnly = listOf(
            ReleaseAsset(name = "SHA256SUMS.txt", downloadUrl = "https://example.com/sha"),
            ReleaseAsset(name = "notice.md", downloadUrl = "https://example.com/md"),
        )
        val selected = AppUpdateManager.selectBestAsset(
            assets = nonApkOnly,
            isUncompressed = false,
            sdkInt = 34,
        )
        assertThat(selected).isNull()
    }

    @Test
    fun selectBestAsset_fallbackWhenUniversalMissing_fallsBackToUncompressedOrAndroid7() {
        val assetsWithoutUniversal = listOf(
            ReleaseAsset(name = "LastWave-v4.2.4-android7.apk", downloadUrl = "https://example.com/a7"),
            ReleaseAsset(name = "LastWave-v4.2.4-universal-uncompressed.apk", downloadUrl = "https://example.com/raw"),
        )
        val selected = AppUpdateManager.selectBestAsset(
            assets = assetsWithoutUniversal,
            isUncompressed = false,
            sdkInt = 34,
        )
        assertThat(selected).isNotNull()
        assertThat(selected?.name).isEqualTo("LastWave-v4.2.4-universal-uncompressed.apk")
    }

    private val assetsWithVivo = sampleAssets + ReleaseAsset(
        name = "LastWave-v4.2.4-vivo-origin.apk",
        downloadUrl = "https://example.com/vivo",
    )

    @Test
    fun classifyAsset_recognizesVivoOrigin() {
        assertThat(AppUpdateManager.classifyAsset("LastWave-v4.2.4-vivo-origin.apk"))
            .isEqualTo(ApkVariant.VIVO_ORIGIN)
    }

    @Test
    fun selectBestAsset_vivoBuild_onlySelectsVivoApk() {
        val selected = AppUpdateManager.selectBestAsset(
            assets = assetsWithVivo,
            isUncompressed = false,
            sdkInt = 34,
            isVivoOrigin = true,
        )
        assertThat(selected?.name).isEqualTo("LastWave-v4.2.4-vivo-origin.apk")
    }

    @Test
    fun selectBestAsset_vivoBuild_withoutVivoApk_selectsNothing() {
        val selected = AppUpdateManager.selectBestAsset(
            assets = sampleAssets,
            isUncompressed = false,
            sdkInt = 34,
            isVivoOrigin = true,
        )
        assertThat(selected).isNull()
    }

    @Test
    fun selectBestAsset_normalBuild_neverSelectsVivoApk() {
        val onlyVivo = listOf(ReleaseAsset(name = "LastWave-v4.2.4-vivo-origin.apk", downloadUrl = "https://example.com/vivo"))
        assertThat(
            AppUpdateManager.selectBestAsset(assets = onlyVivo, isUncompressed = false, sdkInt = 34),
        ).isNull()
        assertThat(
            AppUpdateManager.selectBestAsset(assets = assetsWithVivo, isUncompressed = false, sdkInt = 34)?.name,
        ).isEqualTo("LastWave-v4.2.4-universal.apk")
    }

    @Test
    fun selectBestAsset_claudeWavesUniversalAndCanonicalFallback() {
        val canonicalOnly = listOf(
            ReleaseAsset(
                name = "ClaudeWaves.apk",
                downloadUrl = "https://github.com/aayepp/ClaudeWaves/releases/download/v5.2.3/ClaudeWaves.apk",
                sizeBytes = 24_000_000L,
            ),
        )
        assertThat(
            AppUpdateManager.selectBestAsset(assets = canonicalOnly, isUncompressed = false, sdkInt = 34)?.name,
        ).isEqualTo("ClaudeWaves.apk")

        val withUniversal = canonicalOnly + ReleaseAsset(
            name = "ClaudeWaves-v5.2.3-universal.apk",
            downloadUrl = "https://github.com/aayepp/ClaudeWaves/releases/download/v5.2.3/ClaudeWaves-v5.2.3-universal.apk",
            sizeBytes = 24_000_000L,
        )
        assertThat(
            AppUpdateManager.selectBestAsset(assets = withUniversal, isUncompressed = false, sdkInt = 34)?.name,
        ).isEqualTo("ClaudeWaves-v5.2.3-universal.apk")
    }

    @Test
    fun updateInfo_initialDownloadDefaultsAreIdle() {
        val info = UpdateInfo()
        assertThat(info.isDownloading).isFalse()
        assertThat(info.downloadProgress).isNull()
        assertThat(info.downloadedBytes).isEqualTo(0L)
        assertThat(info.totalBytes).isEqualTo(0L)
        assertThat(info.downloadedApkPath).isNull()
        assertThat(info.downloadError).isNull()
    }
}
