package com.example.arrangement_manager.table_arrangement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.arrangement_manager.retrofit.RetrofitClient.apiService
import com.example.arrangement_manager.retrofit.Table
import com.example.arrangement_manager.retrofit.TableUpdate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

// Class that manages UI state
data class TableUiState(
    val isLoading: Boolean = false,
    val tables: List<Table> = emptyList(),
    val errorMessage: String? = null
)

/**
 * ViewModel for managing the table arrangement screen.
 *
 * It handles the business logic for fetching, adding, updating, and deleting tables
 * associated with a specific user, and exposes the UI state to be observed by the UI layer.
 *
 * @param userEmail The email of the user whose tables are being managed.
 */
class TableArrangementViewModel(
    private val userEmail: String
) : ViewModel()  {

    private val _uiState = MutableStateFlow(TableUiState())
    val uiState: StateFlow<TableUiState> = _uiState.asStateFlow()

    // Executed when the ViewModel is created
    init {
        loadTables()
    }

    /**
     * Loads the list of tables for the current user.
     *
     * This function initiates a network request to fetch all tables associated with the user's email.
     * It updates the UI state with the fetched data, or with an error message in case of failure.
     */
    private fun loadTables() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val response = apiService.getAllTablesByUser(userEmail)
                if (response.isSuccessful) {
                    val tablesList = response.body() ?: emptyList()
                    _uiState.value = _uiState.value.copy(tables = tablesList, isLoading = false)
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Error retrieving tables: ${response.message()}"
                    )
                }
            } catch (e: IOException) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Connection error. Check your network.."
                )
            } catch (e: HttpException) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "HTTP error: ${e.code()}"
                )
            }
        }
    }

    /**
     * Adds a new table for the current user.
     *
     * The function automatically generates a unique name for the new table
     * (e.g., "Table 1", "Table 2", etc.) and calculates its initial position.
     * After successfully adding the table via a network call, it reloads the entire list of tables.
     */
    fun addTable() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val currentTables = _uiState.value.tables
                var nextTableNumber = 1
                while (currentTables.any { it.name == "Table $nextTableNumber" }) {
                    nextTableNumber++
                }
                val tableName = "Table $nextTableNumber"
                val offset = (nextTableNumber - 1) * 20f
                val newTable = Table(
                    name = tableName,
                    idUser = userEmail,
                    xCoordinate = 50f + offset,
                    yCoordinate = 50f + offset,
                    width = 250f,
                    height = 100f
                )

                val response = apiService.insertTable(userEmail, newTable)
                if (response.isSuccessful) {
                    // Dopo l'inserimento ricarica l'intera lista per aggiornare la UI
                    loadTables()
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Error adding table: ${response.message()}"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Error: ${e.message}"
                )
            }
        }
    }

    /**
     * Updates an existing table.
     *
     * This method sends the updated table data to the server and, upon success,
     * reloads the entire table list to reflect the changes in the UI.
     *
     * @param table The `Table` object containing the updated data.
     */
    fun updateTable(table: Table) {
        viewModelScope.launch {
            try {
                // Prepares a TableUpdate object with the updated data
                val tableUpdate = TableUpdate(
                    xCoordinate = table.xCoordinate,
                    yCoordinate = table.yCoordinate,
                    width = table.width,
                    height = table.height
                )
                val response = apiService.updateTable(userEmail, table.name, tableUpdate)
                if (response.isSuccessful) {
                    // Update the table list with server data
                    loadTables()
                } else {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "Error updating table: ${response.message()}"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Error in update: ${e.message}"
                )
            }
        }
    }

    /**
     * Deletes a specific table from the list.
     *
     * It sends a request to the server to delete the table and, if successful,
     * reloads the table list to update the UI.
     *
     * @param table The `Table` object to be deleted.
     */
    fun deleteTable(table: Table) {
        viewModelScope.launch {
            try {
                val response = apiService.deleteTable(userEmail, table.name)
                if (response.isSuccessful) {
                    // After deleting, reload the entire list
                    loadTables()
                } else {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "Error deleting table: ${response.message()}"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Error deleting table: ${e.message}"
                )
            }
        }
    }
}