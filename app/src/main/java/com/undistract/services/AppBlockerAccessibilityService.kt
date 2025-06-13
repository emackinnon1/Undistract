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
import androidx.annotation.VisibleForTesting
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.undistract.R


/**
 * Accessibility service that intercepts and blocks app launches based on a configurable blocklist.
 *
 * This service uses the Android Accessibility API to monitor app launches and prevent
 * access to apps that have been designated as "blocked" in the Undistract application.
 * When a blocked app is detected, the service displays an overlay message and navigates
 * the user back to the home screen.
 */
class AppBlockerAccessibilityService : AccessibilityService() {
    companion object {
        /**
         * Intent action for updating the list of blocked applications
         */
        const val ACTION_UPDATE_BLOCKED_APPS = "com.undistract.UPDATE_BLOCKED_APPS"

        /**
         * Intent extra key for the list of app package names to block
         */
        const val EXTRA_APP_PACKAGES = "app_packages"

        /**
         * Intent extra key for the blocking state flag
         */
        const val EXTRA_IS_BLOCKING = "is_blocking"

        /**
         * Global flag indicating whether app blocking is currently active
         */
        var isBlocking = false

        /**
         * List of package names that should be blocked when launched
         */
        var blockedApps = listOf<String>()

        /**
         * Checks if the accessibility service is enabled and prompts the user to enable it if not.
         *
         * Shows a toast notification instructing the user to enable the service and opens the
         * Accessibility Settings screen.
         *
         * @param context The context used to start the accessibility settings activity
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

        /**
         * Determines if this accessibility service is currently enabled in system settings.
         *
         * @param context Context used to access system settings
         * @return True if the service is enabled, false otherwise
         */
        internal fun isAccessibilityServiceEnabled(context: Context): Boolean {
            val enabledServicesString = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false

            val componentName = ComponentName(context, AppBlockerAccessibilityService::class.java)
            return enabledServicesString.contains(componentName.flattenToString())
        }
    }

    /**
     * Window manager used to display blocking overlays
     */
    private var windowManager: WindowManager? = null

    /**
     * Current overlay view shown when an app is blocked
     */
    private var overlayView: View? = null

    /**
     * Receiver for broadcast messages to update blocked apps list
     */
    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == ACTION_UPDATE_BLOCKED_APPS) {
                blockedApps = intent.getStringArrayListExtra(EXTRA_APP_PACKAGES)?.toList() ?: emptyList()
                isBlocking = intent.getBooleanExtra(EXTRA_IS_BLOCKING, false)
            }
        }
    }

    /**
     * Initializes the service and registers the broadcast receiver
     * for receiving updates to the blocked apps list
     */
    override fun onCreate() {
        super.onCreate()
        LocalBroadcastManager.getInstance(this).registerReceiver(
            broadcastReceiver,
            IntentFilter(ACTION_UPDATE_BLOCKED_APPS)
        )
    }

    /**
     * Cleans up resources when the service is destroyed
     * by unregistering the broadcast receiver and removing any active overlays
     */
    override fun onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver)
        removeOverlay()
        super.onDestroy()
    }

    /**
     * Handles accessibility events to detect and block apps.
     *
     * When a window state change is detected, checks if the launched app
     * is in the blocked list. If it is, shows a blocking overlay and
     * returns the user to the home screen.
     *
     * @param event The accessibility event to process
     */
    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED || !isBlocking) return

        val packageName = event.packageName?.toString() ?: return

        if (blockedApps.contains(packageName)) {
            val appName = getAppName(packageName)
            showBlockedAppOverlay(appName)
            performGlobalAction(GLOBAL_ACTION_HOME)
        }
    }

    /**
     * Retrieves the human-readable application name from a package name.
     *
     * @param packageName The package identifier of the application
     * @return The user-visible name of the application or the package name if not found
     */
    @VisibleForTesting
    internal fun getAppName(packageName: String): String {
        return try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            packageName
        }
    }

    /**
     * Removes the current blocking overlay if one exists.
     * Safe to call even if no overlay is currently displayed.
     */
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

    /**
     * Displays a blocking overlay when a restricted app is launched.
     *
     * Creates and displays a system overlay that informs the user that the
     * requested app is blocked and provides instructions to unblock it.
     *
     * @param appName The name of the app being blocked to display in the message
     */
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

    /**
     * Called when the accessibility service is interrupted.
     * Required by the AccessibilityService class.
     */
    override fun onInterrupt() {
        // Required by AccessibilityService
    }
}