package com.undistract.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_info")
data class AppInfoEntity(
    @PrimaryKey val packageName: String,
    val appName: String,
    val iconResName: String // Store a resource name or ID, not Drawable
)