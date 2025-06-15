package com.undistract.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.undistract.data.daos.ProfileDao
import com.undistract.data.entities.ProfileEntity

@Database(
    entities = [
        ProfileEntity::class,
//        NfcTagEntity::class, // TODO: Placeholder for future implementation of NFC tag functionality.
//        UndistractedStatsEntity::class, // TODO: Placeholder for future implementation of undistracted stats tracking.
//        EmergencyUsageEntity::class // TODO: Placeholder for future implementation of emergency usage tracking.
    ],
    version = 1
)
abstract class UndistractDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
//    abstract fun nfcTagDao(): NfcTagDao // TODO: Placeholder for future implementation of NFC tag DAO.
//    abstract fun statsDao(): UndistractedStatsDao // TODO: Placeholder for future implementation of stats DAO.
//    abstract fun emergencyUsageDao(): EmergencyUsageDao // TODO: Placeholder for future implementation of emergency usage DAO.
}