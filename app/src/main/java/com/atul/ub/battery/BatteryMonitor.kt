package com.atul.ub.battery

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.PowerManager

data class BatteryInfo(
    val percentage: Int = 0,
    val isCharging: Boolean = false,
    val status: String = "Unknown",
    val health: String = "Unknown",
    val temperatureCelsius: Float = 0f,
    val voltageVolts: Float = 0f,
    val technology: String = "Unknown",
    val isPowerSaveMode: Boolean = false,
    val estimatedRemainingMinutes: Long? = null
)

class BatteryMonitor(private val context: Context) {

    private val batteryManager: BatteryManager by lazy {
        context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
    }

    private val powerManager: PowerManager? by lazy {
        context.getSystemService(Context.POWER_SERVICE) as? PowerManager
    }

    fun getBatteryInfo(broadcastIntent: Intent? = null): BatteryInfo {
        val batteryStatus: Intent? = broadcastIntent ?: run {
            val intentFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            context.registerReceiver(null, intentFilter)
        }

        val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val pct = if (level != -1 && scale > 0) (level * 100 / scale) else 0

        val statusInt = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = statusInt == BatteryManager.BATTERY_STATUS_CHARGING ||
                statusInt == BatteryManager.BATTERY_STATUS_FULL

        val status = when (statusInt) {
            BatteryManager.BATTERY_STATUS_CHARGING -> "Charging"
            BatteryManager.BATTERY_STATUS_DISCHARGING -> "Discharging"
            BatteryManager.BATTERY_STATUS_FULL -> "Full"
            BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "Not Charging"
            else -> "Unknown"
        }

        val healthInt = batteryStatus?.getIntExtra(BatteryManager.EXTRA_HEALTH, -1) ?: -1
        val health = when (healthInt) {
            BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheat"
            BatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over Voltage"
            BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> "Failure"
            BatteryManager.BATTERY_HEALTH_COLD -> "Cold"
            else -> "Good"
        }

        val tempTenths = batteryStatus?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
        val temperature = tempTenths / 10.0f

        val voltageMilliVolts = batteryStatus?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) ?: 0
        val voltage = voltageMilliVolts / 1000.0f

        val technology = batteryStatus?.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY) ?: "Li-ion"

        val isPowerSaveMode = powerManager?.isPowerSaveMode ?: false

        val remainingTimeMinutes: Long? = if (isCharging) {
            val computeTime = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                batteryManager.computeChargeTimeRemaining()
            } else {
                -1L
            }
            if (computeTime > 0) computeTime / (1000 * 60) else ((100 - pct) * 2L)
        } else {
            val multiplier = if (isPowerSaveMode) 15L else 9L
            (pct * multiplier)
        }

        return BatteryInfo(
            percentage = pct,
            isCharging = isCharging,
            status = status,
            health = health,
            temperatureCelsius = temperature,
            voltageVolts = voltage,
            technology = technology,
            isPowerSaveMode = isPowerSaveMode,
            estimatedRemainingMinutes = remainingTimeMinutes
        )
    }

    fun registerBatteryReceiver(onBatteryChanged: (BatteryInfo) -> Unit): BroadcastReceiver {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                onBatteryChanged(getBatteryInfo(intent))
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
            addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
        }
        context.registerReceiver(receiver, filter)
        return receiver
    }

    fun unregisterBatteryReceiver(receiver: BroadcastReceiver) {
        try {
            context.unregisterReceiver(receiver)
        } catch (_: Exception) {}
    }
}
