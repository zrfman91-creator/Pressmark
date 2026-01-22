package com.zak.pressmark.feature.inbox.route

import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.zak.pressmark.feature.inbox.screen.InboxScreen
import com.zak.pressmark.feature.inbox.vm.InboxEffect
import com.zak.pressmark.feature.inbox.vm.InboxViewModel

@Composable
fun InboxRoute(
    vm: InboxViewModel,
    onResolveItem: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val items = vm.inboxItems.collectAsState().value
    val selectedFilter = vm.selectedFilter.collectAsState().value
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(vm) {
        vm.effects.collect { effect ->
            when (effect) {
                is InboxEffect.ShowUndoDelete -> {
                    val result = snackbarHostState.showSnackbar(
                        message = "Inbox item deleted",
                        actionLabel = "Undo",
                        withDismissAction = true,
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        vm.undoDeleteInboxItem(effect.inboxItemId)
                    }
                }
            }
        }
    }

    InboxScreen(
        items = items,
        selectedFilter = selectedFilter,
        onFilterChange = vm::setFilter,
        onResolveItem = { item -> onResolveItem(item.id) },
        onRetryFailed = vm::retryFailed,
        onDelete = { item -> vm.deleteInboxItem(item.id) },
        onToggleUnknown = { item, isUnknown -> vm.setUnknown(item.id, isUnknown) },
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    )
}
