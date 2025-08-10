package com.example.arrangement_manager.login

// Singleton that manages a logged in user
data class UserSession(
    val email: String? = null,
    val isLoggedIn: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)