package com.example.arrangement_manager

// singleton che gestisce un utente loggato
// permette di semplificare l'ottenimento di tavoli e menu in base all'utente loggato
data class UserSession(
    val email: String? = null,
    val isLoggedIn: Boolean = false,
    val isLoading: Boolean = false, // Per indicare operazioni in corso (es. login)
    val errorMessage: String? = null // Per errori di login
)