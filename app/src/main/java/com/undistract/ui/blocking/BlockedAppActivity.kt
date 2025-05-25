package com.undistract.ui.blocking

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity

class BlockedAppActivity : AppCompatActivity() {
    
    companion object {
        private const val FINISH_DELAY_MS = 500L
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        Handler(Looper.getMainLooper()).postDelayed({ finish() }, FINISH_DELAY_MS)
    }
}
