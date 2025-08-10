package com.example.arrangement_manager.table_arrangement

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import androidx.navigation.fragment.navArgs
import com.example.arrangement_manager.R
import com.example.arrangement_manager.notification.NotificationService
import com.example.arrangement_manager.retrofit.Table

class TableArrangementFragment : Fragment(), OnTableUpdatedListener, OnTableClickedListener {
    private val args: TableArrangementFragmentArgs by navArgs()
    private lateinit var tableArrangementView: TableArrangementView
    private lateinit var addTableButton: Button
    private lateinit var editModeButton: Button
    private lateinit var doneButton: Button
    private lateinit var deleteButton: Button
    private lateinit var addMenuButton: Button

    private var isEditMode = false

    // This ViewModel handles table-specific logic and data.
    private val tableViewModel: TableArrangementViewModel by viewModels {
        TableArrangementViewModelFactory(
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

        val serviceIntent = Intent(requireContext(), NotificationService::class.java)
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ContextCompat.startForegroundService(requireContext(), serviceIntent)
        } else {
            requireContext().startService(serviceIntent)
        }

        // Get references to UI elements
        tableArrangementView = view.findViewById(R.id.tableArrangementView)
        addTableButton = view.findViewById(R.id.addButton)
        doneButton = view.findViewById(R.id.doneButton)
        deleteButton = view.findViewById(R.id.deleteButton)
        addMenuButton = view.findViewById(R.id.addMenu)
        editModeButton = view.findViewById(R.id.editButton)

        // Setting the fragment itself as a listener for updates
        tableArrangementView.onTableUpdatedListener = this
        tableArrangementView.onTableClickedListener = this

        // Every time the list changes the view is redrawn
        viewLifecycleOwner.lifecycleScope.launch {
            tableViewModel.uiState.collectLatest { uiState ->
                tableArrangementView.setTables(uiState.tables)
            }
        }

        // Add Table button
        addTableButton.setOnClickListener {
            if(isEditMode)
                tableViewModel.addTable()
            else
                Toast.makeText(requireContext(), "Edit mode needed", Toast.LENGTH_SHORT).show()
        }

        // Delete Table button
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

        // Edit Mode button
        editModeButton.setOnClickListener {
            setEditMode(true)
        }
        // Done button
        doneButton.setOnClickListener {
            setEditMode(false)
        }
        // Add Menu button
        addMenuButton.setOnClickListener {
            val action = TableArrangementFragmentDirections.actionTableArrangementFragmentToAddMenuDialogFragment(args.userEmail)
            findNavController().navigate(action)
        }
    }

    // Set the edit mode on or off
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

    // Implementing the callback method
    override fun onTableUpdated(table: Table) {
        // Updates the table in the ViewModel whenever it is moved or resized
        tableViewModel.updateTable(table)
    }

    // Implementing the callback method
    override fun onTableClicked(table: Table) {
        val action = TableArrangementFragmentDirections.actionTableArrangementFragmentToMenuOrderDialogFragment(table, args.userEmail)
        findNavController().navigate(action)
    }
}