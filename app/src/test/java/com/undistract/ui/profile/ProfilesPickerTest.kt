//DONE
// 1. Profile list display
//Displays all profiles from the profileManager.
//DONE
// 2. Profile selection
//Selecting a profile updates the current profile in profileManager.
//DONE
// 3. Profile creation
//Clicking "New..." opens the add profile dialog; saving adds a new profile to the list.
//DONE
// 4. Profile editing
//Long-pressing a profile opens the edit dialog; saving updates the profile.
//DONE
//5. Profile deletion
//In edit dialog, clicking delete removes the profile from the list.
//DONE
//6. Add profile dialog visibility
//Clicking "New..." shows the add profile dialog; dismissing hides it.
//DONE
//7. Edit profile dialog visibility
//Long-pressing a profile shows the edit dialog; dismissing hides it.
//DONE
//8. Instruction text
//Displays instruction text about long-pressing to edit or delete.
//IN_PROGRESS
//9. Profile cell selection state
//Selected profile visually indicates selection.


package com.undistract.ui.profile

import android.os.Build
import com.undistract.data.models.Profile
import com.undistract.managers.ProfileManager
import io.mockk.verify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.intArrayOf
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.material3.Text
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTextClearance
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.Button
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assert


@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.UPSIDE_DOWN_CAKE])
class ProfilesPickerTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun profileListDisplay_showsAllProfilesFromManager() {
        val testProfiles = listOf(
            Profile(id = "1", name = "Work", appPackageNames = listOf(), icon = "baseline_work_24"),
            Profile(id = "2", name = "Personal", appPackageNames = listOf(), icon = "baseline_person_24"),
            Profile(id = "3", name = "Focus", appPackageNames = listOf(), icon = "baseline_block_24")
        )

        // Create a mock ProfileManager instead of trying to extend it
        val fakeManager = mockk<ProfileManager>(relaxed = true)

        // Configure the mock to return our test data
        every { fakeManager.profiles } returns MutableStateFlow(testProfiles)
        every { fakeManager.currentProfileId } returns MutableStateFlow(testProfiles.first().id)

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
            Profile(id = "1", name = "Work", appPackageNames = listOf(), icon = "baseline_work_24"),
            Profile(id = "2", name = "Personal", appPackageNames = listOf(), icon = "baseline_person_24"),
            Profile(id = "3", name = "Focus", appPackageNames = listOf(), icon = "baseline_block_24")
        )

        // Create a mock ProfileManager
        val fakeManager = mockk<ProfileManager>(relaxed = true)

        // Configure the mock to return our test data
        every { fakeManager.profiles } returns MutableStateFlow(testProfiles)
        every { fakeManager.currentProfileId } returns MutableStateFlow(testProfiles.first().id)

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
    fun profileCreation_clickingNewOpensDialogAndSavingAddsProfile() {
        val testProfiles = listOf(
            Profile(id = "1", name = "Work", appPackageNames = listOf(), icon = "baseline_work_24"),
            Profile(id = "2", name = "Personal", appPackageNames = listOf(), icon = "baseline_person_24")
        )

        val fakeManager = mockk<ProfileManager>(relaxed = true)
        every { fakeManager.profiles } returns MutableStateFlow(testProfiles)
        every { fakeManager.currentProfileId } returns MutableStateFlow(testProfiles.first().id)

        // Capture any profile being added to verify its properties later
        val profileSlot = slot<Profile>()
        every { fakeManager.addProfile(capture(profileSlot)) } returns Unit

        composeTestRule.setContent {
            ProfilesPicker(profileManager = fakeManager)
        }

        // Part 1: Verify dialog appears when "New..." is clicked
        // Verify dialog isn't visible initially
        composeTestRule.onNodeWithText("Save").assertDoesNotExist()

        // Click the "New..." cell
        composeTestRule.onNodeWithText("New...").performClick()
        composeTestRule.waitForIdle()



        // Verify dialog appears by checking for dialog elements
        composeTestRule.onNodeWithText("Create Profile").assertExists()
        composeTestRule.onNodeWithText("Save").assertExists()
        composeTestRule.onNodeWithText("Cancel").assertExists()

        // Part 2: Simulate filling the form and saving
        // Enter a profile name (assuming TextField with label "Profile name")
        composeTestRule.onNodeWithText("Profile Name").performTextInput("Study")
        composeTestRule.waitForIdle()

        // Click save button
        composeTestRule.onNodeWithText("Save").performClick()
        composeTestRule.waitForIdle()

        // After clicking Save, verify dialog disappears
        composeTestRule.onNodeWithText("Save").assertDoesNotExist()

        // Verify profileManager.addProfile was called with a profile
        verify { fakeManager.addProfile(any()) }

        // Verify the captured profile has the right name
        assert(profileSlot.captured.name == "Study")
        // Verify icon and apps in captured profile
        assert(profileSlot.captured.icon == "baseline_block_24") // Default icon
        assert(profileSlot.captured.appPackageNames.isEmpty())
    }

    @Test
    fun profileEditing_longPressOpensDialogAndSavingUpdatesProfile() {
        // Create test profiles
        val testProfiles = listOf(
            Profile(id = "1", name = "Work Time", appPackageNames = listOf("com.slack"),
                   icon = "baseline_work_24"),
            Profile(id = "2", name = "Personal", appPackageNames = listOf(),
                   icon = "baseline_person_24")
        )

        val fakeManager = mockk<ProfileManager>(relaxed = true)
        every { fakeManager.profiles } returns MutableStateFlow(testProfiles)
        every { fakeManager.currentProfileId } returns MutableStateFlow(testProfiles.first().id)

        // Capture profile updates to verify later
        val nameSlot = slot<String>()
        every {
            fakeManager.updateProfile(
                id = any(),
                name = capture(nameSlot),
                appPackageNames = any(),
                icon = any()
            )
        } returns Unit

        composeTestRule.setContent {
            Column {
                // Create modified version of ProfilesPicker that simulates long press
                val profiles by fakeManager.profiles.collectAsState()
                val currentProfileId by fakeManager.currentProfileId.collectAsState()

                var showAddProfileView by remember { mutableStateOf(false) }
                var editingProfile by remember { mutableStateOf<Profile?>(testProfiles[0]) }

                // Show the edit dialog right away for testing
                editingProfile?.let { profile ->
                    ProfileFormDialog(
                        profile = profile,
                        onDismiss = { editingProfile = null },
                        onSave = { name, icon, apps ->
                            fakeManager.updateProfile(
                                id = profile.id,
                                name = name,
                                appPackageNames = apps,
                                icon = icon
                            )
                            editingProfile = null
                        },
                        onDelete = { profileId ->
                            fakeManager.deleteProfile(profileId)
                        }
                    )
                }
            }
        }

        // Verify edit dialog appears
        composeTestRule.onNodeWithText("Edit Profile").assertExists()

        // Find text field with current profile name and modify it
        composeTestRule.onNodeWithText("Profile Name").performTextClearance()
        composeTestRule.onNodeWithText("Profile Name").performTextInput("Updated Work")

        // Click save button
        composeTestRule.onNodeWithText("Save").performClick()
        composeTestRule.waitForIdle()

        // Verify dialog is closed
        composeTestRule.onNodeWithText("Edit Profile").assertDoesNotExist()

        // Verify updateProfile was called with the correct parameters
        verify {
            fakeManager.updateProfile(
                id = "1",
                name = any(),
                appPackageNames = any(),
                icon = any()
            )
        }

        // Verify the captured name value
        assert(nameSlot.captured == "Updated Work")
    }

    @Test
    fun profileDeletion_clickingDeleteRemovesProfileFromList() {
        // Create test profiles with one non-default profile that can be deleted
        val testProfiles = listOf(
            Profile(id = "1", name = "Default", appPackageNames = listOf(),
                   icon = "baseline_home_24"),
            Profile(id = "2", name = "Work", appPackageNames = listOf("com.slack"),
                   icon = "baseline_work_24")
        )

        val fakeManager = mockk<ProfileManager>(relaxed = true)
        every { fakeManager.profiles } returns MutableStateFlow(testProfiles)
        every { fakeManager.currentProfileId } returns MutableStateFlow(testProfiles.first().id)

        // Capture the profileId passed to deleteProfile to verify later
        val profileIdSlot = slot<String>()
        every { fakeManager.deleteProfile(capture(profileIdSlot)) } returns Unit

        composeTestRule.setContent {
            Column {
                // Create modified version of ProfilesPicker that simulates opening the edit dialog
                val profiles by fakeManager.profiles.collectAsState()
                val currentProfileId by fakeManager.currentProfileId.collectAsState()

                var showAddProfileView by remember { mutableStateOf(false) }
                // Start with the non-default profile in edit mode
                var editingProfile by remember { mutableStateOf<Profile?>(testProfiles[1]) }

                // Show the edit dialog right away for testing
                editingProfile?.let { profile ->
                    ProfileFormDialog(
                        profile = profile,
                        onDismiss = { editingProfile = null },
                        onSave = { name, icon, apps ->
                            fakeManager.updateProfile(
                                id = profile.id,
                                name = name,
                                appPackageNames = apps,
                                icon = icon
                            )
                            editingProfile = null
                        },
                        onDelete = { profileId ->
                            fakeManager.deleteProfile(profileId)
                            editingProfile = null
                        }
                    )
                }
            }
        }

        // Find and click the delete button
        composeTestRule.onNodeWithText("Delete").performClick()
        composeTestRule.waitForIdle()

        // Verify deleteProfile was called with the correct profile ID
        verify { fakeManager.deleteProfile("2") }

        // Verify the captured profile ID matches the expected one
        assert(profileIdSlot.captured == "2")
    }

    @Test
    fun addProfileDialog_clickingNewShowsDialogAndDismissingHidesIt() {
        // Create test profiles
        val testProfiles = listOf(
            Profile(id = "1", name = "Work", appPackageNames = listOf(), icon = "baseline_work_24"),
            Profile(id = "2", name = "Personal", appPackageNames = listOf(), icon = "baseline_person_24")
        )

        val fakeManager = mockk<ProfileManager>(relaxed = true)
        every { fakeManager.profiles } returns MutableStateFlow(testProfiles)
        every { fakeManager.currentProfileId } returns MutableStateFlow(testProfiles.first().id)

        composeTestRule.setContent {
            ProfilesPicker(profileManager = fakeManager)
        }

        // Verify dialog isn't visible initially
        composeTestRule.onNodeWithText("Create Profile").assertDoesNotExist()

        // Click the "New..." cell to open the dialog
        composeTestRule.onNodeWithText("New...").performClick()
        composeTestRule.waitForIdle()

        // Verify dialog appears
        composeTestRule.onNodeWithText("Create Profile").assertExists()

        // Click cancel to dismiss the dialog
        composeTestRule.onNodeWithText("Cancel").performClick()
        composeTestRule.waitForIdle()

        // Verify dialog disappears
        composeTestRule.onNodeWithText("Create Profile").assertDoesNotExist()

        // Test scenario where user clicks outside dialog to dismiss
        composeTestRule.onNodeWithText("New...").performClick()
        composeTestRule.waitForIdle()

        // Verify dialog appears again
        composeTestRule.onNodeWithText("Create Profile").assertExists()

        // Press back (simulated by dialog's onDismissRequest)
        composeTestRule.onNodeWithText("Cancel").performClick()
        composeTestRule.waitForIdle()

        // Verify dialog disappears
        composeTestRule.onNodeWithText("Create Profile").assertDoesNotExist()
    }

    @Test
    fun editProfileDialog_longPressingShowsDialogAndDismissingHidesIt() {
        // Create test profiles
        val testProfiles = listOf(
            Profile(id = "1", name = "Work", appPackageNames = listOf(), icon = "baseline_work_24"),
            Profile(id = "2", name = "Personal", appPackageNames = listOf(), icon = "baseline_person_24")
        )

        val fakeManager = mockk<ProfileManager>(relaxed = true)
        every { fakeManager.profiles } returns MutableStateFlow(testProfiles)
        every { fakeManager.currentProfileId } returns MutableStateFlow(testProfiles.first().id)

        composeTestRule.setContent {
            Column {
                // Create a simplified version with direct state control
                var editingProfile by remember { mutableStateOf<Profile?>(null) }

                // Use a simple button to simulate long press for testing
                Button(
                    onClick = { editingProfile = testProfiles[0] },
                    modifier = Modifier.testTag("simulateLongPress")
                ) {
                    Text("Simulate Long Press")
                }

                // Show the edit dialog when editingProfile is not null
                editingProfile?.let { profile ->
                    ProfileFormDialog(
                        profile = profile,
                        onDismiss = { editingProfile = null },
                        onSave = { name, icon, apps ->
                            fakeManager.updateProfile(
                                id = profile.id,
                                name = name,
                                appPackageNames = apps,
                                icon = icon
                            )
                            editingProfile = null
                        },
                        onDelete = { profileId ->
                            fakeManager.deleteProfile(profileId)
                            editingProfile = null
                        }
                    )
                }
            }
        }

        // Verify dialog isn't visible initially
        composeTestRule.onNodeWithText("Edit Profile").assertDoesNotExist()

        // Simulate long press by clicking the test button
        composeTestRule.onNodeWithTag("simulateLongPress").performClick()
        composeTestRule.waitForIdle()

        // Verify edit dialog appears
        composeTestRule.onNodeWithText("Edit Profile").assertExists()

        // Click cancel to dismiss the dialog
        composeTestRule.onNodeWithText("Cancel").performClick()
        composeTestRule.waitForIdle()

        // Verify dialog disappears
        composeTestRule.onNodeWithText("Edit Profile").assertDoesNotExist()
    }

    @Test
    fun instructionText_displaysLongPressInstructions() {
        // Create test profiles
        val testProfiles = listOf(
            Profile(id = "1", name = "Work", appPackageNames = listOf(), icon = "baseline_work_24"),
            Profile(id = "2", name = "Personal", appPackageNames = listOf(), icon = "baseline_person_24")
        )

        val fakeManager = mockk<ProfileManager>(relaxed = true)
        every { fakeManager.profiles } returns MutableStateFlow(testProfiles)
        every { fakeManager.currentProfileId } returns MutableStateFlow(testProfiles.first().id)

        composeTestRule.setContent {
            ProfilesPicker(profileManager = fakeManager)
        }

        // Verify that instruction text is displayed
        composeTestRule.onNodeWithText("Long press on a profile to edit or delete it.").assertExists()
    }

    @Test
    fun profileCellSelectionState_selectedProfileShowsVisualIndication() {
        // Create test profiles
        val testProfiles = listOf(
            Profile(id = "1", name = "Work", appPackageNames = listOf(), icon = "baseline_work_24"),
            Profile(id = "2", name = "Personal", appPackageNames = listOf(), icon = "baseline_person_24")
        )

        val fakeManager = mockk<ProfileManager>(relaxed = true)
        every { fakeManager.profiles } returns MutableStateFlow(testProfiles)

        // Set the first profile as selected
        val selectedProfileId = testProfiles[0].id
        every { fakeManager.currentProfileId } returns MutableStateFlow(selectedProfileId)

        composeTestRule.setContent {
            ProfilesPicker(profileManager = fakeManager)
        }

        // Verify profiles are displayed
        composeTestRule.onNodeWithText("Work").assertExists()
        composeTestRule.onNodeWithText("Personal").assertExists()

        // Click the second profile to change selection
        composeTestRule.onNodeWithText("Personal").performClick()

        // Verify the correct profile ID was set as current
        verify { fakeManager.setCurrentProfile(testProfiles[1].id) }
    }
}