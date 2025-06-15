package com.undistract.data.daos

import androidx.room.Dao
import androidx.room.Upsert
import androidx.room.Query
import com.undistract.data.entities.ProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {
    @Query("SELECT * FROM profiles")
    fun getAllProfiles(): Flow<List<ProfileEntity>>

    @Upsert
    suspend fun upsertProfile(profile: ProfileEntity)

    // Additional CRUD operations
}
