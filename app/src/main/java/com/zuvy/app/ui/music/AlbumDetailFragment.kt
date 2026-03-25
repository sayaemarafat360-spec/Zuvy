package com.zuvy.app.ui.music

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.zuvy.app.R
import com.zuvy.app.data.model.Song
import com.zuvy.app.databinding.FragmentAlbumDetailBinding
import com.zuvy.app.databinding.ItemSongBinding
import com.zuvy.app.player.MediaType
import com.zuvy.app.player.PlayerManager
import com.zuvy.app.player.QueueItem
import com.zuvy.app.utils.ToastUtils
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class AlbumDetailFragment : Fragment() {

    private var _binding: FragmentAlbumDetailBinding? = null
    private val binding get() = _binding!!

    private val args: AlbumDetailFragmentArgs by navArgs()
    
    @Inject
    lateinit var playerManager: PlayerManager
    
    private lateinit var songsAdapter: SongsAdapter
    private var albumName: String? = null
    private var artistName: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAlbumDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        albumName = args.albumName
        artistName = args.artistName
        
        setupUI()
        setupRecyclerView()
        loadAlbumSongs()
    }

    private fun setupUI() {
        binding.albumName.text = albumName
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

    private fun loadAlbumSongs() {
        // Sample data
        val sampleSongs = listOf(
            Song(1, "Track 1 - $albumName", artistName ?: "Unknown", albumName, "3:45", null, null, null, 2024, 1),
            Song(2, "Track 2 - $albumName", artistName ?: "Unknown", albumName, "4:12", null, null, null, 2024, 2),
            Song(3, "Track 3 - $albumName", artistName ?: "Unknown", albumName, "3:30", null, null, null, 2024, 3),
            Song(4, "Track 4 - $albumName", artistName ?: "Unknown", albumName, "5:01", null, null, null, 2024, 4)
        )
        
        songsAdapter.submitList(sampleSongs)
        binding.songCount.text = "${sampleSongs.size} songs"
        binding.totalDuration.text = "16 min"
        
        Glide.with(binding.albumArt)
            .load(null)
            .placeholder(R.drawable.ic_music_note)
            .centerCrop()
            .into(binding.albumArt)
    }

    private fun playSong(song: Song, position: Int) {
        val songs = songsAdapter.getCurrentList()
        val queueItems = songs.map { QueueItem(it.uri, it.title, MediaType.AUDIO) }
        playerManager.playQueue(queueItems, position)
        findNavController().navigate(R.id.action_music_to_musicPlayer)
    }

    private fun playAll() {
        val songs = songsAdapter.getCurrentList()
        if (songs.isNotEmpty()) {
            val queueItems = songs.map { QueueItem(it.uri, it.title, MediaType.AUDIO) }
            playerManager.playQueue(queueItems, 0)
            findNavController().navigate(R.id.action_music_to_musicPlayer)
        }
    }

    private fun shuffleAll() {
        val songs = songsAdapter.getCurrentList().shuffled()
        if (songs.isNotEmpty()) {
            val queueItems = songs.map { QueueItem(it.uri, it.title, MediaType.AUDIO) }
            playerManager.playQueue(queueItems, 0)
            findNavController().navigate(R.id.action_music_to_musicPlayer)
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
