package com.example.desainmoviereview2

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.viewpager2.widget.ViewPager2
import com.example.desainmoviereview2.databinding.FragmentHomeBinding
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*

class HomeFragment : Fragment() {
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var bannerAdapter: BannerAdapter
    private lateinit var movieAdapter: MovieAdapter
    private lateinit var movieListRecyclerView: RecyclerView

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var database: DatabaseReference

    private val banners = mutableListOf<MovieItem>()
    private val movies = mutableListOf<MovieItem>()

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
        setupAutoSlide()
    }

    private fun setupRecyclerViews() {
        bannerAdapter = BannerAdapter(banners) { movie ->
            openForumPage(movie)
        }
        binding.bannerViewPager.adapter = bannerAdapter

        movieListRecyclerView = binding.movieList
        movieAdapter = MovieAdapter(movies) { movie ->
            openForumPage(movie)
        }
        movieListRecyclerView.adapter = movieAdapter
        movieListRecyclerView.layoutManager = GridLayoutManager(requireContext(), 2)
        movieListRecyclerView.setHasFixedSize(true)
    }

    private fun fetchMovies() {
        val moviesRef = database.child("movies")
        moviesRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                movies.clear()
                banners.clear()
                for (movieSnapshot in snapshot.children) {
                    val movie = movieSnapshot.getValue(MovieItem::class.java)
                    if (movie != null) {
                        // **FIX:** Manually set the movie_id from the snapshot's key
                        movie.movie_id = movieSnapshot.key
                        movies.add(movie)
                        if (banners.size < 3) {
                            banners.add(movie)
                        }
                    }
                }
                movieAdapter.notifyDataSetChanged()
                bannerAdapter.notifyDataSetChanged()

                if (banners.isNotEmpty()) {
                    val startPosition = (Int.MAX_VALUE / 2) - ((Int.MAX_VALUE / 2) % banners.size)
                    binding.bannerViewPager.setCurrentItem(startPosition, false)
                    startAutoSlideLogic(true)
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
        if (banners.isEmpty()) return

        autoSlideRunnable = Runnable { 
            _binding?.let {
                val currentItem = it.bannerViewPager.currentItem
                val nextItem = currentItem + 1
                it.bannerViewPager.setCurrentItem(nextItem, true)
            }
        }

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
                            startAutoSlideLogic(true)
                            isUserInteracting = false
                        } else {
                            startAutoSlideLogic(false)
                        }
                    }
                }
            }
        }

        pageChangeCallback?.let { binding.bannerViewPager.registerOnPageChangeCallback(it) }

        startAutoSlideLogic(true) 
    }

    private fun startAutoSlideLogic(initialDelay: Boolean) {
        autoSlideRunnable?.let { runnable ->
            handler.removeCallbacks(runnable)
            val delay = if (initialDelay) autoSlideDelay else autoSlideDelay
            handler.postDelayed(runnable, delay)
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
        if (banners.isNotEmpty() && _binding != null) {
            startAutoSlideLogic(true)
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
