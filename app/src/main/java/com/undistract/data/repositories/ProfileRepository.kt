package com.undistract.data.repositories

import com.undistract.data.daos.ProfileDao
import kotlinx.coroutines.flow.Flow
import com.undistract.data.entities.ProfileEntity

interface ProfileRepository {
    fun getAllProfiles(): Flow<List<ProfileEntity>>
    suspend fun saveProfile(profile: ProfileEntity)
    // Other methods
}

class ProfileRepositoryImpl(
    private val profileDao: ProfileDao
) : ProfileRepository {
    override fun getAllProfiles(): Flow<List<ProfileEntity>> =
        profileDao.getAllProfiles()

    override suspend fun saveProfile(profile: ProfileEntity) {
        profileDao.upsertProfile(profile)
    }
}