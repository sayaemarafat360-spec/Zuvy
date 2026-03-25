package com.zuvy.app.utils

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.zuvy.app.R
import java.io.File

/**
 * Utility class for setting audio files as ringtones, notifications, or alarms
 */
object RingtoneUtils {

    /**
     * Check if the app has permission to write settings
     */
    fun hasWriteSettingsPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.System.canWrite(context)
        } else {
            true
        }
    }

    /**
     * Get intent to request write settings permission
     */
    fun getWriteSettingsIntent(): Intent {
        return Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
            data = Uri.parse("package:com.zuvy.app")
        }
    }

    /**
     * Set a file as the default ringtone
     * @param context Context
     * @param fileUri The URI of the audio file
     * @param title The title to show for the ringtone
     * @return true if successful, false otherwise
     */
    fun setAsRingtone(context: Context, fileUri: Uri, title: String): Boolean {
        return try {
            // Add file to MediaStore audio
            val values = ContentValues().apply {
                put(MediaStore.Audio.Media.TITLE, title)
                put(MediaStore.Audio.Media.MIME_TYPE, "audio/mpeg")
                put(MediaStore.Audio.Media.IS_RINGTONE, true)
                put(MediaStore.Audio.Media.IS_NOTIFICATION, false)
                put(MediaStore.Audio.Media.IS_ALARM, false)
                put(MediaStore.Audio.Media.IS_MUSIC, false)
            }

            // Set as default ringtone
            val ringtoneUri = insertRingtone(context, fileUri, values)
            if (ringtoneUri != null) {
                RingtoneManager.setActualDefaultRingtoneUri(
                    context,
                    RingtoneManager.TYPE_RINGTONE,
                    ringtoneUri
                )
                ToastUtils.showSuccess(context, "Ringtone set: $title 🎵")
                true
            } else {
                ToastUtils.showError(context, "Failed to set ringtone")
                false
            }
        } catch (e: Exception) {
            ToastUtils.showError(context, "Error: ${e.message}")
            false
        }
    }

    /**
     * Set a file as notification sound
     */
    fun setAsNotification(context: Context, fileUri: Uri, title: String): Boolean {
        return try {
            val values = ContentValues().apply {
                put(MediaStore.Audio.Media.TITLE, title)
                put(MediaStore.Audio.Media.MIME_TYPE, "audio/mpeg")
                put(MediaStore.Audio.Media.IS_RINGTONE, false)
                put(MediaStore.Audio.Media.IS_NOTIFICATION, true)
                put(MediaStore.Audio.Media.IS_ALARM, false)
                put(MediaStore.Audio.Media.IS_MUSIC, false)
            }

            val ringtoneUri = insertRingtone(context, fileUri, values)
            if (ringtoneUri != null) {
                RingtoneManager.setActualDefaultRingtoneUri(
                    context,
                    RingtoneManager.TYPE_NOTIFICATION,
                    ringtoneUri
                )
                ToastUtils.showSuccess(context, "Notification sound set: $title 🔔")
                true
            } else {
                ToastUtils.showError(context, "Failed to set notification sound")
                false
            }
        } catch (e: Exception) {
            ToastUtils.showError(context, "Error: ${e.message}")
            false
        }
    }

    /**
     * Set a file as alarm sound
     */
    fun setAsAlarm(context: Context, fileUri: Uri, title: String): Boolean {
        return try {
            val values = ContentValues().apply {
                put(MediaStore.Audio.Media.TITLE, title)
                put(MediaStore.Audio.Media.MIME_TYPE, "audio/mpeg")
                put(MediaStore.Audio.Media.IS_RINGTONE, false)
                put(MediaStore.Audio.Media.IS_NOTIFICATION, false)
                put(MediaStore.Audio.Media.IS_ALARM, true)
                put(MediaStore.Audio.Media.IS_MUSIC, false)
            }

            val ringtoneUri = insertRingtone(context, fileUri, values)
            if (ringtoneUri != null) {
                RingtoneManager.setActualDefaultRingtoneUri(
                    context,
                    RingtoneManager.TYPE_ALARM,
                    ringtoneUri
                )
                ToastUtils.showSuccess(context, "Alarm sound set: $title ⏰")
                true
            } else {
                ToastUtils.showError(context, "Failed to set alarm sound")
                false
            }
        } catch (e: Exception) {
            ToastUtils.showError(context, "Error: ${e.message}")
            false
        }
    }

    /**
     * Show dialog to choose ringtone type
     */
    fun showRingtoneTypeDialog(
        context: Context,
        fileUri: Uri,
        title: String,
        fragmentManager: androidx.fragment.app.FragmentManager
    ) {
        val options = arrayOf(
            "Ringtone",
            "Notification Sound",
            "Alarm Sound"
        )

        com.google.android.material.dialog.MaterialAlertDialogBuilder(context)
            .setTitle("Set as...")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> setAsRingtone(context, fileUri, title)
                    1 -> setAsNotification(context, fileUri, title)
                    2 -> setAsAlarm(context, fileUri, title)
                }
            }
            .show()
    }

    private fun insertRingtone(context: Context, fileUri: Uri, values: ContentValues): Uri? {
        return try {
            context.contentResolver.insert(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                values
            )
        } catch (e: Exception) {
            null
        }
    }
}
