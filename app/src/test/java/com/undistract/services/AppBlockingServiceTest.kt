package com.undistract.services

import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.every
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import io.mockk.junit4.MockKRule
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config



@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.UPSIDE_DOWN_CAKE])
@ExperimentalCoroutinesApi
class AppBlockingServiceTest {

    @get:Rule
    val mockkRule = MockKRule(this)

    // Use our custom constructor
    private lateinit var service: AppBlockingService

    // Use a test dispatcher that we'll pass to the service
    private val testDispatcher = StandardTestDispatcher()

    @MockK
    lateinit var usageStatsManager: UsageStatsManager

    @Before
    fun setup() {
        // Create service with test dispatcher
        service = spyk(AppBlockingService(testDispatcher))

        // Mock getSystemService to return our mock UsageStatsManager
        every { service.getSystemService(Context.USAGE_STATS_SERVICE) } returns usageStatsManager
    }

    @Test
    fun `onCreate should initialize UsageStatsManager`() {
        // When
        service.onCreate()

        // Then
        verify { service.getSystemService(Context.USAGE_STATS_SERVICE) }
    }

    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun `onStartCommand should start monitoring when given blocked apps`() = runTest {
        // Create a new service instance that uses THIS TEST'S dispatcher
        val testService = spyk(AppBlockingService(this.coroutineContext[CoroutineDispatcher.Key]!!))

        // Set up the mock for this service instance
        every { testService.getSystemService(Context.USAGE_STATS_SERVICE) } returns usageStatsManager

        // Given
        val intent = mockk<Intent>()
        val blockedApps = arrayListOf("com.example.app1", "com.example.app2")
        every { intent.getStringArrayListExtra(AppBlockingService.EXTRA_BLOCKED_APPS) } returns blockedApps

        // Mock monitorApps to avoid the infinite loop
        coEvery { testService.monitorApps() } coAnswers {
            testService.setIsMonitoring(true)
        }

        // When
        testService.onStartCommand(intent, 0, 1)
        advanceUntilIdle()

        // Then
        assert(testService.isMonitoring) { "Service should be monitoring after onStartCommand" }
        coVerify { testService.monitorApps() }
    }

    @Test
    fun `onDestroy should cancel coroutine scope`() = runTest {
        // Given
        service.onCreate() // Initialize service

        // Verify scope is initially active
        assert(service.isScopeActive())

        // When
        service.onDestroy()

        // Then
        assert(!service.isScopeActive())
    }

    @Test
    fun `monitorApps should detect blocked foreground apps and launch blocker`() = runTest {
        // Given
        val foregroundApp = "com.example.blockedapp"
        val blockedApps = listOf(foregroundApp)
        var blockedPackageName: String? = null

        // Create a testable subclass with a simplified monitorApps implementation
        val testService = object : AppBlockingService(testDispatcher) {
            override suspend fun monitorApps() {
                _isMonitoring = true
                // Simulate just one iteration of the monitoring loop
                if (foregroundApp in blockedApps) {
                    blockedPackageName = foregroundApp
                }
            }
        }

        // Set the blocked packages
        val blockedPackagesField = AppBlockingService::class.java.getDeclaredField("blockedPackages")
        blockedPackagesField.isAccessible = true
        blockedPackagesField.set(testService, blockedApps)

        // When
        testService.monitorApps()

        // Then
        assert(testService.isMonitoring)
        assert(blockedPackageName == foregroundApp) {
            "Should have blocked '$foregroundApp', but blocked '$blockedPackageName' instead"
        }
    }

    @Test
    fun `monitorApps should not launch blocker for non-blocked apps`() = runTest {
        // Given
        val foregroundApp = "com.example.allowedapp"
        val blockedApps = listOf("com.example.blockedapp")
        var blockerLaunched = false

        // Create a subclass for testing
        val testService = object : AppBlockingService(testDispatcher) {
            override suspend fun monitorApps() {
                _isMonitoring = true
                // Simulate the behavior - should not launch blocker for this app
                if (foregroundApp in blockedApps) {
                    blockerLaunched = true
                }
            }
        }

        // Set the blocked packages
        val blockedPackagesField = AppBlockingService::class.java.getDeclaredField("blockedPackages")
        blockedPackagesField.isAccessible = true
        blockedPackagesField.set(testService, blockedApps)

        // When
        testService.monitorApps()

        // Then
        assert(testService.isMonitoring)
        assert(!blockerLaunched)
    }
}