package com.zuvy.app

import android.app.Application
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.google.android.gms.ads.MobileAds
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.zuvy.app.utils.PreferenceManager
import com.zuvy.app.utils.ThemeHelper
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class ZuvyApplication : Application() {

    @Inject
    lateinit var preferenceManager: PreferenceManager

    private val applicationScope = CoroutineScope(Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()

        // Initialize Firebase
        initializeFirebase()

        // Initialize AdMob
        initializeAdMob()

        // Create notification channels
        createNotificationChannels()

        // Apply theme
        applyTheme()
    }

    private fun initializeFirebase() {
        // Enable Crashlytics in release builds
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG)

        // Enable Analytics
        FirebaseAnalytics.getInstance(this).setAnalyticsCollectionEnabled(!BuildConfig.DEBUG)
    }

    private fun initializeAdMob() {
        applicationScope.launch {
            try {
                MobileAds.initialize(this@ZuvyApplication) {
                    // AdMob initialized
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NotificationManager::class.java)

            // Music Playback Channel
            val musicChannel = NotificationChannel(
                CHANNEL_MUSIC_PLAYBACK,
                getString(R.string.music_playback),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.music_playback_description)
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }

            // Download Channel
            val downloadChannel = NotificationChannel(
                CHANNEL_DOWNLOAD,
                getString(R.string.downloads),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.download_description)
                setShowBadge(true)
            }

            notificationManager.createNotificationChannels(listOf(musicChannel, downloadChannel))
        }
    }

    private fun applyTheme() {
        val themeMode = preferenceManager.getThemeMode()
        ThemeHelper.applyTheme(themeMode)
    }

    companion object {
        const val CHANNEL_MUSIC_PLAYBACK = "music_playback"
        const val CHANNEL_DOWNLOAD = "downloads"
    }
}
