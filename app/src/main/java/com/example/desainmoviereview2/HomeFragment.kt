package com.example.desainmoviereview2

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.example.desainmoviereview2.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var adapter: BannerAdapter

    val banners = listOf(
        MovieItem(R.drawable.bg_banner_sementara1, "Interstellar", "A journey beyond the stars"),
        MovieItem(R.drawable.bg_banner_sementara2, "Inception", "Dreams within dreams"),
        MovieItem(R.drawable.bg_banner_sementara3, "Tenet", "Time runs out")
    )

    private var autoSlideRunnable: Runnable? = null
    private val autoSlideDelay = 3000L // Use a constant for clarity

    // Store the callback to be able to unregister it
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

        if (banners.isEmpty()) {
            return
        }

        adapter = BannerAdapter(banners)
        binding.bannerViewPager.adapter = adapter

        val startPosition = (Int.MAX_VALUE / 2) - ((Int.MAX_VALUE / 2) % banners.size)
        binding.bannerViewPager.setCurrentItem(startPosition, false)

        // setupAutoSlide will also call startAutoSlide internally
        setupAutoSlide()
    }

    private fun setupAutoSlide() {
        if (banners.isEmpty()) return

        // Define the runnable once
        autoSlideRunnable = object : Runnable {
            override fun run() {
                // Ensure binding is still valid
                _binding?.let {
                    val currentItem = it.bannerViewPager.currentItem
                    val nextItem = currentItem + 1
                    it.bannerViewPager.setCurrentItem(nextItem, true)
                }
            }
        }

        // Define the page change callback
        pageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
            private var isUserInteracting = false
            private var lastUserInteractionTime = 0L

            override fun onPageScrollStateChanged(state: Int) {
                when (state) {
                    ViewPager2.SCROLL_STATE_DRAGGING -> {
                        isUserInteracting = true
                        stopAutoSlideLogic() // Stop auto-slide when user starts dragging
                    }
                    ViewPager2.SCROLL_STATE_IDLE -> {
                        if (isUserInteracting) {
                            // User finished interacting.
                            // Schedule the next auto-slide from the current position.
                            lastUserInteractionTime = System.currentTimeMillis()
                            startAutoSlideLogic(true) // Restart with delay
                            isUserInteracting = false
                        } else {
                            // Auto-scroll finished. Schedule the next one from the current position.
                            // Only schedule if not recently triggered by user interaction ending.
                            // This check helps prevent double triggers if SCROLL_STATE_IDLE
                            // fires quickly after a user interaction caused an IDLE state.
                            if (System.currentTimeMillis() - lastUserInteractionTime > 100) { // Small buffer
                                startAutoSlideLogic(false) // Continue auto-slide
                            }
                        }
                    }
                    ViewPager2.SCROLL_STATE_SETTLING -> {
                        // Page is settling into its final position.
                        // If it was an auto-scroll, the next auto-scroll will be
                        // scheduled when it becomes IDLE.
                        // If it was user scroll, isUserInteracting is true.
                    }
                }
            }

            override fun onPageSelected(position: Int) {
                // This is called when a new page is selected,
                // either programmatically or by user interaction.
                // If it was NOT a user interaction recently,
                // and auto-slide is active, ensure the next slide is scheduled.
                // This can help if onPageScrollStateChanged(IDLE) is missed.
                // However, be careful not to create conflicting schedules.
                // For now, primary logic is in onPageScrollStateChanged.
            }
        }

        // Register the callback
        pageChangeCallback?.let { binding.bannerViewPager.registerOnPageChangeCallback(it) }

        // Start the initial auto-slide
        startAutoSlideLogic(true) // Start with initial delay
    }

    private fun startAutoSlideLogic(initialDelay: Boolean) {
        // Ensure runnable is defined and binding is available
        autoSlideRunnable?.let { runnable ->
            _binding?.let {
                // Always remove previous callbacks for this runnable to prevent duplicates
                handler.removeCallbacks(runnable)
                val delay = if (initialDelay) autoSlideDelay else autoSlideDelay
                handler.postDelayed(runnable, delay)
            }
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
        // If banners are present and view is created, resume auto-slide
        if (banners.isNotEmpty() && _binding != null) {
            // Restart from the current item, respecting user's last position
            startAutoSlideLogic(true) // Use initial delay when resuming
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopAutoSlideLogic()
        pageChangeCallback?.let { binding.bannerViewPager.unregisterOnPageChangeCallback(it) }
        pageChangeCallback = null
        _binding = null
        // More thorough cleanup of handler if necessary, but removing specific runnable is usually enough
        // handler.removeCallbacksAndMessages(null)
    }
}
