package com.zuvy.app.ui.home.folders

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.zuvy.app.R
import com.zuvy.app.data.model.Folder
import com.zuvy.app.databinding.FragmentFoldersBinding
import com.zuvy.app.databinding.ItemFolderBinding
import com.zuvy.app.ui.home.HomeViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class FoldersFragment : Fragment() {

    private var _binding: FragmentFoldersBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by activityViewModels()
    private lateinit var foldersAdapter: FoldersAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFoldersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        observeData()
    }

    private fun setupRecyclerView() {
        foldersAdapter = FoldersAdapter { folder ->
            navigateToFolderBrowser(folder)
        }
        binding.foldersRecyclerView.adapter = foldersAdapter
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.isLoading.collect { isLoading ->
                        if (isLoading) {
                            binding.shimmerLayout.startShimmer()
                            binding.shimmerLayout.visibility = View.VISIBLE
                            binding.foldersRecyclerView.visibility = View.GONE
                        } else {
                            binding.shimmerLayout.stopShimmer()
                            binding.shimmerLayout.visibility = View.GONE
                            binding.foldersRecyclerView.visibility = View.VISIBLE
                        }
                    }
                }

                launch {
                    viewModel.folders.collect { folders ->
                        if (folders.isEmpty()) {
                            binding.emptyState.visibility = View.VISIBLE
                            binding.foldersRecyclerView.visibility = View.GONE
                        } else {
                            binding.emptyState.visibility = View.GONE
                            binding.foldersRecyclerView.visibility = View.VISIBLE
                            // foldersAdapter.submitList(folders)
                        }
                    }
                }
            }
        }
    }

    private fun navigateToFolderBrowser(folder: Folder) {
        val bundle = Bundle().apply {
            putString("folderPath", folder.path)
        }
        findNavController().navigate(R.id.action_home_to_folderBrowser, bundle)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    inner class FoldersAdapter(
        private val onFolderClick: (Folder) -> Unit
    ) : RecyclerView.Adapter<FoldersAdapter.FolderViewHolder>() {

        private val items = mutableListOf<Folder>()

        fun submitList(newItems: List<Folder>) {
            items.clear()
            items.addAll(newItems)
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FolderViewHolder {
            val binding = ItemFolderBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            return FolderViewHolder(binding)
        }

        override fun onBindViewHolder(holder: FolderViewHolder, position: Int) {
            holder.bind(items[position])
        }

        override fun getItemCount(): Int = items.size

        inner class FolderViewHolder(
            private val binding: ItemFolderBinding
        ) : RecyclerView.ViewHolder(binding.root) {

            init {
                binding.root.setOnClickListener {
                    val position = bindingAdapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        onFolderClick(items[position])
                    }
                }
            }

            fun bind(folder: Folder) {
                binding.folderName.text = folder.name
                binding.folderPath.text = folder.path
                binding.videoCount.text = resources.getQuantityString(
                    R.plurals.videos_count_plural, folder.videoCount, folder.videoCount
                )
            }
        }
    }
}
