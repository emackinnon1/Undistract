//The BlockerService class is a relatively simple Android service with the following main areas of functionality:
//1.Intent Handling:
//ACTION_START_BLOCKING: Starts blocking specified app packages
//ACTION_STOP_BLOCKING: Stops all app blocking

//2. State Management and Broadcasting:
//Updates blocking state based on received intents
//Uses LocalBroadcastManager to notify the AppBlockerAccessibilityService about changes
//Passes along the list of packages to block and the blocking state

//3. Lifecycle Management:
//Standard Service lifecycle handling (onBind, onStartCommand)

package com.undistract.services

import android.app.Service
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Looper
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.junit.Assert.*

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P])
class BlockerServiceTest {

    private lateinit var service: BlockerService
    private var receivedIntent: Intent? = null

    @Before
    fun setUp() {
        service = Robolectric.buildService(BlockerService::class.java).create().get()
        receivedIntent = null

        // Register a broadcast receiver to capture sent broadcasts
        val receiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(context: android.content.Context?, intent: Intent?) {
                receivedIntent = intent
            }
        }

        LocalBroadcastManager.getInstance(service)
            .registerReceiver(receiver, IntentFilter(AppBlockerAccessibilityService.ACTION_UPDATE_BLOCKED_APPS))
    }

    @Test
    fun `onStartCommand processes START_BLOCKING intent correctly`() {
        // Arrange
        val packages = arrayListOf("com.example.app1", "com.example.app2")
        val intent = Intent(BlockerService.ACTION_START_BLOCKING).apply {
            putStringArrayListExtra(BlockerService.EXTRA_APP_PACKAGES, packages)
        }

        // Act
        val result = service.onStartCommand(intent, 0, 1)
        shadowOf(Looper.getMainLooper()).idle()

        // Assert
        assertEquals(Service.START_STICKY, result)
        assertNotNull("Broadcast should have been sent", receivedIntent)

        // Now we know receivedIntent is not null, we can use direct assertions
        val actualIntent = receivedIntent!!
        assertEquals(AppBlockerAccessibilityService.ACTION_UPDATE_BLOCKED_APPS, actualIntent.action)
        assertEquals(packages, actualIntent.getStringArrayListExtra(AppBlockerAccessibilityService.EXTRA_APP_PACKAGES))
        assertTrue(actualIntent.getBooleanExtra(AppBlockerAccessibilityService.EXTRA_IS_BLOCKING, false))
    }

    @Test
    fun `onStartCommand processes STOP_BLOCKING intent correctly`() {
        // Arrange
        val intent = Intent(BlockerService.ACTION_STOP_BLOCKING)

        // Act
        val result = service.onStartCommand(intent, 0, 1)
        shadowOf(Looper.getMainLooper()).idle()

        // Assert
        assertEquals(Service.START_STICKY, result)
        assertNotNull("Broadcast should have been sent", receivedIntent)

        val actualIntent = receivedIntent!!
        assertEquals(AppBlockerAccessibilityService.ACTION_UPDATE_BLOCKED_APPS, actualIntent.action)

        val packages = actualIntent.getStringArrayListExtra(AppBlockerAccessibilityService.EXTRA_APP_PACKAGES)
        assertNotNull("Packages list should not be null", packages)
        assertTrue("Packages list should be empty", packages!!.isEmpty())

        assertFalse(actualIntent.getBooleanExtra(AppBlockerAccessibilityService.EXTRA_IS_BLOCKING, true))
    }

    @Test
    fun `onStartCommand handles null intent gracefully`() {
        // Act
        val result = service.onStartCommand(null, 0, 1)
        shadowOf(Looper.getMainLooper()).idle()

        // Assert
        assertEquals(Service.START_STICKY, result)
        // No broadcast should be sent
        assertNull(receivedIntent)
    }

    @Test
    fun `onStartCommand handles missing extra packages gracefully`() {
        // Arrange
        val intent = Intent(BlockerService.ACTION_START_BLOCKING)
        // Intentionally NOT adding the EXTRA_APP_PACKAGES

        // Act
        service.onStartCommand(intent, 0, 1)
        shadowOf(Looper.getMainLooper()).idle()

        // Assert
        assertNotNull("Broadcast should have been sent", receivedIntent)

        val actualIntent = receivedIntent!!
        val packages = actualIntent.getStringArrayListExtra(AppBlockerAccessibilityService.EXTRA_APP_PACKAGES)
        assertNotNull("Packages list should not be null", packages)
        assertTrue("Packages list should be empty when extra is not provided", packages!!.isEmpty())

        assertTrue(actualIntent.getBooleanExtra(AppBlockerAccessibilityService.EXTRA_IS_BLOCKING, false))
    }


}