package com.zak.pressmark.feature.catalogdetails.route

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zak.pressmark.feature.catalogdetails.screen.CatalogDetailsScreen
import com.zak.pressmark.feature.catalogdetails.vm.CatalogDetailsViewModel

@Composable
fun CatalogDetailsRoute(
    vm: CatalogDetailsViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val details = vm.details.collectAsStateWithLifecycle().value
    CatalogDetailsScreen(
        details = details,
        onBack = onBack,
        modifier = modifier,
    )
}
