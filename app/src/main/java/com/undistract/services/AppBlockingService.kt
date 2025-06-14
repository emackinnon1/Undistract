package com.undistract.services

import android.app.Service
import android.app.usage.UsageStatsManager
import android.content.Intent
import android.os.IBinder
import com.undistract.ui.main.MainActivity
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch


/**
 * Uses UsageStats API to monitor foreground apps and block them.
 * Uses a CoroutineScope with a SupervisorJob for structured concurrency.
 *
 * This service runs in the background and periodically checks which app is in the foreground.
 * If a blocked app is detected, it launches the MainActivity with blocker UI.
 *
 * @property dispatcher The CoroutineDispatcher to use for launching coroutines.
 */
open class AppBlockingService(
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default
) : Service() {
    /**
     * CoroutineScope for launching background monitoring tasks.
     * Uses the injected dispatcher and a SupervisorJob.
     */
    private val serviceScope = CoroutineScope(dispatcher + SupervisorJob())

    /**
     * System service that provides access to device usage statistics.
     * Used to determine the current foreground app.
     */
    private lateinit var usageStatsManager: UsageStatsManager

    /**
     * Internal state tracking whether app monitoring is currently active.
     * Protected to allow modification by subclasses.
     */
    protected var _isMonitoring = false

    /**
     * Public read-only property that indicates if app monitoring is active.
     */
    internal val isMonitoring: Boolean get() = _isMonitoring

    /**
     * Exposes whether the coroutine scope is still active.
     * Primarily used for testing.
     *
     * @return true if the coroutine scope is active, false otherwise
     */
    internal fun isScopeActive() = serviceScope.isActive

    /**
     * Constants used by the service.
     */
    companion object {
        /**
         * Delay between consecutive app usage checks in milliseconds.
         */
        private const val MONITORING_INTERVAL_MS = 500L

        /**
         * Time window for querying usage stats in milliseconds.
         */
        private const val USAGE_STATS_WINDOW_MS = 5000L

        /**
         * Intent extra key for the list of package names to block.
         */
        const val EXTRA_BLOCKED_APPS = "BLOCKED_APPS"

        /**
         * Intent extra key to indicate the blocker UI should be shown.
         */
        const val EXTRA_SHOW_BLOCKER = "SHOW_BLOCKER"

        /**
         * Intent extra key for the package name of the blocked app.
         */
        const val EXTRA_BLOCKED_PACKAGE = "BLOCKED_PACKAGE"
    }

    /**
     * List of package names that should be blocked when detected in foreground.
     */
    private var blockedPackages: List<String> = emptyList()

    /**
     * This service does not support binding.
     *
     * @param intent The intent that was used to bind to this service
     * @return Always returns null as binding is not supported
     */
    override fun onBind(intent: Intent?): IBinder? = null

    /**
     * Initializes the UsageStatsManager when the service is created.
     */
    override fun onCreate() {
        super.onCreate()
        usageStatsManager = getSystemService(USAGE_STATS_SERVICE) as UsageStatsManager
    }

    /**
     * Handles service start commands, updating the list of blocked packages
     * and starting the monitoring coroutine.
     *
     * @param intent The intent containing the list of blocked packages
     * @param flags Additional data about this start request
     * @param startId A unique integer representing this start request
     * @return START_STICKY to indicate the service should be restarted if killed
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.getStringArrayListExtra(EXTRA_BLOCKED_APPS)?.let { packages ->
            blockedPackages = packages
            serviceScope.launch { monitorApps() }
        }
        return START_STICKY
    }

    /**
     * Main monitoring loop that periodically checks the foreground app
     * and launches blocker UI if a blocked app is detected.
     *
     * Open for testing purposes.
     */
    internal open suspend fun monitorApps() {
        _isMonitoring = true
        try {
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
        } finally {
            _isMonitoring = false
        }
    }

    /**
     * Determines the current foreground application by querying usage statistics.
     *
     * @return The package name of the current foreground app, or null if it cannot be determined
     */
    private fun getCurrentForegroundApp(): String? {
        val now = System.currentTimeMillis()
        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            now - USAGE_STATS_WINDOW_MS,
            now
        )
        return stats?.maxByOrNull { it.lastTimeUsed }?.packageName
    }

    /**
     * Launches the MainActivity with blocker UI when a blocked app is detected.
     *
     * @param packageName The package name of the blocked app
     */
    private fun launchBlockerActivity(packageName: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra(EXTRA_SHOW_BLOCKER, true)
            putExtra(EXTRA_BLOCKED_PACKAGE, packageName)
        }
        startActivity(intent)
    }

    /**
     * Cancels all coroutines when the service is destroyed.
     */
    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    /**
     * Sets the monitoring state. Used for testing.
     *
     * @param value The new monitoring state
     */
    internal fun setIsMonitoring(value: Boolean) {
        _isMonitoring = value
    }
}