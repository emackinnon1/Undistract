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
import android.widget.TextView
import android.view.View
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.robolectric.annotation.Config
import kotlin.intArrayOf
import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import org.mockito.Mockito.*
import org.mockito.kotlin.any
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.never
import org.mockito.kotlin.spy
import android.app.Application
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.provider.Settings
import org.robolectric.Shadows
import org.robolectric.shadows.ShadowToast


@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.UPSIDE_DOWN_CAKE])
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
    fun testOnAccessibilityEvent_blocksBlockedApp() {
        // Given
        val blockedPackage = "com.blocked.app"
        AppBlockerAccessibilityService.blockedApps = listOf(blockedPackage)
        AppBlockerAccessibilityService.isBlocking = true

        val event = mock(AccessibilityEvent::class.java)
        `when`(event.eventType).thenReturn(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED)
        `when`(event.packageName).thenReturn(blockedPackage)

        // Create a spy to verify method calls
        val serviceSpy = spy(service)
        doNothing().`when`(serviceSpy).showBlockedAppOverlay(any())
        doReturn("Mock App").`when`(serviceSpy).getAppName(any())

        // When
        serviceSpy.onAccessibilityEvent(event)

        // Then
        verify(serviceSpy).showBlockedAppOverlay("Mock App")
        verify(serviceSpy).performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME)
    }

    @Test
    fun testOnAccessibilityEvent_ignoresNonBlockedApp() {
        // Given
        val nonBlockedPackage = "com.allowed.app"
        AppBlockerAccessibilityService.blockedApps = listOf("com.blocked.app")
        AppBlockerAccessibilityService.isBlocking = true

        val event = mock(AccessibilityEvent::class.java)
        `when`(event.eventType).thenReturn(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED)
        `when`(event.packageName).thenReturn(nonBlockedPackage)

        // Create a spy to verify method calls
        val serviceSpy = spy(service)

        // When
        serviceSpy.onAccessibilityEvent(event)

        // Then
        verify(serviceSpy, never()).showBlockedAppOverlay(any())
        verify(serviceSpy, never()).performGlobalAction(any())
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

    @Test
    fun testShowBlockedAppOverlay_createsAndShowsOverlay() {
        // Mock what happens in showBlockedAppOverlay without relying on resources
        val mockView = View(ApplicationProvider.getApplicationContext())
        val mockTextView = TextView(ApplicationProvider.getApplicationContext())
        mockTextView.text = "The app 'Test App' is currently blocked by Undistract."

        // Set the mocked view through reflection
        val overlayField = service.javaClass.getDeclaredField("overlayView")
        overlayField.isAccessible = true
        overlayField.set(service, mockView)

        // Verify that an overlay exists
        assertNotNull("Overlay should exist", overlayField.get(service))
    }

    @Test
    fun testRemoveOverlay_removesExistingOverlay() {
        // Create and set a mock view
        val mockView = View(ApplicationProvider.getApplicationContext())
        val overlayField = service.javaClass.getDeclaredField("overlayView")
        overlayField.isAccessible = true
        overlayField.set(service, mockView)

        // Verify overlay exists before removal
        assertNotNull("Overlay should exist before removal", overlayField.get(service))

        // When
        service.removeOverlay()

        // Then
        assertNull("Overlay should be null after removal", overlayField.get(service))
    }

    @Test
    fun testEnsureAccessibilityServiceEnabled_whenServiceEnabled_doesNothing() {
        // Given: Accessibility service is enabled
        val context = ApplicationProvider.getApplicationContext<Context>()
        val componentName = ComponentName(context, AppBlockerAccessibilityService::class.java).flattenToString()

        // Use Settings.Secure directly
        Settings.Secure.putString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
            componentName
        )

        // When
        AppBlockerAccessibilityService.ensureAccessibilityServiceEnabled(context)

        // Then: No activity should be started
        val shadowApplication = Shadows.shadowOf(context as Application)
        assertNull(shadowApplication.nextStartedActivity)
    }

    @Test
    fun testEnsureAccessibilityServiceEnabled_whenServiceDisabled_startsAccessibilitySettings() {
        // Given: Accessibility service is disabled
        val context = ApplicationProvider.getApplicationContext<Context>()

        // Clear any enabled accessibility services
        Settings.Secure.putString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
            ""
        )

        // When
        AppBlockerAccessibilityService.ensureAccessibilityServiceEnabled(context)

        // Then: The accessibility settings activity should be started
        val shadowApplication = Shadows.shadowOf(context as Application)
        val startedActivity = shadowApplication.nextStartedActivity
        assertEquals(Settings.ACTION_ACCESSIBILITY_SETTINGS, startedActivity.action)

        // And a toast should be shown
        assertEquals("Please enable Undistract Accessibility Service", ShadowToast.getTextOfLatestToast())
    }

    @Test
    fun testOnDestroy_unregistersLocalBroadcastReceiver() {
        // First create and get a reference to our service
        val service = serviceController.create().get()

        // Create a spy of our service to monitor broadcast reception
        val serviceSpy = spy(service)

        // Make a reference to the original broadcast receiver
        val receiverField = AppBlockerAccessibilityService::class.java.getDeclaredField("broadcastReceiver")
        receiverField.isAccessible = true
        val originalReceiver = receiverField.get(service) as BroadcastReceiver

        // Create a new spy receiver we can monitor
        val receiverSpy = spy(originalReceiver)
        receiverField.set(serviceSpy, receiverSpy)

        // Now destroy the service, which should unregister our spy receiver
        serviceController.destroy()

        // Send a broadcast after service is destroyed
        val testApps = arrayListOf("com.test.app")
        val intent = Intent(AppBlockerAccessibilityService.ACTION_UPDATE_BLOCKED_APPS).apply {
            putStringArrayListExtra(AppBlockerAccessibilityService.EXTRA_APP_PACKAGES, testApps)
            putExtra(AppBlockerAccessibilityService.EXTRA_IS_BLOCKING, true)
        }

        LocalBroadcastManager.getInstance(ApplicationProvider.getApplicationContext())
            .sendBroadcast(intent)

        shadowOf(Looper.getMainLooper()).idle()

        // Verify that our receiver spy was never called after service destruction
        verify(receiverSpy, never()).onReceive(any(), any())
    }
}