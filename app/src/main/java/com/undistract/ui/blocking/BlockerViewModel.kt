package com.undistract.ui.blocking

import android.app.Application
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.undistract.UndistractApp
import com.undistract.data.models.NfcTag
import com.undistract.services.AppBlockerAccessibilityService
import com.undistract.services.BlockerService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray

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
    private val _writtenTags = MutableStateFlow<List<NfcTag>>(emptyList())
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

    // External state flows
    /** Current blocking status from the app blocker */
    val isBlocking = appBlocker.isBlocking
    /** Current profile from the profile manager */
    val currentProfile = profileManager.currentProfile

    /**
     * Initializes the ViewModel and loads saved NFC tags from SharedPreferences.
     */
    init {
        loadSavedTags()
    }

    /**
     * Loads previously saved NFC tags from SharedPreferences.
     *
     * Tags are stored as a JSON array string and converted back to NfcTag objects.
     * If no tags are found or an error occurs, an empty list is set.
     */
    private fun loadSavedTags() {
        try {
            val tagsJson = prefs.getString(TAGS_KEY, null)
            if (tagsJson.isNullOrEmpty()) {
                _writtenTags.value = emptyList()
                return
            }

            Log.d(TAG, "Loading tags JSON: $tagsJson")
            val jsonArray = JSONArray(tagsJson)
            val tagsList = mutableListOf<NfcTag>()

            for (i in 0 until jsonArray.length()) {
                tagsList.add(NfcTag.fromJson(jsonArray.getJSONObject(i)))
            }

            _writtenTags.value = tagsList
            Log.d(TAG, "Loaded ${tagsList.size} tags")
        } catch (e: Exception) {
            Log.e(TAG, "Error loading saved tags", e)
            _writtenTags.value = emptyList()
        }
    }

    /**
     * Saves a new NFC tag with the given payload.
     *
     * The tag is added to the in-memory list and persisted to SharedPreferences.
     *
     * @param payload The string payload to save in the NFC tag
     */
    fun saveTag(payload: String) {
        val newTag = NfcTag(payload = payload)
        val updatedTags = _writtenTags.value.toMutableList().apply { add(newTag) }
        _writtenTags.value = updatedTags

        // Persist to SharedPreferences
        prefs.edit().putString(TAGS_KEY, JSONArray().apply {
            updatedTags.forEach { put(it.toJson()) }
        }.toString()).apply()

        Log.d(TAG, "Saved tag: $payload, total: ${updatedTags.size}")
    }

    /**
     * Processes a scanned NFC tag by validating its payload.
     *
     * If the payload starts with the valid prefix, app blocking is toggled.
     * Otherwise, shows an alert for an invalid tag.
     *
     * @param payload The string payload from the scanned NFC tag
     */
    fun scanTag(payload: String) {
        viewModelScope.launch {
            dismissScanTagAlert()

            if (payload.startsWith(VALID_TAG_PREFIX)) {
                toggleBlocking()
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
     * @param tag The NfcTag object to delete
     */
    fun deleteTag(tag: NfcTag) {
        val updatedTags = _writtenTags.value.toMutableList().apply {
            remove(tag)
        }
        _writtenTags.value = updatedTags

        // Persist to SharedPreferences
        prefs.edit().putString(TAGS_KEY, JSONArray().apply {
            updatedTags.forEach { put(it.toJson()) }
        }.toString()).apply()

        Log.d(TAG, "Deleted tag: ${tag.payload}, remaining: ${updatedTags.size}")
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
    fun generateUniqueTagPayload(): String {
        val timestamp = System.currentTimeMillis()
        val random = (1000 + kotlin.random.Random.nextInt(9000))  // 4-digit number
        return "$VALID_TAG_PREFIX-$timestamp-$random"
    }

    /**
     * Shows the scan tag alert dialog and hides other dialogs.
     */
    fun showScanTagAlert() {
        _showWrongTagAlert.value = false
        _showCreateTagAlert.value = false
        _showScanTagAlert.value = true
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
}