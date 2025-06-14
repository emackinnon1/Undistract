package com.undistract.data.models

import android.graphics.drawable.Drawable

/**
 * Data class representing information about an installed application.
 *
 * This class holds basic identifying information about an Android application,
 * including its package name, display name, and icon drawable.
 */
data class AppInfo(
    /**
     * The unique package name that identifies the application.
     * Example: "com.example.myapp"
     */
    val packageName: String,

    /**
     * The user-facing display name of the application.
     */
    val appName: String,

    /**
     * The application's icon as a Drawable resource.
     */
    val appIcon: Drawable
)