package com.khaled.frais.ui.home.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.khaled.frais.app.AppInfo
import com.khaled.frais.app.FraisData
import com.khaled.frais.ui.components.AppIcon
import com.khaled.frais.ui.components.GlyphState
import com.khaled.frais.ui.components.NothingDivider
import com.khaled.frais.ui.home.viewmodel.HomeViewModel
import com.khaled.frais.ui.theme.NothingRed

@Composable
fun FilterEditDialog(
    filter: FraisData.Tag,
    apps: List<AppInfo>,
    viewModel: HomeViewModel,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(filter.name) }
    var icon by remember { mutableStateOf(filter.icon) }
    var searchQuery by remember { mutableStateOf("") }
    
    var refreshKey by remember { mutableStateOf(0) }
    val appsInFilter = remember(apps, filter.id, refreshKey) {
        apps.filter { filter.id in it.tagIds }
    }
    
    val searchableApps = remember(apps, filter.id, searchQuery, refreshKey) {
        if (searchQuery.length < 2) emptyList()
        else apps.filter { it.name.contains(searchQuery, ignoreCase = true) && filter.id !in it.tagIds }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.extraSmall,
        title = { 
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("EDIT FILTER", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                if (!filter.isBuiltIn) {
                    IconButton(onClick = { 
                        FraisData.deleteTag(filter.id)
                        onDismiss()
                    }) {
                        Icon(Icons.Default.Delete, null, tint = NothingRed)
                    }
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (!filter.isBuiltIn) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("NAME") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.extraSmall
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = icon,
                        onValueChange = { icon = it },
                        label = { Text("ICON (EMOJI)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.extraSmall
                    )
                    Spacer(Modifier.height(16.dp))
                }
                
                NothingDivider()
                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("ADD APPS...", style = MaterialTheme.typography.labelSmall) },
                    leadingIcon = { Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.extraSmall,
                    singleLine = true
                )

                if (searchableApps.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Box(modifier = Modifier.heightIn(max = 200.dp)) {
                        LazyColumn {
                            items(searchableApps) { app ->
                                ListItem(
                                    headlineContent = { Text(app.name.uppercase(), style = MaterialTheme.typography.labelSmall) },
                                    leadingContent = { AppIcon(info = app.applicationInfo, size = 32.dp) },
                                    trailingContent = { Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary) },
                                    modifier = Modifier.clickable {
                                        app.manualTagId = filter.id
                                        app.excludedTagIds.remove(filter.id)
                                        FraisData.saveApps()
                                        refreshKey++
                                        searchQuery = ""
                                        viewModel.triggerTransientGlyph(GlyphState.ADDING)
                                    }
                                )
                            }
                        }
                    }
                }
                
                if (appsInFilter.isNotEmpty()) {
                    Spacer(Modifier.height(16.dp))
                    Text("CURRENT APPS (${appsInFilter.size})", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    Spacer(Modifier.height(8.dp))
                    
                    Box(modifier = Modifier.heightIn(max = 250.dp)) {
                        LazyColumn {
                            items(appsInFilter) { app ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AppIcon(info = app.applicationInfo, size = 32.dp)
                                    Spacer(Modifier.width(12.dp))
                                    Text(
                                        text = app.name.uppercase(),
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.weight(1f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    IconButton(
                                        onClick = {
                                            app.manualTagId = null
                                            if (filter.id !in app.excludedTagIds) {
                                                app.excludedTagIds.add(filter.id)
                                            }
                                            FraisData.saveApps()
                                            refreshKey++
                                            viewModel.triggerTransientGlyph(GlyphState.REMOVING)
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp), tint = NothingRed)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (!filter.isBuiltIn) {
                    filter.name = name
                    filter.icon = icon
                    FraisData.updateTag(filter)
                }
                onDismiss()
            }) { Text("DONE") }
        }
    )
}
