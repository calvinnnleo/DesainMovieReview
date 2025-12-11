package com.example.desainmoviereview2.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ImageNotSupported
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.integration.compose.placeholder
import com.example.desainmoviereview2.MovieItem
import com.example.desainmoviereview2.network.TmdbMovie
import kotlinx.coroutines.delay
import com.example.desainmoviereview2.MyAppTheme


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onSearchQueryChanged: (String) -> Unit,
    onMovieClicked: (MovieItem) -> Unit,
    onMovieLongClicked: (MovieItem) -> Unit,
    onSearchConfirmed: (TmdbMovie) -> Unit,
    onClearSearchResults: () -> Unit,
    onGenreSelected: (String) -> Unit = {}
) {
    Column(modifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)
    ) {
        var searchQuery by remember { mutableStateOf("") }

        SearchTextField(
            query = searchQuery,
            onQueryChange = {
                searchQuery = it
                onSearchQueryChanged(it)
            },
            onClear = {
                searchQuery = ""
                onClearSearchResults()
            },
            modifier = Modifier.padding(16.dp)
        )

        when (uiState) {
            is HomeUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is HomeUiState.Success -> {
                if (searchQuery.isNotBlank() && uiState.searchResults.isNotEmpty()) {
                    SearchList(uiState.searchResults, onSearchConfirmed)
                } else {
                    MainContent(uiState, onMovieClicked, onMovieLongClicked, onGenreSelected)
                }
            }
            is HomeUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = uiState.message)
                }
            }
        }
    }
}

@Composable
fun SearchTextField(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier
            .fillMaxWidth(),
        label = {
            Text(
                "Search Movies"
            ) },
        leadingIcon = {
            Icon(
                Icons.Default.Search,
                contentDescription = "Search Icon"
            ) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = onClear) {
                    Icon(
                        Icons.Default.Clear,
                        contentDescription = "Clear Search"
                    )
                }
            }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { keyboardController?.hide() })
    )
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalGlideComposeApi::class)
@Composable
fun MainContent(
    uiState: HomeUiState.Success,
    onMovieClicked: (MovieItem) -> Unit,
    onMovieLongClicked: (MovieItem) -> Unit,
    onGenreSelected: (String) -> Unit
) {
    LazyColumn {
        // Banner Pager
        if (uiState.banners.isNotEmpty()) {
            item {
                val pageCount = Int.MAX_VALUE
                val startIndex = pageCount / 2
                val pagerState = rememberPagerState(initialPage = startIndex) { pageCount }
                LaunchedEffect(pagerState, uiState.banners.size) {
                    if (pagerState.pageCount > 0) {
                        while (true) {
                            delay(3000)
                            pagerState.animateScrollToPage((pagerState.currentPage + 1) % pagerState.pageCount)
                        }
                    }
                }

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                ) { page ->
                    val movieIndex = (page - startIndex).mod(uiState.banners.size)
                    val movie = uiState.banners[movieIndex]
                    Box(contentAlignment = Alignment.Center) {
                        // Background image that fills the space, blurred
                        GlideImage(
                            model = movie.primary_image_url,
                            contentDescription = null, // Decorative
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .blur(radius = 16.dp)
                        )
                        // Main image that fits inside
                        GlideImage(
                            model = movie.primary_image_url,
                            contentDescription = movie.title,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }

        // Recommended Movies
        item {
            Text(
                text = "🎬 Recommended Movies",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .padding(top = 24.dp, bottom = 8.dp, start = 16.dp, end = 16.dp)

            )
        }

        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                items(uiState.movies) { movie ->
                    MovieCard(
                        movie = movie,
                        onClick = { onMovieClicked(movie) },
                        onLongClick = { onMovieLongClicked(movie) }
                    )
                }
            }
        }

        // Top Rated Movies Section
        if (uiState.topRatedMovies.isNotEmpty()) {
            item {
                Text(
                    text = "⭐ Top Rated Movies",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .padding(top = 24.dp, bottom = 8.dp, start = 16.dp, end = 16.dp)
                )
            }

            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    items(uiState.topRatedMovies) { movie ->
                        MovieCard(
                            movie = movie,
                            onClick = { onMovieClicked(movie) },
                            onLongClick = { onMovieLongClicked(movie) }
                        )
                    }
                }
            }
        }

        // Browse by Genre Section
        item {
            Text(
                text = "🎭 Browse by Genre",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .padding(top = 24.dp, bottom = 8.dp, start = 16.dp, end = 16.dp)
            )
        }

        // Genre Chips
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.genres) { genre ->
                    FilterChip(
                        selected = uiState.selectedGenre == genre,
                        onClick = { onGenreSelected(genre) },
                        label = { Text(genre) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }
        }

        // Movies by Genre
        if (uiState.moviesByGenre.isNotEmpty()) {
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    items(uiState.moviesByGenre) { movie ->
                        MovieCard(
                            movie = movie,
                            onClick = { onMovieClicked(movie) },
                            onLongClick = { onMovieLongClicked(movie) }
                        )
                    }
                }
            }
        } else {
            item {
                Text(
                    text = "No movies found for ${uiState.selectedGenre}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth()
                )
            }
        }

        // Bottom spacing
        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun SearchList(results: List<TmdbMovie>, onSearchConfirmed: (TmdbMovie) -> Unit) {
    LazyColumn(modifier = Modifier.padding(horizontal = 16.dp)) {
        items(results) { movie ->
            ListItem(
                headlineContent = { Text(movie.title) },
                supportingContent = { movie.releaseDate?.let { Text(it) } },
                leadingContent = {
                    GlideImage(
                        model = "https://image.tmdb.org/t/p/w185${movie.posterPath}",
                        contentDescription = movie.title,
                        modifier = Modifier.width(56.dp),
                        contentScale = ContentScale.Crop
                    )
                },
                modifier = Modifier.clickable { onSearchConfirmed(movie) }
            )
            Divider()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalGlideComposeApi::class, ExperimentalFoundationApi::class)
@Composable
fun MovieCard(
    movie: MovieItem,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .padding(8.dp)
            .width(150.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box {
            GlideImage(
                model = movie.primary_image_url,
                contentDescription = movie.title,
                loading = placeholder {
                    Box(
                        modifier = Modifier
                            .fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                },
                failure = placeholder {
                    Box(
                        Modifier
                            .fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.ImageNotSupported,
                            contentDescription = "Image not available"
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentScale = ContentScale.Crop
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenSuccessPreview() {
    MyAppTheme {
        HomeScreen(
            uiState = HomeUiState.Success(
                banners = listOf(
                    MovieItem(title = "Banner Movie 1", primary_image_url = "", rating = 8.5),
                    MovieItem(title = "Banner Movie 2", primary_image_url = "", rating = 7.9)
                ),
                movies = List(5) {
                    MovieItem(
                        title = "Recommended Movie ${it + 1}",
                        primary_image_url = "",
                        rating = (8 - it * 0.5)
                    )
                },
                searchResults = emptyList()
            ),
            onSearchQueryChanged = {},
            onMovieClicked = {},
            onMovieLongClicked = {},
            onSearchConfirmed = {},
            onClearSearchResults = {}
        )
    }
}
