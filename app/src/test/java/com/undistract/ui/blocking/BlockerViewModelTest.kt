package com.undistract.ui.blocking

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.undistract.UndistractApp
import com.undistract.data.entities.NfcTagEntity
import com.undistract.data.models.NfcTag
import com.undistract.data.local.UndistractDatabase
import com.undistract.data.models.Profile
import com.undistract.managers.AppBlockerManager
import com.undistract.managers.ProfileManager
import com.undistract.services.AppBlockerAccessibilityService
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.json.JSONArray
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.junit.Ignore


@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.UPSIDE_DOWN_CAKE])
class BlockerViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = TestCoroutineDispatcher()

    @Mock
    private lateinit var undistractApp: UndistractApp

    @Mock
    private lateinit var sharedPreferences: SharedPreferences

    @Mock
    private lateinit var editor: SharedPreferences.Editor

    private lateinit var db: UndistractDatabase
    private lateinit var nfcTagDao: com.undistract.data.daos.NfcTagDao

    private lateinit var viewModel: BlockerViewModel

    // Mock objects for UndistractApp statics
    private val mockAppBlocker = mockk<AppBlockerManager>(relaxed = true)
    private val mockProfileManager = mockk<ProfileManager>(relaxed = true)
    private val blockingStateFlow = MutableStateFlow(false)
    private val profileStateFlow = MutableStateFlow<Profile?>(null)

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        clearAllMocks() // Clear any previous mock interactions

        // Reset state flows
        blockingStateFlow.value = false
        profileStateFlow.value = null

        // Set up SharedPreferences mock
        `when`(undistractApp.getSharedPreferences(eq("nfc_tags"), eq(Context.MODE_PRIVATE)))
            .thenReturn(sharedPreferences)
        `when`(sharedPreferences.edit()).thenReturn(editor)
        `when`(editor.putString(anyString(), anyString())).thenReturn(editor)
        doNothing().`when`(editor).apply()

        // Mock AppBlockerAccessibilityService.Companion
        mockkObject(AppBlockerAccessibilityService.Companion)
        every { AppBlockerAccessibilityService.ensureAccessibilityServiceEnabled(any()) } returns Unit

        // Set up in-memory Room database
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            UndistractDatabase::class.java
        ).allowMainThreadQueries().build()
        nfcTagDao = db.nfcTagDao()

        // Mock UndistractApp static properties
        mockkObject(UndistractApp.Companion)
        mockkObject(UndistractApp.Companion)
        every { UndistractApp.db } returns db
        every { UndistractApp.appBlocker } returns mockAppBlocker
        every { UndistractApp.profileManager } returns mockProfileManager
        every { UndistractApp.instance } returns undistractApp

        // Setup state flows
        every { mockAppBlocker.isBlocking } returns blockingStateFlow
        every { mockProfileManager.currentProfile } returns profileStateFlow

        // Set main dispatcher for coroutines
        Dispatchers.setMain(testDispatcher)

        // Initialize view model
        viewModel = BlockerViewModel(undistractApp)
    }

    @After
    fun tearDown() {
        db.close()
        blockingStateFlow.value = false
        profileStateFlow.value = null
        Dispatchers.resetMain()
        testDispatcher.cleanupTestCoroutines()
    }

    @Test
    fun `loadSavedTags should load tags correctly`() {
        // Arrange
        viewModel.saveTag("UNDISTRACT-123")
        viewModel.saveTag("UNDISTRACT-456")

        // Act - Create a new ViewModel to trigger loadSavedTags in init
        val newViewModel = BlockerViewModel(undistractApp)

        // Assert
        assertEquals(2, newViewModel.writtenTags.value.size)
        assertEquals("UNDISTRACT-123", newViewModel.writtenTags.value[1].id)
        assertEquals("UNDISTRACT-456", newViewModel.writtenTags.value[0].id)
    }

    @Test
    fun `saveTag should add tag to list`() {
        // Act
        assertEquals(0, viewModel.writtenTags.value.size)
        viewModel.saveTag("UNDISTRACT-TEST")
        // Wait for coroutine to finish (since saveTag is async)
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        assertEquals(1, viewModel.writtenTags.value.size)
        assertEquals("UNDISTRACT-TEST", viewModel.writtenTags.value[0].id)
        assertEquals("profile_tag", viewModel.writtenTags.value[0].payload)
    }

    @Test
    fun `deleteTag should remove tag from list and update SharedPreferences`() {
        // Arrange - Initialize with existing tags
        viewModel.saveTag("UNDISTRACT-123")
        viewModel.saveTag("UNDISTRACT-456")

        val testViewModel = BlockerViewModel(undistractApp)
        val tag = testViewModel.writtenTags.value.filter { it.id == "UNDISTRACT-123" }[0]

        // Act
        testViewModel.deleteTag(tag)

        // Assert
        assertEquals(1, testViewModel.writtenTags.value.size)
        assertEquals("UNDISTRACT-456", testViewModel.writtenTags.value[0].id)

    }

    @Test
    fun `scanTag with valid tag prefix should trigger blocking toggle`() {
        // Arrange
        val profile = Profile(name = "Test", appPackageNames = listOf("com.example"))
        profileStateFlow.value = profile
        blockingStateFlow.value = false  // Starting with blocking disabled

        // Act
        viewModel.scanTag("UNDISTRACT-valid-tag")
        testDispatcher.scheduler.advanceUntilIdle()  // Wait for coroutine

        // Assert
        assertEquals(false, viewModel.showScanTagAlert.value)
        io.mockk.verify { mockAppBlocker.setBlockingState(true) }  // Verify exact parameter
    }
    @Ignore
    @Test
    fun `scanTag with invalid tag should show wrong tag alert`() {
        // Arrange
        val invalidTagPayload = "INVALID-TAG"

        // Act
        viewModel.scanTag(invalidTagPayload)

        // Assert
        assertTrue(viewModel.showWrongTagAlert.value)
    }
    @Ignore
    @Test
    fun `toggleBlocking should start BlockerService when enabling blocking`() {
        // Arrange
        val appPackages = listOf("com.example.app1", "com.example.app2")
        val profile = Profile(name = "Test", appPackageNames = appPackages)
        profileStateFlow.value = profile
        blockingStateFlow.value = false // Currently not blocking

        // Act
        viewModel.scanTag("UNDISTRACT-valid-tag")

        // Assert
        // Verify the accessibility service is checked
        io.mockk.verify { AppBlockerAccessibilityService.ensureAccessibilityServiceEnabled(any()) }
        // Verify the blocking state is set to true
        io.mockk.verify { mockAppBlocker.setBlockingState(true) }
    }
    @Ignore
    @Test
    fun `toggleBlocking should stop BlockerService when disabling blocking`() {
        // Arrange
        val appPackages = listOf("com.example.app1", "com.example.app2")
        val profile = Profile(name = "Test", appPackageNames = appPackages)
        profileStateFlow.value = profile
        blockingStateFlow.value = true // Currently blocking

        // Act
        viewModel.scanTag("UNDISTRACT-valid-tag")

        // Assert
        io.mockk.verify { mockAppBlocker.setBlockingState(false) }
        // Same note about verifying Intent
    }
    @Ignore
    @Test
    fun `toggleBlocking should check accessibility service when enabling blocking`() {
        // Arrange
        val appPackages = listOf("com.example.app1", "com.example.app2")
        val profile = Profile(name = "Test", appPackageNames = appPackages)
        profileStateFlow.value = profile
        blockingStateFlow.value = false // Currently not blocking

        // Act
        viewModel.scanTag("UNDISTRACT-valid-tag")

        // Assert
        io.mockk.verify { AppBlockerAccessibilityService.ensureAccessibilityServiceEnabled(any()) }
    }
    @Ignore
    @Test
    fun `toggleBlocking should do nothing if no profile is selected`() {
        // Arrange
        profileStateFlow.value = null
        blockingStateFlow.value = false

        // Act
        viewModel.scanTag("UNDISTRACT-valid-tag")

        // Assert
        io.mockk.verify(exactly = 0) { mockAppBlocker.setBlockingState(any()) }
        io.mockk.verify(exactly = 0) { AppBlockerAccessibilityService.ensureAccessibilityServiceEnabled(any()) }
    }
    @Ignore
    @Test
    fun `showScanTagAlert should set showScanTagAlert state to true`() {
        // Arrange - initial state should be false
        assertEquals(false, viewModel.showScanTagAlert.value)

        // Act
        viewModel.showScanTagAlert()

        // Assert
        assertEquals(true, viewModel.showScanTagAlert.value)
    }
    @Ignore
    @Test
    fun `dismissScanTagAlert should set showScanTagAlert state to false`() {
        // Arrange - set initial state to true
        viewModel.showScanTagAlert()
        assertEquals(true, viewModel.showScanTagAlert.value)

        // Act
        viewModel.dismissScanTagAlert()

        // Assert
        assertEquals(false, viewModel.showScanTagAlert.value)
    }
    @Ignore
    @Test
    fun `showWrongTagAlert should set showWrongTagAlert state to true`() {
        // Arrange - initial state should be false
        assertEquals(false, viewModel.showWrongTagAlert.value)

        // Act
        viewModel.showWrongTagAlert()

        // Assert
        assertEquals(true, viewModel.showWrongTagAlert.value)
    }
    @Ignore
    @Test
    fun `dismissWrongTagAlert should set showWrongTagAlert state to false`() {
        // Arrange - set initial state to true
        viewModel.showWrongTagAlert()
        assertEquals(true, viewModel.showWrongTagAlert.value)

        // Act
        viewModel.dismissWrongTagAlert()

        // Assert
        assertEquals(false, viewModel.showWrongTagAlert.value)
    }
    @Ignore
    @Test
    fun `showCreateTagAlert should set showCreateTagAlert state to true`() {
        // Arrange - initial state should be false
        assertEquals(false, viewModel.showCreateTagAlert.value)

        // Act
        viewModel.showCreateTagAlert()

        // Assert
        assertEquals(true, viewModel.showCreateTagAlert.value)
    }
    @Ignore
    @Test
    fun `hideCreateTagAlert should set showCreateTagAlert state to false`() {
        // Arrange - set initial state to true
        viewModel.showCreateTagAlert()
        assertEquals(true, viewModel.showCreateTagAlert.value)

        // Act
        viewModel.hideCreateTagAlert()

        // Assert
        assertEquals(false, viewModel.showCreateTagAlert.value)
    }
    @Ignore
    @Test
    fun `setWritingTag should update isWritingTag state`() {
        // Arrange - initial state should be false
        assertEquals(false, viewModel.isWritingTag.value)

        // Act
        viewModel.setWritingTag(true)

        // Assert
        assertEquals(true, viewModel.isWritingTag.value)
    }
    @Ignore
    @Test
    fun `cancelWrite should set isWritingTag state to false`() {
        // Arrange - set initial state to true
        viewModel.setWritingTag(true)
        assertEquals(true, viewModel.isWritingTag.value)

        // Act
        viewModel.cancelWrite()

        // Assert
        assertEquals(false, viewModel.isWritingTag.value)
    }
    @Ignore
    @Test
    fun `onCreateTagConfirmed should update dialog states correctly`() {
        // Arrange - set initial state
        viewModel.showCreateTagAlert()
        assertEquals(true, viewModel.showCreateTagAlert.value)
        assertEquals(false, viewModel.nfcWriteDialogShown.value)

        // Act
        viewModel.onCreateTagConfirmed()

        // Assert
        assertEquals(false, viewModel.showCreateTagAlert.value)
        assertEquals(true, viewModel.nfcWriteDialogShown.value)
    }
    @Ignore
    @Test
    fun `onTagWriteResult should update states with success result`() {
        // Arrange - set initial state
        viewModel.onCreateTagConfirmed()
        assertEquals(true, viewModel.nfcWriteDialogShown.value)
        assertEquals(false, viewModel.nfcWriteSuccess.value)

        // Act
        viewModel.onTagWriteResult(true)

        // Assert
        assertEquals(false, viewModel.nfcWriteDialogShown.value)
        assertEquals(true, viewModel.nfcWriteSuccess.value)
    }
    @Ignore
    @Test
    fun `onTagWriteResult should update states with failure result`() {
        // Arrange - set initial state
        viewModel.onCreateTagConfirmed()
        assertEquals(true, viewModel.nfcWriteDialogShown.value)

        // Act
        viewModel.onTagWriteResult(false)

        // Assert
        assertEquals(false, viewModel.nfcWriteDialogShown.value)
        assertEquals(false, viewModel.nfcWriteSuccess.value)
    }
    @Ignore
    @Test
    fun `dismissNfcWriteSuccessAlert should set nfcWriteSuccess to false`() {
        // Arrange - set initial state to success
        viewModel.onTagWriteResult(true)
        assertEquals(true, viewModel.nfcWriteSuccess.value)

        // Act
        viewModel.dismissNfcWriteSuccessAlert()

        // Assert
        assertEquals(false, viewModel.nfcWriteSuccess.value)
    }
    @Ignore
    @Test
    fun `dialog states are mutually exclusive`() {
        // Act - Show all dialogs one after another
        viewModel.showScanTagAlert()
        viewModel.showWrongTagAlert()
        viewModel.showCreateTagAlert()

        // Assert - Only the last shown dialog should be visible
        assertEquals(true, viewModel.showCreateTagAlert.value)
        assertEquals(false, viewModel.showScanTagAlert.value)
        assertEquals(false, viewModel.showWrongTagAlert.value)
    }
    @Ignore
    @Test
    fun `dismissing one dialog should not affect other dialog states`() {
        // Arrange - show create tag alert, then set writing tag
        viewModel.showCreateTagAlert()
        viewModel.setWritingTag(true)

        // Act
        viewModel.hideCreateTagAlert()

        // Assert - only the specific dialog should be dismissed
        assertEquals(false, viewModel.showCreateTagAlert.value)
        assertEquals(true, viewModel.isWritingTag.value)
    }
    @Ignore
    @Test
    fun `dialog states should reset after failed write operation`() {
        // Arrange - setup write operation flow
        viewModel.showCreateTagAlert()
        viewModel.onCreateTagConfirmed()

        // Act - simulate write failure
        viewModel.onTagWriteResult(false)

        // Assert - all dialog states should be properly reset
        assertEquals(false, viewModel.nfcWriteDialogShown.value)
        assertEquals(false, viewModel.showCreateTagAlert.value)
        assertEquals(false, viewModel.nfcWriteSuccess.value)
        assertEquals(false, viewModel.isWritingTag.value)
    }
    @Ignore
    @Test
    fun `complete tag write workflow should transition states correctly`() {
        // Test the entire workflow from start to finish

        // Start with all states false
        assertEquals(false, viewModel.showCreateTagAlert.value)
        assertEquals(false, viewModel.nfcWriteDialogShown.value)
        assertEquals(false, viewModel.nfcWriteSuccess.value)
        assertEquals(false, viewModel.isWritingTag.value)

        // Step 1: Show create tag dialog
        viewModel.showCreateTagAlert()
        assertEquals(true, viewModel.showCreateTagAlert.value)

        // Step 2: Confirm tag creation
        viewModel.onCreateTagConfirmed()
        assertEquals(false, viewModel.showCreateTagAlert.value)
        assertEquals(true, viewModel.nfcWriteDialogShown.value)

        // Step 3: Start writing
        viewModel.setWritingTag(true)
        assertEquals(true, viewModel.isWritingTag.value)

        // Step 4: Complete write successfully
        viewModel.onTagWriteResult(true)
        assertEquals(false, viewModel.nfcWriteDialogShown.value)
        assertEquals(true, viewModel.nfcWriteSuccess.value)

        // Step 5: Dismiss success notification
        viewModel.dismissNfcWriteSuccessAlert()
        assertEquals(false, viewModel.nfcWriteSuccess.value)
    }
    @Ignore
    @Test
    fun `canceling write operation should reset related states`() {
        // Arrange
        viewModel.setWritingTag(true)
        assertEquals(true, viewModel.isWritingTag.value)

        // Act
        viewModel.cancelWrite()

        // Assert
        assertEquals(false, viewModel.isWritingTag.value)
    }
    @Ignore
    @Test
    fun `generateUniqueTagPayload should follow correct format`() {
        // Act
        val payload = viewModel.generateUniqueTagPayload()

        // Assert
        // Format should be UNDISTRACT-timestamp-random
        val parts = payload.split("-")
        assertEquals(3, parts.size)
        assertEquals("UNDISTRACT", parts[0])

        // Second part should be a numeric timestamp
        val timestamp = parts[1].toLongOrNull()
        assertTrue("Timestamp should be a valid number", timestamp != null)

        // Third part should be a 4-digit number
        val random = parts[2].toIntOrNull()
        assertTrue("Random part should be a valid number", random != null)
        assertTrue("Random part should be at least 1000", random!! >= 1000)
        assertTrue("Random part should be less than 10000", random < 10000)
    }
    @Ignore
    @Test
    fun `generateUniqueTagPayload should create unique tags`() {
        // Act
        val payload1 = viewModel.generateUniqueTagPayload()
        val payload2 = viewModel.generateUniqueTagPayload()
        val payload3 = viewModel.generateUniqueTagPayload()

        // Assert
        // All generated payloads should be different
        assertTrue(payload1 != payload2)
        assertTrue(payload1 != payload3)
        assertTrue(payload2 != payload3)
    }
    @Ignore
    @Test
    fun `generateUniqueTagPayload should create valid tag for scanning`() {
        // Act
        val payload = viewModel.generateUniqueTagPayload()

        // Assert
        // Generated payload should be recognized as a valid tag when scanning
        assertTrue(payload.startsWith("UNDISTRACT"))

        // Test that this tag would be recognized by the scanTag method
        viewModel.scanTag(payload)
        // If it's valid, it would trigger blocking toggle, not wrong tag alert
        assertEquals(false, viewModel.showWrongTagAlert.value)
    }
    @Ignore
    @Test
    fun `generateUniqueTagPayload components should be extractable`() {
        // Act
        val payload = viewModel.generateUniqueTagPayload()
        val parts = payload.split("-")

        // Assert we can extract individual components
        val prefix = parts[0]
        val timestamp = parts[1].toLong()
        val random = parts[2].toInt()

        assertEquals("UNDISTRACT", prefix)
        assertTrue(timestamp > 0)
        assertTrue(random in 1000..9999)

        // Components should combine back to original
        assertEquals(payload, "$prefix-$timestamp-$random")
    }
}