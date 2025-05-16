package com.undistract

import android.app.Application

class UndistractApp : Application() {
    lateinit var appBlocker: AppBlocker
        private set

    lateinit var profileManager: ProfileManager
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        appBlocker = AppBlocker(this)
        profileManager = ProfileManager(this)
    }

    companion object {
        lateinit var instance: UndistractApp
            private set

        val appBlocker get() = instance.appBlocker
        val profileManager get() = instance.profileManager
    }
}