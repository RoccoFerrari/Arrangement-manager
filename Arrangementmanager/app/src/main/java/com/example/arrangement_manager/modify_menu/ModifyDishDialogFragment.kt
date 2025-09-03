package com.example.arrangement_manager.modify_menu

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.arrangement_manager.R
import com.example.arrangement_manager.databinding.DialogAddMenuBinding
import com.example.arrangement_manager.databinding.DialogModifyDishBinding
import com.example.arrangement_manager.retrofit.MenuItemUpdate
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ModifyDishDialogFragment : DialogFragment() {
    // View binding for the dialog's layout
    private var _binding: DialogModifyDishBinding? = null
    private val binding get() = _binding!!

    // Retrieves navigation arguments, specifically the Dish'name, price, quantity and description
    private val args: ModifyDishDialogFragmentArgs by navArgs()

    // Initializes the ViewModel with the user's email from navigation arguments
    private val viewModel: ModifyDishViewModel by viewModels {
        ModifyDishViewModelFactory(args.userEmail)
    }

    private val textWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

        override fun afterTextChanged(s: Editable?) {
            updateButtonText()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogModifyDishBinding.inflate(inflater, container, false)
        return binding.root
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

        binding.editTextName.setHint(args.dishName)
        val priceAsFloat = args.dishPrice.toFloatOrNull()
        if (priceAsFloat != null) {
            binding.editTextPrice.setHint(String.format("%.2f", priceAsFloat) + " €")
        }
        binding.editTextQuantity.setHint(args.dishQuantity)
        binding.editTextDescription.setHint(args.dishDescription)

        binding.editTextName.addTextChangedListener(textWatcher)
        binding.editTextPrice.addTextChangedListener(textWatcher)
        binding.editTextQuantity.addTextChangedListener(textWatcher)
        binding.editTextDescription.addTextChangedListener(textWatcher)

        // Initialize the button text
        updateButtonText()

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collectLatest { uiState ->
                // Loading state
                binding.buttonConfirmItem.isEnabled = !uiState.isLoading

                // Success/error messages
                if (uiState.errorMessage != null) {
                    Toast.makeText(requireContext(), uiState.errorMessage, Toast.LENGTH_SHORT).show()

                    sendResultToMenuOrder(false)
                }
                if (uiState.successMessage != null) {
                    Toast.makeText(requireContext(), uiState.successMessage, Toast.LENGTH_SHORT).show()

                    sendResultToMenuOrder(true)
                }
            }
        }

        binding.buttonConfirmItem.setOnClickListener {
            updateItem()

            findNavController().popBackStack(R.id.menuOrderDialogFragment, false)
            dismiss()
        }
    }

    /**
     * Updates the button text based on the input fields.
     * If any of the fields is not empty, the button text is set to "Confirm Edit".
     * Otherwise, it is set to "Delete item".
     * This method is called after any text change in the input fields.
     */
    private fun updateButtonText() {
        val name = binding.editTextName.text.toString().trim()
        val price = binding.editTextPrice.text.toString().trim()
        val quantity = binding.editTextQuantity.text.toString().trim()
        val description = binding.editTextDescription.text.toString().trim()

        if (name.isNotEmpty() || price.isNotEmpty() || quantity.isNotEmpty() || description.isNotEmpty()) {
            binding.buttonConfirmItem.text = "Confirm Edit"
        } else {
            binding.buttonConfirmItem.text = "Delete item"
        }
    }

    /**
     * Gathers data from the input fields and adds a new menu item to the ViewModel.
     * This method validates the input, creates a new [MenuItemUpdate] object, and then
     * calls the ViewModel to insert it. It also clears the input fields after a successful
     * insertion.
     */
    private fun updateItem() {
        val name = binding.editTextName.text.toString().trim()
        val priceStr = binding.editTextPrice.text.toString().trim()
        val quantityStr = binding.editTextQuantity.text.toString().trim()
        val description = binding.editTextDescription.text.toString().trim()

        if(name.isEmpty() && priceStr.isEmpty() && quantityStr.isEmpty() && description.isEmpty()) {
            viewModel.deleteItem(args.userEmail, args.dishName)
            return
        }

        val newName = name.ifEmpty { args.dishName }
        val newPrice: Float? = priceStr.toFloatOrNull() ?: args.dishPrice.toFloatOrNull()
        val newQuantity: Int? = quantityStr.toIntOrNull() ?: args.dishQuantity.toIntOrNull()
        val newDescription = description.ifEmpty { args.dishDescription }

        val newMenuItemUpdate = MenuItemUpdate(
            name = newName,
            price = newPrice,
            quantity = newQuantity,
            description = newDescription
        )

        viewModel.updateItem(args.userEmail, args.dishName, newMenuItemUpdate)

        binding.editTextName.text?.clear()
        binding.editTextPrice.text?.clear()
        binding.editTextQuantity.text?.clear()
        binding.editTextDescription.text?.clear()

    }

    private fun sendResultToMenuOrder(success: Boolean) {
        val bundle = Bundle()
        bundle.putBoolean("update_success", success)
        setFragmentResult("menu_update_request_key", bundle)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.editTextName.removeTextChangedListener(textWatcher)
        binding.editTextPrice.removeTextChangedListener(textWatcher)
        binding.editTextQuantity.removeTextChangedListener(textWatcher)
        binding.editTextDescription.removeTextChangedListener(textWatcher)
        _binding = null
    }
}