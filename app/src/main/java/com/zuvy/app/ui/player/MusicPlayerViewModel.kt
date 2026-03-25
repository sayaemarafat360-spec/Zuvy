package com.zuvy.app.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zuvy.app.data.local.dao.FavoriteDao
import com.zuvy.app.data.local.entity.FavoriteEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MusicPlayerViewModel @Inject constructor(
    private val favoriteDao: FavoriteDao
) : ViewModel() {

    private val _isFavorite = MutableStateFlow(false)
    val isFavorite: StateFlow<Boolean> = _isFavorite.asStateFlow()

    fun checkFavorite(mediaUri: String) {
        viewModelScope.launch {
            _isFavorite.value = favoriteDao.isFavorite(mediaUri)
        }
    }

    fun toggleFavorite(mediaUri: String, mediaName: String) {
        viewModelScope.launch {
            if (_isFavorite.value) {
                favoriteDao.removeFromFavorites(mediaUri)
                _isFavorite.value = false
            } else {
                favoriteDao.addToFavorites(
                    FavoriteEntity(
                        mediaUri = mediaUri,
                        mediaName = mediaName,
                        mediaType = "AUDIO"
                    )
                )
                _isFavorite.value = true
            }
        }
    }
}
