package com.bignerdranch.android.client

import android.accessibilityservice.AccessibilityService
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityManager
import android.widget.Toast
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
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

class MainActivity : ComponentActivity() {

    private lateinit var CAS: CustomAccessibilityService

    @OptIn(InternalAPI::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        CAS = CustomAccessibilityService()

        setContent {
            ClientTheme {
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

    private fun isAccessibilityServiceEnabled(context: Context, service: Class<out AccessibilityService>): Boolean {
        val enabledServicesSetting = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return false
        val colonSplitter = TextUtils.SimpleStringSplitter(':')
        colonSplitter.setString(enabledServicesSetting)
        val serviceComponentName = "${context.packageName}/${service.name}"
        while (colonSplitter.hasNext()) {
            val componentName = colonSplitter.next()
            if (componentName.equals(serviceComponentName, ignoreCase = true)) {
                return true
            }
        }
        return false
    }


    override fun onResume() {
        super.onResume()
    }

    @Serializable
    data class GestureParams(val direction: Int, val distance: Int)

    private fun connectToWebSocket() {
        GlobalScope.launch(Dispatchers.IO) {
            val client = HttpClient(CIO) {
                install(WebSockets)
            }

            client.ws(host = "192.168.0.102", port = 8080, path = "/ws") {
                val responseText = "Google Chrome is running"
                send(Frame.Text(responseText))
                for (frame in incoming) {
                    frame as? Frame.Text ?: continue
                    val gestureParams = frame.readText()
                    Log.e("Received", gestureParams)

                    sendCustomEvent(applicationContext, gestureParams)
                    // Отправить сообщение об успешном выполнении жеста обратно на сервер
                    val report = "Gesture executed successfully with parameters: $gestureParams"
                    send(Frame.Text(report))
                }
            }
        }
    }

    fun sendCustomEvent(context: Context, gestureParams: String) {
        val intent = Intent(CUSTOM_EVENT_ACTION)
        intent.putExtra("gestureParams", gestureParams)
        context.sendBroadcast(intent)
    }

    @Composable
    fun StartButton(activity: ComponentActivity) {
        Button(
            onClick = {
                if (!isAccessibilityServiceEnabled(activity, CustomAccessibilityService::class.java)) {
                    Toast.makeText(activity, "Please enable accessibility service", Toast.LENGTH_LONG).show()
                    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                    activity.startActivity(intent)
                } else {

                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("http://www.google.com"))
                    intent.setPackage("com.android.chrome")

                    try {
                        activity.startActivity(intent)
                        Log.d("MyApp", "Chrome was successfully started")
                        connectToWebSocket()
                    } catch (e: ActivityNotFoundException) {
                        Log.e("MyApp", "Chrome is not installed", e)
                    } catch (e: Exception) {
                        Log.e("MyApp", "Unexpected error occurred while starting Chrome", e)
                    }
                }
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
