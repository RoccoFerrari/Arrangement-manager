package com.example.arrangement_manager.menu_order

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.arrangement_manager.R
import com.example.arrangement_manager.databinding.DialogOrderMenuBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MenuOrderDialogFragment : DialogFragment() {
    private val args: MenuOrderDialogFragmentArgs by navArgs()

    private var _binding: DialogOrderMenuBinding? = null
    private val binding get() = _binding!!

    private lateinit var menuAdapter: MenuOrderAdapter
    private lateinit var viewModel: MenuOrderViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogOrderMenuBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Imposta lo stile personalizzato che forza il dialog a non essere a schermo intero
        setStyle(STYLE_NORMAL, R.style.MenuOrderDialogTheme)
    }

    override fun onStart() {
        super.onStart()

        dialog?.window?.let { window ->
            val metrics = resources.displayMetrics
            val width = metrics.widthPixels

            val newWidth = (width * 0.85).toInt()

            window.setLayout(newWidth, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val factory = MenuOrderViewModelFactory(args.userEmail)
        viewModel = ViewModelProvider(this, factory)[MenuOrderViewModel::class.java]

        binding.textViewTableNumber.text = (args.table.name)

        setupRecyclerView()
        observeViewModel()

        binding.buttonConfirmOrder.setOnClickListener {
            viewModel.sendOrder(args.table.name)
        }
    }

    private fun setupRecyclerView() {
        menuAdapter = MenuOrderAdapter(
            menuItems = emptyList(),
            orderedQuantities = emptyMap(),
            onQuantityChanged = { menuItem, quantity ->
                viewModel.onQuantityChanged(menuItem, quantity)
            }
        )
        binding.recyclerViewMenuItems.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = menuAdapter
        }
    }

    private fun observeViewModel() {
        // Osserva la lista di menu items
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.menuItems.collectLatest { newMenuItems ->
                val orderedQuantities = viewModel.orderedItems.value
                menuAdapter.updateItems(newMenuItems, orderedQuantities)
            }
        }
        // Osserva le quantità ordinate
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.orderedItems.collectLatest { newOrderedQuantities ->
                val menuItems = viewModel.menuItems.value
                menuAdapter.updateItems(menuItems, newOrderedQuantities)
            }
        }

        // Osserva lo stato del pulsante combinando isLoading e isConfirmButtonEnabled
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isConfirmButtonEnabled.collectLatest { isEnabled ->
                // Controlla anche lo stato di caricamento per evitare clic multipli
                binding.buttonConfirmOrder.isEnabled = isEnabled && !viewModel.isLoading.value
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isLoading.collectLatest { isLoading ->
                // Aggiorna lo stato del pulsante in base a isConfirmButtonEnabled e isLoading
                binding.buttonConfirmOrder.isEnabled = viewModel.isConfirmButtonEnabled.value && !isLoading
            }
        }

        // Osserva i messaggi di errore
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.errorMessage.collectLatest { errorMessage ->
                if (errorMessage != null) {
                    Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show()
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.orderConfirmed.collectLatest { confirmed ->
                if (confirmed) {
                    dismiss()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
