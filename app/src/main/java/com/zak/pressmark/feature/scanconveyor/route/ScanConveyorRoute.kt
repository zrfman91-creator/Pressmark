package com.zak.pressmark.feature.scanconveyor.route

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.zak.pressmark.data.work.InboxPipelineScheduler
import com.zak.pressmark.feature.scanconveyor.screen.ScanConveyorScreen
import com.zak.pressmark.feature.scanconveyor.vm.ScanConveyorViewModel

@Composable
fun ScanConveyorRoute(
    vm: ScanConveyorViewModel,
    onScanBarcode: () -> Unit,
    onCaptureCover: () -> Unit,
    onOpenInbox: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val inboxCount = vm.inboxCount.collectAsState().value
    val libraryCount = vm.libraryCount.collectAsState().value
    val context = LocalContext.current

    ScanConveyorScreen(
        inboxCount = inboxCount,
        libraryCount = libraryCount,
        onScanBarcode = onScanBarcode,
        onCaptureCover = onCaptureCover,
        onQuickAdd = { title, artist ->
            vm.quickAdd(title, artist) {
                InboxPipelineScheduler.enqueueLookupDrain(context)
            }
        },
        onOpenInbox = onOpenInbox,
        modifier = modifier,
    )
}
