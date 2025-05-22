package com.undistract.services.accessibility

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import android.widget.Toast
import android.content.Intent

/**
 * Checks if the accessibility service is enabled and prompts the user to enable it if not.
 */
fun ensureAccessibilityServiceEnabled(context: Context) {
    val accessibilityManager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
    val enabledServices = accessibilityManager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)

    val isServiceEnabled = enabledServices.any {
        it.id.contains(context.packageName + "/.AppBlockerAccessibilityService")
    }

    if (!isServiceEnabled) {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        context.startActivity(intent)
        Toast.makeText(
            context,
            "Please enable Undistract Accessibility Service to block apps",
            Toast.LENGTH_LONG
        ).show()
    }
}