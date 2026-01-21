package com.zak.pressmark.feature.resolveinbox.route

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.zak.pressmark.data.work.InboxPipelineScheduler
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
    val selectedCandidateId = vm.selectedCandidateId.collectAsState().value
    val errorMessage = vm.errorMessage.collectAsState().value
    val context = LocalContext.current

    ResolveInboxItemScreen(
        inboxItem = item,
        candidates = candidates,
        selectedCandidateId = selectedCandidateId,
        errorMessage = errorMessage,
        onSelectCandidate = vm::selectCandidate,
        onCommit = { vm.commitSelectedCandidate(candidates) },
        onAddDetails = { title, artist, label, catalogNo, format ->
            vm.updateManualDetails(title, artist, label, catalogNo, format)
            InboxPipelineScheduler.enqueueLookupDrain(context)
        },
        onDismissError = vm::clearError,
        onBack = onBack,
        modifier = modifier,
    )
}
