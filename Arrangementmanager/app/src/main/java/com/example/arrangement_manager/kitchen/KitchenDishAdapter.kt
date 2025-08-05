package com.example.arrangement_manager.kitchen

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.arrangement_manager.databinding.ItemKitchenDishBinding

class KitchenDishAdapter(
    private val onDishReady: (dishItem: DishItem) -> Unit
) : ListAdapter<DishItem, KitchenDishAdapter.DishViewHolder>(DishDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DishViewHolder {
        val binding = ItemKitchenDishBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DishViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DishViewHolder, position: Int) {
        val dish = getItem(position)
        holder.bind(dish)
    }

    inner class DishViewHolder(private val binding: ItemKitchenDishBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(dishItem: DishItem) {
            binding.tvDishName.text = dishItem.dishName
            binding.tvDishPrice.text = "€ %.2f".format(dishItem.price)

            binding.btnDishReady.setOnClickListener {
                onDishReady(dishItem)
            }
        }
    }

    private class DishDiffCallback : DiffUtil.ItemCallback<DishItem>() {
        override fun areItemsTheSame(oldItem: DishItem, newItem: DishItem): Boolean {
            return oldItem.dishName == newItem.dishName // In un'app reale useresti un ID unico
        }

        override fun areContentsTheSame(oldItem: DishItem, newItem: DishItem): Boolean {
            return oldItem == newItem
        }
    }
}
