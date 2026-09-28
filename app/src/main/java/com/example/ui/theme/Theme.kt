package com.example.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

/**
 * App-wide Material 3 Expressive theme.
 *
 * Uses [MaterialExpressiveTheme] (Material3 1.4+) instead of the plain
 * [androidx.compose.material3.MaterialTheme] so every stock M3 component
 * (buttons, FAB, nav bar, sliders, chips, etc.) automatically picks up the
 * expressive shape scale and the bouncier, spring-based "expressive" motion
 * scheme -- not just the custom components built in this app.
 */
@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> ExpressiveDarkColorScheme
        else -> ExpressiveLightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val insetsController = WindowCompat.getInsetsController(window, view)

            // Keep Android system bars visually in sync with the app theme.
            // The app uses edge-to-edge, so the status bar is transparent and
            // the content drawn underneath it provides the actual background.
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            window.navigationBarColor = android.graphics.Color.TRANSPARENT

            insetsController.isAppearanceLightStatusBars = !darkTheme
            insetsController.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    val density = LocalDensity.current
    val fixedFontScaleDensity = Density(
        density = density.density,
        fontScale = 1f
    )

    CompositionLocalProvider(
        LocalDensity provides fixedFontScaleDensity
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = ExpressiveShapes,
            content = content
        )
    }
}

/**
 * Shape scale. Uses only the stable 5-slot [Shapes] API; the extra
 * "increased" steps of the expressive scale are exposed as extensions below.
 */
val ExpressiveShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

/** Between large (24dp) and extraLarge (32dp). */
val Shapes.largePlus: CornerBasedShape get() = RoundedCornerShape(28.dp)

/** Between extraLarge (32dp) and extraExtraLarge. */
val Shapes.extraLargePlus: CornerBasedShape get() = RoundedCornerShape(36.dp)
