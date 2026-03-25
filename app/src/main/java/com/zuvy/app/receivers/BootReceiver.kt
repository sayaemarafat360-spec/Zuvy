package com.zuvy.app.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.zuvy.app.notifications.NotificationEngine
import com.zuvy.app.notifications.NotificationType
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var notificationEngine: NotificationEngine

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("BootReceiver", "Device booted, scheduling notifications")
            try {
                notificationEngine.scheduleDailyNotifications()
            } catch (e: Exception) {
                Log.e("BootReceiver", "Failed to schedule notifications", e)
            }
        }
    }
}
