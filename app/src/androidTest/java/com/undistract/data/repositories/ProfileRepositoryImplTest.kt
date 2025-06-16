package com.undistract.data.repositories

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.undistract.data.daos.ProfileDao
import com.undistract.data.entities.ProfileEntity
import com.undistract.data.local.UndistractDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProfileRepositoryImplTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var db: UndistractDatabase
    private lateinit var dao: ProfileDao
    private lateinit var repository: ProfileRepositoryImpl

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            UndistractDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = db.profileDao()
        repository = ProfileRepositoryImpl(dao)
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun saveProfile_and_getAllProfiles() = runBlocking {
        val profile = ProfileEntity(
            id = "repo-id",
            name = "Repo Profile",
            appPackageNames = listOf("com.example.repo"),
            icon = "repo_icon"
        )
        repository.saveProfile(profile)
        val allProfiles = repository.getAllProfiles().first()
        Assert.assertTrue(allProfiles.any { it.id == "repo-id" && it.name == "Repo Profile" })
        Assert.assertEquals(1, allProfiles.size)
    }

    @Test
    fun updateProfile_updatesNameAndAppPackageNames() = runBlocking {
        val initialProfile = ProfileEntity(
            id = "update-id",
            name = "Initial Name",
            appPackageNames = listOf("com.example.initial"),
            icon = "icon_initial"
        )
        repository.saveProfile(initialProfile)

        // Update name
        val updatedNameProfile = initialProfile.copy(name = "Updated Name")
        repository.saveProfile(updatedNameProfile)
        var loaded = repository.getAllProfiles().first().first { it.id == "update-id" }
        Assert.assertEquals("Updated Name", loaded.name)
        Assert.assertEquals(listOf("com.example.initial"), loaded.appPackageNames)

        // Update appPackageNames
        val updatedPackagesProfile = updatedNameProfile.copy(appPackageNames = listOf("com.example.updated", "com.example.another"))
        repository.saveProfile(updatedPackagesProfile)
        loaded = repository.getAllProfiles().first().first { it.id == "update-id" }
        Assert.assertEquals("Updated Name", loaded.name)
        Assert.assertEquals(listOf("com.example.updated", "com.example.another"), loaded.appPackageNames)
    }

    @Test
    fun deleteProfile_removesFromRepository() = runBlocking {
        val profile = ProfileEntity(
            id = "delete-id",
            name = "Delete Profile",
            appPackageNames = listOf("com.example.delete"),
            icon = "delete_icon"
        )
        repository.saveProfile(profile)
        var allProfiles = repository.getAllProfiles().first()
        Assert.assertTrue(allProfiles.any { it.id == "delete-id" })

        repository.deleteProfile(profile)
        allProfiles = repository.getAllProfiles().first()
        Assert.assertFalse(allProfiles.any { it.id == "delete-id" })
        Assert.assertEquals(0, allProfiles.size)
    }
}