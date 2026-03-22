package com.dirtfy.srr.ui.performer.login

/**
 * Data class representing the UI state for the Login Screen.
 */
data class LoginUiState(
    val username: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isLoginSuccess: Boolean = false
)
