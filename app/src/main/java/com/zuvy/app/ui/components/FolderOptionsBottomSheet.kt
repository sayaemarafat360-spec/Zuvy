package com.zuvy.app.ui.components

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.zuvy.app.R
import com.zuvy.app.databinding.BottomSheetFolderOptionsBinding
import com.zuvy.app.utils.ToastUtils

class FolderOptionsBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetFolderOptionsBinding? = null
    private val binding get() = _binding!!
    
    private var folderName: String? = null
    private var folderPath: String? = null
    private var videoCount: Int = 0
    
    private var onPlayAll: (() -> Unit)? = null
    private var onShuffleAll: (() -> Unit)? = null
    private var onAddToPlaylist: (() -> Unit)? = null
    private var onHide: (() -> Unit)? = null
    private var onDelete: (() -> Unit)? = null

    companion object {
        fun newInstance(
            folderName: String,
            folderPath: String = "",
            videoCount: Int = 0,
            onPlayAll: () -> Unit = {},
            onShuffleAll: () -> Unit = {},
            onAddToPlaylist: () -> Unit = {},
            onHide: () -> Unit = {},
            onDelete: () -> Unit = {}
        ): FolderOptionsBottomSheet {
            return FolderOptionsBottomSheet().apply {
                arguments = Bundle().apply {
                    putString("folderName", folderName)
                    putString("folderPath", folderPath)
                    putInt("videoCount", videoCount)
                }
                this.onPlayAll = onPlayAll
                this.onShuffleAll = onShuffleAll
                this.onAddToPlaylist = onAddToPlaylist
                this.onHide = onHide
                this.onDelete = onDelete
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = BottomSheetFolderOptionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        folderName = arguments?.getString("folderName")
        folderPath = arguments?.getString("folderPath")
        videoCount = arguments?.getInt("videoCount", 0) ?: 0
        
        binding.titleText.text = folderName
        binding.subtitleText.text = "$videoCount videos"
        
        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.optionPlay.setOnClickListener {
            dismiss()
            onPlayAll?.invoke()
            ToastUtils.showSuccess(requireContext(), "Playing folder: $folderName")
        }
        
        binding.optionShuffle.setOnClickListener {
            dismiss()
            onShuffleAll?.invoke()
            ToastUtils.showSuccess(requireContext(), "Shuffling folder: $folderName")
        }
        
        binding.optionAddPlaylist.setOnClickListener {
            dismiss()
            onAddToPlaylist?.invoke()
            ToastUtils.showInfo(requireContext(), "Add to playlist coming soon")
        }
        
        binding.optionHide.setOnClickListener {
            dismiss()
            onHide?.invoke()
            ToastUtils.showInfo(requireContext(), "Folder hidden from library")
        }
        
        binding.optionDelete.setOnClickListener {
            dismiss()
            onDelete?.invoke()
            ToastUtils.showWarning(requireContext(), "Delete folder?")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
