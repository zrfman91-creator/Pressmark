package com.zak.pressmark.feature.refinepressing.route

import androidx.compose.runtime.Composable
import com.zak.pressmark.feature.refinepressing.screen.RefinePressingScreen

@Composable
fun RefinePressingRoute(
    workId: String,
    onBack: () -> Unit,
) {
    RefinePressingScreen(
        workId = workId,
        onBack = onBack,
    )
}
