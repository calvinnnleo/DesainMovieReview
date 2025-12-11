package com.example.desainmoviereview2.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.desainmoviereview2.R
import com.google.android.material.composethemeadapter3.Mdc3Theme

class ProfileFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                Mdc3Theme {
                    ProfileScreen(
                        onLogout = {
                            findNavController().navigate(R.id.loginFragment)
                        }
                    )
                }
            }
        }
    }
}