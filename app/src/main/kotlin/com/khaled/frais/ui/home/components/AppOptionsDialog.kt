package com.khaled.frais.ui.home.components

import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.khaled.frais.app.AppInfo
import com.khaled.frais.app.AppManager
import com.khaled.frais.app.FraisData
import com.khaled.frais.ui.components.AppIcon
import com.khaled.frais.ui.components.GlyphState
import com.khaled.frais.ui.components.NothingDivider
import com.khaled.frais.ui.home.viewmodel.HomeViewModel
import com.khaled.frais.ui.theme.NothingRed
import com.khaled.frais.utils.HPackages
import com.khaled.frais.utils.HStorage
import com.khaled.frais.utils.HUI
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AppOptionsDialog(
    app: AppInfo,
    viewModel: HomeViewModel,
    onDismiss: () -> Unit,
    onUpdate: () -> Unit = {},
    onFreezeToggle: (AppInfo, Boolean) -> Unit,
    onDetails: (AppInfo) -> Unit
) {
    val frozen = app.state == AppInfo.State.FROZEN
    var showTagPicker by remember { mutableStateOf(false) }
    var showConditions by remember { mutableStateOf(false) }
    var showUninstallConfirm by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.extraSmall,
        title = { 
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppIcon(
                    info = app.applicationInfo,
                    size = 40.dp,
                    grayscale = frozen,
                    isWhitelisted = app.isWhitelisted
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(app.name.uppercase(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(app.packageName, style = MaterialTheme.typography.labelSmall, fontSize = 10.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        },
        text = {
            Column {
                NothingDivider(modifier = Modifier.padding(vertical = 8.dp))

                // Info Section
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("VER: ${HPackages.getVersionName(app.packageName)} • ${HUI.formatDuration(app.usageTime)}", style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontSize = 9.sp)
                    Text("SIZE: ${HStorage.formatSize(app.storageSize)}", style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontSize = 9.sp)
                }
                
                NothingDivider(modifier = Modifier.padding(vertical = 8.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    maxItemsInEachRow = 2
                ) {
                    val buttonModifier = Modifier.weight(1f).height(48.dp)
                    
                    if (!app.isSystemApp || app.isSafeToFreeze) {
                        CompactOptionButton(
                            icon = if (frozen) Icons.Default.PlayArrow else Icons.Default.AcUnit,
                            label = if (frozen) "UNFREEZE" else "FREEZE",
                            color = if (frozen) MaterialTheme.colorScheme.primary else NothingRed,
                            onClick = { onFreezeToggle(app, !frozen) },
                            modifier = buttonModifier
                        )
                    }

                    if (!app.isPrivate) {
                        CompactOptionButton(
                            icon = Icons.Default.PushPin,
                            label = if (app.pinned) "UNPIN" else "PIN",
                            color = if (app.pinned) MaterialTheme.colorScheme.primary else Color.Gray,
                            onClick = { app.pinned = !app.pinned; onUpdate(); onDismiss() },
                            modifier = buttonModifier
                        )

                        CompactOptionButton(
                            icon = Icons.Default.Shield,
                            label = if (app.whitelisted) "UNTRUST" else "TRUST",
                            color = if (app.whitelisted) MaterialTheme.colorScheme.primary else Color.Gray,
                            onClick = { app.whitelisted = !app.whitelisted; onUpdate(); onDismiss() },
                            modifier = buttonModifier
                        )

                        CompactOptionButton(
                            icon = Icons.Default.FilterList,
                            label = "FILTERS",
                            onClick = { showTagPicker = true },
                            modifier = buttonModifier
                        )
                    }

                    CompactOptionButton(
                        icon = Icons.Default.SettingsSuggest,
                        label = "CONDITIONS",
                        onClick = { showConditions = true },
                        modifier = buttonModifier
                    )

                    CompactOptionButton(
                        icon = if (app.isPrivate) Icons.Default.Visibility else Icons.Default.Lock,
                        label = if (app.isPrivate) "RESTORE" else "SECURE",
                        color = if (app.isPrivate) MaterialTheme.colorScheme.primary else NothingRed,
                        onClick = { app.isPrivate = !app.isPrivate; onUpdate(); onDismiss() },
                        modifier = buttonModifier
                    )

                    CompactOptionButton(
                        icon = if (app.hiddenFromHome) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        label = if (app.hiddenFromHome) "RESTORE" else "REMOVE",
                        color = if (app.hiddenFromHome) MaterialTheme.colorScheme.primary else NothingRed,
                        onClick = { app.hiddenFromHome = !app.hiddenFromHome; onUpdate(); onDismiss() },
                        modifier = buttonModifier
                    )

                    CompactOptionButton(
                        icon = Icons.Default.Info,
                        label = "DETAILS",
                        onClick = { onDetails(app) },
                        modifier = buttonModifier
                    )

                    if (app.isSystemApp) {
                        CompactOptionButton(
                            icon = Icons.Default.VerifiedUser,
                            label = if (app.isSafeToFreeze) "REVOKE" else "SAFE",
                            color = if (app.isSafeToFreeze) NothingRed else MaterialTheme.colorScheme.primary,
                            onClick = { app.isSafeToFreeze = !app.isSafeToFreeze; onUpdate(); onDismiss() },
                            modifier = buttonModifier
                        )
                    }

                    CompactOptionButton(
                        icon = Icons.Default.Refresh,
                        label = "REINSTALL",
                        color = MaterialTheme.colorScheme.primary,
                        onClick = {
                            onDismiss()
                            coroutineScope.launch {
                                val success = AppManager.reinstallAppFallback(app.packageName, context)
                                if (!success) HUI.showToast("REINSTALL FAILED")
                            }
                        },
                        modifier = buttonModifier
                    )

                    CompactOptionButton(
                        icon = Icons.Default.Delete,
                        label = "UNINSTALL",
                        color = NothingRed,
                        onClick = {
                            showUninstallConfirm = true
                        },
                        modifier = buttonModifier
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("CLOSE") }
        }
    )

    if (showUninstallConfirm) {
        AlertDialog(
            onDismissRequest = { showUninstallConfirm = false },
            title = { Text("UNINSTALL", fontWeight = FontWeight.Bold) },
            text = { Text("ARE YOU SURE YOU WANT TO UNINSTALL '${app.name.uppercase()}'?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showUninstallConfirm = false
                        onDismiss()
                        viewModel.triggerTransientGlyph(GlyphState.UNINSTALLING)
                        AppManager.uninstallApp(app.packageName)
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = NothingRed)
                ) { Text("UNINSTALL") }
            },
            dismissButton = {
                TextButton(onClick = { showUninstallConfirm = false }) { Text("CANCEL") }
            },
            shape = MaterialTheme.shapes.extraSmall
        )
    }

    if (showTagPicker) {
        TagPickerDialog(app = app, onDismiss = { showTagPicker = false; onUpdate() })
    }

    if (showConditions) {
        AppConditionsDialog(app = app, onDismiss = { showConditions = false; onUpdate() })
    }
}

@Composable
fun CompactOptionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.extraSmall,
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
        color = Color.Transparent
    ) {
        Column(
            modifier = Modifier.padding(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, null, modifier = Modifier.size(18.dp), tint = color)
            Spacer(Modifier.height(2.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = color)
        }
    }
}
