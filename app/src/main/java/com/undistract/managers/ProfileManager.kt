package com.undistract.managers

import android.content.Context
import android.util.Log
import com.undistract.data.models.AppInfo
import com.undistract.data.models.Profile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray


/**
 * Manager class responsible for handling user profiles in the Undistract application.
 *
 * Provides functionality to create, read, update, and delete profiles, as well as
 * manage the current active profile. Profiles are persisted using SharedPreferences
 * and can be observed through StateFlow objects.
 *
 * @property context The application context used for SharedPreferences access
 */
class ProfileManager(private val context: Context) {
    /**
     * SharedPreferences instance used to persist profile data.
     */
    private val sharedPreferences = context.getSharedPreferences("profile_manager_prefs", Context.MODE_PRIVATE)

    /**
     * Mutable state that holds the list of available profiles.
     */
    private val _profiles = MutableStateFlow<List<Profile>>(emptyList())

    /**
     * Read-only state flow exposing the list of available profiles.
     */
    val profiles: StateFlow<List<Profile>> = _profiles.asStateFlow()

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
    private val _currentProfile = MutableStateFlow<Profile?>(null)

    /**
     * Read-only state flow exposing the currently selected profile.
     */
    val currentProfile: StateFlow<Profile?> = _currentProfile.asStateFlow()

    /**
     * Mutable state that holds error messages to be displayed to the user.
     */
    private val _errorMessage = MutableStateFlow<String?>(null)

    /**
     * Read-only state flow exposing error messages.
     */
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    /**
     * Initializes the ProfileManager by loading saved profiles.
     */
    init {
        loadProfiles()
    }

    /**
     * Loads profiles from SharedPreferences.
     *
     * Creates a default profile if no saved profiles exist.
     * Sets the current profile ID to either the previously selected profile
     * or the default profile if the previous selection is no longer valid.
     */
    private fun loadProfiles() {
        try {
            val savedProfilesJson = sharedPreferences.getString("savedProfiles", null)

            val profilesList = if (savedProfilesJson != null) {
                val jsonArray = JSONArray(savedProfilesJson)
                List(jsonArray.length()) { i ->
                    Profile.fromJson(jsonArray.getJSONObject(i))
                }
            } else {
                listOf(createDefaultProfile())
            }

            _profiles.value = profilesList
            _currentProfileId.value = sharedPreferences.getString("currentProfileId", null)

            // Ensure we have a valid current profile
            if (_currentProfileId.value == null || !profileExists(_currentProfileId.value!!)) {
                _currentProfileId.value = profilesList.firstOrNull { it.name == "Default" }?.id
                    ?: profilesList.firstOrNull()?.id
            }

            updateCurrentProfile()
        } catch (e: Exception) {
            Log.e("ProfileManager", "Error loading profiles", e)
            resetToDefaultProfile()
        }
    }

    /**
     * Creates a new default profile with empty app list.
     *
     * @return A new Profile object with name "Default"
     */
    private fun createDefaultProfile(): Profile {
        return Profile(
            name = "Default",
            appPackageNames = emptyList(),
            icon = "baseline_block_24"
        )
    }

    /**
     * Resets to a default profile when an error occurs.
     *
     * Creates a new default profile, sets it as the only available profile,
     * and makes it the current profile.
     */
    private fun resetToDefaultProfile() {
        val defaultProfile = createDefaultProfile()
        _profiles.value = listOf(defaultProfile)
        _currentProfileId.value = defaultProfile.id
        saveProfiles()
    }

    /**
     * Saves all profiles and the current profile ID to SharedPreferences.
     *
     * Converts profiles to JSON format before saving and updates the current profile.
     */
    private fun saveProfiles() {
        try {
            val jsonArray = JSONArray().apply {
                _profiles.value.forEach { put(it.toJson()) }
            }

            sharedPreferences.edit()
                .putString("savedProfiles", jsonArray.toString())
                .putString("currentProfileId", _currentProfileId.value)
                .apply()

            updateCurrentProfile()
        } catch (e: Exception) {
            Log.e("ProfileManager", "Error saving profiles", e)
        }
    }

    /**
     * Updates the current profile object based on the current profile ID.
     *
     * Falls back to the default profile or the first available profile if
     * the current profile ID doesn't match any existing profile.
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
    fun addProfile(newProfile: Profile) {
        _profiles.value = _profiles.value + newProfile
        _currentProfileId.value = newProfile.id
        saveProfiles()
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
        val index = _profiles.value.indexOfFirst { it.id == id }
        if (index != -1) {
            val updatedProfiles = _profiles.value.toMutableList()
            updatedProfiles[index] = updatedProfiles[index].copy(
                name = name ?: updatedProfiles[index].name,
                appPackageNames = appPackageNames ?: updatedProfiles[index].appPackageNames,
                icon = icon ?: updatedProfiles[index].icon
            )
            _profiles.value = updatedProfiles
            saveProfiles()
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
            saveProfiles()
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
        val updatedProfiles = _profiles.value.filter { it.id != id }

        if (updatedProfiles.isEmpty()) {
            _errorMessage.value = "You must have at least one profile"
            return
        }

        _profiles.value = updatedProfiles

        if (_currentProfileId.value == id) {
            _currentProfileId.value = updatedProfiles.firstOrNull()?.id
        }

        saveProfiles()
    }

    /**
     * Deletes all non-default profiles.
     *
     * Keeps only profiles where isDefault is true, and updates
     * the current profile if necessary.
     */
    fun deleteAllNonDefaultProfiles() {
        val defaultProfiles = _profiles.value.filter { it.isDefault }
        _profiles.value = defaultProfiles

        if (!defaultProfiles.any { it.id == _currentProfileId.value }) {
            _currentProfileId.value = defaultProfiles.firstOrNull()?.id
        }

        saveProfiles()
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
     * Retrieves a profile by its ID.
     *
     * @param id The ID of the profile to retrieve
     * @return The profile with the specified ID, or null if not found
     */
    fun getProfileById(id: String): Profile? = _profiles.value.find { it.id == id }

    /**
     * Clears the current error message.
     */
    fun clearErrorMessage() {
        _errorMessage.value = null
    }
}