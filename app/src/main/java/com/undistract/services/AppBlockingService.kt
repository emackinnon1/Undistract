package com.undistract.services

import android.app.Service
import android.app.usage.UsageStatsManager
import android.content.Intent
import android.os.IBinder
import android.view.WindowManager
import kotlinx.coroutines.cancel
import com.undistract.ui.main.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class AppBlockingService : Service() {
    companion object {
        private const val MONITORING_INTERVAL_MS = 500L
        private const val USAGE_STATS_WINDOW_MS = 5000L
        const val EXTRA_BLOCKED_APPS = "BLOCKED_APPS"
        const val EXTRA_SHOW_BLOCKER = "SHOW_BLOCKER"
        const val EXTRA_BLOCKED_PACKAGE = "BLOCKED_PACKAGE"
    }
    
    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private lateinit var usageStatsManager: UsageStatsManager
    private var blockedPackages: List<String> = emptyList()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        usageStatsManager = getSystemService(USAGE_STATS_SERVICE) as UsageStatsManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.getStringArrayListExtra(EXTRA_BLOCKED_APPS)?.let { packages ->
            blockedPackages = packages
            serviceScope.launch { monitorApps() }
        }
        return START_STICKY
    }

    private suspend fun monitorApps() {
        while (serviceScope.isActive) {
            try {
                getCurrentForegroundApp()?.takeIf { it in blockedPackages }?.let {
                    launchBlockerActivity(it)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            delay(MONITORING_INTERVAL_MS)
        }
    }

    private fun getCurrentForegroundApp(): String? {
        val now = System.currentTimeMillis()
        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            now - USAGE_STATS_WINDOW_MS,
            now
        )
        return stats?.maxByOrNull { it.lastTimeUsed }?.packageName
    }

    private fun launchBlockerActivity(packageName: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra(EXTRA_SHOW_BLOCKER, true)
            putExtra(EXTRA_BLOCKED_PACKAGE, packageName)
        }
        startActivity(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
