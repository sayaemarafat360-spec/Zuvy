package com.zuvy.app.ui.music

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.ActionMode
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
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
import com.zuvy.app.utils.ToastUtils
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
    
    // Multi-select state
    private var actionMode: ActionMode? = null
    private val selectedItems = mutableSetOf<Song>()
    private var isMultiSelectMode = false
    
    // Double tap detection
    private var lastTapTime = 0L
    private var lastTappedSong: Song? = null
    private val doubleTapTimeout = 300L

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
                if (isMultiSelectMode) {
                    toggleSelection(song)
                } else {
                    // Double tap detection
                    val now = System.currentTimeMillis()
                    if (lastTappedSong == song && now - lastTapTime < doubleTapTimeout) {
                        // Double tap - Add to favorites
                        onDoubleTap(song)
                        lastTapTime = 0L
                        lastTappedSong = null
                    } else {
                        // Single tap
                        lastTapTime = now
                        lastTappedSong = song
                        Handler(Looper.getMainLooper()).postDelayed({
                            if (lastTappedSong == song) {
                                playSong(song, position)
                            }
                        }, doubleTapTimeout)
                    }
                }
            },
            onOptionsClick = { song ->
                if (!isMultiSelectMode) {
                    showSongOptions(song)
                }
            },
            onLongClick = { song ->
                if (!isMultiSelectMode) {
                    startMultiSelect(song)
                } else {
                    toggleSelection(song)
                }
                true
            }
        )
        binding.songsRecyclerView.adapter = songsAdapter
    }
    
    // Double tap action - Add to favorites
    private fun onDoubleTap(song: Song) {
        ToastUtils.showSuccess(requireContext(), "❤️ Added to favorites: ${song.title}")
    }
    
    // Multi-select functionality
    private fun startMultiSelect(song: Song) {
        isMultiSelectMode = true
        selectedItems.clear()
        selectedItems.add(song)
        actionMode = requireActivity().startActionMode(actionModeCallback)
        songsAdapter.notifyDataSetChanged()
    }
    
    private fun toggleSelection(song: Song) {
        if (selectedItems.contains(song)) {
            selectedItems.remove(song)
            if (selectedItems.isEmpty()) {
                exitMultiSelect()
            }
        } else {
            selectedItems.add(song)
        }
        actionMode?.title = "${selectedItems.size} selected"
        songsAdapter.notifyDataSetChanged()
    }
    
    private fun exitMultiSelect() {
        isMultiSelectMode = false
        selectedItems.clear()
        actionMode?.finish()
        actionMode = null
        songsAdapter.notifyDataSetChanged()
    }
    
    private val actionModeCallback = object : ActionMode.Callback {
        override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
            mode?.menuInflater?.inflate(R.menu.menu_multi_select, menu)
            mode?.title = "1 selected"
            return true
        }

        override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean = false

        override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
            return when (item?.itemId) {
                R.id.action_share -> {
                    ToastUtils.showInfo(requireContext(), "Sharing ${selectedItems.size} songs...")
                    exitMultiSelect()
                    true
                }
                R.id.action_delete -> {
                    ToastUtils.showWarning(requireContext(), "Delete ${selectedItems.size} songs?")
                    exitMultiSelect()
                    true
                }
                R.id.action_add_playlist -> {
                    ToastUtils.showSuccess(requireContext(), "Added ${selectedItems.size} songs to playlist")
                    exitMultiSelect()
                    true
                }
                R.id.action_select_all -> {
                    selectedItems.clear()
                    selectedItems.addAll(songsAdapter.getItems())
                    actionMode?.title = "${selectedItems.size} selected"
                    songsAdapter.notifyDataSetChanged()
                    true
                }
                else -> false
            }
        }

        override fun onDestroyActionMode(mode: ActionMode?) {
            isMultiSelectMode = false
            selectedItems.clear()
            songsAdapter.notifyDataSetChanged()
        }
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
        binding.shuffleChip.setOnClickListener {
            val songs = viewModel.songs.value.shuffled()
            if (songs.isNotEmpty()) {
                playSong(songs.first(), 0)
            }
        }
        
        binding.playAllChip.setOnClickListener {
            val songs = viewModel.songs.value
            if (songs.isNotEmpty()) {
                playSong(songs.first(), 0)
            }
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
        val bottomSheet = com.zuvy.app.ui.components.SongOptionsBottomSheet.newInstance(
            song = song,
            onPlayNext = {
                playerManager.addToQueue(
                    com.zuvy.app.player.QueueItem(song.uri, song.title, com.zuvy.app.player.MediaType.AUDIO),
                    playNext = true
                )
            },
            onAddToQueue = {
                playerManager.addToQueue(
                    com.zuvy.app.player.QueueItem(song.uri, song.title, com.zuvy.app.player.MediaType.AUDIO),
                    playNext = false
                )
            },
            onAddToPlaylist = {
                // Show playlist picker
            },
            onAddToFavorites = {
                // Add to favorites database
            },
            onGoToArtist = {
                // Navigate to artist detail
            },
            onGoToAlbum = {
                // Navigate to album detail
            },
            onSetRingtone = {
                // Set as ringtone
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
        private val onOptionsClick: (Song) -> Unit,
        private val onLongClick: (Song) -> Boolean
    ) : RecyclerView.Adapter<SongsAdapter.SongViewHolder>() {

        private val items = mutableListOf<Song>()
        
        fun getItems(): List<Song> = items.toList()

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
                
                binding.root.setOnLongClickListener {
                    val position = bindingAdapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        onLongClick(items[position])
                    } else {
                        false
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
                
                // Selection state visual feedback
                val isSelected = selectedItems.contains(song)
                binding.root.isSelected = isSelected
                binding.selectionOverlay.visibility = if (isSelected) View.VISIBLE else View.GONE
                binding.checkbox.isChecked = isSelected
                binding.checkbox.visibility = if (isMultiSelectMode) View.VISIBLE else View.GONE

                Glide.with(binding.albumArt)
                    .load(song.albumArtUri)
                    .placeholder(R.drawable.ic_music_note)
                    .circleCrop()
                    .into(binding.albumArt)
            }
        }
    }
}
