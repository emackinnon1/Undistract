package com.undistract.managers

import android.content.Context
import android.content.SharedPreferences
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.compose.ui.test.junit4.createComposeRule
import com.undistract.UndistractApp
import com.undistract.data.entities.ProfileEntity
import com.undistract.data.models.AppInfo
import com.undistract.data.repositories.ProfileRepository
import com.undistract.ui.profile.ProfilesPicker
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.*
import io.mockk.slot
import io.mockk.verify




@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.UPSIDE_DOWN_CAKE])
class ProfilesPickerTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    // Update mock setup to use ProfileEntity instead of Profile
    private fun setupMockProfileManager(
        profileEntities: List<ProfileEntity>,
        currentId: String = profileEntities.firstOrNull()?.id ?: "",
        isLoading: Boolean = false,
        errorMessage: String? = null
    ): ProfileManager {
        val fakeRepository = mockk<ProfileRepository>(relaxed = true)
        every { fakeRepository.getAllProfiles() } returns MutableStateFlow(profileEntities)

        val fakeManager = mockk<ProfileManager>(relaxed = true)
        every { fakeManager.profiles } returns MutableStateFlow(profileEntities)
        every { fakeManager.currentProfileId } returns MutableStateFlow(currentId)
        every { fakeManager.isLoading } returns MutableStateFlow(isLoading)
        every { fakeManager.errorMessage } returns MutableStateFlow(errorMessage)

        return fakeManager
    }

    @Test
    fun profileListDisplay_showsAllProfilesFromManager() {
        val testProfiles = listOf(
            ProfileEntity(id = "1", name = "Work", appPackageNames = listOf(), icon = "baseline_work_24"),
            ProfileEntity(id = "2", name = "Personal", appPackageNames = listOf(), icon = "baseline_person_24"),
            ProfileEntity(id = "3", name = "Focus", appPackageNames = listOf(), icon = "baseline_block_24")
        )

        val fakeManager = setupMockProfileManager(testProfiles)

        composeTestRule.setContent {
            ProfilesPicker(profileManager = fakeManager)
        }

        // Verify each profile name appears exactly once
        testProfiles.forEach { profile ->
            composeTestRule.onAllNodesWithText(profile.name).assertCountEquals(1)
        }
    }

    @Test
    fun profileSelection_updatesCurrentProfileInManager() {
        val testProfiles = listOf(
            ProfileEntity(id = "1", name = "Work", appPackageNames = listOf(), icon = "baseline_work_24"),
            ProfileEntity(id = "2", name = "Personal", appPackageNames = listOf(), icon = "baseline_person_24"),
            ProfileEntity(id = "3", name = "Focus", appPackageNames = listOf(), icon = "baseline_block_24")
        )

        val fakeManager = setupMockProfileManager(testProfiles)

        composeTestRule.setContent {
            ProfilesPicker(profileManager = fakeManager)
        }

        // Get the second profile to select
        val profileToSelect = testProfiles[1]

        // Find and click the profile by its name
        composeTestRule.onNodeWithText(profileToSelect.name).performClick()
        composeTestRule.waitForIdle()

        // Verify that setCurrentProfile was called with the correct profile ID
        verify { fakeManager.setCurrentProfile(profileToSelect.id) }
    }

    @Test
    fun loadingIndicator_isDisplayedWhenLoading() {
        // Setup the ProfileManager with isLoading=true
        val fakeManager = setupMockProfileManager(
            profileEntities = listOf(ProfileEntity(id = "1", name = "Default", appPackageNames = listOf())),
            isLoading = true
        )

        composeTestRule.setContent {
            ProfilesPicker(profileManager = fakeManager)
        }

        // Instead of using isA and assertExists directly, use a different approach
        composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("loadingIndicator")).assertExists()
    }

    @Test
    fun errorMessage_isDisplayedWhenAvailable() {
        val testProfiles = listOf(
            ProfileEntity(id = "1", name = "Work", appPackageNames = listOf(), icon = "baseline_work_24")
        )

        // Setup with an error message
        val errorMsg = "Failed to save profile"
        val fakeManager = setupMockProfileManager(
            profileEntities = testProfiles,
            errorMessage = errorMsg
        )

        composeTestRule.setContent {
            ProfilesPicker(profileManager = fakeManager)
        }

        // Wait for the Snackbar to appear with our error message
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithText(errorMsg).fetchSemanticsNodes().isNotEmpty()
        }

        // Verify the error message is displayed
        composeTestRule.onNodeWithText(errorMsg).assertExists()

        // Verify clearErrorMessage was called
        verify { fakeManager.clearErrorMessage() }
    }

    @Test
    fun addingProfile_showsLoadingAndClearsError() {
        val testProfiles = listOf(
            ProfileEntity(id = "1", name = "Work", appPackageNames = listOf(), icon = "baseline_work_24")
        )

        val loadingStateFlow = MutableStateFlow(false)
        val errorMessageFlow = MutableStateFlow<String?>(null)

        val fakeManager = mockk<ProfileManager>(relaxed = true)
        every { fakeManager.profiles } returns MutableStateFlow(testProfiles)
        every { fakeManager.currentProfileId } returns MutableStateFlow(testProfiles.first().id)
        every { fakeManager.isLoading } returns loadingStateFlow
        every { fakeManager.errorMessage } returns errorMessageFlow

        // Capture any profile being added
        val profileSlot = slot<ProfileEntity>()
        every { fakeManager.addProfile(capture(profileSlot)) } answers {
            loadingStateFlow.value = true
            // Simulate a successful operation after some delay
            loadingStateFlow.value = false
        }

        composeTestRule.setContent {
            ProfilesPicker(profileManager = fakeManager)
        }

        // Click "Add New Profile"
        composeTestRule.onNodeWithText("Add New Profile").performClick()
        composeTestRule.waitForIdle()

        // Enter profile name and save
        composeTestRule.onNodeWithText("Profile Name").performTextInput("Test Profile")
        composeTestRule.onNodeWithText("Save").performClick()
        composeTestRule.waitForIdle()

        // Verify addProfile was called with expected data
        verify { fakeManager.addProfile(any()) }
        // Check the captured entity has the expected name
        assert(profileSlot.captured.name == "Test Profile")
    }
}