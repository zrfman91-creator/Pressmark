// FILE: app/src/main/java/com/zak/pressmark/app/PressmarkApp.kt
package com.zak.pressmark.app

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.navigation.compose.rememberNavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PressmarkApp() {
    val navController = rememberNavController()
    PressmarkNavHost(navController = navController)
}
