package com.example.arrangement_manager.kitchen

import android.app.Application
import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.GsonBuilder
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.net.SocketTimeoutException
import java.util.concurrent.atomic.AtomicBoolean

class KitchenViewModel(application: Application) : AndroidViewModel(application) {

    private val _kitchenOrders = MutableLiveData<List<Order>>()
    val kitchenOrders: LiveData<List<Order>> = _kitchenOrders

    // Communication via wi-fi
    private val isServerRunning = AtomicBoolean(false)
    private var serverJob: Job? = null
    private var serverSocket: ServerSocket? = null
    private val gson = GsonBuilder().create()

    // Register the service for IP lookup
    private var nsdManager: NsdManager? = null
    private var registrationListener: NsdManager.RegistrationListener? = null

    // Service IP
    private val serviceName = "KitchenService"
    private val serviceType = "_http._tcp."
    private val serverPort = 6000

    // Mapping of IPs sending orders: tableName -> ip
    private val clientMap = mutableMapOf<String, String>()

    init {
        _kitchenOrders.value = emptyList()
        startListeningForOrders()
        registerService()
    }

    private fun registerService() {
        nsdManager = getApplication<Application>().getSystemService(Context.NSD_SERVICE) as NsdManager
        val serviceInfo = NsdServiceInfo().apply {
            serviceName = this@KitchenViewModel.serviceName
            serviceType = this@KitchenViewModel.serviceType
            port = this@KitchenViewModel.serverPort
        }

        registrationListener = object : NsdManager.RegistrationListener {
            override fun onRegistrationFailed(serviceInfo: NsdServiceInfo?, errorCode: Int) {
                Log.e("NDS", "Registration failed: $errorCode")
            }
            override fun onServiceRegistered(serviceInfo: NsdServiceInfo?) {
                Log.d("NDS", "Service registered: $serviceInfo")
            }
            override fun onServiceUnregistered(serviceInfo: NsdServiceInfo?) { }
            override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo?, errorCode: Int) { }
        }
        nsdManager?.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener)
    }
    
    private fun startListeningForOrders() {
        if (isServerRunning.compareAndSet(false, true)) {
            serverJob = viewModelScope.launch(Dispatchers.IO) {
                try {
                    serverSocket = ServerSocket(serverPort)
                    Log.d("DEBUG_KITCHEN", "Server listening on port $serverPort...")
                    while (isServerRunning.get()) {
                        val clientSocket = serverSocket?.accept()

                        if (clientSocket != null) {
                            Log.d("DEBUG_KITCHEN", "Connection received from ${clientSocket.inetAddress.hostAddress}")
                            launch {
                                handleClient(clientSocket)
                            }
                        }
                    }
                } catch (e: SocketException) {
                    // Occurs when the server is stopped (viewmodel end of life)
                    Log.d("DEBUG_KITCHEN", "Server stopped: ${e.message}")
                } catch (e: Exception) {
                    Log.e("DEBUG_KITCHEN", "Server error: ${e.message}")
                } finally {
                    isServerRunning.set(false)
                    serverSocket?.close()
                    serverSocket = null
                }
            }
        }
    }
    
    private suspend fun handleClient(clientSocket: java.net.Socket) {
        withContext(Dispatchers.IO) {
            clientSocket.use { socket ->
                val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                val jsonString = reader.readLine() // lettura di una sola riga

                if(jsonString != null) {
                    Log.d("DEBUG_KITCHEN", "JSON data received: $jsonString")
                    try {
                        val order = gson.fromJson(jsonString, Order::class.java)
                        val clientIp = socket.inetAddress.hostAddress
                        Log.d("DEBUG_KITCHEN", "Order received for table ${order.tableId}, IP: $clientIp")
                        clientMap[order.tableId] = clientIp!!
                        // Passing order to UI via viewmodel
                        withContext(Dispatchers.Main) {
                            addOrder(order)
                        }
                    } catch (e: Exception) {
                        Log.e("DEBUG_KITCHEN", "Error converting JSON: ${e.message}")
                    }
                } else {
                    Log.d("DEBUG_KITCHEN", "No orders received")
                }
            }
        }
    }

    // Shut down the server when it is no longer needed
    override fun onCleared() {
        super.onCleared()
        registrationListener?.let { nsdManager?.unregisterService(it) }
        stopListeningForOrders()
    }

    private fun stopListeningForOrders() {
        Log.d("DEBUG_KITCHEN", "Attempting to shut down the server")
        isServerRunning.set(false)
        serverJob?.cancel()
    }

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

    fun removeDishFromOrder(orderId: String, displayDishItem: DisplayDishItem) {
        val currentList = _kitchenOrders.value.orEmpty().toMutableList()
        val orderToUpdate = currentList.find { it.orderId == orderId }

        if (orderToUpdate != null) {
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

    fun removeOrder(orderId: String) {
        val currentList = _kitchenOrders.value.orEmpty().toMutableList()
        val orderToRemove = currentList.find { it.orderId == orderId }
        if(orderToRemove != null) {
            val tableId = orderToRemove.tableId
            currentList.remove(orderToRemove)
            _kitchenOrders.value = currentList

            Log.d("DEBUG_KITCHEN", "Attempt to send notification for table $tableId.")
            sendNotificationToClient(tableId, "The order of the table $tableId completed")
        }
    }

    private fun sendNotificationToClient(tableId: String, message: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val clientIp = clientMap[tableId]
            if (clientIp != null) {
                val timeout = 5000 // 5 seconds

                try {
                    Log.d("DEBUG_KITCHEN", "Attempting to connect to $clientIp on port 6001 for notification with timeout $timeout ms.")

                    val socket = Socket()
                    socket.connect(InetSocketAddress(clientIp, 6001), timeout)

                    PrintWriter(OutputStreamWriter(socket.getOutputStream()), true).use { writer ->
                        Log.d("DEBUG_KITCHEN", "Socket connected, sending message: $message")
                        writer.println(message)
                    }
                    socket.close()
                    clientMap.remove(tableId)
                    Log.d("DEBUG_KITCHEN", "Notification successfully sent to $clientIp.")
                } catch (e: SocketTimeoutException) {
                    Log.d("DEBUG_KITCHEN", "Timeout occurred while connecting to $clientIp.")
                } catch (e: Exception) {
                    Log.e("DEBUG_KITCHEN", "Error sending notification to $clientIp: ${e.message}")
                }
            } else {
                Log.e("DEBUG_KITCHEN", "No IP found for the table $tableId.")
            }
        }
    }
}