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
import androidx.compose.material.icons.filled.OpenInNew
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ProcessCategory
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
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(SleekSurfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Terminal,
                        contentDescription = null,
                        tint = SleekPrimary,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
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
                Spacer(modifier = Modifier.height(14.dp))
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
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(
                items = processes,
                key = { it.pid }
            ) { process ->
                val isExpanded = expandedCommandPids[process.pid] == true
                val isSelected = selectedPids.contains(process.pid)

                SleekProcessRowItem(
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
            item {
                Spacer(modifier = Modifier.height(24.dp))
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
            .padding(horizontal = 16.dp, vertical = 2.dp),
        shape = RoundedCornerShape(10.dp),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isMultiSelectMode) {
                Spacer(modifier = Modifier.width(32.dp))
            }

            // PID Header
            SortableHeaderItem(
                title = "PID",
                column = ProcessSortColumn.PID,
                currentSort = sortColumn,
                isAscending = isSortAscending,
                onSort = onSortColumn,
                modifier = Modifier.width(54.dp)
            )

            // Name Header
            SortableHeaderItem(
                title = "PROCESS / APP",
                column = ProcessSortColumn.NAME,
                currentSort = sortColumn,
                isAscending = isSortAscending,
                onSort = onSortColumn,
                modifier = Modifier.weight(1f)
            )

            // CPU Header
            SortableHeaderItem(
                title = "CPU",
                column = ProcessSortColumn.CPU,
                currentSort = sortColumn,
                isAscending = isSortAscending,
                onSort = onSortColumn,
                modifier = Modifier.width(56.dp)
            )

            // RAM Header
            SortableHeaderItem(
                title = "RAM",
                column = ProcessSortColumn.MEMORY,
                currentSort = sortColumn,
                isAscending = isSortAscending,
                onSort = onSortColumn,
                modifier = Modifier.width(62.dp)
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
                modifier = Modifier.width(40.dp)
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
                modifier = Modifier.size(11.dp)
            )
        }
    }
}

@Composable
fun SleekProcessRowItem(
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
        severity == ResourceSeverity.CRITICAL_BOTH || severity == ResourceSeverity.CRITICAL_CPU -> Color(0xFFFFF6F5)
        severity == ResourceSeverity.CRITICAL_RAM -> Color(0xFFF2FBFA)
        severity == ResourceSeverity.ELEVATED_CPU -> Color(0xFFFFFDF5)
        else -> SleekSurface
    }

    val borderColor = when {
        isSelected -> SleekPrimaryBorder
        severity == ResourceSeverity.CRITICAL_BOTH || severity == ResourceSeverity.CRITICAL_CPU -> SleekError.copy(alpha = 0.6f)
        severity == ResourceSeverity.CRITICAL_RAM -> SleekMemory.copy(alpha = 0.6f)
        severity == ResourceSeverity.ELEVATED_CPU -> SleekWarning.copy(alpha = 0.4f)
        else -> SleekBorderLight
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("process_card_${process.pid}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = containerBg),
        border = androidx.compose.foundation.BorderStroke(if (isSelected || isHighResource) 1.5.dp else 1.dp, borderColor)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Main Compact Line: PID | Name + user/category | CPU% | RAM | Kill
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpand() }
                    .padding(horizontal = 10.dp, vertical = 7.dp),
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

                // PID Badge
                Box(
                    modifier = Modifier
                        .width(48.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            when {
                                severity == ResourceSeverity.CRITICAL_CPU || severity == ResourceSeverity.CRITICAL_BOTH -> SleekErrorContainer
                                severity == ResourceSeverity.CRITICAL_RAM -> SleekMemoryContainer
                                else -> SleekSurfaceVariant
                            }
                        )
                        .padding(horizontal = 4.dp, vertical = 3.dp),
                    contentAlignment = Alignment.Center
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

                Spacer(modifier = Modifier.width(8.dp))

                // Title & Subtitle (User • Category • State)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onOpenDetail() }
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = process.displayTitle,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            ),
                            color = SleekOnBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (process.isSelf) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "(Self)",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = SleekSuccess
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "${process.user} • ${process.type.label}",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = SleekTextMuted
                        )
                        ProcessStateBadge(state = process.state)
                    }
                }

                // CPU with mini meter
                Column(
                    modifier = Modifier.width(54.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = String.format(java.util.Locale.US, "%.1f%%", process.cpuPercent),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        ),
                        color = when {
                            process.cpuPercent >= 6.0 -> SleekError
                            process.cpuPercent >= 2.0 -> SleekWarning
                            else -> SleekOnBackground
                        }
                    )
                    Box(
                        modifier = Modifier
                            .width(44.dp)
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
                        fontSize = 11.sp,
                        fontWeight = if (process.memoryBytes >= 80L * 1024L * 1024L) FontWeight.Bold else FontWeight.Normal
                    ),
                    color = if (process.memoryBytes >= 80L * 1024L * 1024L) SleekMemory else SleekTextMuted,
                    modifier = Modifier.width(60.dp)
                )

                Spacer(modifier = Modifier.width(4.dp))

                // Action Terminate Button
                if (!process.isSelf && process.isTerminable) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
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
                            modifier = Modifier.size(16.dp)
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(SleekSurfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "🔒",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp)
                        )
                    }
                }
            }

            // Expandable Command Line & Actions Drawer
            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SleekSurfaceVariant.copy(alpha = 0.5f))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "CMD: ${process.cmdline.ifBlank { process.name }}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp
                            ),
                            color = SleekOnBackground.copy(alpha = 0.9f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            IconButton(
                                onClick = onCopyCmd,
                                modifier = Modifier
                                    .size(28.dp)
                                    .testTag("copy_cmd_${process.pid}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy Command",
                                    tint = SleekTextMuted,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            IconButton(
                                onClick = onOpenDetail,
                                modifier = Modifier
                                    .size(28.dp)
                                    .testTag("open_detail_${process.pid}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.OpenInNew,
                                    contentDescription = "Full Inspector",
                                    tint = SleekPrimary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProcessStateBadge(state: ProcessState, modifier: Modifier = Modifier) {
    val (bg, textColor, label) = when (state) {
        ProcessState.RUNNING -> Triple(SleekSuccessContainer, SleekSuccess, "R")
        ProcessState.SLEEPING -> Triple(SleekSurfaceVariant, SleekTextMuted, "S")
        ProcessState.DISK_SLEEP -> Triple(SleekWarningContainer, SleekWarning, "D")
        ProcessState.ZOMBIE -> Triple(SleekErrorContainer, SleekError, "Z")
        ProcessState.STOPPED -> Triple(SleekErrorContainer, SleekError, "T")
        ProcessState.PAGING -> Triple(SleekWarningContainer, SleekWarning, "W")
        ProcessState.IDLE -> Triple(SleekSurfaceVariant, SleekTextMuted, "I")
        ProcessState.UNKNOWN -> Triple(SleekSurfaceVariant, SleekTextMuted, "?")
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(bg)
            .padding(horizontal = 4.dp, vertical = 1.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            ),
            color = textColor
        )
    }
}
