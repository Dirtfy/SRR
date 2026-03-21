package com.dirtfy.srr.viewmodel.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


class LoginViewModel : ViewModel() {

    // Internal mutable state
    private val _uiState = MutableStateFlow(LoginUiState())
    // Exposed read-only state for the UI
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onUsernameChange(newValue: String) {
        _uiState.update { it.copy(username = newValue, errorMessage = null) }
    }

    fun onPasswordChange(newValue: String) {
        _uiState.update { it.copy(password = newValue, errorMessage = null) }
    }

    fun login() {
        val currentState = _uiState.value

        // Basic validation
        if (currentState.username.isBlank() || currentState.password.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Username and password cannot be empty") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            try {
                // Simulate network delay
                delay(1500)

                // Mock login logic
                if (currentState.username == "admin" && currentState.password == "1234") {
                    _uiState.update { it.copy(isLoading = false, isLoginSuccess = true) }
                } else {
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = "Invalid credentials")
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = "An error occurred: ${e.message}")
                }
            }
        }
    }

    fun resetLoginStatus() {
        _uiState.update { it.copy(isLoginSuccess = false) }
    }
}