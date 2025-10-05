package com.example.desainmoviereview2

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth

class SplashFragment : Fragment() {

    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_splash, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        auth = FirebaseAuth.getInstance()

        Handler(Looper.getMainLooper()).postDelayed({
            // Check if the fragment is still added and the view is available
            if (isAdded && view != null) {
                checkUserStatus()
            }
        }, 2000) // 2 second delay
    }

    private fun checkUserStatus() {
        val navController = findNavController()
        // Make sure the current destination is the SplashFragment before navigating
        if (navController.currentDestination?.id == R.id.splashFragment) {
            if (auth.currentUser != null) {
                // If user is logged in, go to the main screen
                navController.navigate(R.id.action_splashFragment_to_homeFragment)
            } else {
                // If user is not logged in, go to the login screen
                navController.navigate(R.id.action_splashFragment_to_loginFragment)
            }
        }
    }
}
