// FILE: app/src/main/java/com/zak/pressmark/app/PressmarkNavHost.kt
package com.zak.pressmark.app

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.zak.pressmark.app.di.AppGraph
import com.zak.pressmark.feature.addalbum.route.AddAlbumRoute
import com.zak.pressmark.feature.addalbum.vm.AddAlbumViewModel
import com.zak.pressmark.feature.addalbum.vm.AddAlbumViewModelFactory
import com.zak.pressmark.feature.addalbum.vm.SaveIntent.*
import com.zak.pressmark.feature.albumdetails.route.AlbumDetailsRoute
import com.zak.pressmark.feature.albumdetails.vm.AlbumDetailsViewModel
import com.zak.pressmark.feature.albumdetails.vm.AlbumDetailsViewModelFactory
import com.zak.pressmark.feature.catalog.route.AlbumListRoute
import com.zak.pressmark.feature.catalog.vm.AlbumListViewModel
import com.zak.pressmark.feature.catalog.vm.CatalogViewModelFactory
import com.zak.pressmark.feature.artist.route.ArtistRoute
import com.zak.pressmark.feature.artist.vm.ArtistViewModel
import com.zak.pressmark.feature.artist.vm.ArtistViewModelFactory
import com.zak.pressmark.feature.artworkpicker.route.CoverSearchRoute
import com.zak.pressmark.feature.capturecover.route.CaptureCoverFlowRoute

@OptIn(ExperimentalMaterial3Api::class)
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
            val listFactory = remember(graph) {
                CatalogViewModelFactory(
                    releaseRepo = graph.releaseRepository,
                )
            }
            val vm: AlbumListViewModel = viewModel(factory = listFactory)

            // ✅ No manual refresh needed: list is driven by Room Flow.
            AlbumListRoute(
                vm = vm,
                onAddAlbum = { navController.navigate(PressmarkRoutes.ADD) },
                onOpenRelease = { releaseId ->
                    navController.navigate(PressmarkRoutes.details(releaseId))
                },
            )
        }

        composable(PressmarkRoutes.ADD) {
            val addSavedStateHandle = navController.currentBackStackEntry?.savedStateHandle
            val clearAddFormFlow = remember(addSavedStateHandle) {
                addSavedStateHandle?.clearAddAlbumFormFlow()
            }
            val clearAddForm = clearAddFormFlow
                ?.collectAsStateWithLifecycle(initialValue = false)
                ?.value ?: false

            val factory = remember(graph) {
                AddAlbumViewModelFactory(
                    artistRepository = graph.artistRepository,
                    releaseRepository = graph.releaseRepository,
                )
            }
            val vm: AddAlbumViewModel = viewModel(factory = factory)

            AddAlbumRoute(
                vm = vm,
                onNavigateUp = { navController.popBackStack() },
                clearFormRequested = clearAddForm,
                onClearFormConsumed = { addSavedStateHandle?.consumeClearAddAlbumForm() },
                onAlbumSaved = { albumId, artist, title, intent ->
                    val origin = when (intent) {
                        SaveAndExit -> PressmarkRoutes.COVER_ORIGIN_LIST_SUCCESS
                        AddAnother -> PressmarkRoutes.COVER_ORIGIN_ADD_ANOTHER
                    }
                    navController.navigate(
                        PressmarkRoutes.coverSearch(
                            albumId = albumId,
                            artist = artist,
                            title = title,
                            origin = origin,
                        )
                    )
                }
            )
        }

        composable(
            route = PressmarkRoutes.DETAILS_PATTERN,
            arguments = listOf(navArgument(PressmarkRoutes.ARG_ALBUM_ID) {
                type = NavType.StringType
            }),
        ) { backStackEntry ->
            val albumId =
                backStackEntry.arguments?.getString(PressmarkRoutes.ARG_ALBUM_ID).orEmpty()

            val factory =
                remember(graph, albumId) { AlbumDetailsViewModelFactory(graph, albumId) }
            val vm: AlbumDetailsViewModel = viewModel(
                key = "album_details_$albumId",
                factory = factory,
            )

            AlbumDetailsRoute(
                vm = vm,
                onBack = { navController.popBackStack() },
                onOpenArtist = { artistId ->
                    navController.navigate(PressmarkRoutes.artist(artistId))
                },
            )
        }

        composable(
            route = PressmarkRoutes.COVER_SEARCH_PATTERN,
            arguments = listOf(
                navArgument(PressmarkRoutes.ARG_ALBUM_ID) { type = NavType.StringType },
                navArgument(PressmarkRoutes.ARG_COVER_ARTIST) {
                    type = NavType.StringType
                    defaultValue = ""
                },
                navArgument(PressmarkRoutes.ARG_COVER_TITLE) {
                    type = NavType.StringType
                    defaultValue = ""
                },
                navArgument(PressmarkRoutes.ARG_COVER_ORIGIN) {
                    type = NavType.StringType
                    defaultValue = PressmarkRoutes.COVER_ORIGIN_BACK
                },
            ),
        ) { backStackEntry ->
            val albumId =
                backStackEntry.arguments?.getString(PressmarkRoutes.ARG_ALBUM_ID).orEmpty()
            val artist =
                backStackEntry.arguments?.getString(PressmarkRoutes.ARG_COVER_ARTIST).orEmpty()
            val title =
                backStackEntry.arguments?.getString(PressmarkRoutes.ARG_COVER_TITLE).orEmpty()
            val origin =
                backStackEntry.arguments?.getString(PressmarkRoutes.ARG_COVER_ORIGIN)
                    ?: PressmarkRoutes.COVER_ORIGIN_BACK

            fun closeCoverFlow() {
                when (origin) {
                    PressmarkRoutes.COVER_ORIGIN_DETAILS -> {
                        navController.navigate(PressmarkRoutes.details(albumId)) {
                            popUpTo(PressmarkRoutes.LIST) { inclusive = false }
                        }
                    }

                    PressmarkRoutes.COVER_ORIGIN_LIST_SUCCESS -> {
                        runCatching {
                            navController.getBackStackEntry(PressmarkRoutes.LIST)
                                .savedStateHandle
                                .setSavedAlbumId(albumId)
                        }
                        navController.popBackStack(PressmarkRoutes.LIST, false)
                    }

                    PressmarkRoutes.COVER_ORIGIN_ADD_ANOTHER -> {
                        navController.previousBackStackEntry
                            ?.savedStateHandle
                            ?.requestClearAddAlbumForm()
                        navController.popBackStack()
                    }

                    else -> navController.popBackStack()
                }
            }

            CoverSearchRoute(
                graph = graph,
                albumId = albumId,
                artist = artist,
                title = title,
                shouldPromptAutofill = (origin == PressmarkRoutes.COVER_ORIGIN_LIST_SUCCESS),
                onTakePhoto = {
                    navController.navigate(
                        PressmarkRoutes.coverCapture(
                            albumId = albumId,
                            origin = origin,
                        )
                    )
                },
                onClose = ::closeCoverFlow,
            )
        }

        composable(
            route = PressmarkRoutes.COVER_CAPTURE_PATTERN,
            arguments = listOf(
                navArgument(PressmarkRoutes.ARG_ALBUM_ID) { type = NavType.StringType },
                navArgument(PressmarkRoutes.ARG_COVER_ORIGIN) {
                    type = NavType.StringType
                    defaultValue = PressmarkRoutes.COVER_ORIGIN_BACK
                },
            ),
        ) { backStackEntry ->
            val albumId = backStackEntry.arguments?.getString(PressmarkRoutes.ARG_ALBUM_ID).orEmpty()
            val origin = backStackEntry.arguments?.getString(PressmarkRoutes.ARG_COVER_ORIGIN)
                ?: PressmarkRoutes.COVER_ORIGIN_BACK

            fun closeAfterCapture() {
                when (origin) {
                    PressmarkRoutes.COVER_ORIGIN_DETAILS -> {
                        navController.navigate(PressmarkRoutes.details(albumId)) {
                            popUpTo(PressmarkRoutes.LIST) { inclusive = false }
                        }
                    }

                    PressmarkRoutes.COVER_ORIGIN_LIST_SUCCESS -> {
                        runCatching {
                            navController.getBackStackEntry(PressmarkRoutes.LIST)
                                .savedStateHandle
                                .setSavedAlbumId(albumId)
                        }
                        navController.popBackStack(PressmarkRoutes.LIST, false)
                    }

                    PressmarkRoutes.COVER_ORIGIN_ADD_ANOTHER -> {
                        runCatching {
                            navController.getBackStackEntry(PressmarkRoutes.ADD)
                                .savedStateHandle
                                .requestClearAddAlbumForm()
                        }
                        navController.popBackStack(PressmarkRoutes.ADD, false)
                    }

                    else -> {
                        navController.popBackStack()
                        navController.popBackStack()
                    }
                }
            }

            CaptureCoverFlowRoute(
                albumId = albumId,
                albumRepository = graph.albumRepository,
                onBack = { navController.popBackStack() },
                onDone = { closeAfterCapture() },
            )
        }

        composable(
            route = PressmarkRoutes.ARTIST_PATTERN,
            arguments = listOf(navArgument(PressmarkRoutes.ARG_ARTIST_ID) {
                type = NavType.LongType
            }),
        ) { backStackEntry ->
            val artistId =
                backStackEntry.arguments?.getLong(PressmarkRoutes.ARG_ARTIST_ID) ?: 0L
            val factory = remember(graph, artistId) { ArtistViewModelFactory(graph, artistId) }
            val vm: ArtistViewModel = viewModel(
                key = "artist_$artistId",
                factory = factory,
            )

            ArtistRoute(
                vm = vm,
                onBack = { navController.popBackStack() },
            )
        }
    }
}
