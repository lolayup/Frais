package com.khaled.frais.ui.home.components

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.khaled.frais.app.AppManager
import com.khaled.frais.app.FraisData
import com.khaled.frais.ui.components.NothingCard
import com.khaled.frais.ui.home.viewmodel.HomeUiState
import com.khaled.frais.ui.home.viewmodel.HomeViewModel
import com.khaled.frais.ui.theme.NothingRed

@Composable
fun StatusCards(
    uiState: HomeUiState,
    viewModel: HomeViewModel,
    context: Context,
    showUsageWarning: Boolean
) {
    Column {
        AnimatedVisibility(visible = !uiState.isServiceRunning) {
            NothingCard(
                modifier = Modifier.fillMaxWidth().padding(16.dp, 8.dp, 16.dp, 8.dp),
                onClick = { 
                    if (FraisData.workingMode.startsWith(FraisData.SHIZUKU)) {
                        if (!uiState.isShizukuPermissionGranted) {
                            AppManager.requestShizukuPermission(1001)
                        } else {
                            viewModel.refresh(force = false)
                        }
                    } else {
                        viewModel.refresh(force = false)
                    }
                }
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, null, tint = NothingRed, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = if (FraisData.workingMode.startsWith(FraisData.SHIZUKU) && !uiState.isShizukuPermissionGranted) {
                            "SHIZUKU PERMISSION REQUIRED. TAP TO REQUEST."
                        } else {
                            "SERVICE DISCONNECTED. TAP TO RETRY."
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = NothingRed
                    )
                }
            }
        }

        AnimatedVisibility(visible = showUsageWarning) {
            NothingCard(
                modifier = Modifier.fillMaxWidth().padding(16.dp, 8.dp, 16.dp, 8.dp),
                onClick = { context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) }
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Timeline, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("GRANT USAGE ACCESS FOR DATA ANALYSIS.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}
