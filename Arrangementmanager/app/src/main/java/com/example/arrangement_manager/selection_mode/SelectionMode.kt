package com.example.arrangement_manager.selection_mode

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.arrangement_manager.R

class SelectionMode : Fragment() {

    private val args: SelectionModeArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_selection_mode, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val navController = findNavController()
        val ordersButton = view.findViewById<Button>(R.id.selection_mode_orders_button)
        val kitchenButton = view.findViewById<Button>(R.id.selection_mode_kitchen_button)

        ordersButton.setOnClickListener {
            // Retrieve the email passed from the previous fragment
            val userEmail = args.userEmail

            // Create the navigation action and pass the email as the argument
            val action = SelectionModeDirections.actionSelectionModeToTableArrangementFragment(userEmail)
            navController.navigate(action)
        }

        kitchenButton.setOnClickListener {
            navController.navigate(R.id.action_selectionMode_to_kitchenActivity)
        }
    }
}
