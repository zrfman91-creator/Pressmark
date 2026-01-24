package com.zak.pressmark.feature.landing.route

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.zak.pressmark.feature.landing.screen.LandingScreen

@Composable
fun LandingRoute(
    onOpenCatalog: () -> Unit,
    onOpenScanConveyor: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LandingScreen(
        onOpenCatalog = onOpenCatalog,
        onOpenScanConveyor = onOpenScanConveyor,
        modifier = modifier,
    )
}
