package com.undistract.data.daos

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.undistract.data.local.UndistractDatabase
import com.undistract.data.entities.ProfileEntity
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
    fun upsertProfile_savesToDb() = runBlocking {
        val profile = ProfileEntity(
            id = "test-id",
            name = "Test Profile",
            appPackageNames = listOf("com.example.app"),
            icon = "test_icon"
        )
        val anotherProfile = ProfileEntity(
            id = "another-id",
            name = "Another Profile",
            appPackageNames = listOf("com.another.app"),
            icon = "another_icon"
        )
        dao.upsertProfile(profile)

        var allProfiles = dao.getAllProfiles().first()
        Assert.assertTrue(allProfiles.any { it.id == "test-id" && it.name == "Test Profile" })
        Assert.assertEquals(1, allProfiles.size)

        dao.upsertProfile(anotherProfile)

        allProfiles = dao.getAllProfiles().first()
        Assert.assertEquals(2, allProfiles.size)
    }

    @Test
    fun updateProfile_appPackageNames_persistsUpdate() = runBlocking {
        // Create and insert a profile
        val profile = ProfileEntity(
            id = "test-id",
            name = "Test Profile",
            appPackageNames = listOf("com.example.app"),
            icon = "test_icon"
        )
        dao.upsertProfile(profile)

        // Find the previously-created profile
        val original = dao.getAllProfiles().first().find { it.id == "test-id" }
        Assert.assertNotNull(original)

        // Update the appPackageNames
        val updatedProfile = original!!.copy(appPackageNames = listOf("com.example.app", "com.another.app"))

        // Save the profile
        dao.upsertProfile(updatedProfile)

        // Get the profile from the database and verify the update
        val saved = dao.getAllProfiles().first().find { it.id == "test-id" }
        val allProfiles = dao.getAllProfiles().first()
        Assert.assertNotNull(saved)
        Assert.assertEquals(listOf("com.example.app", "com.another.app"), saved?.appPackageNames)
        Assert.assertEquals(1, allProfiles.size)
    }
}