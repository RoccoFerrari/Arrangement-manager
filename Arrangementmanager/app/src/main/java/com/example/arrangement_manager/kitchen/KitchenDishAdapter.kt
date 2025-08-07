package com.example.arrangement_manager.kitchen

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.arrangement_manager.databinding.ItemKitchenDishBinding

class KitchenDishAdapter(
    private val onDishReady: (displayDishItem: DisplayDishItem) -> Unit
) : RecyclerView.Adapter<KitchenDishAdapter.DishViewHolder>()  {

    private var dishes: List<DisplayDishItem> = emptyList()

    fun submitList(list: List<DisplayDishItem>) {
        this.dishes = list
        notifyDataSetChanged() // Notifica l'adapter che i dati sono cambiati
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DishViewHolder {
        val binding = ItemKitchenDishBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DishViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DishViewHolder, position: Int) {
        val dish = dishes[position]
        holder.bind(dish)
    }

    override fun getItemCount(): Int = dishes.size

    inner class DishViewHolder(private val binding: ItemKitchenDishBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(displayDishItem: DisplayDishItem) {
            binding.tvDishName.text = displayDishItem.dishItem.dishName
            binding.tvDishPrice.text = "€ %.2f".format(displayDishItem.dishItem.price)

            binding.btnDishReady.setOnClickListener {
                onDishReady(displayDishItem)
            }
        }
    }
}