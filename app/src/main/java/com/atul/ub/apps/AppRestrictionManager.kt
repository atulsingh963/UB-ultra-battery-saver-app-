package com.atul.ub.apps

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import com.atul.ub.firewall.UBFirewallService
import com.atul.ub.saver.ActionResultType
import com.atul.ub.saver.CapabilityState
import com.atul.ub.saver.SaverActionResult

data class AppRestrictionCapability(
    val packageName: String,
    val capability: CapabilityState,
    val canRestrictBackground: Boolean,
    val canRestrictNetwork: Boolean
)

class AppRestrictionManager(private val context: Context) {

    fun getCapability(packageName: String): AppRestrictionCapability {
        val hasVpn = UBFirewallService.isPrepared(context)
        return AppRestrictionCapability(
            packageName = packageName,
            capability = if (hasVpn) CapabilityState.AVAILABLE else CapabilityState.USER_PERMISSION_REQUIRED,
            canRestrictBackground = false,
            canRestrictNetwork = hasVpn
        )
    }

    fun restrictApp(packageName: String): SaverActionResult {
        val settingsIntent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", packageName, null)
        ).action

        return SaverActionResult(
            actionName = "Restrict $packageName",
            description = "Android requires App Info settings for manual background hibernation",
            resultType = ActionResultType.REQUIRES_PERMISSION,
            message = "Settings Action Required",
            settingsActionIntent = settingsIntent
        )
    }
}
