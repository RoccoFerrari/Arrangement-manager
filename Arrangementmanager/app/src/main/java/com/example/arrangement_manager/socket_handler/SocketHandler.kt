package com.example.arrangement_manager.socket_handler

import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.joinAll
import java.net.URISyntaxException

object SocketHandler {
    lateinit var mSocket: Socket

    @Synchronized
    fun setSocket(userId: String) {
        try {
            val uri = "https://arrangement-manager.roccoferrari.com"

            val options = IO.Options().apply {

            }
            mSocket = IO.socket(uri, options)
        } catch (e: URISyntaxException) {
            e.printStackTrace()
        }
    }

    @Synchronized
    fun establishConnection(userId: String) {
        mSocket.connect()

        mSocket.on(Socket.EVENT_CONNECT) {
            val json = org.json.JSONObject()
            json.put("userId", userId)
            mSocket.emit("join_restaurant", json)
        }
    }

    @Synchronized
    fun closeConnection() {
        if (mSocket.connected()) {
            mSocket.disconnect()
        }
    }
}