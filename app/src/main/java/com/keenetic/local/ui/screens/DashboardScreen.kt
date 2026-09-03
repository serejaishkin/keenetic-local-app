package com.keenetic.local.ui.screens

import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.keenetic.local.api.SystemInfo
import com.keenetic.local.ui.RouterViewModel
import com.keenetic.local.ui.Screen
import com.keenetic.local.ui.theme.KeeneticColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
    viewModel: RouterViewModel,
    onNavigate: (String) -> Unit = {}
) {
    val sysInfo by viewModel.systemInfo.collectAsState()
    val cpuHistory by viewModel.cpuHistory.collectAsState()
    val ramHistory by viewModel.ramHistory.collectAsState()
    val isLivePolling by viewModel.isLivePolling.collectAsState()
    val pollingInterval by viewModel.pollingIntervalSeconds.collectAsState()
    val lastUpdateTimestamp by viewModel.lastTelemetryTimestamp.collectAsState()
    val clients by viewModel.clients.collectAsState()
    val wifiNetworks by viewModel.wifiNetworks.collectAsState()
    val isDemo by viewModel.isDemoMode.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    // Automatically ensure live polling is active when entering dashboard
    LaunchedEffect(Unit) {
        viewModel.startLivePolling()
    }

    // Refresh icon rotation animation
    var isManualRefreshing by remember { mutableStateOf(false) }
    val infiniteTransition = rememberInfiniteTransition(label = "live_polling_indicator")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    val refreshRotation by animateFloatAsState(
        targetValue = if (isManualRefreshing || isLoading) 360f else 0f,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        finishedListener = { isManualRefreshing = false },
        label = "refresh_rotation"
    )

    val timeFormatter = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
    val formattedLastUpdate = remember(lastUpdateTimestamp) {
        if (lastUpdateTimestamp > 0) timeFormatter.format(Date(lastUpdateTimestamp)) else "—"
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Demo Mode Alert
        if (isDemo) {
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = KeeneticColors.Warning.copy(alpha = 0.12f)),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(KeeneticColors.Warning))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = KeeneticColors.Warning)
                            Column {
                                Text(
                                    "Демо-режим с эмуляцией RCI",
                                    color = KeeneticColors.Warning,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "Телеметрия генерируется в реальном времени",
                                    color = KeeneticColors.TextSecondary,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                        Button(
                            onClick = { viewModel.exitDemoMode() },
                            colors = ButtonDefaults.buttonColors(containerColor = KeeneticColors.Warning),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Выйти", color = KeeneticColors.Background, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 2. Header & Live Telemetry Controls
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(KeeneticColors.CardBorder))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Top identity row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                sysInfo?.title ?: "Keenetic Router",
                                style = MaterialTheme.typography.titleLarge,
                                color = KeeneticColors.TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    sysInfo?.model ?: "KN-1811",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = KeeneticColors.Primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text("•", color = KeeneticColors.TextSecondary)
                                Text(
                                    "KeeneticOS ${sysInfo?.osVersion ?: "5.1.1"}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = KeeneticColors.TextSecondary
                                )
                            }
                        }

                        // Live status badge
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (isLivePolling) KeeneticColors.Success.copy(alpha = 0.15f) else KeeneticColors.Warning.copy(alpha = 0.15f),
                            border = CardDefaults.outlinedCardBorder().copy(
                                brush = androidx.compose.ui.graphics.SolidColor(
                                    if (isLivePolling) KeeneticColors.Success.copy(alpha = 0.5f) else KeeneticColors.Warning.copy(alpha = 0.5f)
                                )
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isLivePolling) KeeneticColors.Success.copy(alpha = pulseAlpha)
                                            else KeeneticColors.Warning
                                        )
                                )
                                Text(
                                    text = if (isLivePolling) "LIVE RCI" else "ПАУЗА",
                                    color = if (isLivePolling) KeeneticColors.Success else KeeneticColors.Warning,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = KeeneticColors.Divider)

                    // Polling control bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Play/Pause button
                            FilledTonalIconButton(
                                onClick = { viewModel.toggleLivePolling() },
                                colors = IconButtonDefaults.filledTonalIconButtonColors(
                                    containerColor = if (isLivePolling) KeeneticColors.SurfaceElevated else KeeneticColors.Primary.copy(alpha = 0.2f),
                                    contentColor = if (isLivePolling) KeeneticColors.TextPrimary else KeeneticColors.Primary
                                ),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    if (isLivePolling) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (isLivePolling) "Пауза" else "Запуск",
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // Manual refresh button
                            FilledTonalIconButton(
                                onClick = {
                                    isManualRefreshing = true
                                    viewModel.loadSystemInfo()
                                },
                                colors = IconButtonDefaults.filledTonalIconButtonColors(
                                    containerColor = KeeneticColors.SurfaceElevated,
                                    contentColor = KeeneticColors.Primary
                                ),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    Icons.Default.Refresh,
                                    contentDescription = "Обновить RCI",
                                    modifier = Modifier
                                        .size(18.dp)
                                        .rotate(refreshRotation)
                                )
                            }

                            Column {
                                Text(
                                    "Обновлено: $formattedLastUpdate",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = KeeneticColors.TextSecondary
                                )
                                Text(
                                    "GET /rci/show/system",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = KeeneticColors.TextSecondary.copy(alpha = 0.6f),
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        // Polling intervals
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            listOf(1, 2, 3, 5).forEach { sec ->
                                val selected = pollingInterval == sec
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (selected) KeeneticColors.Primary else KeeneticColors.SurfaceElevated)
                                        .clickable { viewModel.setPollingInterval(sec) }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "${sec}с",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (selected) KeeneticColors.Background else KeeneticColors.TextSecondary,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Live CPU Status Card with Sparkline
        item {
            val currentCpu = sysInfo?.cpuUsagePercent ?: 12
            val cpuColor = when {
                currentCpu > 80 -> KeeneticColors.Error
                currentCpu > 55 -> KeeneticColors.Warning
                else -> KeeneticColors.Success
            }

            val minCpu = cpuHistory.minOrNull() ?: currentCpu
            val maxCpu = cpuHistory.maxOrNull() ?: currentCpu
            val avgCpu = if (cpuHistory.isNotEmpty()) cpuHistory.average().toInt() else currentCpu

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(KeeneticColors.CardBorder))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(KeeneticColors.Primary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Speed, contentDescription = null, tint = KeeneticColors.Primary, modifier = Modifier.size(20.dp))
                            }
                            Column {
                                Text("Процессор (CPU)", style = MaterialTheme.typography.titleMedium, color = KeeneticColors.TextPrimary, fontWeight = FontWeight.Bold)
                                Text("RCI cpuload • ${sysInfo?.cpus ?: 2} ядра (${sysInfo?.arch ?: "aarch64"})", style = MaterialTheme.typography.bodySmall, color = KeeneticColors.TextSecondary)
                            }
                        }

                        // Big percentage badge
                        Text(
                            "$currentCpu%",
                            style = MaterialTheme.typography.headlineMedium,
                            color = cpuColor,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    // Linear progress indicator
                    LinearProgressIndicator(
                        progress = { (currentCpu / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = cpuColor,
                        trackColor = KeeneticColors.SurfaceElevated
                    )

                    // Live Canvas Sparkline
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(KeeneticColors.SurfaceElevated)
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("История нагрузки CPU (RCI show system)", style = MaterialTheme.typography.labelSmall, color = KeeneticColors.TextSecondary)
                            Text("25 измерений", style = MaterialTheme.typography.labelSmall, color = KeeneticColors.TextSecondary)
                        }

                        LiveSparkline(
                            dataPoints = cpuHistory,
                            lineColor = cpuColor,
                            gradientStartColor = cpuColor.copy(alpha = 0.35f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            MetricChip("Мин: $minCpu%", KeeneticColors.Success)
                            MetricChip("Средн: $avgCpu%", KeeneticColors.Primary)
                            MetricChip("Пик: $maxCpu%", if (maxCpu > 70) KeeneticColors.Error else KeeneticColors.Warning)
                        }
                    }
                }
            }
        }

        // 4. Live RAM Status Card with Sparkline
        item {
            val currentRamPercent = sysInfo?.memoryUsagePercent ?: 40
            val totalBytes = sysInfo?.memoryTotal ?: (512L * 1024 * 1024)
            val freeBytes = sysInfo?.memoryFree ?: (256L * 1024 * 1024)
            val usedBytes = (totalBytes - freeBytes).coerceAtLeast(0L)
            val buffers = sysInfo?.memoryBuffers ?: 0L
            val cached = sysInfo?.memoryCached ?: 0L

            val ramColor = when {
                currentRamPercent > 85 -> KeeneticColors.Error
                currentRamPercent > 65 -> KeeneticColors.Warning
                else -> KeeneticColors.Primary
            }

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(KeeneticColors.CardBorder))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(KeeneticColors.Primary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Memory, contentDescription = null, tint = KeeneticColors.Primary, modifier = Modifier.size(20.dp))
                            }
                            Column {
                                Text("Оперативная память (RAM)", style = MaterialTheme.typography.titleMedium, color = KeeneticColors.TextPrimary, fontWeight = FontWeight.Bold)
                                Text("RCI memory • ${formatBytes(usedBytes)} из ${formatBytes(totalBytes)}", style = MaterialTheme.typography.bodySmall, color = KeeneticColors.TextSecondary)
                            }
                        }

                        Text(
                            "$currentRamPercent%",
                            style = MaterialTheme.typography.headlineMedium,
                            color = ramColor,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    // Progress bar
                    LinearProgressIndicator(
                        progress = { (currentRamPercent / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = ramColor,
                        trackColor = KeeneticColors.SurfaceElevated
                    )

                    // Breakdown chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MemoryBreakdownBox(
                            title = "Занято",
                            value = formatBytes(usedBytes),
                            color = ramColor,
                            modifier = Modifier.weight(1f)
                        )
                        MemoryBreakdownBox(
                            title = "Свободно",
                            value = formatBytes(freeBytes),
                            color = KeeneticColors.Success,
                            modifier = Modifier.weight(1f)
                        )
                        if (buffers > 0 || cached > 0) {
                            MemoryBreakdownBox(
                                title = "Кэш/Буфер",
                                value = formatBytes(buffers + cached),
                                color = KeeneticColors.TextSecondary,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Live RAM history Sparkline
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(KeeneticColors.SurfaceElevated)
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("История использования RAM (RCI show system)", style = MaterialTheme.typography.labelSmall, color = KeeneticColors.TextSecondary)
                            Text("Политика пула ОС", style = MaterialTheme.typography.labelSmall, color = KeeneticColors.TextSecondary)
                        }

                        LiveSparkline(
                            dataPoints = ramHistory,
                            lineColor = KeeneticColors.Primary,
                            gradientStartColor = KeeneticColors.Primary.copy(alpha = 0.35f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            minVal = 20,
                            maxVal = 80
                        )
                    }
                }
            }
        }

        // 5. Live Uptime & System Clock
        item {
            val uptimeSec = sysInfo?.uptime ?: 0L
            val days = uptimeSec / 86400
            val hours = (uptimeSec % 86400) / 3600
            val mins = (uptimeSec % 3600) / 60
            val secs = uptimeSec % 60

            val bootTime = remember(uptimeSec) {
                if (uptimeSec > 0) {
                    val bootEpoch = System.currentTimeMillis() - (uptimeSec * 1000L)
                    SimpleDateFormat("d MMMM yyyy, HH:mm", Locale("ru")).format(Date(bootEpoch))
                } else "—"
            }

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(KeeneticColors.CardBorder))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(KeeneticColors.Primary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Schedule, contentDescription = null, tint = KeeneticColors.Primary, modifier = Modifier.size(20.dp))
                            }
                            Column {
                                Text("Время непрерывной работы (Uptime)", style = MaterialTheme.typography.titleMedium, color = KeeneticColors.TextPrimary, fontWeight = FontWeight.Bold)
                                Text("RCI show system • uptime (сек)", style = MaterialTheme.typography.bodySmall, color = KeeneticColors.TextSecondary)
                            }
                        }
                    }

                    // 4 Uptime Blocks
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        UptimeDigitBlock(value = "$days", label = "ДНЕЙ", modifier = Modifier.weight(1f))
                        UptimeDigitBlock(value = "$hours", label = "ЧАСОВ", modifier = Modifier.weight(1f))
                        UptimeDigitBlock(value = "$mins", label = "МИНУТ", modifier = Modifier.weight(1f))
                        UptimeDigitBlock(value = "$secs", label = "СЕКУНД", isLive = isLivePolling, modifier = Modifier.weight(1f))
                    }

                    // Sub details
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(KeeneticColors.SurfaceElevated)
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DetailInfoRow("Время запуска роутера", bootTime)
                        HorizontalDivider(color = KeeneticColors.Divider.copy(alpha = 0.5f))
                        DetailInfoRow("Всего секунд аптайма", "${String.format(Locale.US, "%,d", uptimeSec)} сек")
                        if (sysInfo?.clockTime?.isNotBlank() == true) {
                            HorizontalDivider(color = KeeneticColors.Divider.copy(alpha = 0.5f))
                            DetailInfoRow("Часы роутера (clock)", sysInfo?.clockTime ?: "—")
                        }
                    }
                }
            }
        }

        // 6. Router Specifications (show version & show system)
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(KeeneticColors.CardBorder))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(KeeneticColors.Primary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Router, contentDescription = null, tint = KeeneticColors.Primary, modifier = Modifier.size(20.dp))
                        }
                        Column {
                            Text("Спецификация оборудования", style = MaterialTheme.typography.titleMedium, color = KeeneticColors.TextPrimary, fontWeight = FontWeight.Bold)
                            Text("RCI show version & show system", style = MaterialTheme.typography.bodySmall, color = KeeneticColors.TextSecondary)
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(KeeneticColors.SurfaceElevated)
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DetailInfoRow("Модель устройства", sysInfo?.model ?: "Keenetic")
                        HorizontalDivider(color = KeeneticColors.Divider.copy(alpha = 0.5f))
                        DetailInfoRow("Версия KeeneticOS", sysInfo?.osVersion ?: "—")
                        HorizontalDivider(color = KeeneticColors.Divider.copy(alpha = 0.5f))
                        DetailInfoRow("Ядро Linux (kernel)", sysInfo?.kernel ?: "—")
                        HorizontalDivider(color = KeeneticColors.Divider.copy(alpha = 0.5f))
                        DetailInfoRow("Архитектура CPU", sysInfo?.arch ?: "—")
                        HorizontalDivider(color = KeeneticColors.Divider.copy(alpha = 0.5f))
                        DetailInfoRow("Ревизия платы (hw)", sysInfo?.hwVersion ?: "rev.A")
                        HorizontalDivider(color = KeeneticColors.Divider.copy(alpha = 0.5f))
                        DetailInfoRow("Имя хоста (hostname)", "${sysInfo?.hostname ?: "Keenetic"}.${sysInfo?.domainName?.ifBlank { "local" }}")
                        HorizontalDivider(color = KeeneticColors.Divider.copy(alpha = 0.5f))
                        DetailInfoRow("Производитель", sysInfo?.manufacturer ?: "Keenetic Limited")
                    }
                }
            }
        }

        // 7. Interactive RCI Payload Inspector ('show version' & 'show system')
        item {
            var selectedRciTab by remember { mutableStateOf(0) }
            val rawVersion = sysInfo?.rawShowVersionJson.orEmpty()
            val rawSystem = sysInfo?.rawShowSystemJson.orEmpty()

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(KeeneticColors.CardBorder))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(KeeneticColors.Primary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Code, contentDescription = null, tint = KeeneticColors.Primary, modifier = Modifier.size(20.dp))
                            }
                            Column {
                                Text("Инспектор RCI API", style = MaterialTheme.typography.titleMedium, color = KeeneticColors.TextPrimary, fontWeight = FontWeight.Bold)
                                Text("Сырые JSON ответы KeeneticOS RCI", style = MaterialTheme.typography.bodySmall, color = KeeneticColors.TextSecondary)
                            }
                        }

                        // Copy button
                        val currentTextToCopy = if (selectedRciTab == 0) rawVersion else rawSystem
                        if (currentTextToCopy.isNotBlank()) {
                            FilledTonalButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(currentTextToCopy))
                                    Toast.makeText(context, "JSON скопирован в буфер обмена", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(containerColor = KeeneticColors.SurfaceElevated)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp), tint = KeeneticColors.Primary)
                                Spacer(Modifier.width(4.dp))
                                Text("Копировать", style = MaterialTheme.typography.labelSmall, color = KeeneticColors.Primary)
                            }
                        }
                    }

                    // Tab bar
                    TabRow(
                        selectedTabIndex = selectedRciTab,
                        containerColor = KeeneticColors.SurfaceElevated,
                        contentColor = KeeneticColors.Primary,
                        modifier = Modifier.clip(RoundedCornerShape(8.dp))
                    ) {
                        Tab(
                            selected = selectedRciTab == 0,
                            onClick = { selectedRciTab = 0 },
                            text = { Text("GET /rci/show/version", maxLines = 1) }
                        )
                        Tab(
                            selected = selectedRciTab == 1,
                            onClick = { selectedRciTab = 1 },
                            text = { Text("GET /rci/show/system", maxLines = 1) }
                        )
                    }

                    // Terminal JSON display
                    val displayText = if (selectedRciTab == 0) {
                        rawVersion.ifBlank { "{\n  \"status\": \"Ожидание данных от /rci/show/version...\"\n}" }
                    } else {
                        rawSystem.ifBlank { "{\n  \"status\": \"Ожидание данных от /rci/show/system...\"\n}" }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 120.dp, max = 220.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(KeeneticColors.TerminalBg)
                            .border(1.dp, KeeneticColors.Divider, RoundedCornerShape(8.dp))
                            .padding(12.dp)
                            .horizontalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = displayText,
                            color = KeeneticColors.TerminalText,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }

        // 8. Quick Navigation Cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickNavCard(
                    title = "Устройства",
                    subtitle = "${clients.size} в сети",
                    icon = Icons.Default.Devices,
                    onClick = { onNavigate(Screen.Devices.route) },
                    modifier = Modifier.weight(1f)
                )
                QuickNavCard(
                    title = "Wi-Fi Сеть",
                    subtitle = if (wifiNetworks.isNotEmpty()) "${wifiNetworks.size} сети" else "2.4G / 5G",
                    icon = Icons.Default.Wifi,
                    onClick = { onNavigate(Screen.WiFi.route) },
                    modifier = Modifier.weight(1f)
                )
                QuickNavCard(
                    title = "RCI / CLI",
                    subtitle = "Конфигуратор",
                    icon = Icons.Default.Terminal,
                    onClick = { onNavigate(Screen.Configuration.route) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

// -------------------------------------------------------------
// UI Subcomponents
// -------------------------------------------------------------

@Composable
fun LiveSparkline(
    dataPoints: List<Int>,
    lineColor: Color,
    gradientStartColor: Color,
    modifier: Modifier = Modifier,
    minVal: Int = 0,
    maxVal: Int = 100
) {
    Canvas(modifier = modifier) {
        if (dataPoints.size < 2) return@Canvas
        val width = size.width
        val height = size.height
        val stepX = width / (dataPoints.size - 1).coerceAtLeast(1)
        val range = (maxVal - minVal).coerceAtLeast(1).toFloat()

        val linePath = Path()
        val fillPath = Path()

        dataPoints.forEachIndexed { index, value ->
            val x = index * stepX
            val normalizedY = ((value - minVal) / range).coerceIn(0f, 1f)
            val y = height - (normalizedY * height)

            if (index == 0) {
                linePath.moveTo(x, y)
                fillPath.moveTo(x, height)
                fillPath.lineTo(x, y)
            } else {
                linePath.lineTo(x, y)
                fillPath.lineTo(x, y)
            }
        }

        fillPath.lineTo(width, height)
        fillPath.close()

        // Gradient under graph
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(gradientStartColor, Color.Transparent),
                startY = 0f,
                endY = height
            )
        )

        // Line
        drawPath(
            path = linePath,
            color = lineColor,
            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
        )

        // Pulsing head circle on the latest reading
        val lastVal = dataPoints.last()
        val lastX = width
        val lastY = height - (((lastVal - minVal) / range).coerceIn(0f, 1f) * height)
        drawCircle(
            color = lineColor,
            radius = 4.dp.toPx(),
            center = androidx.compose.ui.geometry.Offset(lastX, lastY)
        )
    }
}

@Composable
fun MetricChip(text: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = color.copy(alpha = 0.12f)
    ) {
        Text(
            text = text,
            color = color,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun MemoryBreakdownBox(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = KeeneticColors.SurfaceElevated
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(title, style = MaterialTheme.typography.labelSmall, color = KeeneticColors.TextSecondary)
            Text(value, style = MaterialTheme.typography.titleSmall, color = color, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun UptimeDigitBlock(
    value: String,
    label: String,
    isLive: Boolean = false,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = KeeneticColors.SurfaceElevated
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = if (isLive) KeeneticColors.Success else KeeneticColors.TextPrimary,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = KeeneticColors.TextSecondary,
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun DetailInfoRow(title: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.bodySmall, color = KeeneticColors.TextSecondary)
        Text(value, style = MaterialTheme.typography.bodySmall, color = KeeneticColors.TextPrimary, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun QuickNavCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = KeeneticColors.Surface),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(KeeneticColors.CardBorder))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(KeeneticColors.Primary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = KeeneticColors.Primary, modifier = Modifier.size(18.dp))
                }
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = KeeneticColors.TextSecondary.copy(alpha = 0.5f), modifier = Modifier.size(14.dp))
            }
            Column {
                Text(title, style = MaterialTheme.typography.titleSmall, color = KeeneticColors.TextPrimary, fontWeight = FontWeight.Bold, maxLines = 1)
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = KeeneticColors.TextSecondary, maxLines = 1)
            }
        }
    }
}

private fun formatBytes(bytes: Long): String {
    val mb = bytes / (1024 * 1024)
    return if (mb >= 1024) {
        String.format(Locale.US, "%.1f ГБ", mb / 1024.0)
    } else {
        "$mb МБ"
    }
}
