package com.zuvy.app.ui.home.playlists

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.zuvy.app.R
import com.zuvy.app.data.model.Playlist
import com.zuvy.app.databinding.FragmentPlaylistsBinding
import com.zuvy.app.databinding.ItemPlaylistBinding
import com.zuvy.app.databinding.ItemPlaylistSmallBinding
import com.zuvy.app.ui.home.HomeViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PlaylistsFragment : Fragment() {

    private var _binding: FragmentPlaylistsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by activityViewModels()
    private lateinit var autoPlaylistsAdapter: AutoPlaylistsAdapter
    private lateinit var userPlaylistsAdapter: UserPlaylistsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPlaylistsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerViews()
        loadDummyData()
    }

    private fun setupRecyclerViews() {
        autoPlaylistsAdapter = AutoPlaylistsAdapter { playlist ->
            navigateToPlaylistDetail(playlist)
        }
        binding.autoPlaylistsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = autoPlaylistsAdapter
        }

        userPlaylistsAdapter = UserPlaylistsAdapter { playlist ->
            navigateToPlaylistDetail(playlist)
        }
        binding.playlistsRecyclerView.apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter = userPlaylistsAdapter
        }

        binding.createPlaylistButton.setOnClickListener {
            // TODO: Create playlist dialog
        }
    }

    private fun loadDummyData() {
        // Auto-generated playlists
        val autoPlaylists = listOf(
            Playlist(id = "recent", name = "Recently Played", videoCount = 15, iconRes = R.drawable.ic_time),
            Playlist(id = "most", name = "Most Played", videoCount = 32, iconRes = R.drawable.ic_trending),
            Playlist(id = "fav", name = "Favorites", videoCount = 8, iconRes = R.drawable.ic_heart)
        )
        autoPlaylistsAdapter.submitList(autoPlaylists)

        // User playlists (empty for now)
        val userPlaylists = listOf(
            Playlist(id = "1", name = "My Playlist", videoCount = 12),
            Playlist(id = "2", name = "Watch Later", videoCount = 5)
        )
        userPlaylistsAdapter.submitList(userPlaylists)
    }

    private fun navigateToPlaylistDetail(playlist: Playlist) {
        val action = PlaylistsFragmentDirections.actionHomeToPlaylistDetail(playlist.id)
        findNavController().navigate(action)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    inner class AutoPlaylistsAdapter(
        private val onPlaylistClick: (Playlist) -> Unit
    ) : RecyclerView.Adapter<AutoPlaylistsAdapter.ViewHolder>() {

        private val items = mutableListOf<Playlist>()

        fun submitList(newItems: List<Playlist>) {
            items.clear()
            items.addAll(newItems)
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = ItemPlaylistSmallBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(items[position])
        }

        override fun getItemCount(): Int = items.size

        inner class ViewHolder(
            private val binding: ItemPlaylistSmallBinding
        ) : RecyclerView.ViewHolder(binding.root) {

            init {
                binding.root.setOnClickListener {
                    val position = bindingAdapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        onPlaylistClick(items[position])
                    }
                }
            }

            fun bind(playlist: Playlist) {
                binding.playlistName.text = playlist.name
                binding.videoCount.text = resources.getQuantityString(
                    R.plurals.videos_count_plural, playlist.videoCount, playlist.videoCount
                )
                playlist.iconRes?.let { binding.icon.setImageResource(it) }
            }
        }
    }

    inner class UserPlaylistsAdapter(
        private val onPlaylistClick: (Playlist) -> Unit
    ) : RecyclerView.Adapter<UserPlaylistsAdapter.ViewHolder>() {

        private val items = mutableListOf<Playlist>()

        fun submitList(newItems: List<Playlist>) {
            items.clear()
            items.addAll(newItems)
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = ItemPlaylistBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(items[position])
        }

        override fun getItemCount(): Int = items.size

        inner class ViewHolder(
            private val binding: ItemPlaylistBinding
        ) : RecyclerView.ViewHolder(binding.root) {

            init {
                binding.root.setOnClickListener {
                    val position = bindingAdapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        onPlaylistClick(items[position])
                    }
                }
            }

            fun bind(playlist: Playlist) {
                binding.playlistName.text = playlist.name
                binding.videoCount.text = resources.getQuantityString(
                    R.plurals.videos_count_plural, playlist.videoCount, playlist.videoCount
                )
            }
        }
    }
}
