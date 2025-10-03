package com.example.desainmoviereview2

import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
import androidx.appcompat.widget.SearchView

class HomeFragment : Fragment() {
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var bannerAdapter: BannerAdapter
    private lateinit var movieAdapter: MovieAdapter
    private lateinit var movieListRecyclerView: RecyclerView

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    val banners = listOf(
        MovieItem(R.drawable.bg_banner_sementara1, "Interstellar", "A journey beyond the stars", "2014-11-07"),
        MovieItem(R.drawable.bg_banner_sementara2, "Inception", "Dreams within dreams", "2010-07-16"),
        MovieItem(R.drawable.bg_banner_sementara3, "Tenet", "Time runs out", "2020-09-03")
    )

    val movies = listOf(
        MovieItem(R.drawable.bg_banner_sementara1, "Interstellar", "A journey beyond the stars", "2014-11-07"),
        MovieItem(R.drawable.bg_banner_sementara2, "Inception", "Dreams within dreams", "2010-07-16"),
        MovieItem(R.drawable.bg_banner_sementara3, "Tenet", "Time runs out", "2020-09-03"),
        MovieItem(R.drawable.bg_banner_sementara1, "Interstellar", "A journey beyond the stars", "2014-11-07"),
        MovieItem(R.drawable.bg_banner_sementara2, "Inception", "Dreams within dreams", "2010-07-16"),
        MovieItem(R.drawable.bg_banner_sementara3, "Tenet", "Time runs out", "2020-09-03"),
        MovieItem(R.drawable.bg_banner_sementara1, "Interstellar", "A journey beyond the stars", "2014-11-07"),
        MovieItem(R.drawable.bg_banner_sementara2, "Inception", "Dreams within dreams", "2010-07-16"),
        MovieItem(R.drawable.bg_banner_sementara3, "Tenet", "Time runs out", "2020-09-03")
    )

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

        bannerAdapter = BannerAdapter(banners) { movie ->
            openForumPage(movie)
        }
        binding.bannerViewPager.adapter = bannerAdapter

        val startPosition = (Int.MAX_VALUE / 2) - ((Int.MAX_VALUE / 2) % banners.size)
        binding.bannerViewPager.setCurrentItem(startPosition, false)

        setupAutoSlide()

        movieListRecyclerView = binding.movieList

        movieAdapter = MovieAdapter(movies) { movie ->
            openForumPage(movie)
        }
        movieListRecyclerView.adapter = movieAdapter

        movieListRecyclerView.layoutManager = GridLayoutManager(requireContext(), 2)

        movieListRecyclerView.setHasFixedSize(true)
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
