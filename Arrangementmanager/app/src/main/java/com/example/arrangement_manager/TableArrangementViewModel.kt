package com.example.arrangement_manager

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.util.copy
import com.example.arrangement_manager.RetrofitClient.apiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

// Classe che gestisce lo stato dell'UI
data class TableUiState(
    val isLoading: Boolean = false,
    val tables: List<Table> = emptyList(),
    val errorMessage: String? = null
)

class TableArrangementViewModel(
    private val userEmail: String
) : ViewModel()  {

    private val _uiState = MutableStateFlow(TableUiState())
    val uiState: StateFlow<TableUiState> = _uiState.asStateFlow()

    // Coroutine per raccogliere i tavoli dal DAO
    // Eseguita quando il ViewModel viene creato
    init {
        loadTables()
    }

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
                        errorMessage = "Errore nel recupero dei tavoli: ${response.message()}"
                    )
                }
            } catch (e: IOException) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Errore di connessione. Controlla la tua rete."
                )
            } catch (e: HttpException) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Errore HTTP: ${e.code()}"
                )
            }
        }
    }



    // Metodo per aggiungere un nuovo tavolo
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
                        errorMessage = "Errore nell'aggiunta del tavolo: ${response.message()}"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Errore: ${e.message}"
                )
            }
        }
    }

    // Metodo per aggiornare un tavolo esistente
    fun updateTable(table: Table) {
        viewModelScope.launch {
            try {
                // Prepara un oggetto TableUpdate con i dati aggiornati
                val tableUpdate = TableUpdate(
                    xCoordinate = table.xCoordinate,
                    yCoordinate = table.yCoordinate,
                    width = table.width,
                    height = table.height
                )
                val response = apiService.updateTable(userEmail, table.name, tableUpdate)
                if (response.isSuccessful) {
                    // Aggiorna la lista di tavoli con i dati del server
                    loadTables()
                } else {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "Errore nell'aggiornamento del tavolo: ${response.message()}"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Errore nell'aggiornamento: ${e.message}"
                )
            }
        }
    }

    // Metodo per eliminare un tavolo
    fun deleteTable(table: Table) {
        viewModelScope.launch {
            try {
                val response = apiService.deleteTable(userEmail, table.name)
                if (response.isSuccessful) {
                    // Dopo l'eliminazione, ricarica l'intera lista
                    loadTables()
                } else {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "Errore nell'eliminazione del tavolo: ${response.message()}"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Errore nell'eliminazione: ${e.message}"
                )
            }
        }
    }
}