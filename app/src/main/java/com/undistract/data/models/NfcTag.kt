package com.undistract.data.models

import android.util.Log
import org.json.JSONObject
import java.util.UUID

data class NfcTag(
    val id: String = UUID.randomUUID().toString(),
    val payload: String,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toJson(): JSONObject {
        val json = JSONObject()
        json.put("id", id)
        json.put("payload", payload)
        json.put("createdAt", createdAt)
        return json
    }

    companion object {
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