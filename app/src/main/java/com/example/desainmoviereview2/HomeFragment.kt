package com.example.desainmoviereview2

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager2.widget.ViewPager2
import com.example.desainmoviereview2.databinding.FragmentHomeBinding
import com.example.desainmoviereview2.network.AddMovieRequest
import com.example.desainmoviereview2.network.ApiClient
import com.google.firebase.database.*
import kotlinx.coroutines.launch
import java.util.Calendar

class HomeFragment : Fragment() {
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var homeBannerAdapter: HomeBannerAdapter
    private lateinit var homeListAdapter: HomeListAdapter
    private lateinit var searchPredictionAdapter: SearchPredictionAdapter

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var database: DatabaseReference
    private val tmdbApiKey = "24e64bebed4e33fec97d673f70409451"

    private var autoSlideRunnable: Runnable? = null
    private val autoSlideDelay = 3000L
    private var pageChangeCallback: ViewPager2.OnPageChangeCallback? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        database = FirebaseDatabase.getInstance("https://movie-recommendation-b7ce0-default-rtdb.asia-southeast1.firebasedatabase.app").reference
        setupRecyclerViews()
        fetchMovies()
        setupSearchView()
    }

    private fun setMainContentVisibility(isVisible: Boolean) {
        val visibility = if (isVisible) View.VISIBLE else View.GONE
        binding.bannerViewPager.visibility = visibility
        binding.recommendationTitle.visibility = visibility
        binding.movieList.visibility = visibility
    }

    private fun setupSearchView() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (!query.isNullOrEmpty()) {
                    searchMovieInDb(query)
                }
                binding.searchPredictionsRecyclerView.visibility = View.GONE
                setMainContentVisibility(true)
                binding.searchView.clearFocus()
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText.isNullOrEmpty()) {
                    binding.searchPredictionsRecyclerView.visibility = View.GONE
                    setMainContentVisibility(true)
                } else {
                    fetchMoviePredictionsFromTMDb(newText)
                    setMainContentVisibility(false)
                }
                return true
            }
        })

        binding.searchView.setOnCloseListener {
            binding.searchPredictionsRecyclerView.visibility = View.GONE
            setMainContentVisibility(true)
            false
        }
    }

    private fun fetchMoviePredictionsFromTMDb(query: String) {
        lifecycleScope.launch {
            try {
                val response = ApiClient.tmdbService.searchMovies(tmdbApiKey, query)
                searchPredictionAdapter.updatePredictions(response.results)
                binding.searchPredictionsRecyclerView.visibility = if (response.results.isNotEmpty()) View.VISIBLE else View.GONE
            } catch (e: Exception) {
                Log.e("HomeFragment", "Failed to fetch movie predictions from TMDb.", e)
                binding.searchPredictionsRecyclerView.visibility = View.GONE
            }
        }
    }

    private fun searchMovieInDb(title: String, tmdbMovie: com.example.desainmoviereview2.network.TmdbMovie? = null) {
        val moviesRef = database.child("movies")
        moviesRef.orderByChild("title").equalTo(title).limitToFirst(1).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val movieSnapshot = snapshot.children.first()
                    val movie = parseMovie(movieSnapshot)
                    if (movie?.movie_id != null) {
                        navigateToRecommendation(movie.movie_id!!)
                    }
                } else {
                    if (tmdbMovie != null) {
                        addMovieFlow(tmdbMovie)
                    } else {
                        findMovieOnTMDbThenAdd(title)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("HomeFragment", "Failed to search for movie in DB.", error.toException())
                Toast.makeText(context, "Database error.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun findMovieOnTMDbThenAdd(title: String) {
        lifecycleScope.launch {
            try {
                val response = ApiClient.tmdbService.searchMovies(tmdbApiKey, title)
                if (response.results.isNotEmpty()) {
                    addMovieFlow(response.results.first())
                } else {
                    Toast.makeText(context, "Movie '$title' not found.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("HomeFragment", "Error finding movie on TMDb", e)
            }
        }
    }

    private fun addMovieFlow(tmdbMovie: com.example.desainmoviereview2.network.TmdbMovie) {
        Toast.makeText(context, "Movie not in DB. Adding...", Toast.LENGTH_SHORT).show()
        lifecycleScope.launch {
            try {
                val movieDetails = ApiClient.tmdbService.getMovieDetails(tmdbMovie.id, tmdbApiKey)
                val imdbId = movieDetails.imdbId

                if (imdbId.isNullOrBlank()) {
                    Toast.makeText(context, "Could not find IMDb ID for this movie.", Toast.LENGTH_LONG).show()
                    return@launch
                }

                val addMovieRequest = AddMovieRequest(imdbId)
                val addMovieResponse = ApiClient.recommendationService.addMovie(addMovieRequest)

                if (addMovieResponse.status == "ok") {
                    Toast.makeText(context, "Movie added! Getting recommendations...", Toast.LENGTH_SHORT).show()
                    navigateToRecommendation(imdbId)
                } else {
                    Toast.makeText(context, "Error: ${addMovieResponse.message}", Toast.LENGTH_LONG).show()
                }

            } catch (e: Exception) {
                Log.e("HomeFragment", "Failed during add movie flow", e)
                Toast.makeText(context, "An error occurred while adding the movie.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun navigateToRecommendation(imdbID: String) {
        val bundle = bundleOf("imdbID" to imdbID)
        findNavController().navigate(R.id.action_homeFragment_to_recommendationFragment, bundle)
    }

    private fun parseMovie(movieSnapshot: DataSnapshot): MovieItem? {
        val movieMap = movieSnapshot.getValue(object : GenericTypeIndicator<Map<String, Any?>>() {})
        return if (movieMap != null) {
            MovieItem(
                movie_id = movieSnapshot.key,
                title = movieMap["title"] as? String,
                year = movieMap["year"]?.toString(),
                rating = (movieMap["rating"] as? Number)?.toDouble(),
                num_votes = (movieMap["num_votes"] as? Number)?.toDouble(),
                runtime_minutes = (movieMap["runtime_minutes"] as? Number)?.toDouble(),
                directors = movieMap["directors"] as? String,
                writers = movieMap["writers"] as? String,
                genres = movieMap["genres"] as? String,
                overview = movieMap["overview"] as? String,
                crew = movieMap["crew"] as? String,
                primary_image_url = movieMap["primary_image_url"] as? String,
                thumbnail_url = movieMap["thumbnail_url"] as? String
            )
        } else {
            null
        }
    }

    private fun setupRecyclerViews() {
        homeBannerAdapter = HomeBannerAdapter { movie -> openForumPage(movie) }
        binding.bannerViewPager.adapter = homeBannerAdapter

        homeListAdapter = HomeListAdapter { movie -> openForumPage(movie) }
        binding.movieList.adapter = homeListAdapter
        binding.movieList.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.movieList.setHasFixedSize(true)

        searchPredictionAdapter = SearchPredictionAdapter(emptyList()) { tmdbMovie ->
            searchMovieInDb(tmdbMovie.title, tmdbMovie)
        }
        binding.searchPredictionsRecyclerView.layoutManager = LinearLayoutManager(context)
        binding.searchPredictionsRecyclerView.adapter = searchPredictionAdapter
    }

    private fun fetchMovies() {
        val moviesRef = database.child("movies")
        moviesRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (_binding == null) return

                val newMovies = mutableListOf<MovieItem>()
                for (movieSnapshot in snapshot.children) {
                    val movie = parseMovie(movieSnapshot)
                    if (movie != null) newMovies.add(movie)
                }

                val currentYear = Calendar.getInstance().get(Calendar.YEAR)
                val moviesFromCurrentYear = newMovies.filter { it.year?.toIntOrNull() == currentYear }
                val recommendedMovies = moviesFromCurrentYear.sortedWith(
                    compareByDescending<MovieItem> { it.num_votes }.thenByDescending { it.rating }
                )

                val newBanners = recommendedMovies.take(3)
                homeListAdapter.submitList(recommendedMovies)
                homeBannerAdapter.submitList(newBanners)

                if (newBanners.isNotEmpty()) {
                    setupAutoSlide()
                    val startPosition = (Int.MAX_VALUE / 2) - ((Int.MAX_VALUE / 2) % newBanners.size)
                    binding.bannerViewPager.setCurrentItem(startPosition, false)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("HomeFragment", "Failed to read movie data.", error.toException())
            }
        })
    }

    private fun openForumPage(movie: MovieItem) {
        val bundle = bundleOf("movieItem" to movie)
        findNavController().navigate(R.id.action_global_forumFragment, bundle)
    }

    private fun setupAutoSlide() {
        if (homeBannerAdapter.itemCount == 0) return
        if (autoSlideRunnable != null) stopAutoSlideLogic()

        autoSlideRunnable = Runnable {
            _binding?.let {
                val currentItem = it.bannerViewPager.currentItem
                val nextItem = currentItem + 1
                it.bannerViewPager.setCurrentItem(nextItem, true)
            }
        }

        pageChangeCallback?.let { binding.bannerViewPager.unregisterOnPageChangeCallback(it) }
        pageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
            private var isUserInteracting = false
            override fun onPageScrollStateChanged(state: Int) {
                when (state) {
                    ViewPager2.SCROLL_STATE_DRAGGING -> {
                        isUserInteracting = true
                        stopAutoSlideLogic()
                    }
                    ViewPager2.SCROLL_STATE_IDLE -> {
                        if (isUserInteracting) {
                            startAutoSlideLogic()
                            isUserInteracting = false
                        } else {
                            startAutoSlideLogic()
                        }
                    }
                }
            }
        }
        pageChangeCallback?.let { binding.bannerViewPager.registerOnPageChangeCallback(it) }
        startAutoSlideLogic()
    }

    private fun startAutoSlideLogic() {
        autoSlideRunnable?.let { runnable ->
            handler.removeCallbacks(runnable)
            handler.postDelayed(runnable, autoSlideDelay)
        }
    }

    private fun stopAutoSlideLogic() {
        autoSlideRunnable?.let { handler.removeCallbacks(it) }
    }

    override fun onPause() {
        super.onPause()
        stopAutoSlideLogic()
    }

    override fun onResume() {
        super.onResume()
        if (homeBannerAdapter.itemCount > 0 && _binding != null) {
            startAutoSlideLogic()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopAutoSlideLogic()
        pageChangeCallback?.let { binding.bannerViewPager.unregisterOnPageChangeCallback(it) }
        pageChangeCallback = null
        _binding = null
    }
}