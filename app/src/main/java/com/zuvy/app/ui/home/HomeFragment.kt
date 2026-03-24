package com.zuvy.app.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.tabs.TabLayoutMediator
import com.zuvy.app.R
import com.zuvy.app.databinding.FragmentHomeBinding
import com.zuvy.app.ui.home.videos.VideosFragment
import com.zuvy.app.ui.home.folders.FoldersFragment
import com.zuvy.app.ui.home.playlists.PlaylistsFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels()
    private lateinit var pagerAdapter: HomePagerAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViewPager()
        setupTabLayout()
        setupClickListeners()
        observeData()
    }

    private fun setupViewPager() {
        pagerAdapter = HomePagerAdapter(this)
        binding.viewPager.adapter = pagerAdapter
        binding.viewPager.offscreenPageLimit = 3
    }

    private fun setupTabLayout() {
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> getString(R.string.videos)
                1 -> getString(R.string.folders)
                2 -> getString(R.string.playlists)
                else -> ""
            }
        }.attach()

        // Show/hide FAB based on current tab
        binding.tabLayout.addOnTabSelectedListener(object : com.google.android.material.tabs.TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: com.google.android.material.tabs.TabLayout.Tab?) {
                binding.createPlaylistFab.visibility = if (tab?.position == 2) View.VISIBLE else View.GONE
            }
            override fun onTabUnselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
            override fun onTabReselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
        })
    }

    private fun setupClickListeners() {
        binding.searchButton.setOnClickListener {
            // TODO: Open search
        }

        binding.createPlaylistFab.setOnClickListener {
            // TODO: Create playlist dialog
        }
    }

    private fun observeData() {
        viewModel.videoCount.observe(viewLifecycleOwner) { count ->
            binding.videoCountText.text = resources.getQuantityString(
                R.plurals.videos_count_plural, count, count
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

class HomePagerAdapter(fragment: Fragment) : androidx.viewpager2.adapter.FragmentStateAdapter(fragment) {
    override fun getItemCount(): Int = 3

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> VideosFragment()
            1 -> FoldersFragment()
            2 -> PlaylistsFragment()
            else -> VideosFragment()
        }
    }
}
