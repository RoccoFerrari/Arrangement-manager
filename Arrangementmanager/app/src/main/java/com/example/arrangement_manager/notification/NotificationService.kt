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
import com.example.arrangement_manager.socket_handler.SocketHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject
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
    companion object {
        const val FOREGROUND_CHANNEL_ID = "ForegroundServiceChannel"
        const val FOREGROUND_NOTIFICATION_ID = 101
        const val ORDER_CHANNEL_ID = "order_channel" // ID stabile per le notifiche ordini
    }

    override fun onCreate() {
        super.onCreate()
        setupSocketListener()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()
        val notification = createForegroundNotification()

        startForeground(FOREGROUND_NOTIFICATION_ID, notification)

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    /**
     * Collega l'evento Socket.IO 'waiter_notification' alla UI locale
     */
    private fun setupSocketListener() {
        val socket = SocketHandler.getSocket()
        if (socket == null) {
            Log.e("NotificationService", "Socket is null, cannot listen for notifications")
            return
        }

        socket.off("waiter_notification")

        socket.on("waiter_notification") { args ->
            if (args.isNotEmpty()) {
                try {
                    val data = args[0] as JSONObject
                    val message = data.optString("message")

                    Log.d("NotificationService", "Notification received: $message")
                    showOrderReadyNotification(message)

                } catch (e: Exception) {
                    Log.e("NotificationService", "Error parsing notification: ${e.message}")
                }
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                FOREGROUND_CHANNEL_ID,
                "Order Listener Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val orderChannel = NotificationChannel(
                ORDER_CHANNEL_ID,
                "Order Notifications",
                NotificationManager.IMPORTANCE_HIGH
            )

            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
            manager.createNotificationChannel(orderChannel)
        }
    }

    private fun createForegroundNotification(): Notification {
        return NotificationCompat.Builder(this, FOREGROUND_CHANNEL_ID)
            .setContentTitle("Arrangement Manager")
            .setContentText("Connected to kitchen...")
            .setSmallIcon(R.drawable.arrangement_manager)
            .build()
    }

    private fun showOrderReadyNotification(message: String) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            Log.w("NotificationService", "Missing notification permission")
            return
        }

        val notificationManager = NotificationManagerCompat.from(this)

        val builder = NotificationCompat.Builder(this, ORDER_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Order Update!")
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        // System.currentTimeMillis().toInt(): id
        notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
        vibrateDevice()
    }

    private fun vibrateDevice() {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            manager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
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
        // Importante: stacca il listener quando il servizio muore per evitare memory leak o crash
        SocketHandler.getSocket()?.off("waiter_notification")
    }
}