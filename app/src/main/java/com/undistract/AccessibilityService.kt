package com.undistract

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import android.content.IntentFilter
import android.content.BroadcastReceiver
import android.content.Context

class AppBlockerAccessibilityService : AccessibilityService() {
    companion object {
        const val ACTION_UPDATE_BLOCKED_APPS = "com.undistract.UPDATE_BLOCKED_APPS"
        const val EXTRA_APP_PACKAGES = "app_packages"
        const val EXTRA_IS_BLOCKING = "is_blocking"

        var isBlocking = false
        var blockedApps = listOf<String>()
    }

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
        super.onDestroy()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED && isBlocking) {
            val packageName = event.packageName?.toString() ?: return

            if (blockedApps.contains(packageName)) {
                // Show the blocked app message
                val intent = Intent(this, BlockedAppActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    putExtra("app_name", packageName)
                }
                startActivity(intent)

                // Block the app by going back to home
                performGlobalAction(GLOBAL_ACTION_HOME)
            }
        }
    }

    override fun onInterrupt() {
        // Required by AccessibilityService
    }
}