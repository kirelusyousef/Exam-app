package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

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

/**
 * Modern, bright and clean Google Forms light theme as requested.
 * Defaults to light mode for an inviting, clear examination experience.
 */
@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false, // Forced light theme per user request
    primaryHexColor: String? = null,
    dynamicColor: Boolean = false,
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
        secondaryContainer = Color(0xFFEDE7F6),
        onSecondaryContainer = Color(0xFF311B92),
        tertiary = FormPurpleTertiary,
        onTertiary = Color.White,
        background = Color(0xFFF8F9FD), // Crisp light background
        surface = Color(0xFFFFFFFF),    // Clean pure white surface
        onBackground = Color(0xFF1F1F1F),
        onSurface = Color(0xFF1F1F1F),
        surfaceVariant = Color(0xFFF3F4F9),
        onSurfaceVariant = Color(0xFF5F6368),
        outline = Color(0xFFE2E4E9),
        outlineVariant = Color(0xFFECEEF2)
    )

    MaterialTheme(
        colorScheme = lightScheme,
        typography = Typography,
        content = content
    )
}
