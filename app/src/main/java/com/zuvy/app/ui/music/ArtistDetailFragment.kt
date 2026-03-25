package com.zuvy.app.ui.music

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.zuvy.app.R
import com.zuvy.app.data.model.Song
import com.zuvy.app.databinding.FragmentArtistDetailBinding
import com.zuvy.app.databinding.ItemSongBinding
import com.zuvy.app.player.MediaType
import com.zuvy.app.player.PlayerManager
import com.zuvy.app.player.QueueItem
import com.zuvy.app.utils.ToastUtils
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ArtistDetailFragment : Fragment() {

    private var _binding: FragmentArtistDetailBinding? = null
    private val binding get() = _binding!!

    private val args: ArtistDetailFragmentArgs by navArgs()
    
    @Inject
    lateinit var playerManager: PlayerManager
    
    private lateinit var songsAdapter: SongsAdapter
    private var artistName: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentArtistDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        artistName = args.artistName
        
        setupUI()
        setupRecyclerView()
        loadArtistSongs()
    }

    private fun setupUI() {
        binding.artistName.text = artistName
        
        binding.backButton.setOnClickListener {
            findNavController().navigateUp()
        }
        
        binding.playAllButton.setOnClickListener {
            playAll()
        }
        
        binding.shuffleButton.setOnClickListener {
            shuffleAll()
        }
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

    private fun loadArtistSongs() {
        // Sample data - in real app, load from ViewModel/database
        val sampleSongs = generateSampleSongs()
        
        songsAdapter.submitList(sampleSongs)
        binding.songCount.text = "${sampleSongs.size} songs"
        
        if (sampleSongs.isEmpty()) {
            binding.emptyState.visibility = View.VISIBLE
            binding.songsRecyclerView.visibility = View.GONE
        } else {
            binding.emptyState.visibility = View.GONE
            binding.songsRecyclerView.visibility = View.VISIBLE
        }
    }

    private fun generateSampleSongs(): List<Song> {
        return listOf(
            Song(1, "Song One", artistName ?: "Unknown", "Album 1", "3:45", null, null, null, 2024, 1),
            Song(2, "Song Two", artistName ?: "Unknown", "Album 1", "4:12", null, null, null, 2024, 2),
            Song(3, "Song Three", artistName ?: "Unknown", "Album 2", "3:30", null, null, null, 2023, 3),
            Song(4, "Song Four", artistName ?: "Unknown", "Album 2", "5:01", null, null, null, 2023, 4),
            Song(5, "Song Five", artistName ?: "Unknown", "Album 3", "3:55", null, null, null, 2022, 5)
        )
    }

    private fun playSong(song: Song, position: Int) {
        val songs = songsAdapter.getCurrentList()
        val queueItems = songs.map { QueueItem(it.uri, it.title, MediaType.AUDIO) }
        playerManager.playQueue(queueItems, position)
        findNavController().navigate(R.id.action_music_to_musicPlayer)
        ToastUtils.showMusic(requireContext(), "Playing ${song.title}")
    }

    private fun playAll() {
        val songs = songsAdapter.getCurrentList()
        if (songs.isNotEmpty()) {
            val queueItems = songs.map { QueueItem(it.uri, it.title, MediaType.AUDIO) }
            playerManager.playQueue(queueItems, 0)
            findNavController().navigate(R.id.action_music_to_musicPlayer)
            ToastUtils.showMusic(requireContext(), "Playing all songs")
        }
    }

    private fun shuffleAll() {
        val songs = songsAdapter.getCurrentList().shuffled()
        if (songs.isNotEmpty()) {
            val queueItems = songs.map { QueueItem(it.uri, it.title, MediaType.AUDIO) }
            playerManager.playQueue(queueItems, 0)
            findNavController().navigate(R.id.action_music_to_musicPlayer)
            ToastUtils.showMusic(requireContext(), "Shuffling ${songs.size} songs")
        }
    }

    private fun showSongOptions(song: Song) {
        val bottomSheet = com.zuvy.app.ui.components.SongOptionsBottomSheet.newInstance(
            song = song,
            onPlayNext = {
                playerManager.addToQueue(QueueItem(song.uri, song.title, MediaType.AUDIO), true)
            },
            onAddToQueue = {
                playerManager.addToQueue(QueueItem(song.uri, song.title, MediaType.AUDIO), false)
            }
        )
        bottomSheet.show(childFragmentManager, "song_options")
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
        
        fun getCurrentList(): List<Song> = items.toList()

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongViewHolder {
            val binding = ItemSongBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            return SongViewHolder(binding)
        }

        override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
            holder.bind(items.getOrNull(position) ?: return)
        }

        override fun getItemCount() = items.size

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
                binding.artistName.text = song.album ?: song.artist
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
