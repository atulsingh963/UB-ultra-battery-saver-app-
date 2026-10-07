package com.atul.ub.system

import android.content.Context
import android.provider.Settings
import com.atul.ub.firewall.UBFirewallService
import com.atul.ub.notification.UBNotificationListenerService

data class SystemCapabilities(
    val hasNormalApis: Boolean = true,
    val hasWriteSettings: Boolean = false,
    val hasNotificationAccess: Boolean = false,
    val hasVpnPermission: Boolean = false,
    val availableActionCount: Int = 0,
    val restrictedActionCount: Int = 0,
    val unavailableActionCount: Int = 0
)

class SystemPrivilegeProvider(private val context: Context) {

    fun getSystemCapabilities(): SystemCapabilities {
        val canWriteSettings = Settings.System.canWrite(context)
        val hasNotificationAccess = UBNotificationListenerService.isGranted(context)
        val hasVpnPermission = UBFirewallService.isPrepared(context)

        var available = 2 // Launcher Filter + Battery Saver Route
        if (canWriteSettings) available += 2 // Brightness + Timeout
        if (hasVpnPermission) available += 1 // Local Firewall
        if (hasNotificationAccess) available += 1 // Notification Manager

        var restricted = 0
        if (!canWriteSettings) restricted += 2

        return SystemCapabilities(
            hasNormalApis = true,
            hasWriteSettings = canWriteSettings,
            hasNotificationAccess = hasNotificationAccess,
            hasVpnPermission = hasVpnPermission,
            availableActionCount = available,
            restrictedActionCount = restricted,
            unavailableActionCount = 0
        )
    }
}
