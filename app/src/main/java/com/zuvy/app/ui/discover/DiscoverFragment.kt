package com.zuvy.app.ui.discover

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.zuvy.app.R
import com.zuvy.app.databinding.FragmentDiscoverBinding
import com.zuvy.app.databinding.ItemDiscoverVideoBinding
import com.zuvy.app.databinding.ItemRadioBinding
import com.zuvy.app.databinding.ItemPodcastBinding
import com.zuvy.app.utils.ToastUtils
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DiscoverFragment : Fragment() {

    private var _binding: FragmentDiscoverBinding? = null
    private val binding get() = _binding!!

    private lateinit var trendingAdapter: TrendingAdapter
    private lateinit var radioAdapter: RadioAdapter
    private lateinit var podcastAdapter: PodcastAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDiscoverBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerViews()
        setupClickListeners()
        loadContent()
    }

    private fun setupRecyclerViews() {
        // Trending
        trendingAdapter = TrendingAdapter { item ->
            onTrendingClick(item)
        }
        binding.trendingRecyclerView.apply {
            adapter = trendingAdapter
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        }

        // Radio
        radioAdapter = RadioAdapter { item ->
            onRadioClick(item)
        }
        binding.radioRecyclerView.apply {
            adapter = radioAdapter
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        }

        // Podcasts
        podcastAdapter = PodcastAdapter { item ->
            onPodcastClick(item)
        }
        binding.podcastRecyclerView.apply {
            adapter = podcastAdapter
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        }
    }

    private fun setupClickListeners() {
        binding.searchBar.setOnClickListener {
            findNavController().navigate(R.id.searchFragment)
        }
        
        binding.seeAllTrending.setOnClickListener {
            ToastUtils.showInfo(requireContext(), "All trending content")
        }
        
        binding.seeAllRadio.setOnClickListener {
            ToastUtils.showInfo(requireContext(), "All radio stations")
        }
        
        binding.seeAllPodcasts.setOnClickListener {
            ToastUtils.showInfo(requireContext(), "All podcasts")
        }
    }

    private fun loadContent() {
        // Load sample data
        val trendingItems = listOf(
            TrendingItem("1", "Trending Video 1", "10K views", null),
            TrendingItem("2", "Trending Video 2", "8.5K views", null),
            TrendingItem("3", "Trending Video 3", "7.2K views", null),
            TrendingItem("4", "Trending Video 4", "6.1K views", null),
            TrendingItem("5", "Trending Video 5", "5.3K views", null)
        )
        trendingAdapter.submitList(trendingItems)

        val radioItems = listOf(
            RadioItem("1", "Pop Hits Radio", "🎵 Top 40", null),
            RadioItem("2", "Chill Vibes", "😌 Relaxing", null),
            RadioItem("3", "Workout Mix", "💪 Energy", null),
            RadioItem("4", "Jazz Lounge", "🎷 Smooth", null)
        )
        radioAdapter.submitList(radioItems)

        val podcastItems = listOf(
            PodcastItem("1", "Tech Talk Daily", "Episode 42", null),
            PodcastItem("2", "True Crime Stories", "Chapter 15", null),
            PodcastItem("3", "Science Explained", "Latest Episode", null),
            PodcastItem("4", "Comedy Hour", "Special Guest", null)
        )
        podcastAdapter.submitList(podcastItems)
    }

    private fun onTrendingClick(item: TrendingItem) {
        ToastUtils.showInfo(requireContext(), "Playing: ${item.title}")
    }

    private fun onRadioClick(item: RadioItem) {
        ToastUtils.showInfo(requireContext(), "Playing: ${item.name}")
    }

    private fun onPodcastClick(item: PodcastItem) {
        ToastUtils.showInfo(requireContext(), "Playing: ${item.name}")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // Data classes
    data class TrendingItem(val id: String, val title: String, val views: String, val thumbnail: String?)
    data class RadioItem(val id: String, val name: String, val genre: String, val artUrl: String?)
    data class PodcastItem(val id: String, val name: String, val episode: String, val artUrl: String?)

    // Adapters
    inner class TrendingAdapter(
        private val onItemClick: (TrendingItem) -> Unit
    ) : RecyclerView.Adapter<TrendingAdapter.ViewHolder>() {

        private val items = mutableListOf<TrendingItem>()

        fun submitList(newItems: List<TrendingItem>) {
            items.clear()
            items.addAll(newItems)
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = ItemDiscoverVideoBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(items[position])
        }

        override fun getItemCount() = items.size

        inner class ViewHolder(private val binding: ItemDiscoverVideoBinding) : 
            RecyclerView.ViewHolder(binding.root) {
            init {
                binding.root.setOnClickListener {
                    val pos = bindingAdapterPosition
                    if (pos != RecyclerView.NO_POSITION) {
                        onItemClick(items[pos])
                    }
                }
            }

            fun bind(item: TrendingItem) {
                binding.titleText.text = item.title
                binding.viewsText.text = item.views
                Glide.with(binding.thumbnail)
                    .load(item.thumbnail)
                    .placeholder(R.drawable.ic_video_placeholder)
                    .centerCrop()
                    .into(binding.thumbnail)
            }
        }
    }

    inner class RadioAdapter(
        private val onItemClick: (RadioItem) -> Unit
    ) : RecyclerView.Adapter<RadioAdapter.ViewHolder>() {

        private val items = mutableListOf<RadioItem>()

        fun submitList(newItems: List<RadioItem>) {
            items.clear()
            items.addAll(newItems)
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = ItemRadioBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(items[position])
        }

        override fun getItemCount() = items.size

        inner class ViewHolder(private val binding: ItemRadioBinding) : 
            RecyclerView.ViewHolder(binding.root) {
            init {
                binding.root.setOnClickListener {
                    val pos = bindingAdapterPosition
                    if (pos != RecyclerView.NO_POSITION) {
                        onItemClick(items[pos])
                    }
                }
            }

            fun bind(item: RadioItem) {
                binding.radioName.text = item.name
                binding.genreText.text = item.genre
                Glide.with(binding.radioArt)
                    .load(item.artUrl)
                    .placeholder(R.drawable.ic_radio)
                    .circleCrop()
                    .into(binding.radioArt)
            }
        }
    }

    inner class PodcastAdapter(
        private val onItemClick: (PodcastItem) -> Unit
    ) : RecyclerView.Adapter<PodcastAdapter.ViewHolder>() {

        private val items = mutableListOf<PodcastItem>()

        fun submitList(newItems: List<PodcastItem>) {
            items.clear()
            items.addAll(newItems)
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = ItemPodcastBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(items[position])
        }

        override fun getItemCount() = items.size

        inner class ViewHolder(private val binding: ItemPodcastBinding) : 
            RecyclerView.ViewHolder(binding.root) {
            init {
                binding.root.setOnClickListener {
                    val pos = bindingAdapterPosition
                    if (pos != RecyclerView.NO_POSITION) {
                        onItemClick(items[pos])
                    }
                }
            }

            fun bind(item: PodcastItem) {
                binding.podcastName.text = item.name
                binding.episodeText.text = item.episode
                Glide.with(binding.podcastArt)
                    .load(item.artUrl)
                    .placeholder(R.drawable.ic_podcast)
                    .centerCrop()
                    .into(binding.podcastArt)
            }
        }
    }
}
