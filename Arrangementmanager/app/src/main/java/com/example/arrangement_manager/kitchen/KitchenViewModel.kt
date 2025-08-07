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
import java.net.ServerSocket
import java.net.SocketException
import java.util.concurrent.atomic.AtomicBoolean

class KitchenViewModel(application: Application) : AndroidViewModel(application) {

    private val _kitchenOrders = MutableLiveData<List<Order>>()
    val kitchenOrders: LiveData<List<Order>> = _kitchenOrders

    // Comunicazione via wifi
    private val isServerRunning = AtomicBoolean(false)
    private var serverJob: Job? = null
    private var serverSocket: ServerSocket? = null
    private val gson = GsonBuilder().create()

    // Registra il servizio per la ricerca dell'ip
    private var nsdManager: NsdManager? = null
    private var registrationListener: NsdManager.RegistrationListener? = null

    // id del servizio
    private val serviceName = "KitchenService"
    private val serviceType = "_http._tcp."
    private val serverPort = 6000

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
                    Log.d("KitchenViewModel", "Server in ascolto sulla porta $serverPort...")
                    while (isServerRunning.get()) {
                        val clientSocket = serverSocket?.accept()

                        if (clientSocket != null) {
                            Log.d("KitchenViewModel", "Connessione ricevuta da ${clientSocket.inetAddress.hostAddress}")
                            launch {
                                handleClient(clientSocket)
                            }
                        }
                    }
                } catch (e: SocketException) {
                    // Si verifica quando il server viene interrotto (fine di vita del viewmodel)
                    Log.d("KitchenViewModel", "Server interrotto: ${e.message}")
                } catch (e: Exception) {
                    Log.e("KitchenViewModel", "Errore durante il server: ${e.message}")
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
                    Log.d("KitchenViewModel", "Dati Json ricevuti: $jsonString")
                    try {
                        val order = gson.fromJson(jsonString, Order::class.java)
                        // Passaggio dell'ordine all'UI tramite viewmodel
                        withContext(Dispatchers.Main) {
                            addOrder(order)
                        }
                    } catch (e: Exception) {
                        Log.e("KitchenViewModel", "Errore nella conversione del Json: ${e.message}")
                    }
                } else {
                    Log.d("KitchenViewModel", "Nessun ordine ricevuto")
                }
            }
        }
    }

    // Chiude il server quando non è più necessario
    override fun onCleared() {
        super.onCleared()
        registrationListener?.let { nsdManager?.unregisterService(it) }
        stopListeningForOrders()
    }

    private fun stopListeningForOrders() {
        Log.d("KitchenViewModel", "Tentativo di chiusura del server")
        isServerRunning.set(false)
        serverJob?.cancel()
    }

    private fun addOrder(order: Order) {
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