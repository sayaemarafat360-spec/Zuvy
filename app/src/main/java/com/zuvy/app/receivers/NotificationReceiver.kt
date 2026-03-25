package com.zuvy.app.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.zuvy.app.notifications.NotificationType

class NotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        context ?: return
        
        val title = intent?.getStringExtra("title") ?: return
        val message = intent.getStringExtra("message") ?: return
        val typeString = intent.getStringExtra("type") ?: "ENGAGEMENT"
        val notificationId = intent.getIntExtra("notification_id", 1000)
        
        val type = try {
            NotificationType.valueOf(typeString)
        } catch (e: Exception) {
            NotificationType.ENGAGEMENT
        }
        
        // Create notification using NotificationCompat directly
        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            
            val notification = androidx.core.app.NotificationCompat.Builder(context, getChannelId(type))
                .setSmallIcon(com.zuvy.app.R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(androidx.core.app.NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
                .setCategory(androidx.core.app.NotificationCompat.CATEGORY_RECOMMENDATION)
                .setAutoCancel(true)
                .setSound(android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION))
                .setVibrate(longArrayOf(0, 250, 250, 250))
                .build()
            
            notificationManager.notify(notificationId, notification)
            Log.d("NotificationReceiver", "Notification shown: $title")
        } catch (e: Exception) {
            Log.e("NotificationReceiver", "Failed to show notification", e)
        }
    }
    
    private fun getChannelId(type: NotificationType): String {
        return when (type) {
            NotificationType.ENGAGEMENT -> NotificationEngine.CHANNEL_ENGAGEMENT
            NotificationType.FEATURE -> NotificationEngine.CHANNEL_FEATURES
            NotificationType.REMINDER -> NotificationEngine.CHANNEL_REMINDERS
            NotificationType.MOOD -> NotificationEngine.CHANNEL_MOOD
        }
    }
}
