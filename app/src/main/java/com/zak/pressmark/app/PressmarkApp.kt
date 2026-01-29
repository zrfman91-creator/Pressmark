package com.zak.pressmark.app

import androidx.compose.runtime.Composable
import androidx.navigation.compose.rememberNavController

@Composable
fun PressmarkApp() {
    val navController = rememberNavController()

    PressmarkNavSuiteScaffold(
        navController = navController,
    ) {
        PressmarkNavHost(navController = navController)
    }
}
