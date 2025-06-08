//Based on the `BlockerViewModel` code, here are the main functional areas to test:
// DONE
//1. **Tag Management**
//   - Loading saved tags from SharedPreferences
//   - Saving new NFC tags
//   - Deleting tags
//   - Scanning and validating tags
// DONE
//2. **App Blocking Control**
//   - Toggling blocking state
//   - Starting the BlockerService with appropriate app packages
//   - Stopping the BlockerService
// DONE
//3. **Dialog State Management**
//   - Managing various alert dialogs (scan tag, wrong tag, create tag)
//   - NFC write process states (writing, success/failure)
//
//4. **Tag Generation**
//   - Creating unique tag payloads with proper formatting

//To test this ViewModel effectively, we'll need to mock several dependencies:
//- SharedPreferences
//- AppBlockerManager
//- ProfileManager
//- Application context
//
//Let me know which area you'd like to start testing first, and we can create the appropriate test cases.

package com.undistract.ui.blocking

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.undistract.UndistractApp
import com.undistract.data.models.NfcTag
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

        // Mock AppBlockerAccessibilityService.Companion - ADD THIS
        mockkObject(AppBlockerAccessibilityService.Companion)
        every { AppBlockerAccessibilityService.ensureAccessibilityServiceEnabled(any()) } returns Unit

        // Mock UndistractApp static properties
        mockkObject(UndistractApp.Companion)
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
        blockingStateFlow.value = false
        profileStateFlow.value = null
        Dispatchers.resetMain()
        testDispatcher.cleanupTestCoroutines()
    }

    @Test
    fun `loadSavedTags with empty preferences should return empty list`() {
        // Arrange
        `when`(sharedPreferences.getString(eq("nfc_tags"), isNull())).thenReturn(null)

        // Act - loadSavedTags is called in the init block of the ViewModel

        // Assert
        assertEquals(emptyList<NfcTag>(), viewModel.writtenTags.value)
    }


    @Test
    fun `loadSavedTags with valid JSON should load tags correctly`() {
        // Arrange
        val tag1 = NfcTag(payload = "UNDISTRACT-123")
        val tag2 = NfcTag(payload = "UNDISTRACT-456")
        val jsonArray = JSONArray()
        jsonArray.put(tag1.toJson())
        jsonArray.put(tag2.toJson())

        `when`(sharedPreferences.getString(eq("nfc_tags"), isNull())).thenReturn(jsonArray.toString())

        // Act - Create a new ViewModel to trigger loadSavedTags in init
        val newViewModel = BlockerViewModel(undistractApp)

        // Assert
        assertEquals(2, newViewModel.writtenTags.value.size)
        assertEquals("UNDISTRACT-123", newViewModel.writtenTags.value[0].payload)
        assertEquals("UNDISTRACT-456", newViewModel.writtenTags.value[1].payload)
    }

    @Test
    fun `saveTag should add tag to list and save to SharedPreferences`() {
        // Arrange
        `when`(sharedPreferences.getString(eq("nfc_tags"), isNull())).thenReturn(null)

        // Act
        viewModel.saveTag("UNDISTRACT-TEST")

        // Assert
        assertEquals(1, viewModel.writtenTags.value.size)
        assertEquals("UNDISTRACT-TEST", viewModel.writtenTags.value[0].payload)
        verify(editor).putString(eq("nfc_tags"), anyString())
        verify(editor).apply()
    }

    @Test
    fun `deleteTag should remove tag from list and update SharedPreferences`() {
        // Arrange - Initialize with existing tags
        val tag1 = NfcTag(payload = "UNDISTRACT-123")
        val tag2 = NfcTag(payload = "UNDISTRACT-456")
        val jsonArray = JSONArray()
        jsonArray.put(tag1.toJson())
        jsonArray.put(tag2.toJson())

        `when`(sharedPreferences.getString(eq("nfc_tags"), isNull())).thenReturn(jsonArray.toString())
        val testViewModel = BlockerViewModel(undistractApp)

        // Act
        testViewModel.deleteTag(tag1)

        // Assert
        assertEquals(1, testViewModel.writtenTags.value.size)
        assertEquals("UNDISTRACT-456", testViewModel.writtenTags.value[0].payload)
        verify(editor).putString(eq("nfc_tags"), anyString())
        verify(editor).apply()
    }

    @Test
    fun `scanTag with valid tag prefix should trigger blocking toggle`() {
        // Arrange
        val profile = Profile(name = "Test", appPackageNames = listOf("com.example"))
        profileStateFlow.value = profile

        // Act
        viewModel.scanTag("UNDISTRACT-valid-tag")

        // Assert
        assertEquals(false, viewModel.showScanTagAlert.value)
        // Using MockK to verify the interaction
        io.mockk.verify { mockAppBlocker.setBlockingState(any()) }
    }

    @Test
    fun `scanTag with invalid tag should show wrong tag alert`() {
        // Arrange
        val invalidTagPayload = "INVALID-TAG"

        // Act
        viewModel.scanTag(invalidTagPayload)

        // Assert
        assertTrue(viewModel.showWrongTagAlert.value)
    }

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

    @Test
    fun `showScanTagAlert should set showScanTagAlert state to true`() {
        // Arrange - initial state should be false
        assertEquals(false, viewModel.showScanTagAlert.value)

        // Act
        viewModel.showScanTagAlert()

        // Assert
        assertEquals(true, viewModel.showScanTagAlert.value)
    }

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

    @Test
    fun `showWrongTagAlert should set showWrongTagAlert state to true`() {
        // Arrange - initial state should be false
        assertEquals(false, viewModel.showWrongTagAlert.value)

        // Act
        viewModel.showWrongTagAlert()

        // Assert
        assertEquals(true, viewModel.showWrongTagAlert.value)
    }

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

    @Test
    fun `showCreateTagAlert should set showCreateTagAlert state to true`() {
        // Arrange - initial state should be false
        assertEquals(false, viewModel.showCreateTagAlert.value)

        // Act
        viewModel.showCreateTagAlert()

        // Assert
        assertEquals(true, viewModel.showCreateTagAlert.value)
    }

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

    @Test
    fun `setWritingTag should update isWritingTag state`() {
        // Arrange - initial state should be false
        assertEquals(false, viewModel.isWritingTag.value)

        // Act
        viewModel.setWritingTag(true)

        // Assert
        assertEquals(true, viewModel.isWritingTag.value)
    }

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
}