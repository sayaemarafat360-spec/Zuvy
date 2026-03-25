package com.zuvy.app.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.Build
import android.support.v4.media.session.MediaSessionCompat
import androidx.core.app.NotificationCompat
import androidx.media.app.NotificationCompat.MediaStyle
import com.zuvy.app.R
import com.zuvy.app.player.QueueItem
import com.zuvy.app.ui.MainActivity

/**
 * Manages media notifications for music playback
 */
class MediaNotificationManager(
    private val context: Context
) {
    
    companion object {
        const val CHANNEL_ID = "zuvy_music_channel"
        const val CHANNEL_NAME = "Zuvy Music Playback"
        const val NOTIFICATION_ID = 1001
        
        const val ACTION_PLAY = "com.zuvy.app.ACTION_PLAY"
        const val ACTION_PAUSE = "com.zuvy.app.ACTION_PAUSE"
        const val ACTION_NEXT = "com.zuvy.app.ACTION_NEXT"
        const val ACTION_PREVIOUS = "com.zuvy.app.ACTION_PREVIOUS"
        const val ACTION_STOP = "com.zuvy.app.ACTION_STOP"
        const val ACTION_FAVORITE = "com.zuvy.app.ACTION_FAVORITE"
    }
    
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    
    init {
        createNotificationChannel()
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Media playback controls"
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    /**
     * Build media notification
     */
    fun buildNotification(
        item: QueueItem,
        isPlaying: Boolean,
        position: Long,
        duration: Long,
        albumArt: Bitmap?,
        isFavorite: Boolean,
        mediaSessionToken: MediaSessionCompat.Token
    ): Notification {
        // Create content intent
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("open_player", true)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            0,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Create action intents
        val playPauseIntent = createBroadcastIntent(
            if (isPlaying) ACTION_PAUSE else ACTION_PLAY
        )
        val nextIntent = createBroadcastIntent(ACTION_NEXT)
        val previousIntent = createBroadcastIntent(ACTION_PREVIOUS)
        val stopIntent = createBroadcastIntent(ACTION_STOP)
        val favoriteIntent = createBroadcastIntent(ACTION_FAVORITE)
        
        // Build notification
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(item.name)
            .setContentText(item.artist ?: "Unknown Artist")
            .setSubText(item.album)
            .setSmallIcon(R.drawable.ic_music_note)
            .setContentIntent(contentPendingIntent)
            .setDeleteIntent(stopIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(isPlaying)
            .setShowWhen(false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .setStyle(
                MediaStyle()
                    .setMediaSession(mediaSessionToken)
                    .setShowActionsInCompactView(0, 1, 2)
                    .setShowCancelButton(true)
                    .setCancelButtonIntent(stopIntent)
            )
        
        // Add album art
        albumArt?.let { art ->
            builder.setLargeIcon(art)
        } ?: run {
            // Use default artwork
            val defaultArt = getDefaultAlbumArt()
            defaultArt?.let { builder.setLargeIcon(it) }
        }
        
        // Add actions
        builder.addAction(
            R.drawable.ic_skip_previous,
            context.getString(R.string.previous),
            previousIntent
        )
        
        val playPauseIcon = if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
        val playPauseText = if (isPlaying) context.getString(R.string.pause) else context.getString(R.string.play)
        builder.addAction(
            playPauseIcon,
            playPauseText,
            playPauseIntent
        )
        
        builder.addAction(
            R.drawable.ic_skip_next,
            context.getString(R.string.next),
            nextIntent
        )
        
        // Add favorite action
        val favoriteIcon = if (isFavorite) R.drawable.ic_heart else R.drawable.ic_heart
        builder.addAction(
            favoriteIcon,
            context.getString(R.string.add_to_favorites),
            favoriteIntent
        )
        
        // Add progress indicator (Android 11+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            builder.setProgress(duration.toInt(), position.toInt(), false)
        }
        
        return builder.build()
    }
    
    /**
     * Build mini notification for collapsed state
     */
    fun buildCompactNotification(
        item: QueueItem,
        isPlaying: Boolean,
        albumArt: Bitmap?,
        mediaSessionToken: MediaSessionCompat.Token
    ): Notification {
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context, 0, contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(item.name)
            .setContentText(item.artist ?: "Unknown Artist")
            .setSmallIcon(R.drawable.ic_music_note)
            .setContentIntent(contentPendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setStyle(
                MediaStyle()
                    .setMediaSession(mediaSessionToken)
                    .setShowActionsInCompactView(0)
            )
            .setLargeIcon(albumArt)
            .addAction(
                if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play,
                if (isPlaying) "Pause" else "Play",
                createBroadcastIntent(if (isPlaying) ACTION_PAUSE else ACTION_PLAY)
            )
            .build()
    }
    
    private fun createBroadcastIntent(action: String): PendingIntent {
        val intent = Intent(action).setClass(context, NotificationReceiver::class.java)
        return PendingIntent.getBroadcast(
            context,
            action.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
    
    private fun getDefaultAlbumArt(): Bitmap? {
        return try {
            val drawable: Drawable = context.resources.getDrawable(R.drawable.ic_music_note, null)
            val bitmap = Bitmap.createBitmap(500, 500, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            bitmap
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Show/update notification
     */
    fun showNotification(notification: Notification) {
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
    
    /**
     * Hide notification
     */
    fun hideNotification() {
        notificationManager.cancel(NOTIFICATION_ID)
    }
}
