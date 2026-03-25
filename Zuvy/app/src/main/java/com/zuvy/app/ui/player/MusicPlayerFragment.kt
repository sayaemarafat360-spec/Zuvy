package com.zuvy.app.ui.player

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.zuvy.app.databinding.FragmentMusicPlayerBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MusicPlayerFragment : Fragment() {

    private var _binding: FragmentMusicPlayerBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMusicPlayerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
