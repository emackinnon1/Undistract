package com.undistract.services.accessibility

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.TextView
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.undistract.R

class AppBlockerAccessibilityService : AccessibilityService() {
    companion object {
        const val ACTION_UPDATE_BLOCKED_APPS = "com.undistract.UPDATE_BLOCKED_APPS"
        const val EXTRA_APP_PACKAGES = "app_packages"
        const val EXTRA_IS_BLOCKING = "is_blocking"

        var isBlocking = false
        var blockedApps = listOf<String>()
    }

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                ACTION_UPDATE_BLOCKED_APPS -> {
                    blockedApps = intent.getStringArrayListExtra(EXTRA_APP_PACKAGES)?.toList() ?: listOf()
                    isBlocking = intent.getBooleanExtra(EXTRA_IS_BLOCKING, false)
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        LocalBroadcastManager.getInstance(this).registerReceiver(
            broadcastReceiver,
            IntentFilter(ACTION_UPDATE_BLOCKED_APPS)
        )
    }

    override fun onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver)

        // Remove overlay view if it exists
        overlayView?.let {
            try {
                windowManager?.removeView(it)
            } catch (e: Exception) {
                // View might not be attached
            }
        }

        super.onDestroy()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED && isBlocking) {
            val packageName = event.packageName?.toString() ?: return

            if (blockedApps.contains(packageName)) {
                // Get human-readable app name
                val appName = try {
                    val packageManager = packageManager
                    val appInfo = packageManager.getApplicationInfo(packageName, 0)
                    packageManager.getApplicationLabel(appInfo).toString()
                } catch (e: Exception) {
                    packageName
                }

                // Show overlay with blocked message
                showBlockedAppOverlay(appName)

                // Block the app by going back to home
                performGlobalAction(GLOBAL_ACTION_HOME)
            }
        }
    }

    private fun showBlockedAppOverlay(appName: String) {
        if (windowManager == null) {
            windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        }

        // Remove any existing overlay
        overlayView?.let {
            try {
                windowManager?.removeView(it)
            } catch (e: Exception) {
                // View might not be attached
            }
        }

        // Inflate the blocked app layout
        val inflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        overlayView = inflater.inflate(R.layout.activity_blocked_app, null)

        // Update the message with app name
        overlayView?.findViewById<TextView>(R.id.message)?.text =
            "The app '$appName' is currently blocked by Undistract.\nScan your tag to unblock."

        // Add a close button
        overlayView?.findViewById<View>(R.id.close_button)?.setOnClickListener {
            windowManager?.removeView(overlayView)
            overlayView = null
        }

        // Set window parameters
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        )

        // Show the overlay
        try {
            windowManager?.addView(overlayView, params)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onInterrupt() {
        // Required by AccessibilityService
    }
}