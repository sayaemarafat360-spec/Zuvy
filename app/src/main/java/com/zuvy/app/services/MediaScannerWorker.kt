package com.zuvy.app.services

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.zuvy.app.R
import com.zuvy.app.data.local.AppDatabase
import com.zuvy.app.data.local.entity.HistoryEntity
import com.zuvy.app.utils.ToastUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit

class MediaScannerWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            Log.d("MediaScanner", "Starting media scan...")
            scanMedia()
            Result.success()
        } catch (e: Exception) {
            Log.e("MediaScanner", "Scan failed", e)
            Result.retry()
        }
    }

    private suspend fun scanMedia() {
        withContext(Dispatchers.IO) {
            var newVideos = 0
            var newMusic = 0

            // Scan videos
            val videoProjection = arrayOf(
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.DISPLAY_NAME,
                MediaStore.Video.Media.DATA,
                MediaStore.Video.Media.SIZE,
                MediaStore.Video.Media.DURATION
            )

            applicationContext.contentResolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                videoProjection,
                null,
                null,
                "${MediaStore.Video.Media.DATE_ADDED} DESC"
            )?.use { cursor ->
                newVideos = cursor.count
            }

            // Scan music
            val musicProjection = arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.DATA
            )

            applicationContext.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                musicProjection,
                "${MediaStore.Audio.Media.IS_MUSIC} = 1",
                null,
                "${MediaStore.Audio.Media.DATE_ADDED} DESC"
            )?.use { cursor ->
                newMusic = cursor.count
            }

            Log.d("MediaScanner", "Scan complete: $newVideos videos, $newMusic songs")
        }
    }

    companion object {
        private const val WORK_NAME = "media_scanner_work"

        fun schedule(context: Context) {
            val workRequest = PeriodicWorkRequestBuilder<MediaScannerWorker>(
                6, TimeUnit.HOURS
            ).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        }

        fun scanNow(context: Context) {
            val workRequest = PeriodicWorkRequestBuilder<MediaScannerWorker>(
                15, TimeUnit.MINUTES
            ).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME + "_now",
                ExistingPeriodicWorkPolicy.REPLACE,
                workRequest
            )
        }
    }
}
