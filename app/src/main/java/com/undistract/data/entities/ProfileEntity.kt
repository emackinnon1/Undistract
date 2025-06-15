package com.undistract.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.undistract.data.helpers.StringListConverter

@Entity(tableName = "profiles")
@TypeConverters(StringListConverter::class)
data class ProfileEntity(
    @PrimaryKey val id: String,
    val name: String,
    val appPackageNames: List<String> = emptyList(),
    val icon: String = "baseline_block_24"
)