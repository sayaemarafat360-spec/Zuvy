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
import com.zuvy.app.data.model.Artist
import com.zuvy.app.databinding.FragmentArtistsBinding
import com.zuvy.app.databinding.ItemArtistBinding
import com.zuvy.app.utils.ToastUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ArtistsFragment : Fragment() {

    private var _binding: FragmentArtistsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MusicViewModel by activityViewModels()
    private lateinit var artistsAdapter: ArtistsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentArtistsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        observeData()
    }

    private fun setupRecyclerView() {
        artistsAdapter = ArtistsAdapter { artist ->
            navigateToArtistDetail(artist)
        }
        binding.artistsRecyclerView.apply {
            adapter = artistsAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun observeData() {
        // For now, show sample data
        val sampleArtists = listOf(
            Artist(1, "Unknown Artist", 15, 45),
            Artist(2, "Various Artists", 8, 32),
            Artist(3, "Ed Sheeran", 3, 24),
            Artist(4, "The Weeknd", 2, 18),
            Artist(5, "Dua Lipa", 2, 15)
        )
        artistsAdapter.submitList(sampleArtists)
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Observe artists from ViewModel when implemented
            }
        }
    }

    private fun navigateToArtistDetail(artist: Artist) {
        val direction = com.zuvy.app.ui.music.ArtistsFragmentDirections.actionMusicToArtistDetail(
            artistName = artist.name,
            artistId = artist.id
        )
        findNavController().navigate(direction)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    inner class ArtistsAdapter(
        private val onItemClick: (Artist) -> Unit
    ) : RecyclerView.Adapter<ArtistsAdapter.ArtistViewHolder>() {

        private val items = mutableListOf<Artist>()

        fun submitList(newItems: List<Artist>) {
            items.clear()
            items.addAll(newItems)
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ArtistViewHolder {
            val binding = ItemArtistBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            return ArtistViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ArtistViewHolder, position: Int) {
            holder.bind(items[position])
        }

        override fun getItemCount() = items.size

        inner class ArtistViewHolder(
            private val binding: ItemArtistBinding
        ) : RecyclerView.ViewHolder(binding.root) {

            init {
                binding.root.setOnClickListener {
                    val position = bindingAdapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        onItemClick(items[position])
                    }
                }
                
                binding.moreButton.setOnClickListener {
                    ToastUtils.showInfo(requireContext(), "Artist options")
                }
            }

            fun bind(artist: Artist) {
                binding.artistName.text = artist.name
                binding.albumCount.text = "${artist.albumCount} albums"
                binding.songCount.text = "${artist.songCount} songs"

                Glide.with(binding.artistImage)
                    .load(artist.albumArtUri)
                    .placeholder(R.drawable.ic_artist)
                    .circleCrop()
                    .into(binding.artistImage)
            }
        }
    }
}
