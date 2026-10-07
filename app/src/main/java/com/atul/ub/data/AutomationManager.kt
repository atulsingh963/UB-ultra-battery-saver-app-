package com.atul.ub.data

import android.content.Context
import androidx.core.content.edit

enum class SaverProfile {
    LIGHT,
    BALANCED,
    SUPER,
    EMERGENCY
}

data class AutomationSettings(
    val isAutoSaverEnabled: Boolean = false,
    val triggerThreshold: Int = 20,
    val isAutoRestoreOnCharging: Boolean = true,
    val restoreThreshold: Int = 30,
    val activeProfile: SaverProfile = SaverProfile.SUPER
)

class AutomationManager(private val context: Context) {

    private val prefs = context.getSharedPreferences("ub_automation_settings", Context.MODE_PRIVATE)
    @Volatile private var cachedSettings: AutomationSettings? = null

    fun getSettings(): AutomationSettings {
        val cached = cachedSettings
        if (cached != null) return cached

        val profileName = prefs.getString("active_profile", SaverProfile.SUPER.name) ?: SaverProfile.SUPER.name
        val profile = try { SaverProfile.valueOf(profileName) } catch (_: Exception) { SaverProfile.SUPER }

        val settings = AutomationSettings(
            isAutoSaverEnabled = prefs.getBoolean("auto_saver", false),
            triggerThreshold = prefs.getInt("trigger_threshold", 20),
            isAutoRestoreOnCharging = prefs.getBoolean("auto_restore", true),
            restoreThreshold = prefs.getInt("restore_threshold", 30),
            activeProfile = profile
        )
        cachedSettings = settings
        return settings
    }

    fun saveSettings(settings: AutomationSettings) {
        cachedSettings = settings
        prefs.edit {
            putBoolean("auto_saver", settings.isAutoSaverEnabled)
            putInt("trigger_threshold", settings.triggerThreshold)
            putBoolean("auto_restore", settings.isAutoRestoreOnCharging)
            putInt("restore_threshold", settings.restoreThreshold)
            putString("active_profile", settings.activeProfile.name)
        }
    }
}
