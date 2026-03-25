package com.ludoblitz.app.ui.screens

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.ludoblitz.app.databinding.BottomSheetJoinRoomBinding

/**
 * Bottom Sheet for joining a game room
 */
class JoinRoomBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetJoinRoomBinding? = null
    private val binding get() = _binding!!
    
    var onJoinRoom: ((String) -> Unit)? = null

    companion object {
        fun newInstance(onJoin: (String) -> Unit): JoinRoomBottomSheet {
            return JoinRoomBottomSheet().apply {
                onJoinRoom = onJoin
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetJoinRoomBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        binding.btnJoin.setOnClickListener {
            val roomCode = binding.etRoomCode.text.toString().uppercase()
            if (roomCode.length == 6) {
                onJoinRoom?.invoke(roomCode)
                dismiss()
            } else {
                binding.etRoomCode.error = "Enter valid 6-character code"
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
