package com.atul.ub.apps

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import androidx.core.content.edit
import org.json.JSONArray

data class AppInfo(
    val packageName: String,
    val label: String,
    val icon: Drawable?,
    val isEssential: Boolean = false
)

class AppRepository(private val context: Context) {

    private val prefs = context.getSharedPreferences("ub_essential_apps", Context.MODE_PRIVATE)
    @Volatile private var cachedApps: List<AppInfo>? = null
    @Volatile private var cachedEssentialPackages: Set<String>? = null
    @Volatile private var cachedRecentPackages: List<String>? = null

    fun getEssentialPackages(): Set<String> {
        val cached = cachedEssentialPackages
        if (cached != null) return cached
        val set = prefs.getStringSet("essential_packages", emptySet()) ?: emptySet()
        cachedEssentialPackages = set
        return set
    }

    fun saveEssentialPackages(packages: Set<String>) {
        cachedEssentialPackages = packages
        prefs.edit {
            putStringSet("essential_packages", packages)
        }
        // Update cached app essential status flags
        cachedApps = cachedApps?.map { app ->
            app.copy(isEssential = packages.contains(app.packageName))
        }
    }

    fun getRecentPackages(): List<String> {
        val cached = cachedRecentPackages
        if (cached != null) return cached
        val jsonStr = prefs.getString("recent_packages", "[]") ?: "[]"
        val list = mutableListOf<String>()
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                list.add(array.getString(i))
            }
        } catch (_: Exception) {}
        cachedRecentPackages = list
        return list
    }

    fun recordAppLaunch(packageName: String) {
        val currentRecents = getRecentPackages().filter { it != packageName }.toMutableList()
        currentRecents.add(0, packageName)
        val trimmed = currentRecents.take(8)
        cachedRecentPackages = trimmed

        val array = JSONArray()
        for (pkg in trimmed) {
            array.put(pkg)
        }
        prefs.edit {
            putString("recent_packages", array.toString())
        }
    }

    fun getInstalledLaunchableApps(forceRefresh: Boolean = false): List<AppInfo> {
        if (!forceRefresh && cachedApps != null) {
            return cachedApps!!
        }

        val pm = context.packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos = pm.queryIntentActivities(mainIntent, 0)
        val essentialPkgs = getEssentialPackages()

        val apps = mutableListOf<AppInfo>()
        for (info in resolveInfos) {
            val pkg = info.activityInfo.packageName
            if (pkg == context.packageName) continue // Skip self

            val label = info.loadLabel(pm).toString()
            val icon = try {
                info.loadIcon(pm)
            } catch (_: Exception) {
                null
            }

            apps.add(
                AppInfo(
                    packageName = pkg,
                    label = label,
                    icon = icon,
                    isEssential = essentialPkgs.contains(pkg)
                )
            )
        }
        val result = apps.sortedBy { it.label.lowercase() }
        cachedApps = result
        return result
    }
}
