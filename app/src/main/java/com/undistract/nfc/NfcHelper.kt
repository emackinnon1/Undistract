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

class NfcHelper(private val activity: Activity) {
    private val nfcAdapter: NfcAdapter? by lazy { NfcAdapter.getDefaultAdapter(activity) }
    private var onTagReadListener: ((String) -> Unit)? = null
    private var onTagWriteListener: ((Boolean) -> Unit)? = null
    private var isWriteMode = false
    private var textToWrite: String? = null

    val isNfcAvailable: Boolean get() = nfcAdapter != null
    val isNfcEnabled: Boolean get() = nfcAdapter?.isEnabled == true

    fun enableForegroundDispatch() {
        if (!isNfcEnabled) return
        
        val intent = Intent(activity, activity.javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val pendingIntent = PendingIntent.getActivity(activity, 0, intent, PendingIntent.FLAG_MUTABLE)
        nfcAdapter?.enableForegroundDispatch(activity, pendingIntent, null, null)
    }

    fun disableForegroundDispatch() {
        if (isNfcEnabled) {
            nfcAdapter?.disableForegroundDispatch(activity)
        }
    }

    fun handleIntent(intent: Intent) {
        if (!isNfcIntent(intent)) return

        intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)?.let { tag ->
            when {
                isWriteMode && textToWrite != null -> writeToTag(tag, textToWrite!!)
                else -> readFromTag(tag, intent)
            }
        }
    }

    fun isNfcIntent(intent: Intent): Boolean = intent.action in listOf(
        NfcAdapter.ACTION_NDEF_DISCOVERED,
        NfcAdapter.ACTION_TECH_DISCOVERED,
        NfcAdapter.ACTION_TAG_DISCOVERED
    )

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
