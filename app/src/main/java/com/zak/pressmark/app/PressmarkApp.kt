package com.zak.pressmark.app

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.compose.rememberNavController
import com.zak.pressmark.app.di.AppGraph
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PressmarkApp(
    graph: AppGraph,
) {
    // One-time normalization: ensure provider fields are populated for legacy Discogs covers.
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            graph.albumRepository.backfillArtworkProviderFromLegacyDiscogs()
        }
    }

    val navController = rememberNavController()
    PressmarkNavHost(
        navController = navController,
        graph = graph,
    )
}
