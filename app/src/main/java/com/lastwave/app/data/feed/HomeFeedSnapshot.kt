package com.lastwave.app.data.feed

import android.content.Context
import com.lastwave.app.data.music.CountryChart
import com.lastwave.app.data.music.YouTubeMusicTrack
import com.lastwave.app.data.music.YouTubePlaylistSummary
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The parts of the last feed that Home draws from, kept on disk. The feed
 * lived only in memory, so every cold start sat on a skeleton for the whole
 * network round (about 3.5 s on a phone); now Home opens on the last feed
 * and refreshes under it.
 */
@Serializable
private data class Snapshot(
    val key: String,
    val isYtConnected: Boolean,
    val ytRecentSongs: List<YouTubeMusicTrack>,
    val quickPicks: List<YouTubeMusicTrack>,
    val ytLikedSongs: List<YouTubeMusicTrack>,
    val freshFinds: List<YouTubeMusicTrack>,
    val charts: List<YouTubeMusicTrack>,
    val mixes: List<FeedMix>,
    val countryCharts: List<CountryChart>,
    val newReleases: List<YouTubePlaylistSummary>,
    val ytSuggestedPlaylists: List<YouTubePlaylistSummary>,
    val lastUpdatedMillis: Long,
)

@Singleton
class HomeFeedSnapshotStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val file get() = File(context.filesDir, "home_feed.json")
    private val json = Json { ignoreUnknownKeys = true }

    /** The saved feed when it was made for [key] (same account and settings); null otherwise. */
    suspend fun load(key: String): FeedData? = withContext(Dispatchers.IO) {
        val snapshot = try {
            json.decodeFromString<Snapshot>(file.readText())
        } catch (_: Exception) {
            return@withContext null // missing, or written by an older layout
        }
        if (snapshot.key != key) return@withContext null
        FeedData(
            isYtConnected = snapshot.isYtConnected,
            ytRecentSongs = snapshot.ytRecentSongs,
            quickPicks = snapshot.quickPicks,
            ytLikedSongs = snapshot.ytLikedSongs,
            freshFinds = snapshot.freshFinds,
            charts = snapshot.charts,
            mixes = snapshot.mixes,
            countryCharts = snapshot.countryCharts,
            newReleases = snapshot.newReleases,
            ytSuggestedPlaylists = snapshot.ytSuggestedPlaylists,
            lastUpdatedMillis = snapshot.lastUpdatedMillis,
        )
    }

    suspend fun save(key: String, feed: FeedData) = withContext(Dispatchers.IO) {
        val snapshot = Snapshot(
            key = key,
            isYtConnected = feed.isYtConnected,
            ytRecentSongs = feed.ytRecentSongs,
            quickPicks = feed.quickPicks,
            ytLikedSongs = feed.ytLikedSongs,
            freshFinds = feed.freshFinds,
            charts = feed.charts,
            mixes = feed.mixes,
            countryCharts = feed.countryCharts,
            newReleases = feed.newReleases,
            ytSuggestedPlaylists = feed.ytSuggestedPlaylists,
            lastUpdatedMillis = feed.lastUpdatedMillis,
        )
        // Write then rename, so a kill mid-write leaves the previous snapshot whole.
        val tmp = File(file.path + ".tmp")
        try {
            tmp.writeText(json.encodeToString(Snapshot.serializer(), snapshot))
            tmp.renameTo(file)
        } catch (e: Exception) {
            android.util.Log.w("HomeFeedSnapshot", "Couldn't save the home feed", e)
        }
    }
}
