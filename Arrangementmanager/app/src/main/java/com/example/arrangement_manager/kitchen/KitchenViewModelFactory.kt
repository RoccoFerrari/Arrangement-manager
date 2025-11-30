package com.example.arrangement_manager.kitchen

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.arrangement_manager.login.SessionManager
import com.example.arrangement_manager.login.UserSession

class KitchenViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(KitchenViewModel::class.java)) {
            val sessionManager = SessionManager(application)
            val userEmail = sessionManager.getUserEmail() ?: "default_email_error"
            @Suppress("UNCHECKED_CAST")
            return KitchenViewModel(application, userEmail) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}