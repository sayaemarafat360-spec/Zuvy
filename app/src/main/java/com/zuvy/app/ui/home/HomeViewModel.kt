package com.zuvy.app.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.zuvy.app.data.model.MediaItem
import com.zuvy.app.data.model.Folder
import com.zuvy.app.data.repository.MediaRepository
import com.zuvy.app.data.repository.SortBy
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val mediaRepository: MediaRepository
) : ViewModel() {

    private val _videoCount = MutableLiveData(0)
    val videoCount: LiveData<Int> = _videoCount

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _videos = MutableStateFlow<List<MediaItem>>(emptyList())
    val videos: StateFlow<List<MediaItem>> = _videos.asStateFlow()

    private val _folders = MutableStateFlow<List<Folder>>(emptyList())
    val folders: StateFlow<List<Folder>> = _folders.asStateFlow()

    private var currentSortBy: SortBy = SortBy.DATE
    private var currentSortAscending = false

    init {
        loadMedia()
    }

    fun loadMedia() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                mediaRepository.loadAllMedia()
                _videos.value = mediaRepository.sortVideos(currentSortBy, currentSortAscending)
                _folders.value = mediaRepository.folders
                _videoCount.value = _videos.value.size
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun searchVideos(query: String) {
        viewModelScope.launch {
            if (query.isEmpty()) {
                _videos.value = mediaRepository.sortVideos(currentSortBy, currentSortAscending)
            } else {
                _videos.value = mediaRepository.searchVideos(query)
            }
        }
    }

    fun sortVideos(sortBy: SortBy, ascending: Boolean) {
        currentSortBy = sortBy
        currentSortAscending = ascending
        _videos.value = mediaRepository.sortVideos(sortBy, ascending)
    }

    fun getVideosByFolder(folderPath: String): List<MediaItem> {
        return mediaRepository.getVideosByFolder(folderPath)
    }

    fun getVideoByUri(uri: String): MediaItem? {
        return mediaRepository.getMediaByUri(android.net.Uri.parse(uri))
    }
}
