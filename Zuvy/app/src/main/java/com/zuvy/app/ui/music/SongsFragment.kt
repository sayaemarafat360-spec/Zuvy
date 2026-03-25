package com.zuvy.app.ui.music

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.zuvy.app.R
import com.zuvy.app.data.model.Song
import com.zuvy.app.databinding.FragmentSongsBinding
import com.zuvy.app.databinding.ItemSongBinding

class SongsFragment : Fragment() {

    private var _binding: FragmentSongsBinding? = null
    private val binding get() = _binding!!
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
        loadDummyData()
    }

    private fun setupRecyclerView() {
        songsAdapter = SongsAdapter()
        binding.songsRecyclerView.adapter = songsAdapter
    }

    private fun loadDummyData() {
        binding.shimmerLayout.stopShimmer()
        binding.shimmerLayout.visibility = View.GONE
        binding.songsRecyclerView.visibility = View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    inner class SongsAdapter : RecyclerView.Adapter<SongsAdapter.SongViewHolder>() {

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

            fun bind(song: Song) {
                binding.songTitle.text = song.title
                binding.artistName.text = song.artist
                binding.duration.text = song.duration
            }
        }
    }
}
