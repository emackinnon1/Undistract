package com.undistract.data.daos

import androidx.room.*
import com.undistract.data.entities.NfcTagEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NfcTagDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(tag: NfcTagEntity)

    @Delete
    suspend fun delete(tag: NfcTagEntity)

    @Update
    suspend fun update(tag: NfcTagEntity)

    @Query("SELECT * FROM nfc_tags WHERE id = :id")
    suspend fun getById(id: String): NfcTagEntity?

    @Query("SELECT * FROM nfc_tags ORDER BY createdAt DESC")
    fun getAll(): Flow<List<NfcTagEntity>>
}