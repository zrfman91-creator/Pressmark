package com.zak.pressmark.feature.library.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.zak.pressmark.feature.library.vm.LibraryItemUi

@Composable
fun LibraryOverlays(
    deleteTarget: LibraryItemUi?,
    onDismissDelete: () -> Unit,
    onConfirmDelete: (LibraryItemUi) -> Unit,
) {
    deleteTarget?.let { target ->
        AlertDialog(
            modifier = Modifier
                .fillMaxWidth(),
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(4.dp),
            onDismissRequest = onDismissDelete,
            title = { Text(
                modifier = Modifier.fillMaxWidth(),
                text = "Remove album?",
                textAlign = TextAlign.Center
            ) },
            text = {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = buildAnnotatedString {
                        append("This will remove\n")
                        withStyle(
                            style = SpanStyle(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = MaterialTheme.typography.titleMedium.fontSize,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        ) {
                            append("“${target.title}”")
                        }

                        append("\nfrom your library.")
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    textAlign = TextAlign.Center
                )
                   },
            confirmButton = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                ){
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth(),
                        content = {
                            OutlinedButton(
                                shape = RoundedCornerShape(4.dp),
                                onClick = { onConfirmDelete(target) },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    contentColor = MaterialTheme.colorScheme.onSurface,
                                ),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                            ) { Text("Cancel") }

                            Button(
                                modifier = Modifier
                                    .padding(start = 8.dp)
                                    .weight(1f),
                                shape = RoundedCornerShape(4.dp),
                                onClick = { onConfirmDelete(target) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                    contentColor = MaterialTheme.colorScheme.onError,
                                ),
                            ) { Text("Remove") }
                        }
                    )
                }
            },
        )
    }
}
