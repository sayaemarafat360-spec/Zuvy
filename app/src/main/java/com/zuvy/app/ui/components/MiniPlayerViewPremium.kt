package com.zuvy.app.ui.components

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.zuvy.app.R
import com.zuvy.app.databinding.ViewMiniPlayerPremiumBinding
import com.zuvy.app.player.PlayerManager
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Premium Mini Player with glassmorphic design and swipe gestures
 */
class MiniPlayerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val binding: ViewMiniPlayerPremiumBinding
    private var playerManager: PlayerManager? = null
    private var onExpandListener: (() -> Unit)? = null
    private var onSwipeListener: ((direction: SwipeDirection) -> Unit)? = null

    private val gestureDetector: GestureDetector
    private var initialX = 0f
    private var initialTranslationX = 0f
    private var isSwiping = false

    private var progressAnimator: ValueAnimator? = null
    private var glowAnimator: ValueAnimator? = null
    private var isAnimatingGlow = false

    enum class SwipeDirection {
        LEFT, RIGHT, UP
    }

    init {
        binding = ViewMiniPlayerPremiumBinding.inflate(LayoutInflater.from(context), this, true)
        gestureDetector = GestureDetector(context, GestureListener())
        setupTouchListeners()
        setupClickListeners()
    }

    private fun setupTouchListeners() {
        setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = event.rawX
                    initialTranslationX = translationX
                    isSwiping = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaX = event.rawX - initialX
                    
                    if (abs(deltaX) > 20) {
                        isSwiping = true
                        translationX = initialTranslationX + deltaX * 0.5f
                    }
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (isSwiping) {
                        handleSwipeEnd()
                    } else {
                        translationX = 0f
                    }
                    true
                }
                else -> gestureDetector.onTouchEvent(event)
            }
        }
    }

    private fun handleSwipeEnd() {
        val threshold = width * 0.3f
        
        when {
            translationX > threshold -> {
                // Swipe right - previous track
                animateSwipeAndReset(SwipeDirection.RIGHT)
                onSwipeListener?.invoke(SwipeDirection.RIGHT)
            }
            translationX < -threshold -> {
                // Swipe left - next track
                animateSwipeAndReset(SwipeDirection.LEFT)
                onSwipeListener?.invoke(SwipeDirection.LEFT)
            }
            else -> {
                // Reset position
                animate().translationX(0f).setDuration(200).start()
            }
        }
    }

    private fun animateSwipeAndReset(direction: SwipeDirection) {
        val targetX = if (direction == SwipeDirection.LEFT) -width.toFloat() else width.toFloat()
        
        animate()
            .translationX(targetX)
            .alpha(0f)
            .setDuration(200)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    translationX = if (direction == SwipeDirection.LEFT) width.toFloat() else -width.toFloat()
                    animate()
                        .translationX(0f)
                        .alpha(1f)
                        .setDuration(200)
                        .start()
                }
            })
            .start()
    }

    private fun setupClickListeners() {
        binding.root.setOnClickListener {
            if (!isSwiping) {
                onExpandListener?.invoke()
            }
        }

        binding.playPauseButton.setOnClickListener {
            playerManager?.playPause()
            animatePlayPauseButton()
        }

        binding.previousButton.setOnClickListener {
            playerManager?.playPrevious()
        }

        binding.nextButton.setOnClickListener {
            playerManager?.playNext()
        }

        binding.closeButton.setOnClickListener {
            playerManager?.getPlayer()?.stop()
            hideWithAnimation()
        }
    }

    fun setOnExpandListener(listener: () -> Unit) {
        onExpandListener = listener
    }

    fun setOnSwipeListener(listener: (SwipeDirection) -> Unit) {
        onSwipeListener = listener
    }

    fun setPlayerManager(manager: PlayerManager) {
        playerManager = manager
    }

    fun observe(lifecycleOwner: LifecycleOwner, manager: PlayerManager) {
        playerManager = manager

        lifecycleOwner.lifecycleScope.launch {
            manager.currentMedia.collect { media ->
                media?.let {
                    binding.title.text = it.name
                    binding.artist.text = it.artist ?: "Unknown Artist"
                    loadThumbnail(it.uri)
                    showWithAnimation()
                } ?: run {
                    hideWithAnimation()
                }
            }
        }

        lifecycleOwner.lifecycleScope.launch {
            manager.isPlaying.collect { isPlaying ->
                updatePlayPauseButton(isPlaying)
                
                if (isPlaying) {
                    startGlowAnimation()
                    startProgressAnimation()
                } else {
                    stopGlowAnimation()
                    stopProgressAnimation()
                }
            }
        }

        lifecycleOwner.lifecycleScope.launch {
            manager.currentPosition.collect { position ->
                val duration = manager.duration.value
                if (duration > 0) {
                    binding.progressBar.progress = ((position * 100) / duration).toInt()
                }
            }
        }
    }

    private fun loadThumbnail(uri: Uri) {
        Glide.with(binding.thumbnail)
            .load(uri)
            .transition(DrawableTransitionOptions.withCrossFade(300))
            .placeholder(R.drawable.ic_music_note)
            .centerCrop()
            .into(binding.thumbnail)
    }

    private fun updatePlayPauseButton(isPlaying: Boolean) {
        val iconRes = if (isPlaying) R.drawable.ic_pause_glow else R.drawable.ic_play_glow
        binding.playPauseButton.setImageResource(iconRes)
    }

    private fun animatePlayPauseButton() {
        binding.playPauseButton.animate()
            .scaleX(0.8f)
            .scaleY(0.8f)
            .setDuration(100)
            .withEndAction {
                binding.playPauseButton.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(100)
                    .setInterpolator(OvershootInterpolator(1.5f))
                    .start()
            }
            .start()
    }

    fun showWithAnimation() {
        if (visibility == VISIBLE) return
        
        visibility = VISIBLE
        translationY = height.toFloat()
        alpha = 0f
        
        animate()
            .translationY(0f)
            .alpha(1f)
            .setDuration(300)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()
    }

    fun hideWithAnimation() {
        if (visibility != VISIBLE) return
        
        animate()
            .translationY(height.toFloat())
            .alpha(0f)
            .setDuration(300)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction {
                visibility = GONE
            }
            .start()
    }

    private fun startGlowAnimation() {
        if (isAnimatingGlow) return
        isAnimatingGlow = true
        
        glowAnimator?.cancel()
        glowAnimator = ValueAnimator.ofFloat(0.2f, 0.5f, 0.2f).apply {
            duration = 2000
            repeatCount = ValueAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { animator ->
                val alpha = animator.animatedValue as Float
                binding.thumbnailGlow.alpha = alpha
            }
            start()
        }
    }

    private fun stopGlowAnimation() {
        isAnimatingGlow = false
        glowAnimator?.cancel()
        glowAnimator = null
        binding.thumbnailGlow.alpha = 0.3f
    }

    private fun startProgressAnimation() {
        progressAnimator?.cancel()
        progressAnimator = ValueAnimator.ofFloat(1f, 1.05f, 1f).apply {
            duration = 1500
            repeatCount = ValueAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { animator ->
                val scale = animator.animatedValue as Float
                binding.thumbnailContainer.scaleX = scale
                binding.thumbnailContainer.scaleY = scale
            }
            start()
        }
    }

    private fun stopProgressAnimation() {
        progressAnimator?.cancel()
        progressAnimator = null
        binding.thumbnailContainer.scaleX = 1f
        binding.thumbnailContainer.scaleY = 1f
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        progressAnimator?.cancel()
        glowAnimator?.cancel()
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        return gestureDetector.onTouchEvent(ev)
    }

    /**
     * Gesture listener for swipe up to expand
     */
    private inner class GestureListener : GestureDetector.SimpleOnGestureListener() {
        override fun onSingleTapUp(e: MotionEvent): Boolean {
            onExpandListener?.invoke()
            return true
        }

        override fun onFling(
            e1: MotionEvent?,
            e2: MotionEvent,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            val diffY = (e2?.rawY ?: 0f) - (e1?.rawY ?: 0f)
            val diffX = (e2?.rawX ?: 0f) - (e1?.rawX ?: 0f)

            // Swipe up to expand
            if (abs(diffY) > abs(diffX) && diffY < -100 && abs(velocityY) > 500) {
                onExpandListener?.invoke()
                return true
            }

            return false
        }

        override fun onDoubleTap(e: MotionEvent): Boolean {
            // Double tap to toggle favorite
            return true
        }

        override fun onLongPress(e: MotionEvent) {
            // Long press to show options
        }
    }
}
