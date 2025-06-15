package com.undistract.data.daos

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.undistract.data.daos.ProfileDao
import com.undistract.data.local.UndistractDatabase
import com.undistract.data.room.ProfileEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProfileDaoTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var db: UndistractDatabase
    private lateinit var dao: ProfileDao

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            UndistractDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = db.profileDao()
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun insertProfile_savesToDb() = runBlocking {
        val profile = ProfileEntity(
            id = "test-id",
            name = "Test Profile",
            appPackageNames = listOf("com.example.app"),
            icon = "test_icon"
        )
        dao.insertProfile(profile)

        val savedProfile = dao.getAllProfiles().first()
        Assert.assertTrue(savedProfile.any { it.id == "test-id" && it.name == "Test Profile" })
    }
}