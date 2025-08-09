package com.example.arrangement_manager.table_arrangement

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
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

    // Questo ViewModel gestisce la logica e i dati specifici dei tavoli.
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
            tableViewModel.uiState.collectLatest { uiState ->
                tableArrangementView.setTables(uiState.tables)
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
    override fun onTableUpdated(table: Table) {
        // Aggiorna il tavolo nel ViewModel ogni volta che viene spostato o
        // ridimensionato
        tableViewModel.updateTable(table)
    }

    override fun onTableClicked(table: Table) {
        val action = TableArrangementFragmentDirections.actionTableArrangementFragmentToMenuOrderDialogFragment(table, args.userEmail)
        findNavController().navigate(action)
    }
}