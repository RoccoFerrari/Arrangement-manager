package com.example.arrangement_manager

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MenuOrderViewModel (
    private val arrangementDao: ArrangementDAO,
    private val userId: String
) : ViewModel() {
    private val _menuItems = MutableStateFlow<List<MenuItem>>(emptyList())
    val menuItems: StateFlow<List<MenuItem>> = _menuItems.asStateFlow()

    private val _orderedItems = MutableStateFlow<Map<MenuItem, Int>>(emptyMap())
    val orderedItems: StateFlow<Map<MenuItem, Int>> = _orderedItems.asStateFlow()

    // Chiamata quando il ViewModel viene creato
    init {
        viewModelScope.launch {
            arrangementDao.getAllMenuByUser(userId = userId).collectLatest { items ->
                _menuItems.value = items
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
            val orderEntriesToSave = _orderedItems.value.map { (menuItem, quantity) ->
                OrderEntry(
                    tableName = tableName,
                    menuItemName = menuItem.name,
                    id_user = userId,
                    quantity = quantity
                )
            }
            if (orderEntriesToSave.isNotEmpty()) {
                arrangementDao.insertOrderEntries(orderEntriesToSave)
            }

            _orderedItems.value.forEach { (menuItem, quantity) ->
                val newQuantity = menuItem.quantity - quantity
                val updatedMenuItem = menuItem.copy(quantity = newQuantity)
                arrangementDao.updateMenuItem(updatedMenuItem)
            }
        }
    }
}