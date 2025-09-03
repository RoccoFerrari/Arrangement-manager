package com.example.arrangement_manager.menu_order

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.arrangement_manager.databinding.DialogOrderMenuBinding
import com.example.arrangement_manager.retrofit.MenuItem
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.DecimalFormat

/**
 * A DialogFragment for managing and confirming orders for a specific table.
 *
 * This fragment displays a list of menu items, allows the user to adjust the quantity
 * for each item, shows the total price, and sends the order to the backend.
 */
class MenuOrderDialogFragment : DialogFragment() {
    // NavArgs to retrieve arguments passed to the fragment, such as the user's email and the table
    private val args: MenuOrderDialogFragmentArgs by navArgs()

    private var _binding: DialogOrderMenuBinding? = null
    private val binding get() = _binding!!

    private lateinit var menuAdapter: MenuOrderAdapter

    // ViewModel for this fragment, initialized with the user's email
    private val viewModel: MenuOrderViewModel by viewModels {
        MenuOrderViewModelFactory(requireActivity().application, args.userEmail)
    }

    /**
     * Called to have the fragment instantiate its user interface view.
     * Inflates the dialog layout using view binding.
     */
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogOrderMenuBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onStart() {
        super.onStart()

        dialog?.window?.let { window ->
            val metrics = resources.displayMetrics
            val width = metrics.widthPixels
            val height = metrics.heightPixels

            val newHeight = (height * 0.50).toInt()
            val newWidth = (width * 0.85).toInt()

            window.setLayout(newWidth, ViewGroup.LayoutParams.WRAP_CONTENT)
            binding.recyclerViewMenuItems.layoutParams.height = newHeight
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setFragmentResultListener("menu_update_request_key") { key, bundle ->
            val isUpdateSuccessful = bundle.getBoolean("update_success")
            if(isUpdateSuccessful) {
                viewModel.getMenu()
            } else {
                Toast.makeText(requireContext(), "Menu update failed", Toast.LENGTH_SHORT).show()
            }
        }

        binding.textViewTableNumber.text = (args.table.name)

        setupRecyclerView()
        observeViewModel()

        // Set up the click listener for the confirm order button
        binding.buttonConfirmOrder.setOnClickListener {
            viewModel.sendOrder(args.table)
        }
    }

    /**
     * Initializes and configures the RecyclerView for displaying menu items.
     */
    private fun setupRecyclerView() {
        menuAdapter = MenuOrderAdapter(
            menuItems = emptyList(),
            orderedQuantities = emptyMap(),
            onQuantityChanged = { menuItem, quantity ->
                viewModel.onQuantityChanged(menuItem, quantity)
            },
            // Lambda to handle the click event on a menu item
            onDishNameClicked = { menuItem ->
                openDishDescriptionDialog(menuItem)
            }
        )
        binding.recyclerViewMenuItems.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = menuAdapter
        }
    }

    /**
     * Opens a dialog to display the description of a menu item.
     * @param menuItem The [MenuItem] whose description will be shown.
     */
    private fun openDishDescriptionDialog(menuItem: MenuItem) {
        val dishDescription = if (menuItem.description.isEmpty()) {
            "No description"
        } else {
            menuItem.description
        }
        val action = MenuOrderDialogFragmentDirections
            .actionMenuOrderDialogFragmentToDishDescriptionDialogFragment(
                dishName = menuItem.name,
                dishDescription = dishDescription,
                dishPrice = menuItem.price.toString(),
                dishQuantity = menuItem.quantity.toString(),
                userEmail = args.userEmail
            )
        findNavController().navigate(action)
    }

    /**
     * Sets up observers for the ViewModel's LiveData and Flow.
     *
     * It observes changes in the list of menu items, ordered quantities, loading state,
     * error messages, order confirmation, and total price to update the UI accordingly.
     */
    private fun observeViewModel() {
        // Observe at the list of menu items
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.menuItems.collectLatest { newMenuItems ->
                val orderedQuantities = viewModel.orderedItems.value
                menuAdapter.updateItems(newMenuItems, orderedQuantities)
            }
        }
        // Observe the list of ordered quantities
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.orderedItems.collectLatest { newOrderedQuantities ->
                val menuItems = viewModel.menuItems.value
                menuAdapter.updateItems(menuItems, newOrderedQuantities)
            }
        }

        // Observe the confirm button state
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isConfirmButtonEnabled.collectLatest { isEnabled ->
                binding.buttonConfirmOrder.isEnabled = isEnabled && !viewModel.isLoading.value
            }
        }

        // Observe the loading state
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isLoading.collectLatest { isLoading ->
                // Update the button state based on isConfirmButtonEnabled and isLoading
                binding.buttonConfirmOrder.isEnabled = viewModel.isConfirmButtonEnabled.value && !isLoading
            }
        }

        // Observe the error messages
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.errorMessage.collectLatest { errorMessage ->
                if (errorMessage != null) {
                    Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show()
                }
            }
        }

        // Observe if the order has been confirmed
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.orderConfirmed.collectLatest { confirmed ->
                if (confirmed) {
                    dismiss()
                }
            }
        }

        // Observe the total price
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.totalPrice.collectLatest { newTotalPrice ->
                val priceFormat = DecimalFormat("0.00")
                binding.textViewTotalPrice.text = "Total: ${priceFormat.format(newTotalPrice)}€"
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
