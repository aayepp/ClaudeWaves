package com.lastwave.app.playback.karaoke

import androidx.compose.runtime.Immutable

@Immutable
sealed interface KaraokeStatus {
    data object Off : KaraokeStatus
    data object InstantStft : KaraokeStatus
    data class DownloadingModel(val progressPct: Int, val downloadedMb: Int, val totalMb: Int) : KaraokeStatus
    data object PreparingAi : KaraokeStatus
    data object AiActive : KaraokeStatus
    data class AiError(val message: String) : KaraokeStatus
}
