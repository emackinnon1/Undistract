package com.undistract.data.repositories

import com.undistract.data.daos.ProfileDao
import com.undistract.data.models.Profile
import com.undistract.data.room.ProfileEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.undistract.data.mappers.toDomain
import com.undistract.data.mappers.toEntity

interface ProfileRepository {
    fun getAllProfiles(): Flow<List<Profile>>
    suspend fun saveProfile(profile: Profile)
    // Other methods
}

class ProfileRepositoryImpl(
    private val profileDao: ProfileDao
) : ProfileRepository {
    override fun getAllProfiles(): Flow<List<Profile>> =
        profileDao.getAllProfiles().map { entities ->
            entities.map { it.toDomain() }
        }

    override suspend fun saveProfile(profile: Profile) {
        profileDao.insertProfile(profile.toEntity())
    }
}