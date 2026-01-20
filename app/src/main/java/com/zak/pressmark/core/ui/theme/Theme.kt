// FILE: app/src/main/java/com/zak/pressmark/core/ui/theme/Theme.kt
package com.zak.pressmark.core.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val CorporateLightColorScheme: ColorScheme = lightColorScheme(
    // Primary (Office Blue)
    primary =              PressmarkColors.BlueDeep,
    onPrimary =            Color(0xFFFFFFFF),
    primaryContainer =     PressmarkColors.BlueSoft,
    onPrimaryContainer =   Color(0xFF0B1E55),

    // Secondary (Neutral slate)
    secondary =            PressmarkColors.NeutralAccent,
    onSecondary =          Color(0xFFFFFFFF),
    secondaryContainer =   Color(0xFFE2E8F0), // slate-200
    onSecondaryContainer = Color(0xFF111827),

    // Tertiary (muted)
    tertiary =             Color(0xFF0EA5E9), // sky-500 (subtle accent)
    onTertiary =           Color(0xFFFFFFFF),
    tertiaryContainer =    Color(0xFFCCEAF9),
    onTertiaryContainer =  Color(0xFF0B2A3A),

    // Surfaces
    background =           PressmarkColors.Paper,
    onBackground =         PressmarkColors.Ink,
    surface =              PressmarkColors.Surface,
    onSurface =            PressmarkColors.Ink,
    surfaceVariant =       PressmarkColors.Surface2,
    onSurfaceVariant =     Color(0xFF4B5563),

    // Borders / Dividers
    outline =              PressmarkColors.Hairline,
    outlineVariant =       PressmarkColors.HairlineSoft,

    // Error
    error =                PressmarkColors.Error,
    onError =              Color(0xFFFFFFFF),
    errorContainer =       PressmarkColors.ErrorContainer,
    onErrorContainer =     PressmarkColors.OnErrorContainer,
)

private val CorporateDarkColorScheme: ColorScheme = darkColorScheme(
    background =           Color(0xFF0B1220),
    onBackground =         Color(0xFFE5E7EB),
    surface =              Color(0xFF0F172A),
    onSurface =            Color(0xFFE5E7EB),
    surfaceVariant =       Color(0xFF111827),
    onSurfaceVariant =     Color(0xFFB6C0CE),

    primary =              Color(0xFF93C5FD), // blue-300
    onPrimary =            Color(0xFF0B1E55),
    primaryContainer =     Color(0xFF1E3A8A), // indigo-ish
    onPrimaryContainer =   Color(0xFFDCEAFE),

    secondary =            Color(0xFF94A3B8), // slate-400
    onSecondary =          Color(0xFF0F172A),
    secondaryContainer =   Color(0xFF1F2937),
    onSecondaryContainer = Color(0xFFE5E7EB),

    tertiary =             Color(0xFF7DD3FC), // sky-300
    onTertiary =           Color(0xFF0B2A3A),
    tertiaryContainer =    Color(0xFF0B2A3A),
    onTertiaryContainer =  Color(0xFFCCEAF9),

    outline =              Color(0xFF334155),
    outlineVariant =       Color(0xFF1F2937),

    error =                Color(0xFFFFB4AB),
    onError =              Color(0xFF690005),
    errorContainer =       Color(0xFF7F1D1D),
    onErrorContainer =     Color(0xFFFEE2E2),
)

@Composable
fun PressmarkTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true, // corporate apps often use system/dynamic colors
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)

        darkTheme -> CorporateDarkColorScheme
        else -> CorporateLightColorScheme
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
        typography = AppTypography,
        content = content
    )
}
