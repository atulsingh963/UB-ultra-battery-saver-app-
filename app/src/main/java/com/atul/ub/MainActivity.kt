package com.atul.ub

import android.content.BroadcastReceiver
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import com.atul.ub.apps.AppInfo
import com.atul.ub.apps.AppRepository
import com.atul.ub.battery.BatteryInfo
import com.atul.ub.battery.BatteryMonitor
import com.atul.ub.data.AutomationManager
import com.atul.ub.data.BatteryImpactLogEntry
import com.atul.ub.data.BatteryLogManager
import com.atul.ub.saver.ActionResultType
import com.atul.ub.saver.SaverActionResult
import com.atul.ub.saver.SaverState
import com.atul.ub.saver.SuperSaverEngine
import com.atul.ub.system.SystemPrivilegeProvider
import com.atul.ub.ui.*
import com.atul.ub.ui.theme.UBTheme

import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts

class MainActivity : ComponentActivity() {

    private lateinit var batteryMonitor: BatteryMonitor
    private lateinit var appRepository: AppRepository
    private lateinit var saverEngine: SuperSaverEngine
    private lateinit var privilegeProvider: SystemPrivilegeProvider
    private lateinit var automationManager: AutomationManager
    private lateinit var batteryLogManager: BatteryLogManager
    private var batteryReceiver: BroadcastReceiver? = null

    private var sessionStartTime: Long = 0L
    private var sessionStartPct: Int = 0

    private var onHomeIntentReceived: (() -> Unit)? = null

    private val vpnLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        batteryMonitor = BatteryMonitor(this)
        appRepository = AppRepository(this)
        saverEngine = SuperSaverEngine(this)
        privilegeProvider = SystemPrivilegeProvider(this)
        automationManager = AutomationManager(this)
        batteryLogManager = BatteryLogManager(this)

        setContent {
            UBTheme {
                val saverState = saverEngine.currentState
                val isFirstLaunch = remember {
                    val prefs = getSharedPreferences("ub_prefs", MODE_PRIVATE)
                    val isFirst = prefs.getBoolean("first_launch", true)
                    if (isFirst) {
                        prefs.edit().putBoolean("first_launch", false).apply()
                    }
                    isFirst
                }

                var currentScreen by remember {
                    mutableStateOf(
                        if (isFirstLaunch) {
                            Screen.SETUP
                        } else if (saverState == SaverState.ACTIVE || saverState == SaverState.PARTIAL_FAILURE) {
                            Screen.ACTIVE_SAVER
                        } else {
                            Screen.DASHBOARD
                        }
                    )
                }

                var batteryInfo by remember { mutableStateOf(batteryMonitor.getBatteryInfo()) }
                var installedApps by remember { mutableStateOf(emptyList<AppInfo>()) }
                var selectedPackages by remember { mutableStateOf(appRepository.getEssentialPackages()) }
                var actionResults by remember { mutableStateOf(emptyList<SaverActionResult>()) }
                var automationSettings by remember { mutableStateOf(automationManager.getSettings()) }
                var logs by remember { mutableStateOf(batteryLogManager.getLogs()) }

                LaunchedEffect(Unit) {
                    installedApps = appRepository.getInstalledLaunchableApps()
                }

                val currentAutomationSettings by rememberUpdatedState(automationSettings)
                val currentInstalledApps by rememberUpdatedState(installedApps)
                val currentSelectedPackages by rememberUpdatedState(selectedPackages)
                val currentActionResults by rememberUpdatedState(actionResults)

                DisposableEffect(Unit) {
                    onHomeIntentReceived = {
                        val isSaverActive = saverEngine.currentState == SaverState.ACTIVE || saverEngine.currentState == SaverState.PARTIAL_FAILURE
                        currentScreen = if (isSaverActive) Screen.ACTIVE_SAVER else Screen.DASHBOARD
                    }

                    batteryReceiver = batteryMonitor.registerBatteryReceiver { updatedInfo ->
                        batteryInfo = updatedInfo

                        // Charging Automation Auto-Restore Check
                        if (updatedInfo.isCharging && currentAutomationSettings.isAutoRestoreOnCharging) {
                            if (saverEngine.currentState == SaverState.ACTIVE || saverEngine.currentState == SaverState.PARTIAL_FAILURE) {
                                recordSessionLog(updatedInfo.percentage, currentActionResults)
                                saverEngine.restoreState()
                                currentScreen = Screen.DASHBOARD
                            }
                        }

                        // Low Battery Trigger Threshold Automation Check
                        if (!updatedInfo.isCharging && currentAutomationSettings.isAutoSaverEnabled) {
                            if (updatedInfo.percentage <= currentAutomationSettings.triggerThreshold) {
                                if (saverEngine.currentState == SaverState.NORMAL) {
                                    sessionStartTime = System.currentTimeMillis()
                                    sessionStartPct = updatedInfo.percentage
                                    val nonEssentialPkgs = currentInstalledApps.map { it.packageName }.filter { !currentSelectedPackages.contains(it) }
                                    val (_, results) = saverEngine.activateSuperSaver(nonEssentialPkgs)
                                    actionResults = results
                                    currentScreen = Screen.ACTIVATION
                                }
                            }
                        }
                    }
                    onDispose {
                        onHomeIntentReceived = null
                        batteryReceiver?.let { batteryMonitor.unregisterBatteryReceiver(it) }
                    }
                }

                val essentialApps = remember(installedApps, selectedPackages) {
                    installedApps.filter { selectedPackages.contains(it.packageName) }
                }

                val recentApps = remember(installedApps) {
                    val recents = appRepository.getRecentPackages()
                    installedApps.filter { recents.contains(it.packageName) }
                        .sortedBy { recents.indexOf(it.packageName) }
                }

                val isSaverActive = saverEngine.currentState == SaverState.ACTIVE || saverEngine.currentState == SaverState.PARTIAL_FAILURE

                val launchApp = remember {
                    { pkg: String ->
                        appRepository.recordAppLaunch(pkg)
                        val launchIntent = packageManager.getLaunchIntentForPackage(pkg)
                        if (launchIntent != null) {
                            try { startActivity(launchIntent) } catch (_: Exception) {}
                        }
                    }
                }

                when (currentScreen) {
                    Screen.DASHBOARD -> {
                        DashboardScreen(
                            batteryInfo = batteryInfo,
                            essentialApps = essentialApps,
                            isSaverActive = isSaverActive,
                            onActivateClick = {
                                if (isSaverActive) {
                                    currentScreen = Screen.ACTIVE_SAVER
                                } else {
                                    sessionStartTime = System.currentTimeMillis()
                                    sessionStartPct = batteryInfo.percentage
                                    val nonEssentialPkgs = installedApps.map { it.packageName }.filter { !selectedPackages.contains(it) }
                                    val (state, results) = saverEngine.activateSuperSaver(nonEssentialPkgs)
                                    actionResults = results
                                    currentScreen = Screen.ACTIVATION
                                }
                            },
                            onAllAppsClick = { currentScreen = Screen.ALL_APPS },
                            onManageAppsClick = { currentScreen = Screen.ESSENTIAL_APPS },
                            onSettingsClick = { currentScreen = Screen.SETTINGS },
                            onDiagnosticsClick = { currentScreen = Screen.DIAGNOSTICS },
                            onLogsClick = { currentScreen = Screen.LOGS },
                            onLaunchApp = launchApp
                        )
                    }
                    Screen.ALL_APPS -> {
                        AllAppsScreen(
                            allApps = installedApps,
                            recentApps = recentApps,
                            selectedPackages = selectedPackages,
                            onToggleApp = { pkg ->
                                val updated = if (selectedPackages.contains(pkg)) {
                                    selectedPackages - pkg
                                } else {
                                    selectedPackages + pkg
                                }
                                selectedPackages = updated
                                appRepository.saveEssentialPackages(updated)
                            },
                            onLaunchApp = launchApp,
                            onBack = { currentScreen = if (isSaverActive) Screen.ACTIVE_SAVER else Screen.DASHBOARD }
                        )
                    }
                    Screen.ESSENTIAL_APPS -> {
                        EssentialAppsScreen(
                            allApps = installedApps,
                            selectedPackages = selectedPackages,
                            onToggleApp = { pkg ->
                                val updated = if (selectedPackages.contains(pkg)) {
                                    selectedPackages - pkg
                                } else {
                                    selectedPackages + pkg
                                }
                                selectedPackages = updated
                                appRepository.saveEssentialPackages(updated)
                            },
                            onBack = { currentScreen = Screen.DASHBOARD }
                        )
                    }
                    Screen.ACTIVATION -> {
                        ActivationScreen(
                            batteryInfo = batteryInfo,
                            actionResults = actionResults,
                            onComplete = { currentScreen = Screen.ACTIVE_SAVER }
                        )
                    }
                    Screen.ACTIVE_SAVER -> {
                        ActiveSaverScreen(
                            batteryInfo = batteryInfo,
                            essentialApps = essentialApps,
                            onAllAppsClick = { currentScreen = Screen.ALL_APPS },
                            onLaunchApp = launchApp,
                            onExit = {
                                recordSessionLog(batteryInfo.percentage, actionResults)
                                saverEngine.restoreState()
                                logs = batteryLogManager.getLogs()
                                currentScreen = Screen.DASHBOARD
                            }
                        )
                    }
                    Screen.SETTINGS -> {
                        SettingsScreen(
                            settings = automationSettings,
                            onSaveSettings = { updated ->
                                automationSettings = updated
                                automationManager.saveSettings(updated)
                            },
                            onBack = { currentScreen = Screen.DASHBOARD }
                        )
                    }
                    Screen.DIAGNOSTICS -> {
                        DiagnosticsScreen(
                            capabilities = privilegeProvider.getSystemCapabilities(),
                            onBack = { currentScreen = Screen.DASHBOARD }
                        )
                    }
                    Screen.LOGS -> {
                        LogsScreen(
                            logs = logs,
                            onBack = { currentScreen = Screen.DASHBOARD }
                        )
                    }
                    Screen.SETUP -> {
                        SetupScreen(
                            capabilities = privilegeProvider.getSystemCapabilities(),
                            onRequestWriteSettings = {
                                val intent = Intent(android.provider.Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                                    data = android.net.Uri.parse("package:$packageName")
                                }
                                try { startActivity(intent) } catch (_: Exception) {}
                            },
                            onRequestVpn = {
                                val vpnIntent = com.atul.ub.firewall.UBFirewallService.getPrepareIntent(this@MainActivity)
                                if (vpnIntent != null) {
                                    try { vpnLauncher.launch(vpnIntent) } catch (_: Exception) {}
                                }
                            },
                            onRequestNotificationAccess = {
                                val intent = Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
                                try { startActivity(intent) } catch (_: Exception) {}
                            },
                            onCompleteSetup = { currentScreen = Screen.DASHBOARD }
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.hasCategory(Intent.CATEGORY_HOME) || intent.action == Intent.ACTION_MAIN) {
            onHomeIntentReceived?.invoke()
        }
    }

    private fun recordSessionLog(endPct: Int, results: List<SaverActionResult>) {
        val successCount = results.count { it.resultType == ActionResultType.SUCCESS }
        val skippedCount = results.count { it.resultType == ActionResultType.SKIPPED }
        val unavailableCount = results.count { it.resultType == ActionResultType.UNAVAILABLE || it.resultType == ActionResultType.REQUIRES_PERMISSION }

        batteryLogManager.addLog(
            BatteryImpactLogEntry(
                startTime = if (sessionStartTime > 0) sessionStartTime else System.currentTimeMillis() - 60000,
                endTime = System.currentTimeMillis(),
                startPercentage = if (sessionStartPct > 0) sessionStartPct else endPct,
                endPercentage = endPct,
                profile = automationManager.getSettings().activeProfile.name,
                successfulActionsCount = successCount,
                skippedActionsCount = skippedCount,
                unavailableActionsCount = unavailableCount
            )
        )
    }
}
