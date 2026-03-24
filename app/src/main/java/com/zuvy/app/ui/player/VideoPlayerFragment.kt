package com.zuvy.app.ui.player

import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.navigation.fragment.navArgs
import com.zuvy.app.R
import com.zuvy.app.databinding.FragmentVideoPlayerBinding
import com.zuvy.app.utils.formatDuration
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class VideoPlayerFragment : Fragment() {

    private var _binding: FragmentVideoPlayerBinding? = null
    private val binding get() = _binding!!

    private val args: VideoPlayerFragmentArgs by navArgs()
    private var player: ExoPlayer? = null
    private var isControlsVisible = true
    private var isLocked = false
    private var currentSpeed = 1.0f

    private val handler = Handler(Looper.getMainLooper())
    private val hideControlsRunnable = Runnable {
        if (!isLocked) {
            hideControls()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVideoPlayerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupPlayer()
        setupGestures()
        setupClickListeners()
        setupBackPressed()
    }

    private fun setupPlayer() {
        player = ExoPlayer.Builder(requireContext()).build()
        binding.playerView.player = player

        val videoUri = args.videoUri
        val mediaItem = MediaItem.fromUri(Uri.parse(videoUri))
        player?.setMediaItem(mediaItem)
        player?.prepare()
        player?.playWhenReady = true

        // Update progress
        player?.addListener(object : androidx.media3.common.Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                updateControls()
            }
        })
    }

    private fun setupGestures() {
        val gestureDetector = GestureDetector(requireContext(), object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                if (!isLocked) {
                    toggleControls()
                }
                return true
            }

            override fun onDoubleTap(e: MotionEvent): Boolean {
                if (!isLocked) {
                    val width = binding.root.width
                    if (e.x < width / 3) {
                        seekBackward()
                    } else if (e.x > width * 2 / 3) {
                        seekForward()
                    } else {
                        togglePlayPause()
                    }
                }
                return true
            }
        })

        binding.gestureOverlay.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            true
        }
    }

    private fun setupClickListeners() {
        binding.backButton.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        binding.playPauseButton.setOnClickListener {
            togglePlayPause()
        }

        binding.rewindButton.setOnClickListener {
            seekBackward()
        }

        binding.forwardButton.setOnClickListener {
            seekForward()
        }

        binding.speedButton.setOnClickListener {
            changeSpeed()
        }

        binding.lockButton.setOnClickListener {
            toggleLock()
        }

        binding.unlockButton.setOnClickListener {
            toggleLock()
        }

        binding.fullscreenButton.setOnClickListener {
            // Toggle fullscreen
        }

        binding.progressBar.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    val duration = player?.duration ?: 0
                    val position = (duration * progress / 100).toLong()
                    player?.seekTo(position)
                    updateProgress()
                }
            }
            override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {}
        })
    }

    private fun setupBackPressed() {
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                player?.release()
                isEnabled = false
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }
        })
    }

    private fun toggleControls() {
        if (isControlsVisible) {
            hideControls()
        } else {
            showControls()
        }
    }

    private fun showControls() {
        isControlsVisible = true
        binding.controlsOverlay.animate()
            .alpha(1f)
            .setDuration(200)
            .withStartAction { binding.controlsOverlay.visibility = View.VISIBLE }
            .start()
        startHideTimer()
    }

    private fun hideControls() {
        isControlsVisible = false
        binding.controlsOverlay.animate()
            .alpha(0f)
            .setDuration(200)
            .withEndAction { binding.controlsOverlay.visibility = View.GONE }
            .start()
    }

    private fun startHideTimer() {
        handler.removeCallbacks(hideControlsRunnable)
        handler.postDelayed(hideControlsRunnable, 3000)
    }

    private fun togglePlayPause() {
        if (player?.isPlaying == true) {
            player?.pause()
            binding.playPauseButton.setImageResource(R.drawable.ic_play)
        } else {
            player?.play()
            binding.playPauseButton.setImageResource(R.drawable.ic_pause)
        }
        updateProgress()
    }

    private fun seekForward() {
        val current = player?.currentPosition ?: 0
        player?.seekTo(current + 10000)
        showSeekIndicator("+10s")
        updateProgress()
    }

    private fun seekBackward() {
        val current = player?.currentPosition ?: 0
        player?.seekTo(maxOf(0, current - 10000))
        showSeekIndicator("-10s")
        updateProgress()
    }

    private fun showSeekIndicator(text: String) {
        binding.seekIndicator.visibility = View.VISIBLE
        binding.seekText.text = text
        handler.postDelayed({
            binding.seekIndicator.visibility = View.INVISIBLE
        }, 500)
    }

    private fun changeSpeed() {
        val speeds = floatArrayOf(0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f, 2.5f, 3.0f, 4.0f)
        val currentIndex = speeds.indexOfFirst { it == currentSpeed }
        val nextIndex = (currentIndex + 1) % speeds.size
        currentSpeed = speeds[nextIndex]
        player?.setPlaybackSpeed(currentSpeed)
        binding.speedButton.text = "${currentSpeed}x"
    }

    private fun toggleLock() {
        isLocked = !isLocked
        if (isLocked) {
            binding.lockOverlay.visibility = View.VISIBLE
            binding.lockButton.setImageResource(R.drawable.ic_lock)
            hideControls()
        } else {
            binding.lockOverlay.visibility = View.GONE
            binding.lockButton.setImageResource(R.drawable.ic_lock_open)
            showControls()
        }
    }

    private fun updateControls() {
        updateProgress()
    }

    private fun updateProgress() {
        val duration = player?.duration ?: 0
        val position = player?.currentPosition ?: 0

        binding.duration.text = duration.formatDuration()
        binding.currentPosition.text = position.formatDuration()

        if (duration > 0) {
            binding.progressBar.progress = ((position * 100) / duration).toInt()
        }
    }

    override fun onResume() {
        super.onResume()
        startProgressUpdater()
    }

    override fun onPause() {
        super.onPause()
        stopProgressUpdater()
    }

    private fun startProgressUpdater() {
        handler.postDelayed(object : Runnable {
            override fun run() {
                updateProgress()
                handler.postDelayed(this, 1000)
            }
        }, 1000)
    }

    private fun stopProgressUpdater() {
        handler.removeCallbacksAndMessages(null)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        player?.release()
        player = null
        handler.removeCallbacksAndMessages(null)
        _binding = null
    }
}
