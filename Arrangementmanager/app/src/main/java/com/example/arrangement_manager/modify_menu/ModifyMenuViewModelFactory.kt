package com.example.arrangement_manager.modify_menu

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class ModifyDishViewModelFactory(private val userId: String) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ModifyDishViewModel::class.java)) {
            return ModifyDishViewModel(userId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}