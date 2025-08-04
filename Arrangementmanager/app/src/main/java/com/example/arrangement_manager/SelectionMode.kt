package com.example.arrangement_manager

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs

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
            // Recupera l'email passata dal fragment precedente
            val userEmail = args.userEmail

            // Crea l'azione di navigazione e passa l'email come argomento
            val action = SelectionModeDirections.actionSelectionModeToTableArrangementFragment(userEmail)
            navController.navigate(action)
        }

        kitchenButton.setOnClickListener {
            // navController.navigate(R.id.action_selectionMode_to_kitchen)
        }
    }
}
