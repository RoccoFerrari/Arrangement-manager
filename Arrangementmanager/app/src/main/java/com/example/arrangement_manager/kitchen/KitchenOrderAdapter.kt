package com.example.arrangement_manager.kitchen

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.arrangement_manager.databinding.ItemKitchenOrderBinding
import java.util.UUID

// Wrapper for plate data with a unique ID
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
            binding.tvTableNumber.text = "Table: ${order.tableId}"
            val total = order.dishes.sumOf { it.price.toDouble() * it.quantity }
            binding.tvOrderTotal.text = "Total: € %.2f".format(total)

            // Setting up the internal adapter for the cymbals
            val dishAdapter = KitchenDishAdapter { displayDishItem ->
                onDishReady(order.orderId, displayDishItem)
            }

            binding.rvDishesInOrder.adapter = dishAdapter
            binding.rvDishesInOrder.layoutManager = LinearLayoutManager(binding.root.context)

            // Allows to see a dish a 'quantity' number of times
            val expandedDishes = mutableListOf<DisplayDishItem>()
            order.dishes.forEach { dish ->
                repeat(dish.quantity) {
                    expandedDishes.add(DisplayDishItem(dishItem = dish))
                }
            }

            // Use submitList to update the dish list
            dishAdapter.submitList(expandedDishes)

            // Listener for the "Order Complete" button
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