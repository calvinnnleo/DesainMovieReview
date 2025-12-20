package com.example.desainmoviereview2

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.desainmoviereview2.forum.ForumScreen
import com.example.desainmoviereview2.home.HomeScreen
import com.example.desainmoviereview2.login.LoginScreen
import com.example.desainmoviereview2.movielist.MovieListScreen
import com.example.desainmoviereview2.profile.ProfileScreen
import com.example.desainmoviereview2.recommendation.RecommendationScreen
import com.example.desainmoviereview2.register.RegisterScreen
import androidx.navigation.NavGraph.Companion.findStartDestination

@Composable
fun AppNavigation(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Screen.Splash.route) {
        composable(Screen.Splash.route) {
            SplashScreen(navController = navController)
        }
        composable(Screen.Login.route) {
            LoginScreen(navController = navController)
        }
        composable(Screen.Register.route) {
            RegisterScreen(navController = navController)
        }
        composable(Screen.Home.route) {
            HomeScreen(navController = navController)
        }
        composable(Screen.MovieList.route) {
            MovieListScreen(
                onMovieClicked = { movie ->
                    movie.movie_id?.let {
                        navController.navigate("${Screen.Forum.route}/$it")
                    }
                }
            )
        }
        composable(
            route = "${Screen.Recommendation.route}/{imdbID}",
            arguments = listOf(navArgument("imdbID") { type = NavType.StringType })
        ) {
            RecommendationScreen(
                onMovieClicked = { movie ->
                    movie.movie_id?.let {
                         navController.navigate("${Screen.Forum.route}/$it")
                    }
                }
            )
        }
        composable(
            route = "${Screen.Forum.route}/{movieId}",
            arguments = listOf(navArgument("movieId") { type = NavType.StringType })
        ) {
            ForumScreen(onMovieClicked = { movie ->
                movie.movie_id?.let {
                    navController.navigate("${Screen.Recommendation.route}/$it")
                }
            })
        }
        composable(Screen.Profile.route) {
            ProfileScreen(onLogout = {
                navController.navigate(Screen.Login.route) {
                    popUpTo(navController.graph.findStartDestination().id) {
                        inclusive = true
                    }
                    launchSingleTop = true
                }
            })
        }
    }
}
