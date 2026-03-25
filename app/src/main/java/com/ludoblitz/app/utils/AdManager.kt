package com.ludoblitz.app.utils

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.ludoblitz.app.BuildConfig
import com.ludoblitz.app.LudoBlitzApp
import com.ludoblitz.app.data.local.PreferenceManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentLinkedQueue
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Smart Ad Manager with offline caching strategy
 * Handles AdMob integration with preloading for offline scenarios
 */
@Singleton
class AdManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferenceManager: PreferenceManager
) {

    companion object {
        private const val TAG = "AdManager"
        private const val MIN_AD_INTERVAL_MS = 60000L // 1 minute between ads
        private const val MAX_CACHED_ADS = 3
        private const val PRELOAD_DELAY_MS = 2000L
        
        // Test Ad IDs (Replace with real IDs in production)
        private const val INTERSTITIAL_AD_ID = BuildConfig.ADMOB_INTERSTITIAL_ID
        private const val REWARDED_AD_ID = BuildConfig.ADMOB_REWARDED_ID
    }

    // Cached ads queues
    private val cachedInterstitialAds = ConcurrentLinkedQueue<InterstitialAd>()
    private val cachedRewardedAds = ConcurrentLinkedQueue<RewardedAd>()
    
    // Current showing ads
    private var currentInterstitialAd: InterstitialAd? = null
    private var currentRewardedAd: RewardedAd? = null
    
    // Ad loading state
    private var isLoadingInterstitial = false
    private var isLoadingRewarded = false
    
    // Callbacks
    private var onRewardedCallback: (() -> Unit)? = null
    private var onAdClosedCallback: (() -> Unit)? = null
    
    private val scope = CoroutineScope(Dispatchers.Main)

    /**
     * Initialize the ad manager and start preloading
     */
    fun initialize() {
        MobileAds.initialize(context) {
            Log.d(TAG, "MobileAds initialized")
            startPreloadingAds()
        }
    }

    /**
     * Start preloading ads in background
     */
    private fun startPreloadingAds() {
        scope.launch {
            // Preload interstitial ads
            repeat(MAX_CACHED_ADS) {
                preloadInterstitialAd()
                Thread.sleep(PRELOAD_DELAY_MS)
            }
            
            // Preload rewarded ads
            repeat(MAX_CACHED_ADS) {
                preloadRewardedAd()
                Thread.sleep(PRELOAD_DELAY_MS)
            }
        }
    }

    /**
     * Check if should show ad (respects frequency capping)
     */
    private suspend fun shouldShowAd(): Boolean {
        // Premium users don't see ads
        if (preferenceManager.isPremiumUser.flow.value) {
            return false
        }
        
        // Check minimum interval between ads
        val lastAdTime = preferenceManager.lastAdShown.flow.value
        val currentTime = System.currentTimeMillis()
        
        return (currentTime - lastAdTime) >= MIN_AD_INTERVAL_MS
    }

    /**
     * Preload an interstitial ad
     */
    private fun preloadInterstitialAd() {
        if (isLoadingInterstitial || cachedInterstitialAds.size >= MAX_CACHED_ADS) {
            return
        }
        
        if (!isOnline()) {
            Log.d(TAG, "Device offline, skipping interstitial preload")
            return
        }
        
        isLoadingInterstitial = true
        
        InterstitialAd.load(
            context,
            INTERSTITIAL_AD_ID,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.e(TAG, "Interstitial ad failed to load: ${error.message}")
                    isLoadingInterstitial = false
                }
                
                override fun onAdLoaded(ad: InterstitialAd) {
                    Log.d(TAG, "Interstitial ad loaded and cached")
                    cachedInterstitialAds.add(ad)
                    isLoadingInterstitial = false
                }
            }
        )
    }

    /**
     * Preload a rewarded ad
     */
    private fun preloadRewardedAd() {
        if (isLoadingRewarded || cachedRewardedAds.size >= MAX_CACHED_ADS) {
            return
        }
        
        if (!isOnline()) {
            Log.d(TAG, "Device offline, skipping rewarded preload")
            return
        }
        
        isLoadingRewarded = true
        
        RewardedAd.load(
            context,
            REWARDED_AD_ID,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.e(TAG, "Rewarded ad failed to load: ${error.message}")
                    isLoadingRewarded = false
                }
                
                override fun onAdLoaded(ad: RewardedAd) {
                    Log.d(TAG, "Rewarded ad loaded and cached")
                    cachedRewardedAds.add(ad)
                    isLoadingRewarded = false
                }
            }
        )
    }

    /**
     * Show interstitial ad with smart caching
     */
    suspend fun showInterstitialAd(
        activity: Activity,
        onAdShown: () -> Unit = {},
        onAdFailed: () -> Unit = {},
        onAdClosed: () -> Unit = {}
    ) {
        if (!shouldShowAd()) {
            onAdFailed()
            return
        }
        
        // Get cached ad or try to load
        val ad = cachedInterstitialAds.poll() ?: run {
            // No cached ad, try to load one
            if (isOnline()) {
                loadInterstitialAdForShow(activity, onAdShown, onAdFailed, onAdClosed)
            } else {
                Log.d(TAG, "No cached ad and offline")
                onAdFailed()
            }
            return
        }
        
        showInterstitial(activity, ad, onAdShown, onAdFailed, onAdClosed)
    }

    /**
     * Load and show interstitial ad immediately
     */
    private fun loadInterstitialAdForShow(
        activity: Activity,
        onAdShown: () -> Unit,
        onAdFailed: () -> Unit,
        onAdClosed: () -> Unit
    ) {
        InterstitialAd.load(
            context,
            INTERSTITIAL_AD_ID,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.e(TAG, "Failed to load interstitial for show")
                    onAdFailed()
                }
                
                override fun onAdLoaded(ad: InterstitialAd) {
                    showInterstitial(activity, ad, onAdShown, onAdFailed, onAdClosed)
                }
            }
        )
    }

    /**
     * Actually show the interstitial ad
     */
    private fun showInterstitial(
        activity: Activity,
        ad: InterstitialAd,
        onAdShown: () -> Unit,
        onAdFailed: () -> Unit,
        onAdClosed: () -> Unit
    ) {
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "Interstitial ad dismissed")
                onAdClosed()
                scope.launch {
                    preferenceManager.setLastAdShown(System.currentTimeMillis())
                }
                // Preload next ad
                preloadInterstitialAd()
            }
            
            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                Log.e(TAG, "Interstitial failed to show: ${error.message}")
                onAdFailed()
            }
            
            override fun onAdShowedFullScreenContent() {
                Log.d(TAG, "Interstitial ad shown")
                onAdShown()
            }
        }
        
        ad.show(activity)
    }

    /**
     * Show rewarded ad
     */
    suspend fun showRewardedAd(
        activity: Activity,
        onRewarded: () -> Unit,
        onAdFailed: () -> Unit = {},
        onAdClosed: () -> Unit = {}
    ) {
        onRewardedCallback = onRewarded
        onAdClosedCallback = onAdClosed
        
        // Get cached ad
        val ad = cachedRewardedAds.poll() ?: run {
            if (isOnline()) {
                loadRewardedAdForShow(activity, onRewarded, onAdFailed, onAdClosed)
            } else {
                Log.d(TAG, "No cached rewarded ad and offline")
                onAdFailed()
            }
            return
        }
        
        showRewarded(activity, ad, onRewarded, onAdFailed, onAdClosed)
    }

    /**
     * Load and show rewarded ad immediately
     */
    private fun loadRewardedAdForShow(
        activity: Activity,
        onRewarded: () -> Unit,
        onAdFailed: () -> Unit,
        onAdClosed: () -> Unit
    ) {
        RewardedAd.load(
            context,
            REWARDED_AD_ID,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.e(TAG, "Failed to load rewarded for show")
                    onAdFailed()
                }
                
                override fun onAdLoaded(ad: RewardedAd) {
                    showRewarded(activity, ad, onRewarded, onAdFailed, onAdClosed)
                }
            }
        )
    }

    /**
     * Actually show the rewarded ad
     */
    private fun showRewarded(
        activity: Activity,
        ad: RewardedAd,
        onRewarded: () -> Unit,
        onAdFailed: () -> Unit,
        onAdClosed: () -> Unit
    ) {
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "Rewarded ad dismissed")
                onAdClosed()
                // Preload next ad
                preloadRewardedAd()
            }
            
            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                Log.e(TAG, "Rewarded failed to show: ${error.message}")
                onAdFailed()
            }
            
            override fun onAdShowedFullScreenContent() {
                Log.d(TAG, "Rewarded ad shown")
            }
        }
        
        ad.show(activity) { rewardItem ->
            Log.d(TAG, "User earned reward: ${rewardItem.amount} ${rewardItem.type}")
            onRewarded()
        }
    }

    /**
     * Check if device is online
     */
    private fun isOnline(): Boolean {
        return (context as? LudoBlitzApp)?.isOnline() ?: run {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) 
                as android.net.ConnectivityManager
            val network = connectivityManager.activeNetworkInfo
            network != null && network.isConnected
        }
    }

    /**
     * Check if there's a cached rewarded ad available
     */
    fun hasRewardedAdAvailable(): Boolean {
        return cachedRewardedAds.isNotEmpty() || isOnline()
    }

    /**
     * Check if there's a cached interstitial ad available
     */
    fun hasInterstitialAdAvailable(): Boolean {
        return cachedInterstitialAds.isNotEmpty() || isOnline()
    }

    /**
     * Get count of cached ads (for debugging)
     */
    fun getCachedAdsCount(): Pair<Int, Int> {
        return Pair(cachedInterstitialAds.size, cachedRewardedAds.size)
    }

    /**
     * Force preload more ads (call when user comes online)
     */
    fun onNetworkRestored() {
        Log.d(TAG, "Network restored, preloading ads")
        startPreloadingAds()
    }

    /**
     * Clean up resources
     */
    fun destroy() {
        currentInterstitialAd = null
        currentRewardedAd = null
        cachedInterstitialAds.clear()
        cachedRewardedAds.clear()
    }
}
