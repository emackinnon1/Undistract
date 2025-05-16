package com.undistract

import android.app.Activity
import android.content.Intent
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.os.Parcelable
import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.charset.Charset

class NfcHelper(private val activity: Activity) {
    private val nfcAdapter: NfcAdapter? by lazy { NfcAdapter.getDefaultAdapter(activity) }
    private var onTagReadListener: ((String) -> Unit)? = null
    private var onTagWriteListener: ((Boolean) -> Unit)? = null
    private var isWriteMode = false
    private var textToWrite: String? = null

    val isNfcAvailable: Boolean get() = nfcAdapter != null
    val isNfcEnabled: Boolean get() = nfcAdapter?.isEnabled == true

    fun enableForegroundDispatch() {
        if (isNfcEnabled) {
            val intent = Intent(activity, activity.javaClass).apply {
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            val pendingIntent = android.app.PendingIntent.getActivity(
                activity, 0, intent, android.app.PendingIntent.FLAG_MUTABLE
            )
            nfcAdapter?.enableForegroundDispatch(activity, pendingIntent, null, null)
        }
    }

    fun disableForegroundDispatch() {
        if (isNfcEnabled) {
            nfcAdapter?.disableForegroundDispatch(activity)
        }
    }

    fun handleIntent(intent: Intent) {
        when (intent.action) {
            NfcAdapter.ACTION_NDEF_DISCOVERED,
            NfcAdapter.ACTION_TECH_DISCOVERED,
            NfcAdapter.ACTION_TAG_DISCOVERED -> {
                val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
                tag?.let {
                    if (isWriteMode && textToWrite != null) {
                        writeToTag(it, textToWrite!!)
                    } else {
                        readFromTag(it, intent)
                    }
                }
            }
        }
    }

    private fun readFromTag(tag: Tag, intent: Intent) {
        try {
            // First try to get NDEF message directly from intent
            intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)?.let { rawMessages ->
                val messages = rawMessages.map { it as NdefMessage }
                if (messages.isNotEmpty()) {
                    processNdefMessage(messages[0])
                    return
                }
            }

            // If not available in intent, try to read from tag
            val ndef = Ndef.get(tag)
            if (ndef != null) {
                ndef.connect()
                val ndefMessage = ndef.cachedNdefMessage
                processNdefMessage(ndefMessage)
                ndef.close()
            } else {
                showMessage("Tag doesn't contain NDEF data")
            }
        } catch (e: Exception) {
            Log.e("NfcHelper", "Error reading NFC tag", e)
            showMessage("Error reading tag: ${e.message}")
        }
    }

    private fun processNdefMessage(ndefMessage: NdefMessage) {
        for (record in ndefMessage.records) {
            when (record.tnf) {
                NdefRecord.TNF_WELL_KNOWN -> {
                    if (record.type.contentEquals(NdefRecord.RTD_TEXT)) {
                        val payload = record.payload
                        // Skip language code (first byte indicates length)
                        val languageCodeLength = payload[0].toInt() and 0x3F
                        val text = String(
                            payload,
                            1 + languageCodeLength,
                            payload.size - 1 - languageCodeLength,
                            Charset.forName("UTF-8")
                        )
                        onTagReadListener?.invoke(text)
                        return
                    } else if (record.type.contentEquals(NdefRecord.RTD_URI)) {
                        val payload = record.payload
                        // Skip URI identifier code (first byte)
                        val text = String(payload, 1, payload.size - 1, Charset.forName("UTF-8"))
                        onTagReadListener?.invoke(text)
                        return
                    }
                }
                NdefRecord.TNF_ABSOLUTE_URI -> {
                    val text = String(record.payload, Charset.forName("UTF-8"))
                    onTagReadListener?.invoke(text)
                    return
                }
            }
        }
    }

    private fun writeToTag(tag: Tag, text: String) {
        try {
            val ndef = Ndef.get(tag)
            if (ndef != null) {
                ndef.connect()
                if (!ndef.isWritable) {
                    showMessage("Tag is read-only")
                    onTagWriteListener?.invoke(false)
                    return
                }

                val record = NdefRecord.createTextRecord("en", text)
                val message = NdefMessage(arrayOf(record))

                if (message.byteArrayLength > ndef.maxSize) {
                    showMessage("Message too large for tag")
                    onTagWriteListener?.invoke(false)
                    return
                }

                ndef.writeNdefMessage(message)
                showMessage("Write successful!")
                onTagWriteListener?.invoke(true)
                ndef.close()

                // Reset write mode after successful write
                isWriteMode = false
                textToWrite = null
            } else {
                showMessage("Tag doesn't support NDEF")
                onTagWriteListener?.invoke(false)
            }
        } catch (e: Exception) {
            Log.e("NfcHelper", "Error writing to NFC tag", e)
            showMessage("Write failed: ${e.message}")
            onTagWriteListener?.invoke(false)
        }
    }

    // Public methods to start scanning or writing
    fun startScan(onRead: (String) -> Unit) {
        isWriteMode = false
        textToWrite = null
        onTagReadListener = onRead
        showMessage("Hold phone near NFC tag to read")
    }

    fun startWrite(text: String, onWrite: (Boolean) -> Unit) {
        isWriteMode = true
        textToWrite = text
        onTagWriteListener = onWrite
        showMessage("Hold phone near NFC tag to write")
    }

    private fun showMessage(message: String) {
        Toast.makeText(activity, message, Toast.LENGTH_SHORT).show()
    }
}