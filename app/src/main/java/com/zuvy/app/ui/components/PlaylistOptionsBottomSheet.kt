package com.zuvy.app.ui.components

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.zuvy.app.R
import com.zuvy.app.databinding.BottomSheetPlaylistOptionsBinding
import com.zuvy.app.databinding.DialogCreatePlaylistBinding
import com.zuvy.app.utils.ToastUtils

class PlaylistOptionsBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetPlaylistOptionsBinding? = null
    private val binding get() = _binding!!
    
    private var playlistName: String? = null
    private var videoCount: Int = 0
    private var totalDuration: String? = null
    
    private var onPlayAll: (() -> Unit)? = null
    private var onShuffleAll: (() -> Unit)? = null
    private var onRename: ((String) -> Unit)? = null
    private var onShare: (() -> Unit)? = null
    private var onDelete: (() -> Unit)? = null

    companion object {
        fun newInstance(
            playlistName: String,
            videoCount: Int = 0,
            totalDuration: String = "",
            onPlayAll: () -> Unit = {},
            onShuffleAll: () -> Unit = {},
            onRename: (String) -> Unit = {},
            onShare: () -> Unit = {},
            onDelete: () -> Unit = {}
        ): PlaylistOptionsBottomSheet {
            return PlaylistOptionsBottomSheet().apply {
                arguments = Bundle().apply {
                    putString("playlistName", playlistName)
                    putInt("videoCount", videoCount)
                    putString("totalDuration", totalDuration)
                }
                this.onPlayAll = onPlayAll
                this.onShuffleAll = onShuffleAll
                this.onRename = onRename
                this.onShare = onShare
                this.onDelete = onDelete
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = BottomSheetPlaylistOptionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        playlistName = arguments?.getString("playlistName")
        videoCount = arguments?.getInt("videoCount", 0) ?: 0
        totalDuration = arguments?.getString("totalDuration")
        
        binding.titleText.text = playlistName
        binding.subtitleText.text = "$videoCount videos • $totalDuration"
        
        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.optionPlay.setOnClickListener {
            dismiss()
            onPlayAll?.invoke()
            ToastUtils.showSuccess(requireContext(), "Playing playlist: $playlistName")
        }
        
        binding.optionShuffle.setOnClickListener {
            dismiss()
            onShuffleAll?.invoke()
            ToastUtils.showSuccess(requireContext(), "Shuffling playlist: $playlistName")
        }
        
        binding.optionRename.setOnClickListener {
            dismiss()
            showRenameDialog()
        }
        
        binding.optionShare.setOnClickListener {
            dismiss()
            onShare?.invoke()
            ToastUtils.showInfo(requireContext(), "Sharing playlist...")
        }
        
        binding.optionDelete.setOnClickListener {
            dismiss()
            showDeleteConfirmation()
        }
    }
    
    private fun showRenameDialog() {
        val dialogBinding = DialogCreatePlaylistBinding.inflate(layoutInflater)
        dialogBinding.playlistNameInput.setText(playlistName)
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Rename Playlist")
            .setView(dialogBinding.root)
            .setPositiveButton("Rename") { _, _ ->
                val newName = dialogBinding.playlistNameInput.text.toString()
                if (newName.isNotBlank()) {
                    onRename?.invoke(newName)
                    ToastUtils.showSuccess(requireContext(), "Renamed to: $newName")
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDeleteConfirmation() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Playlist?")
            .setMessage("This will permanently delete \"$playlistName\". The videos will NOT be deleted from your device.")
            .setPositiveButton("Delete") { _, _ ->
                onDelete?.invoke()
                ToastUtils.showSuccess(requireContext(), "Playlist deleted")
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
