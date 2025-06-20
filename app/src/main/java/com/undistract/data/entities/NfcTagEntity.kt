package com.undistract.data.entities

import java.util.Date

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "nfc_tags")
data class NfcTagEntity(
    @PrimaryKey val id: String,
    val payload: String,
    val createdAt: Long = System.currentTimeMillis()
)