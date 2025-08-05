package com.example.arrangement_manager.kitchen

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class KitchenViewModel : ViewModel() {

    private val _kitchenOrders = MutableLiveData<List<Order>>()
    val kitchenOrders: LiveData<List<Order>> = _kitchenOrders

    init {
        // Inizializza con una lista vuota o carica gli ordini persistenti dal database
        _kitchenOrders.value = emptyList()
    }

    fun addOrder(order: Order) {
        val currentList = _kitchenOrders.value.orEmpty().toMutableList()
        // Controlla se un ordine per lo stesso tavolo è già presente
        val existingOrderIndex = currentList.indexOfFirst { it.tableId == order.tableId }

        if (existingOrderIndex != -1) {
            // Se esiste, aggiorna l'ordine esistente aggiungendo i nuovi piatti
            val existingOrder = currentList[existingOrderIndex]
            val updatedDishes = existingOrder.dishes.toMutableList().apply {
                addAll(order.dishes)
            }
            currentList[existingOrderIndex] = existingOrder.copy(dishes = updatedDishes)
        } else {
            // Altrimenti, aggiungi il nuovo ordine alla lista
            currentList.add(order)
        }
        _kitchenOrders.value = currentList.sortedBy { it.tableId } // Ordina per numero di tavolo
    }

    fun removeDishFromOrder(orderId: String, dishItem: DishItem) {
        val currentList = _kitchenOrders.value.orEmpty().toMutableList()
        val orderToUpdate = currentList.find { it.orderId == orderId }

        if (orderToUpdate != null) {
            val updatedDishes = orderToUpdate.dishes.toMutableList()
            updatedDishes.remove(dishItem)

            if (updatedDishes.isEmpty()) {
                // Se non ci sono più piatti, rimuovi l'intero ordine
                currentList.remove(orderToUpdate)
            } else {
                // Altrimenti, aggiorna l'ordine con la nuova lista di piatti
                val updatedOrder = orderToUpdate.copy(dishes = updatedDishes)
                val index = currentList.indexOf(orderToUpdate)
                currentList[index] = updatedOrder
            }
        }
        _kitchenOrders.value = currentList
    }

    fun removeOrder(orderId: String) {
        val currentList = _kitchenOrders.value.orEmpty().toMutableList()
        val orderToRemove = currentList.find { it.orderId == orderId }
        currentList.remove(orderToRemove)
        _kitchenOrders.value = currentList
    }
}