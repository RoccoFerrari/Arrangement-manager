package com.example.arrangement_manager.menu_order

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.arrangement_manager.R
import com.example.arrangement_manager.retrofit.MenuItem

/**
 * Adapter for the RecyclerView in [MenuOrderDialogFragment].
 *
 * This adapter binds [MenuItem] data to the view elements in the `item_order_menu.xml` layout,
 * allowing users to view and adjust the quantity of each menu item. It handles click events
 * for adding/removing items and for viewing dish descriptions.
 *
 * @property menuItems The list of all available menu items.
 * @property orderedQuantities A map containing the quantity of each item currently ordered.
 * @property onQuantityChanged A lambda function to be called when an item's quantity is changed.
 * @property onDishNameClicked A lambda function to be called when a dish name is clicked.
 */
class MenuOrderAdapter(
    private var menuItems: List<MenuItem>,
    private var orderedQuantities: Map<MenuItem, Int>,
    private val onQuantityChanged: (MenuItem, Int) -> Unit,
    private val onDishNameClicked: (MenuItem) -> Unit
) : RecyclerView.Adapter<MenuOrderAdapter.MenuOrderViewHolder>() {

    /**
     * ViewHolder for the [MenuOrderAdapter].
     *
     * This class holds references to the UI elements of each item in the RecyclerView,
     * making it efficient to bind data without repeatedly looking up views.
     * @param view The inflated view for a single list item.
     */
    class MenuOrderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nameTextView: TextView = view.findViewById(R.id.textView_item_name)
        val priceTextView: TextView = view.findViewById(R.id.textView_item_price)
        val availableQuantityTextView: TextView = view.findViewById(R.id.textView_item_available_quantity)
        val orderedQuantityTextView: TextView = view.findViewById(R.id.textView_ordered_quantity)
        val addButton: ImageButton = view.findViewById(R.id.button_add)
        val removeButton: ImageButton = view.findViewById(R.id.button_remove)
    }

    /**
     * Updates the data set of the adapter and notifies the RecyclerView of the change.
     * @param newMenuItems The new list of menu items.
     * @param newOrderedQuantities The new map of ordered quantities.
     */
    fun updateItems(newMenuItems: List<MenuItem>, newOrderedQuantities: Map<MenuItem, Int>) {
        this.menuItems = newMenuItems
        this.orderedQuantities = newOrderedQuantities
        notifyDataSetChanged()
    }

    /**
     * Creates and returns a [MenuOrderViewHolder] for the specified view type.
     * @param parent The ViewGroup into which the new View will be added.
     * @param viewType The view type of the new View.
     * @return A new [MenuOrderViewHolder] that holds the View for each item.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MenuOrderViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_order_menu, parent, false)
        return MenuOrderViewHolder(view)
    }

    /**
     * Returns the total number of items in the data set held by the adapter.
     * @return The size of the `menuItems` list.
     */
    override fun getItemCount() = menuItems.size

    /**
     * Binds data from the data set to the views in the [MenuOrderViewHolder].
     *
     * This method is called to update the contents of the item at the given position.
     * It also sets up click listeners for the add and remove buttons and the dish name.
     *
     * @param holder The ViewHolder to be updated.
     * @param position The position of the item within the adapter's data set.
     */
    override fun onBindViewHolder(holder: MenuOrderViewHolder, position: Int) {
        val menuItem = menuItems[position]
        holder.nameTextView.text = menuItem.name
        holder.priceTextView.text = "€${menuItem.price}"

        val orderedQuantity = orderedQuantities[menuItem] ?: 0
        val remainingQuantity = menuItem.quantity - orderedQuantity

        holder.orderedQuantityTextView.text = orderedQuantity.toString()
        holder.availableQuantityTextView.text = remainingQuantity.toString()

        holder.addButton.setOnClickListener {
            if (orderedQuantity < menuItem.quantity) {
                onQuantityChanged(menuItem, orderedQuantity + 1)
            }
        }

        holder.removeButton.setOnClickListener {
            if (orderedQuantity > 0) {
                onQuantityChanged(menuItem, orderedQuantity - 1)
            }
        }

        holder.nameTextView.setOnClickListener {
            onDishNameClicked(menuItem)
        }
    }
}