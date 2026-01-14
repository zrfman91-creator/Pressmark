/* =====================================================================================
 * FILE: app/src/main/java/com/zak/pressmark/ui/theme/Theme.kt
 * ===================================================================================== */
package com.zak.pressmark.core.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme: ColorScheme = lightColorScheme(
    primary =              PressmarkColors.Accent,
    onPrimary =            PressmarkColors.Paper,
    primaryContainer =     PressmarkColors.Slate200,
    onPrimaryContainer =   PressmarkColors.Ink,

    secondary =            PressmarkColors.Accent2,
    secondaryContainer =   PressmarkColors.Slate100,
    onSecondary =          PressmarkColors.Ink80,
    onSecondaryContainer = PressmarkColors.Ink,

    background =           PressmarkColors.Slate300,
    onBackground =         PressmarkColors.Ink,

    surface =              PressmarkColors.Slate100,
    surfaceVariant =       PressmarkColors.Slate300,
    onSurface =            PressmarkColors.Ink,
    onSurfaceVariant =     PressmarkColors.Ink80,

    outline =              PressmarkColors.Slate300,
    outlineVariant =       PressmarkColors.Slate100,

    error =                PressmarkColors.Error,
    onError =              PressmarkColors.Paper,
    errorContainer =       PressmarkColors.Error,
    onErrorContainer =     PressmarkColors.Paper,
)

private val DarkColorScheme: ColorScheme = darkColorScheme(
    primary = Color(0xFF60A5FA), // lighter blue for dark mode contrast
    onPrimary =            PressmarkColors.Slate900,
    primaryContainer =     PressmarkColors.Slate700,
    onPrimaryContainer =   PressmarkColors.Slate100,

    secondary = Color(0xFFC084FC),
    onSecondary =          PressmarkColors.Slate900,
    secondaryContainer =   PressmarkColors.Slate700,
    onSecondaryContainer = PressmarkColors.Slate100,

    background =           PressmarkColors.Slate900,
    onBackground =         PressmarkColors.Slate100,

    surface =              PressmarkColors.Slate900,
    onSurface =            PressmarkColors.Slate100,
    surfaceVariant =       PressmarkColors.Slate800,
    onSurfaceVariant =     PressmarkColors.Slate200,

    outline =              PressmarkColors.Slate600,
    outlineVariant =       PressmarkColors.Slate700,

    error = Color(0xFFF87171),
    onError =          PressmarkColors.Slate900,
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = Color(0xFFFEE2E2),
)

@Composable
fun PressmarkTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (context as Activity).window
            @Suppress("DEPRECATION")
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography, // keep your existing typography object
        content = content
    )
}
