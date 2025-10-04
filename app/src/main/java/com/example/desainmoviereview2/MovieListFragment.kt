package com.example.desainmoviereview2

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.desainmoviereview2.databinding.FragmentMovieListBinding
import com.google.firebase.database.*

class MovieListFragment : Fragment() {

    private var _binding: FragmentMovieListBinding? = null
    private val binding get() = _binding!!

    private lateinit var database: DatabaseReference
    private lateinit var movieAdapter: MovieAdapter
    private val movieList = mutableListOf<MovieItem>()
    private var movieListener: ValueEventListener? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMovieListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        database = FirebaseDatabase.getInstance("https://movie-recommendation-b7ce0-default-rtdb.asia-southeast1.firebasedatabase.app").reference

        setupRecyclerView()
        fetchMoviesFromDatabase()
    }

    private fun setupRecyclerView() {
        movieAdapter = MovieAdapter(movieList) { movie ->
            val bundle = bundleOf("movieItem" to movie)
            findNavController().navigate(R.id.action_global_forumFragment, bundle)
        }
        binding.recyclerView.layoutManager = LinearLayoutManager(context)
        binding.recyclerView.adapter = movieAdapter
    }

    private fun fetchMoviesFromDatabase() {
        val moviesRef = database.child("movies")

        movieListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                movieList.clear()
                for (snapshot in dataSnapshot.children) {
                    val movie = snapshot.getValue(MovieItem::class.java)
                    if (movie != null) {
                        movie.id = snapshot.key // Store the movie ID
                        movieList.add(movie)
                    }
                }
                movieAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.w("MovieListFragment", "loadMovies:onCancelled", databaseError.toException())
            }
        }
        moviesRef.addValueEventListener(movieListener!!)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        movieListener?.let {
            database.child("movies").removeEventListener(it)
        }
        _binding = null
    }
}
