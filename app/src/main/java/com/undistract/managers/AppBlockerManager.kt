package com.undistract.managers

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Process
import android.provider.Settings
import com.undistract.data.models.Profile
import com.undistract.services.AppBlockingService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import androidx.core.content.edit

class AppBlockerManager(private val context: Context) {
    private val _isBlocking = MutableStateFlow(false)
    val isBlocking: StateFlow<Boolean> = _isBlocking.asStateFlow()

    private val _isAuthorized = MutableStateFlow(false)
    val isAuthorized: StateFlow<Boolean> = _isAuthorized.asStateFlow()

    private val blockingServiceIntent by lazy { Intent(context, AppBlockingService::class.java) }

    init {
        loadBlockingState()
        checkAuthorization()
    }

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

    fun requestAuthorization() {
        context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK })
        context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}")).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK })
    }

    fun toggleBlocking(profile: Profile) {
        if (!_isAuthorized.value) return
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
            blockingServiceIntent.putExtra("BLOCKED_APPS", ArrayList(profile.appPackageNames))
            context.startService(blockingServiceIntent)
        } else {
            context.stopService(blockingServiceIntent)
        }
    }

    private fun loadBlockingState() {
        _isBlocking.value = context.getSharedPreferences("app_blocker_prefs", Context.MODE_PRIVATE).getBoolean("isBlocking", false)
    }

    private fun saveBlockingState() {
        context.getSharedPreferences("app_blocker_prefs", Context.MODE_PRIVATE).edit {
            putBoolean(
                "isBlocking",
                _isBlocking.value
            )
        }
    }
}

