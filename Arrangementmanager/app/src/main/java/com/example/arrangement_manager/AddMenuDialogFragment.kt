package com.example.arrangement_manager

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.navigation.fragment.navArgs
import com.example.arrangement_manager.databinding.DialogAddMenuBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AddMenuDialogFragment : DialogFragment() {
    private var _binding: DialogAddMenuBinding? = null
    private val binding get() = _binding!!
    private val args: AddMenuDialogFragmentArgs by navArgs()

    // Lista temporanea per memorizzare i prodotti prima del salvataggio
    private val newMenuItems = mutableListOf<MenuItem>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = DialogAddMenuBinding.inflate(inflater, container, false)
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

        binding.buttonAddItem.setOnClickListener {
            addItem()
        }

        binding.buttonSaveMenu.setOnClickListener {
            saveMenu()
            dismiss()
        }
    }

    private fun addItem() {
        val name = binding.editTextName.text.toString().trim()
        val priceStr = binding.editTextPrice.text.toString().trim()
        val quantityStr = binding.editTextQuantity.text.toString().trim()
        val description = binding.editTextDescription.text.toString().trim()

        if(name.isEmpty() || priceStr.isEmpty() || quantityStr.isEmpty()) {
            Toast.makeText(requireContext(), "Please fill all the fields", Toast.LENGTH_SHORT).show()
            return
        }

        val price = priceStr.toFloatOrNull()
        val quantity = quantityStr.toIntOrNull()

        if(price == null || quantity == null) {
            Toast.makeText(requireContext(), "Invalid price or quantity", Toast.LENGTH_SHORT).show()
            return
        }

        val userId = args.userEmail
        val newMenuItem = MenuItem(
            name = name,
            price = price,
            quantity = quantity,
            description = description,
            id_user = userId
        )
        newMenuItems.add(newMenuItem)

        binding.editTextName.text?.clear()
        binding.editTextPrice.text?.clear()
        binding.editTextQuantity.text?.clear()
        binding.editTextDescription.text?.clear()
    }

    private fun saveMenu() {
        if(newMenuItems.isEmpty()) {
            Toast.makeText(requireContext(), "Please add at least one item", Toast.LENGTH_SHORT).show()
            return
        }
        CoroutineScope(Dispatchers.IO).launch {
            val menuItemDao = (requireActivity().application as YourApplicationClass).arrangementDAO
            newMenuItems.forEach { menuItem ->
                menuItemDao.insertMenu(menuItem)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}