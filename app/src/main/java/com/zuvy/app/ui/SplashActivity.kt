package com.zuvy.app.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.zuvy.app.R
import com.zuvy.app.databinding.ActivitySplashBinding
import com.zuvy.app.onboarding.OnboardingActivity
import com.zuvy.app.utils.PreferenceManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding

    @Inject
    lateinit var preferenceManager: PreferenceManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupAnimation()
    }

    private fun setupAnimation() {
        // Start with logo invisible
        binding.logoImage.alpha = 0f
        binding.logoImage.translationY = 50f

        binding.appName.alpha = 0f
        binding.tagline.alpha = 0f

        // Animate logo
        binding.logoImage.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(600)
            .setStartDelay(200)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    // Animate app name
                    binding.appName.animate()
                        .alpha(1f)
                        .setDuration(400)
                        .start()

                    // Animate tagline
                    binding.tagline.animate()
                        .alpha(1f)
                        .setDuration(400)
                        .setStartDelay(200)
                        .start()
                }
            })
            .start()

        // Navigate after animation
        lifecycleScope.launch {
            delay(1500)
            navigateToNextScreen()
        }
    }

    private fun navigateToNextScreen() {
        // Check if onboarding has been completed
        if (!OnboardingActivity.hasCompletedOnboarding(this)) {
            // Show onboarding for first-time users
            startActivity(Intent(this, OnboardingActivity::class.java))
        } else {
            // Go directly to main activity
            startActivity(Intent(this, MainActivity::class.java))
        }
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        finish()
    }
}
