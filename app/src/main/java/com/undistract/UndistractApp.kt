package com.undistract

import android.app.Application
import androidx.room.Room
import com.undistract.data.local.MIGRATION_1_2
import com.undistract.data.local.UndistractDatabase
import com.undistract.managers.AppBlockerManager
import com.undistract.managers.ProfileManager

class UndistractApp : Application() {
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
    }

    companion object {
        lateinit var instance: UndistractApp
            private set

        val appBlocker get() = instance.appBlockerManager
        val profileManager get() = instance.profileManager
        val db get() = instance.database
    }
}