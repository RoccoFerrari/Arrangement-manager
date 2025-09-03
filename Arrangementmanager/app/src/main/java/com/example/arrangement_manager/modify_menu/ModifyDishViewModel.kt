package com.example.arrangement_manager.modify_menu

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.arrangement_manager.add_menu.AddMenuUiState
import com.example.arrangement_manager.retrofit.MenuItem
import com.example.arrangement_manager.retrofit.MenuItemUpdate
import com.example.arrangement_manager.retrofit.RetrofitClient.apiService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

/**
 * UI state for the ModifyDishViewModel.
 * This includes loading state, success message, and error message.
 * @property isLoading Indicates whether the UI is currently loading data.
 * @property successMessage A message indicating a successful operation.
 * @property errorMessage A message indicating an error.
 */
data class ModifyDishUiState(
    val isLoading: Boolean = false,
    val successMessage: String? = null,
    val errorMessage: String? = null
)

class ModifyDishViewModel (private val userId: String) : ViewModel() {
    private val _uiState = MutableStateFlow(ModifyDishUiState())
    val uiState: StateFlow<ModifyDishUiState> = _uiState.asStateFlow()


    /**
     * Deletes a menu item by making an API call.
     * This method updates the UI state to show a loading indicator, and then attempts to
     * delete the provided [MenuItem]. It handles network and HTTP exceptions, updating the
     * UI state with appropriate success or error messages.
     *
     * @param dishName The dish name object to be deleted.
     * @param userId The ID of the user for whom the menu item is being added.
     */
    fun deleteItem(userId: String, dishName: String) {
        viewModelScope.launch {
            try {
                val response = apiService.deleteMenuItem(userId, dishName)
                if (response.isSuccessful) {
                    _uiState.value = _uiState.value.copy(
                        successMessage = "Article $dishName deleted successfully!",
                        isLoading = false
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "Error deleting article: ${response.code()}",
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

    /**
     * Updates an existing menu item by making an API call.
     * This method updates the UI state to show a loading indicator, and then attempts to
     * update the provided [MenuItemUpdate]. It handles network and HTTP exceptions, updating the
     * UI state with appropriate success or error messages.
     *
     * @param menuItemUpdate The [MenuItemUpdate] object to be inserted.
     * @param userId The ID of the user for whom the menu item is being added.
     */
    fun updateItem(userId: String, currentDishName: String,menuItemUpdate: MenuItemUpdate) {
        viewModelScope.launch {
            try {
                val response = apiService.updateMenuItem(userId, currentDishName, menuItemUpdate)
                if (response.isSuccessful) {
                    _uiState.value = _uiState.value.copy(
                        successMessage = "Article ${menuItemUpdate.name} updated successfully!",
                        isLoading = false
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "Error updating article: ${response.code()}",
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