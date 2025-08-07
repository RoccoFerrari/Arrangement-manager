package com.example.arrangement_manager.kitchen

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.arrangement_manager.databinding.ItemKitchenOrderBinding
import java.util.UUID

// Wrapper per i dati del piatto con un ID univoco
data class DisplayDishItem(
    val id: String = UUID.randomUUID().toString(),
    val dishItem: DishItem
)

class KitchenOrderAdapter(
    private val onDishReady: (orderId: String, displayDishItem: DisplayDishItem) -> Unit,
    private val onOrderReady: (orderId: String) -> Unit
) : ListAdapter<Order, KitchenOrderAdapter.OrderViewHolder>(OrderDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val binding = ItemKitchenOrderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return OrderViewHolder(binding)
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        val order = getItem(position)
        holder.bind(order)
    }

    inner class OrderViewHolder(private val binding: ItemKitchenOrderBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(order: Order) {
            binding.tvTableNumber.text = "Tavolo: ${order.tableId}"
            val total = order.dishes.sumOf { it.price.toDouble() * it.quantity }
            binding.tvOrderTotal.text = "Totale: € %.2f".format(total)

            // Setup dell'adapter interno per i piatti
            val dishAdapter = KitchenDishAdapter { displayDishItem ->
                onDishReady(order.orderId, displayDishItem)
            }

            binding.rvDishesInOrder.adapter = dishAdapter
            binding.rvDishesInOrder.layoutManager = LinearLayoutManager(binding.root.context)

            // Permette di vedere un piatto un numero 'quantity' di volte
            val expandedDishes = mutableListOf<DisplayDishItem>()
            order.dishes.forEach { dish ->
                repeat(dish.quantity) {
                    expandedDishes.add(DisplayDishItem(dishItem = dish))
                }
            }

            // Usa submitList per aggiornare la lista dei piatti
            dishAdapter.submitList(expandedDishes)

            // Listener per il pulsante "Ordine Completato"
            binding.btnCompleteOrder.setOnClickListener {
                onOrderReady(order.orderId)
            }
        }
    }

    private class OrderDiffCallback : DiffUtil.ItemCallback<Order>() {
        override fun areItemsTheSame(oldItem: Order, newItem: Order): Boolean {
            return oldItem.orderId == newItem.orderId
        }

        override fun areContentsTheSame(oldItem: Order, newItem: Order): Boolean {
            return oldItem == newItem
        }
    }
}