package com.zuvy.app.ui.home.videos

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import com.zuvy.app.databinding.ItemVideoBinding
import com.zuvy.app.ui.home.HomeViewModel
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
            layoutManager = GridLayoutManager(requireContext(), 2)
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
        val spanCount = if (isGridView) 2 else 1
        (binding.videosRecyclerView.layoutManager as GridLayoutManager).spanCount = spanCount
        binding.viewModeButton.setImageResource(
            if (isGridView) R.drawable.ic_list else R.drawable.ic_grid
        )
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

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
            val binding = ItemVideoBinding.inflate(
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
            val binding: ItemVideoBinding
        ) : RecyclerView.ViewHolder(binding.root) {

            init {
                binding.root.setOnClickListener {
                    val position = bindingAdapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        onVideoClick(items[position], binding.thumbnail)
                    }
                }

                binding.moreButton.setOnClickListener {
                    showVideoOptions(items[bindingAdapterPosition])
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
    
    private fun showVideoOptions(video: MediaItem) {
        // Show bottom sheet with options
    }
}
