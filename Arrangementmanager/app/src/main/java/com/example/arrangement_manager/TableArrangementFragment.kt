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
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import androidx.navigation.fragment.navArgs

class TableArrangementFragment : Fragment(), OnTableUpdatedListener, OnTableClickedListener {
    private val args: TableArrangementFragmentArgs by navArgs()
    private lateinit var tableArrangementView: TableArrangementView
    private lateinit var addTableButton: Button
    private lateinit var editModeButton: Button
    private lateinit var doneButton: Button
    private lateinit var deleteButton: Button
    private lateinit var addMenuButton: Button

    private var isEditMode = false

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
        addTableButton = view.findViewById(R.id.addButton)
        doneButton = view.findViewById(R.id.doneButton)
        deleteButton = view.findViewById(R.id.deleteButton)
        addMenuButton = view.findViewById(R.id.addMenu)
        editModeButton = view.findViewById(R.id.editButton)

        // Impostazione del fragment stesso come listener per gli aggiornamenti
        tableArrangementView.onTableUpdatedListener = this
        tableArrangementView.onTableClickedListener = this

        // Ogni volta che la lista cambia la vista viene ridisegnata
        viewLifecycleOwner.lifecycleScope.launch {
            tableViewModel.tables.collectLatest { tables ->
                tableArrangementView.setTables(tables)
            }
        }

        // Pulsante Add Table
        addTableButton.setOnClickListener {
            if(isEditMode)
                tableViewModel.addTable()
            else
                Toast.makeText(requireContext(), "Edit mode needed", Toast.LENGTH_SHORT).show()
        }

        deleteButton.setOnClickListener {
            if(isEditMode)
                if(tableArrangementView.getSelectedTable() == null) {
                    Toast.makeText(requireContext(), "Select a table to delete", Toast.LENGTH_SHORT).show()
                } else {
                    tableArrangementView.getSelectedTable()?.let { table ->
                    tableViewModel.deleteTable(table)
                    tableArrangementView.clearSelection()
                    }
                }
            else
                Toast.makeText(requireContext(), "Edit mode needed", Toast.LENGTH_SHORT).show()
        }

        editModeButton.setOnClickListener {
            setEditMode(true)
        }
        doneButton.setOnClickListener {
            setEditMode(false)
        }
        addMenuButton.setOnClickListener {
            val action = TableArrangementFragmentDirections.actionTableArrangementFragmentToAddMenuDialogFragment(args.userEmail)
            findNavController().navigate(action)
        }
    }

    private fun setEditMode(enable: Boolean) {
        isEditMode = enable
        tableArrangementView.setEditMode(enable)

        if(isEditMode) {
            addTableButton.visibility = View.VISIBLE
            editModeButton.visibility = View.GONE
            doneButton.visibility = View.VISIBLE
            deleteButton.visibility = View.VISIBLE
            addMenuButton.visibility = View.GONE
        } else {
            tableArrangementView.clearSelection()
            addTableButton.visibility = View.GONE
            editModeButton.visibility = View.VISIBLE
            doneButton.visibility = View.GONE
            deleteButton.visibility = View.GONE
            addMenuButton.visibility = View.VISIBLE
        }
    }

    // Implementazione del metodo di callback
    override fun onTableUpdated(table: Table_) {
        // Aggiorna il tavolo nel ViewModel ogni volta che viene spostato o
        // ridimensionato
        tableViewModel.updateTable(table)
    }

    override fun onTableClicked(table: Table_) {

        // Naviga al DialogFragment passando gli argomenti con Safe Args
        val action = TableArrangementFragmentDirections.actionTableArrangementFragmentToMenuOrderDialogFragment(table, args.userEmail)
        findNavController().navigate(action)
    }
}