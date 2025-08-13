package com.example.arrangement_manager.kitchen

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.arrangement_manager.databinding.ItemKitchenDishBinding

/**
 * RecyclerView adapter for displaying a list of individual dishes within an order.
 *
 * This adapter is used inside the [KitchenOrderAdapter] to show the dishes for a single
 * table. It is a standard [RecyclerView.Adapter] and is responsible for binding the
 * dish data to the view and handling the "ready" button click for each dish.
 *
 * @property onDishReady A callback function invoked when a dish is marked as ready.
 */
class KitchenDishAdapter(
    private val onDishReady: (displayDishItem: DisplayDishItem) -> Unit
) : RecyclerView.Adapter<KitchenDishAdapter.DishViewHolder>()  {

    private var dishes: List<DisplayDishItem> = emptyList()

    /**
     * Updates the list of dishes displayed by the adapter.
     *
     * This method is used by the parent adapter ([KitchenOrderAdapter]) to provide the
     * list of dishes for a specific order. It then notifies the adapter that the data has changed.
     *
     * @param list The new list of [DisplayDishItem]s to be displayed.
     */
    fun submitList(list: List<DisplayDishItem>) {
        this.dishes = list
        // Notify the adapter that the data has changed
        notifyDataSetChanged()
    }

    /**
     * Called when RecyclerView needs a new [DishViewHolder] to represent a dish item.
     *
     * @param parent The ViewGroup into which the new View will be added.
     * @param viewType The view type of the new View.
     * @return A new [DishViewHolder] that holds a view of the given view type.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DishViewHolder {
        val binding = ItemKitchenDishBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DishViewHolder(binding)
    }

    /**
     * Called by RecyclerView to display the data at the specified position.
     *
     * @param holder The ViewHolder which should be updated to represent the contents of the dish item.
     * @param position The position of the item within the adapter's data set.
     */
    override fun onBindViewHolder(holder: DishViewHolder, position: Int) {
        val dish = dishes[position]
        holder.bind(dish)
    }

    /**
     * Returns the total number of items in the data set held by the adapter.
     *
     * @return The total number of dishes.
     */
    override fun getItemCount(): Int = dishes.size

    /**
     * A [RecyclerView.ViewHolder] that represents an individual dish item in the list.
     *
     * @param binding The view binding for the dish item layout.
     */
    inner class DishViewHolder(private val binding: ItemKitchenDishBinding) :
        RecyclerView.ViewHolder(binding.root) {
        
        /**
         * Binds the data of a [DisplayDishItem] object to the views in the ViewHolder.
         *
         * This method sets the dish name and price, and attaches a click listener to the
         * "ready" button for the dish.
         *
         * @param displayDishItem The [DisplayDishItem] object to bind to the view.
         */
        fun bind(displayDishItem: DisplayDishItem) {
            binding.tvDishName.text = displayDishItem.dishItem.dishName
            binding.tvDishPrice.text = "€ %.2f".format(displayDishItem.dishItem.price)

            binding.btnDishReady.setOnClickListener {
                onDishReady(displayDishItem)
            }
        }
    }
}