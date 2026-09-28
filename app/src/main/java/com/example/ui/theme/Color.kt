package com.example.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// ==========================================
// Material 3 Expressive - Fallback Palettes
// Matches the reference screenshots
// ==========================================

// --- Light Theme (Pastel Lavender & Rich Violet - Screenshot 1) ---
val LightBg = Color(0xFFF9F7FD)
val LightSurface = Color(0xFFF9F7FD)
val LightSurfaceContainer = Color(0xFFF1EEF7)
val LightSurfaceContainerLow = Color(0xFFF7F5FC)
val LightSurfaceContainerHigh = Color(0xFFE9E4F2)
val LightSurfaceContainerHighest = Color(0xFFE2DCEB)
val LightPrimary = Color(0xFF5D54A4) // Rich expressive purple
val LightOnPrimary = Color(0xFFFFFFFF)
val LightPrimaryContainer = Color(0xFF5D54A4)
val LightOnPrimaryContainer = Color(0xFFFFFFFF)
val LightSecondary = Color(0xFF7065AC)
val LightOnSecondary = Color(0xFFFFFFFF)
val LightSecondaryContainer = Color(0xFFE9E3F8)
val LightOnSecondaryContainer = Color(0xFF261958)
val LightTertiary = Color(0xFFA23E72) // Warm magenta accent, third expressive hue
val LightOnTertiary = Color(0xFFFFFFFF)
val LightTertiaryContainer = Color(0xFFFFD9E9)
val LightOnTertiaryContainer = Color(0xFF3E0026)
val LightTextPrimary = Color(0xFF1E1A2B)
val LightTextSecondary = Color(0xFF6B657D)
val LightOutline = Color(0xFFD4CEE0)

// --- Dark Theme (Deep Navy & Electric Sky Blue - Screenshot 2) ---
val DarkBg = Color(0xFF0C1B2B) // Deep slate-navy
val DarkSurface = Color(0xFF0C1B2B)
val DarkSurfaceContainer = Color(0xFF13253A) // Elevated card container
val DarkSurfaceContainerLow = Color(0xFF0E1E2F)
val DarkSurfaceContainerHigh = Color(0xFF1A3049)
val DarkSurfaceContainerHighest = Color(0xFF213B59)
val DarkPrimary = Color(0xFF72C7FF) // Electric sky blue
val DarkOnPrimary = Color(0xFF00344F) // Deep contrast text on primary
val DarkPrimaryContainer = Color(0xFF72C7FF)
val DarkOnPrimaryContainer = Color(0xFF00344F)
val DarkSecondary = Color(0xFF5CB8F6)
val DarkOnSecondary = Color(0xFF00344F)
val DarkSecondaryContainer = Color(0xFF193B5C)
val DarkOnSecondaryContainer = Color(0xFFCFE5FF)
val DarkTertiary = Color(0xFFFFB0D8) // Warm pink accent, third expressive hue
val DarkOnTertiary = Color(0xFF5C0A3B)
val DarkTertiaryContainer = Color(0xFF7A1052)
val DarkOnTertiaryContainer = Color(0xFFFFD9E9)
val DarkTextPrimary = Color(0xFFEDF2F8)
val DarkTextSecondary = Color(0xFF90A3B8)
val DarkOutline = Color(0xFF25415E)

// Shared Semantic Colors
val StatusOngoingGreen = Color(0xFF00E676)
val StatusUpcomingAmber = Color(0xFFFFB300)
val StatusCancelledRed = Color(0xFFFF5252)
val StatusChangedYellow = Color(0xFFFFD54F)

val ExpressiveDarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = DarkSecondary,
    onSecondary = DarkOnSecondary,
    secondaryContainer = DarkSecondaryContainer,
    onSecondaryContainer = DarkOnSecondaryContainer,
    tertiary = DarkTertiary,
    onTertiary = DarkOnTertiary,
    tertiaryContainer = DarkTertiaryContainer,
    onTertiaryContainer = DarkOnTertiaryContainer,
    background = DarkBg,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceContainerHigh,
    onSurfaceVariant = DarkTextSecondary,
    surfaceContainer = DarkSurfaceContainer,
    surfaceContainerLow = DarkSurfaceContainerLow,
    surfaceContainerHigh = DarkSurfaceContainerHigh,
    surfaceContainerHighest = DarkSurfaceContainerHighest,
    outline = DarkOutline,
    outlineVariant = DarkOutline.copy(alpha = 0.5f),
    error = StatusCancelledRed,
    onError = Color.White,
    errorContainer = Color(0xFF4A1818),
    onErrorContainer = Color(0xFFFFB4AB)
)

val ExpressiveLightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = LightOnPrimaryContainer,
    secondary = LightSecondary,
    onSecondary = LightOnSecondary,
    secondaryContainer = LightSecondaryContainer,
    onSecondaryContainer = LightOnSecondaryContainer,
    tertiary = LightTertiary,
    onTertiary = LightOnTertiary,
    tertiaryContainer = LightTertiaryContainer,
    onTertiaryContainer = LightOnTertiaryContainer,
    background = LightBg,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceContainerHigh,
    onSurfaceVariant = LightTextSecondary,
    surfaceContainer = LightSurfaceContainer,
    surfaceContainerLow = LightSurfaceContainerLow,
    surfaceContainerHigh = LightSurfaceContainerHigh,
    surfaceContainerHighest = LightSurfaceContainerHighest,
    outline = LightOutline,
    outlineVariant = LightOutline.copy(alpha = 0.5f),
    error = StatusCancelledRed,
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002)
)
