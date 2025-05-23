package com.undistract.ui.blocking

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity

class BlockedAppActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Get app name that was blocked (optional)
        val appName = intent.getStringExtra("app_name") ?: "This app"

        // Close this activity after a delay
        Handler(Looper.getMainLooper()).postDelayed({
            // Create an intent to launch MainActivity
            finish()
        }, 500)
    }
}