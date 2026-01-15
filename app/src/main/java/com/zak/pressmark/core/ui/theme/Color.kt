/* =====================================================================================
 * FILE: app/src/main/java/com/zak/pressmark/core/ui/theme/Color.kt
 * ===================================================================================== */
package com.zak.pressmark.core.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Pressmark Corporate Standard palette.
 *
 * Intentionally "boring office software":
 * - neutral grays
 * - safe blue primary
 * - clean surfaces
 *
 * Token names remain compatible with existing UI usage.
 */
object PressmarkColors {

    // ===== Neutrals =====
    val Ink = Color(0xFF111827)        // slate-900-ish (primary text)
    val Ink80 = Color(0xCC111827)      // secondary text

    val Paper = Color(0xFFF9FAFB)      // gray-50 (app background)

    // Surface layers
    val Surface = Color(0xFFFFFFFF)    // white cards/sheets
    val Surface2 = Color(0xFFF3F4F6)   // gray-100/200-ish
    val Surface3 = Color(0xFFE5E7EB)   // gray-200/300-ish

    // Borders / hairlines
    val Hairline = Color(0xFFCBD5E1)   // slate-300
    val HairlineSoft = Color(0xFFE5E7EB)

    // ===== Corporate Accents =====
    val Blue = Color(0xFF2563EB)       // blue-600
    val BlueDeep = Color(0xFF1D4ED8)   // blue-700
    val BlueSoft = Color(0xFFDCEAFE)   // blue-100-ish

    val NeutralAccent = Color(0xFF64748B) // slate-500

    // Keep existing token names stable
    val Accent  = BlueDeep
    val Accent2 = NeutralAccent

    // Status
    val Error = Color(0xFFDC2626)          // red-600
    val ErrorContainer = Color(0xFFFEE2E2) // red-100
    val OnErrorContainer = Color(0xFF7F1D1D)

    val Success = Color(0xFF16A34A)        // green-600
    val Warning = Color(0xFFF59E0B)        // amber-500

    // ===== Compatibility neutrals (Slate*) =====
    val Slate100 = Surface
    val Slate200 = Surface2
    val Slate300 = Surface3
    val Slate400 = Hairline
    val Slate500 = Color(0xFF6B7280)       // gray-500
    val Slate600 = Color(0xFF4B5563)       // gray-600
    val Slate700 = Color(0xFF374151)       // gray-700
    val Slate800 = Color(0xFF1F2937)       // gray-800
    val Slate900 = Ink

    // Selection surfaces
    val selectedSurface = Color(0x1A2563EB)    // blue @ ~10%
    val unselectedSurface = Surface
}
