package com.zuvy.app.discover

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * Online Content Manager - Handles all internet content discovery APIs
 */
class OnlineContentManager(private val context: Context) {
    
    companion object {
        const val TAG = "OnlineContentManager"
        
        // API Endpoints
        const val RADIO_BROWSER_API = "https://de1.api.radio-browser.info/json"
        const val ITUNES_API = "https://itunes.apple.com/search"
        const val JAMENDO_API = "https://api.jamendo.com/v3.0"
        
        // API Keys (should be stored securely in production)
        const val JAMENDO_CLIENT_ID = "your_client_id" // Free at jamendo.com
    }
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()
    
    private val gson = Gson()
    
    // ============ RADIO BROWSER API ============
    
    /**
     * Get top radio stations
     */
    suspend fun getTopRadioStations(limit: Int = 20): List<RadioStation> = withContext(Dispatchers.IO) {
        try {
            val url = "$RADIO_BROWSER_API/stations/topvote/$limit"
            val request = Request.Builder().url(url).build()
            
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: return@withContext emptyList()
                    gson.fromJson(body, Array<RadioStation>::class.java).toList()
                } else {
                    emptyList()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get top radio stations", e)
            emptyList()
        }
    }
    
    /**
     * Search radio stations by name
     */
    suspend fun searchRadioStations(query: String, limit: Int = 20): List<RadioStation> = withContext(Dispatchers.IO) {
        try {
            val url = "$RADIO_BROWSER_API/stations/byname/${java.net.URLEncoder.encode(query, "UTF-8")}?limit=$limit"
            val request = Request.Builder().url(url).build()
            
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: return@withContext emptyList()
                    gson.fromJson(body, Array<RadioStation>::class.java).toList()
                } else {
                    emptyList()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to search radio stations", e)
            emptyList()
        }
    }
    
    /**
     * Get radio stations by country
     */
    suspend fun getRadioStationsByCountry(countryCode: String, limit: Int = 20): List<RadioStation> = withContext(Dispatchers.IO) {
        try {
            val url = "$RADIO_BROWSER_API/stations/bycountrycodeexact/$countryCode?limit=$limit&order=clickcount&reverse=true"
            val request = Request.Builder().url(url).build()
            
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: return@withContext emptyList()
                    gson.fromJson(body, Array<RadioStation>::class.java).toList()
                } else {
                    emptyList()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get radio stations by country", e)
            emptyList()
        }
    }
    
    /**
     * Get radio stations by tag/genre
     */
    suspend fun getRadioStationsByTag(tag: String, limit: Int = 20): List<RadioStation> = withContext(Dispatchers.IO) {
        try {
            val url = "$RADIO_BROWSER_API/stations/bytag/${java.net.URLEncoder.encode(tag, "UTF-8")}?limit=$limit&order=clickcount&reverse=true"
            val request = Request.Builder().url(url).build()
            
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: return@withContext emptyList()
                    gson.fromJson(body, Array<RadioStation>::class.java).toList()
                } else {
                    emptyList()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get radio stations by tag", e)
            emptyList()
        }
    }
    
    /**
     * Get popular radio tags/genres
     */
    suspend fun getPopularRadioTags(limit: Int = 50): List<RadioTag> = withContext(Dispatchers.IO) {
        try {
            val url = "$RADIO_BROWSER_API/tags?limit=$limit&order=stationcount&reverse=true"
            val request = Request.Builder().url(url).build()
            
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: return@withContext emptyList()
                    gson.fromJson(body, Array<RadioTag>::class.java).toList()
                } else {
                    emptyList()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get radio tags", e)
            emptyList()
        }
    }
    
    // ============ ITUNES PODCAST API ============
    
    /**
     * Search podcasts
     */
    suspend fun searchPodcasts(query: String, limit: Int = 20): List<Podcast> = withContext(Dispatchers.IO) {
        try {
            val url = "$ITUNES_API?term=${java.net.URLEncoder.encode(query, "UTF-8")}&media=podcast&limit=$limit"
            val request = Request.Builder().url(url).build()
            
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: return@withContext emptyList()
                    val searchResponse = gson.fromJson(body, ITunesSearchResponse::class.java)
                    searchResponse.results
                } else {
                    emptyList()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to search podcasts", e)
            emptyList()
        }
    }
    
    /**
     * Get top podcasts by genre
     */
    suspend fun getTopPodcasts(genreId: Int? = null, limit: Int = 20): List<Podcast> = withContext(Dispatchers.IO) {
        try {
            val genreParam = genreId?.let { "&genreId=$it" } ?: ""
            val url = "$ITUNES_API?term=podcast&media=podcast&limit=$limit$genreParam"
            val request = Request.Builder().url(url).build()
            
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: return@withContext emptyList()
                    val searchResponse = gson.fromJson(body, ITunesSearchResponse::class.java)
                    searchResponse.results
                } else {
                    emptyList()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get top podcasts", e)
            emptyList()
        }
    }
    
    /**
     * Get podcast RSS feed
     */
    suspend fun getPodcastFeed(feedUrl: String): PodcastFeed? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(feedUrl).build()
            
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: return@withContext null
                    parseRssFeed(body)
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get podcast feed", e)
            null
        }
    }
    
    // ============ JAMENDO FREE MUSIC API ============
    
    /**
     * Get free music tracks
     */
    suspend fun getFreeMusicTracks(limit: Int = 20, offset: Int = 0): List<MusicTrack> = withContext(Dispatchers.IO) {
        try {
            val url = "$JAMENDO_API/tracks/?client_id=$JAMENDO_CLIENT_ID&format=json&limit=$limit&offset=$offset&order=popularity_total"
            val request = Request.Builder().url(url).build()
            
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: return@withContext emptyList()
                    val jamendoResponse = gson.fromJson(body, JamendoResponse::class.java)
                    jamendoResponse.results.map { it.toMusicTrack() }
                } else {
                    emptyList()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get free music", e)
            emptyList()
        }
    }
    
    /**
     * Search free music
     */
    suspend fun searchFreeMusic(query: String, limit: Int = 20): List<MusicTrack> = withContext(Dispatchers.IO) {
        try {
            val url = "$JAMENDO_API/tracks/?client_id=$JAMENDO_CLIENT_ID&format=json&limit=$limit&search=${java.net.URLEncoder.encode(query, "UTF-8")}"
            val request = Request.Builder().url(url).build()
            
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: return@withContext emptyList()
                    val jamendoResponse = gson.fromJson(body, JamendoResponse::class.java)
                    jamendoResponse.results.map { it.toMusicTrack() }
                } else {
                    emptyList()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to search free music", e)
            emptyList()
        }
    }
    
    /**
     * Get free music by genre
     */
    suspend fun getFreeMusicByGenre(genre: String, limit: Int = 20): List<MusicTrack> = withContext(Dispatchers.IO) {
        try {
            val url = "$JAMENDO_API/tracks/?client_id=$JAMENDO_CLIENT_ID&format=json&limit=$limit&tags=${java.net.URLEncoder.encode(genre, "UTF-8")}"
            val request = Request.Builder().url(url).build()
            
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: return@withContext emptyList()
                    val jamendoResponse = gson.fromJson(body, JamendoResponse::class.java)
                    jamendoResponse.results.map { it.toMusicTrack() }
                } else {
                    emptyList()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get free music by genre", e)
            emptyList()
        }
    }
    
    // ============ HELPER FUNCTIONS ============
    
    private fun parseRssFeed(xml: String): PodcastFeed {
        // Simple RSS parser for podcast feeds
        val title = extractXmlTag(xml, "title") ?: "Unknown"
        val description = extractXmlTag(xml, "description")
        val author = extractXmlTag(xml, "itunes:author")
        val imageUrl = extractXmlTag(xml, "itunes:image")?.let { 
            extractAttribute(it, "href") 
        } ?: extractXmlTag(xml, "image")?.let { 
            extractXmlTag(it, "url") 
        }
        
        val episodes = extractEpisodes(xml)
        
        return PodcastFeed(
            title = title,
            description = description,
            author = author,
            imageUrl = imageUrl,
            episodes = episodes,
            lastUpdated = null
        )
    }
    
    private fun extractXmlTag(xml: String, tag: String): String? {
        val pattern = "<$tag[^>]*>(.*?)</$tag>".toRegex(RegexOption.DOT_MATCHES_ALL)
        return pattern.find(xml)?.groupValues?.get(1)?.trim()
    }
    
    private fun extractAttribute(xml: String, attr: String): String? {
        val pattern = """$attr="([^"]*)"""".toRegex()
        return pattern.find(xml)?.groupValues?.get(1)
    }
    
    private fun extractEpisodes(xml: String): List<PodcastEpisode> {
        val episodes = mutableListOf<PodcastEpisode>()
        val itemPattern = "<item>(.*?)</item>".toRegex(RegexOption.DOT_MATCHES_ALL)
        
        itemPattern.findAll(xml).forEach { match ->
            val itemXml = match.groupValues[1]
            
            episodes.add(PodcastEpisode(
                guid = extractXmlTag(itemXml, "guid") ?: java.util.UUID.randomUUID().toString(),
                title = extractXmlTag(itemXml, "title") ?: "Unknown",
                description = extractXmlTag(itemXml, "description"),
                audioUrl = extractXmlTag(itemXml, "enclosure")?.let { 
                    extractAttribute(it, "url") 
                },
                duration = extractXmlTag(itemXml, "itunes:duration"),
                publishedAt = extractXmlTag(itemXml, "pubDate"),
                imageUrl = extractXmlTag(itemXml, "itunes:image")?.let { 
                    extractAttribute(it, "href") 
                },
                chapterUrl = null
            ))
        }
        
        return episodes
    }
    
    /**
     * Search radio stations (alias for searchRadioStations)
     */
    suspend fun searchRadio(query: String, limit: Int = 20): List<RadioStation> = searchRadioStations(query, limit)
    
    /**
     * Search podcasts (alias for searchPodcasts)
     */
    suspend fun searchPodcasts(query: String): List<Podcast> = searchPodcasts(query, 20)
    
    /**
     * Search free music (alias for searchFreeMusic)
     */
    suspend fun searchFreeMusic(query: String): List<MusicTrack> = searchFreeMusic(query, 20)
    
    // Extension function to convert JamendoTrack to MusicTrack
    private fun JamendoTrack.toMusicTrack(): MusicTrack {
        return MusicTrack(
            id = this.id,
            name = this.name,
            artistName = this.artistName,
            albumName = this.albumName,
            duration = this.duration,
            audioUrl = this.audioStream ?: this.audioDownload,
            imageUrl = this.imageUrl,
            genre = this.musicInfo?.tags?.genres?.firstOrNull(),
            license = "CC",
            playCount = null,
            downloadUrl = this.audioDownload
        )
    }
}
