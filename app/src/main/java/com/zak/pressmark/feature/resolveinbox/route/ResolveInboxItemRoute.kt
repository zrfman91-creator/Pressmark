package com.zak.pressmark.feature.resolveinbox.route

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import com.zak.pressmark.feature.resolveinbox.screen.ResolveInboxItemScreen
import com.zak.pressmark.feature.resolveinbox.vm.ResolveInboxViewModel

@Composable
fun ResolveInboxItemRoute(
    vm: ResolveInboxViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val item = vm.inboxItem.collectAsState().value
    val candidates = vm.topCandidates.collectAsState().value

    ResolveInboxItemScreen(
        inboxItem = item,
        candidates = candidates,
        onCommit = vm::commit,
        onBack = onBack,
        modifier = modifier,
    )
}
