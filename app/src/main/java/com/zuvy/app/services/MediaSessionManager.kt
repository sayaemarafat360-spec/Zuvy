package com.zuvy.app.services

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.media.app.NotificationCompat
import com.zuvy.app.R
import com.zuvy.app.player.QueueItem

/**
 * Manages MediaSession for lock screen controls and external media control
 */
class MediaSessionManager(
    private val context: Context,
    private val onPlay: () -> Unit,
    private val onPause: () -> Unit,
    private val onNext: () -> Unit,
    private val onPrevious: () -> Unit,
    private val onStop: () -> Unit,
    private val onSeek: (Long) -> Unit,
    private val onFavorite: () -> Unit
) {
    
    companion object {
        const val MEDIA_SESSION_TAG = "ZuvyMusicSession"
        const val ACTION_PLAY = "com.zuvy.app.ACTION_PLAY"
        const val ACTION_PAUSE = "com.zuvy.app.ACTION_PAUSE"
        const val ACTION_NEXT = "com.zuvy.app.ACTION_NEXT"
        const val ACTION_PREVIOUS = "com.zuvy.app.ACTION_PREVIOUS"
        const val ACTION_STOP = "com.zuvy.app.ACTION_STOP"
        const val ACTION_FAVORITE = "com.zuvy.app.ACTION_FAVORITE"
    }
    
    private var mediaSession: MediaSessionCompat? = null
    private var currentMetadata: MediaMetadataCompat? = null
    
    val sessionToken: MediaSessionCompat.Token?
        get() = mediaSession?.sessionToken
    
    init {
        setupMediaSession()
    }
    
    private fun setupMediaSession() {
        mediaSession = MediaSessionCompat(context, MEDIA_SESSION_TAG).apply {
            setFlags(
                MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or
                MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
            )
            
            // Set up callback for media controls
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() {
                    this@MediaSessionManager.onPlay()
                }
                
                override fun onPause() {
                    this@MediaSessionManager.onPause()
                }
                
                override fun onSkipToNext() {
                    this@MediaSessionManager.onNext()
                }
                
                override fun onSkipToPrevious() {
                    this@MediaSessionManager.onPrevious()
                }
                
                override fun onStop() {
                    this@MediaSessionManager.onStop()
                }
                
                override fun onSeekTo(pos: Long) {
                    this@MediaSessionManager.onSeek(pos)
                }
                
                override fun onCustomAction(action: String?, extras: android.os.Bundle?) {
                    when (action) {
                        ACTION_FAVORITE -> this@MediaSessionManager.onFavorite()
                    }
                }
            })
            
            // Set media button receiver
            val mediaButtonIntent = Intent(Intent.ACTION_MEDIA_BUTTON).apply {
                setClass(context, MediaButtonReceiver::class.java)
            }
            val mediaButtonPendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                mediaButtonIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            setMediaButtonReceiver(mediaButtonPendingIntent)
            
            setActive(true)
        }
    }
    
    /**
     * Update playback state
     */
    fun updatePlaybackState(
        isPlaying: Boolean,
        position: Long,
        duration: Long,
        playbackSpeed: Float = 1f,
        canSkipNext: Boolean = true,
        canSkipPrevious: Boolean = true
    ) {
        val state = if (isPlaying) {
            PlaybackStateCompat.STATE_PLAYING
        } else {
            PlaybackStateCompat.STATE_PAUSED
        }
        
        val playbackState = PlaybackStateCompat.Builder()
            .setActions(
                PlaybackStateCompat.ACTION_PLAY or
                PlaybackStateCompat.ACTION_PAUSE or
                PlaybackStateCompat.ACTION_PLAY_PAUSE or
                PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                PlaybackStateCompat.ACTION_STOP or
                PlaybackStateCompat.ACTION_SEEK_TO or
                PlaybackStateCompat.ACTION_SET_RATING
            )
            .addCustomAction(
                PlaybackStateCompat.CustomAction.Builder(
                    ACTION_FAVORITE,
                    context.getString(R.string.add_to_favorites),
                    R.drawable.ic_heart
                ).build()
            )
            .setState(state, position, playbackSpeed)
            .setBufferedPosition(duration)
            .build()
        
        mediaSession?.setPlaybackState(playbackState)
    }
    
    /**
     * Update media metadata for lock screen
     */
    fun updateMetadata(
        item: QueueItem,
        albumArt: Bitmap? = null
    ) {
        val metadataBuilder = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, item.name)
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, item.artist ?: "Unknown Artist")
            .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, item.album ?: "Unknown Album")
            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, item.duration)
            .putLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER, item.trackNumber.toLong())
            .putLong(MediaMetadataCompat.METADATA_KEY_NUM_TRACKS, item.totalTracks.toLong())
        
        albumArt?.let { art ->
            metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, art)
            metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON, art)
        }
        
        currentMetadata = metadataBuilder.build()
        mediaSession?.setMetadata(currentMetadata)
    }
    
    /**
     * Set active state
     */
    fun setActive(active: Boolean) {
        mediaSession?.isActive = active
    }
    
    /**
     * Release resources
     */
    fun release() {
        mediaSession?.isActive = false
        mediaSession?.release()
        mediaSession = null
    }
    
    /**
     * Get current metadata
     */
    fun getCurrentMetadata(): MediaMetadataCompat? = currentMetadata
}
