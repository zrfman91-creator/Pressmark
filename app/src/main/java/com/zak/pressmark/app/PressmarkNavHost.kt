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
import com.zak.pressmark.feature.albumdetails.route.AlbumDetailsRoute
import com.zak.pressmark.feature.albumdetails.vm.AlbumDetailsViewModel
import com.zak.pressmark.feature.albumdetails.vm.AlbumDetailsViewModelFactory
import com.zak.pressmark.feature.albumlist.route.AlbumListRoute
import com.zak.pressmark.feature.albumlist.vm.AlbumListViewModel
import com.zak.pressmark.feature.albumlist.vm.AlbumListViewModelFactory
import com.zak.pressmark.feature.artist.route.ArtistRoute
import com.zak.pressmark.feature.artist.vm.ArtistViewModel
import com.zak.pressmark.feature.artist.vm.ArtistViewModelFactory
import com.zak.pressmark.feature.artworkpicker.route.CoverSearchRoute
import com.zak.pressmark.feature.covercapture.screen.CameraCoverCaptureRoute
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch

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
            // Album list VM
            val listFactory = remember(graph) {
                AlbumListViewModelFactory(
                    albumRepo = graph.albumRepository,
                    artistRepo = graph.artistRepository,
                )
            }
            val vm: AlbumListViewModel = viewModel(factory = listFactory)
            val savedStateHandle = navController.currentBackStackEntry?.savedStateHandle
            val savedAlbumIdFlow = remember(savedStateHandle) {
                savedStateHandle?.savedAlbumIdFlow()
            }
            val savedAlbumId = savedAlbumIdFlow
                ?.collectAsStateWithLifecycle(initialValue = null)
                ?.value

            AlbumListRoute(
                vm = vm,
                graph = graph,
                onAddAlbum = { navController.navigate(PressmarkRoutes.ADD) },
                onOpenAlbum = { albumId ->
                    navController.navigate(PressmarkRoutes.details(albumId))
                },
                onOpenCoverSearch = { albumId, artist, title ->
                    navController.navigate(PressmarkRoutes.coverSearch(albumId, artist, title))
                },
                savedAlbumId = savedAlbumId,
                onAlbumSavedConsumed = { savedStateHandle?.clearSavedAlbumId() },
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
                    albumRepository = graph.albumRepository,
                    artistRepository = graph.artistRepository,
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
                        com.zak.pressmark.feature.addalbum.vm.SaveIntent.SaveAndExit -> PressmarkRoutes.COVER_ORIGIN_LIST_SUCCESS
                        com.zak.pressmark.feature.addalbum.vm.SaveIntent.AddAnother -> PressmarkRoutes.COVER_ORIGIN_ADD_ANOTHER
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
                        // Drop Cover Search (+ Add) from back stack and land on Details.
                        navController.navigate(PressmarkRoutes.details(albumId)) {
                            popUpTo(PressmarkRoutes.LIST) { inclusive = false }
                        }
                    }

                    PressmarkRoutes.COVER_ORIGIN_LIST_SUCCESS -> {
                        // Return to List and show success dialog.
                        runCatching {
                            navController.getBackStackEntry(PressmarkRoutes.LIST)
                                .savedStateHandle
                                .setSavedAlbumId(albumId)
                        }
                        navController.popBackStack(PressmarkRoutes.LIST, false)
                    }

                    PressmarkRoutes.COVER_ORIGIN_ADD_ANOTHER -> {
                        // Return to Add Album and clear form exactly once.
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

            val scope = androidx.compose.runtime.rememberCoroutineScope()

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

            CameraCoverCaptureRoute(
                onBack = { navController.popBackStack() },
                onCaptured = { uri ->
                    scope.launch {
                        graph.albumRepository.setLocalCover(albumId, uri.toString())
                        closeAfterCapture()
                    }
                }
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
