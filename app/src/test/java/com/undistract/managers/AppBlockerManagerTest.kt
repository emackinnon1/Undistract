package com.undistract.managers

import android.app.AppOpsManager
import android.content.Context
import android.os.Build
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
import android.provider.Settings
import org.mockito.ArgumentCaptor
import com.undistract.data.models.Profile


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

    @Test
    fun blockingStateManagement_loadsAndPersistsState() {
        // Set up shared preferences mock
        val editor = Mockito.mock(android.content.SharedPreferences.Editor::class.java)
        val prefs = Mockito.mock(android.content.SharedPreferences::class.java)

        `when`(context.getSharedPreferences("app_blocker_prefs", Context.MODE_PRIVATE)).thenReturn(prefs)
        `when`(prefs.edit()).thenReturn(editor)
        `when`(editor.putBoolean(Mockito.anyString(), Mockito.anyBoolean())).thenReturn(editor)

        // Test initial loading - set SharedPreferences to return true for isBlocking
        `when`(prefs.getBoolean("isBlocking", false)).thenReturn(true)

        // Create manager which will load state in init
        appBlockerManager = AppBlockerManager(context)

        // Verify initial state was loaded correctly
        assertEquals(true, appBlockerManager.isBlocking.value)

        // Test saving state - change the state and verify SharedPreferences was updated
        appBlockerManager.setBlockingState(false)

        // Verify the new state
        assertEquals(false, appBlockerManager.isBlocking.value)

        // Verify the state was saved to SharedPreferences
        Mockito.verify(editor).putBoolean("isBlocking", false)
        Mockito.verify(editor).apply()
    }

    @Test
    fun toggleBlocking_whenAuthorized_togglesStateAndAppliesSettings() {
        // Set up mock Profile
        val profile = Mockito.mock(Profile::class.java)
        val blockedApps = listOf("com.example.app1", "com.example.app2")
        `when`(profile.appPackageNames).thenReturn(blockedApps)

        // Set up SharedPreferences mock
        val editor = Mockito.mock(android.content.SharedPreferences.Editor::class.java)
        val prefs = Mockito.mock(android.content.SharedPreferences::class.java)
        `when`(context.getSharedPreferences("app_blocker_prefs", Context.MODE_PRIVATE)).thenReturn(prefs)
        `when`(prefs.edit()).thenReturn(editor)
        `when`(editor.putBoolean(Mockito.anyString(), Mockito.anyBoolean())).thenReturn(editor)

        // Set initial isBlocking to false
        `when`(prefs.getBoolean("isBlocking", false)).thenReturn(false)

        // Set up authorization to be true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            `when`(appOpsManager.unsafeCheckOpNoThrow(
                Mockito.eq(AppOpsManager.OPSTR_GET_USAGE_STATS),
                Mockito.anyInt(),
                Mockito.anyString()
            )).thenReturn(AppOpsManager.MODE_ALLOWED)
        } else {
            `when`(appOpsManager.checkOpNoThrow(
                Mockito.eq(AppOpsManager.OPSTR_GET_USAGE_STATS),
                Mockito.anyInt(),
                Mockito.anyString()
            )).thenReturn(AppOpsManager.MODE_ALLOWED)
        }

        Mockito.mockStatic(Settings::class.java).use { settingsMock ->
            settingsMock.`when`<Boolean> { Settings.canDrawOverlays(context) }.thenReturn(true)

            // Initialize manager
            appBlockerManager = AppBlockerManager(context)

            // Act - toggle blocking on
            appBlockerManager.toggleBlocking(profile)

            // Assert - state toggled to true
            assertEquals(true, appBlockerManager.isBlocking.value)

            // Verify state was saved to SharedPreferences
            Mockito.verify(editor).putBoolean("isBlocking", true)
            Mockito.verify(editor).apply()

            // Verify service was started with correct apps
            val intentCaptor = ArgumentCaptor.forClass(Intent::class.java)
            Mockito.verify(context).startService(intentCaptor.capture())
            val capturedIntent = intentCaptor.value

            // Check the blocked apps list was passed correctly
            val capturedApps = capturedIntent.getSerializableExtra("BLOCKED_APPS") as ArrayList<*>
            assertEquals(blockedApps, capturedApps)

            // Act again - toggle blocking off
            appBlockerManager.toggleBlocking(profile)

            // Assert - state toggled to false
            assertEquals(false, appBlockerManager.isBlocking.value)

            // Verify service was stopped
            Mockito.verify(context).stopService(Mockito.any(Intent::class.java))
        }
    }

    @Test
    fun setBlockingState_updatesStateAndPersistsIt() {
        // Set up shared preferences mock
        val editor = Mockito.mock(android.content.SharedPreferences.Editor::class.java)
        val prefs = Mockito.mock(android.content.SharedPreferences::class.java)

        `when`(context.getSharedPreferences("app_blocker_prefs", Context.MODE_PRIVATE)).thenReturn(prefs)
        `when`(prefs.edit()).thenReturn(editor)
        `when`(editor.putBoolean(Mockito.anyString(), Mockito.anyBoolean())).thenReturn(editor)

        // Initialize with a known state (false)
        `when`(prefs.getBoolean("isBlocking", false)).thenReturn(false)
        appBlockerManager = AppBlockerManager(context)

        // Initial state should be false
        assertEquals(false, appBlockerManager.isBlocking.value)

        // Act - set blocking state to true
        appBlockerManager.setBlockingState(true)

        // Assert - state is updated
        assertEquals(true, appBlockerManager.isBlocking.value)

        // Verify state was persisted to SharedPreferences (first call)
        Mockito.verify(editor).putBoolean("isBlocking", true)
        Mockito.verify(editor, Mockito.times(1)).apply()

        // Act again - set blocking state to false
        appBlockerManager.setBlockingState(false)

        // Assert - state is updated again
        assertEquals(false, appBlockerManager.isBlocking.value)

        // Verify state was persisted to SharedPreferences again (now a total of 2 calls)
        Mockito.verify(editor).putBoolean("isBlocking", false)
        Mockito.verify(editor, Mockito.times(2)).apply()
    }

    @Test
    fun applyBlockingSettings_startsOrStopsServiceBasedOnState() {
        // Set up mock Profile with blocked apps
        val profile = Mockito.mock(Profile::class.java)
        val blockedApps = listOf("com.example.app1", "com.example.app2")
        `when`(profile.appPackageNames).thenReturn(blockedApps)

        // Set up SharedPreferences mock
        val editor = Mockito.mock(android.content.SharedPreferences.Editor::class.java)
        val prefs = Mockito.mock(android.content.SharedPreferences::class.java)
        `when`(context.getSharedPreferences("app_blocker_prefs", Context.MODE_PRIVATE)).thenReturn(prefs)
        `when`(prefs.edit()).thenReturn(editor)
        `when`(editor.putBoolean(Mockito.anyString(), Mockito.anyBoolean())).thenReturn(editor)

        // Test case 1: Initialize with blocking disabled
        `when`(prefs.getBoolean("isBlocking", false)).thenReturn(false)
        appBlockerManager = AppBlockerManager(context)

        // Apply settings with blocking disabled - should stop service
        appBlockerManager.applyBlockingSettings(profile)

        // Verify service was stopped and not started
        Mockito.verify(context).stopService(Mockito.any(Intent::class.java))
        Mockito.verify(context, Mockito.never()).startService(Mockito.any(Intent::class.java))

        // Reset verification counts
        Mockito.clearInvocations(context)

        // Test case 2: Change to blocking enabled
        appBlockerManager.setBlockingState(true)

        // Apply settings with blocking enabled - should start service
        appBlockerManager.applyBlockingSettings(profile)

        // Verify service was started with correct apps list
        val intentCaptor = ArgumentCaptor.forClass(Intent::class.java)
        Mockito.verify(context).startService(intentCaptor.capture())
        val capturedIntent = intentCaptor.value

        // Check the blocked apps list was passed correctly
        val capturedApps = capturedIntent.getSerializableExtra("BLOCKED_APPS") as ArrayList<*>
        assertEquals(blockedApps, capturedApps)

        // Verify service was not stopped again
        Mockito.verify(context, Mockito.never()).stopService(Mockito.any(Intent::class.java))
    }
}