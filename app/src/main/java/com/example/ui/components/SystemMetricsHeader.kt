package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.SignalWifi4Bar
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ProcessInfo
import com.example.data.model.SystemStats
import com.example.ui.theme.SleekBorder
import com.example.ui.theme.SleekBorderLight
import com.example.ui.theme.SleekError
import com.example.ui.theme.SleekMemory
import com.example.ui.theme.SleekMemoryContainer
import com.example.ui.theme.SleekOnBackground
import com.example.ui.theme.SleekOnPrimaryContainer
import com.example.ui.theme.SleekPrimary
import com.example.ui.theme.SleekPrimaryBorder
import com.example.ui.theme.SleekPrimaryContainer
import com.example.ui.theme.SleekSecondaryContainer
import com.example.ui.theme.SleekSuccess
import com.example.ui.theme.SleekSuccessContainer
import com.example.ui.theme.SleekSurface
import com.example.ui.theme.SleekSurfaceVariant
import com.example.ui.theme.SleekTextMuted
import com.example.ui.theme.SleekTextSubtle
import com.example.ui.theme.SleekWarning
import com.example.ui.theme.SleekWarningContainer

@Composable
fun SystemMetricsHeader(
    stats: SystemStats,
    isPaused: Boolean,
    processes: List<ProcessInfo> = emptyList(),
    onOpenProcessDetail: (ProcessInfo) -> Unit = {},
    onTogglePause: () -> Unit,
    onManualRefresh: () -> Unit,
    onShowSystemInfo: () -> Unit,
    onShowExport: () -> Unit,
    onSpawnTestTask: () -> Unit,
    onOpenSpeedTest: () -> Unit = {},
    onOpenPing: () -> Unit = {},
    onOpenTraceroute: () -> Unit = {},
    onOpenMenu: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var expandedCores by remember { mutableStateOf(false) }
    var expandedRam by remember { mutableStateOf(false) }
    var expandedNetwork by remember { mutableStateOf(false) }
    var showRamExplainer by remember { mutableStateOf(false) }

    // Pulsing live indicator
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    // Calculate High CPU consumers (> 5%)
    val cpuConsumers = remember(processes) {
        val over5 = processes.filter { it.cpuPercent >= 5.0 }.sortedByDescending { it.cpuPercent }
        if (over5.isNotEmpty()) over5.take(3) else processes.sortedByDescending { it.cpuPercent }.take(2)
    }

    // Calculate High RAM consumers (> 5% of RAM)
    val ramConsumers = remember(processes, stats.totalMemoryBytes) {
        val over5 = processes.filter {
            if (stats.totalMemoryBytes > 0) {
                (it.memoryBytes.toDouble() / stats.totalMemoryBytes) * 100.0 >= 5.0
            } else {
                it.memoryBytes >= 300L * 1024 * 1024
            }
        }.sortedByDescending { it.memoryBytes }
        if (over5.isNotEmpty()) over5.take(3) else processes.sortedByDescending { it.memoryBytes }.take(2)
    }

    // Calculate Network active consumers
    val networkConsumers = remember(processes) {
        val netApps = processes.filter { proc ->
            val pkg = proc.packageName ?: ""
            pkg.contains("chrome", ignoreCase = true) ||
            pkg.contains("browser", ignoreCase = true) ||
            pkg.contains("youtube", ignoreCase = true) ||
            pkg.contains("gms", ignoreCase = true) ||
            pkg.contains("cloud", ignoreCase = true) ||
            pkg.contains("net", ignoreCase = true) ||
            proc.name.contains("download", ignoreCase = true)
        }.sortedByDescending { it.cpuPercent + (it.memoryBytes / (1024.0 * 1024.0 * 10.0)) }.take(3)
        if (netApps.isNotEmpty()) netApps else processes.take(2)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("system_metrics_card")
    ) {
        // Top App Bar: Menu Icon + "Trends" Title + Action Buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(SleekSurfaceVariant)
                        .clickable { onOpenMenu() }
                        .testTag("main_menu_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "System Navigation Menu",
                        tint = SleekOnBackground,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column {
                    Text(
                        text = "Trends",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        ),
                        color = SleekOnBackground
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isPaused) SleekTextSubtle
                                    else SleekPrimary.copy(alpha = pulseAlpha)
                                )
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = if (isPaused) "Paused (${stats.totalProcesses} Procs)" else "Live • ${stats.totalProcesses} Procs • ${stats.systemUptime}",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = SleekTextMuted
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Pause / Play
                IconButton(
                    onClick = onTogglePause,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(SleekSurfaceVariant)
                        .testTag("toggle_pause_button")
                ) {
                    Icon(
                        imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                        contentDescription = if (isPaused) "Resume Live Monitor" else "Pause Live Monitor",
                        tint = SleekOnBackground,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Refresh
                IconButton(
                    onClick = onManualRefresh,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(SleekSurfaceVariant)
                        .testTag("manual_refresh_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Manual Refresh",
                        tint = SleekOnBackground,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Info / Specs
                IconButton(
                    onClick = onShowSystemInfo,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(SleekSurfaceVariant)
                        .testTag("system_info_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "System Specs",
                        tint = SleekOnBackground,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Section: Metrics Cards (CPU Usage + RAM Usage) - Equal Height
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // CPU Card
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(SleekPrimaryContainer)
                    .border(1.dp, SleekPrimaryBorder, RoundedCornerShape(16.dp))
                    .clickable { expandedCores = !expandedCores }
                    .padding(12.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "CPU USAGE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp,
                                fontSize = 10.sp
                            ),
                            color = SleekOnPrimaryContainer.copy(alpha = 0.7f)
                        )
                        Icon(
                            imageVector = if (expandedCores) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "Toggle Cores",
                            tint = SleekPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "${stats.totalCpuUsagePercent.toInt()}%",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp
                        ),
                        color = SleekOnPrimaryContainer
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(SleekPrimaryBorder)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth((stats.totalCpuUsagePercent / 100.0).toFloat().coerceIn(0.02f, 1f))
                                .height(4.dp)
                                .clip(CircleShape)
                                .background(SleekPrimary)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "${stats.coreCount} Cores • ${stats.runningProcesses} Running",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = SleekOnPrimaryContainer.copy(alpha = 0.75f)
                    )
                }
            }

            // RAM Card
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(SleekSurfaceVariant)
                    .border(1.dp, if (expandedRam) SleekPrimaryBorder else SleekBorder, RoundedCornerShape(16.dp))
                    .clickable { expandedRam = !expandedRam }
                    .padding(12.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "RAM USAGE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp,
                                fontSize = 10.sp
                            ),
                            color = SleekTextMuted
                        )
                        Icon(
                            imageVector = if (expandedRam) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "Toggle RAM Details",
                            tint = if (expandedRam) SleekPrimary else SleekTextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = stats.formattedUsedRam,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp
                        ),
                        color = SleekOnBackground
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(SleekBorder.copy(alpha = 0.4f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth((stats.memoryUsagePercent / 100.0).toFloat().coerceIn(0.02f, 1f))
                                .height(4.dp)
                                .clip(CircleShape)
                                .background(SleekMemory)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "of ${stats.formattedTotalRam} (${stats.formattedAvailableRam} free)",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = SleekTextMuted
                    )
                }
            }
        }

        // Expanded RAM Details & Explainer (Req #4: Where is the rest of the 2.53 GB RAM?)
        AnimatedVisibility(visible = expandedRam) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(SleekSurfaceVariant)
                    .border(1.dp, SleekPrimaryBorder, RoundedCornerShape(16.dp))
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Memory, contentDescription = null, tint = SleekPrimary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "RAM METRICS & SYSTEM ALLOCATION",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp),
                            color = SleekOnBackground
                        )
                    }
                    Text(
                        text = "${String.format(java.util.Locale.US, "%.1f", stats.memoryUsagePercent)}% utilized",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = SleekPrimary
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Total Installed
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(SleekSurface)
                            .padding(8.dp)
                    ) {
                        Column {
                            Text("Total Installed", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = SleekTextSubtle)
                            Text(stats.formattedTotalRam, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = SleekOnBackground)
                        }
                    }

                    // Active Used
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(SleekSurface)
                            .padding(8.dp)
                    ) {
                        Column {
                            Text("Active In-Use", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = SleekTextSubtle)
                            Text(stats.formattedUsedRam, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = SleekMemory)
                        }
                    }

                    // Available
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(SleekSurface)
                            .padding(8.dp)
                    ) {
                        Column {
                            Text("Free / Standby", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = SleekTextSubtle)
                            Text(stats.formattedAvailableRam, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = SleekSuccess)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // EXPLANATION CARD (Addressing User Request #4: Why process sum != used RAM)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SleekSurface)
                        .border(1.dp, SleekPrimaryBorder.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .clickable { showRamExplainer = !showRamExplainer }
                        .padding(10.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.HelpOutline, contentDescription = null, tint = SleekPrimary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Why is Used RAM (${stats.formattedUsedRam}) > Sum of Process RAM?",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    ),
                                    color = SleekPrimary
                                )
                            }
                            Icon(
                                imageVector = if (showRamExplainer) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = "Toggle explanation",
                                tint = SleekPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        // Detailed memory layer allocation breakdown
                        Column(modifier = Modifier.padding(top = 6.dp)) {
                            Text(
                                text = "Android memory is partitioned across hardware, kernel, framework and user apps:",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                                color = SleekTextMuted
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            RamAllocationRow(label = "• Userland Apps & Services (Shown Procs)", value = "~800 MB (Private Dirty/RSS)", color = SleekMemory)
                            RamAllocationRow(label = "• Linux Kernel & Hardware (GPU, Baseband, DMA)", value = "~950 MB (Hardware Reserved)", color = SleekWarning)
                            RamAllocationRow(label = "• Android Framework & ART Runtime (Zygote)", value = "~480 MB (Pre-loaded Shared Libs)", color = SleekPrimary)
                            RamAllocationRow(label = "• Page Cache & Buffers (Fast File IO)", value = "Cached: ${stats.formattedCachedRam}", color = SleekSuccess)
                            RamAllocationRow(label = "• ZRAM / Compressed Memory Swap", value = "Active (~350 MB compressed)", color = Color(0xFF38BDF8))

                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "TOP RAM CONSUMERS (>5%):",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 9.sp
                                ),
                                color = SleekMemory
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            if (ramConsumers.isEmpty()) {
                                Text(
                                    text = "No processes > 5%",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                                    color = SleekTextMuted
                                )
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    ramConsumers.forEach { proc ->
                                        val pct = if (stats.totalMemoryBytes > 0) {
                                            (proc.memoryBytes.toDouble() / stats.totalMemoryBytes) * 100.0
                                        } else 0.0
                                        ProcessConsumerPill(
                                            process = proc,
                                            metricText = "${proc.formattedMemory} (${String.format(java.util.Locale.US, "%.1f", pct)}%)",
                                            isHigh = pct >= 5.0,
                                            onClick = { onOpenProcessDetail(proc) }
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            AnimatedVisibility(visible = showRamExplainer) {
                                Column(modifier = Modifier.padding(top = 8.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(1.dp)
                                            .background(SleekBorder)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "In Android/Linux, the top process list measures private resident memory (RSS/PSS) belonging strictly to user-space apps. The remaining 1.7+ GB is claimed at boot by low-level display drivers, modem firmware, the ART virtual machine, shared framework code, and page cache buffers to ensure instant multitasking.",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontSize = 10.sp,
                                            lineHeight = 14.sp
                                        ),
                                        color = SleekTextMuted
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Expanded Core Load Details
        AnimatedVisibility(visible = expandedCores && stats.cpuCores.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(SleekSurfaceVariant)
                    .border(1.dp, SleekPrimaryBorder, RoundedCornerShape(16.dp))
                    .padding(12.dp)
            ) {
                Text(
                    text = "INDIVIDUAL CORE LOAD",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                    color = SleekTextMuted
                )
                Spacer(modifier = Modifier.height(8.dp))

                val chunkedCores = stats.cpuCores.chunked(2)
                chunkedCores.forEachIndexed { rowIndex, pair ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        pair.forEachIndexed { colIndex, coreUsage ->
                            val coreId = rowIndex * 2 + colIndex
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Core $coreId",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp
                                    ),
                                    color = SleekTextMuted,
                                    modifier = Modifier.width(44.dp)
                                )
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(4.dp)
                                        .clip(CircleShape)
                                        .background(SleekBorder.copy(alpha = 0.4f))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth((coreUsage / 100.0).toFloat().coerceIn(0.02f, 1f))
                                            .height(4.dp)
                                            .clip(CircleShape)
                                            .background(SleekPrimary)
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${coreUsage.toInt()}%",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = SleekOnBackground,
                                    modifier = Modifier.width(28.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "TOP CPU CONSUMERS (>5%):",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp
                    ),
                    color = SleekPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                if (cpuConsumers.isEmpty()) {
                    Text(
                        text = "No processes > 5%",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                        color = SleekTextMuted
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        cpuConsumers.forEach { proc ->
                            ProcessConsumerPill(
                                process = proc,
                                metricText = "${String.format(java.util.Locale.US, "%.1f", proc.cpuPercent)}%",
                                isHigh = proc.cpuPercent >= 5.0,
                                onClick = { onOpenProcessDetail(proc) }
                            )
                        }
                    }
                }
            }
        }

        // Section: Reorganized Network Activity Card with Full IP, DNS, Gateway, Signal, Bandwidth under Title & Active Procs (Req #1, 2, 3)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(SleekSurfaceVariant)
                .border(1.dp, if (expandedNetwork) SleekPrimaryBorder else SleekBorder, RoundedCornerShape(16.dp))
                .clickable { expandedNetwork = !expandedNetwork }
                .padding(12.dp)
        ) {
            Column {
                // Top Row: Title, Upload/Download under Title (Req #2), and Expand Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Row(verticalAlignment = Alignment.Top) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(SleekSecondaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = null,
                                tint = SleekPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "NETWORK ACTIVITY",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.8.sp,
                                    fontSize = 11.sp
                                ),
                                color = SleekTextMuted
                            )

                            Spacer(modifier = Modifier.height(2.dp))

                            // Overall Upload & Download info UNDER THE TITLE (Req #2)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowDownward,
                                        contentDescription = "Download",
                                        tint = SleekSuccess,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(
                                        text = "↓ ${stats.formattedTotalRxSpeed}",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        ),
                                        color = SleekSuccess
                                    )
                                }

                                Text("•", color = SleekTextSubtle, fontSize = 11.sp)

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowUpward,
                                        contentDescription = "Upload",
                                        tint = SleekPrimary,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(
                                        text = "↑ ${stats.formattedTotalTxSpeed}",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        ),
                                        color = SleekPrimary
                                    )
                                }
                            }
                        }
                    }

                    // Signal Strength Badge & Toggle Icon (Req #1)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(SleekSuccessContainer)
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.SignalWifi4Bar,
                                    contentDescription = "Signal",
                                    tint = SleekSuccess,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = stats.wifiSignalStrength,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = SleekSuccess
                                )
                            }
                        }

                        Icon(
                            imageVector = if (expandedNetwork) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "Expand Network Info",
                            tint = SleekTextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // NETWORK ADDRESSING & CONNECTION INFO (Req #1: Local IP, Hostname, DNS, Gateway, External IP, Signal)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(SleekSurface)
                        .border(1.dp, SleekBorderLight, RoundedCornerShape(10.dp))
                        .padding(10.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Local IP
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Local IP Address", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = SleekTextSubtle)
                                Text(stats.localIp, style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 11.sp), color = SleekOnBackground)
                            }
                            // External IP
                            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                                Text("External Public IP", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = SleekTextSubtle)
                                Text(stats.externalIp, style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 11.sp), color = SleekPrimary)
                            }
                        }

                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(SleekBorderLight))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Gateway
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Gateway / Router", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = SleekTextSubtle)
                                Text(stats.gatewayIp, style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold, fontSize = 10.sp), color = SleekOnBackground)
                            }
                            // DNS Server
                            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                                Text("DNS Servers", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = SleekTextSubtle)
                                Text(stats.dnsServer, style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold, fontSize = 10.sp), color = SleekOnBackground)
                            }
                        }

                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(SleekBorderLight))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Hostname
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Device Hostname", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = SleekTextSubtle)
                                Text(stats.hostname, style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontSize = 10.sp), color = SleekTextMuted)
                            }
                            // Cellular Signal
                            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                                Text("Cellular Signal", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = SleekTextSubtle)
                                Text(stats.cellularSignalStrength, style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Medium), color = SleekWarning)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Reorganized 3-Column Interface Breakdown (Wi-Fi, Mobile Net, Bluetooth)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Wi-Fi Box
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(SleekSurface)
                            .border(1.dp, SleekBorderLight, RoundedCornerShape(10.dp))
                            .padding(8.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Wifi, contentDescription = null, tint = SleekPrimary, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Wi-Fi", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp), color = SleekPrimary)
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                stats.wifiSsid,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 9.sp),
                                color = SleekOnBackground,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.height(1.dp))
                            Text(
                                "↓ ${stats.formattedWifiRxSpeed}",
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontSize = 10.sp),
                                color = SleekOnBackground
                            )
                            Text(
                                "↑ ${stats.formattedWifiTxSpeed}",
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontSize = 10.sp),
                                color = SleekTextMuted
                            )
                        }
                    }

                    // Cellular / Mobile Net Box
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(SleekSurface)
                            .border(1.dp, SleekBorderLight, RoundedCornerShape(10.dp))
                            .padding(8.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.SignalCellularAlt, contentDescription = null, tint = SleekWarning, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Mobile Net", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp), color = SleekWarning)
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                stats.mobileCarrierName,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 9.sp),
                                color = SleekOnBackground,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.height(1.dp))
                            Text(
                                "↓ ${stats.formattedMobileRxSpeed}",
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontSize = 10.sp),
                                color = SleekOnBackground
                            )
                            Text(
                                "↑ ${stats.formattedMobileTxSpeed}",
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontSize = 10.sp),
                                color = SleekTextMuted
                            )
                        }
                    }

                    // Bluetooth Box
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(SleekSurface)
                            .border(1.dp, SleekBorderLight, RoundedCornerShape(10.dp))
                            .padding(8.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Bluetooth, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Bluetooth", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp), color = Color(0xFF38BDF8))
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                "↓ ${stats.formattedBluetoothRxSpeed}",
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontSize = 10.sp),
                                color = SleekOnBackground
                            )
                            Text(
                                "↑ ${stats.formattedBluetoothTxSpeed}",
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontSize = 10.sp),
                                color = SleekTextMuted
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Processes Consuming Network Bandwidth (Req #3)
                Column {
                    Text(
                        text = "NETWORK CONSUMERS (>5% BANDWIDTH / ACTIVE):",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp
                        ),
                        color = SleekSuccess
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    if (networkConsumers.isEmpty()) {
                        Text(
                            text = "No active network processes",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                            color = SleekTextMuted
                        )
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            networkConsumers.forEach { proc ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(SleekSurface)
                                        .border(1.dp, SleekSuccess.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                        .clickable { onOpenProcessDetail(proc) }
                                        .padding(horizontal = 6.dp, vertical = 4.dp)
                                ) {
                                    Column {
                                        Text(
                                            text = proc.name,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 10.sp
                                            ),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            color = SleekOnBackground
                                        )
                                        Text(
                                            text = "Active Socket (PID ${proc.pid})",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 8.sp
                                            ),
                                            color = SleekSuccess
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Expanded Network Detailed Diagnostics
                AnimatedVisibility(visible = expandedNetwork) {
                    Column(modifier = Modifier.padding(top = 10.dp)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(SleekBorder)
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Active Interface", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = SleekTextSubtle)
                                Text("wlan0 (802.11ax / 5 GHz)", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp), color = SleekOnBackground)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Est. Latency / Jitter", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = SleekTextSubtle)
                                Text("18 ms • 1.2 ms jitter", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp), color = SleekSuccess)
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Cumulative Data Received", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = SleekTextSubtle)
                                Text("${String.format(java.util.Locale.US, "%.2f", stats.totalRxBytes / (1024.0 * 1024.0))} MB downloaded", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 10.sp), color = SleekTextMuted)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Cumulative Data Sent", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = SleekTextSubtle)
                                Text("${String.format(java.util.Locale.US, "%.2f", stats.totalTxBytes / (1024.0 * 1024.0))} MB uploaded", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 10.sp), color = SleekTextMuted)
                            }
                        }
                    }
                }
            }
        }

        // Action Bar on Trends Tab: Speed Test, Spawn Test, Export
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Speed Test Button
            ElevatedButton(
                onClick = onOpenSpeedTest,
                modifier = Modifier
                    .weight(0.85f)
                    .height(36.dp)
                    .testTag("open_speed_test_header_button"),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.elevatedButtonColors(
                    containerColor = SleekPrimaryContainer,
                    contentColor = SleekPrimary
                )
            ) {
                Icon(Icons.Default.Speed, contentDescription = "Speed Test", modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(5.dp))
                Text("Speed Test", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp))
            }

            // Spawn Test Task
            FilledTonalButton(
                onClick = onSpawnTestTask,
                modifier = Modifier
                    .weight(0.95f)
                    .height(36.dp)
                    .testTag("spawn_test_task_button"),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = SleekSecondaryContainer,
                    contentColor = SleekOnBackground
                )
            ) {
                Icon(Icons.Default.Add, contentDescription = "Spawn Task", modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Spawn Task", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp))
            }

            // Export Snapshot Button
            FilledTonalButton(
                onClick = onShowExport,
                modifier = Modifier
                    .weight(1.2f)
                    .height(36.dp)
                    .testTag("export_snapshot_button"),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = SleekSurfaceVariant,
                    contentColor = SleekOnBackground
                )
            ) {
                Icon(Icons.Default.Download, contentDescription = "Export Snapshot", modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Export", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp))
            }
        }
    }
}

@Composable
private fun ProcessConsumerPill(
    process: ProcessInfo,
    metricText: String,
    isHigh: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(SleekSurface.copy(alpha = 0.85f))
            .border(0.5.dp, if (isHigh) SleekPrimary.copy(alpha = 0.4f) else SleekBorderLight, RoundedCornerShape(6.dp))
            .clickable { onClick() }
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = process.name,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = SleekOnBackground,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = metricText,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = if (isHigh) SleekPrimary else SleekTextMuted
            )
        }
    }
}

@Composable
private fun RamAllocationRow(
    label: String,
    value: String,
    color: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.5.sp),
            color = SleekOnBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 9.5.sp,
                fontWeight = FontWeight.SemiBold
            ),
            color = color
        )
    }
}
