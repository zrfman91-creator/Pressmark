//FILE: app/src/main/java/com/zak/pressmark/core/ui/theme/Type.kt
package com.zak.pressmark.core.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.zak.pressmark.R
import androidx.compose.material3.Typography as M3Typography

val manrope = FontFamily(
    Font(R.font.manrope)
    )
val marcellusSc = FontFamily(
    Font(R.font.marcellus_sc),
)
/*val overpassMono = FontFamily(
    Font(R.font.overpass_mono),
)*/
val AppTypography = M3Typography(
    // Display / headings
    displayLarge = TextStyle(               // Page headers
        fontFamily = marcellusSc,
        fontWeight = FontWeight.Normal,
        fontSize = 32.sp,
    ),
    // Titles / headings
    titleLarge = TextStyle(
        fontFamily = manrope,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 26.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = manrope,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 22.sp,
    ),
    titleSmall = TextStyle(                 //Landing screen button text
        fontFamily = manrope,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 20.sp,
    ),
    // Body / metadata
    bodyLarge = TextStyle(
        fontFamily = manrope,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 22.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = manrope,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    bodySmall = TextStyle(                  // "Find a Pressing" button text
        fontFamily = manrope,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
)