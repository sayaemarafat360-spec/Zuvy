package com.zuvy.app.ui.home.folders

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.zuvy.app.R
import com.zuvy.app.data.model.MediaItem
import com.zuvy.app.databinding.FragmentFolderBrowserBinding
import com.zuvy.app.databinding.ItemVideoBinding
import com.zuvy.app.ui.home.HomeViewModel
import com.zuvy.app.utils.formatDuration
import com.zuvy.app.utils.formatFileSize
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class FolderBrowserFragment : Fragment() {

    private var _binding: FragmentFolderBrowserBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by activityViewModels()
    private val args: FolderBrowserFragmentArgs by navArgs()
    
    private lateinit var videosAdapter: VideosAdapter
    private var folderVideos: List<MediaItem> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFolderBrowserBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        binding.folderName.text = args.folderName
        binding.folderPath.text = args.folderPath
        
        setupRecyclerView()
        loadVideos()
        setupClickListeners()
    }

    private fun setupRecyclerView() {
        videosAdapter = VideosAdapter { video ->
            navigateToPlayer(video)
        }
        
        binding.videosRecyclerView.apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter = videosAdapter
        }
    }

    private fun loadVideos() {
        folderVideos = viewModel.getVideosByFolder(args.folderPath)
        videosAdapter.submitList(folderVideos)
        
        binding.videoCount.text = "${folderVideos.size} videos"
        
        if (folderVideos.isEmpty()) {
            binding.emptyState.visibility = View.VISIBLE
            binding.videosRecyclerView.visibility = View.GONE
        } else {
            binding.emptyState.visibility = View.GONE
            binding.videosRecyclerView.visibility = View.VISIBLE
        }
    }

    private fun setupClickListeners() {
        binding.backButton.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
        
        binding.sortButton.setOnClickListener {
            // Show sort options
        }
    }

    private fun navigateToPlayer(video: MediaItem) {
        val direction = FolderBrowserFragmentDirections.actionFolderBrowserToVideoPlayer(
            videoUri = video.uri.toString(),
            videoName = video.name
        )
        
        findNavController().navigate(direction)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    inner class VideosAdapter(
        private val onVideoClick: (MediaItem) -> Unit
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

        inner class VideoViewHolder(
            private val binding: ItemVideoBinding
        ) : RecyclerView.ViewHolder(binding.root) {

            init {
                binding.root.setOnClickListener {
                    val position = bindingAdapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        onVideoClick(items[position])
                    }
                }
            }

            fun bind(item: MediaItem) {
                binding.title.text = item.name
                binding.duration.text = item.duration.formatDuration()
                binding.size.text = item.size.formatFileSize()
                binding.resolution.text = "${item.width}x${item.height}"

                Glide.with(binding.thumbnail)
                    .load(item.uri)
                    .placeholder(R.drawable.ic_video_placeholder)
                    .centerCrop()
                    .into(binding.thumbnail)
            }
        }
    }
}
