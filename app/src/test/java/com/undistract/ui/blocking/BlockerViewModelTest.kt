package com.undistract.ui.blocking

import android.os.Build
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.undistract.UndistractApp
import com.undistract.data.local.UndistractDatabase
import com.undistract.data.entities.ProfileEntity
import com.undistract.managers.AppBlockerManager
import com.undistract.managers.ProfileManager
import com.undistract.services.AppBlockerAccessibilityService
import com.undistract.data.entities.NfcTagEntity
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config


@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.UPSIDE_DOWN_CAKE])
class BlockerViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    @Mock
    private lateinit var undistractApp: UndistractApp

    private lateinit var db: UndistractDatabase
    private lateinit var nfcTagDao: com.undistract.data.daos.NfcTagDao

    private lateinit var viewModel: BlockerViewModel

    // Mock objects for UndistractApp statics
    private val mockAppBlocker = mockk<AppBlockerManager>(relaxed = true)
    private val mockProfileManager = mockk<ProfileManager>(relaxed = true)
    private val blockingStateFlow = MutableStateFlow(false)
    private val profileStateFlow = MutableStateFlow<ProfileEntity?>(null)

    @Before
    fun setUp() {
        // Set up test dispatcher
        Dispatchers.setMain(testDispatcher)

        // Initialize mocks
        MockitoAnnotations.openMocks(this)

        // Set up Room database in memory for tests
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            UndistractDatabase::class.java
        ).allowMainThreadQueries().build()
        nfcTagDao = db.nfcTagDao()

        // Mock static dependencies
        mockkObject(UndistractApp)
        mockkObject(AppBlockerAccessibilityService.Companion)

        // Configure mock behavior
        every { UndistractApp.appBlocker } returns mockAppBlocker
        every { UndistractApp.profileManager } returns mockProfileManager
        every { UndistractApp.instance } returns ApplicationProvider.getApplicationContext()
        every { UndistractApp.db } returns db
        every { mockAppBlocker.isBlocking } returns blockingStateFlow
        every { mockProfileManager.currentProfile } returns profileStateFlow
        every { AppBlockerAccessibilityService.ensureAccessibilityServiceEnabled(any()) } returns Unit

        // Set up test data - add a valid tag to the written tags list
        val testTag = NfcTagEntity(
            id = "UNDISTRACT-valid-tag",
            payload = "profile_tag"
        )

        // Create the view model with dependencies injected
        viewModel = BlockerViewModel(ApplicationProvider.getApplicationContext())

        // Set the written tags directly to include our test tag
        val writtenTagsField = BlockerViewModel::class.java.getDeclaredField("_writtenTags")
        writtenTagsField.isAccessible = true

        viewModel.setWrittenTagsForTesting(listOf(testTag))
        // Run any pending operations
        testDispatcher.scheduler.advanceUntilIdle()
    }

    @After
    fun tearDown() {
        db.close()
        blockingStateFlow.value = false
        profileStateFlow.value = null
        Dispatchers.resetMain()
    }

    @Test
    fun `loadSavedTags should load tags correctly`() {
        // Arrange
        viewModel.saveTag("UNDISTRACT-123")
        testDispatcher.scheduler.advanceUntilIdle() // Wait for save operation
        viewModel.saveTag("UNDISTRACT-456")
        testDispatcher.scheduler.advanceUntilIdle() // Wait for save operation

        // Act - Create a new ViewModel to trigger loadSavedTags in init
        val newViewModel = BlockerViewModel(undistractApp)
        testDispatcher.scheduler.advanceUntilIdle() // Wait for loadSavedTags in init

        // Assert
        assertEquals(2, newViewModel.writtenTags.value.size)
        val tagIds = newViewModel.writtenTags.value.map { it.id }
        assertTrue(tagIds.contains("UNDISTRACT-123"))
        assertTrue(tagIds.contains("UNDISTRACT-456"))
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
    fun `deleteTag should remove tag from list`() {
        // Arrange - Initialize with existing tags
        viewModel.saveTag("UNDISTRACT-123")
        testDispatcher.scheduler.advanceUntilIdle() // Wait for save operation
        viewModel.saveTag("UNDISTRACT-456")
        testDispatcher.scheduler.advanceUntilIdle() // Wait for save operation

        val testViewModel = BlockerViewModel(undistractApp)
        testDispatcher.scheduler.advanceUntilIdle() // Wait for viewmodel initialization
        val tag = testViewModel.writtenTags.value.filter { it.id == "UNDISTRACT-123" }[0]

        // Act
        testViewModel.deleteTag(tag)
        testDispatcher.scheduler.advanceUntilIdle() // Wait for delete operation

        // Assert
        assertEquals(1, testViewModel.writtenTags.value.size)
        assertEquals("UNDISTRACT-456", testViewModel.writtenTags.value[0].id)
    }

    @Test
    fun `scanTag with valid tag prefix should trigger blocking toggle`() {
        // Arrange
        val testTag = NfcTagEntity(id = "UNDISTRACT-valid-tag", payload = "profile_tag")

        // Set a valid profile (this is what's missing)
        profileStateFlow.value = ProfileEntity(id = "1", name = "Test Profile", appPackageNames = listOf("com.example.app"))

        // Set up written tags with our test tag
        viewModel.setWrittenTagsForTesting(listOf(testTag))

        // Act
        viewModel.scanTag(testTag.id)
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        io.mockk.verify { mockAppBlocker.setBlockingState(true) }
        assertEquals(false, viewModel.showScanTagAlert.value)
    }

    @Test
    fun `scanTag with invalid tag should show wrong tag alert`() {
        // Arrange
        val invalidTagPayload = "INVALID-TAG"

        // Act
        viewModel.scanTag(invalidTagPayload)
        testDispatcher.scheduler.advanceUntilIdle()  // Wait for coroutine to complete

        // Assert
        assertTrue(viewModel.showWrongTagAlert.value)
    }

    @Test
    fun `toggleBlocking should start BlockerService when enabling blocking`() {
        // Arrange - set a valid profile
        profileStateFlow.value = ProfileEntity("1", "Test Profile", listOf("com.example.app"))

        // Use reflection to call the private toggleBlocking method
        val toggleBlockingMethod = BlockerViewModel::class.java.getDeclaredMethod("toggleBlocking")
        toggleBlockingMethod.isAccessible = true

        // Act
        toggleBlockingMethod.invoke(viewModel)
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        io.mockk.verify { AppBlockerAccessibilityService.ensureAccessibilityServiceEnabled(any()) }
    }

    @Test
    fun `toggleBlocking should stop BlockerService when disabling blocking`() {
        // Arrange
        val appPackages = listOf("com.example.app1", "com.example.app2")
        profileStateFlow.value = ProfileEntity(id = "1", name = "Test Profile", appPackageNames = appPackages)
        blockingStateFlow.value = true // Currently blocking

        // Use reflection to call the private toggleBlocking method
        val toggleBlockingMethod = BlockerViewModel::class.java.getDeclaredMethod("toggleBlocking")
        toggleBlockingMethod.isAccessible = true

        // Act
        toggleBlockingMethod.invoke(viewModel)
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        io.mockk.verify { mockAppBlocker.setBlockingState(false) }
    }

    @Test
    fun `toggleBlocking should check accessibility service when enabling blocking`() {
        // Arrange
        val appPackages = listOf("com.example.app1", "com.example.app2")
        profileStateFlow.value = ProfileEntity(id = "1", name = "Test Profile", appPackageNames = appPackages)
        blockingStateFlow.value = false // Not currently blocking

        // Use reflection to call the private toggleBlocking method
        val toggleBlockingMethod = BlockerViewModel::class.java.getDeclaredMethod("toggleBlocking")
        toggleBlockingMethod.isAccessible = true

        // Act
        toggleBlockingMethod.invoke(viewModel)
        testDispatcher.scheduler.advanceUntilIdle()

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
        testDispatcher.scheduler.advanceUntilIdle()  // Wait for coroutine to complete

        // Assert
        io.mockk.verify(exactly = 0) { mockAppBlocker.setBlockingState(any()) }
        io.mockk.verify(exactly = 0) { AppBlockerAccessibilityService.ensureAccessibilityServiceEnabled(any()) }
    }

    @Test
    fun `showScanTagAlert should set showScanTagAlert state to true`() {
        // Arrange
        val testTag = NfcTagEntity(id = "UNDISTRACT-valid-tag", payload = "profile_tag")
        val writtenTagsField = BlockerViewModel::class.java.getDeclaredField("_writtenTags")
        writtenTagsField.isAccessible = true
        viewModel.setWrittenTagsForTesting(listOf(testTag))

        // Act
        viewModel.showScanTagAlert()
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert - since we have a tag from setUp, the alert should show
        assertEquals(true, viewModel.showScanTagAlert.value)
        assertEquals(false, viewModel.showWrongTagAlert.value)
        assertEquals(false, viewModel.showCreateTagAlert.value)
        assertEquals(false, viewModel.noTagsExistAlert.value)
    }

    @Test
    fun `dismissScanTagAlert should set showScanTagAlert state to false`() {
        // Arrange - set the state to true first
        val scanTagAlertField = BlockerViewModel::class.java.getDeclaredField("_showScanTagAlert")
        scanTagAlertField.isAccessible = true
        (scanTagAlertField.get(viewModel) as MutableStateFlow<Boolean>).value = true

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

    @Test
    fun `generateUniqueTagId should follow correct format`() {
        // Act
        val id = viewModel.generateUniqueTagId()

        // Assert
        // Format should be UNDISTRACT-timestamp-random
        val parts = id.split("-")
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
}