package com.zuvy.app.discover

import com.google.gson.annotations.SerializedName

/**
 * Data models for online content discovery
 */

// ============ YOUTUBE/TRENDING ============
data class YouTubeVideo(
    val id: String,
    val title: String,
    val description: String?,
    val thumbnail: String?,
    val channelTitle: String?,
    val channelId: String?,
    val publishedAt: String?,
    val duration: String?,
    val viewCount: Long?,
    val likeCount: Long?
) {
    val formattedViews: String
        get() = viewCount?.let { formatCount(it) } ?: "0"
    
    val formattedDuration: String
        get() = duration?.let { parseDuration(it) } ?: ""
    
    private fun formatCount(count: Long): String {
        return when {
            count >= 1_000_000 -> String.format("%.1fM", count / 1_000_000.0)
            count >= 1_000 -> String.format("%.1fK", count / 1_000.0)
            else -> count.toString()
        }
    }
    
    private fun parseDuration(isoDuration: String): String {
        // Parse ISO 8601 duration (PT1H2M3S)
        val regex = """PT(?:(\d+)H)?(?:(\d+)M)?(?:(\d+)S)?""".toRegex()
        val match = regex.find(isoDuration) ?: return ""
        val hours = match.groupValues[1].toIntOrNull() ?: 0
        val minutes = match.groupValues[2].toIntOrNull() ?: 0
        val seconds = match.groupValues[3].toIntOrNull() ?: 0
        
        return when {
            hours > 0 -> String.format("%d:%02d:%02d", hours, minutes, seconds)
            else -> String.format("%d:%02d", minutes, seconds)
        }
    }
}

data class YouTubeResponse(
    @SerializedName("items") val items: List<YouTubeItem>,
    @SerializedName("nextPageToken") val nextPageToken: String?,
    @SerializedName("pageInfo") val pageInfo: PageInfo?
)

data class YouTubeItem(
    @SerializedName("id") val id: String,
    @SerializedName("snippet") val snippet: VideoSnippet?,
    @SerializedName("statistics") val statistics: VideoStatistics?,
    @SerializedName("contentDetails") val contentDetails: ContentDetails?
)

data class VideoSnippet(
    @SerializedName("title") val title: String?,
    @SerializedName("description") val description: String?,
    @SerializedName("thumbnails") val thumbnails: Thumbnails?,
    @SerializedName("channelTitle") val channelTitle: String?,
    @SerializedName("channelId") val channelId: String?,
    @SerializedName("publishedAt") val publishedAt: String?
)

data class Thumbnails(
    @SerializedName("default") val default: Thumbnail?,
    @SerializedName("medium") val medium: Thumbnail?,
    @SerializedName("high") val high: Thumbnail?,
    @SerializedName("maxres") val maxres: Thumbnail?
) {
    val best: String?
        get() = maxres?.url ?: high?.url ?: medium?.url ?: default?.url
}

data class Thumbnail(
    @SerializedName("url") val url: String?,
    @SerializedName("width") val width: Int?,
    @SerializedName("height") val height: Int?
)

data class VideoStatistics(
    @SerializedName("viewCount") val viewCount: Long?,
    @SerializedName("likeCount") val likeCount: Long?,
    @SerializedName("commentCount") val commentCount: Long?
)

data class ContentDetails(
    @SerializedName("duration") val duration: String?
)

data class PageInfo(
    @SerializedName("totalResults") val totalResults: Int?,
    @SerializedName("resultsPerPage") val resultsPerPage: Int?
)

// ============ RADIO ============
data class RadioStation(
    @SerializedName("stationuuid") val stationUuid: String,
    @SerializedName("name") val name: String,
    @SerializedName("url") val url: String,
    @SerializedName("url_resolved") val urlResolved: String?,
    @SerializedName("homepage") val homepage: String?,
    @SerializedName("favicon") val favicon: String?,
    @SerializedName("tags") val tags: String?,
    @SerializedName("country") val country: String?,
    @SerializedName("countrycode") val countryCode: String?,
    @SerializedName("language") val language: String?,
    @SerializedName("votes") val votes: Int?,
    @SerializedName("clickcount") val clickCount: Long?,
    @SerializedName("bitrate") val bitrate: Int?,
    @SerializedName("codec") val codec: String?,
    @SerializedName("hls") val hls: Int?
) {
    val formattedListeners: String
        get() = clickCount?.let { "${formatCount(it)} plays" } ?: ""
    
    val formattedBitrate: String
        get() = bitrate?.let { "${it}kbps" } ?: ""
    
    private fun formatCount(count: Long): String {
        return when {
            count >= 1_000_000 -> String.format("%.1fM", count / 1_000_000.0)
            count >= 1_000 -> String.format("%.1fK", count / 1_000.0)
            else -> count.toString()
        }
    }
}

data class RadioTag(
    @SerializedName("name") val name: String,
    @SerializedName("stationcount") val stationCount: Int?
)

// ============ PODCAST ============
data class Podcast(
    @SerializedName("collectionId") val id: Long,
    @SerializedName("collectionName") val name: String,
    @SerializedName("artistName") val artistName: String?,
    @SerializedName("artworkUrl100") val artworkUrl: String?,
    @SerializedName("artworkUrl600") val artworkUrlLarge: String?,
    @SerializedName("primaryGenreName") val genre: String?,
    @SerializedName("trackCount") val trackCount: Int?,
    @SerializedName("feedUrl") val feedUrl: String?,
    @SerializedName("releaseDate") val releaseDate: String?,
    @SerializedName("contentAdvisoryRating") val contentAdvisory: String?
)

data class PodcastFeed(
    val title: String,
    val description: String?,
    val author: String?,
    val imageUrl: String?,
    val episodes: List<PodcastEpisode>,
    val lastUpdated: String?
)

data class PodcastEpisode(
    val guid: String,
    val title: String,
    val description: String?,
    val audioUrl: String?,
    val duration: String?,
    val publishedAt: String?,
    val imageUrl: String?,
    val chapterUrl: String?
) {
    val formattedDuration: String
        get() = duration?.let { parseDuration(it) } ?: ""
    
    private fun parseDuration(duration: String): String {
        val totalSeconds = duration.toIntOrNull() ?: return duration
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        
        return when {
            hours > 0 -> String.format("%d:%02d:%02d", hours, minutes, seconds)
            else -> String.format("%d:%02d", minutes, seconds)
        }
    }
}

data class ITunesSearchResponse(
    @SerializedName("resultCount") val resultCount: Int,
    @SerializedName("results") val results: List<Podcast>
)

// ============ FREE MUSIC ============
data class MusicTrack(
    val id: String,
    val name: String,
    val artistName: String?,
    val albumName: String?,
    val duration: Int?,
    val audioUrl: String?,
    val imageUrl: String?,
    val genre: String?,
    val license: String?,
    val playCount: Long?,
    val downloadUrl: String?
) {
    val formattedDuration: String
        get() = duration?.let { 
            val minutes = it / 60
            val seconds = it % 60
            String.format("%d:%02d", minutes, seconds)
        } ?: ""
    
    val formattedPlays: String
        get() = playCount?.let { "${formatCount(it)} plays" } ?: ""
    
    private fun formatCount(count: Long): String {
        return when {
            count >= 1_000_000 -> String.format("%.1fM", count / 1_000_000.0)
            count >= 1_000 -> String.format("%.1fK", count / 1_000.0)
            else -> count.toString()
        }
    }
}

data class JamendoTrack(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("artist_name") val artistName: String?,
    @SerializedName("album_name") val albumName: String?,
    @SerializedName("duration") val duration: Int?,
    @SerializedName("audiodownload") val audioDownload: String?,
    @SerializedName("audio") val audioStream: String?,
    @SerializedName("image") val imageUrl: String?,
    @SerializedName("musicinfo") val musicInfo: MusicInfo?
)

data class MusicInfo(
    @SerializedName("tags") val tags: Tags?
)

data class Tags(
    @SerializedName("genres") val genres: List<String>?
)

data class JamendoResponse(
    @SerializedName("headers") val headers: JamendoHeaders?,
    @SerializedName("results") val results: List<JamendoTrack>
)

data class JamendoHeaders(
    @SerializedName("status") val status: String?,
    @SerializedName("code") val code: Int?,
    @SerializedName("error_message") val errorMessage: String?,
    @SerializedName("results_count") val resultsCount: Int?
)

// ============ CONTENT CATEGORY ============
enum class ContentCategory(val displayName: String) {
    TRENDING("Trending"),
    MUSIC("Music"),
    GAMING("Gaming"),
    NEWS("News"),
    SPORTS("Sports"),
    TECH("Technology"),
    ENTERTAINMENT("Entertainment")
}

enum class DiscoverSection {
    TRENDING_VIDEOS,
    ONLINE_RADIO,
    PODCASTS,
    FREE_MUSIC,
    RECOMMENDED,
    NEW_RELEASES
}

// ============ UNIFIED CONTENT ITEM ============
sealed class DiscoverItem {
    abstract val id: String
    
    data class VideoItem(val video: YouTubeVideo) : DiscoverItem() {
        override val id: String = video.id
    }
    
    data class RadioItem(val station: RadioStation) : DiscoverItem() {
        override val id: String = station.stationUuid
    }
    
    data class PodcastItem(val podcast: Podcast) : DiscoverItem() {
        override val id: String = podcast.id.toString()
    }
    
    data class MusicItem(val track: MusicTrack) : DiscoverItem() {
        override val id: String = track.id
    }
}
