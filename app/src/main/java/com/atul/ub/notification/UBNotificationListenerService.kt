package com.atul.ub.notification

import android.content.Context
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class UBNotificationListenerService : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        // Minimal, non-blocking notification handling
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
    }

    companion object {
        fun isGranted(context: Context): Boolean {
            val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
            return flat?.contains(context.packageName) == true
        }
    }
}
