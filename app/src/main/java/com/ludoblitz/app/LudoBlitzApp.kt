package com.ludoblitz.app

import android.app.Application
import android.media.AudioAttributes
import android.media.SoundPool
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.gms.ads.MobileAds
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.ludoblitz.app.data.local.PreferenceManager
import com.ludoblitz.app.utils.AdPreloadManager
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Main Application class for Ludo Blitz
 * Handles initialization of Firebase, AdMob, and other core components
 */
@HiltAndroidApp
class LudoBlitzApp : Application() {

    @Inject
    lateinit var preferenceManager: PreferenceManager

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    // Sound Pool for game sounds
    lateinit var soundPool: SoundPool
    private val soundMap = mutableMapOf<String, Int>()

    companion object {
        lateinit var instance: LudoBlitzApp
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        
        // Initialize Firebase
        FirebaseApp.initializeApp(this)
        setupFirestore()
        
        // Initialize AdMob
        applicationScope.launch {
            MobileAds.initialize(this@LudoBlitzApp) {
                // Preload ads when app starts
                AdPreloadManager.getInstance().preloadAds(this@LudoBlitzApp)
            }
        }
        
        // Setup theme based on user preference
        setupTheme()
        
        // Initialize Sound Pool
        setupSoundPool()
        
        // Check and handle pending invites
        checkPendingInvites()
    }

    private fun setupFirestore() {
        val settings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true)
            .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
            .build()
        FirebaseFirestore.getInstance().firestoreSettings = settings
    }

    private fun setupTheme() {
        applicationScope.launch {
            val isDarkMode = preferenceManager.isDarkMode()
            val mode = if (isDarkMode) {
                AppCompatDelegate.MODE_NIGHT_YES
            } else {
                AppCompatDelegate.MODE_NIGHT_NO
            }
            AppCompatDelegate.setDefaultNightMode(mode)
        }
    }

    private fun setupSoundPool() {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(10)
            .setAudioAttributes(audioAttributes)
            .build()

        // Load sounds
        loadSounds()
    }

    private fun loadSounds() {
        // These will be loaded from raw resources
        try {
            soundMap["dice_roll"] = soundPool.load(this, R.raw.dice_roll, 1)
            soundMap["token_move"] = soundPool.load(this, R.raw.token_move, 1)
            soundMap["token_kill"] = soundPool.load(this, R.raw.token_kill, 1)
            soundMap["token_safe"] = soundPool.load(this, R.raw.token_safe, 1)
            soundMap["victory"] = soundPool.load(this, R.raw.victory, 1)
            soundMap["lose"] = soundPool.load(this, R.raw.lose, 1)
            soundMap["button_click"] = soundPool.load(this, R.raw.button_click, 1)
            soundMap["coin_collect"] = soundPool.load(this, R.raw.coin_collect, 1)
            soundMap["six_rolled"] = soundPool.load(this, R.raw.six_rolled, 1)
            soundMap["token_home"] = soundPool.load(this, R.raw.token_home, 1)
        } catch (e: Exception) {
            // Sounds will be loaded later
        }
    }

    fun playSound(soundName: String, volume: Float = 1.0f) {
        if (!preferenceManager.isSoundEnabled()) return
        
        soundMap[soundName]?.let { soundId ->
            soundPool.play(soundId, volume, volume, 1, 0, 1.0f)
        }
    }

    private fun checkPendingInvites() {
        // Check for pending game invites when app starts
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            applicationScope.launch {
                // Check pending invites logic will be implemented
            }
        }
    }

    fun isOnline(): Boolean {
        val connectivityManager = getSystemService(CONNECTIVITY_SERVICE) 
            as android.net.ConnectivityManager
        val network = connectivityManager.activeNetworkInfo
        return network != null && network.isConnected
    }
}
