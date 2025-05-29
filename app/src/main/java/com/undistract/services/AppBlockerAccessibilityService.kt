package com.undistract.services

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.TextView
import android.widget.Toast
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.undistract.R
import androidx.annotation.VisibleForTesting



class AppBlockerAccessibilityService : AccessibilityService() {
    companion object {
        const val ACTION_UPDATE_BLOCKED_APPS = "com.undistract.UPDATE_BLOCKED_APPS"
        const val EXTRA_APP_PACKAGES = "app_packages"
        const val EXTRA_IS_BLOCKING = "is_blocking"

        var isBlocking = false
        var blockedApps = listOf<String>()

        /**
         * Checks if the accessibility service is enabled and prompts the user to enable it if not.
         */
        fun ensureAccessibilityServiceEnabled(context: Context) {
            if (!isAccessibilityServiceEnabled(context)) {
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                Toast.makeText(
                    context, 
                    "Please enable Undistract Accessibility Service",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        // More reliable way to check if the service is enabled
        private fun isAccessibilityServiceEnabled(context: Context): Boolean {
            val enabledServicesString = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false

            val componentName = ComponentName(context, AppBlockerAccessibilityService::class.java)
            return enabledServicesString.contains(componentName.flattenToString())
        }
    }

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == ACTION_UPDATE_BLOCKED_APPS) {
                blockedApps = intent.getStringArrayListExtra(EXTRA_APP_PACKAGES)?.toList() ?: emptyList()
                isBlocking = intent.getBooleanExtra(EXTRA_IS_BLOCKING, false)
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
        removeOverlay()
        super.onDestroy()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED || !isBlocking) return
        
        val packageName = event.packageName?.toString() ?: return
        
        if (blockedApps.contains(packageName)) {
            val appName = getAppName(packageName)
            showBlockedAppOverlay(appName)
            performGlobalAction(GLOBAL_ACTION_HOME)
        }
    }

//    private fun getAppName(packageName: String): String {
//        return try {
//            val appInfo = packageManager.getApplicationInfo(packageName, 0)
//            packageManager.getApplicationLabel(appInfo).toString()
//        } catch (e: Exception) {
//            packageName
//        }
//    }

    @VisibleForTesting
    internal fun getAppName(packageName: String): String {
        return try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            packageName
        }
    }

    @VisibleForTesting
    internal fun removeOverlay() {
        overlayView?.let {
            try {
                windowManager?.removeView(it)
            } catch (e: Exception) {
                // View might not be attached
            }
            overlayView = null
        }
    }

    @VisibleForTesting
    internal fun showBlockedAppOverlay(appName: String) {
        // Initialize window manager if needed
        if (windowManager == null) {
            windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        }

        // Remove existing overlay
        removeOverlay()

        // Create and configure new overlay
        val inflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        overlayView = inflater.inflate(R.layout.activity_blocked_app, null).apply {
            findViewById<TextView>(R.id.message)?.text =
                "The app '$appName' is currently blocked by Undistract.\nScan your tag to unblock."
            
            findViewById<View>(R.id.close_button)?.setOnClickListener {
                removeOverlay()
            }
        }

        // Configure window parameters
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        )

        // Show overlay
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
