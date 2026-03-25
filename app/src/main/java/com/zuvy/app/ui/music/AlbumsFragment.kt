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
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.zuvy.app.R
import com.zuvy.app.data.model.Album
import com.zuvy.app.databinding.FragmentAlbumsBinding
import com.zuvy.app.databinding.ItemAlbumBinding
import com.zuvy.app.utils.ToastUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AlbumsFragment : Fragment() {

    private var _binding: FragmentAlbumsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MusicViewModel by activityViewModels()
    private lateinit var albumsAdapter: AlbumsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAlbumsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        observeData()
    }

    private fun setupRecyclerView() {
        albumsAdapter = AlbumsAdapter { album ->
            navigateToAlbumDetail(album)
        }
        binding.albumsRecyclerView.apply {
            adapter = albumsAdapter
            layoutManager = GridLayoutManager(requireContext(), 2)
        }
    }

    private fun observeData() {
        // For now, show sample data
        val sampleAlbums = listOf(
            Album(1, "Unknown Album", "Various Artists", 0, null, 24, 2024),
            Album(2, "÷ (Divide)", "Ed Sheeran", 1, null, 16, 2017),
            Album(3, "After Hours", "The Weeknd", 2, null, 14, 2020),
            Album(4, "Future Nostalgia", "Dua Lipa", 3, null, 11, 2020),
            Album(5, "Scorpion", "Drake", 4, null, 25, 2018),
            Album(6, "1989", "Taylor Swift", 5, null, 13, 2014)
        )
        albumsAdapter.submitList(sampleAlbums)
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Observe albums from ViewModel when implemented
            }
        }
    }

    private fun navigateToAlbumDetail(album: Album) {
        val direction = com.zuvy.app.ui.music.AlbumsFragmentDirections.actionMusicToAlbumDetail(
            albumName = album.name,
            artistName = album.artist,
            albumId = album.id
        )
        findNavController().navigate(direction)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    inner class AlbumsAdapter(
        private val onItemClick: (Album) -> Unit
    ) : RecyclerView.Adapter<AlbumsAdapter.AlbumViewHolder>() {

        private val items = mutableListOf<Album>()

        fun submitList(newItems: List<Album>) {
            items.clear()
            items.addAll(newItems)
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlbumViewHolder {
            val binding = ItemAlbumBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            return AlbumViewHolder(binding)
        }

        override fun onBindViewHolder(holder: AlbumViewHolder, position: Int) {
            holder.bind(items[position])
        }

        override fun getItemCount() = items.size

        inner class AlbumViewHolder(
            private val binding: ItemAlbumBinding
        ) : RecyclerView.ViewHolder(binding.root) {

            init {
                binding.root.setOnClickListener {
                    val position = bindingAdapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        onItemClick(items[position])
                    }
                }
            }

            fun bind(album: Album) {
                binding.albumName.text = album.name
                binding.artistName.text = album.artist
                binding.songCount.text = "${album.songCount} songs"

                Glide.with(binding.albumArt)
                    .load(album.albumArtUri)
                    .placeholder(R.drawable.ic_music_note)
                    .centerCrop()
                    .into(binding.albumArt)
            }
        }
    }
}
