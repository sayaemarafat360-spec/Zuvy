package com.zuvy.app.ui

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zuvy.app.data.model.MediaItem
import com.zuvy.app.data.repository.MediaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val mediaRepository: MediaRepository
) : ViewModel() {

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _videos = MutableStateFlow<List<MediaItem>>(emptyList())
    val videos: StateFlow<List<MediaItem>> = _videos.asStateFlow()

    private val _music = MutableStateFlow<List<MediaItem>>(emptyList())
    val music: StateFlow<List<MediaItem>> = _music.asStateFlow()

    private val _videoIntent = MutableLiveData<Uri>()
    val videoIntent: LiveData<Uri> = _videoIntent

    init {
        loadMedia()
    }

    fun loadMedia() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                mediaRepository.loadAllMedia()
                _videos.value = mediaRepository.getVideos()
                _music.value = mediaRepository.getMusic().map { song ->
                    MediaItem(
                        id = song.id,
                        name = song.title,
                        uri = song.uri,
                        duration = song.duration,
                        size = song.size,
                        width = 0,
                        height = 0
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun playVideoFromIntent(uri: Uri) {
        _videoIntent.value = uri
    }
}
