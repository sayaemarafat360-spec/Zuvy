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
 * Handles video screenshot capture
 */
class ScreenshotCapture(private val context: Context) {
    
    private val outputDir: File
    
    init {
        outputDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
            "Zuvy/Screenshots"
        ).apply {
            if (!exists()) mkdirs()
        }
    }
    
    /**
     * Capture current frame from video
     */
    suspend fun capture(
        videoUri: Uri,
        position: Long,
        videoName: String = "video"
    ): CaptureResult = withContext(Dispatchers.IO) {
        
        val retriever = MediaMetadataRetriever()
        
        try {
            retriever.setDataSource(context, videoUri)
            
            val bitmap = retriever.getFrameAtTime(
                position * 1000, // microseconds
                MediaMetadataRetriever.OPTION_CLOSEST_SYNC
            )
            
            if (bitmap == null) {
                return@withContext CaptureResult.Error("Could not capture frame")
            }
            
            // Generate filename
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "${videoName.take(30)}_$timestamp.jpg"
            val outputFile = File(outputDir, fileName)
            
            // Save bitmap
            FileOutputStream(outputFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
            }
            
            // Scan file to gallery
            android.media.MediaScannerConnection.scanFile(
                context,
                arrayOf(outputFile.absolutePath),
                arrayOf("image/jpeg"),
                null
            )
            
            CaptureResult.Success(outputFile.absolutePath, bitmap)
            
        } catch (e: Exception) {
            CaptureResult.Error(e.message ?: "Unknown error")
        } finally {
            try {
                retriever.release()
            } catch (e: Exception) {}
        }
    }
    
    /**
     * Capture multiple frames at intervals
     */
    suspend fun captureBatch(
        videoUri: Uri,
        startTime: Long,
        endTime: Long,
        interval: Long = 5000, // 5 seconds
        videoName: String = "video"
    ): List<CaptureResult> = withContext(Dispatchers.IO) {
        
        val results = mutableListOf<CaptureResult>()
        var currentTime = startTime
        
        while (currentTime <= endTime) {
            val result = capture(videoUri, currentTime, videoName)
            results.add(result)
            currentTime += interval
        }
        
        results
    }
    
    sealed class CaptureResult {
        data class Success(
            val filePath: String,
            val bitmap: Bitmap
        ) : CaptureResult()
        
        data class Error(val message: String) : CaptureResult()
    }
}
