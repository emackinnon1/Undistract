package com.undistract.services

import android.content.Intent
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ServiceController
import android.os.Looper


@RunWith(AndroidJUnit4::class)
class AppBlockerAccessibilityServiceTest {

    private lateinit var serviceController: ServiceController<AppBlockerAccessibilityService>
    private lateinit var service: AppBlockerAccessibilityService

    @Before
    fun setUp() {
        // Reset static variables
        AppBlockerAccessibilityService.isBlocking = false
        AppBlockerAccessibilityService.blockedApps = emptyList()

        // Initialize service
        serviceController = Robolectric.buildService(AppBlockerAccessibilityService::class.java)
        service = serviceController.create().get()
    }

    @Test
    fun testBroadcastReceiver_updatesBlockedApps() {
        // Given
        val testApps = arrayListOf("com.example.app1", "com.example.app2")
        val intent = Intent(AppBlockerAccessibilityService.ACTION_UPDATE_BLOCKED_APPS).apply {
            putStringArrayListExtra(AppBlockerAccessibilityService.EXTRA_APP_PACKAGES, testApps)
            putExtra(AppBlockerAccessibilityService.EXTRA_IS_BLOCKING, true)
        }

        // When
        LocalBroadcastManager.getInstance(ApplicationProvider.getApplicationContext())
            .sendBroadcast(intent)

        // Process pending operations on the main looper
        shadowOf(Looper.getMainLooper()).idle()

        // Then
        assertTrue(AppBlockerAccessibilityService.isBlocking)
        assertEquals(2, AppBlockerAccessibilityService.blockedApps.size)
        assertTrue(AppBlockerAccessibilityService.blockedApps.contains("com.example.app1"))
        assertTrue(AppBlockerAccessibilityService.blockedApps.contains("com.example.app2"))
    }

    @Test
    fun testGetAppName_returnsPackageNameWhenNotFound() {
        // Given
        val nonExistentPackage = "com.non.existent.package"

        // When
        val result = service.getAppName(nonExistentPackage)

        // Then
        assertEquals(nonExistentPackage, result)
    }
}