package com.undistract

import android.app.AppOpsManager
import android.os.Build
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Process
import android.provider.Settings
import com.undistract.data.models.Profile
import com.undistract.services.blocking.AppBlockingService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppBlocker(private val context: Context) {
    private val _isBlocking = MutableStateFlow(false)
    val isBlocking: StateFlow<Boolean> = _isBlocking.asStateFlow()

    private val _isAuthorized = MutableStateFlow(false)
    val isAuthorized: StateFlow<Boolean> = _isAuthorized.asStateFlow()

    // Intent for our blocking service (which we'll implement later)
    private val blockingServiceIntent by lazy { Intent(context, AppBlockingService::class.java) }

    init {
        loadBlockingState()
        checkAuthorization()
    }

    fun checkAuthorization() {
        // Check usage stats permission
        val appOpsManager = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val uid = Process.myUid()
        val packageName = context.packageName

        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOpsManager.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, uid, packageName)
        } else {
            @Suppress("DEPRECATION")
            appOpsManager.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, uid, packageName)
        }

        // Check overlay permission
        val canDrawOverlays = Settings.canDrawOverlays(context)

        _isAuthorized.value = mode == AppOpsManager.MODE_ALLOWED && canDrawOverlays
    }

    fun requestAuthorization() {
        // Request usage stats permission
        val usageAccessIntent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        usageAccessIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(usageAccessIntent)

        // Request overlay permission
        val overlayIntent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        )
        overlayIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(overlayIntent)
    }

    fun toggleBlocking(profile: Profile) {
        if (!_isAuthorized.value) {
            println("Not authorized to block apps")
            return
        }

        _isBlocking.value = !_isBlocking.value
        saveBlockingState()
        applyBlockingSettings(profile)
    }

    fun setBlockingState(isBlocking: Boolean) {
        _isBlocking.value = isBlocking
        saveBlockingState()
    }

    fun applyBlockingSettings(profile: Profile) {
        if (_isBlocking.value) {
            println("Blocking ${profile.appPackageNames.size} apps")

            // Start the blocking service
            blockingServiceIntent.putExtra("BLOCKED_APPS", ArrayList(profile.appPackageNames))
            context.startService(blockingServiceIntent)
        } else {
            // Stop the blocking service
            context.stopService(blockingServiceIntent)
        }
    }

    private fun loadBlockingState() {
        val sharedPrefs = context.getSharedPreferences("app_blocker_prefs", Context.MODE_PRIVATE)
        _isBlocking.value = sharedPrefs.getBoolean("isBlocking", false)
    }

    private fun saveBlockingState() {
        val sharedPrefs = context.getSharedPreferences("app_blocker_prefs", Context.MODE_PRIVATE)
        with(sharedPrefs.edit()) {
            putBoolean("isBlocking", _isBlocking.value)
            apply()
        }
    }
}