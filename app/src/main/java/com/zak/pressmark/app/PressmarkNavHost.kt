package com.zak.pressmark.app

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.zak.pressmark.feature.ingest.manual.route.AddWorkRoute
import com.zak.pressmark.feature.ingest.route.IngestRoute
import com.zak.pressmark.feature.ingest.screen.IngestMode
import com.zak.pressmark.feature.library.route.LibraryRoute
import com.zak.pressmark.feature.library.vm.LibraryViewModel
import com.zak.pressmark.feature.refinepressing.route.RefinePressingRoute
import com.zak.pressmark.feature.workdetails.route.WorkDetailsRoute

@Composable
fun PressmarkNavHost(
    navController: NavHostController = rememberNavController(),
) {
    NavHost(
        navController = navController,
        startDestination = PressmarkRoutes.LIBRARY,
    ) {
        composable(PressmarkRoutes.LIBRARY) {
            val vm: LibraryViewModel = hiltViewModel()
            val entry = navController.currentBackStackEntry
            val addedWorkIdState = entry
                ?.savedStateHandle
                ?.getStateFlow<String?>(LIBRARY_ADDED_WORK_ID_KEY, null)
                ?.collectAsStateWithLifecycle()
            val addedWorkId = addedWorkIdState?.value

            LibraryRoute(
                vm = vm,
                onOpenWork = { workId ->
                    navController.navigate(PressmarkRoutes.workDetails(workId))
                },
                onAddManual = { navController.navigate(PressmarkRoutes.ADD_WORK) },

                // Single canonical entrypoint: scan-first.
                onAddBarcode = { navController.navigate(PressmarkRoutes.BARCODE_SCANNER) },

                addedWorkId = addedWorkId,
                onConsumeAddedWorkId = {
                    entry?.savedStateHandle?.set(LIBRARY_ADDED_WORK_ID_KEY, null)
                },
            )
        }

        composable(PressmarkRoutes.ADD_WORK) {
            AddWorkRoute(
                onDone = { navController.popBackStack() },
                onAdded = { workId ->
                    navController.getBackStackEntry(PressmarkRoutes.LIBRARY)
                        .savedStateHandle[LIBRARY_ADDED_WORK_ID_KEY] = workId
                },
            )
        }

        composable(PressmarkRoutes.BARCODE_SCANNER) {
            IngestRoute(
                onBack = { navController.popBackStack() },
                onAddedWork = { workId ->
                    navController.getBackStackEntry(PressmarkRoutes.LIBRARY)
                        .savedStateHandle[LIBRARY_ADDED_WORK_ID_KEY] = workId
                },
                initialMode = IngestMode.SCAN,
            )
        }

        composable(
            route = PressmarkRoutes.WORK_DETAILS_PATTERN,
            arguments = listOf(navArgument(PressmarkRoutes.ARG_WORK_ID) { type = NavType.StringType }),
        ) {
            WorkDetailsRoute(
                onBack = { navController.popBackStack() },
                onRefinePressing = { workId ->
                    navController.navigate(PressmarkRoutes.refinePressing(workId))
                },
            )
        }

        composable(
            route = PressmarkRoutes.REFINE_PRESSING_PATTERN,
            arguments = listOf(navArgument(PressmarkRoutes.ARG_WORK_ID) { type = NavType.StringType }),
        ) {
            RefinePressingRoute(
                onBack = { navController.popBackStack() },
            )
        }
    }
}

private const val LIBRARY_ADDED_WORK_ID_KEY = "libraryAddedWorkId"
