package com.example.arrangement_manager.notification

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.arrangement_manager.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException

/**
 * A foreground service that listens for incoming messages on a specific port
 * and displays notifications with a vibration effect.
 *
 * This service runs in the background to ensure it is not killed by the system,
 * allowing it to continuously monitor for new order notifications from the backend.
 */
class NotificationService : Service() {
    // The port number on which the service listens for incoming connections
    private val notificationPort = 6001

    // A coroutine job that can be cancelled
    private var serviceJob = SupervisorJob()

    // The coroutine scope for the service, using the IO dispatcher
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    /**
     * Companion object to hold constants related to the foreground service notification.
     */
    companion object {
        const val FOREGROUND_CHANNEL_ID = "ForegroundServiceChannel"
        const val FOREGROUND_NOTIFICATION_ID = 101
    }

    /**
     * Called when the service is started.
     *
     * This method creates a notification channel, starts the service in the foreground,
     * and begins listening for connections.
     * @return START_STICKY to indicate the system should try to re-create the service if it's killed.
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()
        val notification = createForegroundNotification()

        startForeground(FOREGROUND_NOTIFICATION_ID, notification)

        startListening()

        return START_STICKY
    }

    /**
     * Called when a client binds to the service.
     *
     * This service is not designed to be bound to, so this method returns null.
     */
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    /**
     * Creates a notification channel for Android O and above.
     *
     * This is required for displaying notifications on newer Android versions.
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                FOREGROUND_CHANNEL_ID,
                "Foreground Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java) as NotificationManager
            manager.createNotificationChannel(serviceChannel)
        }
    }

    /**
     * Creates the notification that will be used for the foreground service.
     *
     * This notification is visible to the user and informs them that a background service is running.
     * @return The created [Notification] object.
     */
    private fun createForegroundNotification(): Notification {
        return NotificationCompat.Builder(this, FOREGROUND_CHANNEL_ID)
            .setContentTitle("Active Order Manager")
            .setContentText("Listening for new order notifications.")
            .setSmallIcon(R.drawable.arrangement_manager)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    /**
     * Starts a coroutine to listen for incoming TCP connections.
     *
     * This function runs a server socket on the specified port. When a connection is accepted,
     * it spawns a new coroutine to handle the client communication and receive the notification message.
     */
    private fun startListening() {
        serviceScope.launch {
            try {
                val notificationServerSocket = ServerSocket(notificationPort)
                Log.d("DEBUG_NOTIFICATION", "Server listening on port $notificationPort")
                while (isActive) {
                    val clientSocket = notificationServerSocket.accept()
                    val clientIp = clientSocket.inetAddress.hostAddress
                    Log.d(
                        "DEBUG_NOTIFICATION",
                        "Connection notification received from $clientIp"
                    )
                    launch {
                        handleClientNotification(clientSocket)
                    }
                }
            } catch (e: SocketException) {
                Log.d("DEBUG_NOTIFICATION", "Listener closed")
            } catch (e: Exception) {
                Log.e("DEBUG_NOTIFICATION", "Error while connecting: ${e.message}")
            }
        }
    }

    /**
     * Handles a single client connection.
     *
     * It reads a message from the client socket, displays a notification with the message,
     * and then closes the socket.
     * @param clientSocket The [Socket] representing the client connection.
     */
    private fun handleClientNotification(clientSocket: Socket) {
        val reader = BufferedReader(InputStreamReader(clientSocket.getInputStream()))
        val message = reader.readLine()
        Log.d("DEBUG_NOTIFICATION", "Message received: $message")

        if(message != null) {
            showOrderReadyNotification(message)
        }
        clientSocket.close()
    }

    /**
     * Displays a high-priority notification to the user.
     *
     * This method also checks for the POST_NOTIFICATIONS permission on newer Android versions
     * and triggers a vibration effect.
     * @param message The text to be displayed in the notification.
     */
    fun showOrderReadyNotification(message: String) {
        val context = applicationContext
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        val notificationManager = NotificationManagerCompat.from(context)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel("order_channel", "Order Notifications", NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        val builder = NotificationCompat.Builder(context, "order_channel")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Order Ready!")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        notificationManager.notify(123, builder.build())
        vibrateDevice()
    }

    /**
     * Triggers a one-shot vibration on the device.
     *
     * It handles different Android versions to ensure compatibility.
     */
    private fun vibrateDevice() {
        val context = applicationContext
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }

        vibrator?.let {
            if (it.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    it.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    it.vibrate(500)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
    }
}