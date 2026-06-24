package com.dirtfy.srr.ui.performer.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    onLoginSuccess: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Auto-login: Firebase Auth caches session locally — this is synchronous
    LaunchedEffect(Unit) {
        if (viewModel.isAlreadySignedIn()) onLoginSuccess()
    }

    LaunchedEffect(uiState.isLoginSuccess) {
        if (uiState.isLoginSuccess) {
            onLoginSuccess()
            viewModel.resetLoginStatus()
        }
    }

    LoginContent(
        uiState           = uiState,
        onEmailChange     = viewModel::onEmailChange,
        onPasswordChange  = viewModel::onPasswordChange,
        onLoginClick      = viewModel::login,
        onSignUpClick     = viewModel::signUp
    )
}

@Composable
fun LoginContent(
    uiState: LoginUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onLoginClick: () -> Unit,
    onSignUpClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "SRR",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value       = uiState.email,
                onValueChange = onEmailChange,
                label       = { Text("Email") },
                singleLine  = true,
                modifier    = Modifier.fillMaxWidth(),
                isError     = uiState.error != null,
                enabled     = !uiState.isLoading
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value       = uiState.password,
                onValueChange = onPasswordChange,
                label       = { Text("Password") },
                singleLine  = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier    = Modifier.fillMaxWidth(),
                isError     = uiState.error != null,
                enabled     = !uiState.isLoading
            )

            uiState.error?.let { message ->
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .align(Alignment.Start)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(40.dp))
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick  = onLoginClick,
                        modifier = Modifier.weight(1f)
                    ) { Text("Login") }

                    OutlinedButton(
                        onClick  = onSignUpClick,
                        modifier = Modifier.weight(1f)
                    ) { Text("Sign Up") }
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "Normal State")
@Composable
fun LoginScreenPreview() {
    MaterialTheme {
        LoginContent(
            uiState          = LoginUiState(email = "admin@example.com"),
            onEmailChange    = {},
            onPasswordChange = {},
            onLoginClick     = {},
            onSignUpClick    = {}
        )
    }
}

@Preview(showBackground = true, name = "Loading State")
@Composable
fun LoginLoadingPreview() {
    MaterialTheme {
        LoginContent(
            uiState          = LoginUiState(isLoading = true),
            onEmailChange    = {},
            onPasswordChange = {},
            onLoginClick     = {},
            onSignUpClick    = {}
        )
    }
}

@Preview(showBackground = true, name = "Error State")
@Composable
fun LoginErrorPreview() {
    MaterialTheme {
        LoginContent(
            uiState          = LoginUiState(error = "Invalid credentials"),
            onEmailChange    = {},
            onPasswordChange = {},
            onLoginClick     = {},
            onSignUpClick    = {}
        )
    }
}
