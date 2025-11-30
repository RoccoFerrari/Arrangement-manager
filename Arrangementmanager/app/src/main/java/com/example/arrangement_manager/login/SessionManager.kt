package com.example.arrangement_manager.login

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("user_session_prefs", Context.MODE_PRIVATE)

    companion object {
        const val KEY_EMAIL = "user_email"
        const val KEY_IS_LOGGED_IN = "is_logged_in"
    }

    // Salva i dati del login
    fun saveUserSession(email: String) {
        val editor = prefs.edit()
        editor.putString(KEY_EMAIL, email)
        editor.putBoolean(KEY_IS_LOGGED_IN, true)
        editor.apply()
    }

    // Recupera l'email salvata (il nostro ID)
    fun getUserEmail(): String? {
        return prefs.getString(KEY_EMAIL, null)
    }

    // Controlla se l'utente è loggato
    fun isLoggedIn(): Boolean {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    // Logout
    fun clearSession() {
        prefs.edit().clear().apply()
    }
}