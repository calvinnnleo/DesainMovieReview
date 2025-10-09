package com.example.desainmoviereview2

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import com.example.desainmoviereview2.databinding.ActivityMainBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_nav)

        navController.addOnDestinationChangedListener { _, destination, _ ->
            // Update the selected item in the bottom navigation
            val menuItem = bottomNav.menu.findItem(destination.id)
            if (menuItem != null) {
                menuItem.isChecked = true
            }
        }

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