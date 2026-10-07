package com.atul.ub.saver

import android.content.Context
import com.atul.ub.saver.actions.*

enum class SaverState {
    NORMAL,
    ACTIVATING,
    ACTIVE,
    PARTIAL_FAILURE,
    RESTORING
}

class SuperSaverEngine(private val context: Context) {

    private val snapshotManager = SnapshotManager(context)
    private val prefs = context.getSharedPreferences("ub_saver_state", Context.MODE_PRIVATE)

    var currentState: SaverState
        get() {
            val name = prefs.getString("state", SaverState.NORMAL.name) ?: SaverState.NORMAL.name
            return try { SaverState.valueOf(name) } catch (_: Exception) { SaverState.NORMAL }
        }
        private set(value) {
            prefs.edit().putString("state", value.name).apply()
        }

    fun getActions(nonEssentialPackages: List<String> = emptyList()): List<SaverAction> {
        return listOf(
            SystemPowerSaveAction(context),
            ReduceBrightnessAction(context),
            ReduceTimeoutAction(context),
            AppLauncherFilterAction(context),
            LocalFirewallSaverAction(context, nonEssentialPackages),
            NotificationSaverAction(context),
            LocationSaverAction(context),
            BluetoothSaverAction(context),
            WifiSaverAction(context)
        )
    }

    fun activateSuperSaver(nonEssentialPackages: List<String> = emptyList()): Pair<SaverState, List<SaverActionResult>> {
        currentState = SaverState.ACTIVATING

        // Take Snapshot
        snapshotManager.createSnapshot()

        val results = mutableListOf<SaverActionResult>()
        var hasFailures = false

        val actions = getActions(nonEssentialPackages)
        for (action in actions) {
            if (action.isAlreadyActive()) {
                results.add(
                    SaverActionResult(
                        actionName = action.name,
                        description = action.description,
                        resultType = ActionResultType.SKIPPED,
                        message = "Already optimized"
                    )
                )
            } else {
                val res = action.execute()
                results.add(res)
                if (res.resultType == ActionResultType.FAILED) {
                    hasFailures = true
                }
            }
        }

        val finalState = if (hasFailures) SaverState.PARTIAL_FAILURE else SaverState.ACTIVE
        currentState = finalState
        return Pair(finalState, results)
    }

    fun restoreState(): Boolean {
        currentState = SaverState.RESTORING
        val snapshot = snapshotManager.getPersistedSnapshot()

        val actions = getActions()
        for (action in actions) {
            val prevVal = when (action.key) {
                "screen_brightness" -> snapshot?.screenBrightness
                "screen_timeout" -> snapshot?.screenTimeout
                "power_save" -> snapshot?.isPowerSaveActive
                else -> null
            }
            action.restore(prevVal)
        }
        if (snapshot != null) {
            snapshotManager.clearSnapshot()
        }

        currentState = SaverState.NORMAL
        return true
    }
}
