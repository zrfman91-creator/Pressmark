package com.zak.pressmark.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.zak.pressmark.feature.ingest.barcode.route.AddBarcodeRoute
import com.zak.pressmark.feature.ingest.barcode.scan.BarcodeScannerRoute
import com.zak.pressmark.feature.ingest.barcode.vm.AddBarcodeViewModel
import com.zak.pressmark.feature.ingest.manual.route.AddWorkRoute
import com.zak.pressmark.feature.library.route.LibraryRoute
import com.zak.pressmark.feature.library.vm.LibraryViewModel
import com.zak.pressmark.feature.workdetails.route.WorkDetailsRoute
import kotlinx.coroutines.flow.collectLatest

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
            LibraryRoute(
                vm = vm,
                onOpenWork = { workId ->
                    navController.navigate(PressmarkRoutes.workDetails(workId))
                },
                onAddManual = { navController.navigate(PressmarkRoutes.ADD_WORK) },
                onAddBarcode = { navController.navigate(PressmarkRoutes.ADD_BARCODE) },
            )
        }

        composable(PressmarkRoutes.ADD_WORK) {
            // Manual add now self-contains its flow and returns via onDone().
            AddWorkRoute(
                onDone = { navController.popBackStack() },
            )
        }

        composable(PressmarkRoutes.ADD_BARCODE) {
            val vm: AddBarcodeViewModel = hiltViewModel()

            // Listen for scan results returned from BarcodeScannerRoute via savedStateHandle.
            LaunchedEffect(Unit) {
                val handle = navController.currentBackStackEntry?.savedStateHandle ?: return@LaunchedEffect
                handle.getStateFlow<String?>(key = SCANNED_BARCODE_KEY, initialValue = null)
                    .collectLatest { scanned ->
                        if (!scanned.isNullOrBlank()) {
                            handle[SCANNED_BARCODE_KEY] = null // consume
                            vm.onBarcodeChanged(scanned)
                            vm.searchByBarcode()
                        }
                    }
            }

            AddBarcodeRoute(
                vm = vm,
                onDone = { navController.popBackStack() },
                onScan = { navController.navigate(PressmarkRoutes.BARCODE_SCANNER) },
                // Stay on the "Add by barcode" screen after add.
                onAdded = { _, autoReopen ->
                    if (autoReopen) {
                        navController.navigate(PressmarkRoutes.BARCODE_SCANNER)
                    }
                },
            )
        }

        composable(PressmarkRoutes.BARCODE_SCANNER) {
            BarcodeScannerRoute(
                onBarcodeDetected = { barcode ->
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set(SCANNED_BARCODE_KEY, barcode)
                    navController.popBackStack()
                },
                onCancel = { navController.popBackStack() },
            )
        }

        composable(
            route = PressmarkRoutes.WORK_DETAILS_PATTERN,
            arguments = listOf(navArgument(PressmarkRoutes.ARG_WORK_ID) { type = NavType.StringType }),
        ) {
            WorkDetailsRoute(
                onBack = { navController.popBackStack() },
            )
        }
    }
}

private const val SCANNED_BARCODE_KEY = "scannedBarcode"
