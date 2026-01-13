package com.zak.pressmark.app

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.zak.pressmark.app.di.AppGraph
import com.zak.pressmark.feature.albumdetails.screen.AlbumDetailsRoute
import com.zak.pressmark.feature.albumdetails.vm.AlbumDetailsViewModel
import com.zak.pressmark.feature.albumdetails.vm.AlbumDetailsViewModelFactory
import com.zak.pressmark.feature.albumlist.screen.AlbumListRoute
import com.zak.pressmark.feature.albumlist.vm.AlbumListViewModelFactory
import com.zak.pressmark.feature.artist.screen.ArtistRoute
import com.zak.pressmark.feature.artist.vm.ArtistViewModel
import com.zak.pressmark.feature.artist.vm.ArtistViewModelFactory
import com.zak.pressmark.feature.albumlist.vm.AlbumListViewModel
import com.zak.pressmark.feature.addalbum.route.AddAlbumRoute
import com.zak.pressmark.feature.addalbum.vm.AddAlbumViewModel
import com.zak.pressmark.feature.addalbum.vm.AddAlbumViewModelFactory
import kotlinx.coroutines.ExperimentalCoroutinesApi


@OptIn(ExperimentalMaterial3Api::class, ExperimentalCoroutinesApi::class)
@Composable
fun PressmarkNavHost(
    navController: NavHostController,
    graph: AppGraph,
) {
    NavHost(
        navController = navController,
        startDestination = PressmarkRoutes.LIST,
    ) {
        composable(PressmarkRoutes.LIST) {
            val factory = remember(graph) { AlbumListViewModelFactory(graph) }
            val vm: AlbumListViewModel = viewModel(factory = factory)

            AlbumListRoute(
                vm = vm,
                graph = graph,
                onAddAlbum = { navController.navigate(PressmarkRoutes.ADD) },
                onOpenAlbum = { albumId -> navController.navigate(PressmarkRoutes.details(albumId)) },)
        }
        composable(PressmarkRoutes.ADD) {
            val factory = remember(graph) {
                AddAlbumViewModelFactory(
                    albumRepository = graph.albumRepository,
                    artistRepository = graph.artistRepository,
                )
            }
            val vm: AddAlbumViewModel = viewModel(factory = factory)

            AddAlbumRoute(
                vm = vm,
                onNavigateUp = { navController.popBackStack() }
            )
        }

        composable(
            route = PressmarkRoutes.DETAILS_PATTERN,
            arguments = listOf(navArgument(PressmarkRoutes.ARG_ALBUM_ID) { type = NavType.StringType }),)
        {
            backStackEntry ->
            val albumId = backStackEntry.arguments?.getString(PressmarkRoutes.ARG_ALBUM_ID).orEmpty()
            val factory = remember(graph, albumId) { AlbumDetailsViewModelFactory(graph, albumId) }
            val vm: AlbumDetailsViewModel = viewModel(key = "album_details_$albumId", factory = factory,)

            AlbumDetailsRoute(
                vm = vm,
                onBack = { navController.popBackStack() },
                onOpenArtist = { artistId -> navController.navigate(PressmarkRoutes.artist(artistId))},)
        }

        composable(
            route = PressmarkRoutes.ARTIST_PATTERN,
            arguments = listOf(navArgument(PressmarkRoutes.ARG_ARTIST_ID) { type = NavType.LongType }),)
        {
            backStackEntry ->
            val artistId = backStackEntry.arguments?.getLong(PressmarkRoutes.ARG_ARTIST_ID) ?: 0L
            val factory = remember(graph, artistId) { ArtistViewModelFactory(graph, artistId) }
            val vm: ArtistViewModel = viewModel(key = "artist_$artistId", factory = factory,)

            ArtistRoute(vm = vm, onBack = { navController.popBackStack() },)
        }
    }
}
