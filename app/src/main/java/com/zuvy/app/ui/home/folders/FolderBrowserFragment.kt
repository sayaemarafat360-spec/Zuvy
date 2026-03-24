package com.zuvy.app.ui.home.folders

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.zuvy.app.databinding.FragmentFolderBrowserBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class FolderBrowserFragment : Fragment() {

    private var _binding: FragmentFolderBrowserBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFolderBrowserBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
