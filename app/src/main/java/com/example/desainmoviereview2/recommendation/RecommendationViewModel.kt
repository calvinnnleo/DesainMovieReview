package com.example.desainmoviereview2.recommendation

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.desainmoviereview2.network.ApiClient
import com.example.desainmoviereview2.network.RecommendationRequest
import com.google.firebase.database.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class RecommendationViewModel(
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val database = FirebaseDatabase.getInstance("https://movie-recommendation-b7ce0-default-rtdb.asia-southeast1.firebasedatabase.app").reference
    private val imdbID: String = checkNotNull(savedStateHandle["imdbID"])

    private val _uiState = MutableStateFlow(RecommendationUiState())
    val uiState: StateFlow<RecommendationUiState> = _uiState.asStateFlow()

    init {
        loadSearchedMovie()
        loadRecommendations()
    }

    private fun loadSearchedMovie() {
        val movieRef = database.child("movies").child(imdbID)

        movieRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val movie = parseMovie(snapshot)
                _uiState.update { it.copy(searchedMovie = movie) }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("RecommendationViewModel", "Failed to load searched movie", error.toException())
            }
        })
    }

    private fun loadRecommendations() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val request = RecommendationRequest(imdbID)
                val response = ApiClient.recommendationService.getRecommendations(request)

                if (response.status == "ok") {
                    val recommendedMovieTitles = response.recommendations.map { it.title }
                    fetchMoviesByTitles(recommendedMovieTitles)
                } else {
                    _uiState.update { it.copy(isLoading = false) }
                }

            } catch (e: Exception) {
                Log.e("RecommendationViewModel", "Failed to get recommendations", e)
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun fetchMoviesByTitles(titles: List<String>) {
        val moviesRef = database.child("movies")
        val recommendedMovies = mutableListOf<com.example.desainmoviereview2.MovieItem>()

        viewModelScope.launch {
            for (title in titles) {
                try {
                    val snapshot = moviesRef.orderByChild("title").equalTo(title).limitToFirst(1).get().await()
                    if (snapshot.exists()) {
                        for (movieSnapshot in snapshot.children) {
                            val movie = parseMovie(movieSnapshot)
                            if (movie != null) {
                                recommendedMovies.add(movie)
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("RecommendationViewModel", "Failed to fetch movie by title: $title", e)
                }
            }
            _uiState.update {
                it.copy(
                    recommendations = recommendedMovies,
                    isLoading = false
                )
            }
        }
    }

    private fun parseMovie(movieSnapshot: DataSnapshot): com.example.desainmoviereview2.MovieItem? {
        val movieMap = movieSnapshot.getValue(object : com.google.firebase.database.GenericTypeIndicator<Map<String, Any?>>() {})
        return if (movieMap != null) {
            val rating = (movieMap["rating"] as? Number)?.toDouble()
            val numVotes = (movieMap["num_votes"] as? Number)?.toDouble()
            val runtimeMinutes = (movieMap["runtime_minutes"] as? Number)?.toDouble()

            _root_ide_package_.com.example.desainmoviereview2.MovieItem(
                movie_id = movieSnapshot.key,
                title = movieMap["title"] as? String,
                year = movieMap["year"]?.toString(),
                rating = rating,
                num_votes = numVotes,
                runtime_minutes = runtimeMinutes,
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
}

data class RecommendationUiState(
    val searchedMovie: com.example.desainmoviereview2.MovieItem? = null,
    val recommendations: List<com.example.desainmoviereview2.MovieItem> = emptyList(),
    val isLoading: Boolean = true
)
