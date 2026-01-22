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
import com.zak.pressmark.feature.catalog.route.AlbumListRoute
import com.zak.pressmark.feature.catalog.vm.AlbumListViewModel
import com.zak.pressmark.feature.catalog.vm.CatalogViewModelFactory
import com.zak.pressmark.feature.artist.route.ArtistRoute
import com.zak.pressmark.feature.artist.vm.ArtistViewModel
import com.zak.pressmark.feature.artist.vm.ArtistViewModelFactory
import com.zak.pressmark.feature.artworkpicker.route.CoverSearchRoute
import com.zak.pressmark.feature.capturecover.route.CaptureCoverFlowRoute
import com.zak.pressmark.feature.releasedetails.route.ReleaseDetailsRoute
import com.zak.pressmark.feature.releasedetails.vm.ReleaseDetailsViewModel
import com.zak.pressmark.feature.releasedetails.vm.ReleaseDetailsViewModelFactory
import com.zak.pressmark.feature.inbox.route.InboxRoute
import com.zak.pressmark.feature.inbox.vm.InboxViewModel
import com.zak.pressmark.feature.inbox.vm.InboxViewModelFactory
import com.zak.pressmark.feature.landing.route.LandingRoute
import com.zak.pressmark.feature.resolveinbox.route.ResolveInboxItemRoute
import com.zak.pressmark.feature.resolveinbox.vm.ResolveInboxViewModel
import com.zak.pressmark.feature.resolveinbox.vm.ResolveInboxViewModelFactory
import com.zak.pressmark.feature.scanconveyor.route.ScanConveyorRoute
import com.zak.pressmark.feature.scanconveyor.route.InboxCoverCaptureRoute
import com.zak.pressmark.feature.scanconveyor.vm.ScanConveyorViewModel
import com.zak.pressmark.feature.scanconveyor.vm.ScanConveyorViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PressmarkNavHost(
    navController: NavHostController,
    graph: AppGraph,
) {
    NavHost(
        navController = navController,
        startDestination = PressmarkRoutes.LANDING,
    ) {
        composable(PressmarkRoutes.LANDING) {
            LandingRoute(
                onOpenCatalog = { navController.navigate(PressmarkRoutes.LIST) },
                onOpenScanConveyor = { navController.navigate(PressmarkRoutes.SCAN_CONVEYOR) },
            )
        }

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
                onOpenScanConveyor = { navController.navigate(PressmarkRoutes.SCAN_CONVEYOR) },
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
                onAlbumSaved = { albumId, artist, title, releaseYear, label, catalogNo, barcode, intent ->
                    val origin = when (intent) {
                        SaveAndExit -> PressmarkRoutes.COVER_ORIGIN_LIST_SUCCESS
                        AddAnother -> PressmarkRoutes.COVER_ORIGIN_ADD_ANOTHER
                    }
                    navController.navigate(
                        PressmarkRoutes.coverSearch(
                            albumId = albumId,
                            artist = artist,
                            title = title,
                            releaseYear = releaseYear,
                            label = label,
                            catalogNo = catalogNo,
                            barcode = barcode,
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
            val releaseId =
                backStackEntry.arguments?.getString(PressmarkRoutes.ARG_ALBUM_ID).orEmpty()

            val factory =
                remember(graph, releaseId) { ReleaseDetailsViewModelFactory(graph, releaseId) }
            val vm: ReleaseDetailsViewModel = viewModel(
                key = "release_details_$releaseId",
                factory = factory,
            )

            ReleaseDetailsRoute(
                vm = vm,
                onBack = { navController.popBackStack() },
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
                navArgument(PressmarkRoutes.ARG_COVER_YEAR) {
                    type = NavType.StringType
                    defaultValue = ""
                },
                navArgument(PressmarkRoutes.ARG_COVER_LABEL) {
                    type = NavType.StringType
                    defaultValue = ""
                },
                navArgument(PressmarkRoutes.ARG_COVER_CATNO) {
                    type = NavType.StringType
                    defaultValue = ""
                },
                navArgument(PressmarkRoutes.ARG_COVER_BARCODE) {
                    type = NavType.StringType
                    defaultValue = ""
                },
                navArgument(PressmarkRoutes.ARG_COVER_ORIGIN) {
                    type = NavType.StringType
                    defaultValue = PressmarkRoutes.COVER_ORIGIN_BACK
                },
            ),
        ) { backStackEntry ->
            val releaseId =
                backStackEntry.arguments?.getString(PressmarkRoutes.ARG_ALBUM_ID).orEmpty()
            val artist =
                backStackEntry.arguments?.getString(PressmarkRoutes.ARG_COVER_ARTIST).orEmpty()
            val title =
                backStackEntry.arguments?.getString(PressmarkRoutes.ARG_COVER_TITLE).orEmpty()
            val releaseYearText =
                backStackEntry.arguments?.getString(PressmarkRoutes.ARG_COVER_YEAR).orEmpty()
            val label =
                backStackEntry.arguments?.getString(PressmarkRoutes.ARG_COVER_LABEL).orEmpty()
            val catalogNo =
                backStackEntry.arguments?.getString(PressmarkRoutes.ARG_COVER_CATNO).orEmpty()
            val barcode =
                backStackEntry.arguments?.getString(PressmarkRoutes.ARG_COVER_BARCODE).orEmpty()
            val origin =
                backStackEntry.arguments?.getString(PressmarkRoutes.ARG_COVER_ORIGIN)
                    ?: PressmarkRoutes.COVER_ORIGIN_BACK

            fun closeCoverFlow() {
                when (origin) {
                    PressmarkRoutes.COVER_ORIGIN_DETAILS -> {
                        navController.navigate(PressmarkRoutes.details(releaseId)) {
                            popUpTo(PressmarkRoutes.LIST) { inclusive = false }
                        }
                    }

                    PressmarkRoutes.COVER_ORIGIN_LIST_SUCCESS -> {
                        runCatching {
                            navController.getBackStackEntry(PressmarkRoutes.LIST)
                                .savedStateHandle
                                .setSavedAlbumId(releaseId)
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
                releaseId = releaseId,
                artist = artist,
                title = title,
                releaseYearText = releaseYearText,
                label = label,
                catalogNo = catalogNo,
                barcode = barcode,
                onTakePhoto = {
                    navController.navigate(
                        PressmarkRoutes.coverCapture(
                            albumId = releaseId,
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
            val releaseId = backStackEntry.arguments?.getString(PressmarkRoutes.ARG_ALBUM_ID).orEmpty()
            val origin = backStackEntry.arguments?.getString(PressmarkRoutes.ARG_COVER_ORIGIN)
                ?: PressmarkRoutes.COVER_ORIGIN_BACK

            fun closeAfterCapture() {
                when (origin) {
                    PressmarkRoutes.COVER_ORIGIN_DETAILS -> {
                        navController.navigate(PressmarkRoutes.details(releaseId)) {
                            popUpTo(PressmarkRoutes.LIST) { inclusive = false }
                        }
                    }

                    PressmarkRoutes.COVER_ORIGIN_LIST_SUCCESS -> {
                        runCatching {
                            navController.getBackStackEntry(PressmarkRoutes.LIST)
                                .savedStateHandle
                                .setSavedAlbumId(releaseId)
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
                releaseId = releaseId,
                releaseRepository = graph.releaseRepository,
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

        composable(PressmarkRoutes.SCAN_CONVEYOR) {
            val factory = remember(graph) {
                ScanConveyorViewModelFactory(
                    inboxRepository = graph.inboxRepository,
                    metadataProvider = graph.metadataProvider,
                    releaseRepository = graph.releaseRepository,
                )
            }
            val vm: ScanConveyorViewModel = viewModel(factory = factory)

            ScanConveyorRoute(
                vm = vm,
                onCaptureCover = { navController.navigate(PressmarkRoutes.INBOX_COVER_CAPTURE) },
                onOpenInbox = { navController.navigate(PressmarkRoutes.INBOX) },
            )
        }

        composable(PressmarkRoutes.INBOX_COVER_CAPTURE) {
            InboxCoverCaptureRoute(
                inboxRepository = graph.inboxRepository,
                onBack = { navController.popBackStack() },
                onDone = { navController.popBackStack() },
            )
        }

        composable(PressmarkRoutes.INBOX) {
            val factory = remember(graph) { InboxViewModelFactory(graph.inboxRepository) }
            val vm: InboxViewModel = viewModel(factory = factory)

            InboxRoute(
                vm = vm,
                onResolveItem = { inboxId ->
                    navController.navigate(PressmarkRoutes.resolveInbox(inboxId))
                },
            )
        }

        composable(
            route = PressmarkRoutes.RESOLVE_INBOX_PATTERN,
            arguments = listOf(navArgument(PressmarkRoutes.ARG_INBOX_ID) {
                type = NavType.StringType
            }),
        ) { backStackEntry ->
            val inboxId = backStackEntry.arguments?.getString(PressmarkRoutes.ARG_INBOX_ID).orEmpty()
            val factory = remember(graph, inboxId) {
                ResolveInboxViewModelFactory(inboxId, graph.inboxRepository, graph.releaseRepository)
            }
            val vm: ResolveInboxViewModel = viewModel(
                key = "resolve_inbox_$inboxId",
                factory = factory,
            )

            ResolveInboxItemRoute(
                vm = vm,
                onCommitComplete = { nextInboxId ->
                    if (nextInboxId != null) {
                        navController.navigate(PressmarkRoutes.resolveInbox(nextInboxId)) {
                            popUpTo(PressmarkRoutes.INBOX) { inclusive = false }
                        }
                    } else {
                        navController.popBackStack(PressmarkRoutes.INBOX, false)
                    }
                },
                onBack = { navController.popBackStack() },
            )
        }
    }
}
