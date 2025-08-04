package com.example.arrangement_manager

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.arrangement_manager.RetrofitClient.apiService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

class MenuOrderViewModel (
    private val userId: String
) : ViewModel() {
    private val _menuItems = MutableStateFlow<List<MenuItem>>(emptyList())
    val menuItems: StateFlow<List<MenuItem>> = _menuItems.asStateFlow()

    private val _orderedItems = MutableStateFlow<Map<MenuItem, Int>>(emptyMap())
    val orderedItems: StateFlow<Map<MenuItem, Int>> = _orderedItems.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Chiamata quando il ViewModel viene creato
    init {
        loadMenuItems()
    }

    private fun loadMenuItems() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val response = apiService.getAllMenuByUser(userId)
                if (response.isSuccessful) {
                    _menuItems.value = response.body() ?: emptyList()
                } else {
                    _errorMessage.value = "Errore nel recupero del menu: ${response.code()}"
                }
            } catch (e: IOException) {
                _errorMessage.value = "Errore di connessione. Controlla la tua rete."
            } catch (e: HttpException) {
                _errorMessage.value = "Errore HTTP: ${e.code()}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun onQuantityChanged(menuItem: MenuItem, newQuantity: Int) {
        val currentOrders = _orderedItems.value.toMutableMap()
        if (newQuantity > 0) {
            currentOrders[menuItem] = newQuantity
        } else {
            currentOrders.remove(menuItem)
        }
        _orderedItems.value = currentOrders
    }

    fun sendOrder(tableName: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            val newOrderEntries = _orderedItems.value.map { (menuItem, quantity) ->
                NewOrderEntry(
                    tableName = tableName,
                    menuItemName = menuItem.name,
                    quantity = quantity
                )
            }
            if (newOrderEntries.isNotEmpty()) {
                try {
                    val response = apiService.insertOrderEntries(userId, newOrderEntries)
                    if (response.isSuccessful) {
                        updateMenuItemQuantities()
                    } else {
                        // Gestione errori
                        _errorMessage.value = "Errore nell'invio dell'ordine: ${response.code()}"
                        _isLoading.value = false
                    }
                } catch (e: IOException) {
                    _errorMessage.value = "Errore di connessione. Controlla la tua rete."
                    _isLoading.value = false
                } catch (e: HttpException) {
                    _errorMessage.value = "Errore HTTP: ${e.code()}"
                    _isLoading.value = false
                }
            } else {
                _isLoading.value = false
            }
        }
    }

    private fun updateMenuItemQuantities() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _orderedItems.value.forEach { (menuItem, quantity) ->
                val newQuantity = menuItem.quantity - quantity
                val updatedMenuItem = menuItem.copy(quantity = newQuantity)

                try {
                    val menuItemUpdate = MenuItemUpdate(
                        price = updatedMenuItem.price,
                        quantity = updatedMenuItem.quantity,
                        description = updatedMenuItem.description
                    )
                    val response = apiService.updateMenuItem(userId, updatedMenuItem.name, menuItemUpdate)
                    if (!response.isSuccessful) {
                        _errorMessage.value = "Errore nell'aggiornamento del menu: ${response.code()}"
                    }
                } catch (e: Exception) {
                    _errorMessage.value = "Errore nell'aggiornamento del menu: ${e.message}"
                }
            }
            // Ricarica il menu per sincronizzare la UI con il server dopo l'aggiornamento
            loadMenuItems()
            _isLoading.value = false
        }
    }
}