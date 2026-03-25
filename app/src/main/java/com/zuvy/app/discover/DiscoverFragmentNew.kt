package com.zuvy.app.discover

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.google.android.material.chip.Chip
import com.zuvy.app.R
import com.zuvy.app.databinding.FragmentDiscoverBinding
import com.zuvy.app.network.NetworkManager
import com.zuvy.app.utils.ToastUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class DiscoverFragmentNew : Fragment() {

    private var _binding: FragmentDiscoverBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var onlineContentManager: OnlineContentManager
    
    @Inject
    lateinit var networkManager: NetworkManager
    
    private val viewModel: DiscoverViewModel by viewModels()
    
    private lateinit var radioAdapter: RadioStationAdapter
    private lateinit var podcastAdapter: PodcastAdapter
    private lateinit var musicAdapter: FreeMusicAdapter
    
    private var selectedCategory = "all"

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
        
        checkNetworkAndLoad()
        setupRecyclerViews()
        setupCategoryChips()
        setupSwipeRefresh()
        setupClickListeners()
        observeState()
    }
    
    private fun checkNetworkAndLoad() {
        if (!networkManager.isOnline()) {
            showOfflineState()
            return
        }
        
        loadContent()
    }
    
    private fun showOfflineState() {
        binding.offlineLayout.visibility = View.VISIBLE
        binding.contentLayout.visibility = View.GONE
        
        binding.retryButton.setOnClickListener {
            if (networkManager.isOnline()) {
                binding.offlineLayout.visibility = View.GONE
                binding.contentLayout.visibility = View.VISIBLE
                loadContent()
            } else {
                ToastUtils.showWarning(requireContext(), "No internet connection")
            }
        }
    }
    
    private fun loadContent() {
        viewLifecycleOwner.lifecycleScope.launch {
            // Load radio stations
            launch {
                val stations = onlineContentManager.getTopRadioStations(15)
                radioAdapter.submitList(stations)
            }
            
            // Load podcasts
            launch {
                val podcasts = onlineContentManager.getTopPodcasts(limit = 15)
                podcastAdapter.submitList(podcasts)
            }
            
            // Load free music
            launch {
                val tracks = onlineContentManager.getFreeMusicTracks(15)
                musicAdapter.submitList(tracks)
            }
        }
    }

    private fun setupRecyclerViews() {
        // Radio stations
        radioAdapter = RadioStationAdapter { station ->
            playRadioStation(station)
        }
        binding.radioRecyclerView.apply {
            adapter = radioAdapter
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        }
        
        // Podcasts
        podcastAdapter = PodcastAdapter { podcast ->
            openPodcastDetail(podcast)
        }
        binding.podcastsRecyclerView.apply {
            adapter = podcastAdapter
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        }
        
        // Free Music
        musicAdapter = FreeMusicAdapter { track ->
            playFreeMusic(track)
        }
        binding.freeMusicRecyclerView.apply {
            adapter = musicAdapter
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        }
    }
    
    private fun setupCategoryChips() {
        val categories = listOf("All", "Pop", "Rock", "Jazz", "Electronic", "Hip Hop", "Classical")
        
        categories.forEach { category ->
            val chip = Chip(requireContext()).apply {
                text = category
                isCheckable = true
                isChecked = category == "All"
                setChipBackgroundColorResource(R.color.chip_selector)
                setTextColor(resources.getColorStateList(R.color.chip_text_selector, null))
                
                setOnClickListener {
                    selectedCategory = category.lowercase()
                    filterByCategory(selectedCategory)
                }
            }
            binding.categoryChipGroup.addView(chip)
        }
    }
    
    private fun filterByCategory(category: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            when (category) {
                "all" -> {
                    radioAdapter.submitList(onlineContentManager.getTopRadioStations(15))
                    musicAdapter.submitList(onlineContentManager.getFreeMusicTracks(15))
                }
                else -> {
                    radioAdapter.submitList(onlineContentManager.getRadioStationsByTag(category, 15))
                    musicAdapter.submitList(onlineContentManager.getFreeMusicByGenre(category, 15))
                }
            }
        }
    }
    
    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            if (networkManager.isOnline()) {
                loadContent()
            } else {
                showOfflineState()
            }
            binding.swipeRefresh.isRefreshing = false
        }
    }

    private fun setupClickListeners() {
        binding.searchBar.setOnClickListener {
            findNavController().navigate(R.id.searchFragment)
        }
        
        binding.seeAllRadio.setOnClickListener {
            // Open all radio stations
            ToastUtils.showInfo(requireContext(), "All radio stations")
        }
        
        binding.seeAllPodcasts.setOnClickListener {
            ToastUtils.showInfo(requireContext(), "All podcasts")
        }
        
        binding.seeAllMusic.setOnClickListener {
            ToastUtils.showInfo(requireContext(), "All free music")
        }
    }
    
    private fun observeState() {
        // Observe network state
        viewLifecycleOwner.lifecycleScope.launch {
            networkManager.observeNetworkState().collect { state ->
                when (state) {
                    is NetworkManager.NetworkState.Lost,
                    is NetworkManager.NetworkState.Unavailable -> {
                        showOfflineState()
                    }
                    is NetworkManager.NetworkState.Available -> {
                        binding.offlineLayout.visibility = View.GONE
                        binding.contentLayout.visibility = View.VISIBLE
                        loadContent()
                    }
                    else -> {}
                }
            }
        }
    }
    
    private fun playRadioStation(station: RadioStation) {
        ToastUtils.showInfo(requireContext(), "Playing: ${station.name}")
        // TODO: Open radio player with station
    }
    
    private fun openPodcastDetail(podcast: Podcast) {
        ToastUtils.showInfo(requireContext(), "Opening: ${podcast.name}")
        // TODO: Open podcast detail
    }
    
    private fun playFreeMusic(track: MusicTrack) {
        ToastUtils.showInfo(requireContext(), "Playing: ${track.name}")
        // TODO: Play free music track
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    
    // ============ ADAPTERS ============
    
    inner class RadioStationAdapter(
        private val onItemClick: (RadioStation) -> Unit
    ) : androidx.recyclerview.widget.ListAdapter<RadioStation, RadioStationAdapter.ViewHolder>(
        DiffCallback<RadioStation> { old, new -> old.stationUuid == new.stationUuid }
    ) {
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_radio, parent, false)
            return ViewHolder(view)
        }
        
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(getItem(position))
        }
        
        inner class ViewHolder(itemView: View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(itemView) {
            init {
                itemView.setOnClickListener { 
                    if (bindingAdapterPosition != androidx.recyclerview.widget.RecyclerView.NO_POSITION) {
                        onItemClick(getItem(bindingAdapterPosition))
                    }
                }
            }
            
            fun bind(station: RadioStation) {
                // Bind station data
                itemView.findViewById<android.widget.TextView>(R.id.radioName)?.text = station.name
                itemView.findViewById<android.widget.TextView>(R.id.genreText)?.text = station.tags?.split(",")?.firstOrNull() ?: ""
                
                itemView.findViewById<android.widget.ImageView>(R.id.radioArt)?.let { imageView ->
                    Glide.with(imageView)
                        .load(station.favicon)
                        .placeholder(R.drawable.ic_radio)
                        .circleCrop()
                        .into(imageView)
                }
            }
        }
    }
    
    inner class PodcastAdapter(
        private val onItemClick: (Podcast) -> Unit
    ) : androidx.recyclerview.widget.ListAdapter<Podcast, PodcastAdapter.ViewHolder>(
        DiffCallback<Podcast> { old, new -> old.id == new.id }
    ) {
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_podcast, parent, false)
            return ViewHolder(view)
        }
        
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(getItem(position))
        }
        
        inner class ViewHolder(itemView: View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(itemView) {
            init {
                itemView.setOnClickListener {
                    if (bindingAdapterPosition != androidx.recyclerview.widget.RecyclerView.NO_POSITION) {
                        onItemClick(getItem(bindingAdapterPosition))
                    }
                }
            }
            
            fun bind(podcast: Podcast) {
                itemView.findViewById<android.widget.TextView>(R.id.podcastName)?.text = podcast.name
                itemView.findViewById<android.widget.TextView>(R.id.episodeText)?.text = podcast.artistName
                
                itemView.findViewById<android.widget.ImageView>(R.id.podcastArt)?.let { imageView ->
                    Glide.with(imageView)
                        .load(podcast.artworkUrlLarge ?: podcast.artworkUrl)
                        .placeholder(R.drawable.ic_podcast)
                        .centerCrop()
                        .into(imageView)
                }
            }
        }
    }
    
    inner class FreeMusicAdapter(
        private val onItemClick: (MusicTrack) -> Unit
    ) : androidx.recyclerview.widget.ListAdapter<MusicTrack, FreeMusicAdapter.ViewHolder>(
        DiffCallback<MusicTrack> { old, new -> old.id == new.id }
    ) {
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_music_card, parent, false)
            return ViewHolder(view)
        }
        
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(getItem(position))
        }
        
        inner class ViewHolder(itemView: View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(itemView) {
            init {
                itemView.setOnClickListener {
                    if (bindingAdapterPosition != androidx.recyclerview.widget.RecyclerView.NO_POSITION) {
                        onItemClick(getItem(bindingAdapterPosition))
                    }
                }
            }
            
            fun bind(track: MusicTrack) {
                itemView.findViewById<android.widget.TextView>(R.id.titleText)?.text = track.name
                itemView.findViewById<android.widget.TextView>(R.id.artistText)?.text = track.artistName
                
                itemView.findViewById<android.widget.ImageView>(R.id.artwork)?.let { imageView ->
                    Glide.with(imageView)
                        .load(track.imageUrl)
                        .placeholder(R.drawable.ic_music_note)
                        .centerCrop()
                        .into(imageView)
                }
            }
        }
    }
    
    // Diff callback helper
    class DiffCallback<T>(
        private val idComparator: (T, T) -> Boolean
    ) : androidx.recyclerview.widget.DiffUtil.ItemCallback<T>() {
        override fun areItemsTheSame(oldItem: T, newItem: T): Boolean = idComparator(oldItem, newItem)
        override fun areContentsTheSame(oldItem: T, newItem: T): Boolean = oldItem == newItem
    }
}
