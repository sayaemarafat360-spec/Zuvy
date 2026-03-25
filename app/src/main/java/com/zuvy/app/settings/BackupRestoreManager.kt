package com.zuvy.app.settings

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Backup & Restore Manager for app data
 */
class BackupRestoreManager(private val context: Context) {
    
    companion object {
        const val TAG = "BackupRestoreManager"
        const val BACKUP_VERSION = 1
        const val BACKUP_FILE_NAME = "zuvy_backup.zip"
    }
    
    private val gson = Gson()
    
    data class BackupData(
        val version: Int = BACKUP_VERSION,
        val timestamp: Long = System.currentTimeMillis(),
        val playlists: List<PlaylistBackup> = emptyList(),
        val favorites: List<String> = emptyList(),
        val history: List<String> = emptyList(),
        val settings: Map<String, Any> = emptyMap(),
        val equalizerPresets: List<EqualizerPresetBackup> = emptyList()
    )
    
    data class PlaylistBackup(
        val id: Long,
        val name: String,
        val createdAt: Long,
        val songs: List<String> // URIs
    )
    
    data class EqualizerPresetBackup(
        val name: String,
        val bands: ShortArray,
        val bassBoost: Int,
        val virtualizer: Int
    )
    
    /**
     * Create backup file
     */
    suspend fun createBackup(
        playlists: List<PlaylistBackup>,
        favorites: List<String>,
        history: List<String>,
        settings: Map<String, Any>,
        equalizerPresets: List<EqualizerPresetBackup>
    ): File = withContext(Dispatchers.IO) {
        val backupData = BackupData(
            version = BACKUP_VERSION,
            timestamp = System.currentTimeMillis(),
            playlists = playlists,
            favorites = favorites,
            history = history,
            settings = settings,
            equalizerPresets = equalizerPresets
        )
        
        val backupFile = File(context.cacheDir, BACKUP_FILE_NAME)
        
        ZipOutputStream(FileOutputStream(backupFile)).use { zipOut ->
            // Add backup data JSON
            val jsonBytes = gson.toJson(backupData).toByteArray(Charsets.UTF_8)
            zipOut.putNextEntry(ZipEntry("backup.json"))
            zipOut.write(jsonBytes)
            zipOut.closeEntry()
            
            // Add database file (optional)
            val dbFile = context.getDatabasePath("zuvy_database")
            if (dbFile.exists()) {
                zipOut.putNextEntry(ZipEntry("database.db"))
                FileInputStream(dbFile).use { input ->
                    input.copyTo(zipOut)
                }
                zipOut.closeEntry()
            }
        }
        
        backupFile
    }
    
    /**
     * Restore from backup file
     */
    suspend fun restoreBackup(backupUri: Uri): BackupData? = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(backupUri)?.use { input ->
                ZipInputStream(input).use { zipIn ->
                    var backupData: BackupData? = null
                    
                    var entry = zipIn.nextEntry
                    while (entry != null) {
                        when (entry.name) {
                            "backup.json" -> {
                                val json = zipIn.readBytes().toString(Charsets.UTF_8)
                                backupData = gson.fromJson(json, BackupData::class.java)
                            }
                            "database.db" -> {
                                // Restore database
                                val dbFile = context.getDatabasePath("zuvy_database")
                                dbFile.parentFile?.mkdirs()
                                FileOutputStream(dbFile).use { output ->
                                    zipIn.copyTo(output)
                                }
                            }
                        }
                        zipIn.closeEntry()
                        entry = zipIn.nextEntry
                    }
                    
                    backupData
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to restore backup", e)
            null
        }
    }
    
    /**
     * Export backup to external storage
     */
    suspend fun exportBackupToStorage(backupFile: File, destinationUri: Uri): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                context.contentResolver.openOutputStream(destinationUri)?.use { output ->
                    FileInputStream(backupFile).use { input ->
                        input.copyTo(output)
                    }
                }
                true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to export backup", e)
                false
            }
        }
    }
    
    /**
     * Get backup info without full restore
     */
    suspend fun getBackupInfo(backupUri: Uri): BackupInfo? = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(backupUri)?.use { input ->
                ZipInputStream(input).use { zipIn ->
                    var entry = zipIn.nextEntry
                    while (entry != null) {
                        if (entry.name == "backup.json") {
                            val json = zipIn.readBytes().toString(Charsets.UTF_8)
                            val data = gson.fromJson(json, BackupData::class.java)
                            return@withContext BackupInfo(
                                version = data.version,
                                timestamp = data.timestamp,
                                playlistCount = data.playlists.size,
                                favoriteCount = data.favorites.size,
                                historyCount = data.history.size,
                                hasSettings = data.settings.isNotEmpty()
                            )
                        }
                        zipIn.closeEntry()
                        entry = zipIn.nextEntry
                    }
                }
            }
            null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get backup info", e)
            null
        }
    }
    
    data class BackupInfo(
        val version: Int,
        val timestamp: Long,
        val playlistCount: Int,
        val favoriteCount: Int,
        val historyCount: Int,
        val hasSettings: Boolean
    ) {
        val formattedDate: String
            get() {
                val sdf = java.text.SimpleDateFormat("MMM dd, yyyy HH:mm", java.util.Locale.getDefault())
                return sdf.format(java.util.Date(timestamp))
            }
        
        val isCompatible: Boolean
            get() = version <= BACKUP_VERSION
    }
    
    /**
     * Clear all app data
     */
    suspend fun clearAllData(): Boolean = withContext(Dispatchers.IO) {
        try {
            // Clear databases
            context.databaseList().forEach { dbName ->
                context.deleteDatabase(dbName)
            }
            
            // Clear shared preferences
            val prefsDir = File(context.filesDir.parentFile, "shared_prefs")
            prefsDir.listFiles()?.forEach { it.delete() }
            
            // Clear cache
            context.cacheDir.deleteRecursively()
            
            // Clear files
            context.filesDir.deleteRecursively()
            
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear all data", e)
            false
        }
    }
    
    /**
     * Get backup file name with timestamp
     */
    fun getBackupFileName(): String {
        val sdf = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault())
        return "zuvy_backup_${sdf.format(java.util.Date())}.zip"
    }
}
