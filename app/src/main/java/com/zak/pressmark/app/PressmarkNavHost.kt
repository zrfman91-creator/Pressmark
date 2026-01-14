package com.zak.pressmark.app

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.zak.pressmark.feature.artworkpicker.DiscogsCoverSearchViewModel
import com.zak.pressmark.feature.artworkpicker.ArtworkPickerViewModelFactory
import com.zak.pressmark.feature.albumlist.route.AlbumListRoute
import com.zak.pressmark.feature.albumlist.vm.AlbumListViewModel
import com.zak.pressmark.feature.albumlist.vm.AlbumListViewModelFactory
import com.zak.pressmark.feature.artist.screen.ArtistRoute
import com.zak.pressmark.feature.artist.vm.ArtistViewModel
import com.zak.pressmark.feature.artist.vm.ArtistViewModelFactory
import com.zak.pressmark.core.artwork.ArtworkPickerDialog
import com.zak.pressmark.core.artwork.ArtworkCandidate
import com.zak.pressmark.core.artwork.ArtworkProviderId
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
            // --- Album list VM (existing) ---
            val listFactory = remember(graph) {
                AlbumListViewModelFactory(
                    albumRepo = graph.albumRepository,
                    artistRepo = graph.artistRepository,
                )
            }
            val vm: AlbumListViewModel = viewModel(factory = listFactory)

            // --- NavHost-owned cover search dialog state ---
            var coverAlbumId: String? by remember { mutableStateOf<String?>(null)}
            var coverArtist by remember { mutableStateOf("") }
            var coverTitle by remember { mutableStateOf("") }
            var coverDialogOpen by remember { mutableStateOf(false) }

            AlbumListRoute(
                vm = vm,
                graph = graph,
                onAddAlbum = { navController.navigate(PressmarkRoutes.ADD) },
                onOpenAlbum = { albumId ->
                    navController.navigate(PressmarkRoutes.details(albumId))
                },
                onOpenCoverSearch = { albumId, artist, title ->
                    coverAlbumId = albumId
                    coverArtist = artist
                    coverTitle = title
                    coverDialogOpen = true
                },
            )

            // --- Discogs Cover Search VM is created HERE (NavHost scope) ---
            if (coverDialogOpen && !coverAlbumId.isNullOrBlank()) {

                // Factory is DI-only (stable)
                val coverFactory = remember(graph) {
                    ArtworkPickerViewModelFactory(
                        albumRepository = graph.albumRepository,
                        discogsApi = graph.discogsApiService,
                    )
                }

                val searchVm: DiscogsCoverSearchViewModel = viewModel(
                    key = "cover_search", // stable key; VM is driven by start()
                    factory = coverFactory,
                )
                val searchState by searchVm.uiState.collectAsStateWithLifecycle()

                // Kick off / update search when dialog inputs change
                LaunchedEffect(coverAlbumId, coverArtist, coverTitle) {
                    searchVm.start(
                        albumId = coverAlbumId!!,
                        artist = coverArtist,
                        title = coverTitle,
                    )
                }

                val discogsResults = searchState.results
                val discogsById = remember(discogsResults) {
                    discogsResults.associateBy { it.id.toString() }
                }

                val candidates: List<ArtworkCandidate> = remember(discogsResults) {
                    discogsResults.map { r ->
                        ArtworkCandidate(
                            provider = ArtworkProviderId.DISCOGS,
                            providerItemId = r.id.toString(),
                            imageUrl = r.coverImage ?: r.thumb,
                            thumbUrl = r.thumb,
                            displayTitle = r.title.toString(),
                            displayArtist = null,
                            subtitle = null,
                        )
                    }
                }

                ArtworkPickerDialog(
                    artist = coverArtist,
                    title = coverTitle,
                    results = candidates,
                    onPick = { candidate ->
                        discogsById[candidate.providerItemId]?.let { picked ->
                            searchVm.pickResult(picked)
                        }
                        coverDialogOpen = false
                        coverAlbumId = null
                        coverArtist = ""
                        coverTitle = ""
                    },
                    onDismiss = {
                        coverDialogOpen = false
                        coverAlbumId = null
                        coverArtist = ""
                        coverTitle = ""
                    },
                )

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
                    onNavigateUp = { navController.popBackStack() },
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
        }
        composable(
            route = PressmarkRoutes.ARTIST_PATTERN,
            arguments = listOf(navArgument(PressmarkRoutes.ARG_ARTIST_ID) { type = NavType.LongType }),
        ) { backStackEntry ->
            val artistId = backStackEntry.arguments?.getLong(PressmarkRoutes.ARG_ARTIST_ID) ?: 0L
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
}
