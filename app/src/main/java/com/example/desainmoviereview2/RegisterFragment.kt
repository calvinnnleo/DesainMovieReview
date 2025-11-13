package com.example.desainmoviereview2

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class RegisterFragment : Fragment() {

    private val viewModel: RegisterViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                val registerState = viewModel.registerState.collectAsState()

                RegisterScreen(
                    onRegisterClick = { fullName, username, email, password ->
                        viewModel.registerUser(fullName, username, email, password)
                    },
                    onGoToLoginClick = {
                        findNavController().navigate(R.id.action_registerFragment_to_loginFragment)
                    }
                )

                when (val state = registerState.value) {
                    is RegisterState.Success -> {
                        Toast.makeText(context, "Registration successful! ✓", Toast.LENGTH_SHORT).show()
                        findNavController().navigate(R.id.action_registerFragment_to_homeFragment)
                    }
                    is RegisterState.Error -> {
                        Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                    }
                    else -> {}
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.registerState.onEach {
            if (it is RegisterState.Success) {
                findNavController().navigate(R.id.action_registerFragment_to_homeFragment)
            }
        }.launchIn(viewLifecycleOwner.lifecycleScope)
    }
}