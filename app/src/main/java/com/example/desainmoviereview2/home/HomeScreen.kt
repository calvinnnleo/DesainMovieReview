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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ImageNotSupported
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.integration.compose.placeholder
import com.example.desainmoviereview2.MovieItem
import com.example.desainmoviereview2.Screen
import com.example.desainmoviereview2.network.TmdbMovie
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    navController: NavController,
    homeViewModel: HomeViewModel = viewModel()
) {
    val uiState by homeViewModel.uiState.collectAsState()

    LaunchedEffect(homeViewModel.navigationEvent) {
        homeViewModel.navigationEvent.collectLatest {
            when(it) {
                is NavigationEvent.ToForum -> {
                    it.movie.movie_id?.let { movieId ->
                        navController.navigate("${Screen.Forum.route}/$movieId")
                    }
                }
                is NavigationEvent.ToRecommendation -> {
                    navController.navigate("${Screen.Recommendation.route}/${it.imdbId}")
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        var searchQuery by remember { mutableStateOf("") }
        SearchTextField(
            query = searchQuery,
            onQueryChange = {
                searchQuery = it
                homeViewModel.searchMovies(it)
            },
            onClear = {
                searchQuery = ""
                homeViewModel.clearSearchResults()
            },
            modifier = Modifier.padding(16.dp)
        )
        when (val state = uiState) {
            is HomeUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is HomeUiState.Success -> {
                if (searchQuery.isNotBlank() && state.searchResults.isNotEmpty()) {
                    SearchList(state.searchResults, homeViewModel::onSearchConfirmed)
                } else {
                    MainContent(
                        uiState = state,
                        onMovieClicked = {
                            it.movie_id?.let { movieId ->
                                navController.navigate("${Screen.Forum.route}/$movieId")
                            }
                        },
                        onMovieLongClicked = {
                            it.movie_id?.let { movieId ->
                                navController.navigate("${Screen.Recommendation.route}/$movieId")
                            }
                        }
                    )
                }
            }
            is HomeUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = state.message)
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
    onMovieLongClicked: (MovieItem) -> Unit
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
                        .height(200.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    pageSpacing = 8.dp
                ) { page ->
                    val movieIndex = (page - startIndex).mod(uiState.banners.size)
                    val movie = uiState.banners[movieIndex]

                    val slantedEdgeShape = object : Shape {
                        override fun createOutline(
                            size: androidx.compose.ui.geometry.Size,
                            layoutDirection: LayoutDirection,
                            density: Density
                        ): androidx.compose.ui.graphics.Outline {
                            val path = Path().apply {
                                moveTo(size.width, 0f)
                                lineTo(size.width, size.height)
                                lineTo(0f, size.height)
                                lineTo(size.width * 0.3f, 0f)
                                close()
                            }
                            return androidx.compose.ui.graphics.Outline.Generic(path)
                        }
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable { onMovieClicked(movie) },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Left side: Text information
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = 16.dp, end = 8.dp, top = 16.dp, bottom = 16.dp),
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = movie.getYearString() ?: "Releasing",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = movie.title ?: "No Title",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = movie.overview ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                // Genres
                                movie.genres?.let { genreString ->
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        items(genreString.split(',').map { it.trim() }.take(3)) { genre ->
                                            Text(
                                                text = genre,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary,
                                            )
                                        }
                                    }
                                }
                            }

                            // Right side: Image
                            GlideImage(
                                model = movie.primary_image_url,
                                contentDescription = movie.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .weight(0.8f)
                                    .fillMaxHeight()
                                    .clip(slantedEdgeShape)
                            )
                        }
                    }
                }
            }
        }

        // Recommended Movies
        item {
            Text(
                text = "Recommended Movies",
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
        // Action Movies Section
        if (uiState.actionMovies.isNotEmpty()) {
            item {
                Text(
                    text = "🔥 Action",
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
                    items(uiState.actionMovies) { movie ->
                        MovieCard(
                            movie = movie,
                            onClick = { onMovieClicked(movie) },
                            onLongClick = { onMovieLongClicked(movie) }
                        )
                    }
                }
            }
        }

        // Comedy Movies Section
        if (uiState.comedyMovies.isNotEmpty()) {
            item {
                Text(
                    text = "😂 Comedy",
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
                    items(uiState.comedyMovies) { movie ->
                        MovieCard(
                            movie = movie,
                            onClick = { onMovieClicked(movie) },
                            onLongClick = { onMovieLongClicked(movie) }
                        )
                    }
                }
            }
        }

        // Drama Movies Section
        if (uiState.dramaMovies.isNotEmpty()) {
            item {
                Text(
                    text = "🎭 Drama",
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
                    items(uiState.dramaMovies) { movie ->
                        MovieCard(
                            movie = movie,
                            onClick = { onMovieClicked(movie) },
                            onLongClick = { onMovieLongClicked(movie) }
                        )
                    }
                }
            }
        }

        // Horror Movies Section
        if (uiState.horrorMovies.isNotEmpty()) {
            item {
                Text(
                    text = "👻 Horror",
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
                    items(uiState.horrorMovies) { movie ->
                        MovieCard(
                            movie = movie,
                            onClick = { onMovieClicked(movie) },
                            onLongClick = { onMovieLongClicked(movie) }
                        )
                    }
                }
            }
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
                        modifier = Modifier
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
