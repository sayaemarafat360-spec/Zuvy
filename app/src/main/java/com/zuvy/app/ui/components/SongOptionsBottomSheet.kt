package com.zuvy.app.ui.components

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.FileProvider
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.zuvy.app.R
import com.zuvy.app.data.model.Song
import com.zuvy.app.databinding.BottomSheetSongOptionsBinding
import com.zuvy.app.databinding.DialogMediaDetailsBinding
import com.zuvy.app.utils.ToastUtils
import java.io.File

class SongOptionsBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetSongOptionsBinding? = null
    private val binding get() = _binding!!
    
    private var song: Song? = null
    private var onPlayNext: (() -> Unit)? = null
    private var onAddToQueue: (() -> Unit)? = null
    private var onAddToPlaylist: (() -> Unit)? = null
    private var onAddToFavorites: (() -> Unit)? = null
    private var onGoToArtist: (() -> Unit)? = null
    private var onGoToAlbum: (() -> Unit)? = null
    private var onSetRingtone: (() -> Unit)? = null

    companion object {
        fun newInstance(
            song: Song,
            onPlayNext: () -> Unit = {},
            onAddToQueue: () -> Unit = {},
            onAddToPlaylist: () -> Unit = {},
            onAddToFavorites: () -> Unit = {},
            onGoToArtist: () -> Unit = {},
            onGoToAlbum: () -> Unit = {},
            onSetRingtone: () -> Unit = {}
        ): SongOptionsBottomSheet {
            return SongOptionsBottomSheet().apply {
                arguments = Bundle().apply {
                    putParcelable("song", song)
                }
                this.onPlayNext = onPlayNext
                this.onAddToQueue = onAddToQueue
                this.onAddToPlaylist = onAddToPlaylist
                this.onAddToFavorites = onAddToFavorites
                this.onGoToArtist = onGoToArtist
                this.onGoToAlbum = onGoToAlbum
                this.onSetRingtone = onSetRingtone
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = BottomSheetSongOptionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        song = arguments?.getParcelable("song")
        
        song?.let { item ->
            binding.titleText.text = item.title
            binding.subtitleText.text = item.artist + " • " + item.duration
        }
        
        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.optionPlayNext.setOnClickListener {
            dismiss()
            onPlayNext?.invoke()
            ToastUtils.showSuccess(requireContext(), "Added to play next ⏭️")
        }
        
        binding.optionAddQueue.setOnClickListener {
            dismiss()
            onAddToQueue?.invoke()
            ToastUtils.showSuccess(requireContext(), "Added to queue 📋")
        }
        
        binding.optionAddPlaylist.setOnClickListener {
            dismiss()
            onAddToPlaylist?.invoke()
            showPlaylistDialog()
        }
        
        binding.optionAddFavorites.setOnClickListener {
            dismiss()
            onAddToFavorites?.invoke()
            ToastUtils.showSuccess(requireContext(), "Added to favorites ❤️")
        }
        
        binding.optionGoArtist.setOnClickListener {
            dismiss()
            onGoToArtist?.invoke()
            ToastUtils.showInfo(requireContext(), "Artist details coming soon")
        }
        
        binding.optionGoAlbum.setOnClickListener {
            dismiss()
            onGoToAlbum?.invoke()
            ToastUtils.showInfo(requireContext(), "Album details coming soon")
        }
        
        binding.optionRingtone.setOnClickListener {
            dismiss()
            onSetRingtone?.invoke()
            song?.let { item ->
                com.zuvy.app.utils.RingtoneUtils.showRingtoneTypeDialog(
                    requireContext(),
                    item.uri,
                    item.title,
                    parentFragmentManager
                )
            }
        }
        
        binding.optionShare.setOnClickListener {
            dismiss()
            shareSong()
        }
        
        binding.optionDetails.setOnClickListener {
            dismiss()
            showDetailsDialog()
        }
        
        binding.optionDelete.setOnClickListener {
            dismiss()
            showDeleteConfirmation()
        }
    }

    private fun showPlaylistDialog() {
        val playlists = arrayOf("Favorites", "Workout Mix", "Chill Vibes", "Road Trip", "+ Create New")
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Add to Playlist")
            .setItems(playlists) { _, which ->
                if (which == playlists.size - 1) {
                    ToastUtils.showInfo(requireContext(), "Create playlist coming soon")
                } else {
                    ToastUtils.showSuccess(requireContext(), "Added to ${playlists[which]}")
                }
            }
            .show()
    }

    private fun shareSong() {
        song?.let { item ->
            try {
                val file = File(item.uri.path ?: "")
                if (file.exists()) {
                    val uri: Uri = FileProvider.getUriForFile(
                        requireContext(),
                        "${requireContext().packageName}.fileprovider",
                        file
                    )
                    
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "audio/*"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    
                    startActivity(Intent.createChooser(shareIntent, "Share song"))
                    ToastUtils.showInfo(requireContext(), "Sharing...")
                } else {
                    ToastUtils.showError(requireContext(), "File not found")
                }
            } catch (e: Exception) {
                ToastUtils.showError(requireContext(), "Failed to share")
            }
        }
    }

    private fun showDetailsDialog() {
        song?.let { item ->
            val dialogBinding = DialogMediaDetailsBinding.inflate(layoutInflater)
            
            dialogBinding.fileName.text = item.title
            dialogBinding.filePath.text = item.uri.path ?: "Unknown"
            dialogBinding.duration.text = item.duration
            dialogBinding.mimeType.text = "audio/*"
            dialogBinding.resolution.text = "N/A"
            dialogBinding.dateAdded.text = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
                .format(java.util.Date(item.dateAdded * 1000))
            
            // Additional song info
            val artistInfo = "Artist: ${item.artist}\nAlbum: ${item.album ?: "Unknown"}\nYear: ${if (item.year > 0) item.year else "N/A"}"
            
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Song Details")
                .setView(dialogBinding.root)
                .setPositiveButton("OK", null)
                .show()
        }
    }

    private fun showDeleteConfirmation() {
        song?.let { item ->
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Delete Song?")
                .setMessage("This will permanently delete \"${item.title}\" from your device.")
                .setPositiveButton("Delete") { _, _ ->
                    ToastUtils.showSuccess(requireContext(), "Song deleted")
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
