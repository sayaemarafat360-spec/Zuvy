package com.ludoblitz.app.ui.screens

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewTreeObserver
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.airbnb.lottie.LottieAnimationView
import com.ludoblitz.app.R
import com.ludoblitz.app.databinding.ActivitySplashBinding
import com.ludoblitz.app.ui.viewmodel.MainUiState
import com.ludoblitz.app.ui.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Splash Activity - First screen shown when app launches
 * Handles authentication check and navigation to appropriate screen
 */
@AndroidEntryPoint
class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding
    private val viewModel: MainViewModel by viewModels()
    
    private var isReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        // Install splash screen before super.onCreate
        val splashScreen = installSplashScreen()
        
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Keep splash screen visible until ready
        splashScreen.setKeepOnScreenCondition { !isReady }
        
        setupAnimation()
        observeViewModel()
    }

    private fun setupAnimation() {
        // Play Lottie animation
        binding.lottieAnimation.apply {
            setAnimation("splash_animation.json")
            playAnimation()
            
            addAnimatorListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    // Animation finished, check if we can proceed
                    checkReadyState()
                }
            })
        }
        
        // Animate logo and text
        binding.logoCard.alpha = 0f
        binding.logoCard.translationY = 50f
        
        binding.appName.alpha = 0f
        binding.appName.translationY = 30f
        
        binding.tagline.alpha = 0f
        binding.tagline.translationY = 20f
        
        // Start animations after a short delay
        lifecycleScope.launch {
            delay(300)
            
            binding.logoCard.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(600)
                .start()
            
            delay(200)
            
            binding.appName.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(500)
                .start()
            
            delay(100)
            
            binding.tagline.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(400)
                .start()
        }
    }

    private fun observeViewModel() {
        viewModel.uiState.observe(this) { state ->
            when (state) {
                is MainUiState.Authenticated -> {
                    // User is logged in, navigate to main
                    navigateToMain()
                }
                is MainUiState.Unauthenticated,
                is MainUiState.SessionExpired -> {
                    // Need to show login
                    navigateToAuth()
                }
                else -> {
                    // Still loading, wait
                }
            }
        }
    }

    private fun checkReadyState() {
        val state = viewModel.uiState.value
        
        when (state) {
            is MainUiState.Authenticated,
            is MainUiState.Unauthenticated,
            is MainUiState.SessionExpired,
            is MainUiState.Error -> {
                isReady = true
            }
            else -> {
                // Still loading, wait a bit more
                Handler(Looper.getMainLooper()).postDelayed({
                    checkReadyState()
                }, 500)
            }
        }
    }

    private fun navigateToMain() {
        lifecycleScope.launch {
            delay(500)
            
            // Start main activity with nice transition
            val intent = Intent(this@SplashActivity, MainActivity::class.java)
            startActivity(intent)
            
            // Override transition
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            
            finish()
        }
    }

    private fun navigateToAuth() {
        lifecycleScope.launch {
            delay(500)
            
            // Start main activity (which will show login)
            val intent = Intent(this@SplashActivity, MainActivity::class.java)
            intent.putExtra("show_login", true)
            startActivity(intent)
            
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.lottieAnimation.cancelAnimation()
    }
}
