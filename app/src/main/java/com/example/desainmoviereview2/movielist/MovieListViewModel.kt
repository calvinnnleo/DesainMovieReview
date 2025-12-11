package com.example.desainmoviereview2.movielist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.desainmoviereview2.MovieItem
import com.example.desainmoviereview2.network.AddMovieRequest
import com.example.desainmoviereview2.network.ApiClient
import com.example.desainmoviereview2.network.TmdbMovie
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.GenericTypeIndicator
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class MovieListViewModel : ViewModel() {

    private val database = FirebaseDatabase.getInstance("https://movie-recommendation-b7ce0-default-rtdb.asia-southeast1.firebasedatabase.app").reference
    private val tmdbApiKey = "945f4f99890ccfe89cf2b99acb95b940"
    
    private val _uiState = MutableStateFlow(MovieListUiState())
    val uiState: StateFlow<MovieListUiState> = _uiState.asStateFlow()

    private val _navigationEvent = MutableStateFlow<MovieListNavigationEvent?>(null)
    val navigationEvent: StateFlow<MovieListNavigationEvent?> = _navigationEvent.asStateFlow()

    private val allMovies = mutableListOf<MovieItem>()

    init {
        fetchMovies()
    }

    private fun fetchMovies() {
        _uiState.update { it.copy(isLoading = true) }
        database.child("movies").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                allMovies.clear()
                for (child in snapshot.children) {
                    parseMovie(child)?.let { allMovies.add(it) }
                }
                applyFiltersAndSorting()
            }

            override fun onCancelled(error: DatabaseError) {
                _uiState.update { it.copy(isLoading = false) }
            }
        })
    }

    fun setGenreFilter(genre: String) {
        _uiState.update { it.copy(currentGenreFilter = genre) }
        applyFiltersAndSorting()
    }

    fun setSortBy(sortBy: String) {
        _uiState.update { it.copy(currentSortBy = sortBy) }
        applyFiltersAndSorting()
    }

    // TMDB Search
    fun searchMovies(query: String) {
        if (query.isBlank()) {
            clearSearchResults()
            return
        }
        viewModelScope.launch {
            try {
                val response = ApiClient.tmdbService.searchMovies(tmdbApiKey, query)
                _uiState.update { it.copy(tmdbSearchResults = response.results) }
            } catch (e: Exception) {
                clearSearchResults()
            }
        }
    }

    fun onSearchConfirmed(tmdbMovie: TmdbMovie) {
        viewModelScope.launch {
            try {
                val moviesRef = database.child("movies")
                val snapshot = moviesRef.orderByChild("title").equalTo(tmdbMovie.title).limitToFirst(1).get().await()
                if (snapshot.exists()) {
                    val movieSnapshot = snapshot.children.first()
                    val movie = parseMovie(movieSnapshot)
                    if (movie?.movie_id != null) {
                        _navigationEvent.value = MovieListNavigationEvent.ToRecommendation(movie.movie_id!!)
                    }
                } else {
                    addMovieFlow(tmdbMovie)
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Failed to search for movie in DB.") }
            }
        }
    }

    private suspend fun addMovieFlow(tmdbMovie: TmdbMovie) {
        try {
            val movieDetails = ApiClient.tmdbService.getMovieDetails(tmdbMovie.id, tmdbApiKey)
            val imdbId = movieDetails.imdbId

            if (imdbId.isNullOrBlank()) {
                _uiState.update { it.copy(errorMessage = "Could not find IMDb ID for this movie.") }
                return
            }

            val addMovieRequest = AddMovieRequest(imdbId)
            val addMovieResponse = ApiClient.recommendationService.addMovie(addMovieRequest)

            if (addMovieResponse.status == "ok") {
                _navigationEvent.value = MovieListNavigationEvent.ToRecommendation(imdbId)
            } else {
                _uiState.update { it.copy(errorMessage = "Error: ${addMovieResponse.message}") }
            }

        } catch (e: Exception) {
            _uiState.update { it.copy(errorMessage = "An error occurred while adding the movie.") }
        }
    }

    fun clearSearchResults() {
        _uiState.update { it.copy(tmdbSearchResults = emptyList()) }
    }

    fun onNavigated() {
        _navigationEvent.value = null
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun applyFiltersAndSorting() {
        val currentState = _uiState.value
        val filtered = if (currentState.currentGenreFilter == "All") {
            allMovies
        } else {
            allMovies.filter { it.genres?.contains(currentState.currentGenreFilter, ignoreCase = true) == true }
        }

        val sorted = when (currentState.currentSortBy) {
            "Default" -> filtered.sortedWith(compareByDescending<MovieItem> { it.num_votes }.thenByDescending { it.rating })
            "Rating" -> filtered.sortedByDescending { it.rating }
            "Newest" -> filtered.sortedByDescending { it.year?.toIntOrNull() }
            else -> filtered.sortedByDescending { it.title }
        }

        _uiState.update {
            it.copy(
                movies = sorted.take(100),
                isLoading = false
            )
        }
    }

    private fun parseMovie(movieSnapshot: DataSnapshot): MovieItem? {
        val movieMap = movieSnapshot.getValue(object : GenericTypeIndicator<Map<String, Any?>>() {})
        return if (movieMap != null) {
            val rating = (movieMap["rating"] as? Number)?.toDouble()
            val numVotes = (movieMap["num_votes"] as? Number)?.toDouble()
            val runtimeMinutes = (movieMap["runtime_minutes"] as? Number)?.toDouble()

            MovieItem(
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

data class MovieListUiState(
    val movies: List<MovieItem> = emptyList(),
    val isLoading: Boolean = false,
    val currentGenreFilter: String = "All",
    val currentSortBy: String = "Default",
    val filterGenres: List<String> = listOf("All", "Action", "Comedy", "Drama", "Horror", "Thriller", "Sci-Fi"),
    val tmdbSearchResults: List<TmdbMovie> = emptyList(),
    val errorMessage: String? = null
)

sealed class MovieListNavigationEvent {
    data class ToRecommendation(val imdbId: String) : MovieListNavigationEvent()
}

