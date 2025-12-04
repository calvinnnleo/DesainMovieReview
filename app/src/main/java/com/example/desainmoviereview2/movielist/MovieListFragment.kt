package com.example.desainmoviereview2.movielist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.desainmoviereview2.MyAppTheme
import com.example.desainmoviereview2.R

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

                    MovieListScreen(
                        uiState = uiState,
                        onGenreFilterChanged = viewModel::setGenreFilter,
                        onSortByChanged = viewModel::setSortBy,
                        onMovieClicked = { movie ->
                            val bundle = bundleOf("movieItem" to movie)
                            findNavController().navigate(R.id.action_global_forumFragment, bundle)
                        }
                    )
                }
            }
        }
    }
}
