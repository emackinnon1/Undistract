package com.undistract.data.models

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class Profile(
    val id: String = UUID.randomUUID().toString(),
    var name: String,
    var appPackageNames: List<String> = emptyList(),
    var icon: String = "baseline_block_24"
) {
    val isDefault: Boolean
        get() = name == "Default"

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