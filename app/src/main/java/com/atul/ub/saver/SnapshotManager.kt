package com.atul.ub.saver

import android.content.Context
import android.os.PowerManager
import android.provider.Settings

data class SaverSnapshot(
    val timestamp: Long = System.currentTimeMillis(),
    val screenBrightnessMode: Int? = null,
    val screenBrightness: Int? = null,
    val screenTimeout: Int? = null,
    val bluetoothState: Boolean? = null,
    val wifiState: Boolean? = null,
    val isPowerSaveActive: Boolean? = null
)

class SnapshotManager(private val context: Context) {

    private val prefs = context.getSharedPreferences("ub_saver_snapshot", Context.MODE_PRIVATE)
    private val powerManager by lazy { context.getSystemService(Context.POWER_SERVICE) as? PowerManager }

    @Volatile private var cachedSnapshot: SaverSnapshot? = null
    @Volatile private var isSnapshotLoaded: Boolean = false

    fun createSnapshot(): SaverSnapshot {
        val cr = context.contentResolver

        val brightnessMode = try {
            Settings.System.getInt(cr, Settings.System.SCREEN_BRIGHTNESS_MODE)
        } catch (_: Exception) { null }

        val brightness = try {
            Settings.System.getInt(cr, Settings.System.SCREEN_BRIGHTNESS)
        } catch (_: Exception) { null }

        val timeout = try {
            Settings.System.getInt(cr, Settings.System.SCREEN_OFF_TIMEOUT)
        } catch (_: Exception) { null }

        val isPowerSave = powerManager?.isPowerSaveMode

        val snapshot = SaverSnapshot(
            screenBrightnessMode = brightnessMode,
            screenBrightness = brightness,
            screenTimeout = timeout,
            isPowerSaveActive = isPowerSave
        )

        saveSnapshotLocally(snapshot)
        return snapshot
    }

    private fun saveSnapshotLocally(snapshot: SaverSnapshot) {
        cachedSnapshot = snapshot
        isSnapshotLoaded = true
        prefs.edit().apply {
            putLong("timestamp", snapshot.timestamp)
            snapshot.screenBrightnessMode?.let { putInt("brightness_mode", it) } ?: remove("brightness_mode")
            snapshot.screenBrightness?.let { putInt("brightness", it) } ?: remove("brightness")
            snapshot.screenTimeout?.let { putInt("timeout", it) } ?: remove("timeout")
            snapshot.isPowerSaveActive?.let { putBoolean("power_save", it) } ?: remove("power_save")
            apply()
        }
    }

    fun getPersistedSnapshot(): SaverSnapshot? {
        if (isSnapshotLoaded) return cachedSnapshot
        if (!prefs.contains("timestamp")) {
            cachedSnapshot = null
            isSnapshotLoaded = true
            return null
        }
        val snapshot = SaverSnapshot(
            timestamp = prefs.getLong("timestamp", 0L),
            screenBrightnessMode = if (prefs.contains("brightness_mode")) prefs.getInt("brightness_mode", 0) else null,
            screenBrightness = if (prefs.contains("brightness")) prefs.getInt("brightness", 0) else null,
            screenTimeout = if (prefs.contains("timeout")) prefs.getInt("timeout", 0) else null,
            isPowerSaveActive = if (prefs.contains("power_save")) prefs.getBoolean("power_save", false) else null
        )
        cachedSnapshot = snapshot
        isSnapshotLoaded = true
        return snapshot
    }

    fun clearSnapshot() {
        cachedSnapshot = null
        isSnapshotLoaded = true
        prefs.edit().clear().apply()
    }
}
