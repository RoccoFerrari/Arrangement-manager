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

class NotificationService : Service() {
    private val notificationPort = 6001
    private var serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    // Constants for foreground service notification
    companion object {
        const val FOREGROUND_CHANNEL_ID = "ForegroundServiceChannel"
        const val FOREGROUND_NOTIFICATION_ID = 101
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()
        val notification = createForegroundNotification()

        startForeground(FOREGROUND_NOTIFICATION_ID, notification)

        startListening()

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                FOREGROUND_CHANNEL_ID,
                "Canale Servizio in Primo Piano",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java) as NotificationManager
            manager.createNotificationChannel(serviceChannel)
        }
    }

    private fun createForegroundNotification(): Notification {
        return NotificationCompat.Builder(this, FOREGROUND_CHANNEL_ID)
            .setContentTitle("Gestore Ordini Attivo")
            .setContentText("In ascolto per nuove notifiche di ordini.")
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Usa l'icona della tua app
            .setPriority(NotificationCompat.PRIORITY_LOW) // Priorità bassa per non disturbare
            .build()
    }

    private fun startListening() {
        serviceScope.launch {
            try {
                val notificationServerSocket = ServerSocket(notificationPort)
                Log.d("DEBUG_NOTIFICATION", "Server in ascolto sulla porta $notificationPort")
                while (isActive) {
                    val clientSocket = notificationServerSocket.accept()
                    val clientIp = clientSocket.inetAddress.hostAddress
                    Log.d(
                        "DEBUG_NOTIFICATION",
                        "Connessione notifica ricevuta da $clientIp"
                    )
                    launch {
                        handleClientNotification(clientSocket)
                    }
                }
            } catch (e: SocketException) {
                Log.d("DEBUG_NOTIFICATION", "Listener chiuso")
            } catch (e: Exception) {
                Log.e("DEBUG_NOTIFICATION", "Errore durante la connessione: ${e.message}")
            }
        }
    }

    private fun handleClientNotification(clientSocket: Socket) {
        val reader = BufferedReader(InputStreamReader(clientSocket.getInputStream()))
        val message = reader.readLine()
        Log.d("DEBUG_NOTIFICATION", "Messaggio ricevuto: $message")

        if(message != null) {
            showOrderReadyNotification(message)
        }
        clientSocket.close()
    }

    fun showOrderReadyNotification(message: String) {
        val context = applicationContext
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        val notificationManager = NotificationManagerCompat.from(context)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel("order_channel", "Notifiche Ordini", NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        val builder = NotificationCompat.Builder(context, "order_channel")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Ordine Pronto!")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        notificationManager.notify(123, builder.build())
        vibrateDevice()
    }

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