package com.undistract.data.migration

import android.content.Context
import android.util.Log
import com.undistract.data.entities.ProfileEntity
import com.undistract.data.mappers.toEntity
import com.undistract.data.models.Profile
import com.undistract.data.repositories.ProfileRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray

/**
 * Utility class to handle one-time migration of profile data
 * from SharedPreferences to Room database
 */
class ProfileMigrationUtil(
    private val context: Context,
    private val profileRepository: ProfileRepository
) {
    private val sharedPreferences = context.getSharedPreferences("profile_manager_prefs", Context.MODE_PRIVATE)
    private val migrationPref = context.getSharedPreferences("migration_prefs", Context.MODE_PRIVATE)
    private val MIGRATION_COMPLETED_KEY = "profile_migration_completed"

    /**
     * Checks if migration is needed (not already performed)
     */
    fun isMigrationNeeded(): Boolean {
        return !migrationPref.getBoolean(MIGRATION_COMPLETED_KEY, false)
    }

    /**
     * Migrates profiles from SharedPreferences to Room database
     * @return Number of profiles migrated, or -1 if an error occurred
     */
    suspend fun migrateProfiles(): Int = withContext(Dispatchers.IO) {
        if (!isMigrationNeeded()) {
            return@withContext 0  // Migration already completed
        }

        try {
            val profiles = loadProfilesFromSharedPreferences()

            // Convert and save each profile to Room
            profiles.forEach { profile ->
                val profileEntity = profile.toEntity()
                profileRepository.saveProfile(profileEntity)
            }

            // Mark migration as completed
            migrationPref.edit()
                .putBoolean(MIGRATION_COMPLETED_KEY, true)
                .apply()

            return@withContext profiles.size
        } catch (e: Exception) {
            Log.e("ProfileMigration", "Error migrating profiles", e)
            return@withContext -1
        }
    }

    /**
     * Loads profiles from SharedPreferences
     */
    private fun loadProfilesFromSharedPreferences(): List<Profile> {
        val savedProfilesJson = sharedPreferences.getString("savedProfiles", null) ?: return emptyList()

        try {
            val jsonArray = JSONArray(savedProfilesJson)
            val profiles = mutableListOf<Profile>()

            for (i in 0 until jsonArray.length()) {
                val profileJson = jsonArray.getJSONObject(i)
                val profile = Profile.fromJson(profileJson)
                profiles.add(profile)
            }

            return profiles
        } catch (e: Exception) {
            Log.e("ProfileMigration", "Error parsing profiles from SharedPreferences", e)
            return emptyList()
        }
    }

    /**
     * Gets the ID of the currently selected profile from SharedPreferences
     */
    fun getCurrentProfileId(): String? {
        return sharedPreferences.getString("currentProfileId", null)
    }
}