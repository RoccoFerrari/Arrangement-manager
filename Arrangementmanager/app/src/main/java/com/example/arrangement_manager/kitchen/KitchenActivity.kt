package com.example.arrangement_manager.kitchen

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.arrangement_manager.databinding.ActivityOrderKitchenBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.ServerSocket
import java.net.Socket
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

// Classe per un singolo piatto all'interno di un ordine
data class DishItem(
    val dishName: String,
    val price: Float,
    val quantity: Int
)
// Classe per un intero ordine, contenente una lista di piatti
data class Order(
    val orderId: String, // ID per l'ordine
    val tableId: String, // ID per il tavolo
    val dishes: List<DishItem>
)
class KitchenActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOrderKitchenBinding
    private lateinit var viewModel: KitchenViewModel
    private lateinit var adapter: KitchenOrderAdapter
    private val serverJob = Job()
    private val serverScope = CoroutineScope(Dispatchers.IO + serverJob)
    private val serverPort = 6000 // Porta per la comunicazione

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOrderKitchenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // KitchenViewModelFactory per istanziare il ViewModel
        viewModel = ViewModelProvider(this, KitchenViewModelFactory()).get(KitchenViewModel::class.java)

        setupRecyclerView()
        startServer()

        viewModel.kitchenOrders.observe(this) { orders ->
            if (orders.isEmpty()) {
                binding.tvNoOrders.visibility = android.view.View.VISIBLE
                binding.rvKitchenOrders.visibility = android.view.View.GONE
            } else {
                binding.tvNoOrders.visibility = android.view.View.GONE
                binding.rvKitchenOrders.visibility = android.view.View.VISIBLE
            }
            adapter.submitList(orders)
        }
    }

    private fun setupRecyclerView() {
        binding.rvKitchenOrders.layoutManager = LinearLayoutManager(this)

        // L'adapter gestisce una lista di oggetti 'Order' (un ordine per ogni tavolo)
        adapter = KitchenOrderAdapter(
            onDishReady = { orderId, dishItem ->
                // Logica per rimuovere un singolo piatto
                Toast.makeText(this, "Piatto '${dishItem.dishName}' dell'ordine $orderId pronto!", Toast.LENGTH_SHORT).show()
                viewModel.removeDishFromOrder(orderId, dishItem)
                // TODO: Implementa qui la logica per notificare il cameriere
            },
            onOrderReady = { orderId ->
                // Logica per rimuovere l'intero ordine
                Toast.makeText(this, "Ordine per tavolo $orderId completato!", Toast.LENGTH_SHORT).show()
                viewModel.removeOrder(orderId)
                // TODO: Implementa qui la logica per notificare il cameriere
            }
        )
        binding.rvKitchenOrders.adapter = adapter
    }

    private fun startServer() {
        serverScope.launch {
            try {
                val serverSocket = ServerSocket(serverPort)
                Log.d("KitchenActivity", "Server in ascolto sulla porta $serverPort...")
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@KitchenActivity, "Modalità Cucina avviata. In attesa di ordini...", Toast.LENGTH_SHORT).show()
                }

                while (isActive) {
                    val clientSocket: Socket = serverSocket.accept()
                    Log.d("KitchenActivity", "Connessione ricevuta da ${clientSocket.inetAddress.hostAddress}")
                    handleClient(clientSocket)
                }
            } catch (e: Exception) {
                Log.e("KitchenActivity", "Errore del server: ${e.message}")
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@KitchenActivity, "Errore durante l'avvio della modalità Cucina.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private suspend fun handleClient(clientSocket: Socket) = withContext(Dispatchers.IO) {
        try {
            val reader = BufferedReader(InputStreamReader(clientSocket.getInputStream()))
            val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
            val adapter = moshi.adapter(Order::class.java)

            // Legge i dati inviati dal client, riga per riga
            val receivedJson = reader.readLine()
            if (receivedJson != null) {
                val newOrder = adapter.fromJson(receivedJson)
                if (newOrder != null) {
                    Log.d("KitchenActivity", "Ordine ricevuto: $newOrder")

                    // Aggiorna il ViewModel con il nuovo ordine
                    withContext(Dispatchers.Main) {
                        viewModel.addOrder(newOrder)
                    }

                    // TODO: Aggiungi qui la logica per salvare l'ordine nel database
                } else {
                    Log.e("KitchenActivity", "Errore nella deserializzazione dell'ordine")
                }
            } else {
                Log.e("KitchenActivity", "Nessun ordine ricevuto")
            }

        } catch (e: Exception) {
            Log.e("KitchenActivity", "Errore durante la gestione del client: ${e.message}")
        } finally {
            clientSocket.close()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serverJob.cancel()
    }
}