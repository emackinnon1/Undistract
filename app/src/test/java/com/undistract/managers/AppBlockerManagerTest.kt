//The AppBlockerManager class provides the following areas of functionality:
//2. Authorization Requesting: Launches system activities to request the necessary permissions from the user.
//3. Blocking State Management: Maintains and persists the blocking state (enabled/disabled) using SharedPreferences.
//4. Blocking Toggle: Toggles the blocking state and applies blocking settings for a given profile.
//5. Blocking State Setting: Explicitly sets the blocking state and persists it.
//6. Blocking Settings Application: Starts or stops the blocking service with the list of blocked apps based on the current state.



package com.undistract.managers

import android.app.AppOpsManager
import android.content.Context
import android.os.Build
//import android.provider.Settings
import com.undistract.UndistractApp
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import org.mockito.ArgumentCaptor


@RunWith(RobolectricTestRunner::class)
@Config(application = UndistractApp::class, sdk = [34])
class AppBlockerManagerTest {

    private lateinit var context: Context
    private lateinit var appOpsManager: AppOpsManager
    private lateinit var appBlockerManager: AppBlockerManager

    @Before
    fun setup() {
        context = Mockito.mock(Context::class.java)
        appOpsManager = Mockito.mock(AppOpsManager::class.java)
        `when`(context.getSystemService(Context.APP_OPS_SERVICE)).thenReturn(appOpsManager)
        `when`(context.packageName).thenReturn("com.undistract")
        // Mock SharedPreferences for init
        val prefs = Mockito.mock(android.content.SharedPreferences::class.java)
        `when`(context.getSharedPreferences(Mockito.anyString(), Mockito.anyInt())).thenReturn(prefs)
        `when`(prefs.getBoolean(Mockito.anyString(), Mockito.anyBoolean())).thenReturn(false)
    }

    @Test
    fun checkAuthorization_permissionsGranted_setsIsAuthorizedTrue() {
        // Simulate permission granted
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            `when`(
                appOpsManager.unsafeCheckOpNoThrow(
                    Mockito.eq(AppOpsManager.OPSTR_GET_USAGE_STATS),
                    Mockito.anyInt(),
                    Mockito.anyString()
                )
            ).thenReturn(AppOpsManager.MODE_ALLOWED)
        } else {
            `when`(
                appOpsManager.checkOpNoThrow(
                    Mockito.eq(AppOpsManager.OPSTR_GET_USAGE_STATS),
                    Mockito.anyInt(),
                    Mockito.anyString()
                )
            ).thenReturn(AppOpsManager.MODE_ALLOWED)
        }
        // Simulate overlay permission granted
        Mockito.mockStatic(Settings::class.java).use { settingsMock ->
            settingsMock.`when`<Boolean> { Settings.canDrawOverlays(context) }.thenReturn(true)
            appBlockerManager = AppBlockerManager(context)
            appBlockerManager.checkAuthorization()
            assertEquals(true, appBlockerManager.isAuthorized.value)
        }
    }

    @Test
    fun requestAuthorization_launchesCorrectSystemActivities() {
        // Arrange
        appBlockerManager = AppBlockerManager(context)

        // Act
        appBlockerManager.requestAuthorization()

        // Assert
        // Capture the intents passed to startActivity
        val intentCaptor = ArgumentCaptor.forClass(Intent::class.java)
        Mockito.verify(context, Mockito.times(2)).startActivity(intentCaptor.capture())

        val capturedIntents = intentCaptor.allValues

        // Verify the first intent (usage stats)
        val usageStatsIntent = capturedIntents[0]
        assertEquals(Settings.ACTION_USAGE_ACCESS_SETTINGS, usageStatsIntent.action)
        assertEquals(Intent.FLAG_ACTIVITY_NEW_TASK, usageStatsIntent.flags)

        // Verify the second intent (overlay permission)
        val overlayIntent = capturedIntents[1]
        assertEquals(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, overlayIntent.action)
        assertEquals(Intent.FLAG_ACTIVITY_NEW_TASK, overlayIntent.flags)
        assertEquals("package:com.undistract", overlayIntent.data.toString())
    }
}