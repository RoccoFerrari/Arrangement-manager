package com.example.arrangement_manager

import TableArrangementViewModelFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import androidx.navigation.fragment.navArgs

class TableArrangementFragment : Fragment(), OnTableUpdatedListener {
    private val args: TableArrangementFragmentArgs by navArgs()
    private lateinit var tableArrangementView: TableArrangementView
    private lateinit var addButton: Button

    private val loginViewModel: LoginViewModel by activityViewModels {
        LoginViewModelFactory(
            requireActivity().application,
            (requireActivity().application as YourApplicationClass).arrangementDAO
        )
    }

    // Questo ViewModel gestisce la logica e i dati specifici dei tavoli.
    private val tableViewModel: TableArrangementViewModel by viewModels {
        TableArrangementViewModelFactory(
            (requireActivity().application as YourApplicationClass).arrangementDAO,
            args.userEmail
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_table_arrangement, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Ottieni i riferimenti agli elementi UI
        tableArrangementView = view.findViewById(R.id.tableArrangementView)
        addButton = view.findViewById(R.id.addButton)

        // Impostazione del fragment stesso come listener per gli aggiornamenti
        tableArrangementView.onTableUpdatedListener = this

        // Ogni volta che la lista cambia la vista viene ridisegnata
        viewLifecycleOwner.lifecycleScope.launch {
            tableViewModel.tables.collectLatest { tables ->
                tableArrangementView.setTables(tables)
            }
        }

        // Quando l'utente è loggato, carica i tavoli specifici per quell'utente.
//        viewLifecycleOwner.lifecycleScope.launch {
//            loginViewModel.userSessionState.collectLatest { state ->
//                if (state.isLoggedIn && state.email != null) {
//                    // Carica i tavoli associati all'utente corrente.
//                    tableViewModel.loadTablesForUser()
//                } else {
//                    Toast.makeText(requireContext(), "Utente non loggato, impossibile caricare tavoli.", Toast.LENGTH_SHORT).show()
//                }
//            }
//        }

        // Pulsante Add Table
        addButton.setOnClickListener {
            tableViewModel.addTable()
            // Recupera l'email dell'utente loggato dal LoginViewModel.
//            val currentUserId = loginViewModel.userSessionState.value.email
//            if (currentUserId != null) {
//                // Chiama il metodo del TableArrangementViewModel per aggiungere un nuovo tavolo
//                tableViewModel.addTable()
//            } else {
//                Toast.makeText(requireContext(), "Devi essere loggato per aggiungere tavoli.", Toast.LENGTH_SHORT).show()
//            }
        }
    }

    // Implementazione del metodo di callback
    override fun onTableUpdated(table: Table_) {
        // Aggiorna il tavolo nel ViewModel ogni volta che viene spostato o
        // ridimensionato
        tableViewModel.updateTable(table)
    }
}