package com.bignerdranch.android.client


import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bignerdranch.android.client.CustomAccessibilityService.Companion.isAccessibilityServiceEnabled
import com.bignerdranch.android.client.ui.theme.ClientTheme
import com.bignerdranch.android.client.websocketclient.WebSocketClient
import com.google.common.net.InetAddresses

class MainActivity : ComponentActivity() , WebSocketClient.ClientCallback {

    var isPlaying by mutableStateOf(false)

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WebSocketClient.initialize(context = applicationContext, callback = this)

        setContent {
                setUI()
            }
        }

    override fun onConnectionLost() {
        runOnUiThread {
            isPlaying = !isPlaying
            Toast.makeText(this, "Соединение с сервером разорвано.", Toast.LENGTH_LONG).show()
        }
    }
    override fun onConnectionFailed() {
        runOnUiThread {
            isPlaying = !isPlaying
            Toast.makeText(this, "Сервер не отвечает. Проверьте IP и порт", Toast.LENGTH_LONG).show()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Composable
    fun setUI() {
        ClientTheme {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    AnimatedPlayStopButton(
                        modifier = Modifier.align(Alignment.Center),
                        onStart = { WebSocketClient.connect() },
                        onStop = { WebSocketClient.disconnect() },
                        activity = this@MainActivity
                    )
                    ControlButtons(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth(),
                    )
                }
            }
        }
    }


    @RequiresApi(Build.VERSION_CODES.O)
    @Composable
    fun ControlButtons(modifier: Modifier = Modifier) {
        var showDialog by remember { mutableStateOf(false) }

        Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.Center,
        ) {
            ConfigButton(onClick = { showDialog = true }, enabled = !isPlaying, modifier = Modifier.wrapContentWidth())
        }

        if (showDialog) {
            ConfigDialog(onDismiss = { showDialog = false })
        }
    }

    @Composable
    fun ConfigButton(onClick: () -> Unit, enabled: Boolean, modifier: Modifier) {
        Button(
            onClick = onClick, enabled = enabled, modifier = modifier
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = rememberVectorPainter(image = Icons.Default.Settings),
                    contentDescription = "Config",
                    modifier = Modifier.size(25.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Config", fontSize = 16.sp)
            }
        }
    }

    @Composable
    fun AnimatedPlayStopButton(modifier: Modifier, onStart: () -> Unit, onStop: () -> Unit, activity: MainActivity)  {
        val configuration = LocalConfiguration.current
        val screenWidth = configuration.screenWidthDp.dp
        val buttonSize = screenWidth / 2
        val triangleSize = buttonSize / 4

        val backgroundColor by animateColorAsState(
            targetValue = if (isPlaying) Color(0xE2E91A1A) else Color(0xFF90EE90),
            animationSpec = tween(durationMillis = 500), label = ""
        )

        Box(
            modifier = modifier
        ) {
            IconButton(
                onClick = {
                    if(!isPlaying) {
                        if (!isAccessibilityServiceEnabled(
                                activity,
                                CustomAccessibilityService::class.java
                            )
                        ) {
                            Toast.makeText(activity, "Включите Accessibility Service для данного приложения", Toast.LENGTH_LONG).show()
                            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                            activity.startActivity(intent)
                        } else {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("http://www.google.com")).apply {
                                setPackage("com.android.chrome")
                            }
                            try {
                                activity.startActivity(intent)
                                onStart()
                                isPlaying = !isPlaying
                            } catch (e: ActivityNotFoundException) {
                                Toast.makeText(applicationContext, "Chrome не установлен", Toast.LENGTH_SHORT).show()
                            }

                        }
                    } else {
                        onStop()
                    }
                },
                modifier = Modifier
                    .size(buttonSize)
                    .clip(CircleShape)
                    .background(backgroundColor)
            ) {
                Box (contentAlignment = Alignment.Center) {
                    Canvas(modifier = Modifier.size(triangleSize)) {
                        if (isPlaying) {
                            drawRect(
                                color = Color.White,
                                size = size,
                            )
                        } else {
                            drawTriangle(
                                Color.White,
                                size.maxDimension
                            )
                        }
                    }
                }
            }
        }
    }

    fun DrawScope.drawTriangle(color: Color, size: Float) {
        val path = Path().apply {
            val yOffset = size / 8
            moveTo(size / 2, 0f - yOffset)
            lineTo(size, size - yOffset)
            lineTo(0f, size - yOffset)
            close()
        }
        rotate(degrees = 90f, pivot = center) {
            drawPath(path, color)
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun ConfigDialog(onDismiss: () -> Unit) {
        var ip by remember { mutableStateOf(TextFieldValue(ConnectionPreferences.getIp(applicationContext))) }
        var port by remember { mutableStateOf(TextFieldValue(ConnectionPreferences.getPort(applicationContext))) }
        val context = LocalContext.current

        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Configuration") },
            text = {
                Column {
                    OutlinedTextField(
                        value = ip,
                        onValueChange = { ip = it },
                        label = { Text("IP Address") },
                        keyboardOptions = KeyboardOptions.Default.copy(
                            keyboardType = KeyboardType.Number
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = port,
                        onValueChange = { port = it },
                        label = { Text("Port") },
                        keyboardOptions = KeyboardOptions.Default.copy(
                            keyboardType = KeyboardType.Number
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF90EE90)
                    ),
                    onClick = {
                        val portValue = port.text.toIntOrNull()
                        if (portValue == null || portValue < 0) {
                            Toast.makeText(context, "Порт это обычно целое неотрицательное число", Toast.LENGTH_SHORT).show()
                        } else if (!InetAddresses.isInetAddress(ip.text)) {
                            Toast.makeText(context, "Некорректный IP адрес", Toast.LENGTH_SHORT).show()
                        } else {
                            ConnectionPreferences.setIp(applicationContext, ip.text)
                            ConnectionPreferences.setPort(applicationContext, port.text)
                            onDismiss()
                        }
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xE2E91A1A)
                    ),
                    onClick = onDismiss
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    companion object {
        const val GESTURE_PARAMS_RECEIVED = "com.bignerdranch.android.client.CUSTOM_EVENT"
    }
}