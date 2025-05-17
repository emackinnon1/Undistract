package com.undistract

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import com.undistract.UndistractApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.StateFlow
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import android.content.Intent

class BlockerViewModel(application: Application) : AndroidViewModel(application) {
    private val _writtenTags = MutableStateFlow<List<NfcTag>>(emptyList())
    val writtenTags = _writtenTags.asStateFlow()

    // Store tags in SharedPreferences for persistence
    private val prefs = application.getSharedPreferences("nfc_tags", Context.MODE_PRIVATE)

    private val appBlocker = UndistractApp.appBlocker
    private val profileManager = UndistractApp.profileManager

    private val tagPhrase = "UNDISTRACT-IS-GREAT"
    private val _isBlocking = MutableStateFlow(false)

    private val _showWrongTagAlert = MutableStateFlow(false)
    val showWrongTagAlert = _showWrongTagAlert.asStateFlow()

    private val _showCreateTagAlert = MutableStateFlow(false)
    val showCreateTagAlert = _showCreateTagAlert.asStateFlow()

    private val _nfcWriteSuccess = MutableStateFlow(false)
    val nfcWriteSuccess = _nfcWriteSuccess.asStateFlow()

    private val _nfcWriteDialogShown = MutableStateFlow(false)
    val nfcWriteDialogShown = _nfcWriteDialogShown.asStateFlow()

    val isBlocking = appBlocker.isBlocking
    val currentProfile = profileManager.currentProfile

    private val _showScanTagAlert = MutableStateFlow(false)
    val showScanTagAlert: StateFlow<Boolean> = _showScanTagAlert.asStateFlow()

    init {
        // Load saved tags on init
        android.util.Log.d("BlockerViewModel", "Initializing ViewModel")
        loadSavedTags()
    }

    private fun loadSavedTags() {
        try {
            val tagsJson = prefs.getString("nfc_tags", null)
            if (tagsJson == null || tagsJson.isEmpty()) {
                _writtenTags.value = emptyList()
                return
            }

            android.util.Log.d("BlockerViewModel", "Loading tags JSON: $tagsJson")

            val jsonArray = org.json.JSONArray(tagsJson)
            val tagsList = mutableListOf<NfcTag>()

            for (i in 0 until jsonArray.length()) {
                val tagJson = jsonArray.getJSONObject(i)
                tagsList.add(NfcTag.fromJson(tagJson))
            }

            _writtenTags.value = tagsList
            android.util.Log.d("BlockerViewModel", "Loaded tags count: ${_writtenTags.value.size}")

        } catch (e: Exception) {
            android.util.Log.e("BlockerViewModel", "Error loading saved tags", e)
            e.printStackTrace() // Adds full stack trace to logcat
            _writtenTags.value = emptyList()
        }
    }

    fun saveTag(payload: String) {
        val newTag = NfcTag(payload = payload)
        val updatedTags = _writtenTags.value.toMutableList().apply { add(newTag) }
        _writtenTags.value = updatedTags

        // Persist to SharedPreferences using JSONArray
        val jsonArray = org.json.JSONArray()
        updatedTags.forEach { tag ->
            jsonArray.put(tag.toJson())
        }

        prefs.edit().putString("nfc_tags", jsonArray.toString()).apply()
        android.util.Log.d("BlockerViewModel", "Saved new tag: $payload, total tags: ${updatedTags.size}")
    }

    fun showScanTagAlert() {
        _showScanTagAlert.value = true
    }

    fun dismissScanTagAlert() {
        _showScanTagAlert.value = false
    }

    fun scanTag(payload: String) {
        viewModelScope.launch {
            dismissScanTagAlert()

            // Check if it's a valid Undistract tag
            if (payload.startsWith("UNDISTRACT")) {
                // Get current profile
                val profile = profileManager.currentProfile.value
                println("VALID TAG DETECTED FOR PROFILE: $profile")

                // Toggle blocking state using appBlocker
                profile?.let {
                    val newBlockingState = !appBlocker.isBlocking.value
                    println("New blocking state: $newBlockingState")

                    if (newBlockingState) {
                        // First ensure the accessibility service is enabled
                        ensureAccessibilityServiceEnabled(getApplication<Application>())

                        // Start blocking with current profile
                        startBlockingApps(it.appPackageNames)
                    } else {
                        // Stop blocking
                        stopBlockingApps()
                    }
                }
            } else {
                _showWrongTagAlert.value = true
            }
        }
    }

    private fun startBlockingApps(appPackages: List<String>) {
        // Implement app blocking using UsageStatsManager, AppOpsManager,
        // or a custom AccessibilityService depending on your approach

        // This could be launching a service or using system APIs
        val intent = Intent(UndistractApp.instance, BlockerService::class.java).apply {
            action = BlockerService.ACTION_START_BLOCKING
            putStringArrayListExtra(BlockerService.EXTRA_APP_PACKAGES, ArrayList(appPackages))
        }
        UndistractApp.instance.startService(intent)
        appBlocker.setBlockingState(true)
    }

    private fun stopBlockingApps() {
        // Stop the blocking service or mechanism
        val intent = Intent(UndistractApp.instance, BlockerService::class.java).apply {
            action = BlockerService.ACTION_STOP_BLOCKING
        }
        UndistractApp.instance.startService(intent)
        appBlocker.setBlockingState(false)
        println("stopBlockingApps blocking state: ${appBlocker.isBlocking}")
    }

    fun showCreateTagAlert() {
        _showCreateTagAlert.value = true
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

    fun dismissWrongTagAlert() {
        _showWrongTagAlert.value = false
    }

    fun dismissNfcWriteSuccessAlert() {
        _nfcWriteSuccess.value = false
    }
}
