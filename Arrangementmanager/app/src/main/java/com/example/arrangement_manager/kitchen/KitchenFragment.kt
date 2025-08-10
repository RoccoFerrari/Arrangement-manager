package com.example.arrangement_manager.kitchen

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.arrangement_manager.databinding.FragmentOrderKitchenBinding // Assicurati di avere il layout corretto per il Fragment


data class DishItem(
    val dishName: String,
    val price: Float,
    val quantity: Int
)

data class Order(
    val orderId: String,
    val tableId: String,
    val dishes: List<DishItem>
)

class KitchenFragment : Fragment() {

    private var _binding: FragmentOrderKitchenBinding? = null
    private val binding get() = _binding!!

    private val viewModel: KitchenViewModel by viewModels {
        KitchenViewModelFactory(requireActivity().application)
    }
    private lateinit var adapter: KitchenOrderAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOrderKitchenBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        binding.rvKitchenOrders.layoutManager = LinearLayoutManager(requireContext())

        adapter = KitchenOrderAdapter(
            onDishReady = { orderId, dishItem ->
                Toast.makeText(requireContext(), "Piatto '${dishItem.dishItem.dishName}' dell'ordine $orderId pronto!", Toast.LENGTH_SHORT).show()
                viewModel.removeDishFromOrder(orderId, dishItem)
            },
            onOrderReady = { orderId ->
                Toast.makeText(requireContext(), "Ordine per tavolo $orderId completato!", Toast.LENGTH_SHORT).show()
                viewModel.removeOrder(orderId)
            }
        )
        binding.rvKitchenOrders.adapter = adapter
    }

    private fun observeViewModel() {
        // View LiveData using viewLifecycleOwner
        viewModel.kitchenOrders.observe(viewLifecycleOwner) { orders ->
            if (orders.isEmpty()) {
                binding.tvNoOrders.visibility = View.VISIBLE
                binding.rvKitchenOrders.visibility = View.GONE
            } else {
                binding.tvNoOrders.visibility = View.GONE
                binding.rvKitchenOrders.visibility = View.VISIBLE
            }
            adapter.submitList(orders)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}