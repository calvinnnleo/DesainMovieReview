package com.example.desainmoviereview2.recommendation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.desainmoviereview2.RecommendationFragmentArgs
import com.example.desainmoviereview2.MyAppTheme
import com.example.desainmoviereview2.R

class RecommendationFragment : androidx.fragment.app.Fragment() {

    private val args: RecommendationFragmentArgs by navArgs()
    
    private val viewModel: RecommendationViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return RecommendationViewModel(
                    savedStateHandle = SavedStateHandle(mapOf("imdbID" to args.imdbID))
                ) as T
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                MyAppTheme {
                    val uiState by viewModel.uiState.collectAsState()

                    RecommendationScreen(
                        searchedMovie = uiState.searchedMovie,
                        recommendations = uiState.recommendations,
                        isLoading = uiState.isLoading,
                        onMovieClicked = { movie ->
                            val bundle = bundleOf("movieItem" to movie)
                            findNavController().navigate(
                                R.id.action_global_forumFragment,
                                bundle
                            )
                        }
                    )
                }
            }
        }
    }
}
