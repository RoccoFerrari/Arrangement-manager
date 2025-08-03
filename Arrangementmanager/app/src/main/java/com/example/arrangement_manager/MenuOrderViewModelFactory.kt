package com.example.arrangement_manager

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class MenuOrderViewModelFactory(
    private val arrangementDao: ArrangementDAO,
    private val userId: String
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MenuOrderViewModel::class.java)) {
            return MenuOrderViewModel(arrangementDao, userId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}