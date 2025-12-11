package com.example.desainmoviereview2.movielist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.desainmoviereview2.MyAppTheme
import com.example.desainmoviereview2.R
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class MovieListFragment : Fragment() {

    private val viewModel: MovieListViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                MyAppTheme {
                    val uiState by viewModel.uiState.collectAsState()
                    var searchQuery by remember { mutableStateOf("") }

                    MovieListScreen(
                        uiState = uiState,
                        onGenreFilterChanged = viewModel::setGenreFilter,
                        onSortByChanged = viewModel::setSortBy,
                        onMovieClicked = { movie ->
                            val bundle = bundleOf("movieItem" to movie)
                            findNavController().navigate(R.id.action_global_forumFragment, bundle)
                        },
                        searchQuery = searchQuery,
                        onSearchQueryChanged = { query ->
                            searchQuery = query
                            viewModel.searchMovies(query)
                        },
                        onTmdbSearchConfirmed = { tmdbMovie ->
                            viewModel.onSearchConfirmed(tmdbMovie)
                        }
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.navigationEvent.onEach { event ->
            when (event) {
                is MovieListNavigationEvent.ToRecommendation -> {
                    val action = MovieListFragmentDirections.actionMovieListFragmentToRecommendationFragment(event.imdbId)
                    findNavController().navigate(action)
                    viewModel.onNavigated()
                }
                null -> {}
            }
        }.launchIn(viewLifecycleOwner.lifecycleScope)
    }
}
