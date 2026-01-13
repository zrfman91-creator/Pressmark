/* =====================================================================================
 * FILE: app/src/main/java/com/zak/pressmark/ui/theme/Color.kt
 * ===================================================================================== */
package com.zak.pressmark.core.theme

import androidx.compose.ui.graphics.Color

/**
 * Pressmark brand palette.
 * Keep these as raw tokens; map into Material ColorScheme in Theme.kt.
 */
object PressmarkColors {
    // Brand
    val Ink = Color(0xFF0F172A)          // deep slate
    val Ink80 = Color(0xCC0F172A)          // deep slate
    val Paper = Color(0xFFFFFFFF)        // near-white
    val Accent = Color(0xFF2563EB)       // blue
    val Accent2 = Color(0xFFFFFFFF)      // purple

    // Neutrals
    val Slate900 = Color(0xFF0B1220)
    val Slate800 = Color(0xFF111A2E)
    val Slate700 = Color(0xFF1B2A4A)
    val Slate600 = Color(0xFF334155)
    val Slate500 = Color(0xFF475569)
    val Slate400 = Color(0xFF657388)
    val Slate300 = Color(0xFF9CA3AF)
    val Slate200 = Color(0xFFE2E8F0)
    val Slate100 = Color(0xFFF1F5F9)

    // Status
    val Success = Color(0xFF16A34A)
    val Warning = Color(0xFFF59E0B)
    val Error = Color(0xFFDC2626)
}