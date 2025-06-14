package com.undistract.managers

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Process
import android.provider.Settings
import androidx.core.content.edit
import com.undistract.data.models.Profile
import com.undistract.services.AppBlockingService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manager class responsible for controlling application blocking functionality.
 *
 * Handles checking and requesting permissions, toggling blocking state,
 * and managing the app blocking service lifecycle.
 *
 * @property context The application context used for system service access and preferences
 */
class AppBlockerManager(private val context: Context) {
    /**
     * Mutable state representing whether app blocking is currently active.
     */
    private val _isBlocking = MutableStateFlow(false)

    /**
     * Public read-only state representing whether app blocking is currently active.
     */
    val isBlocking: StateFlow<Boolean> = _isBlocking.asStateFlow()

    /**
     * Mutable state representing whether the app has required system permissions.
     */
    private val _isAuthorized = MutableStateFlow(false)

    /**
     * Public read-only state representing whether the app has required system permissions.
     */
    val isAuthorized: StateFlow<Boolean> = _isAuthorized.asStateFlow()

    /**
     * Intent for the app blocking service, lazily initialized.
     */
    private val blockingServiceIntent by lazy { Intent(context, AppBlockingService::class.java) }

    /**
     * Initializes the manager by loading saved state and checking authorization.
     */
    init {
        loadBlockingState()
        checkAuthorization()
    }

    /**
     * Checks if the app has necessary permissions to perform app blocking.
     *
     * Verifies usage stats access and overlay permissions.
     */
    fun checkAuthorization() {
        val appOpsManager = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val uid = Process.myUid()
        val packageName = context.packageName
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOpsManager.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, uid, packageName)
        } else {
            @Suppress("DEPRECATION")
            appOpsManager.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, uid, packageName)
        }
        _isAuthorized.value = mode == AppOpsManager.MODE_ALLOWED && Settings.canDrawOverlays(context)
    }

    /**
     * Launches system settings screens to request required permissions.
     *
     * Opens both usage access settings and overlay permission settings.
     */
    fun requestAuthorization() {
        context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK })
        context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}")).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK })
    }

    /**
     * Toggles the app blocking state and applies the changes.
     *
     * @param profile The profile containing app package names to block
     * @return Early if app is not authorized
     */
    fun toggleBlocking(profile: Profile) {
        if (!_isAuthorized.value) return
        _isBlocking.value = !_isBlocking.value
        saveBlockingState()
        applyBlockingSettings(profile)
    }

    /**
     * Sets the blocking state to a specific value without applying it.
     *
     * @param isBlocking Whether blocking should be enabled
     */
    fun setBlockingState(isBlocking: Boolean) {
        _isBlocking.value = isBlocking
        saveBlockingState()
    }

    /**
     * Applies the current blocking settings based on the given profile.
     *
     * Starts or stops the blocking service based on current state.
     *
     * @param profile The profile containing apps to block
     */
    fun applyBlockingSettings(profile: Profile) {
        if (_isBlocking.value) {
            blockingServiceIntent.putExtra("BLOCKED_APPS", ArrayList(profile.appPackageNames))
            context.startService(blockingServiceIntent)
        } else {
            context.stopService(blockingServiceIntent)
        }
    }

    /**
     * Loads the previously saved blocking state from SharedPreferences.
     */
    private fun loadBlockingState() {
        _isBlocking.value = context.getSharedPreferences("app_blocker_prefs", Context.MODE_PRIVATE).getBoolean("isBlocking", false)
    }

    /**
     * Saves the current blocking state to SharedPreferences.
     */
    private fun saveBlockingState() {
        context.getSharedPreferences("app_blocker_prefs", Context.MODE_PRIVATE).edit {
            putBoolean(
                "isBlocking",
                _isBlocking.value
            )
        }
    }
}
