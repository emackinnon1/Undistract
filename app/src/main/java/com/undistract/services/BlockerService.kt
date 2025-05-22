package com.undistract.services

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager

class BlockerService : Service() {
    companion object {
        const val ACTION_START_BLOCKING = "com.undistract.START_BLOCKING"
        const val ACTION_STOP_BLOCKING = "com.undistract.STOP_BLOCKING"
        const val EXTRA_APP_PACKAGES = "app_packages"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_BLOCKING -> {
                val packages = intent.getStringArrayListExtra(EXTRA_APP_PACKAGES) ?: arrayListOf()
                startBlocking(packages)
            }
            ACTION_STOP_BLOCKING -> {
                stopBlocking()
            }
        }
        return START_STICKY
    }

    private fun startBlocking(packages: ArrayList<String>) {
        Log.d("BlockerService", "Starting to block apps: $packages")

        val intent = Intent(AppBlockerAccessibilityService.ACTION_UPDATE_BLOCKED_APPS).apply {
            putStringArrayListExtra(AppBlockerAccessibilityService.EXTRA_APP_PACKAGES, packages)
            putExtra(AppBlockerAccessibilityService.EXTRA_IS_BLOCKING, true)
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    private fun stopBlocking() {
        Log.d("BlockerService", "Stopped blocking apps")

        val intent = Intent(AppBlockerAccessibilityService.ACTION_UPDATE_BLOCKED_APPS).apply {
            putStringArrayListExtra(AppBlockerAccessibilityService.EXTRA_APP_PACKAGES, arrayListOf())
            putExtra(AppBlockerAccessibilityService.EXTRA_IS_BLOCKING, false)
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }
}