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

/**
 * UI state for the AddMenuViewModel.
 * This includes loading state, success message, and error message.
 * @property isLoading Indicates whether the UI is currently loading data.
 * @property successMessage A message indicating a successful operation.
 * @property errorMessage A message indicating an error.
 */
data class AddMenuUiState(
    val isLoading: Boolean = false,
    val successMessage: String? = null,
    val errorMessage: String? = null
)

/**
 * [ViewModel] for the `AddMenuDialogFragment`.
 *
 * This ViewModel handles the business logic for adding a new menu item. It manages
 * the UI state, performs the network call to insert the item, and updates the state
 * with success or error messages.
 *
 * @property userId The ID of the user for whom the menu item is being added.
 */
class AddMenuViewModel(private val userId: String) : ViewModel() {
    private val _uiState = MutableStateFlow(AddMenuUiState())
    val uiState: StateFlow<AddMenuUiState> = _uiState.asStateFlow()

    /**
     * Inserts a new menu item by making an API call.
     *
     * This method updates the UI state to show a loading indicator, and then attempts to
     * insert the provided [MenuItem]. It handles network and HTTP exceptions, updating the
     * UI state with appropriate success or error messages.
     *
     * @param menuItem The [MenuItem] object to be inserted.
     */
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