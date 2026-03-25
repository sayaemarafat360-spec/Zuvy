package com.zuvy.app.search

import android.content.Context
import com.zuvy.app.data.model.MediaItem
import com.zuvy.app.data.model.Song
import com.zuvy.app.data.repository.MediaRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SearchEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val mediaRepository: MediaRepository
) {
    private val _searchResults = MutableStateFlow<SearchResults>(SearchResults.Empty)
    val searchResults: StateFlow<SearchResults> = _searchResults.asStateFlow()
    
    private val _searchHistory = MutableStateFlow<List<String>>(emptyList())
    val searchHistory: StateFlow<List<String>> = _searchHistory.asStateFlow()
    
    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()
    
    private val _suggestions = MutableStateFlow<List<String>>(emptyList())
    val suggestions: StateFlow<List<String>> = _suggestions.asStateFlow()
    
    private val historyMaxSize = 20
    
    suspend fun search(query: String, filters: SearchFilters = SearchFilters()) {
        if (query.isBlank()) {
            _searchResults.value = SearchResults.Empty
            return
        }
        
        _isSearching.value = true
        
        try {
            val results = performSearch(query.trim(), filters)
            _searchResults.value = results
            addToHistory(query.trim())
            updateSuggestions(query.trim())
        } catch (e: Exception) {
            _searchResults.value = SearchResults.Error(e.message ?: "Search failed")
        } finally {
            _isSearching.value = false
        }
    }
    
    private suspend fun performSearch(query: String, filters: SearchFilters): SearchResults {
        val lowerQuery = query.lowercase()
        
        // Get all media
        val allVideos = mediaRepository.videos
        val allMusic = mediaRepository.music
        
        // Filter and search videos
        val matchedVideos = if (filters.includeVideos) {
            allVideos.filter { video ->
                val matchesQuery = video.name.lowercase().contains(lowerQuery) ||
                    video.path.lowercase().contains(lowerQuery) ||
                    video.mimeType?.lowercase()?.contains(lowerQuery) == true
                
                val matchesFilters = checkVideoFilters(video, filters)
                
                matchesQuery && matchesFilters
            }.let { results ->
                when (filters.sortBy) {
                    SearchSort.RELEVANCE -> results.sortedByDescending { 
                        it.name.lowercase().indexOf(lowerQuery).let { if (it == -1) Int.MAX_VALUE else it }
                    }
                    SearchSort.NAME -> results.sortedBy { it.name.lowercase() }
                    SearchSort.DATE -> results.sortedByDescending { it.dateAdded }
                    SearchSort.SIZE -> results.sortedByDescending { it.size }
                    SearchSort.DURATION -> results.sortedByDescending { it.duration }
                }
            }
        } else emptyList()
        
        // Filter and search music
        val matchedMusic = if (filters.includeMusic) {
            allMusic.filter { song ->
                val matchesQuery = song.title.lowercase().contains(lowerQuery) ||
                    song.artist.lowercase().contains(lowerQuery) ||
                    song.album?.lowercase()?.contains(lowerQuery) == true ||
                    song.genre?.lowercase()?.contains(lowerQuery) == true
                
                val matchesFilters = checkMusicFilters(song, filters)
                
                matchesQuery && matchesFilters
            }.let { results ->
                when (filters.sortBy) {
                    SearchSort.RELEVANCE -> results.sortedByDescending { 
                        song.title.lowercase().indexOf(lowerQuery).let { if (it == -1) Int.MAX_VALUE else it }
                    }
                    SearchSort.NAME -> results.sortedBy { it.title.lowercase() }
                    SearchSort.DATE -> results.sortedByDescending { it.dateAdded }
                    SearchSort.DURATION -> results.sortedByDescending { 
                        parseDurationToMs(song.duration) 
                    }
                    else -> results
                }
            }
        } else emptyList()
        
        return SearchResults.Success(
            query = query,
            videos = matchedVideos,
            music = matchedMusic,
            totalResults = matchedVideos.size + matchedMusic.size
        )
    }
    
    private fun checkVideoFilters(video: MediaItem, filters: SearchFilters): Boolean {
        // Duration filter
        val durationSeconds = video.duration / 1000
        if (filters.minDuration > 0 && durationSeconds < filters.minDuration) return false
        if (filters.maxDuration < Int.MAX_VALUE && durationSeconds > filters.maxDuration) return false
        
        // Size filter
        val sizeMB = video.size / (1024 * 1024)
        if (filters.minSize > 0 && sizeMB < filters.minSize) return false
        if (filters.maxSize < Int.MAX_VALUE && sizeMB > filters.maxSize) return false
        
        // Resolution filter
        if (filters.minResolution > 0) {
            val minDim = minOf(video.width, video.height)
            if (minDim < filters.minResolution) return false
        }
        
        return true
    }
    
    private fun checkMusicFilters(song: Song, filters: SearchFilters): Boolean {
        // Artist filter
        if (filters.artist != null && !song.artist.contains(filters.artist, ignoreCase = true)) return false
        
        // Album filter
        if (filters.album != null && song.album?.contains(filters.album, ignoreCase = true) != true) return false
        
        // Genre filter
        if (filters.genre != null && song.genre?.contains(filters.genre, ignoreCase = true) != true) return false
        
        // Year filter
        if (filters.year > 0 && song.year != filters.year) return false
        
        return true
    }
    
    private fun parseDurationToMs(duration: String): Long {
        val parts = duration.split(":").map { it.toIntOrNull() ?: 0 }
        return when (parts.size) {
            2 -> (parts[0] * 60 + parts[1]) * 1000L
            3 -> (parts[0] * 3600 + parts[1] * 60 + parts[2]) * 1000L
            else -> 0L
        }
    }
    
    private fun addToHistory(query: String) {
        val currentHistory = _searchHistory.value.toMutableList()
        currentHistory.remove(query)
        currentHistory.add(0, query)
        if (currentHistory.size > historyMaxSize) {
            currentHistory.removeAt(currentHistory.size - 1)
        }
        _searchHistory.value = currentHistory
    }
    
    private fun updateSuggestions(query: String) {
        val allTitles = mutableListOf<String>()
        allTitles.addAll(mediaRepository.videos.map { it.name })
        allTitles.addAll(mediaRepository.music.map { it.title })
        allTitles.addAll(mediaRepository.music.map { it.artist }.distinct())
        
        val newSuggestions = allTitles
            .filter { it.lowercase().contains(query.lowercase()) && it != query }
            .distinct()
            .take(10)
        
        _suggestions.value = newSuggestions
    }
    
    fun clearHistory() {
        _searchHistory.value = emptyList()
    }
    
    fun removeFromHistory(query: String) {
        _searchHistory.value = _searchHistory.value.filter { it != query }
    }
    
    fun getRecentSearches(): List<String> {
        return _searchHistory.value.take(10)
    }
}

data class SearchFilters(
    val includeVideos: Boolean = true,
    val includeMusic: Boolean = true,
    val minDuration: Int = 0,
    val maxDuration: Int = Int.MAX_VALUE,
    val minSize: Int = 0,
    val maxSize: Int = Int.MAX_VALUE,
    val minResolution: Int = 0,
    val artist: String? = null,
    val album: String? = null,
    val genre: String? = null,
    val year: Int = 0,
    val sortBy: SearchSort = SearchSort.RELEVANCE
)

enum class SearchSort {
    RELEVANCE, NAME, DATE, SIZE, DURATION
}

sealed class SearchResults {
    object Empty : SearchResults()
    data class Success(
        val query: String,
        val videos: List<MediaItem>,
        val music: List<Song>,
        val totalResults: Int
    ) : SearchResults()
    data class Error(val message: String) : SearchResults()
}
