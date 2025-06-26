package com.undistract.data.daos

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.undistract.data.entities.NfcTagEntity
import com.undistract.data.local.UndistractDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.runner.RunWith
import java.util.Date


@RunWith(AndroidJUnit4::class)
class NfcTagDaoTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var db: UndistractDatabase
    private lateinit var dao: NfcTagDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            UndistractDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = db.nfcTagDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun insert_and_getById() = runBlocking {
        val tag = NfcTagEntity("1", "payload")
        dao.insert(tag)
        val loaded = dao.getById("1")
        Assert.assertEquals(tag, loaded)
    }

    @Test
    fun insert_and_getAll() = runBlocking {
        val now = Date()
        val later = Date(now.time + 1000)
        val tag1 = NfcTagEntity("1", "payload1", now)
        val tag2 = NfcTagEntity("2", "payload2", later)
        dao.insert(tag1)
        dao.insert(tag2)
        val all = dao.getAll().first()
        Assert.assertEquals(listOf(tag2, tag1), all) // Ordered by createdAt DESC
    }

    @Test
    fun update_tag() = runBlocking {
        val tag = NfcTagEntity("1", "payload")
        dao.insert(tag)
        val updated = tag.copy(payload = "new_payload")
        dao.update(updated)
        val loaded = dao.getById("1")
        Assert.assertEquals("new_payload", loaded?.payload)
    }

    @Test
    fun delete_tag() = runBlocking {
        val tag = NfcTagEntity("1", "payload")
        dao.insert(tag)
        dao.delete(tag)
        val loaded = dao.getById("1")
        Assert.assertNull(loaded)
    }
}