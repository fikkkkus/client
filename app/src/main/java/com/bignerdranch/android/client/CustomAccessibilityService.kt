package com.bignerdranch.android.client

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Path
import android.os.Build
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.bignerdranch.android.client.MainActivity.Companion.GESTURE_PARAMS_RECEIVED
import com.bignerdranch.android.client.websocketclient.GestureParams
import com.bignerdranch.android.client.websocketclient.Request
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json


const val GESTURE_RESULT_ACTION = "com.bignerdranch.android.client.GESTURE_RESULT_ACTION"

class CustomAccessibilityService : AccessibilityService() {

    private lateinit var customEventReceiver: BroadcastReceiver
    private var isGoogleAppActive = false

    override fun onCreate() {
        super.onCreate()
        customEventReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == GESTURE_PARAMS_RECEIVED) {
                    val gestureParamsJson = intent.getStringExtra("gestureParams")
                    val gestureParams = Json.decodeFromString<GestureParams>(gestureParamsJson!!)
                    if (isGoogleAppActive) {
                        handleCustomEvent(gestureParams)
                    }
                    else{
                        sendGestureResult(gestureParams.requestId, 2)
                    }
                }
            }
        }
        val filter = IntentFilter(GESTURE_PARAMS_RECEIVED)
        registerReceiver(customEventReceiver, filter)
    }

    private fun handleCustomEvent(gestureParams: GestureParams) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val displayMetrics = applicationContext.resources.displayMetrics
            val middleXValue = displayMetrics.widthPixels / 2
            val middleYValue = displayMetrics.heightPixels / 2
            val gestureBuilder = GestureDescription.Builder()
            val path = Path()

            val startY: Float
            val endY: Float

            if (gestureParams.direction == 0) {
                startY = (middleYValue + (gestureParams.distance / 2)).toFloat()
                endY = (middleYValue - (gestureParams.distance / 2)).toFloat()
            } else {
                startY = (middleYValue - (gestureParams.distance / 2)).toFloat()
                endY = (middleYValue + (gestureParams.distance / 2)).toFloat()
            }

            path.moveTo(middleXValue.toFloat(), startY)
            path.lineTo(middleXValue.toFloat(), endY)

            gestureBuilder.addStroke(GestureDescription.StrokeDescription(path, 0, 50))
            dispatchGesture(gestureBuilder.build(), object : GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription) {
                    Log.e("Gesture", "onCompleted")
                    sendGestureResult(gestureParams.requestId, 1)
                    super.onCompleted(gestureDescription)
                }

                override fun onCancelled(gestureDescription: GestureDescription?) {
                    Log.e("Gesture", "OnCancelled")
                    sendGestureResult(gestureParams.requestId, 2)
                    super.onCancelled(gestureDescription)
                }
            }, null)
        }
    }

    private fun sendGestureResult(requestId: Long, status: Int) {
        val request = Request(requestId, status)
        val requestJson = Json.encodeToString(request)
        val intent = Intent(GESTURE_RESULT_ACTION).apply {
            putExtra("requestResult", requestJson)
        }
        sendBroadcast(intent)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event!!.eventType === AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {

            val packageName = if (event!!.packageName != null) event!!.packageName.toString() else ""

            isGoogleAppActive = packageName.contains("com.android.chrome")
        }
    }

    override fun onInterrupt() {
        ;
    }

    companion object {
        fun isAccessibilityServiceEnabled(context: Context, service: Class<out AccessibilityService>): Boolean {
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
    }

}
