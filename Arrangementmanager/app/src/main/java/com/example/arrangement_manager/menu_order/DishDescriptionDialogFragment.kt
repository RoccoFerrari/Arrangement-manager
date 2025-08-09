package com.example.arrangement_manager.menu_order

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.example.arrangement_manager.databinding.DialogDishDescriptionBinding
import androidx.navigation.fragment.navArgs

class DishDescriptionDialogFragment : DialogFragment() {

    private var _binding: DialogDishDescriptionBinding? = null
    private val binding get() = _binding!!

    // Recupera gli argomenti passati al Fragment
    private val args: DishDescriptionDialogFragmentArgs by navArgs()

    override fun onStart() {
        super.onStart()

        dialog?.window?.let { window ->
            val metrics = resources.displayMetrics
            val width = metrics.widthPixels

            val newWidth = (width * 0.75).toInt()

            // Imposta la larghezza all'85% dello schermo e l'altezza su WRAP_CONTENT
            window.setLayout(newWidth, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogDishDescriptionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Imposta il testo della descrizione
        binding.textViewDialogTitle.text = args.dishName
        binding.textViewDialogDescription.text = args.dishDescription

        // Chiudi il dialog al click del pulsante
        binding.buttonDialogClose.setOnClickListener {
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}