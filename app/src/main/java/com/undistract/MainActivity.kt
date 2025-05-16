package com.undistract

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import kotlinx.coroutines.flow.MutableStateFlow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import com.undistract.ui.theme.UndistractTheme


class MainActivity : ComponentActivity() {
    private lateinit var nfcHelper: NfcHelper

    // Create a MutableStateFlow to publish new intents
    val newIntentFlow = MutableStateFlow<Intent?>(null)

//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        nfcHelper = NfcHelper(this)
//        setContent {
//            UndistractTheme {
//                Surface(
//                    modifier = Modifier.fillMaxSize(),
//                    color = MaterialTheme.colorScheme.background
//                ) {
//                    BlockerScreen(nfcHelper = nfcHelper, newIntentFlow = newIntentFlow)
//                }
//            }
//        }
////        setContent {
////            UndistractTheme {
////                Surface(
////                    modifier = Modifier.fillMaxSize(),
////                    color = MaterialTheme.colorScheme.background
////                ) {
////                    Text("Hello World")
////                }
////            }
////        }
//    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Publish new intent to the flow
        newIntentFlow.value = intent
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
            nfcHelper.handleIntent(it)
        }
    }

    override fun onResume() {
        super.onResume()
        nfcHelper.enableForegroundDispatch()
    }

    override fun onPause() {
        super.onPause()
        nfcHelper.disableForegroundDispatch()
    }

//    override fun onNewIntent(intent: Intent) {
//        super.onNewIntent(intent)
//        setIntent(intent)
//        nfcHelper.handleIntent(intent)
//    }
}