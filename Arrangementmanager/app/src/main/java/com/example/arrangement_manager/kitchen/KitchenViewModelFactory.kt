package com.example.arrangement_manager.kitchen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class KitchenViewModelFactory() : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(KitchenViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return KitchenViewModel() as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}