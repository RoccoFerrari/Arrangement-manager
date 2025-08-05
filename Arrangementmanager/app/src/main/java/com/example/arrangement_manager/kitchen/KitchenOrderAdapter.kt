package com.example.arrangement_manager.kitchen

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.arrangement_manager.databinding.ItemKitchenOrderBinding

class KitchenOrderAdapter(
    private val onDishReady: (orderId: String, dishItem: DishItem) -> Unit,
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
            val total = order.dishes.sumOf { it.price.toDouble() }
            binding.tvOrderTotal.text = "Totale: € %.2f".format(total)

            // Setup dell'adapter interno per i piatti
            val dishAdapter = KitchenDishAdapter { dishItem ->
                // Callback per quando un singolo piatto è pronto
                onDishReady(order.orderId, dishItem)
            }
            binding.rvDishesInOrder.adapter = dishAdapter
            binding.rvDishesInOrder.layoutManager = LinearLayoutManager(binding.root.context)

            // Usa submitList per aggiornare la lista dei piatti
            dishAdapter.submitList(order.dishes)

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