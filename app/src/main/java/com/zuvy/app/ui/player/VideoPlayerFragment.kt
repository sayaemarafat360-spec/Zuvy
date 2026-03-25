package com.zuvy.app.ui.player

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.app.PictureInPictureParams
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.annotation.RequiresApi
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.slider.Slider
import com.zuvy.app.R
import com.zuvy.app.databinding.FragmentVideoPlayerBinding
import com.zuvy.app.databinding.DialogPlaybackSpeedBinding
import com.zuvy.app.databinding.DialogSleepTimerBinding
import com.zuvy.app.player.PlayerManager
import com.zuvy.app.utils.formatDuration
import com.zuvy.app.utils.formatFileSize
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.abs

@AndroidEntryPoint
class VideoPlayerFragment : Fragment() {

    private var _binding: FragmentVideoPlayerBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var playerManager: PlayerManager

    private val args: VideoPlayerFragmentArgs by navArgs()
    
    private var player: ExoPlayer? = null
    private var videoUri: String? = null
    private var videoName: String? = null
    
    private var isControlsVisible = true
    private var isLocked = false
    private var currentSpeed = 1.0f
    private var currentBrightness = 0.5f
    private var currentVolume = 0.5f
    private var maxVolume = 0
    
    // Gesture state
    private var gestureStartX = 0f
    private var gestureStartY = 0f
    private var gestureStartBrightness = 0f
    private var gestureStartVolume = 0f
    private var gestureStartPosition = 0L
    private var isBrightnessGesture = false
    private var isVolumeGesture = false
    private var isSeekGesture = false
    
    // Animation state
    private var seekAnimator: ValueAnimator? = null
    private var controlsAnimator: ValueAnimator? = null
    
    private val handler = Handler(Looper.getMainLooper())
    private val hideControlsRunnable = Runnable {
        if (!isLocked && isControlsVisible) {
            hideControls()
        }
    }

    private lateinit var gestureDetector: GestureDetector
    private lateinit var scaleGestureDetector: ScaleGestureDetector

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        videoUri = args.videoUri
        videoName = args.videoName
        
        // Get current brightness
        activity?.window?.attributes?.screenBrightness?.let {
            currentBrightness = it
        }
        
        // Get max volume
        val audioManager = requireContext().getSystemService(android.content.Context.AUDIO_SERVICE) 
            as android.media.AudioManager
        maxVolume = audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC)
        
        currentVolume = audioManager.getStreamVolume(android.media.AudioManager.STREAM_MUSIC)
            .toFloat() / maxVolume
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
        setupWindowInsets()
        
        loadVideo()
    }

    private fun setupPlayer() {
        player = playerManager.getPlayer() ?: ExoPlayer.Builder(requireContext()).build()
        binding.playerView.player = player
        binding.playerView.useController = false // We use custom controls
        
        player?.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                when (state) {
                    Player.STATE_READY -> {
                        binding.loadingProgress.hide()
                        updateProgress()
                        showControls()
                    }
                    Player.STATE_BUFFERING -> {
                        binding.loadingProgress.show()
                    }
                    Player.STATE_ENDED -> {
                        onPlaybackEnded()
                    }
                    Player.STATE_IDLE -> {
                        binding.loadingProgress.hide()
                    }
                }
            }
            
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                updatePlayPauseButton(isPlaying)
            }
        })
    }

    private fun loadVideo() {
        val uri = videoUri ?: return
        
        val mediaItem = MediaItem.Builder()
            .setUri(Uri.parse(uri))
            .setMediaMetadata(
                androidx.media3.common.MediaMetadata.Builder()
                    .setTitle(videoName ?: "Video")
                    .build()
            )
            .build()
        
        player?.setMediaItem(mediaItem)
        player?.prepare()
        player?.playWhenReady = true
        
        binding.videoTitle.text = videoName ?: "Video"
        
        // Load thumbnail
        Glide.with(binding.videoThumbnail)
            .load(Uri.parse(uri))
            .placeholder(R.drawable.ic_video_placeholder)
            .into(binding.videoThumbnail)
    }

    private fun setupGestures() {
        // Tap gesture
        gestureDetector = GestureDetector(requireContext(), object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                if (!isLocked) {
                    toggleControls()
                }
                return true
            }

            override fun onDoubleTap(e: MotionEvent): Boolean {
                if (isLocked) return true
                
                val viewWidth = binding.root.width
                val viewHeight = binding.root.height
                
                when {
                    e.x < viewWidth / 3 -> {
                        // Double tap left - seek backward
                        seekRelative(-10000L)
                        showSeekIndicator("-10s", e.x, e.y)
                    }
                    e.x > viewWidth * 2 / 3 -> {
                        // Double tap right - seek forward
                        seekRelative(10000L)
                        showSeekIndicator("+10s", e.x, e.y)
                    }
                    else -> {
                        // Double tap center - play/pause
                        togglePlayPause()
                        showPlayPauseIndicator()
                    }
                }
                return true
            }

            override fun onLongPress(e: MotionEvent) {
                if (!isLocked) {
                    toggleSpeed()
                }
            }
        })

        // Scale gesture for zoom
        scaleGestureDetector = ScaleGestureDetector(requireContext(), object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                // Handle zoom if needed
                return true
            }
        })

        binding.gestureOverlay.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            scaleGestureDetector.onTouchEvent(event)
            handleSwipeGesture(event)
            true
        }
    }

    private fun handleSwipeGesture(event: MotionEvent) {
        if (isLocked) return
        
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                gestureStartX = event.x
                gestureStartY = event.y
                gestureStartBrightness = currentBrightness
                gestureStartVolume = currentVolume
                gestureStartPosition = player?.currentPosition ?: 0
                isBrightnessGesture = false
                isVolumeGesture = false
                isSeekGesture = false
            }
            
            MotionEvent.ACTION_MOVE -> {
                val deltaX = event.x - gestureStartX
                val deltaY = event.y - gestureStartY
                val viewWidth = binding.root.width.toFloat()
                val viewHeight = binding.root.height.toFloat()
                
                // Determine gesture type based on initial position and direction
                if (!isBrightnessGesture && !isVolumeGesture && !isSeekGesture) {
                    when {
                        abs(deltaY) > 50 && gestureStartX < viewWidth / 2 -> {
                            isBrightnessGesture = true
                            binding.brightnessOverlay.visibility = View.VISIBLE
                        }
                        abs(deltaY) > 50 && gestureStartX > viewWidth / 2 -> {
                            isVolumeGesture = true
                            binding.volumeOverlay.visibility = View.VISIBLE
                        }
                        abs(deltaX) > 50 -> {
                            isSeekGesture = true
                            binding.seekOverlay.visibility = View.VISIBLE
                        }
                    }
                }
                
                when {
                    isBrightnessGesture -> {
                        val progress = -deltaY / viewHeight
                        currentBrightness = (gestureStartBrightness + progress).coerceIn(0f, 1f)
                        setBrightness(currentBrightness)
                        updateBrightnessUI(currentBrightness)
                    }
                    isVolumeGesture -> {
                        val progress = -deltaY / viewHeight
                        currentVolume = (gestureStartVolume + progress).coerceIn(0f, 1f)
                        setVolume(currentVolume)
                        updateVolumeUI(currentVolume)
                    }
                    isSeekGesture -> {
                        val seekAmount = (deltaX / viewWidth * 60000).toLong() // Max 60 seconds
                        val newPosition = (gestureStartPosition + seekAmount).coerceIn(0, player?.duration ?: 0)
                        showSeekPreview(newPosition, seekAmount)
                    }
                }
            }
            
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (isSeekGesture) {
                    // Apply seek
                    val deltaX = event.x - gestureStartX
                    val viewWidth = binding.root.width.toFloat()
                    val seekAmount = (deltaX / viewWidth * 60000).toLong()
                    player?.seekTo((gestureStartPosition + seekAmount).coerceIn(0, player?.duration ?: 0))
                }
                
                // Hide overlays with animation
                animateHideOverlay(binding.brightnessOverlay)
                animateHideOverlay(binding.volumeOverlay)
                animateHideOverlay(binding.seekOverlay)
                
                // Schedule hide controls
                startHideTimer()
            }
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
            seekRelative(-10000L)
        }

        binding.forwardButton.setOnClickListener {
            seekRelative(10000L)
        }

        binding.speedButton.setOnClickListener {
            showSpeedDialog()
        }

        binding.lockButton.setOnClickListener {
            toggleLock()
        }

        binding.unlockButton.setOnClickListener {
            toggleLock()
        }

        binding.fullscreenButton.setOnClickListener {
            toggleFullscreen()
        }

        binding.pipButton.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                enterPipMode()
            }
        }

        binding.sleepButton.setOnClickListener {
            showSleepTimerDialog()
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
            override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {
                handler.removeCallbacks(hideControlsRunnable)
            }
            override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {
                startHideTimer()
            }
        })
    }

    private fun setupBackPressed() {
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (isLocked) {
                    toggleLock()
                    return
                }
                
                playerManager.saveCurrentPosition()
                isEnabled = false
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }
        })
    }

    private fun setupWindowInsets() {
        binding.root.setOnApplyWindowInsetsListener { view, insets ->
            val systemBars = insets.getInsets(WindowInsets.Type.systemBars())
            view.updatePadding(top = systemBars.top, bottom = systemBars.bottom)
            insets
        }
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
        controlsAnimator?.cancel()
        
        binding.controlsOverlay.visibility = View.VISIBLE
        binding.controlsOverlay.alpha = 0f
        controlsAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 200
            interpolator = DecelerateInterpolator()
            addUpdateListener { binding.controlsOverlay.alpha = it.animatedValue as Float }
            start()
        }
        
        if (isLocked) {
            binding.lockOverlay.visibility = View.VISIBLE
        }
        
        startHideTimer()
    }

    private fun hideControls() {
        isControlsVisible = false
        controlsAnimator?.cancel()
        
        controlsAnimator = ValueAnimator.ofFloat(1f, 0f).apply {
            duration = 200
            interpolator = DecelerateInterpolator()
            addUpdateListener { binding.controlsOverlay.alpha = it.animatedValue as Float }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    binding.controlsOverlay.visibility = View.GONE
                }
            })
            start()
        }
        
        handler.removeCallbacks(hideControlsRunnable)
    }

    private fun startHideTimer() {
        handler.removeCallbacks(hideControlsRunnable)
        handler.postDelayed(hideControlsRunnable, 4000)
    }

    private fun togglePlayPause() {
        player?.let {
            if (it.isPlaying) {
                it.pause()
            } else {
                it.play()
            }
        }
    }

    private fun updatePlayPauseButton(isPlaying: Boolean) {
        val iconRes = if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
        binding.playPauseButton.setImageResource(iconRes)
    }

    private fun seekRelative(offsetMs: Long) {
        player?.let {
            val newPosition = (it.currentPosition + offsetMs).coerceIn(0, it.duration)
            it.seekTo(newPosition)
            showSeekIndicator(if (offsetMs > 0) "+${offsetMs/1000}s" else "${offsetMs/1000}s")
        }
    }

    private fun showSeekIndicator(text: String, x: Float = 0f, y: Float = 0f) {
        binding.seekIndicator.visibility = View.VISIBLE
        binding.seekText.text = text
        binding.seekIndicator.animate()
            .alpha(1f)
            .scaleX(1.2f)
            .scaleY(1.2f)
            .setDuration(100)
            .withEndAction {
                binding.seekIndicator.animate()
                    .alpha(0f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(300)
                    .setStartDelay(300)
                    .withEndAction {
                        binding.seekIndicator.visibility = View.INVISIBLE
                    }
                    .start()
            }
            .start()
    }

    private fun showPlayPauseIndicator() {
        val isPlaying = player?.isPlaying == true
        val icon = if (isPlaying) binding.pauseIndicator else binding.playIndicator
        
        icon.visibility = View.VISIBLE
        icon.alpha = 0f
        icon.scaleX = 0.5f
        icon.scaleY = 0.5f
        
        icon.animate()
            .alpha(1f)
            .scaleX(1.2f)
            .scaleY(1.2f)
            .setDuration(150)
            .withEndAction {
                icon.animate()
                    .alpha(0f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(200)
                    .setStartDelay(200)
                    .withEndAction {
                        icon.visibility = View.GONE
                    }
                    .start()
            }
            .start()
    }

    private fun showSeekPreview(position: Long, seekAmount: Long) {
        val seekText = if (seekAmount >= 0) "+${seekAmount/1000}s" else "${seekAmount/1000}s"
        binding.seekOverlayText.text = "$seekText\n${position.formatDuration()}"
    }

    private fun setBrightness(brightness: Float) {
        activity?.window?.attributes = activity?.window?.attributes?.apply {
            screenBrightness = brightness
        }
    }

    private fun setVolume(volume: Float) {
        val audioManager = requireContext().getSystemService(android.content.Context.AUDIO_SERVICE) 
            as android.media.AudioManager
        val volumeLevel = (volume * maxVolume).toInt()
        audioManager.setStreamVolume(
            android.media.AudioManager.STREAM_MUSIC,
            volumeLevel,
            0
        )
    }

    private fun updateBrightnessUI(brightness: Float) {
        binding.brightnessProgress.progress = (brightness * 100).toInt()
        binding.brightnessValue.text = "${(brightness * 100).toInt()}%"
    }

    private fun updateVolumeUI(volume: Float) {
        binding.volumeProgress.progress = (volume * 100).toInt()
        binding.volumeValue.text = "${(volume * 100).toInt()}%"
    }

    private fun animateHideOverlay(view: View) {
        view.animate()
            .alpha(0f)
            .setDuration(200)
            .withEndAction { view.visibility = View.GONE }
            .start()
    }

    private fun toggleLock() {
        isLocked = !isLocked
        binding.lockOverlay.visibility = if (isLocked) View.VISIBLE else View.GONE
        
        if (isLocked) {
            hideControls()
            binding.unlockButton.visibility = View.VISIBLE
        } else {
            binding.unlockButton.visibility = View.GONE
            showControls()
        }
    }

    private fun toggleSpeed() {
        val speeds = floatArrayOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
        val currentIndex = speeds.indexOfFirst { it == currentSpeed }
        val nextIndex = (currentIndex + 1) % speeds.size
        currentSpeed = speeds[nextIndex]
        player?.setPlaybackSpeed(currentSpeed)
        binding.speedButton.text = "${currentSpeed}x"
        
        Toast.makeText(requireContext(), "Speed: ${currentSpeed}x", Toast.LENGTH_SHORT).show()
    }

    private fun showSpeedDialog() {
        val dialogBinding = DialogPlaybackSpeedBinding.inflate(layoutInflater)
        
        val speeds = floatArrayOf(0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f, 2.5f, 3.0f, 4.0f)
        val speedLabels = speeds.map { "${it}x" }.toTypedArray()
        
        var selectedSpeed = currentSpeed
        
        dialogBinding.speedSlider.valueFrom = 0f
        dialogBinding.speedSlider.valueTo = (speeds.size - 1).toFloat()
        dialogBinding.speedSlider.value = speeds.indexOfFirst { it == currentSpeed }.toFloat()
        dialogBinding.speedLabel.text = "${currentSpeed}x"
        
        dialogBinding.speedSlider.addOnChangeListener { _, value, _ ->
            selectedSpeed = speeds[value.toInt()]
            dialogBinding.speedLabel.text = "${selectedSpeed}x"
        }
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Playback Speed")
            .setView(dialogBinding.root)
            .setPositiveButton("Apply") { _, _ ->
                currentSpeed = selectedSpeed
                player?.setPlaybackSpeed(currentSpeed)
                binding.speedButton.text = "${currentSpeed}x"
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showSleepTimerDialog() {
        val dialogBinding = DialogSleepTimerBinding.inflate(layoutInflater)
        
        val options = arrayOf("Off", "5 minutes", "10 minutes", "15 minutes", "30 minutes", "45 minutes", "1 hour")
        val minutes = intArrayOf(0, 5, 10, 15, 30, 45, 60)
        
        var selectedMinutes = 0
        
        dialogBinding.sleepOptions.setOnCheckedChangeListener { _, checkedId ->
            selectedMinutes = when (checkedId) {
                R.id.optionOff -> 0
                R.id.option5min -> 5
                R.id.option10min -> 10
                R.id.option15min -> 15
                R.id.option30min -> 30
                R.id.option45min -> 45
                R.id.option1hour -> 60
                else -> 0
            }
        }
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Sleep Timer")
            .setView(dialogBinding.root)
            .setPositiveButton("Set") { _, _ ->
                if (selectedMinutes > 0) {
                    playerManager.setSleepTimer(selectedMinutes)
                    Toast.makeText(requireContext(), "Sleep timer set for $selectedMinutes minutes", Toast.LENGTH_SHORT).show()
                } else {
                    playerManager.cancelSleepTimer()
                    Toast.makeText(requireContext(), "Sleep timer cancelled", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun toggleFullscreen() {
        val activity = requireActivity()
        
        if (activity.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
            activity.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        } else {
            activity.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun enterPipMode() {
        val aspectRatio = if (player?.videoSize?.width ?: 0 > 0) {
            android.util.Rational(
                player?.videoSize?.width ?: 16,
                player?.videoSize?.height ?: 9
            )
        } else {
            android.util.Rational(16, 9)
        }
        
        val pipParams = PictureInPictureParams.Builder()
            .setAspectRatio(aspectRatio)
            .build()
        
        try {
            requireActivity().enterPictureInPictureMode(pipParams)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "PiP not supported", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateProgress() {
        player?.let {
            val duration = it.duration
            val position = it.currentPosition
            
            binding.duration.text = duration.formatDuration()
            binding.currentPosition.text = position.formatDuration()
            
            if (duration > 0) {
                binding.progressBar.progress = ((position * 100) / duration).toInt()
            }
            
            // Update video info
            val videoSize = it.videoSize
            if (videoSize.width > 0 && videoSize.height > 0) {
                binding.resolution.text = "${videoSize.width}x${videoSize.height}"
            }
        }
    }

    private fun onPlaybackEnded() {
        // Show end state
        binding.playPauseButton.setImageResource(R.drawable.ic_replay)
    }

    override fun onResume() {
        super.onResume()
        startProgressUpdater()
        
        // Keep screen on
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun onPause() {
        super.onPause()
        stopProgressUpdater()
        playerManager.saveCurrentPosition()
        
        activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun startProgressUpdater() {
        handler.postDelayed(object : Runnable {
            override fun run() {
                updateProgress()
                handler.postDelayed(this, 500)
            }
        }, 500)
    }

    private fun stopProgressUpdater() {
        handler.removeCallbacksAndMessages(null)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacksAndMessages(null)
        _binding = null
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode)
        
        if (isInPictureInPictureMode) {
            binding.controlsOverlay.visibility = View.GONE
            hideControls()
        } else {
            showControls()
        }
    }
}
