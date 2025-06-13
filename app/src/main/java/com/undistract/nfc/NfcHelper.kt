package com.undistract.nfc

import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.util.Log
import android.widget.Toast
import java.nio.charset.Charset

/**
 * Helper class for handling NFC (Near Field Communication) operations in Android.
 *
 * Provides functionality to read from and write to NFC tags, handling the appropriate
 * Android NFC intents and lifecycle callbacks. It simplifies working with NFC tags by
 * providing an easy-to-use interface for common NFC operations.
 *
 * @property activity The Android Activity context used for NFC operations
 */
class NfcHelper(private val activity: Activity) {
    /**
     * The NFC adapter for the device, or null if NFC is not available.
     */
    private val nfcAdapter: NfcAdapter? by lazy { NfcAdapter.getDefaultAdapter(activity) }

    /**
     * Callback invoked when an NFC tag is successfully read.
     */
    private var onTagReadListener: ((String) -> Unit)? = null

    /**
     * Callback invoked after attempting to write to an NFC tag.
     * Provides a boolean indicating success (true) or failure (false).
     */
    private var onTagWriteListener: ((Boolean) -> Unit)? = null

    /**
     * Indicates whether the helper is in write mode (true) or read mode (false).
     */
    private var isWriteMode = false

    /**
     * The text to be written to the next detected NFC tag when in write mode.
     */
    private var textToWrite: String? = null

    /**
     * Indicates whether NFC is available on this device.
     */
    val isNfcAvailable: Boolean get() = nfcAdapter != null

    /**
     * Indicates whether NFC is enabled on this device.
     */
    val isNfcEnabled: Boolean get() = nfcAdapter?.isEnabled == true

    /**
     * Enables the foreground dispatch system for detecting NFC tags.
     *
     * Should be called in the activity's onResume method.
     */
    fun enableForegroundDispatch() {
        if (!isNfcEnabled) return

        val intent = Intent(activity, activity.javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val pendingIntent = PendingIntent.getActivity(activity, 0, intent, PendingIntent.FLAG_MUTABLE)
        nfcAdapter?.enableForegroundDispatch(activity, pendingIntent, null, null)
    }

    /**
     * Disables the foreground dispatch system.
     *
     * Should be called in the activity's onPause method.
     */
    fun disableForegroundDispatch() {
        if (isNfcEnabled) {
            nfcAdapter?.disableForegroundDispatch(activity)
        }
    }

    /**
     * Handles an intent that may contain NFC tag data.
     *
     * Call this from the activity's onNewIntent method.
     *
     * @param intent The intent to process, typically from onNewIntent
     */
    fun handleIntent(intent: Intent) {
        if (!isNfcIntent(intent)) return

        intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)?.let { tag ->
            when {
                isWriteMode && textToWrite != null -> writeToTag(tag, textToWrite!!)
                else -> readFromTag(tag, intent)
            }
        }
    }

    /**
     * Checks if the given intent contains NFC tag data.
     *
     * @param intent The intent to check
     * @return true if the intent contains NFC tag data, false otherwise
     */
    fun isNfcIntent(intent: Intent): Boolean = intent.action in listOf(
        NfcAdapter.ACTION_NDEF_DISCOVERED,
        NfcAdapter.ACTION_TECH_DISCOVERED,
        NfcAdapter.ACTION_TAG_DISCOVERED
    )

    /**
     * Reads data from an NFC tag.
     *
     * @param tag The detected NFC tag
     * @param intent The intent that triggered the tag detection
     */
    private fun readFromTag(tag: Tag, intent: Intent) {
        try {
            // Try to get NDEF message from intent first
            intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)?.let { rawMessages ->
                (rawMessages.firstOrNull() as? NdefMessage)?.let {
                    processNdefMessage(it)
                    return
                }
            }

            // Otherwise read from tag
            Ndef.get(tag)?.apply {
                connect()
                try {
                    cachedNdefMessage?.let { processNdefMessage(it) }
                        ?: showMessage("Tag doesn't contain NDEF data")
                } finally {
                    close()
                }
            } ?: showMessage("Tag doesn't support NDEF")
        } catch (e: Exception) {
            Log.e("NfcHelper", "Error reading NFC tag", e)
            showMessage("Error reading tag: ${e.message}")
        }
    }

    /**
     * Processes an NDEF message and extracts text content.
     *
     * @param ndefMessage The NDEF message to process
     */
    private fun processNdefMessage(ndefMessage: NdefMessage) {
        for (record in ndefMessage.records) {
            val text = when {
                // Text record
                record.tnf == NdefRecord.TNF_WELL_KNOWN &&
                record.type.contentEquals(NdefRecord.RTD_TEXT) -> {
                    val payload = record.payload
                    val langCodeLength = payload[0].toInt() and 0x3F
                    String(
                        payload,
                        1 + langCodeLength,
                        payload.size - 1 - langCodeLength,
                        Charset.forName("UTF-8")
                    )
                }

                // URI record
                record.tnf == NdefRecord.TNF_WELL_KNOWN &&
                record.type.contentEquals(NdefRecord.RTD_URI) -> {
                    String(record.payload, 1, record.payload.size - 1, Charset.forName("UTF-8"))
                }

                // Absolute URI
                record.tnf == NdefRecord.TNF_ABSOLUTE_URI -> {
                    String(record.payload, Charset.forName("UTF-8"))
                }

                else -> null
            }

            if (text != null) {
                onTagReadListener?.invoke(text)
                return
            }
        }
    }

    /**
     * Writes text to an NFC tag.
     *
     * @param tag The NFC tag to write to
     * @param text The text content to write
     */
    private fun writeToTag(tag: Tag, text: String) {
        try {
            Ndef.get(tag)?.apply {
                connect()
                try {
                    when {
                        !isWritable -> {
                            showMessage("Tag is read-only")
                            onTagWriteListener?.invoke(false)
                        }
                        else -> {
                            val record = NdefRecord.createTextRecord("en", text)
                            val message = NdefMessage(arrayOf(record))

                            if (message.byteArrayLength > maxSize) {
                                showMessage("Message too large for tag")
                                onTagWriteListener?.invoke(false)
                                return
                            }

                            writeNdefMessage(message)
                            showMessage("Write successful!")
                            onTagWriteListener?.invoke(true)

                            // Reset write mode after successful write
                            isWriteMode = false
                            textToWrite = null
                        }
                    }
                } finally {
                    close()
                }
            } ?: run {
                showMessage("Tag doesn't support NDEF")
                onTagWriteListener?.invoke(false)
            }
        } catch (e: Exception) {
            Log.e("NfcHelper", "Error writing to NFC tag", e)
            showMessage("Write failed: ${e.message}")
            onTagWriteListener?.invoke(false)
        }
    }

    /**
     * Starts scanning for NFC tags in read mode.
     *
     * @param onRead Callback function invoked with the text content of the read tag
     */
    fun startScan(onRead: (String) -> Unit) {
        isWriteMode = false
        textToWrite = null
        onTagReadListener = onRead
        showMessage("Hold phone near NFC tag to read")
    }

    /**
     * Starts scanning for NFC tags in write mode.
     *
     * @param text The text to write to the next detected NFC tag
     * @param onWrite Callback function invoked with the result of the write operation
     */
    fun startWrite(text: String, onWrite: (Boolean) -> Unit) {
        isWriteMode = true
        textToWrite = text
        onTagWriteListener = onWrite
        showMessage("Hold phone near NFC tag to write")
    }

    /**
     * Shows a toast message to the user.
     *
     * @param message The message to display
     */
    private fun showMessage(message: String) {
        Toast.makeText(activity, message, Toast.LENGTH_SHORT).show()
    }
}