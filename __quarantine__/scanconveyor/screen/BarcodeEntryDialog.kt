package com.zak.pressmark.feature.scanconveyor.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun BarcodeEntryDialog(
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var barcode by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        barcode = ""
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Scan barcode") },
        text = {
            Column {
                Text("Enter a barcode.")
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = barcode,
                    onValueChange = { barcode = it },
                    label = { Text("Barcode") },
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(barcode.trim()) },
                enabled = barcode.isNotBlank(),
            ) {
                Text("Save to Inbox")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        modifier = modifier,
    )
}
