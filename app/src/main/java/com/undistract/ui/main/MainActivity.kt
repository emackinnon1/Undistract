package com.undistract.ui.main

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.accessibility.AccessibilityManager
import android.widget.Toast
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

class MainActivity : ComponentActivity() {
    companion object {
        private const val ACCESSIBILITY_SERVICE_ID = "com.undistract/.services.AppBlockerAccessibilityService"
    }
    
    private lateinit var nfcHelper: NfcHelper
    val newIntentFlow = MutableStateFlow<Intent?>(null)

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

        // Process the initial intent if it's an NFC intent
        intent?.let {
            if (nfcHelper.isNfcIntent(it)) {
                nfcHelper.handleIntent(it)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
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

    // Helper method to check if accessibility service is enabled
    private fun isAccessibilityServiceEnabled(): Boolean {
        val accessibilityManager = getSystemService(ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices = accessibilityManager.getEnabledAccessibilityServiceList(
            AccessibilityServiceInfo.FEEDBACK_ALL_MASK
        )
        return enabledServices.any { it.id.contains(ACCESSIBILITY_SERVICE_ID) }
    }

    private fun promptEnableAccessibilityService() {
        AppBlockerAccessibilityService.ensureAccessibilityServiceEnabled(this)
    }

    private fun showMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
