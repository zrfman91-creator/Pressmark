package com.zak.pressmark.feature.inbox.route

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import com.zak.pressmark.feature.inbox.screen.InboxScreen
import com.zak.pressmark.feature.inbox.vm.InboxViewModel

@Composable
fun InboxRoute(
    vm: InboxViewModel,
    onResolveItem: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val items = vm.inboxItems.collectAsState().value
    val selectedFilter = vm.selectedFilter.collectAsState().value

    InboxScreen(
        items = items,
        selectedFilter = selectedFilter,
        onFilterChange = vm::setFilter,
        onResolveItem = { item -> onResolveItem(item.id) },
        onRetryFailed = vm::retryFailed,
        modifier = modifier,
    )
}
