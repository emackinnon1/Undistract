package com.undistract.ui.main

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.os.Bundle
import android.view.accessibility.AccessibilityManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.undistract.nfc.NfcHelper
import com.undistract.services.AppBlockerAccessibilityService
import com.undistract.ui.blocking.BlockerScreen
import com.undistract.ui.theme.UndistractTheme
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Main entry point activity for the Undistract application.
 *
 * This activity serves as the primary interface for the application, handling:
 * - NFC interactions through NfcHelper
 * - Accessibility service verification and enabling
 * - Setting up the Jetpack Compose UI with BlockerScreen
 *
 * The activity monitors NFC intents and forwards them to the BlockerScreen composable
 * via a SharedFlow, enabling the app's core functionality of using NFC tags to
 * control app blocking features.
 */
class MainActivity : ComponentActivity() {
    /**
     * Companion object containing constants used by MainActivity.
     */
    companion object {
        /**
         * The fully qualified identifier for the app's accessibility service.
         * Used to verify if the service is enabled in system settings.
         */
        private const val ACCESSIBILITY_SERVICE_ID = "com.undistract/.services.AppBlockerAccessibilityService"
    }

    /**
     * Helper class that manages NFC operations such as reading and writing tags.
     * Initialized in onCreate.
     */
    internal lateinit var nfcHelper: NfcHelper

    /**
     * Flow that emits new NFC intents received by the activity.
     * This flow is observed by the BlockerScreen to handle NFC tag interactions.
     */
    val newIntentFlow = MutableStateFlow<Intent?>(null)

    /**
     * Initializes the activity, sets up NFC handling, checks accessibility service status,
     * and inflates the Compose UI.
     *
     * This method:
     * 1. Verifies if the accessibility service is enabled and prompts the user if needed
     * 2. Initializes the NFC helper
     * 3. Sets up the Compose UI with the BlockerScreen
     *
     * @param savedInstanceState If the activity is being re-initialized after previously
     * being shut down, this contains the data it most recently supplied in onSaveInstanceState.
     * Otherwise it is null.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!isAccessibilityServiceEnabled()) {
            promptEnableAccessibilityService()
        }

        nfcHelper = NfcHelper(this)

        setContent {
            UndistractTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    BlockerScreen(nfcHelper = nfcHelper, newIntentFlow = newIntentFlow)
                }
            }
        }
    }

    /**
     * Handles new intents directed to this activity, particularly NFC tag discovery events.
     *
     * When the system dispatches a new intent to this activity (such as when an NFC tag is
     * scanned), this method updates the activity's intent reference and emits the new intent
     * to the newIntentFlow, which notifies observers about the NFC event.
     *
     * @param intent The new intent received by the activity, typically containing NFC tag data
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        newIntentFlow.value = intent
    }

    /**
     * Checks if the application's accessibility service is currently enabled in system settings.
     *
     * This method queries the AccessibilityManager to get a list of all enabled accessibility
     * services and checks if the app's service is among them.
     *
     * @return Boolean indicating whether the accessibility service is enabled (true) or not (false)
     */
    private fun isAccessibilityServiceEnabled(): Boolean {
        val accessibilityManager = getSystemService(ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices = accessibilityManager.getEnabledAccessibilityServiceList(
            AccessibilityServiceInfo.FEEDBACK_ALL_MASK
        )
        return enabledServices.any { it.id.contains(ACCESSIBILITY_SERVICE_ID) }
    }

    /**
     * Prompts the user to enable the application's accessibility service.
     *
     * If the accessibility service is not enabled, this method triggers a system prompt
     * that guides the user to the appropriate system settings to enable it.
     * The accessibility service is required for the app to detect and block other applications.
     */
    private fun promptEnableAccessibilityService() {
        AppBlockerAccessibilityService.ensureAccessibilityServiceEnabled(this)
    }
}