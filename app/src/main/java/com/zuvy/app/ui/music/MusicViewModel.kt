package com.zuvy.app.ui.music

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zuvy.app.data.model.Song
import com.zuvy.app.data.repository.MediaRepository
import com.zuvy.app.data.repository.SortBy
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MusicViewModel @Inject constructor(
    private val mediaRepository: MediaRepository
) : ViewModel() {

    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs: StateFlow<List<Song>> = _songs.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadMusic()
    }

    fun loadMusic() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                mediaRepository.loadAllMedia()
                _songs.value = mediaRepository.music
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun searchSongs(query: String) {
        viewModelScope.launch {
            if (query.isEmpty()) {
                _songs.value = mediaRepository.music
            } else {
                _songs.value = mediaRepository.searchMusic(query)
            }
        }
    }

    fun sortSongs(sortBy: SortBy, ascending: Boolean) {
        _songs.value = mediaRepository.sortMusic(sortBy, ascending)
    }
}
