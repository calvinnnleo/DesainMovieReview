package com.example.desainmoviereview2

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import com.example.desainmoviereview2.databinding.ActivityMainBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth

/**
 * The main activity of the application.
 * This activity hosts the NavHostFragment and handles the bottom navigation.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        // Set up the NavHostFragment and NavController
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_nav)

        // Listen for destination changes to update the selected item in the bottom navigation
        navController.addOnDestinationChangedListener { _, destination, _ ->
            // Update the selected item in the bottom navigation
            val menuItem = bottomNav.menu.findItem(destination.id)
            if (menuItem != null) {
                menuItem.isChecked = true
            }
        }

        // Set up the bottom navigation listener
        bottomNav.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.homeFragment -> {
                    navController.navigate(R.id.homeFragment)
                    true
                }
                R.id.movieListFragment -> {
                    navController.navigate(R.id.movieListFragment)
                    true
                }
                R.id.profileFragment -> {
                    // If the user is logged in, navigate to the profile fragment.
                    // Otherwise, navigate to the login fragment.
                    if (auth.currentUser != null) {
                        navController.navigate(R.id.profileFragment)
                    } else {
                        navController.navigate(R.id.loginFragment)
                    }
                    true
                }
                else -> false
            }
        }
    }
}
