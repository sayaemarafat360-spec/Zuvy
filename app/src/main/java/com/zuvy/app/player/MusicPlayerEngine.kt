package com.zuvy.app.player

import android.content.Context
import android.graphics.Bitmap
import android.media.AudioManager
import android.support.v4.media.session.MediaSessionCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.zuvy.app.services.MediaNotificationManager
import com.zuvy.app.services.MediaSessionManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

/**
 * Advanced Music Player Engine integrating all music playback features
 */
class MusicPlayerEngine(
    private val context: Context,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
) {
    
    // Core player
    private val player: ExoPlayer
    private val audioManager: AudioManager
    
    // Feature managers
    private lateinit var equalizerManager: EqualizerManager
    private val crossfadeManager: CrossfadeManager
    private val fadeManager: AudioFadeManager
    private lateinit var mediaSessionManager: MediaSessionManager
    private lateinit var notificationManager: MediaNotificationManager
    
    // State
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()
    
    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()
    
    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()
    
    private val _queue = MutableStateFlow<List<QueueItem>>(emptyList())
    val queue: StateFlow<List<QueueItem>> = _queue.asStateFlow()
    
    private val _queueIndex = MutableStateFlow(-1)
    val queueIndex: StateFlow<Int> = _queueIndex.asStateFlow()
    
    private val _repeatMode = MutableStateFlow(RepeatMode.OFF)
    val repeatMode: StateFlow<RepeatMode> = _repeatMode.asStateFlow()
    
    private val _isShuffled = MutableStateFlow(false)
    val isShuffled: StateFlow<Boolean> = _isShuffled.asStateFlow()
    
    private val _playbackSpeed = MutableStateFlow(1f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()
    
    private val _currentMedia = MutableStateFlow<QueueItem?>(null)
    val currentMedia: StateFlow<QueueItem?> = _currentMedia.asStateFlow()
    
    private val _volume = MutableStateFlow(1f)
    val volume: StateFlow<Float> = _volume.asStateFlow()
    
    private var albumArt: Bitmap? = null
    private var isFavorite = false
    
    // Progress update job
    private var progressJob: Job? = null
    
    // Sleep timer
    private var sleepTimerJob: Job? = null
    private val _sleepTimerRemaining = MutableStateFlow<Long?>(null)
    val sleepTimerRemaining: StateFlow<Long?> = _sleepTimerRemaining.asStateFlow()
    
    init {
        // Initialize audio manager
        audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        
        // Initialize player
        player = ExoPlayer.Builder(context)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                true
            )
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build()
        
        // Initialize equalizer
        equalizerManager = EqualizerManager(context, player.audioSessionId)
        
        // Initialize crossfade
        crossfadeManager = CrossfadeManager(scope, context)
        crossfadeManager.setCallbacks(
            onFadeOut = { /* handled by crossfade manager */ },
            onFadeIn = { /* handled by crossfade manager */ },
            onVolumeChange = { vol -> setVolume(vol) }
        )
        
        // Initialize fade manager
        fadeManager = AudioFadeManager(scope)
        
        // Initialize media session
        mediaSessionManager = MediaSessionManager(
            context,
            onPlay = { play() },
            onPause = { pause() },
            onNext = { playNext() },
            onPrevious = { playPrevious() },
            onStop = { stop() },
            onSeek = { seekTo(it) },
            onFavorite = { toggleFavorite() }
        )
        
        // Initialize notification manager
        notificationManager = MediaNotificationManager(context)
        
        // Set up player listener
        setupPlayerListener()
        
        // Start progress updates
        startProgressUpdates()
    }
    
    private fun setupPlayerListener() {
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
                
                if (isPlaying) {
                    mediaSessionManager.setActive(true)
                }
                
                updateNotification()
            }
            
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_ENDED -> {
                        handleTrackEnded()
                    }
                    Player.STATE_READY -> {
                        _duration.value = player.duration
                    }
                }
            }
        })
    }
    
    private fun startProgressUpdates() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                _currentPosition.value = player.currentPosition
                _duration.value = player.duration
                
                // Check for crossfade
                if (crossfadeManager.isCrossfadeEnabled()) {
                    crossfadeManager.startCrossfade(
                        player.currentPosition,
                        player.duration
                    ) {
                        // On crossfade trigger, prepare next track
                        prepareNextTrack()
                    }
                }
                
                // Update media session
                mediaSessionManager.updatePlaybackState(
                    _isPlaying.value,
                    _currentPosition.value,
                    _duration.value,
                    _playbackSpeed.value
                )
                
                delay(100)
            }
        }
    }
    
    /**
     * Play a queue of items
     */
    fun playQueue(items: List<QueueItem>, startIndex: Int = 0) {
        _queue.value = items
        _queueIndex.value = startIndex
        
        if (items.isNotEmpty() && startIndex in items.indices) {
            playItem(items[startIndex])
        }
    }
    
    /**
     * Play a single item
     */
    fun playItem(item: QueueItem) {
        _currentMedia.value = item
        
        player.setMediaItem(item.toMediaItem())
        player.prepare()
        player.playWhenReady = true
        
        // Update media session metadata
        mediaSessionManager.updateMetadata(item, albumArt)
        
        // Fade in if crossfade is enabled
        if (crossfadeManager.isCrossfadeEnabled()) {
            crossfadeManager.startFadeIn()
        }
    }
    
    /**
     * Play/Pause toggle
     */
    fun playPause() {
        if (_isPlaying.value) {
            pause()
        } else {
            play()
        }
    }
    
    /**
     * Play
     */
    fun play() {
        player.play()
    }
    
    /**
     * Pause
     */
    fun pause() {
        fadeManager.fadeOut(300) { vol ->
            player.volume = vol
        }
        
        scope.launch {
            delay(300)
            player.pause()
        }
    }
    
    /**
     * Stop playback
     */
    fun stop() {
        player.stop()
        _currentMedia.value = null
        notificationManager.hideNotification()
        mediaSessionManager.setActive(false)
    }
    
    /**
     * Play next track
     */
    fun playNext() {
        val currentIndex = _queueIndex.value
        val queue = _queue.value
        
        if (currentIndex < queue.size - 1) {
            // Fade out current, fade in next
            if (crossfadeManager.isCrossfadeEnabled()) {
                crossfadeManager.startFadeOut {
                    _queueIndex.value = currentIndex + 1
                    playItem(queue[currentIndex + 1])
                }
            } else {
                _queueIndex.value = currentIndex + 1
                playItem(queue[currentIndex + 1])
            }
        } else if (_repeatMode.value == RepeatMode.ALL) {
            // Wrap around
            _queueIndex.value = 0
            playItem(queue[0])
        }
    }
    
    /**
     * Play previous track
     */
    fun playPrevious() {
        val currentIndex = _queueIndex.value
        val queue = _queue.value
        
        // If more than 3 seconds played, restart current track
        if (_currentPosition.value > 3000) {
            player.seekTo(0)
            return
        }
        
        if (currentIndex > 0) {
            _queueIndex.value = currentIndex - 1
            playItem(queue[currentIndex - 1])
        } else if (_repeatMode.value == RepeatMode.ALL) {
            // Wrap around
            _queueIndex.value = queue.size - 1
            playItem(queue[queue.size - 1])
        }
    }
    
    /**
     * Seek to position
     */
    fun seekTo(position: Long) {
        player.seekTo(position)
    }
    
    /**
     * Set playback speed
     */
    fun setPlaybackSpeed(speed: Float) {
        player.setPlaybackSpeed(speed)
        _playbackSpeed.value = speed
    }
    
    /**
     * Toggle shuffle
     */
    fun toggleShuffle() {
        val newState = !_isShuffled.value
        _isShuffled.value = newState
        
        if (newState) {
            // Shuffle queue
            val queue = _queue.value.toMutableList()
            val current = queue.removeAt(_queueIndex.value)
            queue.shuffle()
            queue.add(0, current)
            _queue.value = queue
            _queueIndex.value = 0
        }
    }
    
    /**
     * Toggle repeat mode
     */
    fun toggleRepeatMode() {
        _repeatMode.value = when (_repeatMode.value) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        
        player.repeatMode = when (_repeatMode.value) {
            RepeatMode.OFF -> Player.REPEAT_MODE_OFF
            RepeatMode.ALL -> Player.REPEAT_MODE_ALL
            RepeatMode.ONE -> Player.REPEAT_MODE_ONE
        }
    }
    
    /**
     * Set volume (0-1)
     */
    fun setVolume(volume: Float) {
        player.volume = volume
        _volume.value = volume
    }
    
    /**
     * Set album art for notification
     */
    fun setAlbumArt(art: Bitmap?) {
        albumArt = art
        updateNotification()
    }
    
    /**
     * Set favorite state
     */
    fun setFavorite(favorite: Boolean) {
        isFavorite = favorite
        updateNotification()
    }
    
    /**
     * Toggle favorite
     */
    fun toggleFavorite() {
        isFavorite = !isFavorite
        updateNotification()
    }
    
    /**
     * Add to queue
     */
    fun addToQueue(item: QueueItem) {
        val queue = _queue.value.toMutableList()
        queue.add(item)
        _queue.value = queue
    }
    
    /**
     * Add next in queue
     */
    fun addNext(item: QueueItem) {
        val queue = _queue.value.toMutableList()
        queue.add(_queueIndex.value + 1, item)
        _queue.value = queue
    }
    
    /**
     * Remove from queue
     */
    fun removeFromQueue(index: Int) {
        val queue = _queue.value.toMutableList()
        if (index in queue.indices) {
            queue.removeAt(index)
            _queue.value = queue
            
            if (index < _queueIndex.value) {
                _queueIndex.value -= 1
            }
        }
    }
    
    /**
     * Move queue item
     */
    fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        val queue = _queue.value.toMutableList()
        if (fromIndex in queue.indices && toIndex in queue.indices) {
            val item = queue.removeAt(fromIndex)
            queue.add(toIndex, item)
            _queue.value = queue
            
            // Update current index if needed
            when {
                fromIndex == _queueIndex.value -> _queueIndex.value = toIndex
                fromIndex < _queueIndex.value && toIndex >= _queueIndex.value -> _queueIndex.value -= 1
                fromIndex > _queueIndex.value && toIndex <= _queueIndex.value -> _queueIndex.value += 1
            }
        }
    }
    
    /**
     * Play from queue at index
     */
    fun playFromQueue(index: Int) {
        val queue = _queue.value
        if (index in queue.indices) {
            _queueIndex.value = index
            playItem(queue[index])
        }
    }
    
    /**
     * Set sleep timer
     */
    fun setSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        
        val totalMs = minutes * 60_000L
        var remaining = totalMs
        
        _sleepTimerRemaining.value = remaining
        
        sleepTimerJob = scope.launch {
            while (remaining > 0 && isActive) {
                delay(1000)
                remaining -= 1000
                _sleepTimerRemaining.value = remaining
            }
            
            // Time's up - fade out and pause
            fadeManager.fadeOut(3000) { vol ->
                player.volume = vol
            }
            
            delay(3000)
            pause()
            _sleepTimerRemaining.value = null
        }
    }
    
    /**
     * Cancel sleep timer
     */
    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        _sleepTimerRemaining.value = null
    }
    
    /**
     * Get equalizer manager
     */
    fun getEqualizerManager(): EqualizerManager = equalizerManager
    
    /**
     * Get crossfade manager
     */
    fun getCrossfadeManager(): CrossfadeManager = crossfadeManager
    
    /**
     * Get media session token
     */
    fun getMediaSessionToken(): MediaSessionCompat.Token? = mediaSessionManager.sessionToken
    
    /**
     * Get player instance
     */
    fun getPlayer(): ExoPlayer = player
    
    private fun handleTrackEnded() {
        when (_repeatMode.value) {
            RepeatMode.ONE -> {
                player.seekTo(0)
                player.play()
            }
            else -> {
                playNext()
            }
        }
    }
    
    private fun prepareNextTrack() {
        // Pre-load next track for gapless playback
        val nextIndex = _queueIndex.value + 1
        val queue = _queue.value
        
        if (nextIndex in queue.indices) {
            player.setMediaItems(
                listOf(_currentMedia.value!!.toMediaItem(), queue[nextIndex].toMediaItem()),
                0,
                0
            )
        }
    }
    
    private fun updateNotification() {
        val item = _currentMedia.value ?: return
        
        val notification = notificationManager.buildNotification(
            item = item,
            isPlaying = _isPlaying.value,
            position = _currentPosition.value,
            duration = _duration.value,
            albumArt = albumArt,
            isFavorite = isFavorite,
            mediaSessionToken = mediaSessionManager.sessionToken!!
        )
        
        notificationManager.showNotification(notification)
    }
    
    /**
     * Release all resources
     */
    fun release() {
        progressJob?.cancel()
        sleepTimerJob?.cancel()
        
        crossfadeManager.release()
        fadeManager.release()
        equalizerManager.release()
        mediaSessionManager.release()
        
        notificationManager.hideNotification()
        player.release()
        
        scope.cancel()
    }
}

/**
 * Repeat mode enum
 */
enum class RepeatMode {
    OFF, ALL, ONE
}
