package com.example.arrangement_manager

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TableArrangementViewModel(private val arrangementDao: ArrangementDAO, private val userEmail: String) : ViewModel()  {
    private val _tables = MutableStateFlow<List<Table_>>(emptyList())
    val tables: StateFlow<List<Table_>> = _tables.asStateFlow()

    // Variabile per tenere traccia del prossimo numero di tavolo per la generazione del nome
    private var nextTableNumber = 1

    // Coroutine per raccogliere i tavoli dal DAO
    // Eseguita quando il ViewModel viene creato
    fun loadTablesForUser() {
        viewModelScope.launch(Dispatchers.IO) {
            arrangementDao.getAllTablesByUsers(userEmail).collect { tableList ->
                _tables.value = tableList
                // Aggiorna nextTableNumber basandosi sui tavoli esistenti dell'utente
                nextTableNumber = (tableList.mapNotNull { it.name.replace("Tavolo ", "").toIntOrNull() }.maxOrNull() ?: 0) + 1
            }
        }
    }

    // Metodo per aggiungere un nuovo tavolo
    fun addTable() {
        viewModelScope.launch(Dispatchers.IO) {
            val tableName = "Table ${nextTableNumber++}"
            val newTable = Table_(
                tableName,
                50f,
                50f,
                250f,
                100f,
                id_user = userEmail)
            arrangementDao.insertTable(newTable)
        }
    }

    // Metodo per aggiornare un tavolo esistente
    fun updateTable(table: Table_) {
        viewModelScope.launch(Dispatchers.IO) {
            arrangementDao.insertTable(table)
        }
    }

    // Metodo per eliminare un tavolo
    fun deleteTable(table: Table_, userId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            arrangementDao.deleteTableByNameAndUser(table.name, userId)
        }

    }
}