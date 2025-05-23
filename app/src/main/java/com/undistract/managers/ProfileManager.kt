package com.undistract.managers

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.undistract.data.models.Profile
import com.undistract.ui.profile.AppInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray

class ProfileManager(private val context: Context) {
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(
        "profile_manager_prefs", Context.MODE_PRIVATE
    )

    private val _profiles = MutableStateFlow<List<Profile>>(emptyList())
    val profiles: StateFlow<List<Profile>> = _profiles.asStateFlow()

    private val _currentProfileId = MutableStateFlow<String?>(null)
    val currentProfileId: StateFlow<String?> = _currentProfileId.asStateFlow()

    private val _currentProfile = MutableStateFlow<Profile?>(null)
    val currentProfile: StateFlow<Profile?> = _currentProfile.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()


    init {
        loadProfiles()
        ensureDefaultProfile()
    }

    private fun loadProfiles() {
        val savedProfilesJson = sharedPreferences.getString("savedProfiles", null)
        if (savedProfilesJson != null) {
            try {
                val jsonArray = JSONArray(savedProfilesJson)
                val profilesList = mutableListOf<Profile>()

                for (i in 0 until jsonArray.length()) {
                    profilesList.add(Profile.Companion.fromJson(jsonArray.getJSONObject(i)))
                }

                _profiles.value = profilesList
            } catch (e: Exception) {
                Log.e("ProfileManager", "Error loading profiles", e)
                createDefaultProfile()
            }
        } else {
            createDefaultProfile()
        }

        _currentProfileId.value = sharedPreferences.getString("currentProfileId", null)
        updateCurrentProfile()
    }

    private fun createDefaultProfile() {
        val defaultProfile = Profile(
            name = "Default",
            appPackageNames = emptyList(),
            icon = "baseline_block_24"
        )
        _profiles.value = listOf(defaultProfile)
        _currentProfileId.value = defaultProfile.id
    }

    private fun saveProfiles() {
        try {
            val jsonArray = JSONArray()
            _profiles.value.forEach { profile ->
                jsonArray.put(profile.toJson())
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

    private fun updateCurrentProfile() {
        _currentProfile.value = _profiles.value.find { it.id == _currentProfileId.value }
            ?: _profiles.value.find { it.name == "Default" }
            ?: _profiles.value.firstOrNull()
    }

    fun addProfile(newProfile: Profile) {
        val updatedProfiles = _profiles.value.toMutableList()
        updatedProfiles.add(newProfile)
        _profiles.value = updatedProfiles
        _currentProfileId.value = newProfile.id
        saveProfiles()
    }

    fun updateProfile(
        id: String,
        name: String? = null,
        appPackageNames: List<String>? = null,
        icon: String? = null
    ) {
        val index = _profiles.value.indexOfFirst { it.id == id }
        if (index != -1) {
            val updatedProfiles = _profiles.value.toMutableList()
            val currentProfile = updatedProfiles[index]

            updatedProfiles[index] = currentProfile.copy(
                name = name ?: currentProfile.name,
                appPackageNames = appPackageNames ?: currentProfile.appPackageNames,
                icon = icon ?: currentProfile.icon
            )

            _profiles.value = updatedProfiles
            saveProfiles()
        }
    }

    fun setCurrentProfile(id: String) {
        if (_profiles.value.any { it.id == id }) {
            _currentProfileId.value = id
            saveProfiles()
        }
    }

    fun deleteProfile(id: String) {
        val updatedProfiles = _profiles.value.filter { it.id != id }

        if (updatedProfiles.isNotEmpty()) {
            _profiles.value = updatedProfiles

            if (_currentProfileId.value == id) {
                _currentProfileId.value = updatedProfiles.firstOrNull()?.id
            }


            saveProfiles()
        } else {
            _errorMessage.value = "You must have at least one profile"
        }
    }

    fun deleteAllNonDefaultProfiles() {
        val updatedProfiles = _profiles.value.filter { it.isDefault }
        _profiles.value = updatedProfiles

        if (!updatedProfiles.any { it.id == _currentProfileId.value }) {
            _currentProfileId.value = updatedProfiles.firstOrNull()?.id
        }

        saveProfiles()
    }

    fun getFilteredAppList(appList: List<AppInfo>): List<AppInfo> {
        val packageName = "com.undistract" // Your app's package name
        return appList.filter { it.packageName != packageName }
    }

    fun getProfileById(id: String): Profile? {
        return _profiles.value.find { it.id == id }
    }

    private fun ensureDefaultProfile() {
        if (_profiles.value.isEmpty()) {
            createDefaultProfile()
            saveProfiles()
        } else if (_currentProfileId.value == null) {
            _currentProfileId.value = _profiles.value.find { it.name == "Default" }?.id
                ?: _profiles.value.firstOrNull()?.id
            saveProfiles()
        }
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }
}