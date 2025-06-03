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

    @Test
    fun `service correctly updates and broadcasts state changes`() {
        // Step 1: Start blocking with initial packages
        val initialPackages = arrayListOf("com.example.app1", "com.example.app2")
        val startIntent1 = Intent(BlockerService.ACTION_START_BLOCKING).apply {
            putStringArrayListExtra(BlockerService.EXTRA_APP_PACKAGES, initialPackages)
        }

        service.onStartCommand(startIntent1, 0, 1)
        shadowOf(Looper.getMainLooper()).idle()

        // Verify first broadcast
        assertNotNull("First broadcast should have been sent", receivedIntent)
        val firstBroadcast = receivedIntent!!
        assertEquals(AppBlockerAccessibilityService.ACTION_UPDATE_BLOCKED_APPS, firstBroadcast.action)
        assertEquals(initialPackages, firstBroadcast.getStringArrayListExtra(AppBlockerAccessibilityService.EXTRA_APP_PACKAGES))
        assertTrue(firstBroadcast.getBooleanExtra(AppBlockerAccessibilityService.EXTRA_IS_BLOCKING, false))

        // Step 2: Update with different packages
        val updatedPackages = arrayListOf("com.example.app3")
        val startIntent2 = Intent(BlockerService.ACTION_START_BLOCKING).apply {
            putStringArrayListExtra(BlockerService.EXTRA_APP_PACKAGES, updatedPackages)
        }

        // Reset receivedIntent to verify only the new broadcast
        receivedIntent = null

        service.onStartCommand(startIntent2, 0, 2)
        shadowOf(Looper.getMainLooper()).idle()

        // Verify second broadcast
        assertNotNull("Second broadcast should have been sent", receivedIntent)
        val secondBroadcast = receivedIntent!!
        assertEquals(AppBlockerAccessibilityService.ACTION_UPDATE_BLOCKED_APPS, secondBroadcast.action)
        assertEquals(updatedPackages, secondBroadcast.getStringArrayListExtra(AppBlockerAccessibilityService.EXTRA_APP_PACKAGES))
        assertTrue(secondBroadcast.getBooleanExtra(AppBlockerAccessibilityService.EXTRA_IS_BLOCKING, false))

        // Step 3: Stop blocking
        val stopIntent = Intent(BlockerService.ACTION_STOP_BLOCKING)

        // Reset receivedIntent again
        receivedIntent = null

        service.onStartCommand(stopIntent, 0, 3)
        shadowOf(Looper.getMainLooper()).idle()

        // Verify third broadcast
        assertNotNull("Final broadcast should have been sent", receivedIntent)
        val finalBroadcast = receivedIntent!!
        assertEquals(AppBlockerAccessibilityService.ACTION_UPDATE_BLOCKED_APPS, finalBroadcast.action)
        val finalPackages = finalBroadcast.getStringArrayListExtra(AppBlockerAccessibilityService.EXTRA_APP_PACKAGES)
        assertNotNull("Packages list should not be null", finalPackages)
        assertTrue("Packages list should be empty", finalPackages!!.isEmpty())
        assertFalse(finalBroadcast.getBooleanExtra(AppBlockerAccessibilityService.EXTRA_IS_BLOCKING, true))
    }

    @Test
    fun `onBind returns null`() {
        // Act
        val result = service.onBind(Intent())

        // Assert
        assertNull("onBind should return null as the service is not bindable", result)
    }

    @Test
    fun `onStartCommand returns START_STICKY for all intent types`() {
        // The existing tests already verify this for specific intents
        // This test verifies it for an unknown action type

        // Arrange
        val intentWithUnknownAction = Intent("com.undistract.UNKNOWN_ACTION")

        // Act
        val result = service.onStartCommand(intentWithUnknownAction, 0, 1)
        shadowOf(Looper.getMainLooper()).idle()

        // Assert
        assertEquals(Service.START_STICKY, result)
        // No broadcast should be sent for unknown action
        assertNull(receivedIntent)
    }
}