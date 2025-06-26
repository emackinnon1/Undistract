package com.undistract.data.repositories

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.undistract.data.daos.NfcTagDao
import com.undistract.data.entities.NfcTagEntity
import com.undistract.data.local.UndistractDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NfcTagRepositoryImplTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var db: UndistractDatabase
    private lateinit var dao: NfcTagDao
    private lateinit var repository: NfcTagRepositoryImpl

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            UndistractDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = db.nfcTagDao()
        repository = NfcTagRepositoryImpl(dao)
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun saveTag_and_getAllTags() = runBlocking {
        val tag = NfcTagEntity(
            id = "tag-id",
            payload = "payload1",
        )
        val nextTag = NfcTagEntity(
            id = "update-id",
            payload = "initial",
        )
        repository.saveTag(tag)
        val allTags = repository.getAllTags().first()
        Assert.assertTrue(allTags.any { it.id == "tag-id" && it.payload == "payload1" })
        Assert.assertEquals(1, allTags.size)

        repository.saveTag(nextTag)
        val updatedTags = repository.getAllTags().first()
        Assert.assertEquals(2, updatedTags.size)
    }

    @Test
    fun updateTag_updatesPayload() = runBlocking {
        val initialTag = NfcTagEntity(
            id = "update-id",
            payload = "initial",
        )
        repository.saveTag(initialTag)

        val updatedTag = initialTag.copy(payload = "updated")
        repository.updateTag(updatedTag)
        val loaded = repository.getTagById("update-id")
        Assert.assertEquals("updated", loaded?.payload)
    }

    @Test
    fun deleteTag_removesFromRepository() = runBlocking {
        val tag = NfcTagEntity(
            id = "delete-id",
            payload = "to_delete",
        )
        repository.saveTag(tag)
        var allTags = repository.getAllTags().first()
        Assert.assertTrue(allTags.any { it.id == "delete-id" })

        repository.deleteTag(tag)
        allTags = repository.getAllTags().first()
        Assert.assertFalse(allTags.any { it.id == "delete-id" })
        Assert.assertEquals(0, allTags.size)
    }
}