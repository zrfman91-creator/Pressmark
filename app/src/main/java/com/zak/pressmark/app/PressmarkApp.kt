package com.zak.pressmark.app

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.navigation.compose.rememberNavController
import com.zak.pressmark.app.di.AppGraph

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PressmarkApp(
    graph: AppGraph,
) {
    val navController = rememberNavController()
    PressmarkNavHost(
        navController = navController,
        graph = graph,
    )
}
