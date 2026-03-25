package com.zuvy.app.ui.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.view.inputmethod.EditorInfo
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.zuvy.app.R
import com.zuvy.app.databinding.FragmentSearchBinding
import com.zuvy.app.databinding.ItemSearchResultBinding
import com.zuvy.app.databinding.ItemSearchHistoryBinding
import com.zuvy.app.databinding.ItemSearchSuggestionBinding
import com.zuvy.app.search.SearchFilters
import com.zuvy.app.search.SearchResults
import com.zuvy.app.search.SearchViewModel
import com.zuvy.app.utils.ToastUtils
import com.zuvy.app.utils.formatDuration
import com.zuvy.app.utils.formatFileSize
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SearchFragment : Fragment() {

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: SearchViewModel by viewModels()
    
    private lateinit var resultsAdapter: SearchResultsAdapter
    private lateinit var historyAdapter: SearchHistoryAdapter
    private lateinit var suggestionsAdapter: SearchSuggestionsAdapter
    
    private var searchJob: Job? = null
    private var currentFilters = SearchFilters()
    
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupAdapters()
        setupSearchInput()
        setupClickListeners()
        observeResults()
        
        // Animate entrance
        binding.searchContainer.startAnimation(AnimationUtils.loadAnimation(requireContext(), R.anim.slide_up))
    }
    
    private fun setupAdapters() {
        resultsAdapter = SearchResultsAdapter(
            onItemClick = { item, type ->
                navigateToItem(item, type)
                ToastUtils.showInfo(requireContext(), "Opening ${if (type == "video") "video" else "song"}...")
            },
            onOptionsClick = { item, type ->
                showItemOptions(item, type)
            }
        )
        
        historyAdapter = SearchHistoryAdapter(
            onItemClick = { query ->
                binding.searchInput.setText(query)
                performSearch(query)
            },
            onRemoveClick = { query ->
                viewModel.removeFromHistory(query)
                ToastUtils.showInfo(requireContext(), "Removed from history")
            }
        )
        
        suggestionsAdapter = SearchSuggestionsAdapter(
            onItemClick = { suggestion ->
                binding.searchInput.setText(suggestion)
                performSearch(suggestion)
            }
        )
        
        binding.resultsRecyclerView.adapter = resultsAdapter
        binding.historyRecyclerView.adapter = historyAdapter
        binding.suggestionsRecyclerView.adapter = suggestionsAdapter
    }
    
    private fun setupSearchInput() {
        binding.searchInput.requestFocus()
        
        binding.searchInput.doOnTextChanged { text, _, _, _ ->
            val query = text?.toString()?.trim() ?: ""
            
            searchJob?.cancel()
            
            if (query.isEmpty()) {
                showHistory()
            } else if (query.length >= 2) {
                searchJob = viewLifecycleOwner.lifecycleScope.launch {
                    delay(300) // Debounce
                    viewModel.getSuggestions(query)
                    showSuggestions()
                    
                    if (query.length >= 3) {
                        performSearch(query)
                    }
                }
            }
        }
        
        binding.searchInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch(binding.searchInput.text?.toString()?.trim() ?: "")
                true
            } else false
        }
    }
    
    private fun setupClickListeners() {
        binding.backButton.setOnClickListener {
            findNavController().navigateUp()
        }
        
        binding.clearButton.setOnClickListener {
            binding.searchInput.setText("")
            showHistory()
        }
        
        binding.filterButton.setOnClickListener {
            showFilterDialog()
        }
        
        binding.clearHistoryButton.setOnClickListener {
            viewModel.clearHistory()
            ToastUtils.showInfo(requireContext(), "Search history cleared")
        }
        
        binding.voiceSearchButton.setOnClickListener {
            ToastUtils.showInfo(requireContext(), "Voice search coming soon!")
        }
    }
    
    private fun observeResults() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.searchResults.collect { results ->
                        when (results) {
                            is SearchResults.Empty -> showEmpty()
                            is SearchResults.Success -> showResults(results)
                            is SearchResults.Error -> showError(results.message)
                        }
                    }
                }
                
                launch {
                    viewModel.searchHistory.collect { history ->
                        historyAdapter.submitList(history)
                        binding.historyEmptyState.isVisible = history.isEmpty()
                    }
                }
                
                launch {
                    viewModel.suggestions.collect { suggestions ->
                        suggestionsAdapter.submitList(suggestions)
                    }
                }
                
                launch {
                    viewModel.isSearching.collect { isSearching ->
                        binding.searchingProgress.isVisible = isSearching
                    }
                }
            }
        }
    }
    
    private fun performSearch(query: String) {
        if (query.isBlank()) return
        
        binding.clearButton.isVisible = true
        viewModel.search(query, currentFilters)
        ToastUtils.showInfo(requireContext(), "Searching for \"$query\"...")
    }
    
    private fun showHistory() {
        binding.historyContainer.isVisible = true
        binding.suggestionsContainer.isVisible = false
        binding.resultsContainer.isVisible = false
        binding.emptyState.isVisible = false
        binding.clearButton.isVisible = false
    }
    
    private fun showSuggestions() {
        binding.historyContainer.isVisible = false
        binding.suggestionsContainer.isVisible = true
    }
    
    private fun showResults(results: SearchResults.Success) {
        binding.historyContainer.isVisible = false
        binding.suggestionsContainer.isVisible = false
        binding.resultsContainer.isVisible = true
        binding.emptyState.isVisible = false
        
        resultsAdapter.submitResults(results)
        
        binding.resultCountText.text = "${results.totalResults} results found"
        
        if (results.totalResults == 0) {
            showEmpty()
        }
    }
    
    private fun showEmpty() {
        binding.historyContainer.isVisible = false
        binding.suggestionsContainer.isVisible = false
        binding.resultsContainer.isVisible = false
        binding.emptyState.isVisible = true
    }
    
    private fun showError(message: String) {
        ToastUtils.showError(requireContext(), message)
        showEmpty()
    }
    
    private fun showFilterDialog() {
        // Show filter bottom sheet
        ToastUtils.showInfo(requireContext(), "Filters coming soon!")
    }
    
    private fun navigateToItem(item: Any, type: String) {
        when (type) {
            "video" -> {
                val video = item as com.zuvy.app.data.model.MediaItem
                val bundle = Bundle().apply {
                    putString("videoUri", video.uri.toString())
                    putString("videoName", video.name)
                }
                findNavController().navigate(R.id.videoPlayerFragment, bundle)
            }
            "music" -> {
                findNavController().navigate(R.id.musicPlayerFragment)
            }
        }
    }
    
    private fun showItemOptions(item: Any, type: String) {
        ToastUtils.showInfo(requireContext(), "Options coming soon!")
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

class SearchResultsAdapter(
    private val onItemClick: (Any, String) -> Unit,
    private val onOptionsClick: (Any, String) -> Unit
) : RecyclerView.Adapter<SearchResultsAdapter.ViewHolder>() {
    
    private val items = mutableListOf<SearchResultItem>()
    
    fun submitResults(results: SearchResults.Success) {
        items.clear()
        results.videos.forEach { items.add(SearchResultItem(it, "video")) }
        results.music.forEach { items.add(SearchResultItem(it, "music")) }
        notifyDataSetChanged()
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSearchResultBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }
    
    override fun getItemCount() = items.size
    
    inner class ViewHolder(private val binding: ItemSearchResultBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val item = items[position]
                    onItemClick(item.data, item.type)
                }
            }
            
            binding.optionsButton.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val item = items[position]
                    onOptionsClick(item.data, item.type)
                }
            }
        }
        
        fun bind(item: SearchResultItem) {
            when (item.type) {
                "video" -> {
                    val video = item.data as com.zuvy.app.data.model.MediaItem
                    binding.titleText.text = video.name
                    binding.subtitleText.text = video.duration.formatDuration() + " • " + video.size.formatFileSize()
                    binding.typeIcon.setImageResource(R.drawable.ic_video)
                    binding.typeBadge.text = "VIDEO"
                    
                    Glide.with(binding.thumbnail)
                        .load(video.uri)
                        .placeholder(R.drawable.ic_video_placeholder)
                        .centerCrop()
                        .into(binding.thumbnail)
                }
                "music" -> {
                    val song = item.data as com.zuvy.app.data.model.Song
                    binding.titleText.text = song.title
                    binding.subtitleText.text = song.artist + " • " + song.duration
                    binding.typeIcon.setImageResource(R.drawable.ic_music_note)
                    binding.typeBadge.text = "MUSIC"
                    
                    Glide.with(binding.thumbnail)
                        .load(song.albumArtUri)
                        .placeholder(R.drawable.ic_music_note)
                        .circleCrop()
                        .into(binding.thumbnail)
                }
            }
        }
    }
}

data class SearchResultItem(val data: Any, val type: String)

class SearchHistoryAdapter(
    private val onItemClick: (String) -> Unit,
    private val onRemoveClick: (String) -> Unit
) : RecyclerView.Adapter<SearchHistoryAdapter.ViewHolder>() {
    
    private val items = mutableListOf<String>()
    
    fun submitList(newItems: List<String>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSearchHistoryBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }
    
    override fun getItemCount() = items.size
    
    inner class ViewHolder(private val binding: ItemSearchHistoryBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(items[position])
                }
            }
            
            binding.removeButton.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onRemoveClick(items[position])
                }
            }
        }
        
        fun bind(query: String) {
            binding.queryText.text = query
        }
    }
}

class SearchSuggestionsAdapter(
    private val onItemClick: (String) -> Unit
) : RecyclerView.Adapter<SearchSuggestionsAdapter.ViewHolder>() {
    
    private val items = mutableListOf<String>()
    
    fun submitList(newItems: List<String>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSearchSuggestionBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }
    
    override fun getItemCount() = items.size
    
    inner class ViewHolder(private val binding: ItemSearchSuggestionBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(items[position])
                }
            }
        }
        
        fun bind(suggestion: String) {
            binding.suggestionText.text = suggestion
        }
    }
}
