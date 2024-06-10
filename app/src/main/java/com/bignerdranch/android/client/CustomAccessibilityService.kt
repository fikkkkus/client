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
                    val message = intent.getStringExtra("message")
                    handleCustomEvent(message)
                }
            }
        }
        val filter = IntentFilter(CUSTOM_EVENT_ACTION)
        registerReceiver(customEventReceiver, filter)
    }

    private fun handleCustomEvent(message: String?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val displayMetrics = applicationContext.resources.displayMetrics
            val middleXValue = displayMetrics.widthPixels / 2
            val topOfScreen = displayMetrics.heightPixels / 4
            val bottomOfScreen = topOfScreen * 3
            val gestureBuilder = GestureDescription.Builder()
            val path = Path()
            if (message != null && message.toString().contains("0")) {
                // Swipe up
                path.moveTo(middleXValue.toFloat(), bottomOfScreen.toFloat())
                path.lineTo(middleXValue.toFloat(), topOfScreen.toFloat())
                Log.e("onAccessibilityEvent", "onAccessibilityEvent3")
            } else {
                Log.e("onAccessibilityEvent", "onAccessibilityEvent3")
                // Swipe down
                path.moveTo(middleXValue.toFloat(), topOfScreen.toFloat())
                path.lineTo(middleXValue.toFloat(), bottomOfScreen.toFloat())
            }
            Log.e("onAccessibilityEvente", "onAccessibilityEvent4")
            gestureBuilder.addStroke(GestureDescription.StrokeDescription(path, 100, 50))
            //val accessibilityService = context.getSystemService(AccessibilityService::class.java)
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

    }
}