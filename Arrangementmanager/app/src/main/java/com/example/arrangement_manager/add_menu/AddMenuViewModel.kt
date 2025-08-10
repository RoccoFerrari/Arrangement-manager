package com.example.arrangement_manager.add_menu

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.arrangement_manager.retrofit.MenuItem
import com.example.arrangement_manager.retrofit.RetrofitClient.apiService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

data class AddMenuUiState(
    val isLoading: Boolean = false,
    val successMessage: String? = null,
    val errorMessage: String? = null
)

class AddMenuViewModel(private val userId: String) : ViewModel() {
    private val _uiState = MutableStateFlow(AddMenuUiState())
    val uiState: StateFlow<AddMenuUiState> = _uiState.asStateFlow()

    fun insertMenuItem(menuItem: MenuItem) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val newMenuItemRequest = menuItem.copy(idUser = userId)
                val response = apiService.insertMenuItem(userId, newMenuItemRequest)

                if (response.isSuccessful) {
                    _uiState.value = _uiState.value.copy(
                        successMessage = "Article ${menuItem.name} added successfully!",
                        isLoading = false
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "Error adding article: ${response.code()}",
                        isLoading = false
                    )
                }
            } catch (e: IOException) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Connection error. Check your network..",
                    isLoading = false
                )
            } catch (e: HttpException) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "HTTP error: ${e.code()}",
                    isLoading = false
                )
            }
        }
    }
}

class AddMenuViewModelFactory(private val userId: String) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AddMenuViewModel::class.java)) {
            return AddMenuViewModel(userId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}