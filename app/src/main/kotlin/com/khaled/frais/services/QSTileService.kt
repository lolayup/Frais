package com.khaled.frais.services

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import com.khaled.frais.R
import com.khaled.frais.app.FraisApi
import com.khaled.frais.utils.HTarget

@RequiresApi(Build.VERSION_CODES.N)
class QSTileService : TileService() {
    override fun onStartListening() {
        super.onStartListening()
        updateTile()
    }

    override fun onClick() {
        super.onClick()
        // Tile currently does nothing as global actions were removed
        val intent = Intent(FraisApi.ACTION_UNFREEZE_ALL).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        
        if (HTarget.U) {
            startActivityAndCollapse(
                PendingIntent.getActivity(
                    this, 0, intent, PendingIntent.FLAG_IMMUTABLE
                )
            )
        } else {
            @Suppress("DEPRECATION")
            @SuppressLint("StartActivityAndCollapseDeprecated")
            startActivityAndCollapse(intent)
        }
    }

    override fun onTileAdded() {
        updateTile()
    }

    private fun updateTile() {
        qsTile.icon = Icon.createWithResource(this, R.drawable.ic_round_frozen)
        val entries = resources.getStringArray(R.array.tile_action_entries)
        if (entries.isNotEmpty()) {
            qsTile.label = entries[0]
        }
        qsTile.state = Tile.STATE_ACTIVE
        qsTile.updateTile()
    }
}
