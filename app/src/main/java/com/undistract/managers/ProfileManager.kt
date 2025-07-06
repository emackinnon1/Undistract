package com.undistract.managers

import android.content.Context
import android.util.Log
import androidx.lifecycle.asLiveData
import com.undistract.data.entities.ProfileEntity
import com.undistract.data.mappers.toEntity
import com.undistract.data.mappers.toProfile
import com.undistract.data.models.AppInfo
import com.undistract.data.models.Profile
import com.undistract.data.repositories.ProfileRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray

/**
 * Manager class responsible for handling user profiles in the Undistract application.
 *
 * Provides functionality to create, read, update, and delete profiles, as well as
 * manage the current active profile. Profiles are persisted using Room database
 * and can be observed through StateFlow objects.
 *
 * @property context The application context used for SharedPreferences access
 * @property profileRepository Repository used to access profiles in the Room database
 */
class ProfileManager(
    private val context: Context,
    private val profileRepository: ProfileRepository
) {
    // Coroutine scope for performing database operations
    private val managerScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    /**
     * SharedPreferences instance used to persist current profile ID.
     */
    private val sharedPreferences = context.getSharedPreferences("profile_manager_prefs", Context.MODE_PRIVATE)

    /**
     * Mutable state that holds the list of available profiles.
     */
    private val _profiles = MutableStateFlow<List<ProfileEntity>>(emptyList())

    /**
     * Read-only state flow exposing the list of available profiles.
     */
    val profiles: StateFlow<List<ProfileEntity>> = _profiles.asStateFlow()

    /**
     * Mutable state that holds the ID of the currently selected profile.
     */
    private val _currentProfileId = MutableStateFlow<String?>(null)

    /**
     * Read-only state flow exposing the ID of the currently selected profile.
     */
    val currentProfileId: StateFlow<String?> = _currentProfileId.asStateFlow()

    /**
     * Mutable state that holds the currently selected profile object.
     */
    private val _currentProfile = MutableStateFlow<ProfileEntity?>(null)

    /**
     * Read-only state flow exposing the currently selected profile.
     */
    val currentProfile: StateFlow<ProfileEntity?> = _currentProfile.asStateFlow()

    /**
     * Mutable state that holds error messages to be displayed to the user.
     */
    private val _errorMessage = MutableStateFlow<String?>(null)

    /**
     * Read-only state flow exposing error messages.
     */
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()


    /**
     * Initializes the ProfileManager by loading saved profiles.
     */
    init {
        // Load the current profile ID from SharedPreferences
        _currentProfileId.value = sharedPreferences.getString("currentProfileId", null)

        // Observe profiles from the repository
        managerScope.launch {
            profileRepository.getAllProfiles().collectLatest { profileEntities ->
                _profiles.value = profileEntities

                // Ensure we have a valid current profile
                if (_currentProfileId.value == null || !profileExists(_currentProfileId.value!!)) {
                    _currentProfileId.value = _profiles.value.firstOrNull { it.name == "Default" }?.id
                        ?: _profiles.value.firstOrNull()?.id

                    // Save the current profile ID
                    sharedPreferences.edit()
                        .putString("currentProfileId", _currentProfileId.value)
                        .apply()
                }

                updateCurrentProfile()

                // Create default profile if no profiles exist
                if (_profiles.value.isEmpty()) {
                    createAndAddDefaultProfile()
                }
            }
        }
    }

    /**
     * Creates a new default profile with empty app list.
     */
    private suspend fun createAndAddDefaultProfile() {
        val defaultProfile = ProfileEntity(
            id = java.util.UUID.randomUUID().toString(),
            name = "Default",
            appPackageNames = emptyList(),
            icon = "baseline_block_24"
        )

        addProfile(defaultProfile)
    }

    /**
     * Updates the current profile object based on the current profile ID.
     */
    private fun updateCurrentProfile() {
        _currentProfile.value = _profiles.value.find { it.id == _currentProfileId.value }
            ?: _profiles.value.find { it.name == "Default" }
            ?: _profiles.value.firstOrNull()
    }

    /**
     * Checks if a profile with the given ID exists in the profiles list.
     *
     * @param id The profile ID to check
     * @return True if a profile with the ID exists, false otherwise
     */
    private fun profileExists(id: String): Boolean = _profiles.value.any { it.id == id }

    /**
     * Adds a new profile to the profiles list and sets it as the current profile.
     *
     * @param newProfile The profile to add
     */
    fun addProfile(newProfile: ProfileEntity) {
        _isLoading.value = true
        managerScope.launch {
            try {
                profileRepository.saveProfile(newProfile)
                _currentProfileId.value = newProfile.id

                // Save the current profile ID in SharedPreferences
                sharedPreferences.edit()
                    .putString("currentProfileId", newProfile.id)
                    .apply()
            } catch (e: Exception) {
                Log.e("ProfileManager", "Error adding profile", e)
                _errorMessage.value = "Failed to add profile: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Updates an existing profile with new values.
     *
     * @param id The ID of the profile to update
     * @param name Optional new name for the profile
     * @param appPackageNames Optional new list of app package names
     * @param icon Optional new icon identifier
     */
    fun updateProfile(
        id: String,
        name: String? = null,
        appPackageNames: List<String>? = null,
        icon: String? = null
    ) {
        val existingProfile = _profiles.value.find { it.id == id } ?: return

        val updatedProfile = existingProfile.copy(
            name = name ?: existingProfile.name,
            appPackageNames = appPackageNames ?: existingProfile.appPackageNames,
            icon = icon ?: existingProfile.icon
        )

        managerScope.launch {
            try {
                profileRepository.saveProfile(updatedProfile)
            } catch (e: Exception) {
                Log.e("ProfileManager", "Error updating profile", e)
                _errorMessage.value = "Failed to update profile: ${e.localizedMessage}"
            }
        }
    }

    /**
     * Sets the specified profile as the current profile.
     *
     * @param id The ID of the profile to set as current
     */
    fun setCurrentProfile(id: String) {
        if (profileExists(id)) {
            _currentProfileId.value = id
            updateCurrentProfile()

            // Save the current profile ID in SharedPreferences
            sharedPreferences.edit()
                .putString("currentProfileId", id)
                .apply()
        }
    }

    /**
     * Deletes a profile by ID.
     *
     * If the deleted profile is the current profile, the first available profile
     * becomes the current profile. Cannot delete if it would leave no profiles.
     *
     * @param id The ID of the profile to delete
     */
    fun deleteProfile(id: String) {
        // Check if this would leave us with no profiles
        if (_profiles.value.size <= 1) {
            _errorMessage.value = "You must have at least one profile"
            return
        }

        val profileToDelete = _profiles.value.find { it.id == id } ?: return

        managerScope.launch {
            try {
                // Delete from database
                profileRepository.deleteProfile(profileToDelete)

                // If the deleted profile was current, select another profile
                if (_currentProfileId.value == id) {
                    val newCurrentId = _profiles.value.firstOrNull { it.id != id }?.id

                    _currentProfileId.value = newCurrentId

                    // Save the current profile ID in SharedPreferences
                    sharedPreferences.edit()
                        .putString("currentProfileId", newCurrentId)
                        .apply()
                }
            } catch (e: Exception) {
                Log.e("ProfileManager", "Error deleting profile", e)
                _errorMessage.value = "Failed to delete profile: ${e.localizedMessage}"
            }
        }
    }

    /**
     * Filters the provided app list to exclude the Undistract app itself.
     *
     * @param appList List of apps to filter
     * @return Filtered list of apps excluding the Undistract app
     */
    fun getFilteredAppList(appList: List<AppInfo>): List<AppInfo> {
        return appList.filter { it.packageName != "com.undistract" }
    }

    /**
     * Clears the current error message.
     */
    fun clearErrorMessage() {
        _errorMessage.value = null
    }
}