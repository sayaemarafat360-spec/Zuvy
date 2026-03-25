package com.zuvy.app.ui.music

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.zuvy.app.R
import com.zuvy.app.data.model.Song
import com.zuvy.app.databinding.FragmentSongsBinding
import com.zuvy.app.databinding.ItemSongBinding
import com.zuvy.app.player.MediaType
import com.zuvy.app.player.PlayerManager
import com.zuvy.app.player.QueueItem
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SongsFragment : Fragment() {

    private var _binding: FragmentSongsBinding? = null
    private val binding get() = _binding!!
    
    @Inject
    lateinit var playerManager: PlayerManager
    
    private val viewModel: MusicViewModel by activityViewModels()
    private lateinit var songsAdapter: SongsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSongsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        observeData()
        setupClickListeners()
    }

    private fun setupRecyclerView() {
        songsAdapter = SongsAdapter(
            onItemClick = { song, position ->
                playSong(song, position)
            },
            onOptionsClick = { song ->
                showSongOptions(song)
            }
        )
        binding.songsRecyclerView.adapter = songsAdapter
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.songs.collect { songs ->
                    if (songs.isEmpty()) {
                        binding.shimmerLayout.visibility = View.VISIBLE
                        binding.shimmerLayout.startShimmer()
                        binding.songsRecyclerView.visibility = View.GONE
                    } else {
                        binding.shimmerLayout.stopShimmer()
                        binding.shimmerLayout.visibility = View.GONE
                        binding.songsRecyclerView.visibility = View.VISIBLE
                        songsAdapter.submitList(songs)
                    }
                }
            }
        }
    }

    private fun setupClickListeners() {
        binding.shuffleAll.setOnClickListener {
            val songs = viewModel.songs.value.shuffled()
            if (songs.isNotEmpty()) {
                playSong(songs.first(), 0)
            }
        }
        
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.loadMusic()
            binding.swipeRefresh.isRefreshing = false
        }
    }

    private fun playSong(song: Song, position: Int) {
        val queueItems = viewModel.songs.value.map { 
            QueueItem(it.uri, it.title, MediaType.AUDIO)
        }
        
        playerManager.playQueue(queueItems, position)
        
        // Navigate to music player
        findNavController().navigate(R.id.action_music_to_musicPlayer)
    }

    private fun showSongOptions(song: Song) {
        // Show bottom sheet with options
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    inner class SongsAdapter(
        private val onItemClick: (Song, Int) -> Unit,
        private val onOptionsClick: (Song) -> Unit
    ) : RecyclerView.Adapter<SongsAdapter.SongViewHolder>() {

        private val items = mutableListOf<Song>()

        fun submitList(newItems: List<Song>) {
            items.clear()
            items.addAll(newItems)
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongViewHolder {
            val binding = ItemSongBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            return SongViewHolder(binding)
        }

        override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
            holder.bind(items.getOrNull(position) ?: return)
        }

        override fun getItemCount(): Int = items.size

        inner class SongViewHolder(
            private val binding: ItemSongBinding
        ) : RecyclerView.ViewHolder(binding.root) {

            init {
                binding.root.setOnClickListener {
                    val position = bindingAdapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        onItemClick(items[position], position)
                    }
                }
                
                binding.moreButton.setOnClickListener {
                    val position = bindingAdapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        onOptionsClick(items[position])
                    }
                }
            }

            fun bind(song: Song) {
                binding.songTitle.text = song.title
                binding.artistName.text = song.artist
                binding.duration.text = song.duration

                Glide.with(binding.albumArt)
                    .load(song.albumArtUri)
                    .placeholder(R.drawable.ic_music_note)
                    .circleCrop()
                    .into(binding.albumArt)
            }
        }
    }
}
