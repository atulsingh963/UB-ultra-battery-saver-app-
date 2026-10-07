package com.atul.ub.ui

import android.content.Intent
import android.graphics.drawable.Drawable
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.atul.ub.apps.AppInfo
import com.atul.ub.battery.BatteryInfo
import com.atul.ub.data.AutomationSettings
import com.atul.ub.data.BatteryImpactLogEntry
import com.atul.ub.data.SaverProfile
import com.atul.ub.saver.ActionResultType
import com.atul.ub.saver.SaverActionResult
import com.atul.ub.system.SystemCapabilities
import com.atul.ub.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

enum class Screen {
    DASHBOARD,
    ALL_APPS,
    ESSENTIAL_APPS,
    ACTIVATION,
    ACTIVE_SAVER,
    SETTINGS,
    DIAGNOSTICS,
    LOGS,
    SETUP
}

@Composable
fun DashboardScreen(
    batteryInfo: BatteryInfo,
    essentialApps: List<AppInfo>,
    isSaverActive: Boolean,
    onActivateClick: () -> Unit,
    onAllAppsClick: () -> Unit,
    onManageAppsClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onDiagnosticsClick: () -> Unit,
    onLogsClick: () -> Unit,
    onLaunchApp: (String) -> Unit
) {
    val currentTime = remember { SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()) }
    val currentDate = remember { SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(Date()) }

    val remainingHours = (batteryInfo.estimatedRemainingMinutes ?: 0) / 60
    val remainingMins = (batteryInfo.estimatedRemainingMinutes ?: 0) % 60

    val chargingGlowAlpha = if (batteryInfo.isCharging) {
        val infiniteTransition = rememberInfiniteTransition(label = "chargingGlow")
        val alpha by infiniteTransition.animateFloat(
            initialValue = 0.3f,
            targetValue = 0.9f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1100, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "alpha"
        )
        alpha
    } else 0.25f

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onDiagnosticsClick) {
                Icon(Icons.Default.Info, contentDescription = "Diagnostics", tint = TextSecondary)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = currentTime,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Light,
                    fontFamily = FontFamily.Monospace,
                    color = TextPrimary
                )
                Text(
                    text = currentDate,
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
            Row {
                IconButton(onClick = onLogsClick) {
                    Icon(Icons.Default.History, contentDescription = "Logs", tint = TextSecondary)
                }
                IconButton(onClick = onSettingsClick) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings", tint = TextSecondary)
                }
            }
        }

        // Battery Hero Ring
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(210.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            if (batteryInfo.isCharging) Color(0xFFC77DFF).copy(alpha = chargingGlowAlpha)
                            else if (isSaverActive) ActiveGreen.copy(alpha = 0.25f)
                            else PurpleAccent.copy(alpha = 0.25f),
                            Color.Transparent
                        )
                    )
                )
                .border(
                    width = 2.dp,
                    color = if (batteryInfo.isCharging) Color(0xFFC77DFF) else if (isSaverActive) ActiveGreen else PurpleAccent,
                    shape = CircleShape
                )
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${batteryInfo.percentage}%",
                    fontSize = 54.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = if (batteryInfo.isCharging) "Charging (${batteryInfo.status})"
                    else "~${remainingHours}h ${remainingMins}m remaining",
                    fontSize = 13.sp,
                    color = if (isSaverActive) ActiveGreen else PurpleGlow,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Essential Apps Section
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.Start
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ESSENTIAL APPS (${essentialApps.size})",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = TextSecondary
                )
                Row {
                    TextButton(onClick = onManageAppsClick) {
                        Text("Manage", color = PurpleGlow, fontSize = 12.sp)
                    }
                    TextButton(onClick = onAllAppsClick) {
                        Text("All Apps", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (essentialApps.isEmpty()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onManageAppsClick() },
                    color = SurfaceDark
                ) {
                    Text(
                        text = "No essential apps selected. Tap to configure launcher.",
                        modifier = Modifier.padding(16.dp),
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    essentialApps.take(4).forEach { app ->
                        AppIconChip(app = app, onClick = { onLaunchApp(app.packageName) })
                    }
                }
            }
        }

        // Bottom Actions: Super Saver & All Apps Drawer Button
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onActivateClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isSaverActive) ActiveGreen else PurpleAccent
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Bolt,
                    contentDescription = null,
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isSaverActive) "VIEW ACTIVE SAVER" else "ENABLE SUPER SAVER",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            OutlinedButton(
                onClick = onAllAppsClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(25.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                border = ButtonDefaults.outlinedButtonBorder.copy(brush = Brush.horizontalGradient(listOf(SurfaceVariantDark, SurfaceDark)))
            ) {
                Icon(Icons.Default.Apps, contentDescription = null, tint = TextSecondary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("ALL APPS DRAWER", fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            }
        }
    }
}

@Composable
fun AllAppsScreen(
    allApps: List<AppInfo>,
    recentApps: List<AppInfo>,
    selectedPackages: Set<String>,
    onToggleApp: (String) -> Unit,
    onLaunchApp: (String) -> Unit,
    onBack: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredApps = remember(allApps, searchQuery) {
        if (searchQuery.isEmpty()) allApps
        else allApps.filter { it.label.contains(searchQuery, ignoreCase = true) || it.packageName.contains(searchQuery, ignoreCase = true) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
            }
            Text(
                text = "All Applications (${allApps.size})",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            placeholder = { Text("Search installed apps...", color = TextSecondary) },
            singleLine = true,
            shape = RoundedCornerShape(24.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = SurfaceDark,
                unfocusedContainerColor = SurfaceDark,
                focusedBorderColor = PurpleAccent,
                unfocusedBorderColor = Color.Transparent,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary) }
        )

        // Recent Apps Row if available
        if (searchQuery.isEmpty() && recentApps.isNotEmpty()) {
            Text(
                text = "RECENTLY LAUNCHED",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = TextSecondary,
                modifier = Modifier.padding(vertical = 6.dp)
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(bottom = 10.dp)
            ) {
                items(recentApps, key = { "recent_" + it.packageName }) { app ->
                    AppIconChip(app = app, onClick = { onLaunchApp(app.packageName) })
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filteredApps, key = { it.packageName }) { app ->
                val isSelected = selectedPackages.contains(app.packageName)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onLaunchApp(app.packageName) },
                    color = SurfaceDark
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        DrawableImage(drawable = app.icon, modifier = Modifier.size(40.dp))
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = app.label, fontSize = 16.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
                            Text(text = app.packageName, fontSize = 11.sp, color = TextSecondary)
                        }
                        IconButton(onClick = { onToggleApp(app.packageName) }) {
                            Icon(
                                imageVector = if (isSelected) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = "Toggle Essential",
                                tint = if (isSelected) PurpleGlow else TextSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EssentialAppsScreen(
    allApps: List<AppInfo>,
    selectedPackages: Set<String>,
    onToggleApp: (String) -> Unit,
    onBack: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredApps = remember(allApps, searchQuery) {
        if (searchQuery.isEmpty()) allApps
        else allApps.filter { it.label.contains(searchQuery, ignoreCase = true) || it.packageName.contains(searchQuery, ignoreCase = true) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
            }
            Text(
                text = "Configure Essential Apps",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            placeholder = { Text("Search apps...", color = TextSecondary) },
            singleLine = true,
            shape = RoundedCornerShape(24.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = SurfaceDark,
                unfocusedContainerColor = SurfaceDark,
                focusedBorderColor = PurpleAccent,
                unfocusedBorderColor = Color.Transparent,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary) }
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filteredApps, key = { it.packageName }) { app ->
                val isSelected = selectedPackages.contains(app.packageName)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onToggleApp(app.packageName) },
                    color = SurfaceDark
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        DrawableImage(drawable = app.icon, modifier = Modifier.size(40.dp))
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = app.label, fontSize = 16.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
                            Text(text = app.packageName, fontSize = 11.sp, color = TextSecondary)
                        }
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = { onToggleApp(app.packageName) },
                            colors = CheckboxDefaults.colors(checkedColor = PurpleAccent)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(
    settings: AutomationSettings,
    onSaveSettings: (AutomationSettings) -> Unit,
    onBack: () -> Unit
) {
    var autoSaver by remember { mutableStateOf(settings.isAutoSaverEnabled) }
    var triggerThreshold by remember { mutableFloatStateOf(settings.triggerThreshold.toFloat()) }
    var autoRestore by remember { mutableStateOf(settings.isAutoRestoreOnCharging) }
    var activeProfile by remember { mutableStateOf(settings.activeProfile) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = {
                onSaveSettings(
                    settings.copy(
                        isAutoSaverEnabled = autoSaver,
                        triggerThreshold = triggerThreshold.toInt(),
                        isAutoRestoreOnCharging = autoRestore,
                        activeProfile = activeProfile
                    )
                )
                onBack()
            }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
            }
            Text(
                text = "Automation & Settings",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp)),
            color = SurfaceDark
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Automatic Super Saver", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                        Text("Triggers when battery reaches threshold", color = TextSecondary, fontSize = 12.sp)
                    }
                    Switch(
                        checked = autoSaver,
                        onCheckedChange = { autoSaver = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = PurpleAccent)
                    )
                }

                if (autoSaver) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Trigger Threshold: ${triggerThreshold.toInt()}%", color = TextPrimary, fontSize = 13.sp)
                    Slider(
                        value = triggerThreshold,
                        onValueChange = { triggerThreshold = it },
                        valueRange = 5f..50f,
                        steps = 8,
                        colors = SliderDefaults.colors(thumbColor = PurpleGlow, activeTrackColor = PurpleAccent)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp)),
            color = SurfaceDark
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Auto Restore when Charging", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                    Text("Exit Super Saver upon charger connection", color = TextSecondary, fontSize = 12.sp)
                }
                Switch(
                    checked = autoRestore,
                    onCheckedChange = { autoRestore = it },
                    colors = SwitchDefaults.colors(checkedThumbColor = PurpleAccent)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("Saver Profile", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SaverProfile.values().forEach { profile ->
                val isSelected = activeProfile == profile
                FilterChip(
                    selected = isSelected,
                    onClick = { activeProfile = profile },
                    label = { Text(profile.name) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PurpleAccent,
                        selectedLabelColor = Color.White
                    )
                )
            }
        }
    }
}

@Composable
fun DiagnosticsScreen(
    capabilities: SystemCapabilities,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
            }
            Text(
                text = "UB Diagnostics",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                DiagCard(title = "App Version", value = "UB Battery Saver Launcher v1.1.0")
            }
            item {
                DiagCard(title = "Android Version", value = "Android ${android.os.Build.VERSION.RELEASE} (API ${android.os.Build.VERSION.SDK_INT})")
            }
            item {
                DiagCard(title = "Device Model", value = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}")
            }
            item {
                DiagCard(
                    title = "Write System Settings",
                    value = if (capabilities.hasWriteSettings) "✓ Granted (Brightness/Timeout Active)" else "⚠ Permission Required"
                )
            }
            item {
                DiagCard(
                    title = "Local VPN Firewall",
                    value = if (capabilities.hasVpnPermission) "✓ Granted (Per-App Data Blocking Ready)" else "⚠ Consent Required"
                )
            }
            item {
                DiagCard(
                    title = "Notification Manager",
                    value = if (capabilities.hasNotificationAccess) "✓ Granted (Listener Active)" else "⚠ Permission Required"
                )
            }
            item {
                DiagCard(
                    title = "Action Capabilities",
                    value = "${capabilities.availableActionCount} Available | ${capabilities.restrictedActionCount} Restricted (Permission Needed)"
                )
            }
        }
    }
}

@Composable
fun DiagCard(title: String, value: String) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp)),
        color = SurfaceDark
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, fontSize = 12.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, fontSize = 15.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun LogsScreen(
    logs: List<BatteryImpactLogEntry>,
    onBack: () -> Unit
) {
    val sdf = remember { SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
            }
            Text(
                text = "Battery Impact Log",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (logs.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No session logs recorded yet.", color = TextSecondary)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(logs) { entry ->
                    val durationMins = (entry.endTime - entry.startTime) / (1000 * 60)
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp)),
                        color = SurfaceDark
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = sdf.format(Date(entry.startTime)),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PurpleGlow
                                )
                                Text(
                                    text = "Duration: ${durationMins}m",
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Battery: ${entry.startPercentage}% → ${entry.endPercentage}%",
                                fontSize = 14.sp,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Actions: ${entry.successfulActionsCount} success | ${entry.skippedActionsCount} skipped | ${entry.unavailableActionsCount} unavailable",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ActivationScreen(
    batteryInfo: BatteryInfo,
    actionResults: List<SaverActionResult>,
    onComplete: () -> Unit
) {
    val context = LocalContext.current
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(2000)
        onComplete()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = 24.dp)) {
            Text(
                text = "ACTIVATING SUPER SAVER",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                color = PurpleGlow
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Executing capability sequence...",
                fontSize = 13.sp,
                color = TextSecondary
            )
        }

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(180.dp)
                .scale(scale)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(PurpleAccent.copy(alpha = 0.4f), Color.Transparent)
                    )
                )
                .border(3.dp, PurpleAccent, CircleShape)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(140.dp),
                color = PurpleGlow,
                strokeWidth = 4.dp
            )
            Text(
                text = "${batteryInfo.percentage}%",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
                .padding(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(actionResults) { action ->
                val statusColor = when (action.resultType) {
                    ActionResultType.SUCCESS -> ActiveGreen
                    ActionResultType.SKIPPED -> TextSecondary
                    ActionResultType.REQUIRES_PERMISSION -> Color(0xFFFFB703)
                    ActionResultType.UNAVAILABLE -> Color(0xFFFB8500)
                    ActionResultType.FAILED -> Color(0xFFE63946)
                }
                val statusPrefix = when (action.resultType) {
                    ActionResultType.SUCCESS -> "✓ "
                    ActionResultType.SKIPPED -> "— "
                    ActionResultType.REQUIRES_PERMISSION -> "⚠ "
                    ActionResultType.UNAVAILABLE -> "🔒 "
                    ActionResultType.FAILED -> "✗ "
                }

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(enabled = action.settingsActionIntent != null) {
                            action.settingsActionIntent?.let { intentAction ->
                                val intent = Intent(intentAction)
                                try { context.startActivity(intent) } catch (_: Exception) {}
                            }
                        },
                    color = SurfaceDark
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = statusPrefix + action.actionName,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            Text(text = action.description, fontSize = 12.sp, color = TextSecondary)
                        }
                        Text(
                            text = action.message,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = statusColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ActiveSaverScreen(
    batteryInfo: BatteryInfo,
    essentialApps: List<AppInfo>,
    onAllAppsClick: () -> Unit,
    onLaunchApp: (String) -> Unit,
    onExit: () -> Unit
) {
    val remainingHours = (batteryInfo.estimatedRemainingMinutes ?: 0) / 60
    val remainingMins = (batteryInfo.estimatedRemainingMinutes ?: 0) % 60

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(ActiveGreen)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "SUPER SAVER ACTIVE",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                    color = ActiveGreen
                )
            }
            Text(
                text = "${batteryInfo.percentage}%",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "~${remainingHours}h ${remainingMins}m",
                fontSize = 56.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = "ESTIMATED REMAINING",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = TextSecondary
            )
        }

        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ALLOWED ESSENTIAL APPS",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = TextSecondary
                )
                TextButton(onClick = onAllAppsClick) {
                    Text("All Apps", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(essentialApps) { app ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { onLaunchApp(app.packageName) }
                    ) {
                        DrawableImage(drawable = app.icon, modifier = Modifier.size(52.dp))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = app.label,
                            fontSize = 11.sp,
                            color = TextPrimary,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        Button(
            onClick = onExit,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(containerColor = SurfaceVariantDark)
        ) {
            Text(
                text = "EXIT SUPER SAVER & RESTORE",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                letterSpacing = 1.sp
            )
        }
    }
}

@Composable
fun AppIconChip(app: AppInfo, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() },
        color = SurfaceDark
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DrawableImage(drawable = app.icon, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = app.label, fontSize = 12.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun DrawableImage(drawable: Drawable?, modifier: Modifier = Modifier) {
    if (drawable != null) {
        val bitmap = remember(drawable) {
            drawable.toBitmap(width = 96, height = 96)
        }
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = null,
            modifier = modifier
        )
    } else {
        Box(
            modifier = modifier
                .clip(CircleShape)
                .background(SurfaceVariantDark),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Android, contentDescription = null, tint = TextSecondary)
        }
    }
}

@Composable
fun SetupScreen(
    capabilities: SystemCapabilities,
    onRequestWriteSettings: () -> Unit,
    onRequestVpn: () -> Unit,
    onRequestNotificationAccess: () -> Unit,
    onCompleteSetup: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.BatterySaver, contentDescription = null, tint = PurpleAccent, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text(text = "UB Setup & Capabilities", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "Standalone offline battery-saving launcher setup", fontSize = 13.sp, color = TextSecondary)

        Spacer(modifier = Modifier.height(20.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(text = "READY CAPABILITIES", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PurpleAccent)
            }
            item {
                SetupItem(title = "Battery Monitoring", subtitle = "Real-time event-driven battery stats", isReady = true)
            }
            item {
                SetupItem(title = "Launcher & App Discovery", subtitle = "Dynamic installed app search & launch", isReady = true)
            }
            item {
                SetupItem(title = "Essential Apps Filter", subtitle = "Persisted local essential app launcher", isReady = true)
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "OPTIONAL USER PERMISSIONS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
            }
            item {
                SetupPermissionItem(
                    title = "Modify System Settings",
                    description = "Required for automatic screen brightness & timeout reduction during Super Saver.",
                    isGranted = capabilities.hasWriteSettings,
                    onRequest = onRequestWriteSettings
                )
            }
            item {
                SetupPermissionItem(
                    title = "Local Per-App Firewall (VPN)",
                    description = "Blocks network access for non-essential apps locally on-device. No cloud or external server used.",
                    isGranted = capabilities.hasVpnPermission,
                    onRequest = onRequestVpn
                )
            }
            item {
                SetupPermissionItem(
                    title = "Notification Manager Access",
                    description = "Allows observing and managing notifications during Super Saver mode.",
                    isGranted = capabilities.hasNotificationAccess,
                    onRequest = onRequestNotificationAccess
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onCompleteSetup,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PurpleAccent),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(text = "Proceed to Launcher", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

@Composable
fun SetupItem(title: String, subtitle: String, isReady: Boolean) {
    Surface(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)),
        color = SurfaceDark
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = PurpleAccent)
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text(text = subtitle, fontSize = 12.sp, color = TextSecondary)
            }
        }
    }
}

@Composable
fun SetupPermissionItem(title: String, description: String, isGranted: Boolean, onRequest: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)),
        color = SurfaceDark
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text(
                    text = if (isGranted) "✓ Granted" else "⚠ Required",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isGranted) PurpleAccent else Color(0xFFFFB74D)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = description, fontSize = 12.sp, color = TextSecondary)
            if (!isGranted) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onRequest,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, PurpleAccent)
                ) {
                    Text(text = "Grant Permission", fontSize = 12.sp, color = PurpleAccent)
                }
            }
        }
    }
}
