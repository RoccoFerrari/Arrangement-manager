package com.example.arrangement_manager

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class MenuOrderAdapter(
    private var menuItems: List<MenuItem>,
    private val onQuantityChanged: (MenuItem, Int) -> Unit
) : RecyclerView.Adapter<MenuOrderAdapter.MenuOrderViewHolder>() {

    class MenuOrderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nameTextView: TextView = view.findViewById(R.id.textView_item_name)
        val priceTextView: TextView = view.findViewById(R.id.textView_item_price)
        val availableQuantityTextView: TextView = view.findViewById(R.id.textView_item_available_quantity)
        val orderedQuantityTextView: TextView = view.findViewById(R.id.textView_ordered_quantity)
        val addButton: ImageButton = view.findViewById(R.id.button_add)
        val removeButton: ImageButton = view.findViewById(R.id.button_remove)
    }

    fun updateItems(newMenuItems: List<MenuItem>) {
        this.menuItems = newMenuItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MenuOrderViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_order_menu, parent, false)
        return MenuOrderViewHolder(view)
    }

    override fun getItemCount() = menuItems.size

    override fun onBindViewHolder(holder: MenuOrderViewHolder, position: Int) {
        val menuItem = menuItems[position]
        holder.nameTextView.text = menuItem.name
        holder.priceTextView.text = "€${menuItem.price}"
        holder.availableQuantityTextView.text = menuItem.quantity.toString()
        var orderedQuantity = 0
        holder.orderedQuantityTextView.text = orderedQuantity.toString()

        holder.addButton.setOnClickListener {
            if (orderedQuantity < menuItem.quantity) {
                orderedQuantity++
                holder.orderedQuantityTextView.text = orderedQuantity.toString()
                onQuantityChanged(menuItem, orderedQuantity)
            }
        }

        holder.removeButton.setOnClickListener {
            if (orderedQuantity > 0) {
                orderedQuantity--
                holder.orderedQuantityTextView.text = orderedQuantity.toString()
                onQuantityChanged(menuItem, orderedQuantity)
            }
        }
    }
}