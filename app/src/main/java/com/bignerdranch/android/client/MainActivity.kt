package com.bignerdranch.android.client

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bignerdranch.android.client.ui.theme.ClientTheme
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.ws
import io.ktor.util.InternalAPI
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch



class MainActivity : ComponentActivity() {

    private lateinit var CAS: CustomAccessibilityService

    @OptIn(InternalAPI::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ClientTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        StartButton(this@MainActivity)
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
    }

    private fun connectToWebSocket() {
        GlobalScope.launch(Dispatchers.IO) {
            val client = HttpClient(CIO) {
                install(WebSockets)
            }

            client.ws(host = "192.168.0.102", port = 8080, path = "/ws") {
                val responseText = "OOOOOOOOOOOOOOOO"
                send(Frame.Text(responseText))
                for (frame in incoming) {
                    frame as? Frame.Text ?: continue
                    val receivedText = frame.readText()
                    Log.e("Received", receivedText)

                    val params = receivedText.split(".")
                    val param1 = params[0].toIntOrNull() ?: 0
                    val param2 = params.getOrElse(1) { "0" }.toIntOrNull() ?: 0

                    val event = AccessibilityEvent.obtain()
                    event.eventType = AccessibilityEvent.TYPE_ANNOUNCEMENT
                    event.text.add(param1.toString() + "." + param2.toString())

                CAS.onAccessibilityEvent(event)
                }


            }
        }
    }

    fun sendCustomEvent(context: Context, message: String) {
        val intent = Intent(CUSTOM_EVENT_ACTION)
        intent.putExtra("message", message)
        context.sendBroadcast(intent)
    }


    @Composable
    fun StartButton(activity: ComponentActivity) {
        Button(
            onClick = {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("http://www.google.com"))
                activity.startActivity(intent)
                CAS = CustomAccessibilityService()
                // Запуск функции для подключения к веб-сокету

                val handler = Handler(Looper.getMainLooper())

                val event = AccessibilityEvent.obtain()
                event.eventType = AccessibilityEvent.TYPE_ANNOUNCEMENT
                event.text.add(1.toString() + "." + 20.toString())
                var runnable = object : Runnable {
                    override fun run() {
                        sendCustomEvent(applicationContext, "0.20")

                        // Запланировать выполнение через 10 секунд
                        handler.postDelayed(this, 1000)
                    }
                }
                runnable.run()
            },
            modifier = Modifier.padding(16.dp)
        ) {
            Text(text = "Start Google Chrome")
        }
    }
}




@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    ClientTheme {
        Greeting("Android")
    }
}

