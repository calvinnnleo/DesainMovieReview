package com.example.desainmoviereview2.recommendation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.ImageNotSupported
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.integration.compose.placeholder
import com.example.desainmoviereview2.MovieItem
import com.example.desainmoviereview2.movielist.MovieListItem

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun RecommendationScreen(
    onMovieClicked: (MovieItem) -> Unit,
    viewModel: RecommendationViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val searchedMovie = uiState.searchedMovie
    val recommendations = uiState.recommendations
    val isLoading = uiState.isLoading

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
        floatingActionButton = {
            if (searchedMovie != null) {
                MultiFloatingActionButton(
                    searchedMovie = searchedMovie,
                    onGoToForumClicked = { onMovieClicked(it) },
                    onRefreshClicked = { viewModel.fetchRecommendations(searchedMovie.movie_id) }
                )
            }
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            CompositionLocalProvider(
                LocalContentColor provides MaterialTheme.colorScheme.onBackground
            ) {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 16.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    item {
                        if (searchedMovie != null) {
                            SearchedMovieHeader(
                                movie = searchedMovie,
                                onClick = { onMovieClicked(searchedMovie) }
                            )
                            Divider(modifier = Modifier.padding(16.dp))
                        }
                    }

                    item {
                        Text(
                            text = "Maybe you will like",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }

                    items(recommendations) { movie ->
                        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                            MovieListItem(movie = movie, onClick = { onMovieClicked(movie) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MultiFloatingActionButton(
    searchedMovie: MovieItem,
    onGoToForumClicked: (MovieItem) -> Unit,
    onRefreshClicked: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(targetValue = if (isExpanded) 45f else 0f)

    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AnimatedVisibility(visible = isExpanded) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Go to Forum", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    SmallFloatingActionButton(
                        onClick = {
                            onGoToForumClicked(searchedMovie)
                            isExpanded = false
                        },
                        shape = CircleShape,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Icon(Icons.Filled.Chat, contentDescription = "Go to Forum")
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Refresh", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    SmallFloatingActionButton(
                        onClick = {
                            onRefreshClicked()
                            isExpanded = false
                        },
                        shape = CircleShape,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh Recommendations")
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { isExpanded = !isExpanded },
            shape = CircleShape
        ) {
            Icon(
                Icons.Filled.Add,
                contentDescription = "Open FAB Menu",
                modifier = Modifier.rotate(rotation)
            )
        }
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun SearchedMovieHeader(
    movie: MovieItem,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
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
            contentScale = ContentScale.Fit, // Or Crop depending on preference
            modifier = Modifier
                .width(150.dp)
                .height(225.dp)
                .padding(top = 16.dp)
        )

        Text(
            text = movie.title ?: "Unknown Title",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(top = 16.dp, start = 16.dp, end = 16.dp)
        )

        Text(
            text = movie.overview ?: "No overview available.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Start,
            modifier = Modifier
                .padding(top = 8.dp, start = 16.dp, end = 16.dp)
        )
    }
}
