package com.example.desainmoviereview2.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.desainmoviereview2.MyAppTheme

@Composable
fun LoginScreen(
    uiState: LoginUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onLoginClick: () -> Unit,
    onGoToRegisterClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OutlinedTextField(
                value = uiState.email,
                onValueChange = onEmailChange,
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = uiState.password,
                onValueChange = onPasswordChange,
                label = { Text("Password") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onLoginClick,
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading
            ) {
                Text("Login")
            }

            TextButton(
                onClick = onGoToRegisterClick,
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading
            ) {
                Text("Don't have an account? Register")
            }
        }

        if (uiState.isLoading) {
            CircularProgressIndicator()
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    MyAppTheme {
        LoginScreen(
            uiState = LoginUiState(isLoading = false),
            onEmailChange = {},
            onPasswordChange = {},
            onLoginClick = {},
            onGoToRegisterClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun LoginScreenLoadingPreview() {
    MyAppTheme {
        LoginScreen(
            uiState = LoginUiState(isLoading = true),
            onEmailChange = {},
            onPasswordChange = {},
            onLoginClick = {},
            onGoToRegisterClick = {}
        )
    }
}
