package com.khaled.frais.ui.home.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.khaled.frais.app.AppInfo
import com.khaled.frais.ui.components.NothingDivider
import com.khaled.frais.ui.settings.SettingsItem
import com.khaled.frais.utils.HShizuku

@Composable
fun AppConditionsDialog(
    app: AppInfo,
    onDismiss: () -> Unit,
    onUpdate: () -> Unit = {}
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.extraSmall,
        title = { Text("LAUNCH CONDITIONS", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                NothingDivider(modifier = Modifier.padding(vertical = 8.dp))
                SettingsItem(
                    title = "MUTE ON LAUNCH",
                    description = "SILENCE MEDIA VOLUME"
                ) {
                    Switch(checked = app.muteOnLaunch, onCheckedChange = { app.muteOnLaunch = it; onUpdate() })
                }
                NothingDivider()
                SettingsItem(
                    title = "LOCATION ON LAUNCH",
                    description = "AUTO-ENABLE GPS"
                ) {
                    Switch(checked = app.locationOnLaunch, onCheckedChange = { app.locationOnLaunch = it; onUpdate() })
                }
                NothingDivider()
                SettingsItem(
                    title = "DATA ON LAUNCH",
                    description = "AUTO-ENABLE MOBILE DATA"
                ) {
                    Switch(checked = app.dataOnLaunch, onCheckedChange = { app.dataOnLaunch = it; onUpdate() })
                }
                NothingDivider()
                SettingsItem(
                    title = "BATTERY SAVER",
                    description = "TOGGLE LOW POWER MODE"
                ) {
                    Switch(checked = app.batterySaverOnLaunch, onCheckedChange = { app.batterySaverOnLaunch = it; onUpdate() })
                }
                NothingDivider()
                SettingsItem(
                    title = "PREVENT NETWORK",
                    description = "BLOCK INTERNET ACCESS"
                ) {
                    Switch(checked = app.preventNetwork, onCheckedChange = { 
                        app.preventNetwork = it
                        if (!it) {
                            HShizuku.setAppNetworkAllowed(app.packageName, true)
                        }
                        onUpdate() 
                    })
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("DONE") }
        }
    )
}
