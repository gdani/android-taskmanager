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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SystemStats
import com.example.ui.theme.SleekBorder
import com.example.ui.theme.SleekError
import com.example.ui.theme.SleekErrorContainer
import com.example.ui.theme.SleekOnBackground
import com.example.ui.theme.SleekOnPrimaryContainer
import com.example.ui.theme.SleekPrimary
import com.example.ui.theme.SleekPrimaryBorder
import com.example.ui.theme.SleekPrimaryContainer
import com.example.ui.theme.SleekSecondaryContainer
import com.example.ui.theme.SleekSurface
import com.example.ui.theme.SleekSurfaceVariant
import com.example.ui.theme.SleekTextMuted
import com.example.ui.theme.SleekTextSubtle

@Composable
fun SystemMetricsHeader(
    stats: SystemStats,
    isPaused: Boolean,
    isRefreshing: Boolean,
    onTogglePause: () -> Unit,
    onManualRefresh: () -> Unit,
    onKillAllBackground: () -> Unit,
    onSpawnTestTask: () -> Unit,
    onShowKillHistory: () -> Unit,
    onShowExport: () -> Unit,
    onShowSystemInfo: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expandedCores by remember { mutableStateOf(false) }

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

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("system_metrics_card")
    ) {
        // Top App Bar: Menu Icon + "Task Manager" Title + Action Buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(SleekSurfaceVariant)
                        .clickable { onShowSystemInfo() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "System Menu",
                        tint = SleekOnBackground,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column {
                    Text(
                        text = "Task Manager",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 22.sp
                        ),
                        color = SleekOnBackground
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isPaused) SleekTextSubtle
                                    else SleekPrimary.copy(alpha = pulseAlpha)
                                )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isPaused) "Paused (${stats.totalProcesses} Procs)" else "Live Monitor • ${stats.totalProcesses} Procs",
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
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(SleekSurfaceVariant)
                        .testTag("toggle_pause_button")
                ) {
                    Icon(
                        imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                        contentDescription = if (isPaused) "Resume Live Monitor" else "Pause Live Monitor",
                        tint = SleekOnBackground,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Refresh
                IconButton(
                    onClick = onManualRefresh,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(SleekSurfaceVariant)
                        .testTag("manual_refresh_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Manual Refresh",
                        tint = SleekOnBackground,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Info / Specs
                IconButton(
                    onClick = onShowSystemInfo,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(SleekSurfaceVariant)
                        .testTag("system_info_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "System Specs",
                        tint = SleekOnBackground,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Section: Metrics Cards (CPU Usage #EADDFF + Memory #F3EDF7)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // CPU Card (#EADDFF container, #21005D text, #6750A4 progress bar)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(18.dp))
                    .background(SleekPrimaryContainer)
                    .clickable { expandedCores = !expandedCores }
                    .padding(16.dp)
            ) {
                Column {
                    Text(
                        text = "CPU USAGE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = SleekOnPrimaryContainer.copy(alpha = 0.7f)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "${stats.totalCpuUsagePercent.toInt()}%",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 26.sp
                        ),
                        color = SleekOnPrimaryContainer
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Progress bar matching design
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

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "${stats.coreCount} Cores • ${stats.runningProcesses} Active",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = SleekOnPrimaryContainer.copy(alpha = 0.75f)
                    )
                }
            }

            // Memory Card (#F3EDF7 container, border #CAC4D0, text #1D1B20)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(18.dp))
                    .background(SleekSurfaceVariant)
                    .border(1.dp, SleekBorder, RoundedCornerShape(18.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Text(
                        text = "MEMORY",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = SleekTextMuted.copy(alpha = 0.8f)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = stats.formattedUsedRam,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 26.sp
                        ),
                        color = SleekOnBackground
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Progress bar
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
                                .background(SleekPrimary)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "of ${stats.formattedTotalRam} active",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = SleekTextMuted
                    )
                }
            }
        }

        // Expanded Multi-Core Activity Bars
        AnimatedVisibility(visible = expandedCores && stats.cpuCores.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(SleekSurfaceVariant)
                    .border(1.dp, SleekBorder, RoundedCornerShape(16.dp))
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
            }
        }

        // Action Quick Bar: Clean BG, Spawn Task, History, Export
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Clean Background Tasks (M3 Error container style)
            ElevatedButton(
                onClick = onKillAllBackground,
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp)
                    .testTag("end_background_tasks_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.elevatedButtonColors(
                    containerColor = SleekErrorContainer,
                    contentColor = SleekError
                )
            ) {
                Icon(
                    imageVector = Icons.Default.CleaningServices,
                    contentDescription = "Clean Background Tasks",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Clean BG",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            // Spawn Test Task (M3 Secondary container)
            FilledTonalButton(
                onClick = onSpawnTestTask,
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp)
                    .testTag("spawn_test_task_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = SleekSecondaryContainer,
                    contentColor = SleekOnBackground
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Spawn Test Task",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Test Task",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            // Kill History Button
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(SleekSurfaceVariant)
                    .border(1.dp, SleekBorder, RoundedCornerShape(12.dp))
                    .clickable { onShowKillHistory() }
                    .testTag("kill_history_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = "Kill History",
                    tint = SleekOnBackground,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Export Button
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(SleekSurfaceVariant)
                    .border(1.dp, SleekBorder, RoundedCornerShape(12.dp))
                    .clickable { onShowExport() }
                    .testTag("export_snapshot_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = "Export Process Snapshot",
                    tint = SleekOnBackground,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
