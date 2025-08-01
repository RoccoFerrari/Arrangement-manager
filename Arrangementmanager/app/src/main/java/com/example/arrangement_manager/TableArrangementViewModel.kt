package com.example.arrangement_manager

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TableArrangementViewModel(
    private val arrangementDao: ArrangementDAO,
    private val userEmail: String
) : ViewModel()  {

    private val _tables = MutableStateFlow<List<Table_>>(emptyList())
    val tables: StateFlow<List<Table_>> = _tables.asStateFlow()

    // Coroutine per raccogliere i tavoli dal DAO
    // Eseguita quando il ViewModel viene creato
    init {
        viewModelScope.launch(Dispatchers.IO) {
            arrangementDao.getAllTablesByUsers(userEmail).collect { tableList ->
                _tables.value = tableList
            }
        }
    }

    // Metodo per aggiungere un nuovo tavolo
    fun addTable() {
        viewModelScope.launch(Dispatchers.IO) {
            val currentTables = _tables.value
            var nextTableNumber = 1
            while(currentTables.any { it.name == "Table $nextTableNumber" }) {
                nextTableNumber++
            }
            val tableName = "Table $nextTableNumber"
            val offset = (nextTableNumber - 1) * 20f
            val newTable = Table_(
                name = tableName,
                x_coordinate = 50f + offset,
                y_coordinate = 50f + offset,
                width = 250f,
                height = 100f,
                id_user = userEmail)
            arrangementDao.insertTable(newTable)
        }
    }

    // Metodo per inserire un tavolo esistente
    fun updateTable(table: Table_) {
        // Aggiorna lo stato interno del ViewModel prima di chiamare il DB
        _tables.value = _tables.value.map { if (it.name == table.name) table else it }

        viewModelScope.launch(Dispatchers.IO) {
                arrangementDao.updateTable(table.copy(id_user = userEmail))
        }
    }

    // Metodo per eliminare un tavolo
    fun deleteTable(table: Table_) {
        viewModelScope.launch(Dispatchers.IO) {
            arrangementDao.deleteTableByNameAndUser(table.name, userEmail)
        }

    }
}