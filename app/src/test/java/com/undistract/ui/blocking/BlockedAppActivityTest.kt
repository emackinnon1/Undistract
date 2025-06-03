package com.undistract.ui.blocking

import android.os.Build
import android.os.Looper
import androidx.test.core.app.ActivityScenario
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.robolectric.annotation.Config
import java.util.concurrent.TimeUnit
import androidx.lifecycle.Lifecycle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.UPSIDE_DOWN_CAKE])
class BlockedAppActivityTest {

    @Test
    fun `activity should finish after delay`() {
        // Launch the activity
        val scenario = ActivityScenario.launch(BlockedAppActivity::class.java)

        // Verify activity is not finished immediately
        scenario.onActivity { activity ->
            assertFalse("Activity should not finish immediately", activity.isFinishing)

            // Advance the main looper by slightly more than the delay time
            val shadowLooper = shadowOf(Looper.getMainLooper())
            shadowLooper.idleFor(550L, TimeUnit.MILLISECONDS)

            // Now verify activity is finished
            assertTrue("Activity should be finished after delay", activity.isFinishing)
        }
    }

    @Test
    fun `activity follows standard lifecycle`() {
        // Launch the activity
        val scenario = ActivityScenario.launch(BlockedAppActivity::class.java)

        // Verify activity is created successfully and in expected state
        assertEquals(Lifecycle.State.RESUMED, scenario.state)

        // Verify activity instance is accessible and properly initialized
        scenario.onActivity { activity ->
            assertNotNull("Activity should be properly instantiated", activity)
            assertNotNull("Activity window should be created", activity.window)
            assertNotNull("Activity content should be available", activity.window.decorView)
        }

        // Test moving through standard lifecycle states
        scenario.moveToState(Lifecycle.State.STARTED)
        assertEquals(Lifecycle.State.STARTED, scenario.state)

        scenario.moveToState(Lifecycle.State.CREATED)
        assertEquals(Lifecycle.State.CREATED, scenario.state)

        // Destroy activity - should not crash
        scenario.moveToState(Lifecycle.State.DESTROYED)
    }
}