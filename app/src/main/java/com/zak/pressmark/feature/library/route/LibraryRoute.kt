package com.zak.pressmark.feature.library.route

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zak.pressmark.feature.library.screen.LibraryScreen
import com.zak.pressmark.feature.library.vm.LibraryItemUi
import com.zak.pressmark.feature.library.vm.LibraryViewModel

@Composable
fun LibraryRoute(
    vm: LibraryViewModel,
    onOpenWork: (String) -> Unit,
    onAddManual: () -> Unit,
    onAddBarcode: () -> Unit,
) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val (deleteTarget, setDeleteTarget) = remember { mutableStateOf<LibraryItemUi?>(null) }

    LibraryScreen(
        state = state,
        onOpenWork = onOpenWork,
        onAddManual = onAddManual,
        onAddBarcode = onAddBarcode,
        onSortChanged = vm::updateSort,
        onGroupChanged = vm::updateGroup,
        onToggleGroup = vm::toggleGroupExpanded,
        onToggleAllSections = vm::toggleAllSections,
        deleteTarget = deleteTarget,
        onRequestDelete = { setDeleteTarget(it) },
        onDismissDelete = { setDeleteTarget(null) },
        onConfirmDelete = { target ->
            vm.deleteWork(target.workId)
            setDeleteTarget(null)
        },
    )
}
