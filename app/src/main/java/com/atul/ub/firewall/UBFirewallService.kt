package com.atul.ub.firewall

import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor

class UBFirewallService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == ACTION_STOP) {
            stopVpn()
            return START_NOT_STICKY
        }

        val nonEssentialPkgs = intent?.getStringArrayExtra(EXTRA_NON_ESSENTIAL_PKGS)
        startVpn(nonEssentialPkgs)
        return START_STICKY
    }

    private fun startVpn(nonEssentialPkgs: Array<String>?) {
        stopVpn()
        try {
            val builder = Builder()
                .setSession("UB Local Firewall")
                .addAddress("10.120.0.1", 32)
                .setMtu(1500)

            if (!nonEssentialPkgs.isNullOrEmpty()) {
                for (pkg in nonEssentialPkgs) {
                    try {
                        // Routing non-essential apps into local dummy interface blocks their traffic locally.
                        builder.addAllowedApplication(pkg)
                    } catch (_: Exception) {}
                }
            }

            vpnInterface = builder.establish()
        } catch (_: Exception) {
            stopVpn()
        }
    }

    private fun stopVpn() {
        try {
            vpnInterface?.close()
        } catch (_: Exception) {}
        vpnInterface = null
        stopSelf()
    }

    override fun onDestroy() {
        stopVpn()
        super.onDestroy()
    }

    companion object {
        const val ACTION_START = "com.atul.ub.firewall.START"
        const val ACTION_STOP = "com.atul.ub.firewall.STOP"
        const val EXTRA_NON_ESSENTIAL_PKGS = "non_essential_pkgs"

        fun isPrepared(context: Context): Boolean {
            return VpnService.prepare(context) == null
        }

        fun getPrepareIntent(context: Context): Intent? {
            return VpnService.prepare(context)
        }

        fun startFirewall(context: Context, nonEssentialPkgs: List<String>) {
            if (!isPrepared(context)) return
            val intent = Intent(context, UBFirewallService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_NON_ESSENTIAL_PKGS, nonEssentialPkgs.toTypedArray())
            }
            try {
                context.startService(intent)
            } catch (_: Exception) {}
        }

        fun stopFirewall(context: Context) {
            val intent = Intent(context, UBFirewallService::class.java).apply {
                action = ACTION_STOP
            }
            try {
                context.startService(intent)
            } catch (_: Exception) {}
        }
    }
}
