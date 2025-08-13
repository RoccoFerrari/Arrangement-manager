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

/**
 * RecyclerView adapter for displaying a list of kitchen orders.
 *
 * This adapter uses [ListAdapter] with [DiffUtil] for efficient list updates. Each item in the list
 * represents an order for a specific table, and internally it manages another RecyclerView
 * to display the individual dishes within that order.
 *
 * @property onDishReady A callback function invoked when a single dish is marked as ready.
 * @property onOrderReady A callback function invoked when an entire order is marked as ready.
 */
class KitchenOrderAdapter(
    private val onDishReady: (orderId: String, displayDishItem: DisplayDishItem) -> Unit,
    private val onOrderReady: (orderId: String) -> Unit
) : ListAdapter<Order, KitchenOrderAdapter.OrderViewHolder>(OrderDiffCallback()) {

    /**
     * Called when RecyclerView needs a new [OrderViewHolder] of the given type to represent an item.
     *
     * @param parent The ViewGroup into which the new View will be added.
     * @param viewType The view type of the new View.
     * @return A new [OrderViewHolder] that holds a view of the given view type.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val binding = ItemKitchenOrderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return OrderViewHolder(binding)
    }

    /**
     * Called by RecyclerView to display the data at the specified position.
     *
     * @param holder The ViewHolder which should be updated to represent the contents of the item at the given position.
     * @param position The position of the item within the adapter's data set.
     */
    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        val order = getItem(position)
        holder.bind(order)
    }

    /**
     * A [RecyclerView.ViewHolder] that represents an individual order item in the list.
     *
     * @param binding The view binding for the order item layout.
     */
    inner class OrderViewHolder(private val binding: ItemKitchenOrderBinding) :
        RecyclerView.ViewHolder(binding.root) {
        
        /**
         * Binds the data of an [Order] object to the views in the ViewHolder.
         *
         * This method sets the table number, calculates and displays the total price, and
         * sets up an internal adapter ([KitchenDishAdapter]) for the dishes in the order.
         * It also attaches a click listener to the "Complete Order" button.
         *
         * @param order The [Order] object to bind to the view.
         */
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

    /**
     * A [DiffUtil.ItemCallback] implementation to calculate the difference between two lists of [Order]s.
     *
     * This callback is used by [ListAdapter] to efficiently update the list when data changes.
     */
    private class OrderDiffCallback : DiffUtil.ItemCallback<Order>() {
        /**
         * Checks if two items represent the same item.
         * @return `true` if the items are the same, `false` otherwise.
         */
        override fun areItemsTheSame(oldItem: Order, newItem: Order): Boolean {
            return oldItem.orderId == newItem.orderId
        }

        /**
         * Checks if the contents of two items are the same.
         * @return `true` if the contents are the same, `false` otherwise.
         */
        override fun areContentsTheSame(oldItem: Order, newItem: Order): Boolean {
            return oldItem == newItem
        }
    }
}