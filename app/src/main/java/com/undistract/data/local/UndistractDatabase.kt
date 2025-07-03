package com.undistract.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
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
//    TODO: Uncomment and implement auto-migrations when needed.
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
    companion object {
        @Volatile
        private var Instance: UndistractDatabase? = null

        fun getDatabase(context: Context): UndistractDatabase {
            // if the Instance is not null, return it, otherwise create a new database instance.
            return Instance ?: synchronized(this) {
                Room.databaseBuilder(context, UndistractDatabase::class.java, "undistract-db")
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { Instance = it }
            }
        }
    }
}