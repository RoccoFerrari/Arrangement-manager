package com.example.arrangement_manager.menu_order

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.arrangement_manager.kitchen.DishItem
import com.example.arrangement_manager.kitchen.Order
import com.example.arrangement_manager.retrofit.MenuItem
import com.example.arrangement_manager.retrofit.MenuItemUpdate
import com.example.arrangement_manager.retrofit.NewOrderEntry
import com.example.arrangement_manager.retrofit.RetrofitClient
import com.example.arrangement_manager.retrofit.RetrofitClient.apiService
import com.example.arrangement_manager.retrofit.Table
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.net.Socket
import java.util.UUID

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

    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val orderAdapter = moshi.adapter(Order::class.java)
    private val apiService = RetrofitClient.apiService


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

    fun sendOrder(table: Table) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            val newOrderEntries = _orderedItems.value.map { (menuItem, quantity) ->
                NewOrderEntry(
                    tableName = table.name,
                    menuItemName = menuItem.name,
                    quantity = quantity
                )
            }.filter { it.quantity > 0 }

            if (newOrderEntries.isNotEmpty()) {
                try {
                    val response = apiService.insertOrderEntries(userId, newOrderEntries)
                    if (response.isSuccessful) {
                        Log.d("MenuOrderViewModel", "Ordine inviato con successo. Procedo con l'invio alla cucina.")
                        try {
                            val dishesToSend = _orderedItems.value.map { (menuItem, quantity) ->
                                DishItem(
                                    dishName = menuItem.name,
                                    price = menuItem.price,
                                    quantity = quantity
                                )
                            }.filter { it.quantity > 0 }

                            val tableId = "${table.idUser}::${table.name}"
                            val orderId = UUID.randomUUID().toString()

                            val orderToSend = Order(
                                orderId = orderId,
                                tableId = tableId,
                                dishes = dishesToSend
                            )
                            val jsonString = orderAdapter.toJson(orderToSend)
                            val kitchenIpAddress = "10.0.2.2"
                            val kitchenPort = 6000

                            Socket(kitchenIpAddress, kitchenPort).use { socket ->
                                val writer = PrintWriter(OutputStreamWriter(socket.getOutputStream()), true)
                                writer.println(jsonString)
                                writer.close()
                            }

                            Log.d("MenuOrderViewModel", "Ordine inviato con successo anche alla cucina.")
                            updateMenuItemQuantities()
                            _orderConfirmed.value = true
                        } catch (e: Exception) {
                            // Errore specifico per l'invio Wi-Fi. L'ordine è comunque salvato nel DB.
                            _errorMessage.value = "Ordine salvato nel database, ma errore nell'invio alla cucina: ${e.message}"
                        }
                        updateMenuItemQuantities()
                    } else {
                        Log.e("MenuOrderViewModel", "Errore nell'invio dell'ordine al database: ${response.code()} - ${response.errorBody()?.string()}")
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