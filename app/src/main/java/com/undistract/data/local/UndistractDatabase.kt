package com.undistract.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.undistract.data.daos.ProfileDao
import com.undistract.data.entities.ProfileEntity

@Database(
    entities = [
        ProfileEntity::class,
//        NfcTagEntity::class,
//        AppInfoEntity::class,
//        UndistractedStatsEntity::class,
//        EmergencyUsageEntity::class
    ],
    version = 1
)
abstract class UndistractDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
//    abstract fun nfcTagDao(): NfcTagDao
//    abstract fun appInfoDao(): AppInfoDao
//    abstract fun statsDao(): UndistractedStatsDao
//    abstract fun emergencyUsageDao(): EmergencyUsageDao
}