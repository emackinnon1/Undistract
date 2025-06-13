package com.undistract.data.models

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Data class representing a user profile configuration.
 *
 * Stores identifying information, name, associated application package names,
 * and an icon identifier for a profile. Provides functionality to convert
 * between Profile objects and JSON representation.
 */
data class Profile(
    /**
     * Unique identifier for the profile.
     * Defaults to a randomly generated UUID if not specified.
     */
    val id: String = UUID.randomUUID().toString(),

    /**
     * The display name of the profile.
     */
    var name: String,

    /**
     * List of package names for applications associated with this profile.
     * Defaults to an empty list if not specified.
     */
    var appPackageNames: List<String> = emptyList(),

    /**
     * Icon identifier used to represent this profile visually.
     * Defaults to "baseline_block_24" if not specified.
     */
    var icon: String = "baseline_block_24"
) {
    /**
     * Indicates whether this is the default profile.
     * A profile is considered default if its name is "Default".
     */
    val isDefault: Boolean
        get() = name == "Default"

    /**
     * Converts this Profile object to a JSON representation.
     *
     * @return JSONObject containing the profile's id, name, icon, and appPackageNames
     */
    fun toJson(): JSONObject {
        val json = JSONObject()
        json.put("id", id)
        json.put("name", name)
        json.put("icon", icon)

        val appsJsonArray = JSONArray()
        appPackageNames.forEach { appsJsonArray.put(it) }
        json.put("appPackageNames", appsJsonArray)

        return json
    }

    companion object {
        /**
         * Creates a Profile instance from its JSON representation.
         *
         * @param json JSONObject containing profile data with "id", "name", "icon", and "appPackageNames" fields
         * @return Profile object created from the JSON data
         */
        fun fromJson(json: JSONObject): Profile {
            val id = json.getString("id")
            val name = json.getString("name")
            val icon = json.getString("icon")

            val appsJsonArray = json.getJSONArray("appPackageNames")
            val appPackageNames = mutableListOf<String>()
            for (i in 0 until appsJsonArray.length()) {
                appPackageNames.add(appsJsonArray.getString(i))
            }

            return Profile(id, name, appPackageNames, icon)
        }
    }
}