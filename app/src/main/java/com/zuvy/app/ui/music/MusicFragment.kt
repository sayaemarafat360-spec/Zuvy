package com.zuvy.app.ui.music

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.tabs.TabLayoutMediator
import com.zuvy.app.R
import com.zuvy.app.databinding.FragmentMusicBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MusicFragment : Fragment() {

    private var _binding: FragmentMusicBinding? = null
    private val binding get() = _binding!!

    private lateinit var pagerAdapter: MusicPagerAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMusicBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViewPager()
        setupMiniPlayer()
    }

    private fun setupViewPager() {
        pagerAdapter = MusicPagerAdapter(this)
        binding.viewPager.adapter = pagerAdapter

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> getString(R.string.songs)
                1 -> getString(R.string.artists)
                2 -> getString(R.string.albums)
                3 -> getString(R.string.genres)
                4 -> getString(R.string.favorites)
                else -> ""
            }
        }.attach()
    }

    private fun setupMiniPlayer() {
        binding.miniPlayer.setOnClickListener {
            findNavController().navigate(R.id.action_music_to_musicPlayer)
        }

        binding.playPauseButton.setOnClickListener {
            // TODO: Toggle play/pause
        }

        binding.nextButton.setOnClickListener {
            // TODO: Next track
        }

        // Show mini player (for demo)
        binding.miniPlayer.visibility = View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

class MusicPagerAdapter(fragment: Fragment) : androidx.viewpager2.adapter.FragmentStateAdapter(fragment) {
    override fun getItemCount(): Int = 5

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> SongsFragment()
            1 -> ArtistsFragment()
            2 -> AlbumsFragment()
            3 -> GenresFragment()
            4 -> FavoritesFragment()
            else -> SongsFragment()
        }
    }
}
