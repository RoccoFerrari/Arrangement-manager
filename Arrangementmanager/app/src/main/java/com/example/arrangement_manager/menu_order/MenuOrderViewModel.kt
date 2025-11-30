package com.example.arrangement_manager.menu_order

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.arrangement_manager.kitchen.DishItem
import com.example.arrangement_manager.kitchen.Order
import com.example.arrangement_manager.retrofit.MenuItem
import com.example.arrangement_manager.retrofit.MenuItemUpdate
import com.example.arrangement_manager.retrofit.NewOrderEntry
import com.example.arrangement_manager.retrofit.RetrofitClient
import kotlinx.coroutines.Dispatchers
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
import java.util.UUID
import com.example.arrangement_manager.socket_handler.SocketHandler
import org.json.JSONObject

/**
 * ViewModel for the MenuOrderDialogFragment.
 *
 * This ViewModel handles all the business logic for the menu order dialog, including fetching
 * menu items, managing the quantities of ordered items, calculating the total price,
 * and sending the final order to the backend API and the kitchen via a local network socket.
 * It also uses Network Service Discovery (NSD) to find the kitchen server's IP address.
 */
class MenuOrderViewModel (
    application: Application,
    private val userId: String
) : AndroidViewModel(application) {
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

    // Moshi and Retrofit setup for JSON serialization and API calls
    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val orderAdapter = moshi.adapter(Order::class.java)
    private val apiService = RetrofitClient.apiService

    /**
     * Called when the ViewModel is created.
     * It initiates loading the menu items and starts the network service discovery.
     */
    init {
        loadMenuItems()
        viewModelScope.launch {
            orderedItems.collectLatest { quantities ->
                calculateTotalPrice(quantities)
            }
        }
    }

    fun getMenu() {
        loadMenuItems()
    }

    /**
     * Fetches all menu items from the backend API for the current user.
     * Updates the UI state with the result or an error message.
     */
    private fun loadMenuItems() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val response = apiService.getAllMenuByUser(userId)
                if (response.isSuccessful) {
                    _menuItems.value = response.body() ?: emptyList()
                } else {
                    _errorMessage.value = "Error retrieving menu: ${response.code()}"
                }
            } catch (e: IOException) {
                _errorMessage.value = "Connection error. Check your network."
            } catch (e: HttpException) {
                _errorMessage.value = "HTTP error: ${e.code()}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Updates the quantity of a specific menu item in the order.
     * @param menuItem The menu item to update.
     * @param newQuantity The new quantity.
     */
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

    /**
     * Sends the final order to the backend API and then to the kitchen server via a socket.
     * @param table The table for which the order is being placed.
     */
    fun sendOrder(table: Table) {
        viewModelScope.launch(Dispatchers.IO) {
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
                        Log.d("DEBUG_MENU_ORDER", "Order sent to database successfully.")

                        updateMenuItemQuantities()
                        _orderConfirmed.value = true

                    } else {
                        val errorBody = response.errorBody()?.string()
                        Log.e("DEBUG_MENU_ORDER", "Error sending order: ${response.code()} - $errorBody")
                        _errorMessage.value = "Error sending order: ${response.code()}"
                    }
                } catch (e: IOException) {
                    _errorMessage.value = "Connection error. Check your network.."
                } catch (e: HttpException) {
                    _errorMessage.value = "HTTP error: ${e.code()}"
                } finally {
                    _isLoading.value = false
                }
            } else {
                _isLoading.value = false
            }
        }
    }

    /**
     * Updates the quantities of ordered menu items in the backend API.
     *
     * This is called after a successful order submission to the database.
     */
    private fun updateMenuItemQuantities() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            val updates = _orderedItems.value.map { (menuItem, quantity) ->
                Log.d("DEBUG_MENU_ORDER", "Order received for ${menuItem.name}: ordered=${quantity}, available=${menuItem.quantity}")
                val updatedItem = MenuItemUpdate(
                    price = menuItem.price,
                    quantity = menuItem.quantity - quantity,
                    description = menuItem.description
                )
                Log.d("DEBUG_MENU_ORDER", "Updating item: ${menuItem.name}, with new quantity: ${updatedItem.quantity}")
                menuItem.name to updatedItem
            }

            try {
                for ((menuItemName, menuItemUpdate) in updates) {
                    Log.d("DEBUG_MENU_ORDER", "Attempting to update ${menuItemName} to quantity: ${menuItemUpdate.quantity}")
                    val response = apiService.updateMenuItem(userId, menuItemName, menuItemUpdate)
                    if (!response.isSuccessful) {
                        val errorBody = response.errorBody()?.string()
                        Log.e("DEBUG_MENU_ORDER", "Update failed for $menuItemName: ${response.code()} - Error: $errorBody")
                        _errorMessage.value = "Error updating article $menuItemName: ${response.code()}"
                        _isLoading.value = false
                        _orderConfirmed.value = false
                        return@launch
                    } else {
                        Log.d("DEBUG_MENU_ORDER", "Update successful for $menuItemName.")
                    }
                }

                loadMenuItems()
                _orderConfirmed.value = true

            } catch (e: Exception) {
                _errorMessage.value = "Error while updating menu: ${e.message}"
                _isLoading.value = false
                _orderConfirmed.value = false
            } finally {
                _orderedItems.value = emptyMap()
                _isLoading.value = false
            }
        }
    }

    /**
     * Calculates the total price of all ordered items.
     * @param quantities A map of menu items to their ordered quantities.
     */
    private fun calculateTotalPrice(quantities: Map<MenuItem, Int>) {
        var total = 0.0
        quantities.forEach { (menuItem, quantity) ->
            total += menuItem.price * quantity
        }
        _totalPrice.value = total
    }

}