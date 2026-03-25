package com.zuvy.app.onboarding

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.BounceInterpolator
import android.view.animation.OvershootInterpolator
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.core.view.isVisible
import androidx.viewpager2.widget.ViewPager2
import com.airbnb.lottie.LottieDrawable
import com.zuvy.app.MainActivity
import com.zuvy.app.R
import com.zuvy.app.databinding.ActivityOnboardingBinding
import com.zuvy.app.databinding.ItemOnboardingPageBinding
import dagger.hilt.android.AndroidEntryPoint

/**
 * OnboardingActivity - Interactive animated onboarding for first-time users
 * 
 * Features:
 * - Animated Lottie illustrations
 * - Smooth page transitions
 * - Interactive gestures (swipe, tap)
 * - Skip option
 * - Animated buttons and indicators
 */
@AndroidEntryPoint
class OnboardingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOnboardingBinding
    private var currentPage = 0

    companion object {
        fun hasCompletedOnboarding(context: Context): Boolean {
            return context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
                .getBoolean("onboarding_completed", false)
        }
    }

    private val onboardingPages = listOf(
        OnboardingPage(
            title = "Welcome to Zuvy",
            description = "Your all-in-one premium media player. Play videos, music, and discover content with a beautiful experience.",
            animationRes = R.raw.onboarding_welcome,
            iconRes = R.drawable.ic_logo,
            backgroundColor = R.color.onboarding_page1
        ),
        OnboardingPage(
            title = "Powerful Video Player",
            description = "Gestures for brightness, volume, and seek. Pinch to zoom, double-tap to seek, and enjoy advanced playback controls.",
            animationRes = R.raw.onboarding_video,
            iconRes = R.drawable.ic_video,
            backgroundColor = R.color.onboarding_page2
        ),
        OnboardingPage(
            title = "Immersive Music Experience",
            description = "Equalizer, visualizer, synced lyrics, and crossfade. Your music, your way.",
            animationRes = R.raw.onboarding_music,
            iconRes = R.drawable.ic_music,
            backgroundColor = R.color.onboarding_page3
        ),
        OnboardingPage(
            title = "Discover & Stream",
            description = "Explore trending content, online radio, podcasts, and stream from the web. Discover endless entertainment.",
            animationRes = R.raw.onboarding_discover,
            iconRes = R.drawable.ic_discover,
            backgroundColor = R.color.onboarding_page4
        ),
        OnboardingPage(
            title = "Go Premium",
            description = "Unlock all features, remove ads, and support development. Premium themes, backup, and exclusive features await!",
            animationRes = R.raw.onboarding_premium,
            iconRes = R.drawable.ic_crown,
            backgroundColor = R.color.onboarding_page5
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViewPager()
        setupClickListeners()
        setupBackPressed()
        animateEntrance()
    }

    private fun setupViewPager() {
        binding.viewPager.adapter = OnboardingAdapter(onboardingPages)
        binding.viewPager.offscreenPageLimit = 1
        
        // Custom page transformer for smooth animations
        binding.viewPager.setPageTransformer { page, position ->
            val absPosition = kotlin.math.abs(position)
            
            // Fade and scale animation
            page.alpha = 1f - (absPosition * 0.5f)
            page.scaleX = 1f - (absPosition * 0.1f)
            page.scaleY = 1f - (absPosition * 0.1f)
            
            // Translation for parallax effect
            page.translationX = position * -page.width * 0.2f
        }

        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                currentPage = position
                updateIndicators(position)
                updateButtons(position)
                animatePageContent(position)
            }
        })

        // Setup indicators
        setupIndicators()
    }

    private fun setupIndicators() {
        val indicators = arrayOf(
            binding.indicator1,
            binding.indicator2,
            binding.indicator3,
            binding.indicator4,
            binding.indicator5
        )

        indicators.forEachIndexed { index, view ->
            view.setOnClickListener {
                binding.viewPager.currentItem = index
            }
        }
    }

    private fun updateIndicators(position: Int) {
        val indicators = arrayOf(
            binding.indicator1,
            binding.indicator2,
            binding.indicator3,
            binding.indicator4,
            binding.indicator5
        )

        indicators.forEachIndexed { index, view ->
            val isSelected = index == position
            view.animate()
                .scaleX(if (isSelected) 1.4f else 1f)
                .scaleY(if (isSelected) 1.4f else 1f)
                .setDuration(200)
                .setInterpolator(OvershootInterpolator())
                .start()
            
            view.setBackgroundResource(
                if (isSelected) R.drawable.bg_indicator_active 
                else R.drawable.bg_indicator_inactive
            )
        }
    }

    private fun updateButtons(position: Int) {
        val isLastPage = position == onboardingPages.size - 1
        
        // Animate button text change
        val buttonText = if (isLastPage) "Get Started" else "Next"
        binding.nextButton.animate()
            .alpha(0f)
            .setDuration(150)
            .withEndAction {
                binding.nextButton.text = buttonText
                binding.nextButton.animate()
                    .alpha(1f)
                    .setDuration(150)
                    .start()
            }
            .start()

        // Show/hide skip button
        binding.skipButton.isVisible = !isLastPage
        
        // Animate skip button
        if (!isLastPage) {
            binding.skipButton.animate()
                .alpha(1f)
                .translationX(0f)
                .setDuration(200)
                .start()
        } else {
            binding.skipButton.animate()
                .alpha(0f)
                .translationX(50f)
                .setDuration(200)
                .start()
        }
    }

    private fun animatePageContent(position: Int) {
        // Animate content entrance for current page
        // This is handled by the adapter
    }

    private fun animateEntrance() {
        // Animate logo and title entrance
        binding.logoImage.apply {
            alpha = 0f
            scaleX = 0.5f
            scaleY = 0.5f
            animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(800)
                .setInterpolator(OvershootInterpolator())
                .start()
        }

        // Staggered indicator animation
        val indicators = arrayOf(
            binding.indicator1, binding.indicator2,
            binding.indicator3, binding.indicator4, binding.indicator5
        )

        indicators.forEachIndexed { index, view ->
            view.apply {
                alpha = 0f
                translationY = 20f
                animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setStartDelay(100L * (index + 1))
                    .setDuration(300)
                    .setInterpolator(AccelerateDecelerateInterpolator())
                    .start()
            }
        }

        // Animate buttons
        binding.nextButton.apply {
            alpha = 0f
            translationY = 30f
            animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay(600)
                .setDuration(400)
                .setInterpolator(OvershootInterpolator())
                .start()
        }

        binding.skipButton.apply {
            alpha = 0f
            translationX = 30f
            animate()
                .alpha(1f)
                .translationX(0f)
                .setStartDelay(700)
                .setDuration(400)
                .start()
        }
    }

    private fun setupClickListeners() {
        binding.nextButton.setOnClickListener {
            if (currentPage == onboardingPages.size - 1) {
                completeOnboarding()
            } else {
                binding.viewPager.currentItem = currentPage + 1
            }
        }

        binding.skipButton.setOnClickListener {
            showSkipConfirmation()
        }
    }

    private fun showSkipConfirmation() {
        // Animate skip confirmation
        val scaleX = ObjectAnimator.ofFloat(binding.skipButton, "scaleX", 1f, 0.9f, 1f)
        val scaleY = ObjectAnimator.ofFloat(binding.skipButton, "scaleY", 1f, 0.9f, 1f)
        
        AnimatorSet().apply {
            playTogether(scaleX, scaleY)
            duration = 200
            interpolator = BounceInterpolator()
            doOnEnd {
                completeOnboarding()
            }
            start()
        }
    }

    private fun completeOnboarding() {
        // Save onboarding completion
        getSharedPreferences("app_settings", Context.MODE_PRIVATE)
            .edit { putBoolean("onboarding_completed", true) }

        // Animate transition
        animateExit()
    }

    private fun animateExit() {
        // Fade out everything
        binding.root.animate()
            .alpha(0f)
            .setDuration(300)
            .withEndAction {
                // Start main activity
                startActivity(android.content.Intent(this, MainActivity::class.java))
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                finish()
            }
            .start()
    }

    private fun setupBackPressed() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (currentPage > 0) {
                    binding.viewPager.currentItem = currentPage - 1
                } else {
                    showSkipConfirmation()
                }
            }
        })
    }

    // ============================================
    // ADAPTER
    // ============================================

    inner class OnboardingAdapter(
        private val pages: List<OnboardingPage>
    ) : androidx.recyclerview.widget.RecyclerView.Adapter<OnboardingAdapter.OnboardingViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OnboardingViewHolder {
            val binding = ItemOnboardingPageBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            return OnboardingViewHolder(binding)
        }

        override fun onBindViewHolder(holder: OnboardingViewHolder, position: Int) {
            holder.bind(pages[position], position)
        }

        override fun getItemCount(): Int = pages.size

        inner class OnboardingViewHolder(
            private val binding: ItemOnboardingPageBinding
        ) : androidx.recyclerview.widget.RecyclerView.ViewHolder(binding.root) {

            fun bind(page: OnboardingPage, position: Int) {
                binding.titleText.text = page.title
                binding.descriptionText.text = page.description
                
                // Load animation or static icon
                if (page.animationRes != 0) {
                    try {
                        binding.animationView.setAnimation(page.animationRes)
                        binding.animationView.repeatCount = LottieDrawable.INFINITE
                        binding.animationView.playAnimation()
                    } catch (e: Exception) {
                        binding.animationView.setImageResource(page.iconRes)
                    }
                } else {
                    binding.animationView.setImageResource(page.iconRes)
                }

                // Animate content entrance
                animateContent()
            }

            private fun animateContent() {
                binding.animationView.apply {
                    alpha = 0f
                    scaleX = 0.8f
                    scaleY = 0.8f
                    animate()
                        .alpha(1f)
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(600)
                        .setInterpolator(OvershootInterpolator())
                        .start()
                }

                binding.titleText.apply {
                    alpha = 0f
                    translationY = 30f
                    animate()
                        .alpha(1f)
                        .translationY(0f)
                        .setStartDelay(200)
                        .setDuration(400)
                        .setInterpolator(AccelerateDecelerateInterpolator())
                        .start()
                }

                binding.descriptionText.apply {
                    alpha = 0f
                    translationY = 30f
                    animate()
                        .alpha(1f)
                        .translationY(0f)
                        .setStartDelay(400)
                        .setDuration(400)
                        .setInterpolator(AccelerateDecelerateInterpolator())
                        .start()
                }
            }
        }
    }
}

// ============================================
// DATA CLASS
// ============================================

data class OnboardingPage(
    val title: String,
    val description: String,
    val animationRes: Int = 0,
    val iconRes: Int,
    val backgroundColor: Int
)
