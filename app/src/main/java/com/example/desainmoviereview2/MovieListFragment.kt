package com.example.desainmoviereview2

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.desainmoviereview2.databinding.FragmentMovieListBinding
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.IOException

class MovieListFragment : Fragment() {

    private var _binding: FragmentMovieListBinding? = null
    private val binding get() = _binding!!

    private lateinit var movieAdapter: MovieAdapter
    private val movieList = mutableListOf<MovieItem>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMovieListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        loadMoviesFromJson()
    }

    private fun setupRecyclerView() {
        movieAdapter = MovieAdapter(movieList) { movie ->
            val bundle = bundleOf("movieItem" to movie)
            findNavController().navigate(R.id.action_global_forumFragment, bundle)
        }
        binding.recyclerView.layoutManager = LinearLayoutManager(context)
        binding.recyclerView.adapter = movieAdapter
    }

    private fun loadMoviesFromJson() {
        try {
            val jsonString = context?.assets?.open("movies.json")?.bufferedReader().use { it?.readText() }
            if (jsonString != null) {
                val movies = Json.decodeFromString<List<MovieItem>>(jsonString)
                movieList.clear()
                movieList.addAll(movies)
                movieAdapter.notifyDataSetChanged()
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
