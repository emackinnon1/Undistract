// DONE
//1.Accessibility Service Management
//
//Checking if the accessibility service is enabled
//Prompting the user to enable the service if it's not enabled
// DONE
//2.NFC Handling
//
//Initializing NFC helper
//Processing new intents (likely NFC-related)
//Publishing intents to a StateFlow for consumption in the UI
// IN_PROGRESS
//3.UI Setup with Jetpack Compose
//
//Setting up the main UI with BlockerScreen
//Providing dependencies to the Compose UI

package com.undistract.ui.main

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.accessibility.AccessibilityManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.undistract.nfc.NfcHelper
import com.undistract.services.AppBlockerAccessibilityService
import com.undistract.ui.blocking.BlockerScreen
import io.mockk.*
import io.mockk.impl.annotations.MockK
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.Robolectric
import androidx.compose.runtime.Composable
import com.undistract.ui.blocking.BlockerScreen
import kotlinx.coroutines.flow.MutableStateFlow
import org.robolectric.android.controller.ActivityController
import org.robolectric.Shadows
import androidx.test.core.app.ApplicationProvider
import kotlin.text.get

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.UPSIDE_DOWN_CAKE])
class MainActivityTest {

    private lateinit var activity: MainActivity

    @MockK
    private lateinit var accessibilityManager: AccessibilityManager

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        activity = spyk(MainActivity())
        // TODO: Use Robolectric to create the activity instance
        // activity = Robolectric.buildActivity(MainActivity::class.java)

        // Mock getSystemService to return our mock AccessibilityManager
        every { activity.getSystemService(Context.ACCESSIBILITY_SERVICE) } returns accessibilityManager
    }

    @Test
    fun `onCreate passes correct dependencies to UI setup`() {
        // Arrange
        mockkObject(AppBlockerAccessibilityService)
        every { AppBlockerAccessibilityService.ensureAccessibilityServiceEnabled(any()) } just runs

        // Act - Use Robolectric (instead of the mocked activity) to properly test lifecycle methods
        val activityController = Robolectric.buildActivity(MainActivity::class.java)
        activityController.create() // This triggers the real onCreate() lifecycle method
        val createdActivity = activityController.get()

        // Assert
        assertNotNull(createdActivity.nfcHelper) // Verify nfcHelper was initialized
        assertNotNull(createdActivity.newIntentFlow) // Verify newIntentFlow was initialized

        // Clean up
        unmockkObject(AppBlockerAccessibilityService)
    }

    @Test
    fun `isAccessibilityServiceEnabled returns true when service is enabled`() {
        // Arrange
        val serviceInfo = mockk<AccessibilityServiceInfo>()
        every { serviceInfo.id } returns "com.undistract/.services.AppBlockerAccessibilityService"
        every { accessibilityManager.getEnabledAccessibilityServiceList(any()) } returns listOf(serviceInfo)

        // Act
        val result = activity.callPrivateFunc("isAccessibilityServiceEnabled")

        // Assert
        assert(result as Boolean)
    }

    @Test
    fun `isAccessibilityServiceEnabled returns false when service is not enabled`() {
        // Arrange
        val serviceInfo = mockk<AccessibilityServiceInfo>()
        every { serviceInfo.id } returns "com.other.service"
        every { accessibilityManager.getEnabledAccessibilityServiceList(any()) } returns listOf(serviceInfo)

        // Act
        val result = activity.callPrivateFunc("isAccessibilityServiceEnabled")

        // Assert
        assert(!(result as Boolean))
    }

    @Test
    fun `isAccessibilityServiceEnabled returns false when no services are enabled`() {
        // Arrange
        every { accessibilityManager.getEnabledAccessibilityServiceList(any()) } returns emptyList()

        // Act
        val result = activity.callPrivateFunc("isAccessibilityServiceEnabled")

        // Assert
        assert(!(result as Boolean))
    }

    @Test
    fun `promptEnableAccessibilityService calls AppBlockerAccessibilityService ensureEnabled`() {
        // Arrange
        mockkObject(AppBlockerAccessibilityService)
        every { AppBlockerAccessibilityService.ensureAccessibilityServiceEnabled(any()) } just runs

        // Act
        activity.callPrivateFunc("promptEnableAccessibilityService")

        // Assert
        verify(exactly = 1) { AppBlockerAccessibilityService.ensureAccessibilityServiceEnabled(activity) }

        unmockkObject(AppBlockerAccessibilityService)
    }

    @Test
    fun `onCreate initializes NfcHelper`() {
        // Arrange
        mockkObject(AppBlockerAccessibilityService)
        every { AppBlockerAccessibilityService.ensureAccessibilityServiceEnabled(any()) } just runs

        // Act - Use Robolectric (instead of the mocked activity) to properly test lifecycle methods
        val activityController = Robolectric.buildActivity(MainActivity::class.java)
        activityController.create() // This triggers the real onCreate() lifecycle method
        val createdActivity = activityController.get()

        // Assert
        assertNotNull(createdActivity.nfcHelper)

        // Clean up
        unmockkObject(AppBlockerAccessibilityService)
    }

    @Test
    fun `onNewIntent sets intent and updates newIntentFlow`() {
        // Arrange
        val testIntent = mockk<Intent>()
        every { activity.setIntent(any()) } just runs

        // Act
        val method = MainActivity::class.java.getDeclaredMethod("onNewIntent", Intent::class.java)
        method.isAccessible = true
        method.invoke(activity, testIntent)

        // Assert
        verify { activity.setIntent(testIntent) }
        assert(activity.newIntentFlow.value == testIntent)
    }

    @Test
    fun `newIntentFlow initially has null value`() {
        // Assert
        assert(activity.newIntentFlow.value == null)
    }

    // Extension function to access private functions for testing
    private fun MainActivity.callPrivateFunc(name: String): Any? {
        val method = MainActivity::class.java.getDeclaredMethod(name)
        method.isAccessible = true
        return method.invoke(this)
    }
}