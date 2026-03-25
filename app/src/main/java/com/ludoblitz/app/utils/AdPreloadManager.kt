package com.ludoblitz.app.utils

import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.InterstitialAd
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.ludoblitz.app.BuildConfig
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Singleton manager for preloading ads when app starts
 * Ensures ads are ready even if user goes offline
 */
class AdPreloadManager private constructor() {

    companion object {
        private const val TAG = "AdPreloadManager"
        private const val MAX_PRELOADED_ADS = 3
        
        @Volatile
        private var instance: AdPreloadManager? = null
        
        fun getInstance(): AdPreloadManager {
            return instance ?: synchronized(this) {
                instance ?: AdPreloadManager().also { instance = it }
            }
        }
    }

    private val preloadedInterstitialAds = ConcurrentLinkedQueue<InterstitialAd>()
    private val preloadedRewardedAds = ConcurrentLinkedQueue<RewardedAd>()

    /**
     * Preload ads when app initializes
     */
    fun preloadAds(context: Context) {
        Log.d(TAG, "Starting ad preloading...")
        
        // Initialize MobileAds
        MobileAds.initialize(context) {
            Log.d(TAG, "MobileAds initialized, loading ads...")
            
            // Preload interstitial ads
            preloadInterstitialAds(context)
            
            // Preload rewarded ads
            preloadRewardedAds(context)
        }
    }

    private fun preloadInterstitialAds(context: Context) {
        repeat(MAX_PRELOADED_ADS) { index ->
            val adRequest = AdRequest.Builder().build()
            
            InterstitialAd.load(
                context,
                BuildConfig.ADMOB_INTERSTITIAL_ID,
                adRequest,
                object : InterstitialAdLoadCallback() {
                    override fun onAdLoaded(ad: InterstitialAd) {
                        preloadedInterstitialAds.add(ad)
                        Log.d(TAG, "Interstitial ad preloaded (${index + 1}/$MAX_PRELOADED_ADS)")
                    }
                    
                    override fun onAdFailedToLoad(error: LoadAdError) {
                        Log.w(TAG, "Failed to preload interstitial ad: ${error.message}")
                    }
                }
            )
            
            // Small delay between loads
            Thread.sleep(500)
        }
    }

    private fun preloadRewardedAds(context: Context) {
        repeat(MAX_PRELOADED_ADS) { index ->
            val adRequest = AdRequest.Builder().build()
            
            RewardedAd.load(
                context,
                BuildConfig.ADMOB_REWARDED_ID,
                adRequest,
                object : RewardedAdLoadCallback() {
                    override fun onAdLoaded(ad: RewardedAd) {
                        preloadedRewardedAds.add(ad)
                        Log.d(TAG, "Rewarded ad preloaded (${index + 1}/$MAX_PRELOADED_ADS)")
                    }
                    
                    override fun onAdFailedToLoad(error: LoadAdError) {
                        Log.w(TAG, "Failed to preload rewarded ad: ${error.message}")
                    }
                }
            )
            
            // Small delay between loads
            Thread.sleep(500)
        }
    }

    /**
     * Get a preloaded interstitial ad
     */
    fun getInterstitialAd(): InterstitialAd? {
        return preloadedInterstitialAds.poll()
    }

    /**
     * Get a preloaded rewarded ad
     */
    fun getRewardedAd(): RewardedAd? {
        return preloadedRewardedAds.poll()
    }

    /**
     * Check if ads are available
     */
    fun hasInterstitialAd(): Boolean = preloadedInterstitialAds.isNotEmpty()
    fun hasRewardedAd(): Boolean = preloadedRewardedAds.isNotEmpty()

    /**
     * Refill ads after consumption
     */
    fun refillAds(context: Context) {
        if (preloadedInterstitialAds.size < MAX_PRELOADED_ADS) {
            preloadInterstitialAds(context)
        }
        if (preloadedRewardedAds.size < MAX_PRELOADED_ADS) {
            preloadRewardedAds(context)
        }
    }

    /**
     * Clear all cached ads
     */
    fun clearAds() {
        preloadedInterstitialAds.clear()
        preloadedRewardedAds.clear()
    }
}
