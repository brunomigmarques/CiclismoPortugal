package com.ciclismo.portugal.presentation.auth

import com.ciclismo.portugal.domain.model.User

sealed class AuthUiState {
    object Initial : AuthUiState()
    object Loading : AuthUiState()
    data class Authenticated(val user: User) : AuthUiState()
    object NotAuthenticated : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}
