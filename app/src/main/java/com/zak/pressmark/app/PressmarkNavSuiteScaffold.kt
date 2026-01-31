@file:OptIn(ExperimentalMaterial3Api::class)

package com.zak.pressmark.app

import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardAlt
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.ExperimentalMaterial3AdaptiveNavigationSuiteApi
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.zak.pressmark.R
import com.zak.pressmark.feature.ingest.barcode.scan.BarcodeScannerIngestHandler
import com.zak.pressmark.feature.ingest.barcode.ui.ManualEntryOverlay
import com.zak.pressmark.feature.library.ui.LibrarySearchBar

/**
 * Pressmark adaptive navigation shell + a global "Search" action.
 *
 * Key detail:
 * - We REUSE your existing FAB-style LibrarySearchBar so the IME/padding behavior matches exactly.
 * - Because NavigationSuiteScaffold already owns the bottom navigation/rail layout, we pass
 *   scaffoldBottomPadding = 0.dp so you don't double-account for system nav bars.
 */

private sealed interface TopLevelDestination {
    val label: String

    data class Vector(
        val route: String,
        override val label: String,
        val icon: ImageVector,
    ) : TopLevelDestination

    data class Drawable(
        val route: String,
        override val label: String,
        val resId: Int,
    ) : TopLevelDestination

    data class Action(
        override val label: String,
        val icon: ImageVector,
        val onClick: () -> Unit,
        val selected: () -> Boolean = { false },
    ) : TopLevelDestination
}




@OptIn(ExperimentalMaterial3AdaptiveNavigationSuiteApi::class)
@Composable
fun PressmarkNavSuiteScaffold(
    navController: NavHostController,
    content: @Composable () -> Unit,
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    var searchExpanded by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }

    var ingestSheetOpen by rememberSaveable { mutableStateOf(false) }
    var ingestMode by rememberSaveable { mutableStateOf(IngestMode.CAMERA) }

    var manualBarcodeExpanded by rememberSaveable { mutableStateOf(false) }
    var manualBarcode by rememberSaveable { mutableStateOf("") }

    var manualArtist by rememberSaveable { mutableStateOf("") }
    var manualTitle by rememberSaveable { mutableStateOf("") }

    val isScannerDestination = currentDestination?.hierarchy?.any { it.route == PressmarkRoutes.BARCODE_SCANNER } == true
    val scanIconInteraction = remember { MutableInteractionSource() }

    LaunchedEffect(isScannerDestination, ingestMode) {
        if (!isScannerDestination) {
            manualBarcodeExpanded = false
            ingestSheetOpen = false
            manualBarcode = ""
            manualArtist = ""
            manualTitle = ""
        } else if (ingestMode == IngestMode.MANUAL) {
            manualBarcodeExpanded = true
        }
    }
    LaunchedEffect(manualBarcodeExpanded) {
        BarcodeScannerIngestHandler.manualEntryExpanded = manualBarcodeExpanded
    }

    val actionLabel = if (isScannerDestination) "Manual Entry" else "Search"   // Library search & manual barcode entry icon/logic swap on destination change.
    val actionSelected = { if (isScannerDestination) manualBarcodeExpanded else searchExpanded }
    val actionIcon = if (isScannerDestination) Icons.Outlined.KeyboardAlt else Icons.Outlined.Search
    val onActionClick = {
        if (isScannerDestination) {
            ingestMode = IngestMode.MANUAL
            ingestSheetOpen = false
            manualBarcodeExpanded = true
        } else {
            searchExpanded = !searchExpanded
        }
    }

    val itemColors = NavigationSuiteDefaults.itemColors(
        navigationBarItemColors = NavigationBarItemDefaults.colors(
            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
            selectedTextColor = MaterialTheme.colorScheme.onPrimary,
            unselectedIconColor = MaterialTheme.colorScheme.primaryContainer,
            unselectedTextColor = MaterialTheme.colorScheme.primaryContainer,
        ),
        navigationRailItemColors = NavigationRailItemDefaults.colors(
            indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
            selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
            selectedTextColor = MaterialTheme.colorScheme.onSecondaryContainer,
            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
        navigationDrawerItemColors = NavigationDrawerItemDefaults.colors(
            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
            selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
            selectedTextColor = MaterialTheme.colorScheme.onSecondaryContainer,
            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    )  // Navigation scaffold color selections

    val navContainer = MaterialTheme.colorScheme.primary

    val destinations: List<TopLevelDestination> = remember(searchExpanded, manualBarcodeExpanded, isScannerDestination) {
        listOf(
            TopLevelDestination.Vector(
                route = PressmarkRoutes.LIBRARY,
                label = "Library",
                icon = Icons.Outlined.LibraryMusic,

            ),
            TopLevelDestination.Drawable(
                route = PressmarkRoutes.BARCODE_SCANNER,
                label = "Add Albums",
                resId = R.drawable.barcode_scanner,
            ),
            TopLevelDestination.Action(
                label = actionLabel,
                icon = actionIcon,
                selected = actionSelected,
                onClick = onActionClick,
            ),
        )
    }

    NavigationSuiteScaffold(
        navigationSuiteColors = NavigationSuiteDefaults.colors(
            navigationBarContainerColor = navContainer,          // <- bar BG
            navigationBarContentColor = MaterialTheme.colorScheme.onSurface,         // <- default content tint
        ),
        navigationSuiteItems = {
            destinations.forEach { destination ->
                when (destination) {
                    is TopLevelDestination.Vector -> {
                        val selected = currentDestination.isTopLevelSelected(destination.route)
                        item(
                            icon = { Icon(destination.icon, contentDescription = destination.label) },
                            label = { Text(destination.label) },
                            selected = selected,
                            onClick = {
                                navController.navigate(destination.route) {
                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            colors = itemColors,
                        )
                    }

                    is TopLevelDestination.Drawable -> {
                        val selected = currentDestination.isTopLevelSelected(destination.route)
                        val handleScanClick = {
                            ingestSheetOpen = false
                            manualBarcodeExpanded = false
                            ingestMode = IngestMode.CAMERA
                            navController.navigate(PressmarkRoutes.BARCODE_SCANNER) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                        item(
                            icon = {
                                Icon(
                                    painter = painterResource(destination.resId),
                                    contentDescription = destination.label,
                                    modifier = Modifier.combinedClickable(
                                        interactionSource = scanIconInteraction,
                                        indication = null,
                                        onClick = handleScanClick,
                                        onLongClick = { ingestSheetOpen = true },
                                    ),
                                )
                            },
                            label = { Text(destination.label) },
                            selected = selected,
                            onClick = handleScanClick,
                            colors = itemColors,
                        )
                    }

                    is TopLevelDestination.Action -> {
                        item(
                            icon = {
                                Icon(
                                    destination.icon,
                                    contentDescription = destination.label
                                )
                            },
                            label = { Text(destination.label) },
                            selected = destination.selected(),
                            onClick = destination.onClick,
                            colors = itemColors,
                        )
                    }
                }
            }
        },
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            content()

            LibrarySearchBar(
                modifier = Modifier.fillMaxSize(),
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                onClear = { searchQuery = "" },
                expanded = searchExpanded,
                onExpandedChange = { searchExpanded = it },
                placeholder = "Search library\u2026",
                expandedKeyboardGap = 2.dp,
            )

            ManualEntryOverlay(
                modifier = Modifier.fillMaxSize(),
                expanded = manualBarcodeExpanded && isScannerDestination,

                barcode = manualBarcode,
                onBarcodeChange = { manualBarcode = it },

                artist = manualArtist,
                onArtistChange = { manualArtist = it },

                title = manualTitle,
                onTitleChange = { manualTitle = it },

                onDismiss = { manualBarcodeExpanded = false },

                onSubmit = { inputs ->

                    BarcodeScannerIngestHandler.onManualSubmit?.invoke(inputs)
                    manualBarcodeExpanded = false
                },

            )

            if (ingestSheetOpen) {
                ModalBottomSheet(
                    onDismissRequest = { ingestSheetOpen = false },
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        Text(
                            text = "Ingest mode",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 8.dp),
                        )
                        ListItem(
                            headlineContent = { Text("Barcode (camera)") },
                            modifier = Modifier.clickable {
                                ingestMode = IngestMode.CAMERA
                                ingestSheetOpen = false
                                manualBarcodeExpanded = false
                                navController.navigate(PressmarkRoutes.BARCODE_SCANNER) {
                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                        )
                        ListItem(
                            headlineContent = { Text("Barcode (manual)") },
                            modifier = Modifier.clickable {
                                ingestMode = IngestMode.MANUAL
                                ingestSheetOpen = false
                                navController.navigate(PressmarkRoutes.BARCODE_SCANNER) {
                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                        )
                        ListItem(
                            headlineContent = { Text("Cover OCR (coming soon)") },
                            modifier = Modifier.alpha(0.5f),
                        )
                        ListItem(
                            headlineContent = { Text("Label OCR (coming soon)") },
                            modifier = Modifier.alpha(0.5f),
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

private enum class IngestMode {
    CAMERA,
    MANUAL,
}

private fun NavDestination?.isTopLevelSelected(route: String): Boolean {
    val matchesHierarchy = this?.hierarchy?.any { it.route == route } == true
    if (matchesHierarchy) return true

    if (route == PressmarkRoutes.LIBRARY) {
        val currentRoute = this?.route.orEmpty()
        if (currentRoute == PressmarkRoutes.WORK_DETAILS_PATTERN || currentRoute.startsWith(PressmarkRoutes.WORK_DETAILS)) {
            return true
        }
    }
    return false
}
