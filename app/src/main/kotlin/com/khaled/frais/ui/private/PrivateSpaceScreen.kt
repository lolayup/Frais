package com.khaled.frais.ui.private

import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import com.khaled.frais.R
import com.khaled.frais.app.AppInfo
import com.khaled.frais.ui.components.*
import com.khaled.frais.ui.home.AppItem
import com.khaled.frais.ui.home.AppOptionsDialog
import com.khaled.frais.ui.home.HomeViewModel
import com.khaled.frais.ui.theme.NothingRed
import com.khaled.frais.utils.HUI
import com.khaled.frais.utils.HPackages
import android.provider.Settings
import me.zhanghai.compose.preference.rememberPreferenceState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivateSpaceScreen(
    viewModel: HomeViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val isAuthenticated by viewModel.isPrivateSpaceAuthenticated.collectAsState()

    val gridColumnsPref by rememberPreferenceState("grid_columns_f", "4")
    val iconSizePref by rememberPreferenceState("icon_size_f", "64")
    val showLabelsPref by rememberPreferenceState("show_labels", true)

    if (!isAuthenticated) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Lock, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.secondary)
                Spacer(Modifier.height(24.dp))
                Text(
                    text = "PRIVATE SPACE", 
                    style = MaterialTheme.typography.headlineSmall, 
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "AUTHENTICATE TO ACCESS PROTECTED APPS", 
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
                Spacer(Modifier.height(32.dp))
                Button(
                    onClick = {
                        val activity = context as? FragmentActivity ?: return@Button
                        val executor = androidx.core.content.ContextCompat.getMainExecutor(activity)
                        val biometricPrompt = BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
                            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                                viewModel.setPrivateSpaceAuthenticated(true)
                            }
                            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                                HUI.showToast(errString.toString())
                            }
                        })
                        val promptInfo = BiometricPrompt.PromptInfo.Builder()
                            .setTitle("Private Space")
                            .setSubtitle("Authenticate to enter")
                            .setAllowedAuthenticators(androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG or androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                            .build()
                        biometricPrompt.authenticate(promptInfo)
                    },
                    shape = MaterialTheme.shapes.extraSmall,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text("ENTER SECURE AREA")
                }
            }
        }
    } else {
        var showAddAppPopup by remember { mutableStateOf(false) }

        Scaffold(
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { showAddAppPopup = true },
                    modifier = Modifier.padding(bottom = 80.dp),
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = MaterialTheme.shapes.extraSmall
                ) {
                    Icon(Icons.Default.Add, "Add to Secure Area")
                }
            }
        ) { paddingValues ->
            if (showAddAppPopup) {
                MoveToPrivatePopup(
                    viewModel = viewModel,
                    onClose = { showAddAppPopup = false }
                )
            }
            if (uiState.privateApps.isEmpty()) {
                Box(modifier = Modifier.padding(paddingValues).fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "NO PRIVATE APPS", 
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "LONG PRESS APPS ON HOME TO PROTECT THEM", 
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f)
                        )
                    }
                }
            } else {
                val gridColumns = gridColumnsPref.toIntOrNull() ?: 4
                val iconSize = (iconSizePref.toFloatOrNull() ?: 64f).dp
                val showLabels = showLabelsPref

                LazyVerticalGrid(
                    columns = GridCells.Fixed(gridColumns),
                    contentPadding = PaddingValues(8.dp),
                    modifier = Modifier.padding(paddingValues).fillMaxSize()
                ) {
                    items(uiState.privateApps, key = { it.packageName }) { app ->
                        var showOptions by remember { mutableStateOf(false) }
                        
                        AppItem(
                            app = app,
                            iconSize = iconSize,
                            showLabel = showLabels,
                            onClick = { viewModel.launchApp(app.packageName, context) },
                            onLongClick = { showOptions = true },
                            isGlyphActive = uiState.actionableAppsCount > 0 || uiState.actionablePrivateAppsCount > 0
                        )

                        if (showOptions) {
                            AppOptionsDialog(
                                app = app,
                                viewModel = viewModel,
                                onDismiss = { showOptions = false },
                                onUpdate = { viewModel.updateFilteredApps() },
                                onFreezeToggle = { appInfo, frozen ->
                                    showOptions = false
                                    viewModel.setAppFrozen(appInfo, frozen) { success ->
                                        HUI.showToast(if (success) (if (frozen) "FROZEN ${appInfo.name}" else "UNFROZEN ${appInfo.name}") else "FAILED TO ${if (frozen) "FREEZE" else "UNFREEZE"} ${appInfo.name}")
                                    }
                                },
                                onDetails = { appInfo ->
                                    HUI.startActivity(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, HPackages.packageUri(appInfo.packageName))
                                    showOptions = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MoveToPrivatePopup(
    viewModel: HomeViewModel,
    onClose: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }

    val availableApps by remember(uiState.allApps, searchQuery) {
        derivedStateOf {
            uiState.allApps.filter { app ->
                !app.isPrivate && 
                (app.name.contains(searchQuery, ignoreCase = true) || 
                 app.packageName.contains(searchQuery, ignoreCase = true))
            }.sortedBy { it.name.lowercase() }
        }
    }

    AlertDialog(
        onDismissRequest = onClose,
        title = {
            Column {
                Text("ADD TO SECURED AREA", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("SEARCH APPS...") },
                    singleLine = true,
                    shape = MaterialTheme.shapes.extraSmall
                )
            }
        },
        text = {
            Box(modifier = Modifier.heightIn(max = 400.dp)) {
                if (availableApps.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("NO APPS FOUND", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                    }
                } else {
                    LazyColumn {
                        items(availableApps) { app ->
                            AppItem(
                                app = app,
                                iconSize = 32.dp,
                                showLabel = true,
                                onClick = {
                                    app.isPrivate = true
                                    viewModel.updateFilteredApps()
                                    HUI.showToast("${app.name} moved to Secure Area")
                                    // Keep open for more? Or close?
                                    // Let's close for clarity
                                    onClose()
                                },
                                onLongClick = {},
                                isGlyphActive = uiState.actionableAppsCount > 0 || uiState.actionablePrivateAppsCount > 0
                            )
                            NothingDivider()
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onClose) { Text("CLOSE") }
        },
        shape = MaterialTheme.shapes.extraSmall
    )
}
