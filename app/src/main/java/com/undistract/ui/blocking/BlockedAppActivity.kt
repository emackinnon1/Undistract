package com.undistract.ui.blocking

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity

/**
 * Activity that is displayed when the user tries to open a blocked app.
 *
 * This activity serves as an interception mechanism that appears briefly when a user
 * attempts to launch an application that has been configured as blocked in the Undistract
 * system. The activity automatically dismisses itself after a short delay, effectively
 * preventing access to the target application without requiring user interaction.
 *
 * The brief appearance helps users recognize that the app they tried to open is currently
 * blocked by the Undistract service.
 */
class BlockedAppActivity : AppCompatActivity() {

    /**
     * Companion object containing constants used by the BlockedAppActivity.
     */
    companion object {
        /**
         * The delay in milliseconds before the activity automatically finishes.
         * This value is intentionally short to provide brief visual feedback without
         * requiring user interaction.
         */
        private const val FINISH_DELAY_MS = 500L
    }

    /**
     * Initializes the activity and sets up the automatic dismissal.
     *
     * This method posts a delayed task to finish the activity after the specified delay.
     * No UI elements are inflated as this activity is meant to be transient.
     *
     * @param savedInstanceState If the activity is being re-initialized after previously
     * being shut down, this contains the data it most recently supplied in onSaveInstanceState.
     * Otherwise it is null.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Handler(Looper.getMainLooper()).postDelayed({ finish() }, FINISH_DELAY_MS)
    }
}