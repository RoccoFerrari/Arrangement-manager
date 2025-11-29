package com.example.arrangement_manager.socket_handler

import android.util.Log
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.joinAll
import java.net.URISyntaxException

object SocketHandler {
    private var mSocket: Socket? = null
    private const val TAG = "SocketHandler"
    private const val SERVER_URL = "https://arrangement-manager.roccoferrari.com"

    @Synchronized
    fun setSocket(userId: String) {
        try {
            if (mSocket != null && mSocket!!.connected()) {
                return
            }
            // Reconnection
            val options = IO.Options().apply {
                reconnection = true
                reconnectionAttempts = Int.MAX_VALUE
                reconnectionDelay = 1000
                reconnectionDelayMax = 5000
                timeout = 20000
            }
            mSocket = IO.socket(SERVER_URL, options)
            setupLifecycleEvents(userId)

        } catch (e: URISyntaxException) {
            e.printStackTrace()
            Log.e(TAG, "Sintax error in URI Socket: ${e.message}")
        }
    }

    private fun setupLifecycleEvents(userId: String) {
        mSocket?.on(Socket.EVENT_CONNECT) {
            Log.d(TAG, "Socket connected. ID: ${mSocket?.id()}")

            val json = org.json.JSONObject()
            json.put("userId", userId)
            mSocket?.emit("join_restaurant", json)
        }

        mSocket?.on(Socket.EVENT_DISCONNECT) {
            Log.d(TAG, "Socket disconnected")
        }

        mSocket?.on(Socket.EVENT_CONNECT_ERROR) { args ->
            Log.e(TAG, "Socket connection error: ${args[0]}")
        }
    }

    @Synchronized
    fun establishConnection() {
        mSocket?.connect()
    }

    @Synchronized
    fun closeConnection() {
        mSocket?.disconnect()
    }

    @Synchronized
    fun getSocket(): Socket? {
        return mSocket
    }
}