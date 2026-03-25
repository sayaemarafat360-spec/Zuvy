package com.zuvy.app.ui.components

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.FrameLayout
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.zuvy.app.R
import com.zuvy.app.databinding.ViewMiniPlayerBinding
import com.zuvy.app.player.PlayerManager
import kotlinx.coroutines.launch

class MiniPlayerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val binding: ViewMiniPlayerBinding
    private var playerManager: PlayerManager? = null
    private var onExpandListener: (() -> Unit)? = null

    init {
        binding = ViewMiniPlayerBinding.inflate(LayoutInflater.from(context), this, true)
        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.root.setOnClickListener {
            onExpandListener?.invoke()
        }

        binding.playPauseButton.setOnClickListener {
            playerManager?.playPause()
        }

        binding.nextButton.setOnClickListener {
            playerManager?.playNext()
        }

        binding.closeButton.setOnClickListener {
            playerManager?.getPlayer()?.stop()
            visibility = View.GONE
        }
    }

    fun setOnExpandListener(listener: () -> Unit) {
        onExpandListener = listener
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
                    loadThumbnail(it.uri)
                    visibility = View.VISIBLE
                    animateSlideUp()
                } ?: run {
                    visibility = View.GONE
                }
            }
        }

        lifecycleOwner.lifecycleScope.launch {
            manager.isPlaying.collect { isPlaying ->
                val iconRes = if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
                binding.playPauseButton.setImageResource(iconRes)
                
                if (isPlaying) {
                    startProgressAnimation()
                } else {
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
            .placeholder(R.drawable.ic_music_note)
            .circleCrop()
            .into(binding.thumbnail)
    }

    private fun animateSlideUp() {
        translationY = height.toFloat()
        alpha = 0f
        
        animate()
            .translationY(0f)
            .alpha(1f)
            .setDuration(300)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()
    }

    private var progressAnimator: ValueAnimator? = null

    private fun startProgressAnimation() {
        progressAnimator?.cancel()
        progressAnimator = ValueAnimator.ofFloat(1f, 1.05f, 1f).apply {
            duration = 1000
            repeatCount = ValueAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { animator ->
                val scale = animator.animatedValue as Float
                binding.thumbnail.scaleX = scale
                binding.thumbnail.scaleY = scale
            }
            start()
        }
    }

    private fun stopProgressAnimation() {
        progressAnimator?.cancel()
        progressAnimator = null
        binding.thumbnail.scaleX = 1f
        binding.thumbnail.scaleY = 1f
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        progressAnimator?.cancel()
    }
}
