package com.zak.pressmark.feature.library.screen

import androidx.compose.runtime.Composable
import com.zak.pressmark.feature.library.route.LibraryRoute
import com.zak.pressmark.feature.library.vm.LibraryViewModel

@Composable
fun LibraryScreen(
    vm: LibraryViewModel,
    onOpenWork: (String) -> Unit,
    onAddManual: () -> Unit,
    onAddBarcode: () -> Unit,
) {
    LibraryRoute(
        vm = vm,
        onOpenWork = onOpenWork,
        onAddManual = onAddManual,
        onAddBarcode = onAddBarcode,
    )
}
