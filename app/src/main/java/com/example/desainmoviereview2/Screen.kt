package com.example.desainmoviereview2

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Login : Screen("login")
    object Register : Screen("register")
    object Home : Screen("home")
    object MovieList : Screen("movieList")
    object Recommendation : Screen("recommendation")
    object Forum : Screen("forum")
    object Profile : Screen("profile")
}