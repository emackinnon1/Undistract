package com.undistract

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.undistract.data.models.Profile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class ProfileFormViewModel(
    private val profileManagerRepository: ProfileManagerRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    // Get profile ID from navigation arguments, if any
    private val _profileId = MutableStateFlow<String?>(savedStateHandle["profileId"])
    val profileId: StateFlow<String?> = _profileId.asStateFlow()
    private val profile = profileId.value?.let { profileManagerRepository.getProfileById(it) }

    // UI state
    private val _profileName = MutableStateFlow(profile?.name ?: "")
    val profileName = _profileName.asStateFlow()

    private val _profileIcon = MutableStateFlow(profile?.icon ?: "baseline_block_24")
    val profileIcon = _profileIcon.asStateFlow()

    private val _selectedApps = MutableStateFlow<List<String>>(profile?.appPackageNames ?: emptyList())
    val selectedApps = _selectedApps.asStateFlow()

    val isEditing: Boolean
        get() = _profileId.value != null

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
        if (profile != null) {
            profileManagerRepository.updateProfile(
                id = profile.id,
                name = _profileName.value,
                appPackageNames = _selectedApps.value,
                icon = _profileIcon.value
            )
        } else {
            val newProfile = Profile(
                id = UUID.randomUUID().toString(),
                name = _profileName.value,
                appPackageNames = _selectedApps.value,
                icon = _profileIcon.value
            )
            profileManagerRepository.addProfile(newProfile)
        }
        onComplete()
    }

    fun deleteProfile(onComplete: () -> Unit) {
        profile?.let {
            profileManagerRepository.deleteProfile(it.id)
            onComplete()
        }
    }

    // Check if form is valid
    fun isFormValid(): Boolean {
        return _profileName.value.isNotBlank()
    }
}