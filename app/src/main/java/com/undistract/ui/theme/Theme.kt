package com.undistract.ui.theme


import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Dark colors
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF90CAF9),           // Light blue
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF1E1E1E),  // Dark container
    onPrimaryContainer = Color(0xFFE0E0E0), // Light text
    secondary = Color(0xFF80DEEA),         // Light cyan
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF252525), // Dark container
    onSecondaryContainer = Color(0xFFE0E0E0), // Light text
    tertiary = Color(0xFF9FA8DA),          // Light indigo
    onTertiary = Color.Black,
    background = Color(0xFF121212),        // Dark background
    onBackground = Color(0xFFE0E0E0),      // Light text
    surface = Color(0xFF1D1D1D),           // Dark surface
    onSurface = Color(0xFFE0E0E0),         // Light text
    surfaceVariant = Color(0xFF2D2D2D),    // Slightly lighter surface
    onSurfaceVariant = Color(0xFFD0D0D0),  // Slightly dimmer light text
    error = Color(0xFFEF9A9A),             // Light red
    onError = Color.Black
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40

    /* Other default colors to override
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    */
)

/*
@Composable
fun UndistractTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
 */

@Composable
fun UndistractTheme(
    darkTheme: Boolean = true, // Force dark theme by default
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography, // Your existing Typography
//        shapes = Shapes, // Your existing Shapes
        content = content
    )
}