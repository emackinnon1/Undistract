package com.undistract.services

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager

/**
 * Service that handles intents to start/stop app blocking functionality.
 *
 * This service acts as a controller for the app blocking system, receiving external commands
 * to start or stop blocking specific applications. When an intent is received, it processes
 * the command and broadcasts updates to the AccessibilityService that performs the actual
 * blocking behavior.
 */
class BlockerService : Service() {
    /**
     * Constants used for intent actions, extras, and logging.
     */
    companion object {
        /**
         * Tag used for logging messages from this service.
         */
        private const val TAG = "BlockerService"

        /**
         * Intent action to start blocking specified applications.
         */
        const val ACTION_START_BLOCKING = "com.undistract.START_BLOCKING"

        /**
         * Intent action to stop blocking all applications.
         */
        const val ACTION_STOP_BLOCKING = "com.undistract.STOP_BLOCKING"

        /**
         * Intent extra key for the list of application package names to block.
         */
        const val EXTRA_APP_PACKAGES = "app_packages"
    }

    /**
     * This service does not support binding.
     *
     * @param intent The intent that was used to bind to this service
     * @return Always returns null as binding is not supported
     */
    override fun onBind(intent: Intent?): IBinder? = null

    /**
     * Processes start commands received by the service.
     *
     * Handles two types of intents:
     * - ACTION_START_BLOCKING: Begins blocking specified apps
     * - ACTION_STOP_BLOCKING: Stops blocking all apps
     *
     * @param intent The intent containing the action and app packages to block
     * @param flags Additional data about this start request
     * @param startId A unique integer representing this start request
     * @return START_STICKY to indicate the service should be restarted if killed
     */
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

    /**
     * Updates the blocking state and notifies the AccessibilityService.
     *
     * Logs the blocking action and broadcasts an intent to the AccessibilityService
     * with the updated list of apps to block and the blocking state.
     *
     * @param packages List of application package names to block
     * @param isBlocking Boolean flag indicating whether blocking should be enabled
     */
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