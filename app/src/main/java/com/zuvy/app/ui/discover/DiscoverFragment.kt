package com.zuvy.app.ui.discover

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.zuvy.app.R
import com.zuvy.app.databinding.FragmentDiscoverBinding
import com.zuvy.app.databinding.ItemDiscoverVideoBinding
import com.zuvy.app.databinding.ItemRadioBinding
import com.zuvy.app.databinding.ItemPodcastBinding
import com.zuvy.app.databinding.ItemMusicCardBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DiscoverFragment : Fragment() {

    private var _binding: FragmentDiscoverBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDiscoverBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerViews()
    }

    private fun setupRecyclerViews() {
        // Trending
        val trendingAdapter = TrendingAdapter()
        binding.trendingRecyclerView.adapter = trendingAdapter

        // Radio
        val radioAdapter = RadioAdapter()
        binding.radioRecyclerView.adapter = radioAdapter

        // Podcasts
        val podcastsAdapter = PodcastsAdapter()
        binding.podcastsRecyclerView.adapter = podcastsAdapter

        // Free Music
        val freeMusicAdapter = FreeMusicAdapter()
        binding.freeMusicRecyclerView.adapter = freeMusicAdapter
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // Trending Adapter
    inner class TrendingAdapter : RecyclerView.Adapter<TrendingAdapter.ViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = ItemDiscoverVideoBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            // Bind data
        }

        override fun getItemCount(): Int = 10

        inner class ViewHolder(binding: ItemDiscoverVideoBinding) : RecyclerView.ViewHolder(binding.root)
    }

    // Radio Adapter
    inner class RadioAdapter : RecyclerView.Adapter<RadioAdapter.ViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = ItemRadioBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            // Bind data
        }

        override fun getItemCount(): Int = 8

        inner class ViewHolder(binding: ItemRadioBinding) : RecyclerView.ViewHolder(binding.root)
    }

    // Podcasts Adapter
    inner class PodcastsAdapter : RecyclerView.Adapter<PodcastsAdapter.ViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = ItemPodcastBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            // Bind data
        }

        override fun getItemCount(): Int = 6

        inner class ViewHolder(binding: ItemPodcastBinding) : RecyclerView.ViewHolder(binding.root)
    }

    // Free Music Adapter
    inner class FreeMusicAdapter : RecyclerView.Adapter<FreeMusicAdapter.ViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = ItemMusicCardBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            // Bind data
        }

        override fun getItemCount(): Int = 10

        inner class ViewHolder(binding: ItemMusicCardBinding) : RecyclerView.ViewHolder(binding.root)
    }
}
