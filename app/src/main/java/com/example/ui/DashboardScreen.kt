package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.RadarStateRepository
import com.example.model.DetectionConfig
import com.example.model.StreakRecord
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    hasOverlayPermission: Boolean,
    hasNotificationPermission: Boolean,
    onRequestOverlayPermission: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onStartService: () -> Unit,
    onStopService: () -> Unit,
    onOpenCropOverlay: () -> Unit,
    onTestAlert: () -> Unit,
    onSaveConfig: (DetectionConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    val isServiceActive by RadarStateRepository.isServiceActive.collectAsState()
    val isDetecting by RadarStateRepository.isDetecting.collectAsState()
    val currentStreak by RadarStateRepository.currentStreak.collectAsState()
    val peakStreak by RadarStateRepository.peakStreak.collectAsState()
    val lastScore by RadarStateRepository.lastDetectedMultiplier.collectAsState()
    val config by RadarStateRepository.config.collectAsState()
    val recentRecords by RadarStateRepository.recentRecords.collectAsState()

    LazyColumn(
        modifier = modifier
            .padding(horizontal = 16.dp)
            .testTag("dashboard_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            // --- 1. HERO STATUS & STREAK RADAR CARD ---
            HeroStreakCard(
                isServiceActive = isServiceActive,
                isDetecting = isDetecting,
                currentStreak = currentStreak,
                targetStreak = config.targetStreak,
                threshold = config.threshold,
                lastScore = lastScore,
                peakStreak = peakStreak,
                onStartService = onStartService,
                onStopService = onStopService,
                onTogglePause = { RadarStateRepository.setDetecting(!isDetecting) },
                onResetStreak = { RadarStateRepository.resetStreak() },
                onOpenCropOverlay = onOpenCropOverlay
            )
        }

        // --- 2. PERMISSIONS STATUS (IF ANY MISSING) ---
        if (!hasOverlayPermission || !hasNotificationPermission) {
            item {
                PermissionWarningCard(
                    hasOverlay = hasOverlayPermission,
                    hasNotification = hasNotificationPermission,
                    onRequestOverlay = onRequestOverlayPermission,
                    onRequestNotification = onRequestNotificationPermission
                )
            }
        }

        // --- 3. CROP AREA & AVIATOR SIMULATOR ---
        item {
            CropSimulatorComponent(
                cropRegion = config.cropRegion,
                onOpenCropSelector = onOpenCropOverlay
            )
        }

        // --- 4. RADAR CONFIGURATION SETTINGS ---
        item {
            SettingsConfigCard(
                config = config,
                onConfigChanged = { newCfg ->
                    RadarStateRepository.updateConfig(newCfg)
                    onSaveConfig(newCfg)
                },
                onTestAlert = onTestAlert
            )
        }

        // --- 5. DETECTED ROUNDS HISTORY HEADER ---
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Rounds (${recentRecords.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                if (recentRecords.isNotEmpty()) {
                    OutlinedButton(
                        onClick = { RadarStateRepository.clearHistory() },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Clear", fontSize = 12.sp)
                    }
                }
            }
        }

        if (recentRecords.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No rounds tracked yet.\nTap 'Start Radar Service' or use simulator buttons above!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(recentRecords, key = { it.id }) { record ->
                HistoryRecordItem(record = record, threshold = config.threshold)
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun HeroStreakCard(
    isServiceActive: Boolean,
    isDetecting: Boolean,
    currentStreak: Int,
    targetStreak: Int,
    threshold: Double,
    lastScore: Double?,
    peakStreak: Int,
    onStartService: () -> Unit,
    onStopService: () -> Unit,
    onTogglePause: () -> Unit,
    onResetStreak: () -> Unit,
    onOpenCropOverlay: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("hero_streak_card"),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF0F172A)
        ),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF00E5FF))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Status Header
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
                            .background(
                                when {
                                    !isServiceActive -> Color(0xFF64748B)
                                    isDetecting -> Color(0xFF00E676)
                                    else -> Color(0xFFFFD600)
                                }
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when {
                            !isServiceActive -> "RADAR INACTIVE"
                            isDetecting -> "MONITORING (10 checks/s)"
                            else -> "RADAR PAUSED"
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            !isServiceActive -> Color(0xFF94A3B8)
                            isDetecting -> Color(0xFF00E676)
                            else -> Color(0xFFFFD600)
                        },
                        letterSpacing = 1.sp
                    )
                }

                Surface(
                    color = Color(0xFF1E293B),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "Peak: ${peakStreak}x",
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Main Streak Display
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = "CONSECUTIVE STREAK",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF94A3B8)
                    )
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "$currentStreak",
                            fontSize = 48.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (currentStreak >= targetStreak) Color(0xFF00E676) else Color(0xFF00E5FF)
                        )
                        Text(
                            text = " / $targetStreak",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF64748B),
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }
                    Text(
                        text = "Scores strictly under < ${String.format("%.2fx", threshold)}",
                        fontSize = 12.sp,
                        color = Color(0xFF38BDF8)
                    )
                }

                // Last Score Badge
                Surface(
                    color = Color(0xFF161F33),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
                    modifier = Modifier.padding(bottom = 4.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "LAST SCORE",
                            fontSize = 10.sp,
                            color = Color(0xFF64748B),
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (lastScore != null) String.format("%.2fx", lastScore) else "--",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (lastScore != null && lastScore < threshold) Color(0xFF00E676) else Color(0xFFFF5252)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Progress bar to target streak
            val progress = (currentStreak.toFloat() / targetStreak.toFloat()).coerceIn(0f, 1f)
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = if (currentStreak >= targetStreak) Color(0xFF00E676) else Color(0xFF00E5FF),
                trackColor = Color(0xFF1E293B),
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (!isServiceActive) {
                    Button(
                        onClick = onStartService,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF00E5FF),
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("start_radar_button")
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Start Radar", fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = onTogglePause,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDetecting) Color(0xFFEF4444) else Color(0xFF00E676),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("pause_resume_button")
                    ) {
                        Text(if (isDetecting) "Pause" else "Resume", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onStopService,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("stop_radar_button")
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = null)
                    }
                }

                OutlinedButton(
                    onClick = onResetStreak,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("reset_streak_button")
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Reset Streak", tint = Color.White)
                }

                Button(
                    onClick = onOpenCropOverlay,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1E293B),
                        contentColor = Color(0xFF00E5FF)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("hero_crop_button")
                ) {
                    Text("✂ Crop")
                }
            }
        }
    }
}

@Composable
private fun PermissionWarningCard(
    hasOverlay: Boolean,
    hasNotification: Boolean,
    onRequestOverlay: () -> Unit,
    onRequestNotification: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().testTag("permission_card"),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF332000)
        ),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFB300))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFFFB300))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Permissions Required (No Root Needed)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFFD54F)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (!hasOverlay) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "• Floating HUD & Crop Overlay",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFFFECB3)
                    )
                    Button(
                        onClick = onRequestOverlay,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB300), contentColor = Color.Black),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Grant Overlay", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
            }

            if (!hasNotification) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "• Notification & Streak Alerts",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFFFECB3)
                    )
                    Button(
                        onClick = onRequestNotification,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB300), contentColor = Color.Black),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Grant Notifications", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsConfigCard(
    config: DetectionConfig,
    onConfigChanged: (DetectionConfig) -> Unit,
    onTestAlert: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().testTag("settings_card"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Radar & Alert Settings",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 1. Multiplier Threshold
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Multiplier Threshold:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "< ${String.format("%.2fx", config.threshold)}",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Slider(
                value = config.threshold.toFloat(),
                onValueChange = { onConfigChanged(config.copy(threshold = (it * 100).toInt() / 100.0)) },
                valueRange = 1.10f..5.00f,
                steps = 38,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.testTag("threshold_slider")
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(1.50, 2.00, 2.50, 3.00).forEach { value ->
                    FilterChip(
                        selected = Math.abs(config.threshold - value) < 0.05,
                        onClick = { onConfigChanged(config.copy(threshold = value)) },
                        label = { Text("${value}x", fontSize = 12.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Consecutive Target Streak
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Consecutive Streak Goal:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${config.targetStreak} rounds",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Slider(
                value = config.targetStreak.toFloat(),
                onValueChange = { onConfigChanged(config.copy(targetStreak = it.toInt())) },
                valueRange = 2f..15f,
                steps = 12,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.testTag("streak_slider")
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(3, 5, 7, 10).forEach { s ->
                    FilterChip(
                        selected = config.targetStreak == s,
                        onClick = { onConfigChanged(config.copy(targetStreak = s)) },
                        label = { Text("$s rounds", fontSize = 12.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3. Checks Per Second Rate
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Detection Frequency:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${config.checksPerSecond} checks/sec",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(5, 10, 15).forEach { rate ->
                    FilterChip(
                        selected = config.checksPerSecond == rate,
                        onClick = { onConfigChanged(config.copy(checksPerSecond = rate)) },
                        label = { Text("$rate / sec", fontSize = 12.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 4. Alert Methods: Vibration & Sound
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Vibration, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Vibrate Phone on Alert", style = MaterialTheme.typography.bodyMedium)
                }
                Switch(
                    checked = config.vibrateOnAlert,
                    onCheckedChange = { onConfigChanged(config.copy(vibrateOnAlert = it)) },
                    modifier = Modifier.testTag("vibrate_switch")
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sound Tone on Alert", style = MaterialTheme.typography.bodyMedium)
                }
                Switch(
                    checked = config.soundOnAlert,
                    onCheckedChange = { onConfigChanged(config.copy(soundOnAlert = it)) },
                    modifier = Modifier.testTag("sound_switch")
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onTestAlert,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("test_alert_button")
            ) {
                Text("⚡ Test Vibration & Sound Alert Now")
            }
        }
    }
}

@Composable
private fun HistoryRecordItem(
    record: StreakRecord,
    threshold: Double
) {
    val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    val formattedTime = timeFormat.format(Date(record.timestamp))

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (record.isUnderThreshold) Color(0xFF00E676) else Color(0xFFFF5252))
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = String.format("%.2fx", record.multiplier),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = if (record.isUnderThreshold) Color(0xFF00E676) else Color(0xFFFF5252)
                    )
                    Text(
                        text = if (record.isUnderThreshold) "Under < ${String.format("%.2fx", threshold)}" else "Streak Reset (>= ${String.format("%.2fx", threshold)})",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Surface(
                    color = if (record.isUnderThreshold) Color(0xFF004D40) else Color(0xFF334155),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "Streak: ${record.streakAfterRound}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (record.isUnderThreshold) Color(0xFF74FF9D) else Color(0xFFCBD5E1),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = formattedTime,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
