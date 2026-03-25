package com.zuvy.app.notifications

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.zuvy.app.R
import com.zuvy.app.receivers.NotificationReceiver
import java.util.Calendar
import java.util.Random
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationEngine @Inject constructor(
    private val context: Context
) {
    companion object {
        const val CHANNEL_ENGAGEMENT = "engagement"
        const val CHANNEL_FEATURES = "features"
        const val CHANNEL_REMINDERS = "reminders"
        const val CHANNEL_MOOD = "mood"
        const val NOTIFICATION_ID_PREFIX = 1000
        
        val ENGAGEMENT_NOTIFICATIONS = listOf(
            NotificationContent("🎵 Time for Music!", "Your favorite tracks are waiting. Discover new music now!", NotificationType.ENGAGEMENT),
            NotificationContent("🎬 Video Marathon?", "You have unwatched videos in your playlist. Continue watching!", NotificationType.ENGAGEMENT),
            NotificationContent("🔥 Hot Picks Today", "Trending videos are here! See what everyone's watching.", NotificationType.ENGAGEMENT),
            NotificationContent("⏰ Music Break Time", "Take a 5-minute music break. Your ears deserve it!", NotificationType.ENGAGEMENT),
            NotificationContent("🎧 Your Playlist Misses You", "It's been a while. Come back and enjoy your tunes!", NotificationType.ENGAGEMENT),
            NotificationContent("🌙 Night Owl Mode", "Perfect time for some relaxing music before sleep.", NotificationType.ENGAGEMENT),
            NotificationContent("☀️ Good Morning!", "Start your day with energizing music. Rise and shine!", NotificationType.ENGAGEMENT),
            NotificationContent("🚗 Commute Time?", "Make your journey musical! Play your road trip playlist.", NotificationType.ENGAGEMENT),
            NotificationContent("💪 Workout Buddy", "Your workout playlist is ready. Let's get moving!", NotificationType.ENGAGEMENT),
            NotificationContent("📚 Study Session", "Focus music for productive study sessions. Let's go!", NotificationType.ENGAGEMENT),
            NotificationContent("🌟 Weekly Top 10", "Check out this week's most played tracks!", NotificationType.ENGAGEMENT),
            NotificationContent("🎤 Sing Along", "Karaoke time! Your favorite lyrics are waiting.", NotificationType.ENGAGEMENT),
            NotificationContent("🎸 Rock Out!", "Time for some guitar riffs and drum solos!", NotificationType.ENGAGEMENT),
            NotificationContent("🎹 Piano Moments", "Relax with beautiful piano melodies.", NotificationType.ENGAGEMENT),
            NotificationContent("🎭 Mix It Up", "Try our smart shuffle for a fresh experience!", NotificationType.ENGAGEMENT)
        )
        
        val FEATURE_NOTIFICATIONS = listOf(
            NotificationContent("✨ New Feature: Equalizer", "Customize your sound! Try our new audio equalizer now.", NotificationType.FEATURE),
            NotificationContent("🌙 Sleep Timer Added", "Fall asleep to music! Set a sleep timer for auto-stop.", NotificationType.FEATURE),
            NotificationContent("🎨 Dynamic Themes", "Your player now adapts colors to your music art!", NotificationType.FEATURE),
            NotificationContent("🔄 Background Play", "Keep music playing even when screen is off!", NotificationType.FEATURE),
            NotificationContent("📱 Mini Player", "Control playback from anywhere with mini player!", NotificationType.FEATURE),
            NotificationContent("⭐ Favorites Synced", "Your favorites are safe! They sync across devices.", NotificationType.FEATURE),
            NotificationContent("🎯 Smart Playlists", "Auto-generated playlists based on your taste!", NotificationType.FEATURE),
            NotificationContent("🎵 Lyrics Added", "Sing along! Now showing lyrics for most songs.", NotificationType.FEATURE)
        )
        
        val MOOD_NOTIFICATIONS = listOf(
            NotificationContent("😊 Feeling Happy?", "We've got the perfect upbeat playlist for your mood!", NotificationType.MOOD),
            NotificationContent("😢 Need Comfort?", "Let soothing music heal your heart. We're here for you.", NotificationType.MOOD),
            NotificationContent("🥳 Party Mode!", "Turn up the volume! Party playlist is ready to rock.", NotificationType.MOOD),
            NotificationContent("😌 Zen Mode", "Relax and unwind with our calm meditation sounds.", NotificationType.MOOD),
            NotificationContent("💘 Romantic Vibes", "Love is in the air! Play romantic melodies now.", NotificationType.MOOD),
            NotificationContent("😤 Stress Relief", "Let music wash away your stress. Take a breather.", NotificationType.MOOD),
            NotificationContent("🎯 Focus Mode", "Deep work needs deep focus. Ambient sounds ready!", NotificationType.MOOD),
            NotificationContent("🎉 Celebration Time!", "Every moment is worth celebrating. Play joy!", NotificationType.MOOD),
            NotificationContent("🌈 Rainbow Vibes", "Colorful tunes for a colorful day!", NotificationType.MOOD),
            NotificationContent("🌊 Ocean Waves", "Relax with nature sounds and peaceful melodies.", NotificationType.MOOD)
        )
        
        val REMINDER_NOTIFICATIONS = listOf(
            NotificationContent("🔔 Don't Forget!", "You haven't checked your playlists today. New hits waiting!", NotificationType.REMINDER),
            NotificationContent("📥 Downloaded Ready", "Your downloaded media is ready for offline enjoyment!", NotificationType.REMINDER),
            NotificationContent("⏱️ Watch Later Reminder", "Videos in Watch Later are waiting for you!", NotificationType.REMINDER),
            NotificationContent("🆕 Fresh Media Detected", "New media found on your device. Scan complete!", NotificationType.REMINDER),
            NotificationContent("💾 Storage Space", "Clean up old downloads? Free up space for new media!", NotificationType.REMINDER),
            NotificationContent("📊 Weekly Stats", "Your listening report is ready! See your top tracks.", NotificationType.REMINDER),
            NotificationContent("🏆 Achievement Unlocked", "You've listened to 100 songs! Music lover badge earned.", NotificationType.REMINDER)
        )
    }
    
    private val notificationManager = NotificationManagerCompat.from(context)
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val random = Random()
    
    init {
        createNotificationChannels()
    }
    
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val defaultSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            
            val channels = listOf(
                createChannel(CHANNEL_ENGAGEMENT, "Engagement", "Stay connected with your media", NotificationManager.IMPORTANCE_HIGH, defaultSound),
                createChannel(CHANNEL_FEATURES, "Features", "New features and updates", NotificationManager.IMPORTANCE_DEFAULT, defaultSound),
                createChannel(CHANNEL_REMINDERS, "Reminders", "Important reminders", NotificationManager.IMPORTANCE_HIGH, defaultSound),
                createChannel(CHANNEL_MOOD, "Mood & Vibes", "Personalized mood-based suggestions", NotificationManager.IMPORTANCE_HIGH, defaultSound)
            )
            
            val manager = context.getSystemService(NotificationManager::class.java)
            channels.forEach { manager.createNotificationChannel(it) }
        }
    }
    
    private fun createChannel(id: String, name: String, description: String, importance: Int, soundUri: android.net.Uri): NotificationChannel {
        return NotificationChannel(id, name, importance).apply {
            this.description = description
            enableLights(true)
            enableVibration(true)
            setSound(soundUri, AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION).build())
        }
    }
    
    fun scheduleDailyNotifications() {
        val notifications = mutableListOf<NotificationContent>().apply {
            addAll(ENGAGEMENT_NOTIFICATIONS.shuffled().take(15))
            addAll(FEATURE_NOTIFICATIONS.shuffled().take(8))
            addAll(MOOD_NOTIFICATIONS.shuffled().take(10))
            addAll(REMINDER_NOTIFICATIONS.shuffled().take(7))
        }.shuffled()
        
        val startHour = 7
        val activeHours = 16
        
        notifications.take(40).forEachIndexed { index, content ->
            val triggerTime = calculateRandomTime(index, notifications.size, startHour, activeHours)
            scheduleNotification(content, triggerTime, index)
        }
    }
    
    private fun calculateRandomTime(index: Int, total: Int, startHour: Int, activeHours: Int): Long {
        val now = Calendar.getInstance()
        val minutesPerNotification = (activeHours * 60) / total
        val baseMinute = index * minutesPerNotification
        val randomOffset = random.nextInt(30) - 15
        val finalMinute = (baseMinute + randomOffset).coerceIn(0, activeHours * 60 - 1)
        
        now.set(Calendar.HOUR_OF_DAY, startHour + (finalMinute / 60))
        now.set(Calendar.MINUTE, finalMinute % 60)
        now.set(Calendar.SECOND, 0)
        
        if (now.timeInMillis <= System.currentTimeMillis()) {
            now.add(Calendar.DAY_OF_MONTH, 1)
        }
        
        return now.timeInMillis
    }
    
    private fun scheduleNotification(content: NotificationContent, triggerTime: Long, id: Int) {
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra("title", content.title)
            putExtra("message", content.message)
            putExtra("type", content.type.name)
            putExtra("notification_id", NOTIFICATION_ID_PREFIX + id)
        }
        
        val pendingIntent = PendingIntent.getBroadcast(
            context, NOTIFICATION_ID_PREFIX + id, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        try {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        } catch (e: Exception) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        }
    }
    
    fun showNotification(title: String, message: String, type: NotificationType, notificationId: Int = random.nextInt(10000)) {
        val channelId = when (type) {
            NotificationType.ENGAGEMENT -> CHANNEL_ENGAGEMENT
            NotificationType.FEATURE -> CHANNEL_FEATURES
            NotificationType.REMINDER -> CHANNEL_REMINDERS
            NotificationType.MOOD -> CHANNEL_MOOD
        }
        
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_RECOMMENDATION)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
            .setVibrate(longArrayOf(0, 250, 250, 250))
            .build()
        
        try {
            notificationManager.notify(notificationId, notification)
        } catch (e: Exception) { }
    }
    
    fun sendImmediateEngagement() {
        val notification = ENGAGEMENT_NOTIFICATIONS.random()
        showNotification(notification.title, notification.message, NotificationType.ENGAGEMENT)
    }
    
    fun cancelAllNotifications() {
        notificationManager.cancelAll()
        for (i in 0..50) {
            val intent = Intent(context, NotificationReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(context, NOTIFICATION_ID_PREFIX + i, intent, PendingIntent.FLAG_IMMUTABLE)
            alarmManager.cancel(pendingIntent)
        }
    }
}

data class NotificationContent(val title: String, val message: String, val type: NotificationType)
enum class NotificationType { ENGAGEMENT, FEATURE, REMINDER, MOOD }
