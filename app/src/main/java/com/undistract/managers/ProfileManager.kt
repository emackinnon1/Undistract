package com.undistract.managers

import android.content.Context
import android.util.Log
import com.undistract.data.models.AppInfo
import com.undistract.data.models.Profile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray

class ProfileManager(private val context: Context) {
    private val sharedPreferences = context.getSharedPreferences("profile_manager_prefs", Context.MODE_PRIVATE)
    
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
    }

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

    private fun createDefaultProfile(): Profile {
        return Profile(
            name = "Default",
            appPackageNames = emptyList(),
            icon = "baseline_block_24"
        )
    }
    
    private fun resetToDefaultProfile() {
        val defaultProfile = createDefaultProfile()
        _profiles.value = listOf(defaultProfile)
        _currentProfileId.value = defaultProfile.id
        saveProfiles()
    }

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

    private fun updateCurrentProfile() {
        _currentProfile.value = _profiles.value.find { it.id == _currentProfileId.value }
            ?: _profiles.value.find { it.name == "Default" }
            ?: _profiles.value.firstOrNull()
    }
    
    private fun profileExists(id: String): Boolean = _profiles.value.any { it.id == id }

    fun addProfile(newProfile: Profile) {
        _profiles.value = _profiles.value + newProfile
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
            updatedProfiles[index] = updatedProfiles[index].copy(
                name = name ?: updatedProfiles[index].name,
                appPackageNames = appPackageNames ?: updatedProfiles[index].appPackageNames,
                icon = icon ?: updatedProfiles[index].icon
            )
            _profiles.value = updatedProfiles
            saveProfiles()
        }
    }

    fun setCurrentProfile(id: String) {
        if (profileExists(id)) {
            _currentProfileId.value = id
            saveProfiles()
        }
    }

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

    fun deleteAllNonDefaultProfiles() {
        val defaultProfiles = _profiles.value.filter { it.isDefault }
        _profiles.value = defaultProfiles
        
        if (!defaultProfiles.any { it.id == _currentProfileId.value }) {
            _currentProfileId.value = defaultProfiles.firstOrNull()?.id
        }
        
        saveProfiles()
    }

    fun getFilteredAppList(appList: List<AppInfo>): List<AppInfo> {
        return appList.filter { it.packageName != "com.undistract" }
    }

    fun getProfileById(id: String): Profile? = _profiles.value.find { it.id == id }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }
}
