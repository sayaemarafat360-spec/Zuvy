package com.ludoblitz.app.ui.screens

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.ludoblitz.app.databinding.BottomSheetCreateRoomBinding

/**
 * Bottom Sheet for creating a game room
 */
class CreateRoomBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetCreateRoomBinding? = null
    private val binding get() = _binding!!
    
    var onCreateRoom: ((String, Int) -> Unit)? = null

    companion object {
        fun newInstance(onCreate: (String, Int) -> Unit): CreateRoomBottomSheet {
            return CreateRoomBottomSheet().apply {
                onCreateRoom = onCreate
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetCreateRoomBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        binding.btnCreate.setOnClickListener {
            val roomName = binding.etRoomName.text.toString()
            val maxPlayers = when (binding.chipGroupPlayers.checkedChipId) {
                binding.chip2Players.id -> 2
                binding.chip3Players.id -> 3
                else -> 4
            }
            onCreateRoom?.invoke(roomName, maxPlayers)
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
