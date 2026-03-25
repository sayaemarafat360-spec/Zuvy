package com.zuvy.app.player

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.zuvy.app.data.local.dao.HistoryDao
import com.zuvy.app.data.local.dao.QueueDao
import com.zuvy.app.data.local.entity.HistoryEntity
import com.zuvy.app.data.local.entity.QueueEntity
import com.zuvy.app.utils.PreferenceManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlayerManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val historyDao: HistoryDao,
    private val queueDao: QueueDao,
    private val preferenceManager: PreferenceManager
) {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // ExoPlayer instance
    private var player: ExoPlayer? = null

    // Current media state
    private val _currentMedia = MutableStateFlow<CurrentMedia?>(null)
    val currentMedia: StateFlow<CurrentMedia?> = _currentMedia

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _playbackState = MutableStateFlow<PlaybackState>(PlaybackState.Idle)
    val playbackState: StateFlow<PlaybackState> = _playbackState

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration

    private val _bufferedPosition = MutableStateFlow(0L)
    val bufferedPosition: StateFlow<Long> = _bufferedPosition

    // Queue state
    private val _queue = MutableStateFlow<List<QueueItem>>(emptyList())
    val queue: StateFlow<List<QueueItem>> = _queue

    private val _queueIndex = MutableStateFlow(-1)
    val queueIndex: StateFlow<Int> = _queueIndex

    // Repeat and shuffle
    private val _repeatMode = MutableStateFlow(RepeatMode.OFF)
    val repeatMode: StateFlow<RepeatMode> = _repeatMode

    private val _isShuffled = MutableStateFlow(false)
    val isShuffled: StateFlow<Boolean> = _isShuffled

    // Sleep timer
    private var sleepTimerJob: Job? = null
    private val _sleepTimerRemaining = MutableStateFlow<Long?>(null)
    val sleepTimerRemaining: StateFlow<Long?> = _sleepTimerRemaining

    init {
        initializePlayer()
        loadQueue()
    }

    private fun initializePlayer() {
        player = ExoPlayer.Builder(context).apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                true
            )
            setHandleAudioBecomingNoisy(true)
        }.build().also { exoPlayer ->
            exoPlayer.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(playing: Boolean) {
                    _isPlaying.value = playing
                }

                override fun onPlaybackStateChanged(state: Int) {
                    _playbackState.value = when (state) {
                        Player.STATE_IDLE -> PlaybackState.Idle
                        Player.STATE_BUFFERING -> PlaybackState.Buffering
                        Player.STATE_READY -> PlaybackState.Ready
                        Player.STATE_ENDED -> PlaybackState.Ended
                        else -> PlaybackState.Idle
                    }

                    if (state == Player.STATE_ENDED) {
                        onPlaybackEnded()
                    }
                }

                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    mediaItem?.let {
                        updateCurrentMedia(it)
                    }
                }
            })
        }

        // Start position update loop
        scope.launch {
            while (isActive) {
                player?.let { exoPlayer ->
                    _currentPosition.value = exoPlayer.currentPosition
                    _duration.value = exoPlayer.duration.takeIf { it > 0 } ?: 0
                    _bufferedPosition.value = exoPlayer.bufferedPosition
                }
                delay(500)
            }
        }
    }

    fun playMedia(uri: Uri, name: String, type: MediaType, position: Long = 0) {
        val mediaItem = MediaItem.Builder()
            .setUri(uri)
            .setMediaMetadata(
                androidx.media3.common.MediaMetadata.Builder()
                    .setTitle(name)
                    .build()
            )
            .build()

        player?.setMediaItem(mediaItem)
        player?.prepare()
        player?.seekTo(position)
        player?.playWhenReady = true

        _currentMedia.value = CurrentMedia(uri, name, type)
        
        // Save to history
        scope.launch(Dispatchers.IO) {
            val existing = historyDao.getByUri(uri.toString())
            if (existing != null) {
                historyDao.insertOrUpdate(existing.copy(
                    lastPlayedAt = System.currentTimeMillis(),
                    playCount = existing.playCount + 1
                ))
            } else {
                historyDao.insertOrUpdate(
                    HistoryEntity(
                        mediaUri = uri.toString(),
                        mediaName = name,
                        mediaType = type.name,
                        lastPosition = 0
                    )
                )
            }
        }
    }

    fun playQueue(items: List<QueueItem>, startIndex: Int = 0) {
        scope.launch(Dispatchers.IO) {
            queueDao.clearQueue()
            val queueEntities = items.mapIndexed { index, item ->
                QueueEntity(
                    mediaUri = item.uri.toString(),
                    mediaName = item.name,
                    mediaType = item.type.name,
                    position = index
                )
            }
            queueDao.addToQueue(queueEntities)
        }

        _queue.value = items
        _queueIndex.value = startIndex

        playFromQueue(startIndex)
    }

    fun addToQueue(item: QueueItem, playNext: Boolean = false) {
        val currentQueue = _queue.value.toMutableList()
        
        if (playNext) {
            val insertIndex = (_queueIndex.value + 1).coerceIn(0, currentQueue.size)
            currentQueue.add(insertIndex, item)
        } else {
            currentQueue.add(item)
        }
        
        _queue.value = currentQueue

        scope.launch(Dispatchers.IO) {
            queueDao.addToQueue(
                QueueEntity(
                    mediaUri = item.uri.toString(),
                    mediaName = item.name,
                    mediaType = item.type.name,
                    position = currentQueue.size - 1
                )
            )
        }
    }

    private fun playFromQueue(index: Int) {
        val items = _queue.value
        if (index in items.indices) {
            _queueIndex.value = index
            val item = items[index]
            playMedia(item.uri, item.name, item.type)
        }
    }

    private fun onPlaybackEnded() {
        when (_repeatMode.value) {
            RepeatMode.ONE -> {
                player?.seekTo(0)
                player?.play()
            }
            RepeatMode.ALL -> {
                val nextIndex = (_queueIndex.value + 1) % _queue.value.size
                playFromQueue(nextIndex)
            }
            RepeatMode.OFF -> {
                if (_queueIndex.value < _queue.value.size - 1) {
                    playFromQueue(_queueIndex.value + 1)
                }
            }
        }
    }

    fun playNext() {
        if (_queue.value.isNotEmpty()) {
            val nextIndex = if (_isShuffled.value) {
                _queue.value.indices.random()
            } else {
                (_queueIndex.value + 1) % _queue.value.size
            }
            playFromQueue(nextIndex)
        }
    }

    fun playPrevious() {
        if (_currentPosition.value > 3000) {
            player?.seekTo(0)
        } else if (_queue.value.isNotEmpty()) {
            val prevIndex = if (_queueIndex.value > 0) _queueIndex.value - 1 else _queue.value.size - 1
            playFromQueue(prevIndex)
        }
    }

    fun playPause() {
        player?.let {
            if (it.isPlaying) {
                it.pause()
            } else {
                it.play()
            }
        }
    }

    fun seekTo(position: Long) {
        player?.seekTo(position)
    }

    fun seekRelative(offsetMs: Long) {
        player?.let {
            val newPosition = (it.currentPosition + offsetMs).coerceIn(0, it.duration)
            it.seekTo(newPosition)
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        player?.setPlaybackSpeed(speed)
        preferenceManager.setPlaybackSpeed(speed)
    }

    fun toggleRepeatMode() {
        _repeatMode.value = when (_repeatMode.value) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        
        player?.repeatMode = when (_repeatMode.value) {
            RepeatMode.OFF -> Player.REPEAT_MODE_OFF
            RepeatMode.ALL -> Player.REPEAT_MODE_ALL
            RepeatMode.ONE -> Player.REPEAT_MODE_ONE
        }
    }

    fun toggleShuffle() {
        _isShuffled.value = !_isShuffled.value
        player?.shuffleModeEnabled = _isShuffled.value
    }

    fun setSleepTimer(durationMinutes: Int) {
        sleepTimerJob?.cancel()
        
        val durationMs = durationMinutes * 60_000L
        _sleepTimerRemaining.value = durationMs

        sleepTimerJob = scope.launch {
            var remaining = durationMs
            while (remaining > 0) {
                delay(1000)
                remaining -= 1000
                _sleepTimerRemaining.value = remaining
            }
            player?.pause()
            _sleepTimerRemaining.value = null
        }
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        _sleepTimerRemaining.value = null
    }

    fun removeFromQueue(index: Int) {
        val currentQueue = _queue.value.toMutableList()
        if (index in currentQueue.indices) {
            currentQueue.removeAt(index)
            _queue.value = currentQueue
            
            scope.launch(Dispatchers.IO) {
                val queueItems = queueDao.getQueueSync()
                queueItems.getOrNull(index)?.let { queueDao.removeFromQueue(it.id) }
            }
        }
    }

    fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        val currentQueue = _queue.value.toMutableList()
        if (fromIndex in currentQueue.indices && toIndex in currentQueue.indices) {
            val item = currentQueue.removeAt(fromIndex)
            currentQueue.add(toIndex, item)
            _queue.value = currentQueue

            scope.launch(Dispatchers.IO) {
                queueDao.reorderQueue(fromIndex, toIndex)
            }
        }
    }

    fun clearQueue() {
        _queue.value = emptyList()
        _queueIndex.value = -1
        scope.launch(Dispatchers.IO) {
            queueDao.clearQueue()
        }
    }

    private fun updateCurrentMedia(mediaItem: MediaItem) {
        _currentMedia.value?.let { current ->
            scope.launch(Dispatchers.IO) {
                historyDao.updatePosition(
                    current.uri.toString(),
                    _currentPosition.value
                )
            }
        }
    }

    private fun loadQueue() {
        scope.launch(Dispatchers.IO) {
            queueDao.getQueueSync().map { entity ->
                QueueItem(
                    uri = Uri.parse(entity.mediaUri),
                    name = entity.mediaName,
                    type = MediaType.valueOf(entity.mediaType)
                )
            }.also { items ->
                _queue.value = items
            }
        }
    }

    fun saveCurrentPosition() {
        _currentMedia.value?.let { media ->
            scope.launch(Dispatchers.IO) {
                historyDao.updatePosition(
                    media.uri.toString(),
                    _currentPosition.value
                )
            }
        }
    }

    fun release() {
        saveCurrentPosition()
        player?.release()
        player = null
        scope.cancel()
    }

    fun getPlayer(): ExoPlayer? = player

    companion object {
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "zuvy_playback"
    }
}

// Data classes
data class CurrentMedia(
    val uri: Uri,
    val name: String,
    val type: MediaType
)

data class QueueItem(
    val uri: Uri,
    val name: String,
    val type: MediaType
)

enum class MediaType {
    VIDEO, AUDIO
}

enum class PlaybackState {
    Idle, Buffering, Ready, Ended
}

enum class RepeatMode {
    OFF, ALL, ONE
}
