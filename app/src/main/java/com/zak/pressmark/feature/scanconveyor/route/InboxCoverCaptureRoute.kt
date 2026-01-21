package com.zak.pressmark.feature.scanconveyor.route

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zak.pressmark.data.repository.InboxRepository
import com.zak.pressmark.data.work.InboxPipelineScheduler
import com.zak.pressmark.feature.capturecover.screen.CameraCoverCaptureRoute
import com.zak.pressmark.feature.scanconveyor.vm.InboxCoverCaptureEffect
import com.zak.pressmark.feature.scanconveyor.vm.InboxCoverCaptureViewModel
import com.zak.pressmark.feature.scanconveyor.vm.InboxCoverCaptureViewModelFactory

@Composable
fun InboxCoverCaptureRoute(
    inboxRepository: InboxRepository,
    onBack: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val factory = remember(inboxRepository) {
        InboxCoverCaptureViewModelFactory(inboxRepository)
    }
    val vm: InboxCoverCaptureViewModel = viewModel(factory = factory)

    LaunchedEffect(vm) {
        vm.effects.collect { effect ->
            when (effect) {
                is InboxCoverCaptureEffect.Done -> {
                    InboxPipelineScheduler.enqueueOcrDrain(context)
                    Toast.makeText(context, "Saved to Inbox", Toast.LENGTH_SHORT).show()
                    onDone()
                }
            }
        }
    }

    CameraCoverCaptureRoute(
        modifier = modifier,
        onBack = onBack,
        onCaptured = { uri ->
            vm.saveCover(uri)
        },
    )
}
