package com.zuvy.app.ui.components

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.zuvy.app.R
import com.zuvy.app.databinding.BottomSheetAlbumOptionsBinding
import com.zuvy.app.utils.ToastUtils

class AlbumOptionsBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetAlbumOptionsBinding? = null
    private val binding get() = _binding!!
    
    private var albumName: String? = null
    private var artistName: String? = null
    private var albumArtUri: String? = null
    private var songCount: Int = 0
    
    private var onPlayAll: (() -> Unit)? = null
    private var onShuffleAll: (() -> Unit)? = null
    private var onAddToQueue: (() -> Unit)? = null
    private var onAddToPlaylist: (() -> Unit)? = null
    private var onGoToArtist: (() -> Unit)? = null
    private var onShare: (() -> Unit)? = null
    private var onDelete: (() -> Unit)? = null

    companion object {
        fun newInstance(
            albumName: String,
            artistName: String = "",
            albumArtUri: String? = null,
            songCount: Int = 0,
            onPlayAll: () -> Unit = {},
            onShuffleAll: () -> Unit = {},
            onAddToQueue: () -> Unit = {},
            onAddToPlaylist: () -> Unit = {},
            onGoToArtist: () -> Unit = {},
            onShare: () -> Unit = {},
            onDelete: () -> Unit = {}
        ): AlbumOptionsBottomSheet {
            return AlbumOptionsBottomSheet().apply {
                arguments = Bundle().apply {
                    putString("albumName", albumName)
                    putString("artistName", artistName)
                    putString("albumArtUri", albumArtUri)
                    putInt("songCount", songCount)
                }
                this.onPlayAll = onPlayAll
                this.onShuffleAll = onShuffleAll
                this.onAddToQueue = onAddToQueue
                this.onAddToPlaylist = onAddToPlaylist
                this.onGoToArtist = onGoToArtist
                this.onShare = onShare
                this.onDelete = onDelete
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = BottomSheetAlbumOptionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        albumName = arguments?.getString("albumName")
        artistName = arguments?.getString("artistName")
        albumArtUri = arguments?.getString("albumArtUri")
        songCount = arguments?.getInt("songCount", 0) ?: 0
        
        binding.titleText.text = albumName
        binding.subtitleText.text = "$songCount songs • $artistName"
        
        Glide.with(binding.albumArt)
            .load(albumArtUri)
            .placeholder(R.drawable.ic_music_note)
            .centerCrop()
            .into(binding.albumArt)
        
        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.optionPlay.setOnClickListener {
            dismiss()
            onPlayAll?.invoke()
            ToastUtils.showSuccess(requireContext(), "Playing album: $albumName")
        }
        
        binding.optionShuffle.setOnClickListener {
            dismiss()
            onShuffleAll?.invoke()
            ToastUtils.showSuccess(requireContext(), "Shuffling album: $albumName")
        }
        
        binding.optionAddQueue.setOnClickListener {
            dismiss()
            onAddToQueue?.invoke()
            ToastUtils.showSuccess(requireContext(), "Added album to queue")
        }
        
        binding.optionAddPlaylist.setOnClickListener {
            dismiss()
            onAddToPlaylist?.invoke()
            ToastUtils.showInfo(requireContext(), "Add to playlist coming soon")
        }
        
        binding.optionGoArtist.setOnClickListener {
            dismiss()
            onGoToArtist?.invoke()
            ToastUtils.showInfo(requireContext(), "Artist details coming soon")
        }
        
        binding.optionShare.setOnClickListener {
            dismiss()
            onShare?.invoke()
            ToastUtils.showInfo(requireContext(), "Sharing album...")
        }
        
        binding.optionDelete.setOnClickListener {
            dismiss()
            onDelete?.invoke()
            ToastUtils.showWarning(requireContext(), "Delete album?")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
