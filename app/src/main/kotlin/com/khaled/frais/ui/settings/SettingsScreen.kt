package com.khaled.frais.ui.settings

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.khaled.frais.app.BackupManager
import com.khaled.frais.app.FraisData
import com.khaled.frais.ui.components.*
import com.khaled.frais.ui.home.FilterEditDialog
import com.khaled.frais.ui.home.HomeViewModel
import com.khaled.frais.ui.theme.NothingRed
import com.khaled.frais.utils.HUI
import kotlinx.coroutines.launch
import me.zhanghai.compose.preference.rememberPreferenceState

@Composable
fun SettingsScreen(
    viewModel: HomeViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    var isSmartExpanded by rememberPreferenceState(FraisData.SETTINGS_SMART_EXPANDED, false)
    var isFiltersExpanded by rememberPreferenceState(FraisData.SETTINGS_FILTERS_EXPANDED, false)
    var isCoreExpanded by rememberPreferenceState(FraisData.SETTINGS_CORE_EXPANDED, false)
    var isAppearanceExpanded by rememberPreferenceState(FraisData.SETTINGS_APPEARANCE_EXPANDED, false)
    var isDataExpanded by rememberPreferenceState(FraisData.SETTINGS_DATA_EXPANDED, false)
    var expandedFilterId by rememberPreferenceState(FraisData.SETTINGS_EXPANDED_FILTER, -1)
    var filterToDelete by remember { mutableStateOf<FraisData.Tag?>(null) }

    var gridColumns by rememberPreferenceState(FraisData.GRID_COLUMNS, "4")
    var iconSize by rememberPreferenceState(FraisData.ICON_SIZE, "64")
    var showLabels by rememberPreferenceState(FraisData.SHOW_LABELS, true)
    var spacingType by rememberPreferenceState(FraisData.SPACING_TYPE, "comfortable")
    var appTheme by rememberPreferenceState(FraisData.APP_THEME, FraisData.THEME_AMOLED)
    var grainIntensity by rememberPreferenceState(FraisData.GRAIN_INTENSITY, 0.1f)
    var showPulseDot by rememberPreferenceState(FraisData.SHOW_PULSE_DOT, true)
    var smartClassification by rememberPreferenceState(FraisData.SMART_CLASSIFICATION, true)
    var flexibleFilters by rememberPreferenceState(FraisData.FLEXIBLE_FILTERS, true)
    var showNonLaunchable by rememberPreferenceState(FraisData.SHOW_NON_LAUNCHABLE_APPS, false)
    var autoFreezeNotification by rememberPreferenceState(FraisData.AUTO_FREEZE_NOTIFICATION, false)
    var smartMappingLocation by rememberPreferenceState(FraisData.SMART_MAPPING_LOCATION, true)
    var smartMappingData by rememberPreferenceState(FraisData.SMART_MAPPING_DATA, true)

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            autoFreezeNotification = true
            com.khaled.frais.workers.FreezeNotificationWorker.schedule(context)
        } else {
            HUI.showToast("NOTIFICATION PERMISSION DENIED")
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            coroutineScope.launch {
                val success = BackupManager.exportData(context, it)
                if (success) HUI.showToast("DATA EXPORTED") else HUI.showToast("EXPORT FAILED")
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            coroutineScope.launch {
                val success = BackupManager.importData(context, it)
                if (success) {
                    HUI.showToast("DATA IMPORTED")
                    viewModel.refresh(force = true)
                } else {
                    HUI.showToast("IMPORT FAILED")
                }
            }
        }
    }

    val isAnySectionExpanded = isCoreExpanded || isSmartExpanded || isFiltersExpanded || isAppearanceExpanded || isDataExpanded

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(MaterialTheme.colorScheme.background).statusBarsPadding()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SYSTEM CONFIG",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                }
                NothingDivider(modifier = Modifier.padding(horizontal = 16.dp))
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                SettingsSection(
                    title = "CORE",
                    description = "SYSTEM PERMISSIONS & PACKAGES",
                    icon = Icons.Default.Settings,
                    isExpanded = isCoreExpanded,
                    onToggle = { isCoreExpanded = !isCoreExpanded },
                    isAnyExpanded = isAnySectionExpanded
                ) {
                    SettingsItem(
                        title = "SHIZUKU PERMISSION",
                        description = if (uiState.isShizukuPermissionGranted) "AUTHORIZED" else "PERMISSION REQUIRED"
                    ) {
                        if (!uiState.isShizukuPermissionGranted) {
                            Button(
                                onClick = { com.khaled.frais.app.AppManager.requestShizukuPermission(1001) },
                                shape = MaterialTheme.shapes.extraSmall,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text("REQUEST", style = MaterialTheme.typography.labelSmall)
                            }
                        } else {
                            Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    NothingDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    SettingsItem(
                        title = "USAGE METRICS",
                        description = if (uiState.isUsagePermissionGranted) "GRANTED" else "ACCESS REQUIRED FOR TRACKING"
                    ) {
                        if (!uiState.isUsagePermissionGranted) {
                            Button(
                                onClick = { context.startActivity(Intent(android.provider.Settings.ACTION_USAGE_ACCESS_SETTINGS)) },
                                shape = MaterialTheme.shapes.extraSmall,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text("GRANT", style = MaterialTheme.typography.labelSmall)
                            }
                        } else {
                            Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    NothingDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    SettingsItem(
                        title = "NOTIFICATION PERMISSION",
                        description = "REQUIRED FOR SMART ALERTS"
                    ) {
                        val isGranted = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                            context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED
                        } else true

                        if (!isGranted) {
                            Button(
                                onClick = { 
                                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                        notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                },
                                shape = MaterialTheme.shapes.extraSmall,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text("GRANT", style = MaterialTheme.typography.labelSmall)
                            }
                        } else {
                            Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    NothingDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    SettingsItem(
                        title = "NON-LAUNCHABLE PACKAGES",
                        description = "INCLUDE SERVICES AND APPS WITHOUT ICONS"
                    ) {
                        Switch(checked = showNonLaunchable, onCheckedChange = { 
                            showNonLaunchable = it
                            FraisData.showNonLaunchableApps = it
                            viewModel.updateFilteredApps()
                        })
                    }
                    NothingDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    SettingsItem(
                        title = "INACTIVITY NOTIFICATION",
                        description = "NOTIFY TO FREEZE APPS UNUSED FOR 15M"
                    ) {
                        Switch(checked = autoFreezeNotification, onCheckedChange = { 
                            if (it) {
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                    val isGranted = context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED
                                    if (isGranted) {
                                        autoFreezeNotification = true
                                        com.khaled.frais.workers.FreezeNotificationWorker.schedule(context)
                                    } else {
                                        notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                } else {
                                    autoFreezeNotification = true
                                    com.khaled.frais.workers.FreezeNotificationWorker.schedule(context)
                                }
                            } else {
                                autoFreezeNotification = false
                                com.khaled.frais.workers.FreezeNotificationWorker.cancel(context)
                            }
                        })
                    }
                }
            }

            item {
                SettingsSection(
                    title = "SMART FEATURES",
                    description = "INTELLIGENT CATEGORIZATION",
                    icon = Icons.Default.AutoAwesome,
                    isExpanded = isSmartExpanded,
                    onToggle = { isSmartExpanded = !isSmartExpanded },
                    isAnyExpanded = isAnySectionExpanded
                ) {
                    SettingsItem(
                        title = "SMART CLASSIFICATION",
                        description = "AUTOMATICALLY CATEGORIZE NEW APPS"
                    ) {
                        Switch(checked = smartClassification, onCheckedChange = { 
                            smartClassification = it
                            viewModel.toggleSmartClassification(it)
                        })
                    }
                    NothingDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    SettingsItem(
                        title = "AUTO-LOCATION",
                        description = "ENABLE GPS FOR MAPPING APPS"
                    ) {
                        Switch(checked = smartMappingLocation, onCheckedChange = { 
                            smartMappingLocation = it
                            FraisData.smartMappingLocation = it
                        })
                    }
                    NothingDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    SettingsItem(
                        title = "AUTO-MOBILE DATA",
                        description = "ENABLE DATA FOR MAPPING APPS"
                    ) {
                        Switch(checked = smartMappingData, onCheckedChange = { 
                            smartMappingData = it
                            FraisData.smartMappingData = it
                        })
                    }
                    NothingDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    SettingsItem(
                        title = "FLEXIBLE FILTERS",
                        description = "SORT FILTERS BY USAGE PATTERNS"
                    ) {
                        Switch(checked = flexibleFilters, onCheckedChange = { 
                            flexibleFilters = it
                            viewModel.updateFilteredApps()
                        })
                    }
                    NothingDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    SettingsItem(
                        title = "INACTIVITY NOTIFICATION",
                        description = "NOTIFY TO FREEZE APPS UNUSED FOR 15M"
                    ) {
                        Switch(checked = autoFreezeNotification, onCheckedChange = { 
                            if (it) {
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                    val isGranted = context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED
                                    if (isGranted) {
                                        autoFreezeNotification = true
                                        com.khaled.frais.workers.FreezeNotificationWorker.schedule(context)
                                    } else {
                                        notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                } else {
                                    autoFreezeNotification = true
                                    com.khaled.frais.workers.FreezeNotificationWorker.schedule(context)
                                }
                            } else {
                                autoFreezeNotification = false
                                com.khaled.frais.workers.FreezeNotificationWorker.cancel(context)
                            }
                        })
                    }
                }
            }

            item {
                SettingsSection(
                    title = "FILTER MANAGEMENT",
                    description = "CUSTOMIZE APP CATEGORIES",
                    icon = Icons.Default.FilterList,
                    isExpanded = isFiltersExpanded,
                    onToggle = { isFiltersExpanded = !isFiltersExpanded },
                    isAnyExpanded = isAnySectionExpanded
                ) {
                    uiState.filters.forEach { filterWithCount ->
                        val tag = filterWithCount.filter
                        val isExpanded = expandedFilterId == tag.id
                        
                        Column(
                            modifier = Modifier.clickable {
                                expandedFilterId = if (isExpanded) -1 else tag.id
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (isExpanded) {
                                    Box(
                                        modifier = Modifier
                                            .width(2.dp)
                                            .height(24.dp)
                                            .background(MaterialTheme.colorScheme.primary)
                                    )
                                    Spacer(Modifier.width(10.dp))
                                }

                                Text(tag.icon, fontSize = 20.sp)
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(tag.name.uppercase(), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                                    if (tag.isBuiltIn) {
                                        Text("SYSTEM FILTER", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                    }
                                }
                                
                                Switch(
                                    checked = tag.isEnabled,
                                    onCheckedChange = { isEnabled ->
                                        val updatedTag = tag.copy(isEnabled = isEnabled)
                                        FraisData.updateTag(updatedTag)
                                        viewModel.updateFilteredApps()
                                    },
                                    scale = 0.8f
                                )

                                IconButton(onClick = { filterToDelete = tag }) {
                                    Icon(Icons.Default.Delete, null, tint = NothingRed, modifier = Modifier.size(20.dp))
                                }
                            }

                            AnimatedVisibility(visible = isExpanded) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    var localName by remember(tag.id) { mutableStateOf(tag.name) }
                                    var localIcon by remember(tag.id) { mutableStateOf(tag.icon) }

                                    OutlinedTextField(
                                        value = localName,
                                        onValueChange = { 
                                            localName = it
                                            val updated = tag.copy(name = it)
                                            FraisData.updateTag(updated)
                                            viewModel.updateFilteredApps()
                                        },
                                        label = { Text("NAME", style = MaterialTheme.typography.labelSmall) },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = MaterialTheme.shapes.extraSmall,
                                        textStyle = MaterialTheme.typography.bodyMedium
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    OutlinedTextField(
                                        value = localIcon,
                                        onValueChange = { 
                                            localIcon = it
                                            val updated = tag.copy(icon = it)
                                            FraisData.updateTag(updated)
                                            viewModel.updateFilteredApps()
                                        },
                                        label = { Text("ICON", style = MaterialTheme.typography.labelSmall) },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = MaterialTheme.shapes.extraSmall,
                                        textStyle = MaterialTheme.typography.bodyMedium
                                    )
                                    Spacer(Modifier.height(16.dp))

                                    Text("APP MANAGEMENT", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color.Gray)
                                    Spacer(Modifier.height(8.dp))
                                    
                                    var searchQuery by remember(tag.id) { mutableStateOf("") }
                                    var refreshKey by remember(tag.id) { mutableStateOf(0) }
                                    
                                    val appsInFilter = remember(uiState.apps, tag.id, refreshKey) {
                                        uiState.apps.filter { tag.id in it.tagIds }
                                    }
                                    
                                    val searchableApps = remember(uiState.apps, tag.id, searchQuery, refreshKey) {
                                        if (searchQuery.length < 2) emptyList()
                                        else uiState.apps.filter { it.name.contains(searchQuery, ignoreCase = true) && tag.id !in it.tagIds }
                                    }

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
                                        searchableApps.take(5).forEach { app ->
                                            ListItem(
                                                headlineContent = { Text(app.name.uppercase(), style = MaterialTheme.typography.labelSmall) },
                                                leadingContent = { AppIcon(info = app.applicationInfo, size = 24.dp) },
                                                trailingContent = { Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary) },
                                                modifier = Modifier.clickable {
                                                    app.manualTagId = tag.id
                                                    app.excludedTagIds.remove(tag.id)
                                                    FraisData.saveApps()
                                                    refreshKey++
                                                    searchQuery = ""
                                                    viewModel.triggerTransientGlyph(GlyphState.ADDING)
                                                }
                                            )
                                        }
                                    }

                                    if (appsInFilter.isNotEmpty()) {
                                        Spacer(Modifier.height(8.dp))
                                        Text("CURRENT APPS (${appsInFilter.size})", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                        Column {
                                            appsInFilter.take(10).forEach { app ->
                                                Row(
                                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    AppIcon(info = app.applicationInfo, size = 24.dp)
                                                    Spacer(Modifier.width(12.dp))
                                                    Text(app.name.uppercase(), style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                    IconButton(onClick = {
                                                        app.manualTagId = null
                                                        if (tag.id !in app.excludedTagIds) app.excludedTagIds.add(tag.id)
                                                        FraisData.saveApps()
                                                        refreshKey++
                                                        viewModel.triggerTransientGlyph(GlyphState.REMOVING)
                                                    }, modifier = Modifier.size(24.dp)) {
                                                        Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp), tint = NothingRed)
                                                    }
                                                }
                                            }
                                            if (appsInFilter.size > 10) {
                                                Text("AND ${appsInFilter.size - 10} MORE...", style = MaterialTheme.typography.labelSmall, color = Color.Gray, modifier = Modifier.padding(top = 4.dp))
                                            }
                                        }
                                    }
                                }
                            }
                            NothingDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        }
                    }
                }
            }

            item {
                SettingsSection(
                    title = "APPEARANCE",
                    description = "UI & VISUAL PREFERENCES",
                    icon = Icons.Default.Palette,
                    isExpanded = isAppearanceExpanded,
                    onToggle = { isAppearanceExpanded = !isAppearanceExpanded },
                    isAnyExpanded = isAnySectionExpanded
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("THEME", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FraisData.APP_THEME_VALUES.forEach { theme ->
                                val isSelected = appTheme == theme
                                Surface(
                                    onClick = { appTheme = theme },
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                    shape = MaterialTheme.shapes.extraSmall,
                                    border = if (isSelected) null else BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = theme.replace("theme_", "").uppercase(),
                                        modifier = Modifier.padding(vertical = 8.dp),
                                        textAlign = TextAlign.Center,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                    NothingDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("GRID COLUMNS", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("NUMBER OF APPS PER ROW", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            }
                            Text(gridColumns, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        Slider(
                            value = gridColumns.toFloatOrNull() ?: 4f,
                            onValueChange = { gridColumns = it.toInt().toString() },
                            valueRange = 2f..6f,
                            steps = 3
                        )
                    }
                    NothingDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("ICON SIZE", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("SCALE OF APPLICATION ICONS", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            }
                            Text("${iconSize}DP", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        Slider(
                            value = iconSize.toFloatOrNull() ?: 64f,
                            onValueChange = { iconSize = it.toInt().toString() },
                            valueRange = 40f..96f
                        )
                    }
                    NothingDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    SettingsItem(
                        title = "SHOW LABELS",
                        description = "DISPLAY APP NAMES BELOW ICONS"
                    ) {
                        Switch(checked = showLabels, onCheckedChange = { showLabels = it })
                    }
                    NothingDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("SPACING TYPE", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("compact", "comfortable", "spacious").forEach { type ->
                                val isSelected = spacingType == type
                                Surface(
                                    onClick = { spacingType = type },
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                    shape = MaterialTheme.shapes.extraSmall,
                                    border = if (isSelected) null else BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = type.uppercase(),
                                        modifier = Modifier.padding(vertical = 8.dp),
                                        textAlign = TextAlign.Center,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                    NothingDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("GRAIN INTENSITY", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("STRENGTH OF THE NOISE EFFECT", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            }
                            Text("${(grainIntensity * 100).toInt()}%", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        Slider(
                            value = grainIntensity,
                            onValueChange = { grainIntensity = it },
                            valueRange = 0f..0.5f
                        )
                    }
                    NothingDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    SettingsItem(
                        title = "PULSE DOT",
                        description = "SHOW ANIMATED DOT FOR ACTIVE FILTERS"
                    ) {
                        Switch(checked = showPulseDot, onCheckedChange = { showPulseDot = it })
                    }
                }
            }

            item {
                SettingsSection(
                    title = "DATA",
                    description = "BACKUP & RESTORE",
                    icon = Icons.Default.Storage,
                    isExpanded = isDataExpanded,
                    onToggle = { isDataExpanded = !isDataExpanded },
                    isAnyExpanded = isAnySectionExpanded
                ) {
                    SettingsItem(
                        title = "EXPORT DATA",
                        description = "SAVE SETTINGS AND FILTERS TO A FILE"
                    ) {
                        IconButton(onClick = { exportLauncher.launch("Frais_Backup_${System.currentTimeMillis()}.json") }) {
                            Icon(Icons.Default.Upload, null)
                        }
                    }
                    NothingDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    SettingsItem(
                        title = "IMPORT DATA",
                        description = "RESTORE SETTINGS AND FILTERS FROM A FILE"
                    ) {
                        IconButton(onClick = { importLauncher.launch(arrayOf("application/json", "application/octet-stream")) }) {
                            Icon(Icons.Default.Download, null)
                        }
                    }
                }
            }

            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("FRAIS VERSION ${FraisData.VERSION}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    Text("DESIGNED BY KHALED", style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                }
            }
        }
    }

    if (filterToDelete != null) {
        AlertDialog(
            onDismissRequest = { filterToDelete = null },
            shape = MaterialTheme.shapes.extraSmall,
            title = { Text("DELETE FILTER", fontWeight = FontWeight.Bold) },
            text = { Text("ARE YOU SURE YOU WANT TO DELETE '${filterToDelete?.name?.uppercase()}'?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        filterToDelete?.let {
                            FraisData.deleteTag(it.id)
                            viewModel.refresh(force = false)
                        }
                        filterToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = NothingRed)
                ) { Text("DELETE") }
            },
            dismissButton = {
                TextButton(onClick = { filterToDelete = null }) { Text("CANCEL") }
            }
        )
    }
}

@Composable
fun SettingsSection(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    description: String? = null,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    isAnyExpanded: Boolean,
    content: @Composable ColumnScope.() -> Unit
) {
    val alpha by animateFloatAsState(if (isAnyExpanded && !isExpanded) 0.4f else 1f)
    val rotation by animateFloatAsState(if (isExpanded) 180f else 0f)
    val backgroundColor by animateColorAsState(
        if (isExpanded) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
        else Color.Transparent
    )

    NothingCard(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer(alpha = alpha)
    ) {
        Column(modifier = Modifier.background(backgroundColor)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle() }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = if (isExpanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.width(16.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title.uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isExpanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        letterSpacing = 1.sp
                    )
                    if (description != null) {
                        Text(
                            text = description.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray
                        )
                    }
                }
                Icon(
                    imageVector = Icons.Default.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier
                        .size(20.dp)
                        .rotate(rotation),
                    tint = if (isExpanded) MaterialTheme.colorScheme.primary else Color.Gray
                )
            }
            
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column {
                    NothingDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    content()
                }
            }
        }
    }
}

@Composable
fun SettingsItem(
    title: String,
    description: String? = null,
    trailingContent: @Composable () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
            if (description != null) {
                Text(
                    text = description.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
            }
        }
        trailingContent()
    }
}

@Composable
fun Switch(checked: Boolean, onCheckedChange: (Boolean) -> Unit, scale: Float = 1f) {
    androidx.compose.material3.Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = Modifier.scale(scale)
    )
}
