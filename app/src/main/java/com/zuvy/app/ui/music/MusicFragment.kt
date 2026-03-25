package com.zuvy.app.ui.music

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.tabs.TabLayoutMediator
import com.zuvy.app.R
import com.zuvy.app.databinding.FragmentMusicBinding
import com.zuvy.app.player.PlayerManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MusicFragment : Fragment() {

    private var _binding: FragmentMusicBinding? = null
    private val binding get() = _binding!!

    private lateinit var pagerAdapter: MusicPagerAdapter
    
    @Inject
    lateinit var playerManager: PlayerManager

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
        observePlayerState()
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
            playerManager.playPause()
        }

        binding.nextButton.setOnClickListener {
            playerManager.playNext()
        }
    }

    private fun observePlayerState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    playerManager.currentMedia.collect { media ->
                        if (media != null) {
                            binding.miniPlayer.visibility = View.VISIBLE
                            binding.songTitle.text = media.name
                        } else {
                            binding.miniPlayer.visibility = View.GONE
                        }
                    }
                }

                launch {
                    playerManager.isPlaying.collect { isPlaying ->
                        val iconRes = if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
                        binding.playPauseButton.setImageResource(iconRes)
                    }
                }
            }
        }
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
