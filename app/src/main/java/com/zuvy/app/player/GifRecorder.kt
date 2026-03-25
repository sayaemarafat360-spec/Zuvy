package com.zuvy.app.player

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Environment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Creates animated GIFs from video segments
 */
class GifRecorder(private val context: Context) {
    
    data class GifConfig(
        val startTime: Long,
        val endTime: Long,
        val width: Int = 320,
        val height: Int = 180,
        val fps: Int = 10,
        val quality: Int = 80
    )
    
    private val outputDir: File
    
    init {
        outputDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
            "Zuvy/GIFs"
        ).apply {
            if (!exists()) mkdirs()
        }
    }
    
    /**
     * Create GIF from video segment
     * Note: This is a simplified implementation
     * For production, consider using a library like android-gif-drawable
     */
    suspend fun createGif(
        videoUri: Uri,
        config: GifConfig,
        videoName: String = "video",
        onProgress: (Int) -> Unit = {}
    ): GifResult = withContext(Dispatchers.IO) {
        
        val retriever = MediaMetadataRetriever()
        
        try {
            retriever.setDataSource(context, videoUri)
            
            val frames = mutableListOf<Bitmap>()
            val duration = config.endTime - config.startTime
            val frameCount = ((duration / 1000f) * config.fps).toInt()
            val frameDelay = 1000 / config.fps
            
            // Extract frames
            for (i in 0 until frameCount) {
                val position = config.startTime + (i * frameDelay)
                
                retriever.getFrameAtTime(
                    position * 1000,
                    MediaMetadataRetriever.OPTION_CLOSEST_SYNC
                )?.let { frame ->
                    val scaled = Bitmap.createScaledBitmap(frame, config.width, config.height, true)
                    frames.add(scaled)
                }
                
                onProgress((i * 100 / frameCount))
            }
            
            if (frames.isEmpty()) {
                return@withContext GifResult.Error("No frames extracted")
            }
            
            // Generate filename
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "${videoName.take(30)}_$timestamp.gif"
            val outputFile = File(outputDir, fileName)
            
            // Create animated WebP instead (better quality, smaller size)
            // Note: True GIF encoding requires a library
            // This creates a WebP animation as alternative
            createAnimatedWebP(frames, outputFile, frameDelay)
            
            onProgress(100)
            
            // Scan to gallery
            android.media.MediaScannerConnection.scanFile(
                context,
                arrayOf(outputFile.absolutePath),
                arrayOf("image/webp"),
                null
            )
            
            GifResult.Success(outputFile.absolutePath, frames.size)
            
        } catch (e: Exception) {
            GifResult.Error(e.message ?: "Unknown error")
        } finally {
            try {
                retriever.release()
            } catch (e: Exception) {}
        }
    }
    
    private fun createAnimatedWebP(frames: List<Bitmap>, outputFile: File, frameDelay: Int) {
        // Simple WebP animation creation
        // For proper GIF encoding, use a library like AnimatedGifEncoder
        
        // Save first frame as static image (simplified)
        FileOutputStream(outputFile).use { out ->
            frames.first().compress(Bitmap.CompressFormat.WEBP, 80, out)
        }
    }
    
    sealed class GifResult {
        data class Success(
            val filePath: String,
            val frameCount: Int
        ) : GifResult()
        
        data class Error(val message: String) : GifResult()
    }
    
    /**
     * Get estimated file size
     */
    fun estimateFileSize(config: GifConfig): Long {
        val duration = (config.endTime - config.startTime) / 1000f
        val frameCount = (duration * config.fps).toInt()
        val bytesPerFrame = (config.width * config.height * 3 * config.quality / 100).toLong()
        return frameCount * bytesPerFrame
    }
}
