package com.zuvy.app.ui.components

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.fragment.app.activityViewModels
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.zuvy.app.R
import com.zuvy.app.data.model.MediaItem
import com.zuvy.app.databinding.BottomSheetMediaOptionsBinding
import com.zuvy.app.databinding.DialogMediaDetailsBinding
import com.zuvy.app.ui.home.HomeViewModel
import com.zuvy.app.utils.ToastUtils
import com.zuvy.app.utils.formatDuration
import com.zuvy.app.utils.formatFileSize
import dagger.hilt.android.AndroidEntryPoint
import java.io.File

@AndroidEntryPoint
class MediaOptionsBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetMediaOptionsBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: HomeViewModel by activityViewModels()
    
    private var mediaItem: MediaItem? = null
    private var onPlayNext: (() -> Unit)? = null
    private var onAddToPlaylist: (() -> Unit)? = null
    private var onAddToFavorites: (() -> Unit)? = null

    companion object {
        fun newInstance(
            mediaItem: MediaItem,
            onPlayNext: () -> Unit = {},
            onAddToPlaylist: () -> Unit = {},
            onAddToFavorites: () -> Unit = {}
        ): MediaOptionsBottomSheet {
            return MediaOptionsBottomSheet().apply {
                arguments = Bundle().apply {
                    putParcelable("mediaItem", mediaItem)
                }
                this.onPlayNext = onPlayNext
                this.onAddToPlaylist = onAddToPlaylist
                this.onAddToFavorites = onAddToFavorites
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = BottomSheetMediaOptionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        mediaItem = arguments?.getParcelable("mediaItem")
        
        mediaItem?.let { item ->
            binding.titleText.text = item.name
            binding.subtitleText.text = item.duration.formatDuration() + " • " + item.size.formatFileSize()
        }
        
        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.optionPlay.setOnClickListener {
            dismiss()
            // Play action handled by item click
        }
        
        binding.optionPlayNext.setOnClickListener {
            dismiss()
            onPlayNext?.invoke()
            ToastUtils.showSuccess(requireContext(), "Added to play next")
        }
        
        binding.optionAddPlaylist.setOnClickListener {
            dismiss()
            onAddToPlaylist?.invoke()
            ToastUtils.showInfo(requireContext(), "Add to playlist coming soon")
        }
        
        binding.optionAddFavorites.setOnClickListener {
            dismiss()
            onAddToFavorites?.invoke()
            ToastUtils.showSuccess(requireContext(), "Added to favorites ❤️")
        }
        
        binding.optionShare.setOnClickListener {
            dismiss()
            shareMedia()
        }
        
        binding.optionDetails.setOnClickListener {
            dismiss()
            showDetailsDialog()
        }
        
        binding.optionRename.setOnClickListener {
            dismiss()
            ToastUtils.showInfo(requireContext(), "Rename coming soon")
        }
        
        binding.optionDelete.setOnClickListener {
            dismiss()
            showDeleteConfirmation()
        }
        
        binding.optionHide.setOnClickListener {
            dismiss()
            ToastUtils.showInfo(requireContext(), "Hidden from library")
        }
    }

    private fun shareMedia() {
        mediaItem?.let { item ->
            try {
                val file = File(item.path)
                if (file.exists()) {
                    val uri: Uri = FileProvider.getUriForFile(
                        requireContext(),
                        "${requireContext().packageName}.fileprovider",
                        file
                    )
                    
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = item.mimeType ?: "video/*"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    
                    startActivity(Intent.createChooser(shareIntent, "Share video"))
                    ToastUtils.showInfo(requireContext(), "Sharing...")
                } else {
                    ToastUtils.showError(requireContext(), "File not found")
                }
            } catch (e: Exception) {
                ToastUtils.showError(requireContext(), "Failed to share: ${e.message}")
            }
        }
    }

    private fun showDetailsDialog() {
        mediaItem?.let { item ->
            val dialogBinding = DialogMediaDetailsBinding.inflate(layoutInflater)
            
            dialogBinding.fileName.text = item.name
            dialogBinding.filePath.text = item.path
            dialogBinding.fileSize.text = item.size.formatFileSize()
            dialogBinding.duration.text = item.duration.formatDuration()
            dialogBinding.resolution.text = if (item.width > 0) "${item.width} x ${item.height}" else "N/A"
            dialogBinding.mimeType.text = item.mimeType ?: "Unknown"
            dialogBinding.dateAdded.text = java.text.SimpleDateFormat("MMM dd, yyyy HH:mm", java.util.Locale.getDefault())
                .format(java.util.Date(item.dateAdded * 1000))
            
            val file = File(item.path)
            dialogBinding.dateModified.text = java.text.SimpleDateFormat("MMM dd, yyyy HH:mm", java.util.Locale.getDefault())
                .format(java.util.Date(file.lastModified()))
            
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Video Details")
                .setView(dialogBinding.root)
                .setPositiveButton("OK", null)
                .show()
        }
    }

    private fun showDeleteConfirmation() {
        mediaItem?.let { item ->
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Delete Video?")
                .setMessage("This will permanently delete \"${item.name}\" from your device.")
                .setPositiveButton("Delete") { _, _ ->
                    deleteMedia(item)
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun deleteMedia(item: MediaItem) {
        try {
            val file = File(item.path)
            if (file.exists()) {
                val deleted = file.delete()
                if (deleted) {
                    ToastUtils.showSuccess(requireContext(), "Video deleted")
                    viewModel.loadMedia() // Refresh list
                } else {
                    ToastUtils.showError(requireContext(), "Failed to delete")
                }
            }
        } catch (e: Exception) {
            ToastUtils.showError(requireContext(), "Error: ${e.message}")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
