package com.zuvy.app.ui.home.playlists

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.zuvy.app.databinding.FragmentPlaylistDetailBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PlaylistDetailFragment : Fragment() {

    private var _binding: FragmentPlaylistDetailBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPlaylistDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
