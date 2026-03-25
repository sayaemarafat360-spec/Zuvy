package com.zuvy.app.settings

import android.content.Context
import android.os.Environment
import android.os.StatFs
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Storage Manager for storage analysis and cleanup
 */
class StorageManager(private val context: Context) {
    
    companion object {
        const val TAG = "StorageManager"
    }
    
    data class StorageInfo(
        val totalSpace: Long,
        val freeSpace: Long,
        val usedSpace: Long,
        val appCacheSize: Long,
        val appDataSize: Long,
        val videosSize: Long,
        val musicSize: Long,
        val thumbnailsSize: Long,
        val downloadsSize: Long
    ) {
        val usedPercentage: Int
            get() = if (totalSpace > 0) ((usedSpace * 100) / totalSpace).toInt() else 0
        
        val formattedTotal: String
            get() = formatSize(totalSpace)
        
        val formattedFree: String
            get() = formatSize(freeSpace)
        
        val formattedUsed: String
            get() = formatSize(usedSpace)
        
        private fun formatSize(bytes: Long): String {
            return when {
                bytes < 1024 -> "$bytes B"
                bytes < 1024 * 1024 -> String.format("%.1f KB", bytes / 1024.0)
                bytes < 1024 * 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024))
                else -> String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024))
            }
        }
    }
    
    data class FolderInfo(
        val path: String,
        val name: String,
        val size: Long,
        val fileCount: Int,
        val isExcluded: Boolean = false
    )
    
    /**
     * Get complete storage analysis
     */
    suspend fun analyzeStorage(): StorageInfo = withContext(Dispatchers.IO) {
        val externalDir = Environment.getExternalStorageDirectory()
        val stat = StatFs(externalDir.path)
        
        val totalSpace = stat.totalBytes
        val freeSpace = stat.availableBytes
        val usedSpace = totalSpace - freeSpace
        
        StorageInfo(
            totalSpace = totalSpace,
            freeSpace = freeSpace,
            usedSpace = usedSpace,
            appCacheSize = calculateDirectorySize(context.cacheDir),
            appDataSize = calculateDirectorySize(context.filesDir),
            videosSize = calculateMediaTypeSize("video"),
            musicSize = calculateMediaTypeSize("audio"),
            thumbnailsSize = calculateThumbnailsSize(),
            downloadsSize = calculateDownloadsSize()
        )
    }
    
    /**
     * Get size of a specific directory
     */
    private fun calculateDirectorySize(directory: File): Long {
        if (!directory.exists()) return 0
        
        var size = 0L
        directory.listFiles()?.forEach { file ->
            size += if (file.isDirectory) {
                calculateDirectorySize(file)
            } else {
                file.length()
            }
        }
        return size
    }
    
    /**
     * Calculate media type size from MediaStore
     */
    private fun calculateMediaTypeSize(mediaType: String): Long {
        // This would query MediaStore for actual media sizes
        // Simplified implementation
        val zuvyDir = File(Environment.getExternalStorageDirectory(), "Zuvy/${mediaType}s")
        return if (zuvyDir.exists()) calculateDirectorySize(zuvyDir) else 0L
    }
    
    /**
     * Calculate thumbnails cache size
     */
    private fun calculateThumbnailsSize(): Long {
        val thumbDir = File(context.cacheDir, "thumbnails")
        return if (thumbDir.exists()) calculateDirectorySize(thumbDir) else 0L
    }
    
    /**
     * Calculate downloads size
     */
    private fun calculateDownloadsSize(): Long {
        val downloadsDir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "Zuvy")
        return if (downloadsDir.exists()) calculateDirectorySize(downloadsDir) else 0L
    }
    
    /**
     * Get list of large files
     */
    suspend fun getLargeFiles(minSizeMB: Int = 10): List<FolderInfo> = withContext(Dispatchers.IO) {
        val largeFiles = mutableListOf<FolderInfo>()
        val minSize = minSizeMB * 1024L * 1024L
        
        val zuvyDir = File(Environment.getExternalStorageDirectory(), "Zuvy")
        if (zuvyDir.exists()) {
            findLargeFiles(zuvyDir, minSize, largeFiles)
        }
        
        largeFiles.sortedByDescending { it.size }
    }
    
    private fun findLargeFiles(directory: File, minSize: Long, result: MutableList<FolderInfo>) {
        directory.listFiles()?.forEach { file ->
            if (file.isDirectory) {
                findLargeFiles(file, minSize, result)
            } else if (file.length() >= minSize) {
                result.add(FolderInfo(
                    path = file.absolutePath,
                    name = file.name,
                    size = file.length(),
                    fileCount = 1
                ))
            }
        }
    }
    
    /**
     * Clear app cache
     */
    suspend fun clearCache(): Long = withContext(Dispatchers.IO) {
        var clearedSize = 0L
        
        // Clear internal cache
        clearedSize += clearDirectory(context.cacheDir)
        
        // Clear external cache
        context.externalCacheDir?.let {
            clearedSize += clearDirectory(it)
        }
        
        // Clear thumbnail cache
        val thumbDir = File(context.cacheDir, "thumbnails")
        clearedSize += clearDirectory(thumbDir)
        
        clearedSize
    }
    
    /**
     * Clear thumbnails
     */
    suspend fun clearThumbnails(): Long = withContext(Dispatchers.IO) {
        val thumbDir = File(context.cacheDir, "thumbnails")
        clearDirectory(thumbDir)
    }
    
    /**
     * Clear downloads
     */
    suspend fun clearDownloads(): Long = withContext(Dispatchers.IO) {
        val downloadsDir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "Zuvy")
        clearDirectory(downloadsDir)
    }
    
    /**
     * Remove orphaned files (files no longer in database)
     */
    suspend fun removeOrphanedFiles(): Int = withContext(Dispatchers.IO) {
        // This would compare files on disk with database entries
        // Simplified implementation - remove empty directories
        var removedCount = 0
        
        val zuvyDir = File(Environment.getExternalStorageDirectory(), "Zuvy")
        if (zuvyDir.exists()) {
            removedCount = removeEmptyDirectories(zuvyDir)
        }
        
        removedCount
    }
    
    private fun clearDirectory(directory: File): Long {
        if (!directory.exists()) return 0L
        
        var clearedSize = 0L
        directory.listFiles()?.forEach { file ->
            clearedSize += if (file.isDirectory) {
                val size = clearDirectory(file)
                file.delete()
                size
            } else {
                val size = file.length()
                file.delete()
                size
            }
        }
        return clearedSize
    }
    
    private fun removeEmptyDirectories(directory: File): Int {
        var count = 0
        directory.listFiles()?.forEach { file ->
            if (file.isDirectory) {
                count += removeEmptyDirectories(file)
                if (file.listFiles()?.isEmpty() != false) {
                    file.delete()
                    count++
                }
            }
        }
        return count
    }
    
    /**
     * Get storage breakdown by folder
     */
    suspend fun getStorageBreakdown(): List<FolderInfo> = withContext(Dispatchers.IO) {
        val folders = mutableListOf<FolderInfo>()
        
        val zuvyDir = File(Environment.getExternalStorageDirectory(), "Zuvy")
        if (zuvyDir.exists()) {
            zuvyDir.listFiles()?.forEach { folder ->
                if (folder.isDirectory) {
                    folders.add(FolderInfo(
                        path = folder.absolutePath,
                        name = folder.name,
                        size = calculateDirectorySize(folder),
                        fileCount = countFiles(folder)
                    ))
                }
            }
        }
        
        folders.sortedByDescending { it.size }
    }
    
    private fun countFiles(directory: File): Int {
        var count = 0
        directory.listFiles()?.forEach { file ->
            if (file.isDirectory) {
                count += countFiles(file)
            } else {
                count++
            }
        }
        return count
    }
}
