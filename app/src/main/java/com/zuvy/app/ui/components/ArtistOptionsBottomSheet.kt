package com.zuvy.app.ui.components

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.zuvy.app.R
import com.zuvy.app.databinding.BottomSheetArtistOptionsBinding
import com.zuvy.app.utils.ToastUtils

class ArtistOptionsBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetArtistOptionsBinding? = null
    private val binding get() = _binding!!
    
    private var artistName: String? = null
    private var artistImageUri: String? = null
    private var songCount: Int = 0
    
    private var onPlayAll: (() -> Unit)? = null
    private var onShuffleAll: (() -> Unit)? = null
    private var onAddToQueue: (() -> Unit)? = null
    private var onAddToPlaylist: (() -> Unit)? = null
    private var onShare: (() -> Unit)? = null

    companion object {
        fun newInstance(
            artistName: String,
            artistImageUri: String? = null,
            songCount: Int = 0,
            onPlayAll: () -> Unit = {},
            onShuffleAll: () -> Unit = {},
            onAddToQueue: () -> Unit = {},
            onAddToPlaylist: () -> Unit = {},
            onShare: () -> Unit = {}
        ): ArtistOptionsBottomSheet {
            return ArtistOptionsBottomSheet().apply {
                arguments = Bundle().apply {
                    putString("artistName", artistName)
                    putString("artistImageUri", artistImageUri)
                    putInt("songCount", songCount)
                }
                this.onPlayAll = onPlayAll
                this.onShuffleAll = onShuffleAll
                this.onAddToQueue = onAddToQueue
                this.onAddToPlaylist = onAddToPlaylist
                this.onShare = onShare
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = BottomSheetArtistOptionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        artistName = arguments?.getString("artistName")
        artistImageUri = arguments?.getString("artistImageUri")
        songCount = arguments?.getInt("songCount", 0) ?: 0
        
        binding.titleText.text = artistName
        binding.subtitleText.text = "$songCount songs"
        
        Glide.with(binding.artistImage)
            .load(artistImageUri)
            .placeholder(R.drawable.ic_music_note)
            .circleCrop()
            .into(binding.artistImage)
        
        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.optionPlay.setOnClickListener {
            dismiss()
            onPlayAll?.invoke()
            ToastUtils.showSuccess(requireContext(), "Playing artist: $artistName")
        }
        
        binding.optionShuffle.setOnClickListener {
            dismiss()
            onShuffleAll?.invoke()
            ToastUtils.showSuccess(requireContext(), "Shuffling artist: $artistName")
        }
        
        binding.optionAddQueue.setOnClickListener {
            dismiss()
            onAddToQueue?.invoke()
            ToastUtils.showSuccess(requireContext(), "Added all songs to queue")
        }
        
        binding.optionAddPlaylist.setOnClickListener {
            dismiss()
            onAddToPlaylist?.invoke()
            ToastUtils.showInfo(requireContext(), "Add to playlist coming soon")
        }
        
        binding.optionShare.setOnClickListener {
            dismiss()
            onShare?.invoke()
            ToastUtils.showInfo(requireContext(), "Sharing artist...")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
