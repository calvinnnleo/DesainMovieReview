package com.example.desainmoviereview2

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.viewpager2.widget.ViewPager2
import com.example.desainmoviereview2.databinding.FragmentHomeBinding
import com.google.firebase.database.*
import java.util.Calendar

class HomeFragment : Fragment() {
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var homeBannerAdapter: HomeBannerAdapter
    private lateinit var homeListAdapter: HomeListAdapter

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var database: DatabaseReference

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

    private fun setupSearchView() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (!query.isNullOrEmpty()) {
                    searchMovie(query)
                }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return false
            }
        })
    }

    private fun searchMovie(title: String) {
        val moviesRef = database.child("movies")
        moviesRef.orderByChild("title").equalTo(title).limitToFirst(1).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    for (movieSnapshot in snapshot.children) {
                        val movie = movieSnapshot.getValue(MovieItem::class.java)
                        if (movie != null) {
                            movie.movie_id = movieSnapshot.key
                            val bundle = bundleOf("imdbID" to movie.movie_id)
                            findNavController().navigate(R.id.action_homeFragment_to_recommendationFragment, bundle)
                        }
                    }
                }
                 else {
                    Log.d("HomeFragment", "Movie with title '$title' not found.")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("HomeFragment", "Failed to search for movie.", error.toException())
            }
        })
    }

    private fun setupRecyclerViews() {
        // Initializes the adapter for the ViewPager2 (banner).
        homeBannerAdapter = HomeBannerAdapter { movie ->
            openForumPage(movie)
        }
        binding.bannerViewPager.adapter = homeBannerAdapter

        // Initializes the RecyclerView for the home movie list.
        homeListAdapter = HomeListAdapter { movie ->
            openForumPage(movie)
        }
        binding.movieList.adapter = homeListAdapter

        // Sets the layout manager to a 2-column grid. This is a good choice for displaying movie posters.
        binding.movieList.layoutManager = GridLayoutManager(requireContext(), 2)

        // SetHasFixedSize(true) tells the RecyclerView that the item size won't change
        binding.movieList.setHasFixedSize(true)
    }

    private fun fetchMovies() {
        val moviesRef = database.child("movies")
        moviesRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (_binding == null) {
                    return // View is destroyed, do not proceed
                }

                val newMovies = mutableListOf<MovieItem>()

                for (movieSnapshot in snapshot.children) {
                    val movie = movieSnapshot.getValue(MovieItem::class.java)
                    if (movie != null) {
                        movie.movie_id = movieSnapshot.key
                        newMovies.add(movie)
                    }
                }

                val currentYear = Calendar.getInstance().get(Calendar.YEAR)
                val moviesFromCurrentYear = newMovies.filter { it.year == currentYear }
                val recommendedMovies = moviesFromCurrentYear.sortedWith(
                    compareByDescending<MovieItem> { it.num_votes }
                        .thenByDescending { it.rating })

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

        if (autoSlideRunnable != null) {
            stopAutoSlideLogic()
        }

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
                            startAutoSlideLogic() // Argument removed
                            isUserInteracting = false
                        } else {
                            startAutoSlideLogic() // Argument removed
                        }
                    }
                }
            }
        }

        pageChangeCallback?.let { binding.bannerViewPager.registerOnPageChangeCallback(it) }

        startAutoSlideLogic()
    }

    private fun startAutoSlideLogic() { // Parameter removed
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
        // CORRECTED: Check the adapter's item count directly.
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
