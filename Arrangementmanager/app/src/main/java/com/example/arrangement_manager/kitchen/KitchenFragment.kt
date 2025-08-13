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

/**
 * A [Fragment] that displays a list of incoming orders for the kitchen staff.
 *
 * This fragment acts as the UI component for the kitchen server, observing the
 * [KitchenViewModel]'s LiveData to display real-time order updates. It provides functionality
 * for kitchen staff to manage orders, such as marking individual dishes or entire orders as ready.
 */
class KitchenFragment : Fragment() {

    // View binding for the fragment's layout
    private var _binding: FragmentOrderKitchenBinding? = null
    private val binding get() = _binding!!

    // Initializes the ViewModel using a custom factory to provide the application context
    private val viewModel: KitchenViewModel by viewModels {
        KitchenViewModelFactory(requireActivity().application)
    }

    // Adapter for the RecyclerView to display the list of orders
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

    /**
     * Configures the RecyclerView for displaying kitchen orders.
     *
     * This method initializes the [LinearLayoutManager] and the [KitchenOrderAdapter], setting up
     * click listeners for when a dish or an entire order is marked as ready.
     */
    private fun setupRecyclerView() {
        binding.rvKitchenOrders.layoutManager = LinearLayoutManager(requireContext())

        adapter = KitchenOrderAdapter(
            onDishReady = { orderId, dishItem ->
                Toast.makeText(requireContext(), "Dish '${dishItem.dishItem.dishName}' ready!", Toast.LENGTH_SHORT).show()
                viewModel.removeDishFromOrder(orderId, dishItem)
            },
            onOrderReady = { orderId ->
                Toast.makeText(requireContext(), "Order $orderId completed!", Toast.LENGTH_SHORT).show()
                viewModel.removeOrder(orderId)
            }
        )
        binding.rvKitchenOrders.adapter = adapter
    }

    /**
     * Observes the [KitchenViewModel]'s [kitchenOrders] LiveData.
     *
     * When the list of orders changes, this method updates the adapter to reflect the new data
     * and shows/hides a "No orders" message based on whether the list is empty.
     */
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