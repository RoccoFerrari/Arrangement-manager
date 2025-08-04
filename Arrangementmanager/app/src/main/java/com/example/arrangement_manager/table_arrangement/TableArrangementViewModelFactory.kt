package com.example.arrangement_manager.table_arrangement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class TableArrangementViewModelFactory(
    private val userEmail: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if(modelClass.isAssignableFrom(TableArrangementViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TableArrangementViewModel(userEmail) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
