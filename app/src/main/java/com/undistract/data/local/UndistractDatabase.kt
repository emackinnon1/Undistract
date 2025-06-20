package com.undistract.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.undistract.data.daos.NfcTagDao
import com.undistract.data.daos.ProfileDao
import com.undistract.data.entities.NfcTagEntity
import com.undistract.data.entities.ProfileEntity
import com.undistract.data.helpers.DateTypeConverters

@Database(
    entities = [
        ProfileEntity::class,
        NfcTagEntity::class,
//        UndistractedStatsEntity::class, // TODO: Placeholder for future implementation of undistracted stats tracking.
//        EmergencyUsageEntity::class // TODO: Placeholder for future implementation of emergency usage tracking.
    ],
    version = 2,
//    autoMigrations = [
//    AutoMigration (
//      from = 1,
//      to = 2,
//      spec = AppDatabase.MyAutoMigration::class
//    )
//  ]
    exportSchema = false
)
@TypeConverters(DateTypeConverters::class)
abstract class UndistractDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun nfcTagDao(): NfcTagDao
//    abstract fun statsDao(): UndistractedStatsDao // TODO: Placeholder for future implementation of stats DAO.
//    abstract fun emergencyUsageDao(): EmergencyUsageDao // TODO: Placeholder for future implementation of emergency usage DAO.
}