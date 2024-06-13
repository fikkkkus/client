package com.bignerdranch.android.client.websocketclient

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import com.bignerdranch.android.client.ConnectionPreferences
import com.bignerdranch.android.client.GESTURE_RESULT_ACTION
import com.bignerdranch.android.client.MainActivity
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.ws
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.IOException
import java.net.ConnectException

object WebSocketClient {
    private lateinit var resultReceiver: BroadcastReceiver
    private var applicationContext: Context? = null
    private lateinit var client: HttpClient
    private var webSocketSession: DefaultClientWebSocketSession? = null
    private var callback: ClientCallback? = null

    fun initialize(context: Context, callback: ClientCallback) {
        client = HttpClient(CIO) {
            install(WebSockets)
        }
        this.callback = callback
        applicationContext = context

        resultReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == GESTURE_RESULT_ACTION) {
                    val requestResultJson = intent.getStringExtra("requestResult")
                    GlobalScope.launch(Dispatchers.IO) {
                        sendResultToServer(requestResultJson!!)
                    }
                }
            }
        }
        registerReceiver()
    }

    private fun registerReceiver() {
        val filter = IntentFilter(GESTURE_RESULT_ACTION)
        applicationContext?.registerReceiver(resultReceiver, filter)
    }

    fun connect() {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                client.ws(
                    host = ConnectionPreferences.getIp(applicationContext!!),
                    port = ConnectionPreferences.getPort(applicationContext!!).toInt(),
                    path = "/ws"
                ) {
                    webSocketSession = this
                    val responseText = "Google Chrome is running"
                    send(Frame.Text(responseText))

                    for (frame in incoming) {
                        frame as? Frame.Text ?: continue
                        val gestureParams = frame.readText()
                        Log.e("Received", gestureParams)
                        broadcastGestureParams(gestureParams)
                    }
                }

                callback?.onConnectionLost()
            } catch (e: Exception) {
                callback?.onConnectionFailed()
            }
        }
    }

    fun disconnect() {
        GlobalScope.launch(Dispatchers.IO) {
            webSocketSession?.close()
            webSocketSession = null
        }
    }

    fun sendResultToServer(json: String) {
        GlobalScope.launch(Dispatchers.IO) {
            webSocketSession?.send(Frame.Text(json))
        }
    }

    private fun broadcastGestureParams(gestureParams: String) {
        val intent = Intent(MainActivity.GESTURE_PARAMS_RECEIVED).apply {
            putExtra("gestureParams", gestureParams)
        }
        applicationContext?.sendBroadcast(intent)
    }


    interface ClientCallback {
        fun onConnectionFailed()
        fun onConnectionLost()
    }
}
