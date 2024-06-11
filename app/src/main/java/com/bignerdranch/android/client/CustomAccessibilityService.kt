package com.bignerdranch.android.client

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Path
import android.os.Build
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

// Определение уникального действия для вашего события
const val CUSTOM_EVENT_ACTION = "com.bignerdranch.android.client.CUSTOM_EVENT"

class CustomAccessibilityService(
) : AccessibilityService() {

    private lateinit var customEventReceiver: BroadcastReceiver
    override fun onCreate() {
        super.onCreate()
        // Регистрация ресивера для пользовательских событий
        customEventReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == CUSTOM_EVENT_ACTION) {
                    Log.e("Gesture", "fuck")
                    val gestureParamsJson = intent.getStringExtra("gestureParams")
                    val gestureParams = Json.decodeFromString<MainActivity.GestureParams>(gestureParamsJson!!)
                    handleCustomEvent(gestureParams)
                }
            }
        }
        val filter = IntentFilter(CUSTOM_EVENT_ACTION)
        registerReceiver(customEventReceiver, filter)
    }

    private fun handleCustomEvent(gestureParams: MainActivity.GestureParams) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val displayMetrics = applicationContext.resources.displayMetrics
            val middleXValue = displayMetrics.widthPixels / 2
            val middleYValue = displayMetrics.heightPixels / 2
            val gestureBuilder = GestureDescription.Builder()
            val path = Path()

            val startY: Float
            val endY: Float

            if (gestureParams.direction == 0) {
                // Swipe up
                startY = (middleYValue + (gestureParams.distance / 2)).toFloat()
                endY = (middleYValue - (gestureParams.distance / 2)).toFloat()
            } else {
                // Swipe down
                startY = (middleYValue - (gestureParams.distance / 2)).toFloat()
                endY = (middleYValue + (gestureParams.distance / 2)).toFloat()
            }

            path.moveTo(middleXValue.toFloat(), startY)
            path.lineTo(middleXValue.toFloat(), endY)

            gestureBuilder.addStroke(GestureDescription.StrokeDescription(path, 0, 50))
            dispatchGesture(gestureBuilder.build(), object : GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription) {
                    Log.e("Gesture", "onCompleted")
                    super.onCompleted(gestureDescription)
                }

                override fun onCancelled(gestureDescription: GestureDescription?) {
                    Log.e("Gesture", "OnCancelled")
                    super.onCancelled(gestureDescription)
                }
            }, null)
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        ;
    }

    override fun onInterrupt() {
        ;
    }
}