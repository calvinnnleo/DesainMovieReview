package com.example.desainmoviereview2.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.desainmoviereview2.MovieItem
import com.example.desainmoviereview2.network.AddMovieRequest
import com.example.desainmoviereview2.network.ApiClient
import com.example.desainmoviereview2.network.TmdbMovie
import com.google.firebase.database.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Calendar

class HomeViewModel : ViewModel() {

    private val database: DatabaseReference = FirebaseDatabase.getInstance("https://movie-recommendation-b7ce0-default-rtdb.asia-southeast1.firebasedatabase.app").reference
    private val tmdbApiKey = "24e64bebed4e33fec97d673f70409451"

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _navigationEvent = MutableStateFlow<NavigationEvent?>(null)
    val navigationEvent: StateFlow<NavigationEvent?> = _navigationEvent.asStateFlow()

    private var allMovies = mutableListOf<MovieItem>()

    init {
        fetchMovies()
    }

    private fun fetchMovies() {
        database.child("movies").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                allMovies.clear()
                for (movieSnapshot in snapshot.children) {
                    val movie = parseMovie(movieSnapshot)
                    if (movie != null) allMovies.add(movie)
                }

                val currentYear = Calendar.getInstance().get(Calendar.YEAR)
                val moviesFromCurrentYear = allMovies.filter { it.year?.toIntOrNull() == currentYear }
                val recommendedMovies = moviesFromCurrentYear.sortedWith(
                    compareByDescending<MovieItem> { it.num_votes ?: 0.0 }.thenByDescending { it.rating ?: 0.0 }
                )

                val banners: List<MovieItem>
                val movieList: List<MovieItem>

                if (recommendedMovies.isNotEmpty()) {
                    banners = recommendedMovies.take(3)
                    movieList = recommendedMovies.take(10)
                } else {
                    banners = emptyList()
                    movieList = emptyList()
                }

                // Top Rated Movies - sorted by rating descending
                val topRatedMovies = allMovies
                    .filter { (it.rating ?: 0.0) >= 7.0 }
                    .sortedByDescending { it.rating }
                    .take(10)

                // Genre-specific movie lists
                val comedyMovies = allMovies
                    .filter { it.genres?.contains("Comedy", ignoreCase = true) == true }
                    .sortedByDescending { it.rating }
                    .take(10)

                val dramaMovies = allMovies
                    .filter { it.genres?.contains("Drama", ignoreCase = true) == true }
                    .sortedByDescending { it.rating }
                    .take(10)

                val horrorMovies = allMovies
                    .filter { it.genres?.contains("Horror", ignoreCase = true) == true }
                    .sortedByDescending { it.rating }
                    .take(10)

                val actionMovies = allMovies
                    .filter { it.genres?.contains("Action", ignoreCase = true) == true }
                    .sortedByDescending { it.rating }
                    .take(10)

                _uiState.value = HomeUiState.Success(
                    banners = banners, 
                    movies = movieList, 
                    searchResults = emptyList(),
                    topRatedMovies = topRatedMovies,
                    comedyMovies = comedyMovies,
                    dramaMovies = dramaMovies,
                    horrorMovies = horrorMovies,
                    actionMovies = actionMovies
                )
            }

            override fun onCancelled(error: DatabaseError) {
                _uiState.value = HomeUiState.Error(error.message)
            }
        })
    }


    fun searchMovies(query: String) {
        if (query.isBlank()) {
            clearSearchResults()
            return
        }
        viewModelScope.launch {
            try {
                val response = ApiClient.tmdbService.searchMovies(tmdbApiKey, query)
                val currentState = _uiState.value
                if (currentState is HomeUiState.Success) {
                    _uiState.value = currentState.copy(searchResults = response.results)
                }
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
                        _navigationEvent.value = NavigationEvent.ToRecommendation(movie.movie_id!!)
                    }
                } else {
                    addMovieFlow(tmdbMovie)
                }
            } catch (e: Exception) {
                 _uiState.value = HomeUiState.Error("Failed to search for movie in DB.")
            }
        }
    }

    private suspend fun addMovieFlow(tmdbMovie: TmdbMovie) {
        try {
            val movieDetails = ApiClient.tmdbService.getMovieDetails(tmdbMovie.id, tmdbApiKey)
            val imdbId = movieDetails.imdbId

            if (imdbId.isNullOrBlank()) {
                 _uiState.value = HomeUiState.Error("Could not find IMDb ID for this movie.")
                return
            }

            val addMovieRequest = AddMovieRequest(imdbId)
            val addMovieResponse = ApiClient.recommendationService.addMovie(addMovieRequest)

            if (addMovieResponse.status == "ok") {
                _navigationEvent.value = NavigationEvent.ToRecommendation(imdbId)
            } else {
                 _uiState.value = HomeUiState.Error("Error: ${addMovieResponse.message}")
            }

        } catch (e: Exception) {
            _uiState.value = HomeUiState.Error("An error occurred while adding the movie.")
        }
    }
    
    fun clearSearchResults() {
        val currentState = _uiState.value
        if (currentState is HomeUiState.Success) {
            _uiState.value = currentState.copy(searchResults = emptyList())
        }
    }

    fun onNavigated() {
        _navigationEvent.value = null
    }

    private fun parseMovie(movieSnapshot: DataSnapshot): MovieItem? {
        val movieMap = movieSnapshot.getValue(object : GenericTypeIndicator<Map<String, Any?>>() {})
        return if (movieMap != null) {
            val rating = (movieMap["rating"] as? Number)?.toDouble() ?: 0.0
            val numVotes = (movieMap["num_votes"] as? Number)?.toDouble() ?: 0.0
            val runtimeMinutes = (movieMap["runtime_minutes"] as? Number)?.toDouble() ?: 0.0
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

sealed class HomeUiState {
    object Loading : HomeUiState()
    data class Success(
        val banners: List<MovieItem>, 
        val movies: List<MovieItem>, 
        val searchResults: List<TmdbMovie>,
        val topRatedMovies: List<MovieItem> = emptyList(),
        val comedyMovies: List<MovieItem> = emptyList(),
        val dramaMovies: List<MovieItem> = emptyList(),
        val horrorMovies: List<MovieItem> = emptyList(),
        val actionMovies: List<MovieItem> = emptyList()
    ) : HomeUiState()
    data class Error(val message: String) : HomeUiState()
}

sealed class NavigationEvent {
    data class ToRecommendation(val imdbId: String) : NavigationEvent()
}
