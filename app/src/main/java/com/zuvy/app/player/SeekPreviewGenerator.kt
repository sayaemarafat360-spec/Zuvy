package com.zuvy.app.player

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * Generates video thumbnails for seek preview
 */
class SeekPreviewGenerator(private val context: Context) {
    
    private val retriever = MediaMetadataRetriever()
    private val thumbnailCache = mutableMapOf<Long, Bitmap>()
    private var videoUri: Uri? = null
    private var durationMs: Long = 0
    private var intervalMs: Long = 10000 // 10 seconds
    
    fun setVideo(uri: Uri, duration: Long) {
        videoUri = uri
        durationMs = duration
        clearCache()
        
        try {
            retriever.setDataSource(context, uri)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    fun setInterval(intervalSeconds: Int) {
        intervalMs = (intervalSeconds * 1000L).coerceIn(1000, 60000)
    }
    
    suspend fun generateThumbnails(): List<Bitmap> = withContext(Dispatchers.IO) {
        val thumbnails = mutableListOf<Bitmap>()
        
        try {
            var position = 0L
            while (position < durationMs) {
                retriever.getFrameAtTime(
                    position * 1000, // microseconds
                    MediaMetadataRetriever.OPTION_CLOSEST_SYNC
                )?.let { bitmap ->
                    // Scale down for memory efficiency
                    val scaled = Bitmap.createScaledBitmap(bitmap, 160, 90, true)
                    thumbnails.add(scaled)
                    thumbnailCache[position] = scaled
                }
                position += intervalMs
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        thumbnails
    }
    
    fun getThumbnailAt(position: Long): Bitmap? {
        // Find closest cached thumbnail
        val closestKey = thumbnailCache.keys.minByOrNull { kotlin.math.abs(it - position) }
        return if (closestKey != null && kotlin.math.abs(closestKey - position) < intervalMs) {
            thumbnailCache[closestKey]
        } else {
            // Generate on demand
            try {
                retriever.getFrameAtTime(
                    position * 1000,
                    MediaMetadataRetriever.OPTION_CLOSEST_SYNC
                )?.let { bitmap ->
                    Bitmap.createScaledBitmap(bitmap, 160, 90, true)
                }
            } catch (e: Exception) {
                null
            }
        }
    }
    
    fun clearCache() {
        thumbnailCache.values.forEach { it.recycle() }
        thumbnailCache.clear()
    }
    
    fun release() {
        try {
            retriever.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        clearCache()
    }
}
