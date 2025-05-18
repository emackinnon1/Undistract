package com.undistract.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Dark colors
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF6200EA),           // Deep electric purple
    onPrimary = Color(0xFFD6CDFF),         // Light purple for contrast
    primaryContainer = Color(0xFF0A0A14),  // Very dark blue-black
    onPrimaryContainer = Color(0xFF9D4DFF), // Bright electric purple
    secondary = Color(0xFF8C38FF),         // Electric purple (main accent)
    onSecondary = Color(0xFFE0CDFF),       // Light purple
    secondaryContainer = Color(0xFF12101A), // Dark purple-black
    onSecondaryContainer = Color(0xFFBB86FC), // Lighter purple
    tertiary = Color(0xFFA346FF),          // Neon electric purple
    onTertiary = Color(0xFF000000),        // Black text on bright tertiary
    background = Color(0xFF050510),        // Nearly black with slight purple tint
    onBackground = Color(0xFFE0E0E0),      // Light text
    surface = Color(0xFF0E0B19),           // Dark surface with purple undertone
    onSurface = Color(0xFFE9DAFF),         // Light purple-white text
    surfaceVariant = Color(0xFF1A1625),    // Slightly lighter purple-dark
    onSurfaceVariant = Color(0xFFCBB8E8),  // Soft light purple text
    error = Color(0xFFB035FF),             // Neon purple error
    onError = Color(0xFF000000),           // Black text on error
    errorContainer = Color(0xFF230F35)     // Dark purple error container
)

@Composable
fun UndistractTheme(
    darkTheme: Boolean = true, // Force dark theme by default
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography, // Your existing Typography
        content = content
    )
}
