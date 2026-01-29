package com.zak.pressmark.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.ExperimentalMaterial3AdaptiveNavigationSuiteApi
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.zak.pressmark.R
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
        val icon: androidx.compose.ui.graphics.vector.ImageVector,
    ) : TopLevelDestination

    data class Drawable(
        val route: String,
        override val label: String,
        val resId: Int,
    ) : TopLevelDestination

    data class Action(
        override val label: String,
        val icon: androidx.compose.ui.graphics.vector.ImageVector,
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

    val destinations: List<TopLevelDestination> = remember(searchExpanded) {
        listOf(
            TopLevelDestination.Vector(
                route = PressmarkRoutes.LIBRARY,
                label = "Library",
                icon = Icons.Outlined.LibraryMusic,
            ),
            TopLevelDestination.Drawable(
                route = PressmarkRoutes.ADD_BARCODE,
                label = "Add",
                resId = R.drawable.barcode_scanner,
            ),
            TopLevelDestination.Action(
                label = "Search",
                icon = Icons.Outlined.Search,
                selected = { searchExpanded },
                onClick = { searchExpanded = !searchExpanded },
            ),
        )
    }

    NavigationSuiteScaffold(
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
                                if (!selected) {
                                    navController.navigate(destination.route) {
                                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                        )
                    }

                    is TopLevelDestination.Drawable -> {
                        val selected = currentDestination.isTopLevelSelected(destination.route)
                        item(
                            icon = {
                                Icon(
                                    painter = painterResource(destination.resId),
                                    contentDescription = destination.label,
                                )
                            },
                            label = { Text(destination.label) },
                            selected = selected,
                            onClick = {
                                if (!selected) {
                                    navController.navigate(destination.route) {
                                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                        )
                    }

                    is TopLevelDestination.Action -> {
                        item(
                            icon = { Icon(destination.icon, contentDescription = destination.label) },
                            label = { Text(destination.label) },
                            selected = destination.selected(),
                            onClick = destination.onClick,
                        )
                    }
                }
            }
        },
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            content()

            // ✅ This is your proven FAB search bar implementation, reused.
            // IMPORTANT: scaffoldBottomPadding = 0.dp because NavigationSuiteScaffold
            // already lays out content above its own bottom bar/rail.
            LibrarySearchBar(
                modifier = Modifier.fillMaxSize(),
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                onClear = { searchQuery = "" },
                expanded = searchExpanded,
                onExpandedChange = { searchExpanded = it },
                placeholder = "Search library…",
                expandedKeyboardGap = 2.dp,
            )
        }
    }
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
