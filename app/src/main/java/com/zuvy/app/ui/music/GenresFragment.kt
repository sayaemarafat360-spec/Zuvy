package com.zuvy.app.ui.music

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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.zuvy.app.R
import com.zuvy.app.data.model.Genre
import com.zuvy.app.databinding.FragmentGenresBinding
import com.zuvy.app.databinding.ItemGenreBinding
import com.zuvy.app.utils.ToastUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class GenresFragment : Fragment() {

    private var _binding: FragmentGenresBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MusicViewModel by activityViewModels()
    private lateinit var genresAdapter: GenresAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGenresBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        observeData()
    }

    private fun setupRecyclerView() {
        genresAdapter = GenresAdapter { genre ->
            navigateToGenreDetail(genre)
        }
        binding.genresRecyclerView.apply {
            adapter = genresAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun observeData() {
        // For now, show sample data
        val sampleGenres = listOf(
            Genre(1, "Pop", 120),
            Genre(2, "Hip-Hop", 85),
            Genre(3, "Rock", 64),
            Genre(4, "Electronic", 48),
            Genre(5, "R&B", 36),
            Genre(6, "Jazz", 24),
            Genre(7, "Classical", 18),
            Genre(8, "Country", 15)
        )
        genresAdapter.submitList(sampleGenres)
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Observe genres from ViewModel when implemented
            }
        }
    }

    private fun navigateToGenreDetail(genre: Genre) {
        ToastUtils.showInfo(requireContext(), "Genre: ${genre.name}")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    inner class GenresAdapter(
        private val onItemClick: (Genre) -> Unit
    ) : RecyclerView.Adapter<GenresAdapter.GenreViewHolder>() {

        private val items = mutableListOf<Genre>()

        fun submitList(newItems: List<Genre>) {
            items.clear()
            items.addAll(newItems)
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GenreViewHolder {
            val binding = ItemGenreBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            return GenreViewHolder(binding)
        }

        override fun onBindViewHolder(holder: GenreViewHolder, position: Int) {
            holder.bind(items[position])
        }

        override fun getItemCount() = items.size

        inner class GenreViewHolder(
            private val binding: ItemGenreBinding
        ) : RecyclerView.ViewHolder(binding.root) {

            init {
                binding.root.setOnClickListener {
                    val position = bindingAdapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        onItemClick(items[position])
                    }
                }
            }

            fun bind(genre: Genre) {
                binding.genreName.text = genre.name
                binding.songCount.text = "${genre.songCount} songs"
            }
        }
    }
}
