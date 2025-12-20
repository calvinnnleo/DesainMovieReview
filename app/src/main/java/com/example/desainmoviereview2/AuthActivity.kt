package com.example.desainmoviereview2

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

class AuthActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyAppTheme {
                AuthNavigation()
            }
        }
    }
}
