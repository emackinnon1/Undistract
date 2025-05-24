package com.undistract

import android.content.Context
import android.content.SharedPreferences
import android.graphics.drawable.Drawable
import com.undistract.data.models.AppInfo
import com.undistract.managers.ProfileManager
import org.junit.Test
import org.junit.Assert.*
import org.junit.Before
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

//@Config(manifest = "src/main/AndroidManifest.xml")
@RunWith(RobolectricTestRunner::class)
@Config(
    application = com.undistract.UndistractApp::class,
    sdk = [34]
)
class ProfileManagerTest {

    private lateinit var profileManager: ProfileManager

    @Before
    fun setup() {
        // Create mocks
        val mockContext = mock(Context::class.java)
        val mockSharedPreferences = mock(SharedPreferences::class.java)
        val mockEditor = mock(SharedPreferences.Editor::class.java)

        // Configure mock behavior
        `when`(mockContext.getSharedPreferences(Mockito.anyString(), Mockito.anyInt()))
            .thenReturn(mockSharedPreferences)
        `when`(mockSharedPreferences.edit()).thenReturn(mockEditor)
        `when`(mockEditor.putString(Mockito.anyString(), Mockito.anyString())).thenReturn(mockEditor)
        `when`(mockEditor.putBoolean(Mockito.anyString(), Mockito.anyBoolean())).thenReturn(mockEditor)

        profileManager = ProfileManager(mockContext)
    }

    @Test
    fun getFilteredAppList_removesOwnPackage() {
        // Create mock Drawable
        val mockDrawable = mock(Drawable::class.java)

        // Create test app list with mock drawable
        val testAppList = listOf(
            AppInfo("com.example.app1", "App 1", mockDrawable),
            AppInfo("com.undistract", "Undistract", mockDrawable),
            AppInfo("com.example.app2", "App 2", mockDrawable)
        )

        // Call method under test
        val filteredList = profileManager.getFilteredAppList(testAppList)


        // Verify results
        assertEquals(2, filteredList.size)
        assertTrue(filteredList.none { it.packageName == "com.undistract" })
        assertTrue(filteredList.any { it.packageName == "com.example.app1" })
        assertTrue(filteredList.any { it.packageName == "com.example.app2" })
    }
}