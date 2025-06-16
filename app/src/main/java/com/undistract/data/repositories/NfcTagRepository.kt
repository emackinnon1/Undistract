package com.undistract.data.repositories

import com.undistract.data.daos.NfcTagDao
import com.undistract.data.entities.NfcTagEntity
import kotlinx.coroutines.flow.Flow

interface NfcTagRepository {
    fun getAllTags(): Flow<List<NfcTagEntity>>
    suspend fun getTagById(id: String): NfcTagEntity?
    suspend fun saveTag(tag: NfcTagEntity)
    suspend fun updateTag(tag: NfcTagEntity)
    suspend fun deleteTag(tag: NfcTagEntity)
}

class NfcTagRepositoryImpl(
    private val nfcTagDao: NfcTagDao
) : NfcTagRepository {
    override fun getAllTags(): Flow<List<NfcTagEntity>> = nfcTagDao.getAll()

    override suspend fun getTagById(id: String): NfcTagEntity? = nfcTagDao.getById(id)

    override suspend fun saveTag(tag: NfcTagEntity) {
        nfcTagDao.insert(tag)
    }

    override suspend fun updateTag(tag: NfcTagEntity) {
        nfcTagDao.update(tag)
    }

    override suspend fun deleteTag(tag: NfcTagEntity) {
        nfcTagDao.delete(tag)
    }
}