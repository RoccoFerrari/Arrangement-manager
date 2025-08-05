package com.example.arrangement_manager.menu_order

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.arrangement_manager.retrofit.MenuItem
import com.example.arrangement_manager.retrofit.MenuItemUpdate
import com.example.arrangement_manager.retrofit.NewOrderEntry
import com.example.arrangement_manager.retrofit.RetrofitClient.apiService
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

    private val _isConfirmButtonEnabled = MutableStateFlow(false)
    val isConfirmButtonEnabled: StateFlow<Boolean> = _isConfirmButtonEnabled.asStateFlow()

    private val _orderConfirmed = MutableStateFlow(false)
    val orderConfirmed: StateFlow<Boolean> = _orderConfirmed.asStateFlow()

    private val _totalPrice = MutableStateFlow(0.0)
    val totalPrice: StateFlow<Double> = _totalPrice.asStateFlow()

    // Chiamata quando il ViewModel viene creato
    init {
        loadMenuItems()
        viewModelScope.launch {
            orderedItems.collectLatest { quantities ->
                calculateTotalPrice(quantities)
            }
        }
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
        _isConfirmButtonEnabled.value = currentOrders.isNotEmpty()
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
                        Log.d("MenuOrderViewModel", "Ordine inviato con successo. Procedo con l'aggiornamento.")
                        updateMenuItemQuantities()
                    } else {
                        Log.e("MenuOrderViewModel", "Errore nell'invio dell'ordine: ${response.code()} - ${response.errorBody()?.string()}")
                        _errorMessage.value = "Errore nell'invio dell'ordine: ${response.code()}"
                    }
                } catch (e: IOException) {
                    _errorMessage.value = "Errore di connessione. Controlla la tua rete."
                } catch (e: HttpException) {
                    _errorMessage.value = "Errore HTTP: ${e.code()}"
                } finally {
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

            val updates = _orderedItems.value.map { (menuItem, quantity) ->
                Log.d("MenuOrderViewModel", "Order received for ${menuItem.name}: ordered=${quantity}, available=${menuItem.quantity}")
                val updatedItem = MenuItemUpdate(
                    price = menuItem.price,
                    quantity = menuItem.quantity - quantity,
                    description = menuItem.description
                )
                Log.d("MenuOrderViewModel", "Updating item: ${menuItem.name}, with new quantity: ${updatedItem.quantity}")
                menuItem.name to updatedItem
            }

            try {
                for ((menuItemName, menuItemUpdate) in updates) {
                    Log.d("MenuOrderViewModel", "Attempting to update ${menuItemName} to quantity: ${menuItemUpdate.quantity}")
                    val response = apiService.updateMenuItem(userId, menuItemName, menuItemUpdate)
                    if (!response.isSuccessful) {
                        val errorBody = response.errorBody()?.string()
                        Log.e("MenuOrderViewModel", "Update failed for $menuItemName: ${response.code()} - Error: $errorBody")
                        _errorMessage.value = "Errore nell'aggiornamento dell'articolo $menuItemName: ${response.code()}"
                        _isLoading.value = false
                        _orderConfirmed.value = false
                        return@launch
                    } else {
                        Log.d("MenuOrderViewModel", "Update successful for $menuItemName.")
                    }
                }

                loadMenuItems()
                _orderConfirmed.value = true

            } catch (e: Exception) {
                _errorMessage.value = "Errore durante l'aggiornamento del menu: ${e.message}"
                _isLoading.value = false
                _orderConfirmed.value = false
            } finally {
                _orderedItems.value = emptyMap()
                _isLoading.value = false
            }
        }
    }

    private fun calculateTotalPrice(quantities: Map<MenuItem, Int>) {
        var total = 0.0
        quantities.forEach { (menuItem, quantity) ->
            total += menuItem.price * quantity
        }
        _totalPrice.value = total
    }
}