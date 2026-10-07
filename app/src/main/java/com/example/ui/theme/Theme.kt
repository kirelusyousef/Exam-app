package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

fun parseHexColor(hexString: String?, defaultColor: Color): Color {
    if (hexString.isNullOrBlank()) return defaultColor
    return try {
        val cleanHex = hexString.removePrefix("#")
        val colorInt = cleanHex.toLong(16)
        if (cleanHex.length == 6) {
            Color(0xFF000000 or colorInt)
        } else if (cleanHex.length == 8) {
            Color(colorInt)
        } else {
            defaultColor
        }
    } catch (_: Exception) {
        defaultColor
    }
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    primaryHexColor: String? = null,
    dynamicColor: Boolean = false, // prefer app brand color
    content: @Composable () -> Unit,
) {
    val primaryColor = parseHexColor(primaryHexColor, FormPurplePrimary)

    val lightScheme = lightColorScheme(
        primary = primaryColor,
        onPrimary = Color.White,
        primaryContainer = primaryColor.copy(alpha = 0.12f),
        onPrimaryContainer = primaryColor,
        secondary = FormPurpleSecondary,
        onSecondary = Color.White,
        tertiary = FormPurpleTertiary,
        background = Color(0xFFF6F8FC),
        surface = Color(0xFFFFFFFF),
        onBackground = TextPrimary,
        onSurface = TextPrimary,
        surfaceVariant = Color(0xFFF1F3F4),
        onSurfaceVariant = TextSecondary,
        outline = Color(0xFFDADCE0)
    )

    val darkScheme = darkColorScheme(
        primary = primaryColor,
        onPrimary = Color.White,
        primaryContainer = primaryColor.copy(alpha = 0.25f),
        onPrimaryContainer = Color.White,
        secondary = FormPurpleTertiary,
        onSecondary = Color.White,
        background = Color(0xFF131314),
        surface = Color(0xFF1E1F20),
        onBackground = Color(0xFFE3E3E3),
        onSurface = Color(0xFFE3E3E3),
        surfaceVariant = Color(0xFF282A2C),
        onSurfaceVariant = Color(0xFFC4C7C5),
        outline = Color(0xFF444746)
    )

    val colorScheme = if (darkTheme) darkScheme else lightScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
