package com.undistract.ui.profile

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.undistract.data.models.Profile
import com.undistract.managers.ProfileManager
import kotlinx.coroutines.flow.MutableStateFlow
//import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class ProfileFormViewModel(
    private val profileManager: ProfileManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    // Retrieve existing profile or create new form state
    private val profileId = savedStateHandle.get<String>("profileId")
    private val existingProfile = profileId?.let { profileManager.getProfileById(it) }

    // UI state with default values or existing profile data
    private val _profileName = MutableStateFlow(existingProfile?.name ?: "")
    val profileName = _profileName.asStateFlow()

    private val _profileIcon = MutableStateFlow(existingProfile?.icon ?: "baseline_block_24")
    val profileIcon = _profileIcon.asStateFlow()

    private val _selectedApps = MutableStateFlow(existingProfile?.appPackageNames ?: emptyList<String>())
    val selectedApps = _selectedApps.asStateFlow()

    // Simple property to check if we're editing or creating
    val isEditing = profileId != null

    // Update state functions
    fun updateProfileName(name: String) {
        _profileName.value = name
    }

    fun updateProfileIcon(icon: String) {
        _profileIcon.value = icon
    }

    fun updateSelectedApps(apps: List<String>) {
        _selectedApps.value = apps
    }

    // Save profile
    fun saveProfile(onComplete: () -> Unit) {
        if (isEditing) {
            profileId?.let {
                profileManager.updateProfile(
                    id = it,
                    name = _profileName.value,
                    appPackageNames = _selectedApps.value,
                    icon = _profileIcon.value
                )
            }
        } else {
            profileManager.addProfile(
                Profile(
                    id = UUID.randomUUID().toString(),
                    name = _profileName.value,
                    appPackageNames = _selectedApps.value,
                    icon = _profileIcon.value
                )
            )
        }
        onComplete()
    }

    fun deleteProfile(onComplete: () -> Unit) {
        profileId?.let {
            profileManager.deleteProfile(it)
            onComplete()
        }
    }

    // Simple validation check
    fun isFormValid() = _profileName.value.isNotBlank()
}
