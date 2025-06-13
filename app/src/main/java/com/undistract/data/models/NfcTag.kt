package com.undistract.data.models

import android.util.Log
import org.json.JSONObject
import java.util.UUID

/**
 * Data class representing an NFC tag.
 *
 * Stores identification, payload data, and creation timestamp for an NFC tag.
 * Provides functionality to convert between NfcTag objects and JSON representation.
 */
data class NfcTag(
    /**
     * Unique identifier for the tag.
     * Defaults to a randomly generated UUID if not specified.
     */
    val id: String = UUID.randomUUID().toString(),

    /**
     * The content/data stored in the NFC tag.
     */
    val payload: String,

    /**
     * Timestamp when this tag was created (milliseconds since epoch).
     * Defaults to current system time if not specified.
     */
    val createdAt: Long = System.currentTimeMillis()
) {
    /**
     * Converts this NfcTag object to a JSON representation.
     *
     * @return JSONObject containing the tag's id, payload and creation timestamp
     */
    fun toJson(): JSONObject {
        val json = JSONObject()
        json.put("id", id)
        json.put("payload", payload)
        json.put("createdAt", createdAt)
        return json
    }

    companion object {
        /**
         * Creates an NfcTag instance from its JSON representation.
         *
         * @param json JSONObject containing tag data with "id", "payload", and "createdAt" fields
         * @return NfcTag object created from the JSON data, or a default tag with error payload if parsing fails
         */
        fun fromJson(json: JSONObject): NfcTag {
            return try {
                val id = json.getString("id")
                val payload = json.getString("payload")
                val createdAt = json.getLong("createdAt")

                NfcTag(id, payload, createdAt)
            } catch (e: Exception) {
                // Log the error
                Log.e("NfcTag", "Error parsing JSON: ${e.message}")
                // Return a default tag if parsing fails
                NfcTag(payload = "error_parsing")
            }
        }
    }
}