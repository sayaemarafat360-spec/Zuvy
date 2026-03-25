package com.zuvy.app.discover

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zuvy.app.network.NetworkManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DiscoverViewModel @Inject constructor(
    private val onlineContentManager: OnlineContentManager,
    private val networkManager: NetworkManager
) : ViewModel() {
    
    sealed class State {
        object Loading : State()
        data class Loaded(
            val radioStations: List<RadioStation>,
            val podcasts: List<Podcast>,
            val musicTracks: List<MusicTrack>
        ) : State()
        object Offline : State()
        data class Error(val message: String) : State()
    }
    
    private val _state = MutableStateFlow<State>(State.Loading)
    val state: StateFlow<State> = _state.asStateFlow()
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    private val _selectedCategory = MutableStateFlow("all")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()
    
    init {
        loadContent()
    }
    
    fun loadContent() {
        viewModelScope.launch {
            if (!networkManager.isOnline()) {
                _state.value = State.Offline
                return@launch
            }
            
            _state.value = State.Loading
            
            try {
                // Load all content in parallel
                val radioStations = onlineContentManager.getTopRadioStations(20)
                val podcasts = onlineContentManager.getTopPodcasts(limit = 20)
                val musicTracks = onlineContentManager.getFreeMusicTracks(20)
                
                _state.value = State.Loaded(radioStations, podcasts, musicTracks)
            } catch (e: Exception) {
                _state.value = State.Error(e.message ?: "Failed to load content")
            }
        }
    }
    
    fun search(query: String) {
        _searchQuery.value = query
        
        if (query.isBlank()) {
            loadContent()
            return
        }
        
        viewModelScope.launch {
            if (!networkManager.isOnline()) {
                _state.value = State.Offline
                return@launch
            }
            
            _state.value = State.Loading
            
            try {
                val radioStations = onlineContentManager.searchRadioStations(query)
                val podcasts = onlineContentManager.searchPodcasts(query)
                val musicTracks = onlineContentManager.searchFreeMusic(query)
                
                _state.value = State.Loaded(radioStations, podcasts, musicTracks)
            } catch (e: Exception) {
                _state.value = State.Error(e.message ?: "Search failed")
            }
        }
    }
    
    fun setCategory(category: String) {
        _selectedCategory.value = category
        
        viewModelScope.launch {
            if (!networkManager.isOnline()) {
                _state.value = State.Offline
                return@launch
            }
            
            _state.value = State.Loading
            
            try {
                val radioStations = if (category == "all") {
                    onlineContentManager.getTopRadioStations(20)
                } else {
                    onlineContentManager.getRadioStationsByTag(category)
                }
                
                val musicTracks = if (category == "all") {
                    onlineContentManager.getFreeMusicTracks(20)
                } else {
                    onlineContentManager.getFreeMusicByGenre(category)
                }
                
                val podcasts = onlineContentManager.getTopPodcasts(limit = 20)
                
                _state.value = State.Loaded(radioStations, podcasts, musicTracks)
            } catch (e: Exception) {
                _state.value = State.Error(e.message ?: "Failed to filter content")
            }
        }
    }
    
    fun retry() {
        loadContent()
    }
}
