package com.atul.ub.data

import android.content.Context
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject

data class BatteryImpactLogEntry(
    val id: String = java.util.UUID.randomUUID().toString(),
    val startTime: Long,
    val endTime: Long,
    val startPercentage: Int,
    val endPercentage: Int,
    val profile: String,
    val successfulActionsCount: Int,
    val skippedActionsCount: Int,
    val unavailableActionsCount: Int
)

class BatteryLogManager(private val context: Context) {

    private val prefs = context.getSharedPreferences("ub_battery_logs", Context.MODE_PRIVATE)
    @Volatile private var cachedLogs: List<BatteryImpactLogEntry>? = null

    fun getLogs(): List<BatteryImpactLogEntry> {
        val cached = cachedLogs
        if (cached != null) return cached

        val jsonStr = prefs.getString("logs_array", "[]") ?: "[]"
        val list = mutableListOf<BatteryImpactLogEntry>()
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    BatteryImpactLogEntry(
                        id = obj.optString("id"),
                        startTime = obj.optLong("startTime"),
                        endTime = obj.optLong("endTime"),
                        startPercentage = obj.optInt("startPercentage"),
                        endPercentage = obj.optInt("endPercentage"),
                        profile = obj.optString("profile"),
                        successfulActionsCount = obj.optInt("successfulActionsCount"),
                        skippedActionsCount = obj.optInt("skippedActionsCount"),
                        unavailableActionsCount = obj.optInt("unavailableActionsCount")
                    )
                )
            }
        } catch (_: Exception) {}
        val sorted = list.sortedByDescending { it.startTime }
        cachedLogs = sorted
        return sorted
    }

    fun addLog(entry: BatteryImpactLogEntry) {
        val currentLogs = getLogs().toMutableList()
        currentLogs.add(0, entry)
        // Keep max 20 entries
        val trimmed = currentLogs.take(20)
        cachedLogs = trimmed

        val array = JSONArray()
        for (item in trimmed) {
            val obj = JSONObject().apply {
                put("id", item.id)
                put("startTime", item.startTime)
                put("endTime", item.endTime)
                put("startPercentage", item.startPercentage)
                put("endPercentage", item.endPercentage)
                put("profile", item.profile)
                put("successfulActionsCount", item.successfulActionsCount)
                put("skippedActionsCount", item.skippedActionsCount)
                put("unavailableActionsCount", item.unavailableActionsCount)
            }
            array.put(obj)
        }

        prefs.edit {
            putString("logs_array", array.toString())
        }
    }
}
