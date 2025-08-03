package com.example.arrangement_manager

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val dao = (requireActivity().application as YourApplicationClass).arrangementDAO
        val factory = MenuOrderViewModelFactory(dao, args.userEmail)

        viewModel = ViewModelProvider(this, factory)[MenuOrderViewModel::class.java]

        binding.textViewTableNumber.text = "Tavolo N° ${args.table.name}"

        setupRecyclerView()
        observeViewModel()

        binding.buttonConfirmOrder.setOnClickListener {
            viewModel.sendOrder()
            dismiss()
        }
    }

    private fun setupRecyclerView() {
        menuAdapter = MenuOrderAdapter(
            menuItems = emptyList(), // I dati iniziali sono vuoti
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
        viewLifecycleOwner.lifecycleScope.launch {
            // Osserviamo la lista di menu items dal ViewModel
            viewModel.menuItems.collectLatest { newMenuItems ->
                // Quando la lista cambia, aggiorniamo l'adapter
                menuAdapter.updateItems(newMenuItems)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
