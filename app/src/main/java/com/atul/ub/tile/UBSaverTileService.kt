package com.atul.ub.tile

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.atul.ub.MainActivity
import com.atul.ub.apps.AppRepository
import com.atul.ub.saver.SaverState
import com.atul.ub.saver.SuperSaverEngine

class UBSaverTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    override fun onClick() {
        super.onClick()
        val engine = SuperSaverEngine(this)
        val currentState = engine.currentState

        if (currentState == SaverState.ACTIVE || currentState == SaverState.PARTIAL_FAILURE) {
            engine.restoreState()
        } else {
            val appRepo = AppRepository(this)
            val installedApps = appRepo.getInstalledLaunchableApps()
            val essentialPkgs = appRepo.getEssentialPackages()
            val nonEssentialPkgs = installedApps.map { it.packageName }.filter { !essentialPkgs.contains(it) }
            engine.activateSuperSaver(nonEssentialPkgs)
        }
        updateTileState()

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
                startActivityAndCollapse(pendingIntent)
            } else {
                @Suppress("DEPRECATION")
                startActivityAndCollapse(intent)
            }
        } catch (_: Exception) {}
    }

    private fun updateTileState() {
        val tile = qsTile ?: return
        val engine = SuperSaverEngine(this)
        val state = engine.currentState

        val (tileState, label) = when (state) {
            SaverState.ACTIVE -> Pair(Tile.STATE_ACTIVE, "Super Saver On")
            SaverState.PARTIAL_FAILURE -> Pair(Tile.STATE_ACTIVE, "Super Saver (Partial)")
            SaverState.ACTIVATING -> Pair(Tile.STATE_ACTIVE, "Activating...")
            SaverState.RESTORING -> Pair(Tile.STATE_INACTIVE, "Restoring...")
            SaverState.NORMAL -> Pair(Tile.STATE_INACTIVE, "Super Saver Off")
        }

        tile.state = tileState
        tile.label = "Super Saver"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = label
        }
        tile.updateTile()
    }
}
