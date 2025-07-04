package com.undistract

import android.app.Application
import android.util.Log
import androidx.room.Room
import com.undistract.data.local.MIGRATION_1_2
import com.undistract.data.repositories.ProfileRepositoryImpl
import com.undistract.data.migration.ProfileMigrationUtil
import com.undistract.data.local.UndistractDatabase
import com.undistract.managers.AppBlockerManager
import com.undistract.managers.ProfileManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class UndistractApp : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    lateinit var appBlockerManager: AppBlockerManager
        private set

    lateinit var profileManager: ProfileManager
        private set

    lateinit var database: UndistractDatabase
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        appBlockerManager = AppBlockerManager(this)
        this@UndistractApp.profileManager = ProfileManager(this)
        database = UndistractDatabase.getDatabase(this)

        // Add this code for profile migration
        val profileRepository = ProfileRepositoryImpl(database.profileDao())
        val migrationUtil = ProfileMigrationUtil(this, profileRepository)

        // Run migration in a background scope
        applicationScope.launch {
            if (migrationUtil.isMigrationNeeded()) {
                val migratedCount = migrationUtil.migrateProfiles()
                Log.d("UndistractApp", "Migrated $migratedCount profiles")
            }
        }
    }

    companion object {
        lateinit var instance: UndistractApp
            private set

        val appBlocker get() = instance.appBlockerManager
        val profileManager get() = instance.profileManager
        val db get() = instance.database
    }
}