package com.khaled.frais.features.widgets

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.khaled.frais.ui.components.NothingDivider
import com.khaled.frais.ui.components.NothingSectionHeader

@Composable
fun WidgetPage(
    viewModel: WidgetViewModel = viewModel()
) {
    val widgets by viewModel.widgets.collectAsState()
    var showPicker by remember { mutableStateOf(false) }
    val context = LocalContext.current
    
    val configLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data = result.data
        val appWidgetId = data?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1) ?: -1
        if (result.resultCode == android.app.Activity.RESULT_OK && appWidgetId != -1) {
            val provider = WidgetManager.getAppWidgetInfo(appWidgetId)
            provider?.let {
                WidgetManager.addWidget(appWidgetId, it.provider.flattenToString())
            }
        } else if (appWidgetId != -1) {
            WidgetManager.deleteAppWidgetId(appWidgetId)
        }
    }

    val bindLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data = result.data
        val appWidgetId = data?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1) ?: -1
        if (result.resultCode == android.app.Activity.RESULT_OK && appWidgetId != -1) {
            val provider = WidgetManager.getAppWidgetInfo(appWidgetId)
            if (provider != null) {
                if (provider.configure != null) {
                    val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE).apply {
                        component = provider.configure
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                    }
                    configLauncher.launch(intent)
                } else {
                    WidgetManager.addWidget(appWidgetId, provider.provider.flattenToString())
                }
            }
        } else if (appWidgetId != -1) {
            WidgetManager.deleteAppWidgetId(appWidgetId)
        }
    }

    DisposableEffect(Unit) {
        WidgetManager.startListening()
        onDispose {
            WidgetManager.stopListening()
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showPicker = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = MaterialTheme.shapes.extraSmall
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Widget")
            }
        },
        containerColor = androidx.compose.ui.graphics.Color.Transparent
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            NothingSectionHeader(
                text = "WIDGET HUB",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            if (widgets.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                    Text("NO WIDGETS ADDED", style = MaterialTheme.typography.labelSmall, color = androidx.compose.ui.graphics.Color.Gray)
                }
            } else {
                WidgetStack(
                    widgets = widgets,
                    onRemoveWidget = { viewModel.removeWidget(it) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    if (showPicker) {
        WidgetPicker(
            onWidgetSelected = { provider ->
                showPicker = false
                val id = WidgetManager.allocateAppWidgetId()
                
                if (WidgetManager.bindWidget(id, provider.provider)) {
                    if (provider.configure != null) {
                        val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE).apply {
                            component = provider.configure
                            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                        }
                        try {
                            configLauncher.launch(intent)
                        } catch (e: Exception) {
                            WidgetManager.deleteAppWidgetId(id)
                            com.khaled.frais.utils.HUI.showToast("FAILED TO LAUNCH CONFIGURATION")
                        }
                    } else {
                        WidgetManager.addWidget(id, provider.provider.flattenToString())
                    }
                } else {
                    val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_BIND).apply {
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, provider.provider)
                    }
                    try {
                        bindLauncher.launch(intent)
                    } catch (e: Exception) {
                        WidgetManager.deleteAppWidgetId(id)
                        com.khaled.frais.utils.HUI.showToast("FAILED TO BIND WIDGET")
                    }
                }
            },
            onDismiss = { showPicker = false }
        )
    }
}

@Composable
fun WidgetPicker(
    onWidgetSelected: (AppWidgetProviderInfo) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val initialProviders = remember { WidgetManager.getInitialProviders() }
    val allProviders = remember { 
        WidgetManager.getInstalledProviders()
            .filter { it !in initialProviders }
            .sortedBy { it.loadLabel(context.packageManager).lowercase() } 
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ADD WIDGET", style = MaterialTheme.typography.labelMedium) },
        text = {
            LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                if (initialProviders.isNotEmpty()) {
                    item {
                        Text(
                            "RECOMMENDED",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    items(initialProviders) { provider ->
                        WidgetPickerItem(provider, onWidgetSelected)
                    }
                    item {
                        Spacer(Modifier.height(16.dp))
                        NothingDivider()
                        Spacer(Modifier.height(16.dp))
                    }
                }
                
                item {
                    Text(
                        "ALL WIDGETS",
                        style = MaterialTheme.typography.labelSmall,
                        color = androidx.compose.ui.graphics.Color.Gray,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                
                items(allProviders) { provider ->
                    WidgetPickerItem(provider, onWidgetSelected)
                }
            }
        },
        confirmButton = { 
            TextButton(onClick = onDismiss) { 
                Text("CANCEL", style = MaterialTheme.typography.labelSmall) 
            } 
        },
        shape = MaterialTheme.shapes.extraSmall
    )
}

@Composable
fun WidgetPickerItem(
    provider: AppWidgetProviderInfo,
    onWidgetSelected: (AppWidgetProviderInfo) -> Unit
) {
    val context = LocalContext.current
    ListItem(
        headlineContent = { 
            Text(
                provider.loadLabel(context.packageManager).uppercase(),
                style = MaterialTheme.typography.labelSmall
            ) 
        },
        supportingContent = { 
            Text(
                provider.provider.packageName,
                style = MaterialTheme.typography.labelSmall,
                color = androidx.compose.ui.graphics.Color.Gray
            ) 
        },
        modifier = Modifier.clickable { onWidgetSelected(provider) }
    )
}
