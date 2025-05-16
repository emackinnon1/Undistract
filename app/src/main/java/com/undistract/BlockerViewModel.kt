package com.undistract

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.undistract.UndistractApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.StateFlow

class BlockerViewModel(application: Application) : AndroidViewModel(application) {
    private val appBlocker = UndistractApp.appBlocker
    private val profileManager = UndistractApp.profileManager

    private val tagPhrase = "BROKE-IS-GREAT"

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

    // In BlockerViewModel.kt
    private val _showScanTagAlert = MutableStateFlow(false)
    val showScanTagAlert: StateFlow<Boolean> = _showScanTagAlert.asStateFlow()

    fun showScanTagAlert() {
        _showScanTagAlert.value = true
    }

    fun dismissScanTagAlert() {
        _showScanTagAlert.value = false
    }

    fun scanTag(payload: String) {
        if (payload == tagPhrase) {
            profileManager.currentProfile.value?.let { profile ->
                appBlocker.toggleBlocking(profile)
            }
        } else {
            _showWrongTagAlert.value = true
        }
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