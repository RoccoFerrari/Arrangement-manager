package com.example.arrangement_manager.kitchen

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class KitchenViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(KitchenViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return KitchenViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}