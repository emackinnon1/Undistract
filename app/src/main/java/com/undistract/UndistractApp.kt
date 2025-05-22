package com.undistract

import android.app.Application
import com.undistract.data.repositories.ProfileManagerRepository

class UndistractApp : Application() {
    lateinit var appBlocker: AppBlocker
        private set

    lateinit var profileManagerRepository: ProfileManagerRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        appBlocker = AppBlocker(this)
        profileManagerRepository = ProfileManagerRepository(this)
    }

    companion object {
        lateinit var instance: UndistractApp
            private set

        val appBlocker get() = instance.appBlocker
        val profileManager get() = instance.profileManagerRepository
    }
}