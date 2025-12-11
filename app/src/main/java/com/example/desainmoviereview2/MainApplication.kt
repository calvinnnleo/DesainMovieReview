package com.example.desainmoviereview2

import android.app.Application
import android.util.Log
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.database.FirebaseDatabase

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Log.d("APP_CHECK_INIT", "Initializing App Check")
        // Enable Firebase offline persistence for the correct database URL
        FirebaseDatabase.getInstance("https://movie-recommendation-b7ce0-default-rtdb.asia-southeast1.firebasedatabase.app").setPersistenceEnabled(true)

        val firebaseAppCheck = FirebaseAppCheck.getInstance()
        if (BuildConfig.DEBUG) {
            Log.d("APP_CHECK_INIT", "Using DEBUG provider")
            firebaseAppCheck.installAppCheckProviderFactory(
                DebugAppCheckProviderFactory.getInstance()
            )
        } else {
            Log.d("APP_CHECK_INIT", "Using RELEASE provider")
            firebaseAppCheck.installAppCheckProviderFactory(
                PlayIntegrityAppCheckProviderFactory.getInstance()
            )
        }
    }
}
