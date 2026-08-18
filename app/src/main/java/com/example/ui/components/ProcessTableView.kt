package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
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
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ProcessInfo
import com.example.data.model.ProcessSortColumn
import com.example.data.model.ProcessState
import com.example.ui.theme.SleekBorder
import com.example.ui.theme.SleekBorderLight
import com.example.ui.theme.SleekError
import com.example.ui.theme.SleekErrorContainer
import com.example.ui.theme.SleekMemory
import com.example.ui.theme.SleekMemoryBorder
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
import com.example.ui.theme.SleekSurfaceSelected
import com.example.ui.theme.SleekSurfaceVariant
import com.example.ui.theme.SleekTextMuted
import com.example.ui.theme.SleekTextSubtle
import com.example.ui.theme.SleekWarning
import com.example.ui.theme.SleekWarningContainer

enum class ResourceSeverity {
    NORMAL,
    ELEVATED_CPU,
    CRITICAL_CPU,
    ELEVATED_RAM,
    CRITICAL_RAM,
    CRITICAL_BOTH
}

fun getProcessResourceSeverity(process: ProcessInfo): ResourceSeverity {
    val isCriticalCpu = process.cpuPercent >= 8.0
    val isElevatedCpu = process.cpuPercent >= 2.5
    val isCriticalRam = process.memoryBytes >= 100L * 1024L * 1024L
    val isElevatedRam = process.memoryBytes >= 50L * 1024L * 1024L

    return when {
        isCriticalCpu && isCriticalRam -> ResourceSeverity.CRITICAL_BOTH
        isCriticalCpu -> ResourceSeverity.CRITICAL_CPU
        isCriticalRam -> ResourceSeverity.CRITICAL_RAM
        isElevatedCpu && isElevatedRam -> ResourceSeverity.CRITICAL_BOTH
        isElevatedCpu -> ResourceSeverity.ELEVATED_CPU
        isElevatedRam -> ResourceSeverity.ELEVATED_RAM
        else -> ResourceSeverity.NORMAL
    }
}

@Composable
fun ProcessTableView(
    processes: List<ProcessInfo>,
    sortColumn: ProcessSortColumn,
    isSortAscending: Boolean,
    onSortColumn: (ProcessSortColumn) -> Unit,
    onTerminateProcess: (ProcessInfo) -> Unit,
    onOpenDetail: (ProcessInfo) -> Unit,
    isMultiSelectMode: Boolean,
    selectedPids: Set<Int>,
    onToggleSelectPid: (Int) -> Unit,
    isCompactView: Boolean,
    onClearFilters: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val expandedCommandPids = remember { mutableStateMapOf<Int, Boolean>() }

    fun copyToClipboard(text: String, label: String = "Command Line") {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Copied to clipboard: $label", Toast.LENGTH_SHORT).show()
    }

    if (processes.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(SleekSurfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Terminal,
                        contentDescription = null,
                        tint = SleekPrimary,
                        modifier = Modifier.size(36.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No processes found",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = SleekOnBackground
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Try adjusting your search query or category filters",
                    style = MaterialTheme.typography.bodySmall,
                    color = SleekTextMuted
                )
                Spacer(modifier = Modifier.height(16.dp))
                FilledTonalButton(
                    onClick = onClearFilters,
                    shape = RoundedCornerShape(12.dp),
                    colors = androidx.compose.material3.ButtonDefaults.filledTonalButtonColors(
                        containerColor = SleekPrimaryContainer,
                        contentColor = SleekOnPrimaryContainer
                    )
                ) {
                    Text("Reset Filters")
                }
            }
        }
        return
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Table Column Headers (Sortable)
        TableHeaderBar(
            sortColumn = sortColumn,
            isSortAscending = isSortAscending,
            onSortColumn = onSortColumn,
            isMultiSelectMode = isMultiSelectMode
        )

        // Process List
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(if (isCompactView) 6.dp else 10.dp)
        ) {
            items(
                items = processes,
                key = { it.pid }
            ) { process ->
                val isExpanded = expandedCommandPids[process.pid] == true
                val isSelected = selectedPids.contains(process.pid)

                if (isCompactView) {
                    CompactProcessRow(
                        process = process,
                        isExpanded = isExpanded,
                        isSelected = isSelected,
                        isMultiSelectMode = isMultiSelectMode,
                        onToggleSelect = { onToggleSelectPid(process.pid) },
                        onToggleExpand = { expandedCommandPids[process.pid] = !isExpanded },
                        onTerminate = { onTerminateProcess(process) },
                        onOpenDetail = { onOpenDetail(process) },
                        onCopyCmd = { copyToClipboard(process.cmdline, "Process ${process.pid} Command") }
                    )
                } else {
                    DetailedProcessCard(
                        process = process,
                        isExpanded = isExpanded,
                        isSelected = isSelected,
                        isMultiSelectMode = isMultiSelectMode,
                        onToggleSelect = { onToggleSelectPid(process.pid) },
                        onToggleExpand = { expandedCommandPids[process.pid] = !isExpanded },
                        onTerminate = { onTerminateProcess(process) },
                        onOpenDetail = { onOpenDetail(process) },
                        onCopyCmd = { copyToClipboard(process.cmdline, "Process ${process.pid} Command") }
                    )
                }
            }
            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun TableHeaderBar(
    sortColumn: ProcessSortColumn,
    isSortAscending: Boolean,
    onSortColumn: (ProcessSortColumn) -> Unit,
    isMultiSelectMode: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isMultiSelectMode) {
                Spacer(modifier = Modifier.width(36.dp))
            }

            // PID Header
            SortableHeaderItem(
                title = "PID",
                column = ProcessSortColumn.PID,
                currentSort = sortColumn,
                isAscending = isSortAscending,
                onSort = onSortColumn,
                modifier = Modifier.width(64.dp)
            )

            // Name Header
            SortableHeaderItem(
                title = "PROCESS / CMD",
                column = ProcessSortColumn.NAME,
                currentSort = sortColumn,
                isAscending = isSortAscending,
                onSort = onSortColumn,
                modifier = Modifier.weight(1f)
            )

            // CPU Header
            SortableHeaderItem(
                title = "CPU %",
                column = ProcessSortColumn.CPU,
                currentSort = sortColumn,
                isAscending = isSortAscending,
                onSort = onSortColumn,
                modifier = Modifier.width(64.dp)
            )

            // RAM Header
            SortableHeaderItem(
                title = "RAM",
                column = ProcessSortColumn.MEMORY,
                currentSort = sortColumn,
                isAscending = isSortAscending,
                onSort = onSortColumn,
                modifier = Modifier.width(72.dp)
            )

            // Action Header
            Text(
                text = "ACTION",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    letterSpacing = 0.5.sp
                ),
                color = SleekTextMuted,
                modifier = Modifier.width(48.dp)
            )
        }
    }
}

@Composable
fun SortableHeaderItem(
    title: String,
    column: ProcessSortColumn,
    currentSort: ProcessSortColumn,
    isAscending: Boolean,
    onSort: (ProcessSortColumn) -> Unit,
    modifier: Modifier = Modifier
) {
    val isSelected = currentSort == column
    Row(
        modifier = modifier
            .clickable { onSort(column) }
            .testTag("sort_header_${column.name.lowercase()}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                fontSize = 10.sp,
                letterSpacing = 0.5.sp
            ),
            color = if (isSelected) SleekPrimary else SleekTextMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (isSelected) {
            Spacer(modifier = Modifier.width(2.dp))
            Icon(
                imageVector = if (isAscending) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                contentDescription = if (isAscending) "Ascending" else "Descending",
                tint = SleekPrimary,
                modifier = Modifier.size(12.dp)
            )
        }
    }
}

@Composable
fun DetailedProcessCard(
    process: ProcessInfo,
    isExpanded: Boolean,
    isSelected: Boolean,
    isMultiSelectMode: Boolean,
    onToggleSelect: () -> Unit,
    onToggleExpand: () -> Unit,
    onTerminate: () -> Unit,
    onOpenDetail: () -> Unit,
    onCopyCmd: () -> Unit,
    modifier: Modifier = Modifier
) {
    val severity = getProcessResourceSeverity(process)
    val isHighResource = severity != ResourceSeverity.NORMAL

    // Pulsing animation for critical alert badges
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_alert_${process.pid}")
    val alertAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(750, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alert_pulse"
    )

    // Dynamic Card Colors based on Resource Severity
    val containerBg = when {
        isSelected -> SleekSurfaceSelected
        severity == ResourceSeverity.CRITICAL_BOTH || severity == ResourceSeverity.CRITICAL_CPU -> Color(0xFFFFF7F6)
        severity == ResourceSeverity.CRITICAL_RAM -> Color(0xFFF3FBFB)
        severity == ResourceSeverity.ELEVATED_CPU -> Color(0xFFFFFBF2)
        severity == ResourceSeverity.ELEVATED_RAM -> Color(0xFFF7FCFC)
        else -> SleekSurface
    }

    val borderColor = when {
        isSelected -> SleekPrimaryBorder
        severity == ResourceSeverity.CRITICAL_BOTH || severity == ResourceSeverity.CRITICAL_CPU -> SleekError.copy(alpha = 0.7f)
        severity == ResourceSeverity.CRITICAL_RAM -> SleekMemory.copy(alpha = 0.7f)
        severity == ResourceSeverity.ELEVATED_CPU -> SleekWarning.copy(alpha = 0.5f)
        severity == ResourceSeverity.ELEVATED_RAM -> SleekMemory.copy(alpha = 0.4f)
        else -> SleekBorderLight
    }

    val borderWidth = if (isSelected || severity == ResourceSeverity.CRITICAL_CPU || severity == ResourceSeverity.CRITICAL_RAM || severity == ResourceSeverity.CRITICAL_BOTH) 1.5.dp else 1.dp

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("process_card_${process.pid}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = containerBg),
        border = androidx.compose.foundation.BorderStroke(borderWidth, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected || isHighResource) 2.dp else 1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left-edge Visual Indicator Stripe for High Resource Consumption
            if (isHighResource) {
                val stripeColor = when (severity) {
                    ResourceSeverity.CRITICAL_BOTH, ResourceSeverity.CRITICAL_CPU -> SleekError
                    ResourceSeverity.CRITICAL_RAM -> SleekMemory
                    ResourceSeverity.ELEVATED_CPU -> SleekWarning
                    ResourceSeverity.ELEVATED_RAM -> SleekMemory.copy(alpha = 0.8f)
                    ResourceSeverity.NORMAL -> Color.Transparent
                }
                Box(
                    modifier = Modifier
                        .width(5.dp)
                        .height(88.dp)
                        .clip(RoundedCornerShape(topStart = 18.dp, bottomStart = 18.dp))
                        .background(stripeColor)
                        .testTag("high_resource_indicator_${process.pid}")
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(14.dp)
            ) {
                // Main Top Row: Checkbox / PID / Name / High Resource Badge / State / Terminate Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isMultiSelectMode) {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = { onToggleSelect() },
                            enabled = !process.isSelf && process.isTerminable,
                            colors = CheckboxDefaults.colors(
                                checkedColor = SleekPrimary,
                                uncheckedColor = SleekBorder
                            ),
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }

                    // PID Badge (soft surface variant rounded pill)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                when {
                                    severity == ResourceSeverity.CRITICAL_CPU || severity == ResourceSeverity.CRITICAL_BOTH -> SleekErrorContainer
                                    severity == ResourceSeverity.CRITICAL_RAM -> SleekMemoryContainer
                                    else -> SleekSurfaceVariant
                                }
                            )
                            .padding(horizontal = 7.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = process.pid.toString(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            ),
                            color = when {
                                severity == ResourceSeverity.CRITICAL_CPU || severity == ResourceSeverity.CRITICAL_BOTH -> SleekError
                                severity == ResourceSeverity.CRITICAL_RAM -> SleekMemory
                                else -> SleekPrimary
                            }
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    // Title & Dynamic Resource Badges / Category / User
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onOpenDetail() }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = process.displayTitle,
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                ),
                                color = SleekOnBackground,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            if (process.isSelf) {
                                Text(
                                    text = "(Self)",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                    color = SleekSuccess
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // User & Type
                            Text(
                                text = "${process.user} • ${process.type.label}",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                color = SleekTextMuted
                            )
                            // State badge
                            ProcessStateBadge(state = process.state)

                            // Dynamic High Resource Alert Badges
                            when (severity) {
                                ResourceSeverity.CRITICAL_BOTH -> {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(SleekErrorContainer.copy(alpha = alertAlpha))
                                            .border(1.dp, SleekError.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                            .padding(horizontal = 6.dp, vertical = 1.dp)
                                            .testTag("high_resource_badge_${process.pid}")
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Warning,
                                                contentDescription = "Heavy Load",
                                                tint = SleekError,
                                                modifier = Modifier.size(11.dp)
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(
                                                text = "HEAVY LOAD",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.ExtraBold
                                                ),
                                                color = SleekError
                                            )
                                        }
                                    }
                                }
                                ResourceSeverity.CRITICAL_CPU -> {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(SleekErrorContainer.copy(alpha = alertAlpha))
                                            .border(1.dp, SleekError.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                            .padding(horizontal = 6.dp, vertical = 1.dp)
                                            .testTag("high_cpu_badge_${process.pid}")
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Whatshot,
                                                contentDescription = "High CPU",
                                                tint = SleekError,
                                                modifier = Modifier.size(11.dp)
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(
                                                text = "HIGH CPU",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.ExtraBold
                                                ),
                                                color = SleekError
                                            )
                                        }
                                    }
                                }
                                ResourceSeverity.CRITICAL_RAM -> {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(SleekMemoryContainer.copy(alpha = alertAlpha))
                                            .border(1.dp, SleekMemory.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                            .padding(horizontal = 6.dp, vertical = 1.dp)
                                            .testTag("high_ram_badge_${process.pid}")
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Memory,
                                                contentDescription = "High RAM",
                                                tint = SleekMemory,
                                                modifier = Modifier.size(11.dp)
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(
                                                text = "HIGH RAM",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.ExtraBold
                                                ),
                                                color = SleekMemory
                                            )
                                        }
                                    }
                                }
                                ResourceSeverity.ELEVATED_CPU -> {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(SleekWarningContainer)
                                            .padding(horizontal = 5.dp, vertical = 1.dp)
                                            .testTag("elevated_cpu_badge_${process.pid}")
                                    ) {
                                        Text(
                                            text = "CPU LOAD",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = SleekWarning
                                        )
                                    }
                                }
                                ResourceSeverity.ELEVATED_RAM -> {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(SleekMemoryContainer.copy(alpha = 0.5f))
                                            .padding(horizontal = 5.dp, vertical = 1.dp)
                                            .testTag("elevated_ram_badge_${process.pid}")
                                    ) {
                                        Text(
                                            text = "RAM LOAD",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = SleekMemory
                                        )
                                    }
                                }
                                ResourceSeverity.NORMAL -> {}
                            }
                        }
                    }

                    // CPU & Memory metrics with visual proportional mini meters
                    Column(
                        horizontalAlignment = Alignment.End,
                        modifier = Modifier.padding(horizontal = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (severity == ResourceSeverity.CRITICAL_CPU || severity == ResourceSeverity.CRITICAL_BOTH) {
                                Icon(
                                    imageVector = Icons.Default.Whatshot,
                                    contentDescription = "High CPU Alert",
                                    tint = SleekError,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                            }
                            Text(
                                text = String.format(java.util.Locale.US, "%.1f%% CPU", process.cpuPercent),
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = when {
                                    process.cpuPercent >= 6.0 -> SleekError
                                    process.cpuPercent >= 2.0 -> SleekWarning
                                    else -> SleekOnBackground
                                }
                            )
                        }

                        // CPU mini meter bar
                        Box(
                            modifier = Modifier
                                .width(56.dp)
                                .height(3.dp)
                                .clip(CircleShape)
                                .background(SleekBorderLight)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth((process.cpuPercent / 15.0).toFloat().coerceIn(0.05f, 1f))
                                    .fillMaxHeight()
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            process.cpuPercent >= 6.0 -> SleekError
                                            process.cpuPercent >= 2.0 -> SleekWarning
                                            else -> SleekPrimary
                                        }
                                    )
                            )
                        }

                        Spacer(modifier = Modifier.height(3.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (severity == ResourceSeverity.CRITICAL_RAM || severity == ResourceSeverity.CRITICAL_BOTH) {
                                Icon(
                                    imageVector = Icons.Default.Memory,
                                    contentDescription = "High RAM Alert",
                                    tint = SleekMemory,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                            }
                            Text(
                                text = process.formattedMemory,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    fontWeight = if (process.memoryBytes >= 80L * 1024L * 1024L) FontWeight.Bold else FontWeight.Normal
                                ),
                                color = if (process.memoryBytes >= 80L * 1024L * 1024L) SleekMemory else SleekTextMuted
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // Single-Click Terminate Button (Matching HTML Sleek button: bg-[#F9DEDC] text-[#B3261E] rounded-full)
                    if (!process.isSelf && process.isTerminable) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(SleekErrorContainer)
                                .clickable { onTerminate() }
                                .testTag("terminate_button_${process.pid}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Terminate Task",
                                tint = SleekError,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .height(30.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(SleekSurfaceVariant)
                                .padding(horizontal = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (process.isSelf) "App" else "Locked",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = SleekTextSubtle
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Command Line Preview Banner & Expand Button (Sleek Surface Variant)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SleekSurfaceVariant)
                        .clickable { onToggleExpand() }
                        .padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Terminal,
                            contentDescription = "Command Line",
                            tint = SleekPrimary,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = process.cmdline.ifBlank { process.name },
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp
                            ),
                            color = SleekOnBackground.copy(alpha = 0.85f),
                            maxLines = if (isExpanded) 8 else 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onCopyCmd,
                            modifier = Modifier
                                .size(24.dp)
                                .testTag("copy_cmd_${process.pid}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy Command Line",
                                tint = SleekTextMuted,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (isExpanded) "Collapse" else "Expand",
                            tint = SleekTextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Expanded Full Command Line Details
                AnimatedVisibility(visible = isExpanded) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(SleekSurfaceSelected)
                            .border(1.dp, SleekPrimaryBorder, RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "FULL COMMAND LINE & ARGS:",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                letterSpacing = 0.8.sp
                            ),
                            color = SleekOnPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = process.cmdline.ifBlank { "No command line arguments found" },
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                lineHeight = 16.sp
                            ),
                            color = SleekOnPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "PPID: ${process.ppid} | Threads: ${process.threadsCount} | VSZ: ${process.formattedVsz}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp
                                ),
                                color = SleekTextMuted
                            )
                            OutlinedButton(
                                onClick = onOpenDetail,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Text("Full Inspector", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CompactProcessRow(
    process: ProcessInfo,
    isExpanded: Boolean,
    isSelected: Boolean,
    isMultiSelectMode: Boolean,
    onToggleSelect: () -> Unit,
    onToggleExpand: () -> Unit,
    onTerminate: () -> Unit,
    onOpenDetail: () -> Unit,
    onCopyCmd: () -> Unit,
    modifier: Modifier = Modifier
) {
    val severity = getProcessResourceSeverity(process)
    val isHighResource = severity != ResourceSeverity.NORMAL

    val containerBg = when {
        isSelected -> SleekSurfaceSelected
        severity == ResourceSeverity.CRITICAL_BOTH || severity == ResourceSeverity.CRITICAL_CPU -> Color(0xFFFFF7F6)
        severity == ResourceSeverity.CRITICAL_RAM -> Color(0xFFF3FBFB)
        severity == ResourceSeverity.ELEVATED_CPU -> Color(0xFFFFFBF2)
        severity == ResourceSeverity.ELEVATED_RAM -> Color(0xFFF7FCFC)
        else -> SleekSurface
    }

    val borderColor = when {
        isSelected -> SleekPrimaryBorder
        severity == ResourceSeverity.CRITICAL_BOTH || severity == ResourceSeverity.CRITICAL_CPU -> SleekError.copy(alpha = 0.7f)
        severity == ResourceSeverity.CRITICAL_RAM -> SleekMemory.copy(alpha = 0.7f)
        severity == ResourceSeverity.ELEVATED_CPU -> SleekWarning.copy(alpha = 0.5f)
        severity == ResourceSeverity.ELEVATED_RAM -> SleekMemory.copy(alpha = 0.4f)
        else -> SleekBorderLight
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("compact_row_${process.pid}"),
        shape = RoundedCornerShape(12.dp),
        color = containerBg,
        border = androidx.compose.foundation.BorderStroke(
            if (isSelected || isHighResource) 1.5.dp else 1.dp,
            borderColor
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left stripe indicator
            if (isHighResource) {
                val stripeColor = when (severity) {
                    ResourceSeverity.CRITICAL_BOTH, ResourceSeverity.CRITICAL_CPU -> SleekError
                    ResourceSeverity.CRITICAL_RAM -> SleekMemory
                    ResourceSeverity.ELEVATED_CPU -> SleekWarning
                    ResourceSeverity.ELEVATED_RAM -> SleekMemory.copy(alpha = 0.8f)
                    ResourceSeverity.NORMAL -> Color.Transparent
                }
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(48.dp)
                        .clip(RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp))
                        .background(stripeColor)
                        .testTag("compact_high_resource_${process.pid}")
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isMultiSelectMode) {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = { onToggleSelect() },
                            enabled = !process.isSelf && process.isTerminable,
                            colors = CheckboxDefaults.colors(
                                checkedColor = SleekPrimary,
                                uncheckedColor = SleekBorder
                            ),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }

                    // PID
                    Text(
                        text = process.pid.toString(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        ),
                        color = when {
                            severity == ResourceSeverity.CRITICAL_CPU || severity == ResourceSeverity.CRITICAL_BOTH -> SleekError
                            severity == ResourceSeverity.CRITICAL_RAM -> SleekMemory
                            else -> SleekPrimary
                        },
                        modifier = Modifier.width(52.dp)
                    )

                    // Name, Type & High Resource icon
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onOpenDetail() }
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = process.displayTitle,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = SleekOnBackground,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (severity == ResourceSeverity.CRITICAL_CPU || severity == ResourceSeverity.CRITICAL_BOTH) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.Whatshot,
                                    contentDescription = "High CPU",
                                    tint = SleekError,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                            if (severity == ResourceSeverity.CRITICAL_RAM) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.Memory,
                                    contentDescription = "High RAM",
                                    tint = SleekMemory,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                        Text(
                            text = "${process.user} • ${process.type.label}",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = SleekTextMuted
                        )
                    }

                    // CPU with mini meter
                    Column(
                        modifier = Modifier.width(58.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = String.format(java.util.Locale.US, "%.1f%%", process.cpuPercent),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            ),
                            color = when {
                                process.cpuPercent >= 6.0 -> SleekError
                                process.cpuPercent >= 2.0 -> SleekWarning
                                else -> SleekOnBackground
                            }
                        )
                        Box(
                            modifier = Modifier
                                .width(40.dp)
                                .height(2.5.dp)
                                .clip(CircleShape)
                                .background(SleekBorderLight)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth((process.cpuPercent / 15.0).toFloat().coerceIn(0.05f, 1f))
                                    .fillMaxHeight()
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            process.cpuPercent >= 6.0 -> SleekError
                                            process.cpuPercent >= 2.0 -> SleekWarning
                                            else -> SleekPrimary
                                        }
                                    )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // RAM
                    Text(
                        text = process.formattedMemory,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            fontWeight = if (process.memoryBytes >= 80L * 1024L * 1024L) FontWeight.Bold else FontWeight.Normal
                        ),
                        color = if (process.memoryBytes >= 80L * 1024L * 1024L) SleekMemory else SleekTextMuted,
                        modifier = Modifier.width(62.dp)
                    )

                    // Terminate Action
                    if (!process.isSelf && process.isTerminable) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(SleekErrorContainer)
                                .clickable { onTerminate() }
                                .testTag("compact_kill_${process.pid}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Kill Task",
                                tint = SleekError,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.width(28.dp))
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // Expand command line
                    IconButton(
                        onClick = onToggleExpand,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "Expand Command",
                            tint = SleekTextMuted,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                AnimatedVisibility(visible = isExpanded) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(SleekSurfaceVariant)
                            .padding(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = process.cmdline.ifBlank { process.name },
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp
                            ),
                            color = SleekOnBackground.copy(alpha = 0.85f),
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = onCopyCmd,
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy",
                                tint = SleekTextMuted,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProcessStateBadge(state: ProcessState, modifier: Modifier = Modifier) {
    val (bgColor, textColor) = when (state) {
        ProcessState.RUNNING -> Pair(SleekSuccessContainer, SleekSuccess)
        ProcessState.SLEEPING -> Pair(SleekSurfaceVariant, SleekTextMuted)
        ProcessState.DISK_SLEEP -> Pair(SleekWarningContainer, SleekWarning)
        ProcessState.ZOMBIE -> Pair(SleekErrorContainer, SleekError)
        ProcessState.STOPPED -> Pair(SleekErrorContainer, SleekError)
        else -> Pair(SleekSurfaceVariant, SleekTextSubtle)
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(bgColor)
            .padding(horizontal = 5.dp, vertical = 1.dp)
    ) {
        Text(
            text = state.name,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium
            ),
            color = textColor
        )
    }
}
