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
 * Manages video bookmarks
 */
class BookmarkManager(private val context: Context) {
    
    data class Bookmark(
        val id: String,
        val videoId: String,
        val videoPath: String,
        val position: Long,
        val name: String,
        val thumbnailPath: String?,
        val createdAt: Long
    )
    
    private val bookmarks = mutableListOf<Bookmark>()
    private val bookmarkDir: File
    
    init {
        bookmarkDir = File(context.filesDir, "bookmarks").apply {
            if (!exists()) mkdirs()
        }
    }
    
    /**
     * Add a bookmark at current position
     */
    suspend fun addBookmark(
        videoId: String,
        videoPath: String,
        position: Long,
        name: String? = null,
        thumbnail: Bitmap? = null
    ): Bookmark = withContext(Dispatchers.IO) {
        
        val id = System.currentTimeMillis().toString()
        val bookmarkName = name ?: "Bookmark ${bookmarks.size + 1}"
        
        // Save thumbnail
        var thumbnailPath: String? = null
        thumbnail?.let {
            val thumbFile = File(bookmarkDir, "$id.jpg")
            FileOutputStream(thumbFile).use { out ->
                it.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
            thumbnailPath = thumbFile.absolutePath
        }
        
        val bookmark = Bookmark(
            id = id,
            videoId = videoId,
            videoPath = videoPath,
            position = position,
            name = bookmarkName,
            thumbnailPath = thumbnailPath,
            createdAt = System.currentTimeMillis()
        )
        
        bookmarks.add(bookmark)
        saveBookmarks()
        
        bookmark
    }
    
    /**
     * Get all bookmarks for a video
     */
    fun getBookmarksForVideo(videoId: String): List<Bookmark> {
        return bookmarks.filter { it.videoId == videoId }
            .sortedBy { it.position }
    }
    
    /**
     * Get all bookmarks
     */
    fun getAllBookmarks(): List<Bookmark> {
        return bookmarks.sortedByDescending { it.createdAt }
    }
    
    /**
     * Delete a bookmark
     */
    fun deleteBookmark(bookmarkId: String) {
        bookmarks.find { it.id == bookmarkId }?.let { bookmark ->
            // Delete thumbnail
            bookmark.thumbnailPath?.let {
                File(it).delete()
            }
            bookmarks.removeAll { it.id == bookmarkId }
            saveBookmarks()
        }
    }
    
    /**
     * Rename a bookmark
     */
    fun renameBookmark(bookmarkId: String, newName: String) {
        bookmarks.indexOfFirst { it.id == bookmarkId }.let { index ->
            if (index >= 0) {
                val old = bookmarks[index]
                bookmarks[index] = old.copy(name = newName)
                saveBookmarks()
            }
        }
    }
    
    /**
     * Capture frame at position
     */
    suspend fun captureFrame(videoUri: Uri, position: Long): Bitmap? = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, videoUri)
            retriever.getFrameAtTime(
                position * 1000, // microseconds
                MediaMetadataRetriever.OPTION_CLOSEST_SYNC
            )
        } catch (e: Exception) {
            null
        } finally {
            try {
                retriever.release()
            } catch (e: Exception) {}
        }
    }
    
    private fun saveBookmarks() {
        // Persist to SharedPreferences or database
        val prefs = context.getSharedPreferences("bookmarks", Context.MODE_PRIVATE)
        val editor = prefs.edit()
        
        val bookmarksData = bookmarks.joinToString(";") { bookmark ->
            "${bookmark.id},${bookmark.videoId},${bookmark.videoPath},${bookmark.position},${bookmark.name},${bookmark.thumbnailPath ?: ""},${bookmark.createdAt}"
        }
        
        editor.putString("data", bookmarksData)
        editor.apply()
    }
    
    fun loadBookmarks() {
        val prefs = context.getSharedPreferences("bookmarks", Context.MODE_PRIVATE)
        val data = prefs.getString("data", "") ?: ""
        
        bookmarks.clear()
        
        if (data.isNotEmpty()) {
            data.split(";").forEach { entry ->
                val parts = entry.split(",")
                if (parts.size >= 7) {
                    bookmarks.add(Bookmark(
                        id = parts[0],
                        videoId = parts[1],
                        videoPath = parts[2],
                        position = parts[3].toLongOrNull() ?: 0,
                        name = parts[4],
                        thumbnailPath = parts[5].ifEmpty { null },
                        createdAt = parts[6].toLongOrNull() ?: 0
                    ))
                }
            }
        }
    }
}
