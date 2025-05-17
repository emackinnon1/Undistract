//package com.undistract
//
//import android.accessibilityservice.AccessibilityService
//import android.app.Service
//import android.content.Intent
//import android.os.IBinder
//import android.util.Log
//import android.app.ActivityManager
//import android.content.Context
//import android.app.AppOpsManager
//import android.app.usage.UsageStatsManager
//import android.os.Process
//import kotlinx.coroutines.*
//
//class BlockerService : Service() {
//    private val serviceScope = CoroutineScope(Dispatchers.Default)
//    private var isBlocking = false
//    private var appsToBlock = listOf<String>()
//    private var blockingJob: Job? = null
//
//    companion object {
//        const val ACTION_START_BLOCKING = "com.undistract.START_BLOCKING"
//        const val ACTION_STOP_BLOCKING = "com.undistract.STOP_BLOCKING"
//        const val EXTRA_APP_PACKAGES = "app_packages"
//    }
//
//    override fun onBind(intent: Intent?): IBinder? = null
//
//    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
//        when (intent?.action) {
//            ACTION_START_BLOCKING -> {
//                val packages = intent.getStringArrayListExtra(EXTRA_APP_PACKAGES) ?: arrayListOf()
//                startBlocking(packages)
//            }
//            ACTION_STOP_BLOCKING -> {
//                stopBlocking()
//            }
//        }
//        return START_STICKY
//    }
//
//    private fun startBlocking(packages: ArrayList<String>) {
//        isBlocking = true
//        appsToBlock = packages.toList()
//
//        Log.d("BlockerService", "Starting to block apps: $appsToBlock")
//
//        blockingJob = serviceScope.launch {
//            while (isBlocking) {
//                checkAndBlockApps()
//                delay(500) // Check every half second
//            }
//        }
//    }
//
//    private fun stopBlocking() {
//        isBlocking = false
//        blockingJob?.cancel()
//        blockingJob = null
//        Log.d("BlockerService", "Stopped blocking apps")
//    }
//
//    private fun checkAndBlockApps() {
//        val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
//
//        // Get currently running app
//        val tasks = activityManager.getRunningTasks(1)
//        if (tasks.isNotEmpty()) {
//            val topPackage = tasks[0].topActivity?.packageName
//
//            // Check if it's one of our blocked apps
//            if (topPackage != null && appsToBlock.contains(topPackage) &&
//                topPackage != packageName) {
//
//                Log.d("BlockerService", "Blocking app: $topPackage")
//
//                // Close the app
//                val homeIntent = Intent(Intent.ACTION_MAIN).apply {
//                    addCategory(Intent.CATEGORY_HOME)
//                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
//                }
//                startActivity(homeIntent)
//
//                // Optionally show notification that app was blocked
//                showBlockedAppNotification(topPackage)
//            }
//        }
//    }
//
//    private fun showBlockedAppNotification(packageName: String) {
//        // Implement notification to inform user an app was blocked
//        // This would use NotificationManager
//    }
//
//    override fun onDestroy() {
//        blockingJob?.cancel()
//        serviceScope.cancel()
//        super.onDestroy()
//    }
//}
package com.undistract

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