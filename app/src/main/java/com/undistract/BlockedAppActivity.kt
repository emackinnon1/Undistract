package com.undistract

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity

class BlockedAppActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Get app name that was blocked (optional)
        val appName = intent.getStringExtra("app_name") ?: "This app"

        // Close this activity after a delay
        Handler(Looper.getMainLooper()).postDelayed({
            // Create an intent to launch MainActivity
            val intent = Intent(this, MainActivity::class.java).apply {
                // Clear the back stack so user can't return to blocked app
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

                // Optional: Pass information about the blocked app
                putExtra("came_from_blocked_app", true)
                putExtra("blocked_app_name", appName)
            }
            startActivity(intent)
            finish()
        }, 500)
    }
}