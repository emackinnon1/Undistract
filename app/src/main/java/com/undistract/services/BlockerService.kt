package com.undistract.services

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager

class BlockerService : Service() {
    companion object {
        private const val TAG = "BlockerService"
        const val ACTION_START_BLOCKING = "com.undistract.START_BLOCKING"
        const val ACTION_STOP_BLOCKING = "com.undistract.STOP_BLOCKING"
        const val EXTRA_APP_PACKAGES = "app_packages"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            when (it.action) {
                ACTION_START_BLOCKING -> {
                    val packages = it.getStringArrayListExtra(EXTRA_APP_PACKAGES) ?: arrayListOf()
                    updateBlockingState(packages, true)
                }
                ACTION_STOP_BLOCKING -> updateBlockingState(arrayListOf(), false)
            }
        }
        return START_STICKY
    }

    private fun updateBlockingState(packages: ArrayList<String>, isBlocking: Boolean) {
        val action = if (isBlocking) "Started blocking" else "Stopped blocking"
        Log.d(TAG, "$action apps: ${if (isBlocking) packages else "all"}")

        LocalBroadcastManager.getInstance(this).sendBroadcast(
            Intent(AppBlockerAccessibilityService.ACTION_UPDATE_BLOCKED_APPS).apply {
                putStringArrayListExtra(AppBlockerAccessibilityService.EXTRA_APP_PACKAGES, packages)
                putExtra(AppBlockerAccessibilityService.EXTRA_IS_BLOCKING, isBlocking)
            }
        )
    }
}
