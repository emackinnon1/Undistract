package com.undistract

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import kotlinx.coroutines.flow.MutableStateFlow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.undistract.ui.theme.UndistractTheme
import android.accessibilityservice.AccessibilityServiceInfo
import android.view.accessibility.AccessibilityManager
import android.provider.Settings
import android.widget.Toast
import android.content.Context

class MainActivity : ComponentActivity() {
    private lateinit var nfcHelper: NfcHelper

    // Create a MutableStateFlow to publish new intents
    val newIntentFlow = MutableStateFlow<Intent?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!isAccessibilityServiceEnabled(this)) {
            promptEnableAccessibilityService()
        }

        // Check if we're coming from BlockedAppActivity
//        if (intent.getBooleanExtra("came_from_blocked_app", false)) {
//            showMessage("App is blocked by Undistract")
//        } else {
//            showMessage("Hold phone near NFC tag to read")
//        }


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

        // Process the initial intent if it's an NFC intent
        intent?.let {
            if (nfcHelper.isNfcIntent(it)) {
                nfcHelper.handleIntent(it)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent) // Store the intent for later use
        newIntentFlow.value = intent
    }

    override fun onResume() {
        super.onResume()
        nfcHelper.enableForegroundDispatch()
    }

    override fun onPause() {
        super.onPause()
        nfcHelper.disableForegroundDispatch()
    }

    override fun onStop() {
        super.onStop()
        // TODO: Ensure any resources are properly released
    }

    // Helper method to check if accessibility service is enabled
    private fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val accessibilityManager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices = accessibilityManager.getEnabledAccessibilityServiceList(
            AccessibilityServiceInfo.FEEDBACK_ALL_MASK
        )

        return enabledServices.any { it.id.contains("com.undistract/.AppBlockerAccessibilityService") }
    }

    private fun promptEnableAccessibilityService() {
        Toast.makeText(this, "Please enable the accessibility service", Toast.LENGTH_LONG).show()
        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }

    private fun showMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}