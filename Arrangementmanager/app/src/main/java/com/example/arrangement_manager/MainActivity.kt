package com.example.arrangement_manager

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.arrangement_manager.login.SessionManager
import com.example.arrangement_manager.socket_handler.SocketHandler
import org.json.JSONObject

class MainActivity : AppCompatActivity() {

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                // Permission granted. Can send notifications
            } else {
                // Permission denied
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        askNotificationPermission()

        setupSocketConnection()
    }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PermissionChecker.PERMISSION_GRANTED
            ) {
                // Permission has already been granted previously
            } else {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun setupSocketConnection() {
        val sessionManager = SessionManager(this)

        // Check if the user is logged in
        if (sessionManager.isLoggedIn()) {
            val userId = sessionManager.getUserEmail()

            if (userId != null) {
                Log.d("MainActivity", "User logged in: $userId - Initializing Socket...")

                // socket connection setup
                SocketHandler.setSocket(userId)
                SocketHandler.establishConnection()

                // start connecting to the server
                val socket = SocketHandler.getSocket()

                // Remove the previous listener if it exists
                socket?.off("waiter_notification")

                socket?.on("waiter_notification") { args ->
                    if (args.isNotEmpty()) {
                        try {
                            val data = args[0] as JSONObject
                            val message = data.optString("message")
                            val tableId = data.optString("tableId")

                            // I callback del socket sono in background, dobbiamo usare runOnUiThread per la UI
                            runOnUiThread {
                                Toast.makeText(
                                    this,
                                    "🔔 Kitchen: $message",
                                    Toast.LENGTH_LONG
                                ).show()

                                // Vibration

                            }
                        } catch (e: Exception) {
                            Log.e("MainActivity", "Error in parsing notification: ${e.message}")
                        }
                    }
                }
            }
        }
    }
}