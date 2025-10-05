package com.example.desainmoviereview2

import android.app.Application
import com.google.firebase.database.FirebaseDatabase

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Enable Firebase offline persistence for the correct database URL
        FirebaseDatabase.getInstance("https://movie-recommendation-b7ce0-default-rtdb.asia-southeast1.firebasedatabase.app").setPersistenceEnabled(true)
    }
}
