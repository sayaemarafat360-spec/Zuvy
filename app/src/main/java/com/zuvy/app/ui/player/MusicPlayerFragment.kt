package com.zuvy.app.ui.player

import android.animation.ValueAnimator
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.media3.common.Player
import androidx.navigation.fragment.findNavController
import androidx.palette.graphics.Palette
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.zuvy.app.R
import com.zuvy.app.databinding.FragmentMusicPlayerBinding
import com.zuvy.app.databinding.ItemQueueBinding
import com.zuvy.app.player.MediaType
import com.zuvy.app.player.PlayerManager
import com.zuvy.app.player.QueueItem
import com.zuvy.app.player.RepeatMode
import com.zuvy.app.utils.formatDuration
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.abs

@AndroidEntryPoint
class MusicPlayerFragment : Fragment() {

    private var _binding: FragmentMusicPlayerBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var playerManager: PlayerManager

    private val viewModel: MusicPlayerViewModel by viewModels()
    
    private var isExpanded = true
    private var isFavorite = false
    private var dominantColor = Color.parseColor("#6C63FF")
    
    private lateinit var queueAdapter: QueueAdapter
    
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMusicPlayerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupQueueAdapter()
        setupClickListeners()
        setupSwipeToDismiss()
        observeState()
        updateProgress()
    }

    private fun setupQueueAdapter() {
        queueAdapter = QueueAdapter(
            onItemClick = { position ->
                playerManager.playFromQueue(position)
            },
            onRemoveClick = { position ->
                playerManager.removeFromQueue(position)
            },
            onReorder = { from, to ->
                playerManager.moveQueueItem(from, to)
            }
        )
        
        binding.queueRecyclerView.adapter = queueAdapter
        
        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN,
            ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val from = viewHolder.bindingAdapterPosition
                val to = target.bindingAdapterPosition
                queueAdapter.notifyItemMoved(from, to)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                playerManager.removeFromQueue(viewHolder.bindingAdapterPosition)
            }

            override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
                super.onSelectedChanged(viewHolder, actionState)
                if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
                    viewHolder?.itemView?.alpha = 0.7f
                }
            }

            override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
                super.clearView(recyclerView, viewHolder)
                viewHolder.itemView.alpha = 1f
                // Notify adapter to update positions
                val queue = queueAdapter.currentList.mapIndexed { index, item ->
                    item to index
                }
            }
        }).attachToRecyclerView(binding.queueRecyclerView)
    }

    private fun setupClickListeners() {
        binding.backButton.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.playPauseButton.setOnClickListener {
            playerManager.playPause()
            animatePlayPause()
        }

        binding.nextButton.setOnClickListener {
            playerManager.playNext()
        }

        binding.previousButton.setOnClickListener {
            playerManager.playPrevious()
        }

        binding.repeatButton.setOnClickListener {
            playerManager.toggleRepeatMode()
        }

        binding.shuffleButton.setOnClickListener {
            playerManager.toggleShuffle()
        }

        binding.favoriteButton.setOnClickListener {
            toggleFavorite()
        }

        binding.sleepButton.setOnClickListener {
            showSleepTimerDialog()
        }

        binding.speedButton.setOnClickListener {
            showSpeedDialog()
        }

        binding.progressBar.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    val duration = playerManager.duration.value
                    if (duration > 0) {
                        val position = (duration * progress / 100)
                        playerManager.seekTo(position)
                    }
                }
            }
            override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {}
        })

        binding.toggleQueue.setOnClickListener {
            toggleQueue()
        }

        // Gesture controls on album art
        binding.albumArt.setOnTouchListener { _, event ->
            when (event.action) {
                android.view.MotionEvent.ACTION_DOWN -> true
                android.view.MotionEvent.ACTION_UP -> {
                    playerManager.playPause()
                    animatePlayPause()
                    true
                }
                else -> false
            }
        }
    }

    private fun setupSwipeToDismiss() {
        // Can be used for swipe to dismiss the player
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Observe current media
                launch {
                    playerManager.currentMedia.collect { media ->
                        media?.let {
                            binding.songTitle.text = it.name
                            loadAlbumArt(it.uri)
                        }
                    }
                }

                // Observe playing state
                launch {
                    playerManager.isPlaying.collect { isPlaying ->
                        updatePlayPauseButton(isPlaying)
                    }
                }

                // Observe position
                launch {
                    playerManager.currentPosition.collect { position ->
                        binding.currentPosition.text = position.formatDuration()
                        val duration = playerManager.duration.value
                        if (duration > 0) {
                            binding.progressBar.progress = ((position * 100) / duration).toInt()
                        }
                    }
                }

                // Observe duration
                launch {
                    playerManager.duration.collect { duration ->
                        binding.duration.text = duration.formatDuration()
                    }
                }

                // Observe repeat mode
                launch {
                    playerManager.repeatMode.collect { mode ->
                        updateRepeatButton(mode)
                    }
                }

                // Observe shuffle
                launch {
                    playerManager.isShuffled.collect { shuffled ->
                        updateShuffleButton(shuffled)
                    }
                }

                // Observe queue
                launch {
                    playerManager.queue.collect { queue ->
                        queueAdapter.submitList(queue)
                        binding.queueCount.text = "${queue.size} songs"
                    }
                }

                // Observe queue index
                launch {
                    playerManager.queueIndex.collect { index ->
                        queueAdapter.setCurrentPlaying(index)
                    }
                }

                // Observe sleep timer
                launch {
                    playerManager.sleepTimerRemaining.collect { remaining ->
                        remaining?.let {
                            val minutes = it / 60000
                            val seconds = (it % 60000) / 1000
                            binding.sleepTimer.text = String.format("%02d:%02d", minutes, seconds)
                            binding.sleepTimer.visibility = View.VISIBLE
                        } ?: run {
                            binding.sleepTimer.visibility = View.GONE
                        }
                    }
                }
            }
        }
    }

    private fun loadAlbumArt(uri: Uri) {
        Glide.with(this)
            .asBitmap()
            .load(uri)
            .placeholder(R.drawable.ic_music_note)
            .error(R.drawable.ic_music_note)
            .into(object : CustomTarget<Bitmap>() {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    binding.albumArt.setImageBitmap(resource)
                    extractColors(resource)
                }

                override fun onLoadCleared(placeholder: Drawable?) {
                    binding.albumArt.setImageDrawable(placeholder)
                }
            })
    }

    private fun extractColors(bitmap: Bitmap) {
        Palette.from(bitmap).generate { palette ->
            palette?.let {
                val vibrantColor = it.vibrantSwatch?.rgb
                    ?: it.dominantSwatch?.rgb
                    ?: it.darkVibrantSwatch?.rgb
                    ?: Color.parseColor("#6C63FF")
                
                dominantColor = vibrantColor
                applyDynamicColors(vibrantColor)
            }
        }
    }

    private fun applyDynamicColors(color: Int) {
        // Animate background color
        val colorAnim = ValueAnimator.ofArgb(binding.root.solidColor, color)
        colorAnim.duration = 500
        colorAnim.interpolator = AccelerateDecelerateInterpolator()
        colorAnim.addUpdateListener {
            // Apply subtle color tint to background
            val bgColor = darkenColor(color, 0.8f)
            binding.root.setBackgroundColor(bgColor)
        }
        colorAnim.start()
        
        // Update button tints
        binding.playPauseButton.setColorFilter(color)
        binding.progressBar.progressTintList = android.content.res.ColorStateList.valueOf(color)
        binding.progressBar.thumbTintList = android.content.res.ColorStateList.valueOf(color)
    }

    private fun darkenColor(color: Int, factor: Float): Int {
        val r = (Color.red(color) * factor).toInt()
        val g = (Color.green(color) * factor).toInt()
        val b = (Color.blue(color) * factor).toInt()
        return Color.rgb(r, g, b)
    }

    private fun updatePlayPauseButton(isPlaying: Boolean) {
        val iconRes = if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
        binding.playPauseButton.setImageResource(iconRes)
    }

    private fun animatePlayPause() {
        binding.playPauseButton.animate()
            .scaleX(1.2f)
            .scaleY(1.2f)
            .setDuration(100)
            .withEndAction {
                binding.playPauseButton.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(100)
                    .start()
            }
            .start()
    }

    private fun updateRepeatButton(mode: RepeatMode) {
        val (iconRes, tint) = when (mode) {
            RepeatMode.OFF -> R.drawable.ic_replay to Color.GRAY
            RepeatMode.ALL -> R.drawable.ic_replay to dominantColor
            RepeatMode.ONE -> R.drawable.ic_replay to dominantColor
        }
        
        binding.repeatButton.setImageResource(iconRes)
        binding.repeatButton.setColorFilter(tint)
        
        // Show indicator for repeat one
        binding.repeatOneIndicator.visibility = if (mode == RepeatMode.ONE) View.VISIBLE else View.GONE
    }

    private fun updateShuffleButton(shuffled: Boolean) {
        val tint = if (shuffled) dominantColor else Color.GRAY
        binding.shuffleButton.setColorFilter(tint)
    }

    private fun toggleFavorite() {
        isFavorite = !isFavorite
        
        // Animate heart
        binding.favoriteButton.animate()
            .scaleX(0.8f)
            .scaleY(0.8f)
            .setDuration(100)
            .withEndAction {
                val iconRes = if (isFavorite) R.drawable.ic_heart else R.drawable.ic_heart
                binding.favoriteButton.setImageResource(iconRes)
                binding.favoriteButton.setColorFilter(
                    if (isFavorite) Color.RED else Color.GRAY
                )
                
                binding.favoriteButton.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(100)
                    
                if (isFavorite) {
                    // Show burst animation
                    showFavoriteAnimation()
                }
            }
            .start()
        
        // Save to favorites
        playerManager.currentMedia.value?.let { media ->
            viewModel.toggleFavorite(media.uri.toString(), media.name)
        }
    }

    private fun showFavoriteAnimation() {
        // Could add Lottie animation here
        Toast.makeText(requireContext(), "Added to favorites", Toast.LENGTH_SHORT).show()
    }

    private fun toggleQueue() {
        isExpanded = !isExpanded
        
        if (isExpanded) {
            binding.queuePanel.visibility = View.VISIBLE
            binding.queuePanel.animate()
                .translationY(0f)
                .alpha(1f)
                .setDuration(300)
                .start()
        } else {
            binding.queuePanel.animate()
                .translationY(binding.queuePanel.height.toFloat())
                .alpha(0f)
                .setDuration(300)
                .withEndAction {
                    binding.queuePanel.visibility = View.GONE
                }
                .start()
        }
    }

    private fun showSleepTimerDialog() {
        val options = arrayOf("Off", "5 min", "10 min", "15 min", "30 min", "45 min", "1 hour")
        val minutes = intArrayOf(0, 5, 10, 15, 30, 45, 60)
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Sleep Timer")
            .setSingleChoiceItems(options, 0) { dialog, which ->
                if (minutes[which] > 0) {
                    playerManager.setSleepTimer(minutes[which])
                    Toast.makeText(requireContext(), "Sleep timer set for ${minutes[which]} minutes", Toast.LENGTH_SHORT).show()
                } else {
                    playerManager.cancelSleepTimer()
                    Toast.makeText(requireContext(), "Sleep timer cancelled", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
            .show()
    }

    private fun showSpeedDialog() {
        val speeds = floatArrayOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
        val speedLabels = speeds.map { "${it}x" }.toTypedArray()
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Playback Speed")
            .setSingleChoiceItems(speedLabels, speeds.indexOf(1.0f)) { dialog, which ->
                playerManager.setPlaybackSpeed(speeds[which])
                binding.speedButton.text = "${speeds[which]}x"
                dialog.dismiss()
            }
            .show()
    }

    private fun updateProgress() {
        handler.postDelayed(object : Runnable {
            override fun run() {
                playerManager.currentMedia.value?.let {
                    val position = playerManager.currentPosition.value
                    val duration = playerManager.duration.value
                    
                    binding.currentPosition.text = position.formatDuration()
                    binding.duration.text = duration.formatDuration()
                    
                    if (duration > 0) {
                        binding.progressBar.progress = ((position * 100) / duration).toInt()
                    }
                }
                handler.postDelayed(this, 500)
            }
        }, 500)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacksAndMessages(null)
        _binding = null
    }

    // Queue Adapter
    inner class QueueAdapter(
        private val onItemClick: (Int) -> Unit,
        private val onRemoveClick: (Int) -> Unit,
        private val onReorder: (Int, Int) -> Unit
    ) : RecyclerView.Adapter<QueueAdapter.ViewHolder>() {

        private val items = mutableListOf<QueueItem>()
        private var currentPlaying = -1

        fun submitList(newItems: List<QueueItem>) {
            items.clear()
            items.addAll(newItems)
            notifyDataSetChanged()
        }

        fun setCurrentPlaying(index: Int) {
            val oldIndex = currentPlaying
            currentPlaying = index
            notifyItemChanged(oldIndex)
            notifyItemChanged(currentPlaying)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = ItemQueueBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(items.getOrNull(position) ?: return, position == currentPlaying)
        }

        override fun getItemCount(): Int = items.size

        inner class ViewHolder(
            private val binding: ItemQueueBinding
        ) : RecyclerView.ViewHolder(binding.root) {

            init {
                binding.root.setOnClickListener {
                    onItemClick(bindingAdapterPosition)
                }
                binding.removeButton.setOnClickListener {
                    onRemoveClick(bindingAdapterPosition)
                }
            }

            fun bind(item: QueueItem, isPlaying: Boolean) {
                binding.title.text = item.name
                
                if (isPlaying) {
                    binding.title.setTextColor(dominantColor)
                    binding.playingIndicator.visibility = View.VISIBLE
                } else {
                    binding.title.setTextColor(Color.WHITE)
                    binding.playingIndicator.visibility = View.GONE
                }

                Glide.with(binding.thumbnail)
                    .load(item.uri)
                    .placeholder(R.drawable.ic_music_note)
                    .into(binding.thumbnail)
            }
        }
    }
}
