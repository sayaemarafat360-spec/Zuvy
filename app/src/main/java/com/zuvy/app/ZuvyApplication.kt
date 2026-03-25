package com.zuvy.app

import android.app.Application
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.google.android.gms.ads.MobileAds
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.zuvy.app.notifications.NotificationEngine
import com.zuvy.app.premium.AdManager
import com.zuvy.app.scanner.AdvancedMediaScanner
import com.zuvy.app.utils.PreferenceManager
import com.zuvy.app.utils.ThemeHelper
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class ZuvyApplication : Application() {

    @Inject
    lateinit var preferenceManager: PreferenceManager
    
    @Inject
    lateinit var notificationEngine: NotificationEngine

    @Inject
    lateinit var adManager: AdManager

    @Inject
    lateinit var mediaScanner: AdvancedMediaScanner

    private val applicationScope = CoroutineScope(Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()

        // Initialize Timber for logging
        initializeTimber()

        // Initialize Firebase
        initializeFirebase()

        // Initialize AdMob using AdManager
        initializeAdMob()

        // Initialize Media Scanner
        initializeMediaScanner()

        // Create notification channels
        createNotificationChannels()

        // Apply theme
        applyTheme()
        
        // Schedule daily engagement notifications
        scheduleNotifications()
    }

    private fun initializeTimber() {
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        Timber.d("Zuvy Application initialized")
    }

    private fun initializeFirebase() {
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG)
        FirebaseAnalytics.getInstance(this).setAnalyticsCollectionEnabled(!BuildConfig.DEBUG)
    }

    private fun initializeAdMob() {
        // Initialize AdManager which handles AdMob SDK initialization
        adManager.initialize()
    }

    private fun initializeMediaScanner() {
        // Initialize the advanced media scanner for real-time monitoring
        applicationScope.launch {
            mediaScanner.initialize()
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NotificationManager::class.java)

            val musicChannel = NotificationChannel(
                CHANNEL_MUSIC_PLAYBACK,
                getString(R.string.music_playback),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.music_playback_description)
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }

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
    
    private fun scheduleNotifications() {
        applicationScope.launch {
            try {
                notificationEngine.scheduleDailyNotifications()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    companion object {
        const val CHANNEL_MUSIC_PLAYBACK = "music_playback"
        const val CHANNEL_DOWNLOAD = "downloads"
    }
}
