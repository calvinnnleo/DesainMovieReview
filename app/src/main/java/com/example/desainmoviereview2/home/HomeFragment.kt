package com.example.desainmoviereview2.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import com.example.desainmoviereview2.MyAppTheme

class HomeFragment : Fragment() {

    private val viewModel: HomeViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                // 🌙 Wrap your whole Compose content in the app theme
                MyAppTheme {
                    val uiState = viewModel.uiState.collectAsState()

                    HomeScreen(
                        uiState = uiState.value,
                        onSearchQueryChanged = { query -> viewModel.searchMovies(query) },
                        onMovieClicked = { movie ->
                            val action = HomeFragmentDirections.actionHomeFragmentToForumFragment(movie)
                            findNavController().navigate(action)
                        },
                        onMovieLongClicked = { movie ->
                            movie.movie_id?.let {
                                val action = HomeFragmentDirections.actionHomeFragmentToRecommendationFragment(it)
                                findNavController().navigate(action)
                            }
                        },
                        onSearchConfirmed = { tmdbMovie ->
                            viewModel.onSearchConfirmed(tmdbMovie)
                        },
                        onClearSearchResults = {
                            viewModel.clearSearchResults()
                        }
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.navigationEvent.onEach { event ->
            when(event) {
                is NavigationEvent.ToRecommendation -> {
                    val action = HomeFragmentDirections.actionHomeFragmentToRecommendationFragment(event.imdbId)
                    findNavController().navigate(action)
                    viewModel.onNavigated()
                }
                null -> {}
            }
        }.launchIn(viewLifecycleOwner.lifecycleScope)
    }
}
