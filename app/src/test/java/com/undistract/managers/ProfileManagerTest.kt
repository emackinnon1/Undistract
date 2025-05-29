package com.undistract.managers

import android.content.Context
import android.content.SharedPreferences
import android.graphics.drawable.Drawable
import com.undistract.UndistractApp
import com.undistract.data.models.AppInfo
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

//@Config(manifest = "src/main/AndroidManifest.xml")
@RunWith(RobolectricTestRunner::class)
@Config(
    application = UndistractApp::class,
    sdk = [34]
)
class ProfileManagerTest {

    private lateinit var profileManager: ProfileManager

    @Before
    fun setup() {
        // Create mocks
        val mockContext = Mockito.mock(Context::class.java)
        val mockSharedPreferences = Mockito.mock(SharedPreferences::class.java)
        val mockEditor = Mockito.mock(SharedPreferences.Editor::class.java)

        // Configure mock behavior
        Mockito.`when`(mockContext.getSharedPreferences(Mockito.anyString(), Mockito.anyInt()))
            .thenReturn(mockSharedPreferences)
        Mockito.`when`(mockSharedPreferences.edit()).thenReturn(mockEditor)
        Mockito.`when`(mockEditor.putString(Mockito.anyString(), Mockito.anyString())).thenReturn(mockEditor)
        Mockito.`when`(mockEditor.putBoolean(Mockito.anyString(), Mockito.anyBoolean())).thenReturn(mockEditor)

        profileManager = ProfileManager(mockContext)
    }

    @Test
    fun getFilteredAppList_removesOwnPackage() {
        // Create mock Drawable
        val mockDrawable = Mockito.mock(Drawable::class.java)

        // Create test app list with mock drawable
        val testAppList = listOf(
            AppInfo("com.example.app1", "App 1", mockDrawable),
            AppInfo("com.undistract", "Undistract", mockDrawable),
            AppInfo("com.example.app2", "App 2", mockDrawable)
        )

        // Call method under test
        val filteredList = profileManager.getFilteredAppList(testAppList)


        // Verify results
        Assert.assertEquals(2, filteredList.size)
        Assert.assertTrue(filteredList.none { it.packageName == "com.undistract" })
        Assert.assertTrue(filteredList.any { it.packageName == "com.example.app1" })
        Assert.assertTrue(filteredList.any { it.packageName == "com.example.app2" })
    }
}