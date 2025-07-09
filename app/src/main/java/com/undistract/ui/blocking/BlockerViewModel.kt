package com.undistract.ui.blocking

import android.app.Application
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.undistract.UndistractApp
import com.undistract.data.entities.NfcTagEntity
import com.undistract.data.repositories.NfcTagRepository
import com.undistract.data.repositories.NfcTagRepositoryImpl
import com.undistract.services.AppBlockerAccessibilityService
import com.undistract.services.BlockerService
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch


/**
 * ViewModel responsible for managing app blocking functionality using NFC tags.
 *
 * This ViewModel handles the operations related to app blocking, including:
 * - Managing NFC tag data (creating, saving, deleting)
 * - Controlling app blocking state (enabling/disabling)
 * - Managing UI state for various dialogs and alerts
 * - Communicating with BlockerService to control app blocking
 *
 * @property application The Android application context
 */
class BlockerViewModel(application: Application) : AndroidViewModel(application) {
    /**
     * Constants and utility methods for the BlockerViewModel.
     */
    companion object {
        /** Tag for logging purposes */
        private const val TAG = "BlockerViewModel"
        /** Name of SharedPreferences file for storing NFC tag data */
        private const val PREFS_NAME = "nfc_tags"
        /** Key for storing NFC tags in SharedPreferences */
        private const val TAGS_KEY = "nfc_tags"
        /** Prefix that identifies valid Undistract NFC tags */
        private const val VALID_TAG_PREFIX = "UNDISTRACT"
    }

    /** SharedPreferences for persisting NFC tag data */
    private val prefs = application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    /** Reference to the application's app blocker component */
    private val appBlocker = UndistractApp.appBlocker
    /** Reference to the application's profile manager component */
    private val profileManager = UndistractApp.profileManager
    /** Application context */
    private val appContext = UndistractApp.instance

    // State flows
    /** List of NFC tags written by the user */
    private val _writtenTags = MutableStateFlow<List<NfcTagEntity>>(emptyList())
    /** Public immutable flow of written NFC tags */
    val writtenTags = _writtenTags.asStateFlow()

    /** Controls visibility of the wrong tag alert dialog */
    private val _showWrongTagAlert = MutableStateFlow(false)
    /** Public immutable flow for wrong tag alert visibility */
    val showWrongTagAlert = _showWrongTagAlert.asStateFlow()

    /** Controls visibility of the create tag dialog */
    private val _showCreateTagAlert = MutableStateFlow(false)
    /** Public immutable flow for create tag dialog visibility */
    val showCreateTagAlert = _showCreateTagAlert.asStateFlow()

    /** Indicates whether tag writing is in progress */
    private val _isWritingTag = MutableStateFlow(false)
    /** Public immutable flow for tag writing status */
    val isWritingTag: StateFlow<Boolean> = _isWritingTag

    /** Indicates whether NFC tag was successfully written */
    private val _nfcWriteSuccess = MutableStateFlow(false)
    /** Public immutable flow for NFC write success status */
    val nfcWriteSuccess = _nfcWriteSuccess.asStateFlow()

    /** Controls visibility of the NFC write dialog */
    private val _nfcWriteDialogShown = MutableStateFlow(false)
    /** Public immutable flow for NFC write dialog visibility */
    val nfcWriteDialogShown = _nfcWriteDialogShown.asStateFlow()

    /** Controls visibility of the scan tag alert dialog */
    private val _showScanTagAlert = MutableStateFlow(false)
    /** Public immutable flow for scan tag alert visibility */
    val showScanTagAlert = _showScanTagAlert.asStateFlow()

    /** Indicates whether no tags exist in the system */
    private val _noTagsExistAlert = MutableStateFlow(false)
    /** Public immutable flow for no tags exist alert visibility */
    val noTagsExistAlert: StateFlow<Boolean> = _noTagsExistAlert

    // External state flows
    /** Current blocking status from the app blocker */
    val isBlocking = appBlocker.isBlocking
    /** Current profile from the profile manager */
    val currentProfile = profileManager.currentProfile

    val db = UndistractApp.db
    val nfcTagDao = db.nfcTagDao()
    val nfcTagRepo: NfcTagRepository = NfcTagRepositoryImpl(nfcTagDao)

    /**
     * Initializes the ViewModel and loads saved NFC tags from SharedPreferences.
     */
    init {
        viewModelScope.launch { loadSavedTags() }
    }

    /**
     * Loads previously saved NFC tags from SharedPreferences.
     *
     * Tags are stored as a JSON array string and converted back to NfcTag objects.
     * If no tags are found or an error occurs, an empty list is set.
     */

    private suspend fun loadSavedTags() {
        // Get all tags from the repository (Room DB)
        val entities = nfcTagRepo.getAllTags().first()
        _writtenTags.value = entities
    }

    /**
     * Saves a new NFC tag with the given payload.
     *
     * The tag is added to the in-memory list and persisted to SharedPreferences.
     *
     * @param uniqueId The unique id to save in as the primary key for the NFC tag
     * TODO: payload is hardcoded to "profile_tag" for now, it will be dynamic as features expand
     */
    fun saveTag(uniqueId: String) {
        viewModelScope.launch {
            val newTag = NfcTagEntity(id = uniqueId, payload = "profile_tag")
            nfcTagRepo.saveTag(newTag)
            loadSavedTags()
            Log.d(TAG, "Saved tag with id: $uniqueId")
        }
    }

    /**
     * Processes a scanned NFC tag by validating its payload.
     *
     * If the payload starts with the valid prefix, app blocking is toggled.
     * Otherwise, shows an alert for an invalid tag.
     *
     * @param id The string payload from the scanned NFC tag
     */
    fun scanTag(id: String) {
        viewModelScope.launch {
            dismissScanTagAlert()

            // First check if it's an Undistract tag format
            if (id.startsWith(VALID_TAG_PREFIX)) {
                // Then verify this specific tag ID exists in our database
                val tagExists = _writtenTags.value.any { it.id == id }
                if (tagExists) {
                    toggleBlocking()
                } else {
                    showWrongTagAlert() // Tag format is valid but not in our database
                }
            } else {
                showWrongTagAlert()
            }
        }
    }

    /**
     * Deletes an NFC tag from the saved list.
     *
     * The tag is removed from the in-memory list and the change is persisted to SharedPreferences.
     *
     * @param tag The NfcTagEntity object to delete
     */
    fun deleteTag(tag: NfcTagEntity) {
        viewModelScope.launch {
            nfcTagRepo.deleteTag(tag)
            loadSavedTags()
            Log.d(TAG, "Deleted tag with id: ${tag.id}")
        }
    }

    /**
     * Toggles the app blocking state based on the current profile.
     *
     * If blocking is currently off, it enables blocking for the apps in the current profile.
     * If blocking is on, it disables blocking.
     */
    private fun toggleBlocking() {
        profileManager.currentProfile.value?.let { profile ->
            val newBlockingState = !appBlocker.isBlocking.value
            Log.d(TAG, "Toggling blocking to: $newBlockingState for profile: ${profile.name}")

            if (newBlockingState) {
                AppBlockerAccessibilityService.ensureAccessibilityServiceEnabled(getApplication())
                startBlockingApps(profile.appPackageNames)
            } else {
                stopBlockingApps()
            }
        }
    }

    /**
     * Starts blocking the specified apps by initiating the BlockerService.
     *
     * @param appPackages List of package names to block
     */
    private fun startBlockingApps(appPackages: List<String>) {
        Intent(appContext, BlockerService::class.java).apply {
            action = BlockerService.ACTION_START_BLOCKING
            putStringArrayListExtra(BlockerService.EXTRA_APP_PACKAGES, ArrayList(appPackages))
            appContext.startService(this)
        }
        appBlocker.setBlockingState(true)
    }

    /**
     * Stops blocking all apps by sending a stop command to the BlockerService.
     */
    private fun stopBlockingApps() {
        Intent(appContext, BlockerService::class.java).apply {
            action = BlockerService.ACTION_STOP_BLOCKING
            appContext.startService(this)
        }
        appBlocker.setBlockingState(false)
    }

    /**
     * Generates a unique tag payload with the UNDISTRACT prefix and a unique identifier
     * Format: UNDISTRACT-{timestamp}-{random}
     *
     * @return A unique string payload for a new NFC tag
     */
    fun generateUniqueTagId(): String {
        val timestamp = System.currentTimeMillis()
        val random = (1000 + kotlin.random.Random.nextInt(9000))  // 4-digit number
        return "$VALID_TAG_PREFIX-$timestamp-$random"
    }

    /**
     * Shows the scan tag alert dialog and hides other dialogs.
     */
    fun showScanTagAlert() {
        if (_writtenTags.value.isNotEmpty()) {
            _showWrongTagAlert.value = false
            _showCreateTagAlert.value = false
            _showScanTagAlert.value = true
        } else {
            _noTagsExistAlert.value = true
        }
    }

    /**
     * Dismisses the scan tag alert dialog.
     */
    fun dismissScanTagAlert() {
        _showScanTagAlert.value = false
    }

    /**
     * Shows the wrong tag alert dialog and hides other dialogs.
     */
    fun showWrongTagAlert() {
        _showScanTagAlert.value = false
        _showCreateTagAlert.value = false
        _showWrongTagAlert.value = true
    }

    /**
     * Dismisses the wrong tag alert dialog.
     */
    fun dismissWrongTagAlert() {
        _showWrongTagAlert.value = false
    }

    /**
     * Shows the create tag alert dialog and hides other dialogs.
     */
    fun showCreateTagAlert() {
        _showScanTagAlert.value = false
        _showWrongTagAlert.value = false
        _showCreateTagAlert.value = true
    }

    /**
     * Sets the writing tag state.
     *
     * @param writing True if tag writing is in progress, false otherwise
     */
    fun setWritingTag(writing: Boolean) {
        _isWritingTag.value = writing
    }

    /**
     * Cancels the current tag writing operation.
     */
    fun cancelWrite() {
        _isWritingTag.value = false
    }

    /**
     * Hides the create tag alert dialog.
     */
    fun hideCreateTagAlert() {
        _showCreateTagAlert.value = false
    }

    /**
     * Confirms tag creation and shows the NFC write dialog.
     */
    fun onCreateTagConfirmed() {
        _showCreateTagAlert.value = false
        _nfcWriteDialogShown.value = true
    }

    /**
     * Handles the result of tag writing operation.
     *
     * @param success True if tag was successfully written, false otherwise
     */
    fun onTagWriteResult(success: Boolean) {
        _nfcWriteDialogShown.value = false
        _nfcWriteSuccess.value = success
    }

    /**
     * Dismisses the NFC write success alert.
     */
    fun dismissNfcWriteSuccessAlert() {
        _nfcWriteSuccess.value = false
    }

    /**
     * Dismisses the alert shown when no tags exist.
     */
    fun dismissNoTagsExistAlert() {
        _noTagsExistAlert.value = false
    }
}