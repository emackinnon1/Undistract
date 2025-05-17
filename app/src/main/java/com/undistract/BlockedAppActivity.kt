package com.undistract

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity

class BlockedAppActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_blocked_app)

        // Get app name that was blocked (optional)
        val appName = intent.getStringExtra("app_name") ?: "This app"

        // Close this activity after a delay
        Handler(Looper.getMainLooper()).postDelayed({
            finish()
        }, 2000) // 2 seconds
    }
}