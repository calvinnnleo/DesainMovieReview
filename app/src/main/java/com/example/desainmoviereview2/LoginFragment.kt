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

class LoginFragment : Fragment() {

    private val viewModel: LoginViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                val loginState = viewModel.loginState.collectAsState()

                LoginScreen(
                    onLoginClick = { email, password ->
                        viewModel.loginUser(email, password)
                    },
                    onGoToRegisterClick = {
                        findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
                    }
                )

                when (val state = loginState.value) {
                    is LoginState.Success -> {
                        findNavController().navigate(R.id.action_loginFragment_to_homeFragment)
                    }
                    is LoginState.Error -> {
                        Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                    }
                    else -> {}
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.loginState.onEach {
            if (it is LoginState.Success) {
                findNavController().navigate(R.id.action_loginFragment_to_homeFragment)
            }
        }.launchIn(viewLifecycleOwner.lifecycleScope)
    }
}