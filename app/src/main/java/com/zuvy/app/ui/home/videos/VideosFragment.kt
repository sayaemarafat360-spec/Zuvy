package com.zuvy.app.ui.home.videos

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.ActionMode
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.core.view.doOnPreDraw
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.transition.MaterialElevationScale
import com.zuvy.app.R
import com.zuvy.app.data.model.MediaItem
import com.zuvy.app.databinding.FragmentVideosBinding
import com.zuvy.app.databinding.ItemVideoCompactBinding
import com.zuvy.app.ui.home.HomeViewModel
import com.zuvy.app.utils.ToastUtils
import com.zuvy.app.utils.formatDuration
import com.zuvy.app.utils.formatFileSize
import com.bumptech.glide.Glide
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class VideosFragment : Fragment() {

    private var _binding: FragmentVideosBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by activityViewModels()
    private lateinit var videosAdapter: VideosAdapter
    private var isGridView = true
    
    // Multi-select state
    private var actionMode: ActionMode? = null
    private val selectedItems = mutableSetOf<MediaItem>()
    private var isMultiSelectMode = false
    
    // Double tap detection
    private var lastTapTime = 0L
    private var lastTappedItem: MediaItem? = null
    private val doubleTapTimeout = 300L

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVideosBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupClickListeners()
        observeData()
        postponeEnterTransition()
        view.doOnPreDraw { startPostponedEnterTransition() }
    }

    private fun setupRecyclerView() {
        videosAdapter = VideosAdapter { video, thumbnail ->
            navigateToPlayer(video, thumbnail)
        }
        
        binding.videosRecyclerView.apply {
            // Use LinearLayoutManager for compact list view
            layoutManager = androidx.recyclerview.widget.LinearLayoutManager(requireContext())
            adapter = videosAdapter
            setHasFixedSize(true)
            setItemViewCacheSize(20)
        }
    }

    private fun setupClickListeners() {
        binding.sortButton.setOnClickListener {
            showSortDialog()
        }

        binding.viewModeButton.setOnClickListener {
            toggleViewMode()
        }
        
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.loadMedia()
            binding.swipeRefresh.isRefreshing = false
        }
    }

    private fun toggleViewMode() {
        isGridView = !isGridView
        if (isGridView) {
            binding.videosRecyclerView.layoutManager = GridLayoutManager(requireContext(), 2)
            binding.viewModeButton.setImageResource(R.drawable.ic_list)
        } else {
            binding.videosRecyclerView.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(requireContext())
            binding.viewModeButton.setImageResource(R.drawable.ic_grid)
        }
    }

    private fun showSortDialog() {
        // Show sort options
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.isLoading.collect { isLoading ->
                        if (isLoading) {
                            binding.shimmerLayout.startShimmer()
                            binding.shimmerLayout.visibility = View.VISIBLE
                            binding.videosRecyclerView.visibility = View.GONE
                        } else {
                            binding.shimmerLayout.stopShimmer()
                            binding.shimmerLayout.visibility = View.GONE
                            binding.videosRecyclerView.visibility = View.VISIBLE
                        }
                    }
                }

                launch {
                    viewModel.videos.collect { videos ->
                        if (videos.isEmpty()) {
                            binding.emptyState.visibility = View.VISIBLE
                            binding.videosRecyclerView.visibility = View.GONE
                        } else {
                            binding.emptyState.visibility = View.GONE
                            binding.videosRecyclerView.visibility = View.VISIBLE
                            videosAdapter.submitList(videos)
                        }
                    }
                }
            }
        }
    }

    private fun navigateToPlayer(video: MediaItem, thumbnail: View) {
        exitTransition = MaterialElevationScale(false).apply {
            duration = 300
        }
        reenterTransition = MaterialElevationScale(true).apply {
            duration = 300
        }

        val extras = FragmentNavigatorExtras(thumbnail to "video_thumbnail_${video.id}")
        
        // Navigate directly to video player with arguments
        val bundle = Bundle().apply {
            putString("videoUri", video.uri.toString())
            putString("videoName", video.name)
        }
        
        findNavController().navigate(R.id.videoPlayerFragment, bundle, null, extras)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    inner class VideosAdapter(
        private val onVideoClick: (MediaItem, View) -> Unit
    ) : RecyclerView.Adapter<VideosAdapter.VideoViewHolder>() {

        private val items = mutableListOf<MediaItem>()

        fun submitList(newItems: List<MediaItem>) {
            items.clear()
            items.addAll(newItems)
            notifyDataSetChanged()
        }
        
        fun getItems(): List<MediaItem> = items.toList()

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
            val binding = ItemVideoCompactBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            return VideoViewHolder(binding)
        }

        override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
            holder.bind(items[position])
        }

        override fun getItemCount(): Int = items.size

        override fun onViewRecycled(holder: VideoViewHolder) {
            super.onViewRecycled(holder)
            // Clear Glide request
            Glide.with(holder.itemView.context).clear(holder.binding.thumbnail)
        }

        inner class VideoViewHolder(
            val binding: ItemVideoCompactBinding
        ) : RecyclerView.ViewHolder(binding.root) {

            init {
                // Single tap with double-tap detection
                binding.root.setOnClickListener {
                    val position = bindingAdapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        val item = items[position]
                        
                        if (isMultiSelectMode) {
                            toggleSelection(item)
                        } else {
                            // Double tap detection
                            val now = System.currentTimeMillis()
                            if (lastTappedItem == item && now - lastTapTime < doubleTapTimeout) {
                                // Double tap - Add to favorites
                                onDoubleTap(item)
                                lastTapTime = 0L
                                lastTappedItem = null
                            } else {
                                // Single tap - wait for potential double tap
                                lastTapTime = now
                                lastTappedItem = item
                                Handler(Looper.getMainLooper()).postDelayed({
                                    if (lastTappedItem == item) {
                                        onVideoClick(item, binding.thumbnail)
                                    }
                                }, doubleTapTimeout)
                            }
                        }
                    }
                }
                
                // Long press - Enter multi-select mode or show options
                binding.root.setOnLongClickListener {
                    val position = bindingAdapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        val item = items[position]
                        if (!isMultiSelectMode) {
                            startMultiSelect(item)
                        } else {
                            toggleSelection(item)
                        }
                        true
                    } else {
                        false
                    }
                }

                binding.moreButton.setOnClickListener {
                    if (!isMultiSelectMode) {
                        showVideoOptions(items[bindingAdapterPosition])
                    }
                }
            }

            fun bind(item: MediaItem) {
                binding.title.text = item.name
                binding.duration.text = item.duration.formatDuration()
                binding.size.text = item.size.formatFileSize()
                
                if (item.width > 0 && item.height > 0) {
                    binding.resolution.text = "${item.width}x${item.height}"
                    binding.resolution.visibility = View.VISIBLE
                } else {
                    binding.resolution.visibility = View.GONE
                }
                
                // Selection state visual feedback
                val isSelected = selectedItems.contains(item)
                binding.root.isSelected = isSelected
                binding.selectionOverlay.visibility = if (isSelected) View.VISIBLE else View.GONE
                binding.checkbox.isChecked = isSelected
                binding.checkbox.visibility = if (isMultiSelectMode) View.VISIBLE else View.GONE

                // Set transition name
                binding.thumbnail.transitionName = "video_thumbnail_${item.id}"

                // Load thumbnail with Glide
                Glide.with(binding.thumbnail.context)
                    .load(item.uri)
                    .placeholder(R.drawable.ic_video_placeholder)
                    .centerCrop()
                    .into(binding.thumbnail)
            }
        }
    }
    
    // Double tap action - Add to favorites
    private fun onDoubleTap(item: MediaItem) {
        ToastUtils.showSuccess(requireContext(), "❤️ Added to favorites: ${item.name}")
        // Animate heart
        binding.root.animate()
            .scaleX(0.9f)
            .scaleY(0.9f)
            .setDuration(100)
            .withEndAction {
                binding.root.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(100)
                    .start()
            }
            .start()
    }
    
    // Multi-select functionality
    private fun startMultiSelect(item: MediaItem) {
        isMultiSelectMode = true
        selectedItems.clear()
        selectedItems.add(item)
        actionMode = requireActivity().startActionMode(actionModeCallback)
        videosAdapter.notifyDataSetChanged()
    }
    
    private fun toggleSelection(item: MediaItem) {
        if (selectedItems.contains(item)) {
            selectedItems.remove(item)
            if (selectedItems.isEmpty()) {
                exitMultiSelect()
            }
        } else {
            selectedItems.add(item)
        }
        actionMode?.title = "${selectedItems.size} selected"
        videosAdapter.notifyDataSetChanged()
    }
    
    private fun exitMultiSelect() {
        isMultiSelectMode = false
        selectedItems.clear()
        actionMode?.finish()
        actionMode = null
        videosAdapter.notifyDataSetChanged()
    }
    
    private val actionModeCallback = object : ActionMode.Callback {
        override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
            mode?.menuInflater?.inflate(R.menu.menu_multi_select, menu)
            mode?.title = "1 selected"
            return true
        }

        override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
            return false
        }

        override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
            return when (item?.itemId) {
                R.id.action_share -> {
                    shareSelectedItems()
                    true
                }
                R.id.action_delete -> {
                    deleteSelectedItems()
                    true
                }
                R.id.action_add_playlist -> {
                    addToPlaylistSelected()
                    true
                }
                R.id.action_select_all -> {
                    selectAll()
                    true
                }
                else -> false
            }
        }

        override fun onDestroyActionMode(mode: ActionMode?) {
            isMultiSelectMode = false
            selectedItems.clear()
            videosAdapter.notifyDataSetChanged()
        }
    }
    
    private fun shareSelectedItems() {
        ToastUtils.showInfo(requireContext(), "Sharing ${selectedItems.size} videos...")
        exitMultiSelect()
    }
    
    private fun deleteSelectedItems() {
        ToastUtils.showWarning(requireContext(), "Delete ${selectedItems.size} videos?")
        exitMultiSelect()
    }
    
    private fun addToPlaylistSelected() {
        ToastUtils.showSuccess(requireContext(), "Added ${selectedItems.size} videos to playlist")
        exitMultiSelect()
    }
    
    private fun selectAll() {
        selectedItems.clear()
        selectedItems.addAll(videosAdapter.getItems())
        actionMode?.title = "${selectedItems.size} selected"
        videosAdapter.notifyDataSetChanged()
    }
    
    private fun showVideoOptions(video: MediaItem) {
        val bottomSheet = com.zuvy.app.ui.components.MediaOptionsBottomSheet.newInstance(
            mediaItem = video,
            onPlayNext = {
                // Add to play next in queue
                ToastUtils.showSuccess(requireContext(), "Added to play next ⏭️")
            },
            onAddToPlaylist = {
                // Show playlist picker
                ToastUtils.showInfo(requireContext(), "Playlist picker coming soon")
            },
            onAddToFavorites = {
                // Add to favorites
                ToastUtils.showSuccess(requireContext(), "Added to favorites ❤️")
            }
        )
        bottomSheet.show(childFragmentManager, "media_options")
    }
}
