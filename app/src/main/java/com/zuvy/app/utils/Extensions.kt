package com.zuvy.app.utils

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.zuvy.app.R

fun Long.formatDuration(): String {
    val seconds = (this / 1000) % 60
    val minutes = (this / (1000 * 60)) % 60
    val hours = this / (1000 * 60 * 60)

    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%d:%02d", minutes, seconds)
    }
}

fun Long.formatFileSize(): String {
    if (this < 1024) return "$this B"
    val kb = this / 1024.0
    if (kb < 1024) return "%.1f KB".format(kb)
    val mb = kb / 1024.0
    if (mb < 1024) return "%.1f MB".format(mb)
    val gb = mb / 1024.0
    return "%.1f GB".format(gb)
}

fun AppCompatActivity.showRateAppDialog() {
    com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
        .setTitle(R.string.rate_title)
        .setMessage(R.string.rate_message)
        .setPositiveButton(R.string.rate_now) { dialog, _ ->
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName"))
                startActivity(intent)
            } catch (e: Exception) {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName"))
                startActivity(intent)
            }
            dialog.dismiss()
        }
        .setNegativeButton(R.string.later) { dialog, _ ->
            dialog.dismiss()
        }
        .setNeutralButton(R.string.never) { dialog, _ ->
            dialog.dismiss()
        }
        .show()
}

object ThemeHelper {
    fun applyTheme(themeMode: Int) {
        when (themeMode) {
            0 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            1 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            2 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }
}
