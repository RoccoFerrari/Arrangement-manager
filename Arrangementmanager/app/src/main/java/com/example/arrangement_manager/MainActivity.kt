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

        if (sessionManager.isLoggedIn()) {
            val userId = sessionManager.getUserEmail()

            if (userId != null) {
                Log.d("MainActivity", "User logged in: $userId - Initializing Socket...")

                SocketHandler.setSocket(userId)
                SocketHandler.establishConnection()
            }
        }
    }
}