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
import com.zuvy.app.databinding.FragmentFavoritesBinding
import com.zuvy.app.databinding.ItemSongBinding
import com.zuvy.app.player.MediaType
import com.zuvy.app.player.PlayerManager
import com.zuvy.app.player.QueueItem
import com.zuvy.app.utils.ToastUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class FavoritesFragment : Fragment() {

    private var _binding: FragmentFavoritesBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var playerManager: PlayerManager

    private val viewModel: MusicViewModel by activityViewModels()
    private lateinit var favoritesAdapter: FavoritesAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFavoritesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        observeData()
    }

    private fun setupRecyclerView() {
        favoritesAdapter = FavoritesAdapter(
            onItemClick = { song, position ->
                playSong(song, position)
            },
            onRemoveClick = { song ->
                removeFromFavorites(song)
            }
        )
        binding.favoritesRecyclerView.apply {
            adapter = favoritesAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun observeData() {
        // For now, show empty state message
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Show empty state initially
                binding.emptyState.visibility = View.VISIBLE
                binding.favoritesRecyclerView.visibility = View.GONE
                
                // When database is implemented, observe favorites
                // viewModel.favorites.collect { favorites ->
                //     if (favorites.isEmpty()) {
                //         binding.emptyState.visibility = View.VISIBLE
                //     } else {
                //         binding.emptyState.visibility = View.GONE
                //         favoritesAdapter.submitList(favorites)
                //     }
                // }
            }
        }
    }

    private fun playSong(song: Song, position: Int) {
        val queueItems = listOf(QueueItem(song.uri, song.title, MediaType.AUDIO))
        playerManager.playQueue(queueItems, 0)
        findNavController().navigate(R.id.action_music_to_musicPlayer)
        ToastUtils.showMusic(requireContext(), "Now playing: ${song.title}")
    }

    private fun removeFromFavorites(song: Song) {
        ToastUtils.showInfo(requireContext(), "Removed from favorites")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    inner class FavoritesAdapter(
        private val onItemClick: (Song, Int) -> Unit,
        private val onRemoveClick: (Song) -> Unit
    ) : RecyclerView.Adapter<FavoritesAdapter.FavoriteViewHolder>() {

        private val items = mutableListOf<Song>()

        fun submitList(newItems: List<Song>) {
            items.clear()
            items.addAll(newItems)
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FavoriteViewHolder {
            val binding = ItemSongBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            return FavoriteViewHolder(binding)
        }

        override fun onBindViewHolder(holder: FavoriteViewHolder, position: Int) {
            holder.bind(items.getOrNull(position) ?: return)
        }

        override fun getItemCount() = items.size

        inner class FavoriteViewHolder(
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
                        onRemoveClick(items[position])
                    }
                }
            }

            fun bind(song: Song) {
                binding.songTitle.text = song.title
                binding.artistName.text = song.artist
                binding.duration.text = song.duration

                Glide.with(binding.albumArt)
                    .load(song.albumArtUri)
                    .placeholder(R.drawable.ic_heart)
                    .circleCrop()
                    .into(binding.albumArt)
            }
        }
    }
}
