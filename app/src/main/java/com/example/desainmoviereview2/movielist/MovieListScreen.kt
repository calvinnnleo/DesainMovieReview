package com.example.desainmoviereview2.movielist

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.ImageNotSupported
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.integration.compose.placeholder
import com.example.desainmoviereview2.MovieItem
import com.example.desainmoviereview2.network.TmdbMovie
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovieListScreen(
    uiState: MovieListUiState,
    onGenreFilterChanged: (String) -> Unit,
    onSortByChanged: (String) -> Unit,
    onMovieClicked: (MovieItem) -> Unit,
    onSearchQueryChanged: (String) -> Unit = {},
    onTmdbSearchConfirmed: (TmdbMovie) -> Unit = {},
    searchQuery: String = ""
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column(
                modifier = Modifier.background(MaterialTheme.colorScheme.background)
            ) {
                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChanged,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = { Text("Search all movies (TMDB)...") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChanged("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors()
                )
                
                // Only show filter bar when not showing TMDB results
                if (uiState.tmdbSearchResults.isEmpty()) {
                    FilterBar(
                        currentGenre = uiState.currentGenreFilter,
                        genres = uiState.filterGenres,
                        onGenreSelected = onGenreFilterChanged,
                        onSortSelected = onSortByChanged
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (searchQuery.isNotBlank() && uiState.tmdbSearchResults.isNotEmpty()) {
                // Show TMDB search results
                TmdbSearchResultsList(
                    results = uiState.tmdbSearchResults,
                    onTmdbSearchConfirmed = onTmdbSearchConfirmed
                )
            } else {
                // Show normal movie list
                if (uiState.movies.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No movies available",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(uiState.movies) { movie ->
                            MovieListItem(movie = movie, onClick = { onMovieClicked(movie) })
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun TmdbSearchResultsList(
    results: List<TmdbMovie>,
    onTmdbSearchConfirmed: (TmdbMovie) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(results) { tmdbMovie ->
            Card(
                onClick = { onTmdbSearchConfirmed(tmdbMovie) },
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .padding(12.dp)
                        .height(100.dp)
                ) {
                    GlideImage(
                        model = "https://image.tmdb.org/t/p/w200${tmdbMovie.posterPath}",
                        contentDescription = tmdbMovie.title,
                        contentScale = ContentScale.Crop,
                        loading = placeholder {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(modifier = Modifier.padding(8.dp))
                            }
                        },
                        failure = placeholder {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.ImageNotSupported, contentDescription = null)
                            }
                        },
                        modifier = Modifier
                            .width(70.dp)
                            .fillMaxHeight()
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = tmdbMovie.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2
                        )
                        Text(
                            text = tmdbMovie.releaseDate?.take(4) ?: "N/A",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = tmdbMovie.overview?.take(80)?.plus("...") ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 2,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun FilterBar(
    currentGenre: String,
    genres: List<String>,
    onGenreSelected: (String) -> Unit,
    onSortSelected: (String) -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Filter by Genre",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            SortMenu(onSortSelected = onSortSelected)
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(genres) { genre ->
                FilterChip(
                    selected = genre == currentGenre,
                    onClick = { onGenreSelected(genre) },
                    label = { Text(genre) }
                )
            }
        }
    }
}

@Composable
fun SortMenu(onSortSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val sortOptions = listOf("Default", "Rating", "Newest")

    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Default.FilterList, contentDescription = "Sort")
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            sortOptions.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSortSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalGlideComposeApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MovieListItem(movie: MovieItem, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(8.dp)
                .height(120.dp)
        ) {
            GlideImage(
                model = movie.primary_image_url,
                contentDescription = movie.title,
                loading = placeholder {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                },
                failure = placeholder {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.ImageNotSupported, contentDescription = null)
                    }
                },
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .width(80.dp)
                    .fillMaxSize()
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = movie.title ?: "Unknown Title",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2
                    )
                    Text(
                        text = movie.genres ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
                
                Column {
                    val ratingText = String.format(Locale.US, "%.1f", movie.rating ?: 0.0)
                    val yearText = movie.year ?: "N/A"
                    val runtimeText = movie.runtime_minutes?.toInt()?.let { "$it min" } ?: "N/A"
                    
                    Text(
                        text = "$yearText | $runtimeText | Rating: $ratingText",
                        style = MaterialTheme.typography.bodySmall
                    )
                    
                    val director = movie.directors
                    val writer = movie.writers
                    
                    if (director == writer && director != null) {
                         Text(
                            text = "Director & Writer: $director",
                            style = MaterialTheme.typography.labelSmall,
                             maxLines = 1
                        )
                    } else {
                        if (director != null) {
                            Text(
                                text = "Director: $director",
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1
                            )
                        }
                        if (writer != null) {
                             Text(
                                text = "Writers: $writer",
                                style = MaterialTheme.typography.labelSmall,
                                 maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}
