package com.undistract

import android.app.Service
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.view.WindowManager
import kotlinx.coroutines.*
import kotlin.coroutines.coroutineContext

class AppBlockingService : Service() {
    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Default + job)

    private lateinit var windowManager: WindowManager
    private lateinit var usageStatsManager: UsageStatsManager

    private var blockedPackages: List<String> = emptyList()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.hasExtra("BLOCKED_APPS") == true) {
            @Suppress("UNCHECKED_CAST")
            blockedPackages = intent.getSerializableExtra("BLOCKED_APPS") as? ArrayList<String> ?: emptyList()

            // We'll implement this fully in a future step
            scope.launch {
                monitorApps()
            }
        }

        return START_STICKY
    }

    private suspend fun monitorApps() {
        while (coroutineContext.isActive) {
            try {
                val currentApp = getCurrentForegroundApp()
                if (blockedPackages.contains(currentApp)) {
                    // Found a blocked app - take action
                    launchBlockerActivity(currentApp)
                }
                delay(500) // Check every half second
            } catch (e: Exception) {
                // Log the error but don't crash the monitoring loop
                e.printStackTrace()
            }
        }
    }

    private fun getCurrentForegroundApp(): String {
        val time = System.currentTimeMillis()
        // Query usage stats for the last few seconds
        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            time - 5000,
            time
        )

        // Find the most recently used app
        return stats?.maxByOrNull { it.lastTimeUsed }?.packageName ?: ""
    }

    private fun launchBlockerActivity(packageName: String) {
        // Launch main activity with flag to show blocking screen
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra("SHOW_BLOCKER", true)
            putExtra("BLOCKED_PACKAGE", packageName)
        }
        startActivity(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }
}