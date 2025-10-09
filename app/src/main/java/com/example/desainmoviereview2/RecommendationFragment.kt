package com.example.desainmoviereview2

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.desainmoviereview2.databinding.FragmentRecommendationBinding
import com.example.desainmoviereview2.network.ApiClient
import com.example.desainmoviereview2.network.RecommendationRequest
import com.google.firebase.database.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Fragment for displaying movie recommendations.
 */
class RecommendationFragment : Fragment() {

    private var _binding: FragmentRecommendationBinding? = null
    private val binding get() = _binding!!

    private val args: RecommendationFragmentArgs by navArgs()
    private lateinit var database: DatabaseReference
    private lateinit var recommendationAdapter: MovieListAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRecommendationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        database = FirebaseDatabase.getInstance("https://movie-recommendation-b7ce0-default-rtdb.asia-southeast1.firebasedatabase.app").reference

        setupRecyclerView()
        loadSearchedMovie()
        loadRecommendations()
    }

    /**
     * Sets up the RecyclerView for the recommendation list.
     */
    private fun setupRecyclerView() {
        recommendationAdapter = MovieListAdapter(mutableListOf()) { movie ->
            val bundle = bundleOf("movieItem" to movie)
            findNavController().navigate(R.id.action_global_forumFragment, bundle)
        }
        binding.recommendationList.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = recommendationAdapter
        }
    }

    /**
     * Loads the details of the movie that was searched for.
     */
    private fun loadSearchedMovie() {
        val imdbID = args.imdbID
        val movieRef = database.child("movies").child(imdbID)

        movieRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val movie = snapshot.getValue(MovieItem::class.java)
                movie?.let {
                    binding.searchedMovieTitle.text = it.title
                    binding.searchedMovieOverview.text = it.overview
                    Glide.with(requireContext())
                        .load(it.primary_image_url)
                        .into(binding.searchedMoviePoster)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("RecommendationFragment", "Failed to load searched movie", error.toException())
            }
        })
    }

    /**
     * Loads the movie recommendations from the API.
     */
    private fun loadRecommendations() {
        val imdbID = args.imdbID
        lifecycleScope.launch {
            try {
                val request = RecommendationRequest(imdbID)
                val response = ApiClient.apiService.getRecommendations(request)

                if (response.status == "ok") {
                    val recommendedMovieTitles = response.recommendations.map { it.title }
                    fetchMoviesByTitles(recommendedMovieTitles)
                }

            } catch (e: Exception) {
                Log.e("RecommendationFragment", "Failed to get recommendations", e)
            }
        }
    }

    /**
     * Fetches the full movie details for the recommended movies.
     */
    private fun fetchMoviesByTitles(titles: List<String>) {
        val moviesRef = database.child("movies")
        val recommendedMovies = mutableListOf<MovieItem>()

        lifecycleScope.launch {
            for (title in titles) {
                val snapshot = moviesRef.orderByChild("title").equalTo(title).limitToFirst(1).get().await()
                if (snapshot.exists()) {
                    for (movieSnapshot in snapshot.children) {
                        val movie = movieSnapshot.getValue(MovieItem::class.java)
                        if (movie != null) {
                            movie.movie_id = movieSnapshot.key
                            recommendedMovies.add(movie)
                        }
                    }
                }
            }
            recommendationAdapter.updateMovies(recommendedMovies)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
