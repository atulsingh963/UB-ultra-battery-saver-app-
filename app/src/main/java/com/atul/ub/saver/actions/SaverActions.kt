package com.atul.ub.saver.actions

import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.provider.Settings
import com.atul.ub.firewall.UBFirewallService
import com.atul.ub.notification.UBNotificationListenerService
import com.atul.ub.saver.*

class SystemPowerSaveAction(private val context: Context) : SaverAction {
    override val key: String = "power_save"
    override val name: String = "System Battery Saver"
    override val description: String = "Prompts system power-save mode route"
    override val capability: CapabilityState = CapabilityState.USER_PERMISSION_REQUIRED

    override fun isAlreadyActive(): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        return pm?.isPowerSaveMode ?: false
    }

    override fun execute(): SaverActionResult {
        if (isAlreadyActive()) {
            return SaverActionResult(name, description, ActionResultType.SKIPPED, "Battery Saver already active")
        }

        return SaverActionResult(
            actionName = name,
            description = description,
            resultType = ActionResultType.REQUIRES_PERMISSION,
            message = "Toggle in System Battery Saver Settings",
            settingsActionIntent = Settings.ACTION_BATTERY_SAVER_SETTINGS
        )
    }

    override fun restore(previousValue: Any?): Boolean = true
    override fun getCurrentValue(): Any? {
        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        return pm?.isPowerSaveMode
    }
}

class ReduceBrightnessAction(private val context: Context) : SaverAction {
    override val key: String = "screen_brightness"
    override val name: String = "Screen Brightness"
    override val description: String = "Lowers screen brightness to conserve display power"
    override val capability: CapabilityState
        get() = if (Settings.System.canWrite(context)) CapabilityState.AVAILABLE else CapabilityState.USER_PERMISSION_REQUIRED

    override fun isAlreadyActive(): Boolean {
        val brightness = getCurrentValue() as? Int ?: 255
        return brightness <= 30
    }

    override fun execute(): SaverActionResult {
        if (isAlreadyActive()) {
            return SaverActionResult(name, description, ActionResultType.SKIPPED, "Brightness already low")
        }
        if (!Settings.System.canWrite(context)) {
            return SaverActionResult(
                actionName = name,
                description = description,
                resultType = ActionResultType.REQUIRES_PERMISSION,
                message = "Modify System Settings permission needed",
                settingsActionIntent = Settings.ACTION_MANAGE_WRITE_SETTINGS
            )
        }
        return try {
            Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL)
            Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, 20)
            SaverActionResult(name, description, ActionResultType.SUCCESS, "Reduced brightness to 8%")
        } catch (e: Exception) {
            SaverActionResult(name, description, ActionResultType.FAILED, e.localizedMessage ?: "Failed")
        }
    }

    override fun restore(previousValue: Any?): Boolean {
        val prev = previousValue as? Int ?: return false
        if (!Settings.System.canWrite(context)) return false
        return try {
            Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, prev)
            true
        } catch (_: Exception) { false }
    }

    override fun getCurrentValue(): Any? {
        return try {
            Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS)
        } catch (_: Exception) { null }
    }
}

class ReduceTimeoutAction(private val context: Context) : SaverAction {
    override val key: String = "screen_timeout"
    override val name: String = "Screen Timeout"
    override val description: String = "Sets display timeout to 15 seconds"
    override val capability: CapabilityState
        get() = if (Settings.System.canWrite(context)) CapabilityState.AVAILABLE else CapabilityState.USER_PERMISSION_REQUIRED

    override fun isAlreadyActive(): Boolean {
        val timeout = getCurrentValue() as? Int ?: 60000
        return timeout <= 15000
    }

    override fun execute(): SaverActionResult {
        if (isAlreadyActive()) {
            return SaverActionResult(name, description, ActionResultType.SKIPPED, "Timeout already 15s")
        }
        if (!Settings.System.canWrite(context)) {
            return SaverActionResult(
                actionName = name,
                description = description,
                resultType = ActionResultType.REQUIRES_PERMISSION,
                message = "Modify System Settings permission needed",
                settingsActionIntent = Settings.ACTION_MANAGE_WRITE_SETTINGS
            )
        }
        return try {
            Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_OFF_TIMEOUT, 15000)
            SaverActionResult(name, description, ActionResultType.SUCCESS, "Set timeout to 15s")
        } catch (e: Exception) {
            SaverActionResult(name, description, ActionResultType.FAILED, e.localizedMessage ?: "Failed")
        }
    }

    override fun restore(previousValue: Any?): Boolean {
        val prev = previousValue as? Int ?: return false
        if (!Settings.System.canWrite(context)) return false
        return try {
            Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_OFF_TIMEOUT, prev)
            true
        } catch (_: Exception) { false }
    }

    override fun getCurrentValue(): Any? {
        return try {
            Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_OFF_TIMEOUT)
        } catch (_: Exception) { null }
    }
}

class AppLauncherFilterAction(private val context: Context) : SaverAction {
    override val key: String = "essential_apps_filter"
    override val name: String = "Essential App Launcher Filter"
    override val description: String = "Displays only essential apps during Super Saver UI mode"
    override val capability: CapabilityState = CapabilityState.AVAILABLE

    override fun isAlreadyActive(): Boolean = false
    override fun execute(): SaverActionResult {
        return SaverActionResult(name, description, ActionResultType.SUCCESS, "Essential App Launcher active")
    }
    override fun restore(previousValue: Any?): Boolean = true
    override fun getCurrentValue(): Any? = null
}

class LocalFirewallSaverAction(
    private val context: Context,
    private val nonEssentialPackages: List<String>
) : SaverAction {
    override val key: String = "local_firewall"
    override val name: String = "Local Per-App Firewall"
    override val description: String = "Blocks network access for non-essential apps locally via VpnService"
    override val capability: CapabilityState
        get() = if (UBFirewallService.isPrepared(context)) CapabilityState.AVAILABLE else CapabilityState.USER_PERMISSION_REQUIRED

    override fun isAlreadyActive(): Boolean = false

    override fun execute(): SaverActionResult {
        if (!UBFirewallService.isPrepared(context)) {
            return SaverActionResult(
                actionName = name,
                description = description,
                resultType = ActionResultType.REQUIRES_PERMISSION,
                message = "VPN consent required to block non-essential app traffic locally"
            )
        }
        if (nonEssentialPackages.isEmpty()) {
            return SaverActionResult(name, description, ActionResultType.SKIPPED, "No non-essential apps to block")
        }

        UBFirewallService.startFirewall(context, nonEssentialPackages)
        return SaverActionResult(
            actionName = name,
            description = description,
            resultType = ActionResultType.SUCCESS,
            message = "Blocked network access for ${nonEssentialPackages.size} apps locally"
        )
    }

    override fun restore(previousValue: Any?): Boolean {
        UBFirewallService.stopFirewall(context)
        return true
    }

    override fun getCurrentValue(): Any? = null
}

class NotificationSaverAction(private val context: Context) : SaverAction {
    override val key: String = "notification_management"
    override val name: String = "Notification Manager"
    override val description: String = "Monitors and manages notifications during Super Saver"
    override val capability: CapabilityState
        get() = if (UBNotificationListenerService.isGranted(context)) CapabilityState.AVAILABLE else CapabilityState.USER_PERMISSION_REQUIRED

    override fun isAlreadyActive(): Boolean = false

    override fun execute(): SaverActionResult {
        if (!UBNotificationListenerService.isGranted(context)) {
            return SaverActionResult(
                actionName = name,
                description = description,
                resultType = ActionResultType.REQUIRES_PERMISSION,
                message = "Notification Access required in System Settings",
                settingsActionIntent = "android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"
            )
        }
        return SaverActionResult(name, description, ActionResultType.SUCCESS, "Notification Listener active")
    }

    override fun restore(previousValue: Any?): Boolean = true
    override fun getCurrentValue(): Any? = null
}

class LocationSaverAction(private val context: Context) : SaverAction {
    override val key: String = "location"
    override val name: String = "Location Hardware"
    override val description: String = "Location hardware settings route"
    override val capability: CapabilityState = CapabilityState.USER_PERMISSION_REQUIRED

    override fun isAlreadyActive(): Boolean = false

    override fun execute(): SaverActionResult {
        return SaverActionResult(
            name, description, ActionResultType.REQUIRES_PERMISSION,
            "Toggle in Location Settings",
            settingsActionIntent = Settings.ACTION_LOCATION_SOURCE_SETTINGS
        )
    }

    override fun restore(previousValue: Any?): Boolean = true
    override fun getCurrentValue(): Any? = null
}

class BluetoothSaverAction(private val context: Context) : SaverAction {
    override val key: String = "bluetooth"
    override val name: String = "Bluetooth Radio"
    override val description: String = "Bluetooth settings route"
    override val capability: CapabilityState = CapabilityState.USER_PERMISSION_REQUIRED

    override fun isAlreadyActive(): Boolean = false

    override fun execute(): SaverActionResult {
        return SaverActionResult(
            name, description, ActionResultType.REQUIRES_PERMISSION,
            "Toggle in Bluetooth Settings",
            settingsActionIntent = Settings.ACTION_BLUETOOTH_SETTINGS
        )
    }

    override fun restore(previousValue: Any?): Boolean = true
    override fun getCurrentValue(): Any? = null
}

class WifiSaverAction(private val context: Context) : SaverAction {
    override val key: String = "wifi"
    override val name: String = "Wi-Fi Radio"
    override val description: String = "Wi-Fi settings route"
    override val capability: CapabilityState = CapabilityState.USER_PERMISSION_REQUIRED

    override fun isAlreadyActive(): Boolean = false

    override fun execute(): SaverActionResult {
        return SaverActionResult(
            name, description, ActionResultType.REQUIRES_PERMISSION,
            "Toggle in Wi-Fi Settings",
            settingsActionIntent = Settings.ACTION_WIFI_SETTINGS
        )
    }

    override fun restore(previousValue: Any?): Boolean = true
    override fun getCurrentValue(): Any? = null
}
