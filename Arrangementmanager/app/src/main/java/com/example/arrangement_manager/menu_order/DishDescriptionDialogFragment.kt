package com.example.arrangement_manager.menu_order

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.navigation.fragment.findNavController
import com.example.arrangement_manager.databinding.DialogDishDescriptionBinding
import androidx.navigation.fragment.navArgs

/**
 * A DialogFragment that displays the name and description of a menu item.
 *
 * It retrieves the dish information from the navigation arguments and sets up the
 * UI to show the details to the user.
 */
class DishDescriptionDialogFragment : DialogFragment() {

    // View binding for the dialog layout
    private var _binding: DialogDishDescriptionBinding? = null
    private val binding get() = _binding!!

    // Retrieves the arguments passed to the Fragment
    private val args: DishDescriptionDialogFragmentArgs by navArgs()

    /**
     * Called when the fragment is visible to the user and the dialog is displayed.
     *
     * This method sets the dialog's width to 75% of the screen width for a better
     * visual presentation.
     */
    override fun onStart() {
        super.onStart()

        dialog?.window?.let { window ->
            val metrics = resources.displayMetrics
            val width = metrics.widthPixels

            val newWidth = (width * 0.75).toInt()

            // Set the width to 85% of the screen and the height to WRAP_CONTENT
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

        // Set the description text
        binding.textViewDialogTitle.text = args.dishName
        binding.textViewDialogDescription.text = args.dishDescription

        binding.buttonDialogModify.setOnClickListener {
            val action = DishDescriptionDialogFragmentDirections.actionDishDescriptionDialogFragmentToModifyDishDialogFragment(
                args.dishName,
                args.dishPrice,
                args.dishQuantity,
                args.dishDescription,
                args.userEmail)
            findNavController().navigate(action)
        }

        // Closes the dialog when the button is clicked
        binding.buttonDialogClose.setOnClickListener {
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}