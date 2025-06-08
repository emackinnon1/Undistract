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

class BlockerViewModel(application: Application) : AndroidViewModel(application) {
    companion object {
        private const val TAG = "BlockerViewModel"
        private const val PREFS_NAME = "nfc_tags"
        private const val TAGS_KEY = "nfc_tags"
        private const val VALID_TAG_PREFIX = "UNDISTRACT"
    }

    private val prefs = application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val appBlocker = UndistractApp.appBlocker
    private val profileManager = UndistractApp.profileManager
    private val appContext = UndistractApp.instance

    // State flows
    private val _writtenTags = MutableStateFlow<List<NfcTag>>(emptyList())
    val writtenTags = _writtenTags.asStateFlow()
    
    private val _showWrongTagAlert = MutableStateFlow(false)
    val showWrongTagAlert = _showWrongTagAlert.asStateFlow()
    
    private val _showCreateTagAlert = MutableStateFlow(false)
    val showCreateTagAlert = _showCreateTagAlert.asStateFlow()

    private val _isWritingTag = MutableStateFlow(false)
    val isWritingTag: StateFlow<Boolean> = _isWritingTag

    private val _nfcWriteSuccess = MutableStateFlow(false)
    val nfcWriteSuccess = _nfcWriteSuccess.asStateFlow()
    
    private val _nfcWriteDialogShown = MutableStateFlow(false)
    val nfcWriteDialogShown = _nfcWriteDialogShown.asStateFlow()
    
    private val _showScanTagAlert = MutableStateFlow(false)
    val showScanTagAlert = _showScanTagAlert.asStateFlow()
    
    // External state flows
    val isBlocking = appBlocker.isBlocking
    val currentProfile = profileManager.currentProfile

    init {
        loadSavedTags()
    }

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

    private fun startBlockingApps(appPackages: List<String>) {
        Intent(appContext, BlockerService::class.java).apply {
            action = BlockerService.ACTION_START_BLOCKING
            putStringArrayListExtra(BlockerService.EXTRA_APP_PACKAGES, ArrayList(appPackages))
            appContext.startService(this)
        }
        appBlocker.setBlockingState(true)
    }

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
     */
    fun generateUniqueTagPayload(): String {
        val timestamp = System.currentTimeMillis()
        val random = (1000 + kotlin.random.Random.nextInt(9000))  // 4-digit number
        return "$VALID_TAG_PREFIX-$timestamp-$random"
    }

    // Dialog management functions
    fun showScanTagAlert() {
        _showWrongTagAlert.value = false
        _showCreateTagAlert.value = false
        _showScanTagAlert.value = true
    }

    fun dismissScanTagAlert() {
        _showScanTagAlert.value = false
    }

    fun showWrongTagAlert() {
        _showScanTagAlert.value = false
        _showCreateTagAlert.value = false
        _showWrongTagAlert.value = true
    }

    fun dismissWrongTagAlert() {
        _showWrongTagAlert.value = false
    }

    fun showCreateTagAlert() {
        _showScanTagAlert.value = false
        _showWrongTagAlert.value = false
        _showCreateTagAlert.value = true
    }

    fun setWritingTag(writing: Boolean) {
        _isWritingTag.value = writing
    }

    fun cancelWrite() {
        _isWritingTag.value = false
    }

    fun hideCreateTagAlert() {
        _showCreateTagAlert.value = false
    }

    fun onCreateTagConfirmed() {
        _showCreateTagAlert.value = false
        _nfcWriteDialogShown.value = true
    }

    fun onTagWriteResult(success: Boolean) {
        _nfcWriteDialogShown.value = false
        _nfcWriteSuccess.value = success
    }

    fun dismissNfcWriteSuccessAlert() {
        _nfcWriteSuccess.value = false
    }
}
