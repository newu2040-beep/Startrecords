package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

enum class AppTheme(val displayName: String) {
    MIDNIGHT_BLUE("Midnight Blue"),
    EMERALD_GREEN("Emerald Green"),
    ARCTIC_WHITE("Arctic White"),
    LAVENDER_PURPLE("Lavender Purple"),
    OCEAN_CYAN("Ocean Cyan"),
    SUNSET_ORANGE("Sunset Orange")
}

private val MidnightBlueColorScheme = darkColorScheme(
    primary = Color(0xFF3B82F6),       // Professional Polish Blue
    secondary = Color(0xFF1D4ED8),     // Indigo / Dark Blue
    tertiary = Color(0xFF6366F1),      // Indigo Accent
    background = Color(0xFF050A18),    // Dark Space (#050A18)
    surface = Color(0xFF0A1225),       // Frosted card slate (#0A1225)
    onPrimary = Color(0xFFFFFFFF),
    onSecondary = Color(0xFFFFFFFF),
    onTertiary = Color(0xFFFFFFFF),
    onBackground = Color(0xFFE2E8F0),  // Slate-200
    onSurface = Color(0xFFF1F5F9)      // Slate-100
)

private val EmeraldGreenColorScheme = darkColorScheme(
    primary = Color(0xFF34D399),       // Neon Emerald
    secondary = Color(0xFF059669),     // Deep Emerald
    tertiary = Color(0xFF6EE7B7),      // Mint
    background = Color(0xFF021B14),    // Dark Forest Depth
    surface = Color(0xFF062E25),       // Forest Frosted Card
    onPrimary = Color(0xFF021B14),
    onSecondary = Color(0xFF021B14),
    onTertiary = Color(0xFF021B14),
    onBackground = Color(0xFFECFDF5),
    onSurface = Color(0xFFD1FAE5)
)

private val ArcticWhiteColorScheme = lightColorScheme(
    primary = Color(0xFF2563EB),       // Deep Arctic Blue
    secondary = Color(0xFF0284C7),     // Ocean Sky
    tertiary = Color(0xFF4F46E5),      // Violet Accent
    background = Color(0xFFF9FAFB),    // Pure Glacier White
    surface = Color(0xFFFFFFFF),       // Clear Ice Card
    onPrimary = Color(0xFFFFFFFF),
    onSecondary = Color(0xFFFFFFFF),
    onTertiary = Color(0xFFFFFFFF),
    onBackground = Color(0xFF111827),
    onSurface = Color(0xFF1F2937)
)

private val LavenderPurpleColorScheme = darkColorScheme(
    primary = Color(0xFFA78BFA),       // Radiant Purple
    secondary = Color(0xFFC084FC),     // Heliotrope
    tertiary = Color(0xFFF472B6),      // Soft Rose Accent
    background = Color(0xFF0E0B1E),    // Velvet Void
    surface = Color(0xFF18122B),       // Lavender Slate Card
    onPrimary = Color(0xFF0E0B1E),
    onSecondary = Color(0xFF0E0B1E),
    onTertiary = Color(0xFF0E0B1E),
    onBackground = Color(0xFFF5F3FF),
    onSurface = Color(0xFFEDE9FE)
)

private val OceanCyanColorScheme = darkColorScheme(
    primary = Color(0xFF22D3EE),       // Cyber Cyan
    secondary = Color(0xFF0EA5E9),     // Marine Sky
    tertiary = Color(0xFF2DD4BF),      // Aurora Turquoise
    background = Color(0xFF081821),    // Mariana Abyss
    surface = Color(0xFF0C2433),       // Deep Sea Glass Card
    onPrimary = Color(0xFF081821),
    onSecondary = Color(0xFF081821),
    onTertiary = Color(0xFF081821),
    onBackground = Color(0xFFECFEFF),
    onSurface = Color(0xFFCFFAFE)
)

private val SunsetOrangeColorScheme = darkColorScheme(
    primary = Color(0xFFFB923C),       // Sunset Ember
    secondary = Color(0xFFF97316),     // Neon Tangerine
    tertiary = Color(0xFFF43F5E),      // Coral Pink Accent
    background = Color(0xFF1B0F11),    // Volcanic Ash
    surface = Color(0xFF2D161A),       // Sunset Frosted Card
    onPrimary = Color(0xFF1B0F11),
    onSecondary = Color(0xFF1B0F11),
    onTertiary = Color(0xFF1B0F11),
    onBackground = Color(0xFFFFF7ED),
    onSurface = Color(0xFFFED7AA)
)

@Composable
fun StartRecordTheme(
    selectedTheme: AppTheme = AppTheme.MIDNIGHT_BLUE,
    content: @Composable () -> Unit
) {
    val colorScheme = when (selectedTheme) {
        AppTheme.MIDNIGHT_BLUE -> MidnightBlueColorScheme
        AppTheme.EMERALD_GREEN -> EmeraldGreenColorScheme
        AppTheme.ARCTIC_WHITE -> ArcticWhiteColorScheme
        AppTheme.LAVENDER_PURPLE -> LavenderPurpleColorScheme
        AppTheme.OCEAN_CYAN -> OceanCyanColorScheme
        AppTheme.SUNSET_ORANGE -> SunsetOrangeColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
