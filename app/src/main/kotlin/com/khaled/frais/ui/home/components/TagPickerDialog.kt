package com.khaled.frais.ui.home.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.khaled.frais.app.AppInfo
import com.khaled.frais.app.FraisData

@Composable
fun TagPickerDialog(app: AppInfo, onDismiss: () -> Unit) {
    val filters = FraisData.tags

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.extraSmall,
        title = { Text("MANAGE FILTERS", fontWeight = FontWeight.Bold) },
        text = {
            if (filters.isEmpty()) {
                Text("No filters created yet.")
            } else {
                LazyColumn {
                    items(filters) { filter ->
                        var isChecked by remember { mutableStateOf(filter.id == app.manualTagId) }
                        ListItem(
                            headlineContent = { Text(filter.name.uppercase(), style = MaterialTheme.typography.labelMedium) },
                            trailingContent = {
                                RadioButton(
                                    selected = isChecked,
                                    onClick = { 
                                        if (isChecked) app.manualTagId = null
                                        else app.manualTagId = filter.id
                                        onDismiss()
                                    }
                                )
                            },
                            modifier = Modifier.clickable {
                                if (isChecked) app.manualTagId = null
                                else app.manualTagId = filter.id
                                onDismiss()
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("DONE") }
        }
    )
}
