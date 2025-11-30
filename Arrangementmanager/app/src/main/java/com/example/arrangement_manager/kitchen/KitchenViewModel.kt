package com.example.arrangement_manager.kitchen

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.gson.GsonBuilder
import com.example.arrangement_manager.socket_handler.SocketHandler
import io.socket.client.Socket
import org.json.JSONObject

/**
 * ViewModel for the Kitchen screen.
 *
 * This ViewModel is responsible for managing the state of kitchen orders. It acts as a server,
 * listening for incoming orders from client devices over a local Wi-Fi network using TCP sockets.
 * It also uses Network Service Discovery (NSD) to broadcast its presence to clients.
 *
 * It provides a [LiveData] of orders to the UI and methods to manage these orders, such as
 * adding, removing dishes, and marking an entire order as completed. When an order is completed,
 * it sends a notification back to the original client device.
 *
 * @param application The application context.
 */
class KitchenViewModel(application: Application, private val userId: String) : AndroidViewModel(application) {

    // LiveData to hold the list of incoming orders. The UI observes this data
    private val _kitchenOrders = MutableLiveData<List<Order>>()
    val kitchenOrders: LiveData<List<Order>> = _kitchenOrders

    // --- Server Communication via Wi-Fi ---
    // AtomicBoolean to track if the server is running, ensuring thread-safe state management
    private val gson = GsonBuilder().create()
    private var mSocket: Socket? = null

    init {
        _kitchenOrders.value = emptyList()

        mSocket = SocketHandler.getSocket()

        setupSocketListener()
    }

    private fun setupSocketListener() {
        // Listen the event from the Server
        mSocket?.on("receive_order") { args ->
            if(args.isNotEmpty()) {
                try {
                    val data = args[0] as JSONObject
                    Log.d("DEBUG_KITCHEN", "Received data: $data")

                    // Convert JSON in Kotlin Obj
                    val order = gson.fromJson(data.toString(), Order::class.java)
                    addOrder(order)
                } catch (e: Exception) {
                    Log.e("DEBUG_KITCHEN", "Error converting JSON: ${e.message}")
                }
            }
        }
    }

    /**
     * Adds a new order to the list of kitchen orders.
     *
     * If an order for the same table already exists, it merges the new dishes with the existing
     * order. Otherwise, it adds the new order to the list. The list is then sorted by table number.
     * @param order The new [Order] to be added or merged.
     */
    private fun addOrder(order: Order) {
        val currentList = _kitchenOrders.value.orEmpty().toMutableList()
        // Check if an order for the same table is already present
        val existingOrderIndex = currentList.indexOfFirst { it.tableId == order.tableId }

        if (existingOrderIndex != -1) {
            // If it exists, update the existing order by adding the new dishes
            val existingOrder = currentList[existingOrderIndex]
            val updatedDishes = existingOrder.dishes.toMutableList().apply {
                addAll(order.dishes)
            }
            currentList[existingOrderIndex] = existingOrder.copy(dishes = updatedDishes)
        } else {
            // Otherwise, add the new order to the list
            currentList.add(order)
        }
        // Sort by table number
        _kitchenOrders.value = currentList.sortedBy { it.tableId }
    }

    /**
     * Removes a single dish from an order.
     *
     * This function decrements the quantity of a specific dish within an order. If the quantity
     * becomes zero, the dish is removed from the order. If the order becomes empty, it is removed
     * from the list of orders.
     * @param orderId The unique ID of the order.
     * @param displayDishItem The dish to be removed or have its quantity reduced.
     */
    fun removeDishFromOrder(orderId: String, displayDishItem: DisplayDishItem) {
        val currentList = _kitchenOrders.value.orEmpty().toMutableList()
        val orderToUpdate = currentList.find { it.orderId == orderId }

        if (orderToUpdate != null) {
            // PK formed by tableId::dishName, so we split where '::' and keep the last part
            val tableNumber = orderToUpdate.tableId.split("::Table ").last()
            // Notification for ready dish
            val message = "Dish '${displayDishItem.dishItem.dishName}' from table $tableNumber is ready!"
            sendNotificationToServer(orderToUpdate.tableId, message, isOrderComplete = false)

            val updatedDishes = orderToUpdate.dishes.toMutableList()
            // Find the dish to update by name
            val dishToRemove = updatedDishes.find { it.dishName == displayDishItem.dishItem.dishName }

            if (dishToRemove != null) {
                // Decrease the quantity
                if (dishToRemove.quantity > 1) {
                    val index = updatedDishes.indexOf(dishToRemove)
                    updatedDishes[index] = dishToRemove.copy(quantity = dishToRemove.quantity - 1)
                } else {
                    // If quantity is 1, remove the dish from the list
                    updatedDishes.remove(dishToRemove)
                }
            }

            if (updatedDishes.isEmpty()) {
                currentList.remove(orderToUpdate)
            } else {
                val updatedOrder = orderToUpdate.copy(dishes = updatedDishes)
                val index = currentList.indexOf(orderToUpdate)
                currentList[index] = updatedOrder
            }
        }
        _kitchenOrders.value = currentList
    }

    /**
     * Removes an entire order and sends a completion notification to the client.
     *
     * This function removes an order from the list and then calls [sendNotificationToServer]
     * to inform the client that their order is ready.
     * @param orderId The unique ID of the order to be removed.
     */
    fun removeOrder(orderId: String) {
        val currentList = _kitchenOrders.value.orEmpty().toMutableList()
        val orderToRemove = currentList.find { it.orderId == orderId }
        if(orderToRemove != null) {
            val tableId = orderToRemove.tableId
            val tableNumber = tableId.split("::Table ").last()
            currentList.remove(orderToRemove)
            _kitchenOrders.value = currentList

            Log.d("DEBUG_KITCHEN", "Attempt to send notification for table $tableId.")
            sendNotificationToServer(tableId, "Order of the table $tableNumber completed", isOrderComplete = true)
        }
    }

    /**
     * Sends a notification message to the client device.
     *
     * This method uses the stored IP address for a given table to connect to the client on a
     * different port (6001) and send a message. The IP is removed from the map after the message
     * is sent.
     * @param tableId The ID of the table to send the notification to.
     * @param message The message content to be sent.
     */
    private fun sendNotificationToServer(tableId: String, message: String, isOrderComplete: Boolean) {
        val payload = JSONObject()
        try {
            payload.put("userId", userId)
            payload.put("tableId", tableId)
            payload.put("message", message)
            payload.put("type", if(isOrderComplete) "ORDER_COMPLETE" else "DISH_READY")
            mSocket?.emit("kitchen_status_update", payload)
            Log.d("DEBUG_KITCHEN", "Notification sent to $tableId")
        } catch (e: Exception) {
            Log.e("DEBUG_KITCHEN", "Error sending notification: ${e.message}")
        }
    }

    override fun onCleared() {
        super.onCleared()
        mSocket?.off("receive_order")
    }
}