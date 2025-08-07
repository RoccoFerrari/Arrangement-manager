package com.example.arrangement_manager.menu_order

import android.app.Application
import android.net.nsd.NsdManager
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
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
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.net.Socket
import java.util.UUID
import android.content.Context
import android.net.nsd.NsdServiceInfo
import java.net.InetSocketAddress

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

    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val orderAdapter = moshi.adapter(Order::class.java)
    private val apiService = RetrofitClient.apiService

    // Variabili per ip cucina dinamico
    private var nsdManager: NsdManager? = null
    private var discoveryListener: NsdManager.DiscoveryListener? = null
    private val serviceType = "_http._tcp."
    private val kitchenPort = 6000

    private val _kitchenIpAddress = MutableLiveData<String?>(null)
    private val kitchenIpAddress: LiveData<String?> = _kitchenIpAddress

    // Chiamata quando il ViewModel viene creato
    init {
        loadMenuItems()
        viewModelScope.launch {
            orderedItems.collectLatest { quantities ->
                calculateTotalPrice(quantities)
            }
        }
        // Avvio della scoperta dell'ip
        startDiscovery()
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
        viewModelScope.launch(Dispatchers.IO) { // Sposta l'operazione su un thread I/O
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
                        Log.d("MenuOrderViewModel", "Ordine inviato con successo al database. Procedo con l'invio alla cucina.")

                        var wifiSendSuccess = false
                        val kitchenIp = kitchenIpAddress.value

                        if (kitchenIp != null) {
                            var socket: Socket? = null
                            var writer: PrintWriter? = null
                            try {
                                Log.d("MenuOrderViewModel", "Tentativo di connessione all'IP: $kitchenIp sulla porta $kitchenPort")

                                // Utilizza un socket con timeout per una gestione più robusta
                                socket = Socket()
                                socket.connect(InetSocketAddress(kitchenIp, kitchenPort), 5000) // Timeout di 5 secondi
                                Log.d("MenuOrderViewModel", "Socket creato e connesso")

                                writer = PrintWriter(OutputStreamWriter(socket.getOutputStream()), true)
                                Log.d("MenuOrderViewModel", "Writer creato")

                                // Invio dell'ordine alla cucina tramite Wi-Fi
                                val dishesToSend = _orderedItems.value.map { (menuItem, quantity) ->
                                    DishItem(dishName = menuItem.name, price = menuItem.price, quantity = quantity)
                                }.filter { it.quantity > 0 }

                                val tableId = "${table.idUser}::${table.name}"
                                val orderId = UUID.randomUUID().toString()
                                val orderToSend = Order(orderId = orderId, tableId = tableId, dishes = dishesToSend)
                                val jsonString = orderAdapter.toJson(orderToSend)

                                Log.d("MenuOrderViewModel", "Invio di dati JSON: $jsonString")

                                writer.println(jsonString)
                                // Con `autoFlush` a `true` (impostato nel costruttore di PrintWriter),
                                // il flush è automatico dopo `println`.
                                // L'aggiunta di `writer.flush()` esplicito non è strettamente necessaria ma può essere utile in alcuni casi.

                                Log.d("MenuOrderViewModel", "Ordine inviato con successo anche alla cucina.")
                                wifiSendSuccess = true

                            } catch (e: Exception) {
                                Log.e("MenuOrderViewModel", "Errore nell'invio Wi-Fi: ${e.message}", e)
                                _errorMessage.value = "Ordine salvato nel database, ma errore nell'invio alla cucina: ${e.message}"
                            } finally {
                                // Assicurati che le risorse vengano sempre chiuse
                                writer?.close()
                                socket?.close()
                                Log.d("MenuOrderViewModel", "Socket e Writer chiusi.")
                            }

                        } else {
                            Log.e("MenuOrderViewModel", "Indirizzo IP della cucina non disponibile.")
                            _errorMessage.value = "Ordine salvato nel database, ma impossibile trovare il server della cucina."
                        }

                        // Continua con il resto della logica dopo l'invio
                        updateMenuItemQuantities()
                        _orderConfirmed.value = true

                    } else {
                        val errorBody = response.errorBody()?.string()
                        Log.e("MenuOrderViewModel", "Errore invio ordine al database: ${response.code()} - $errorBody")
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

    private fun startDiscovery() {
        nsdManager = getApplication<Application>().getSystemService(Context.NSD_SERVICE) as NsdManager

        discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onStartDiscoveryFailed(serviceType: String?, errorCode: Int) { Log.e("NSD", "Avvio scoperta fallito: $errorCode") }
            override fun onStopDiscoveryFailed(serviceType: String?, errorCode: Int) { Log.e("NSD", "Arresto scoperta fallito: $errorCode") }
            override fun onDiscoveryStarted(serviceType: String?) { Log.d("NSD", "Scoperta avviata: $serviceType") }
            override fun onDiscoveryStopped(serviceType: String?) { Log.d("NSD", "Scoperta arrestata: $serviceType") }
            override fun onServiceLost(serviceInfo: NsdServiceInfo?) { Log.d("NSD", "Servizio perso: ${serviceInfo?.serviceName}") }

            override fun onServiceFound(serviceInfo: NsdServiceInfo?) {
                if (serviceInfo?.serviceType == serviceType) {
                    Log.d("NSD", "Servizio trovato: ${serviceInfo.serviceName}")
                    // Trovato un servizio del tipo giusto, ora risolviamolo per ottenere l'IP
                    nsdManager?.resolveService(serviceInfo, object : NsdManager.ResolveListener {
                        override fun onResolveFailed(serviceInfo: NsdServiceInfo?, errorCode: Int) {
                            Log.e("NSD", "Risoluzione del servizio fallita: $errorCode")
                        }
                        override fun onServiceResolved(serviceInfo: NsdServiceInfo?) {
                            val ipAddress = serviceInfo?.host?.hostAddress
                            Log.d("NSD", "IP della cucina risolto: $ipAddress")
                            _kitchenIpAddress.postValue(ipAddress)
                            stopDiscovery()
                        }
                    })
                }
            }
        }

        nsdManager?.discoverServices(serviceType, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
    }
    private fun stopDiscovery() {
        discoveryListener?.let { listener ->
            nsdManager?.stopServiceDiscovery(listener)
            discoveryListener = null
        }
    }
    override fun onCleared() {
        super.onCleared()
        stopDiscovery()
    }
}