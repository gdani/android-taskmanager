package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ProcessCategory
import com.example.data.model.ProcessInfo
import com.example.ui.theme.SleekBorder
import com.example.ui.theme.SleekBorderLight
import com.example.ui.theme.SleekError
import com.example.ui.theme.SleekErrorContainer
import com.example.ui.theme.SleekOnBackground
import com.example.ui.theme.SleekOnPrimaryContainer
import com.example.ui.theme.SleekPrimary
import com.example.ui.theme.SleekPrimaryBorder
import com.example.ui.theme.SleekPrimaryContainer
import com.example.ui.theme.SleekSecondaryContainer
import com.example.ui.theme.SleekSuccess
import com.example.ui.theme.SleekSurface
import com.example.ui.theme.SleekSurfaceVariant
import com.example.ui.theme.SleekTextMuted
import com.example.ui.theme.SleekWarning
import com.example.ui.theme.SleekWarningContainer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProcessDetailSheet(
    process: ProcessInfo,
    onDismiss: () -> Unit,
    onTerminate: (signal: String) -> Unit,
    onRestartApp: (ProcessInfo) -> Unit = {},
    onClearCache: (ProcessInfo) -> Unit = {},
    onToggleService: (ProcessInfo) -> Unit = {},
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    val context = LocalContext.current

    fun copyToClipboard(text: String, label: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "$label copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    fun openAppInfo(packageName: String) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(context, "Unable to open application info", Toast.LENGTH_SHORT).show()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SleekSurface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(SleekBorder)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
                .testTag("process_detail_sheet")
        ) {
            // Header: Icon/PID/Title/Close
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(SleekPrimaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = process.pid.toString(),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            ),
                            color = SleekOnPrimaryContainer
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = process.displayTitle,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp
                            ),
                            color = SleekOnBackground
                        )
                        Text(
                            text = "PID ${process.pid} • PPID ${process.ppid} • User: ${process.user}",
                            style = MaterialTheme.typography.bodySmall,
                            color = SleekTextMuted
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(SleekSurfaceVariant)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Sheet",
                        tint = SleekOnBackground,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (process.state == com.example.data.model.ProcessState.STOPPED) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SleekWarningContainer)
                        .border(1.dp, SleekWarning, RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "ℹ️", fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Process completed execution or is stopped. Snapshot retained for inspection.",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = SleekWarning
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // STORAGE & CACHE USAGE BREAKDOWN (Req #11)
            Text(
                text = "STORAGE, DATA & CACHE FOOTPRINT",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                ),
                color = SleekPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SleekSurfaceVariant)
                    .border(1.dp, SleekBorderLight, RoundedCornerShape(16.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricRow(label = "Application / Program Size", value = process.formattedCodeSize, valueColor = SleekOnBackground)
                MetricRow(label = "User & App Data Size", value = process.formattedDataSize, valueColor = SleekPrimary)
                MetricRow(label = "Temporary Cache Footprint", value = process.formattedCacheSize, valueColor = SleekWarning)
                MetricRow(label = "Total Storage Allocated", value = process.formattedTotalSize, valueColor = SleekSuccess)
            }

            Spacer(modifier = Modifier.height(14.dp))

            // APPLICATION & CACHE MANAGEMENT ACTIONS (Req #12, 13, 14)
            Text(
                text = "APPLICATION LIFECYCLE & STORAGE ACTIONS",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                ),
                color = SleekTextMuted
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Row 1: Restart Application & Clear Cache
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Restart Application (Req #13)
                ElevatedButton(
                    onClick = { onRestartApp(process) },
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .testTag("restart_app_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.elevatedButtonColors(
                        containerColor = SleekPrimaryContainer,
                        contentColor = SleekPrimary
                    )
                ) {
                    Icon(imageVector = Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Restart App", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                }

                // Clear Cache (Req #12)
                FilledTonalButton(
                    onClick = { onClearCache(process) },
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .testTag("clear_cache_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = SleekSecondaryContainer,
                        contentColor = SleekOnBackground
                    )
                ) {
                    Icon(imageVector = Icons.Default.CleaningServices, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Clear Cache", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Row 2: Delete Data / Storage Settings & Service Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Delete Application Data (Req #12)
                OutlinedButton(
                    onClick = {
                        if (process.packageName != null) {
                            openAppInfo(process.packageName)
                        } else {
                            Toast.makeText(context, "Cannot delete system process data directly", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .testTag("delete_data_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp), tint = SleekError)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Delete Data", color = SleekError, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                }

                // Service Toggle (if service or daemon) (Req #14)
                if (process.type == ProcessCategory.SERVICE || process.type == ProcessCategory.DAEMON || !process.isServiceEnabled) {
                    ElevatedButton(
                        onClick = { onToggleService(process) },
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .testTag("toggle_service_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.elevatedButtonColors(
                            containerColor = if (!process.isServiceEnabled) SleekPrimaryContainer else SleekErrorContainer,
                            contentColor = if (!process.isServiceEnabled) SleekPrimary else SleekError
                        )
                    ) {
                        Icon(
                            imageVector = if (!process.isServiceEnabled) Icons.Default.PlayCircle else Icons.Default.PauseCircle,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (!process.isServiceEnabled) "Enable Service" else "Disable Service",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // COMMAND LINE INSPECTION SECTION
            Text(
                text = "FULL COMMAND LINE & EXECUTABLE",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                ),
                color = SleekTextMuted
            )

            Spacer(modifier = Modifier.height(6.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(SleekSurfaceVariant)
                    .border(1.dp, SleekBorder, RoundedCornerShape(14.dp))
                    .padding(14.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Terminal,
                                contentDescription = null,
                                tint = SleekPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "cmdline",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = SleekPrimary
                            )
                        }

                        ElevatedButton(
                            onClick = { copyToClipboard(process.cmdline, "Command Line") },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .height(30.dp)
                                .testTag("copy_full_command_button"),
                            colors = ButtonDefaults.elevatedButtonColors(
                                containerColor = SleekPrimaryContainer,
                                contentColor = SleekOnPrimaryContainer
                            ),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy",
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Copy", style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold))
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = process.cmdline.ifBlank { process.name },
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            lineHeight = 18.sp
                        ),
                        color = SleekOnBackground
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // PROCESS METRICS GRID
            Text(
                text = "RESOURCE CONSUMPTION & ATTRIBUTES",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                ),
                color = SleekTextMuted
            )

            Spacer(modifier = Modifier.height(6.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SleekSurfaceVariant)
                    .border(1.dp, SleekBorderLight, RoundedCornerShape(16.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricRow(label = "CPU Usage", value = "${process.cpuPercent}%", valueColor = if (process.cpuPercent > 10) SleekError else SleekOnBackground)
                MetricRow(label = "Resident RAM (RSS)", value = process.formattedMemory, valueColor = SleekPrimary)
                MetricRow(label = "Virtual Memory (VSZ)", value = process.formattedVsz)
                MetricRow(label = "Process State", value = "${process.state.displayName} (${process.state.code})")
                MetricRow(label = "Thread Count", value = "${process.threadsCount} active threads")
                MetricRow(label = "Category Type", value = process.type.label)
                MetricRow(label = "Priority / Nice", value = "${process.priority} / ${process.nice}")
                MetricRow(label = "UID / User", value = "${process.uid} (${process.user})")
                if (process.packageName != null) {
                    MetricRow(label = "Package Identifier", value = process.packageName)
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // PROCESS TERMINATION ACTIONS
            Text(
                text = "PROCESS TERMINATION & SIGNALS",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                ),
                color = SleekError
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (!process.isSelf && process.isTerminable) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // SIGTERM
                    ElevatedButton(
                        onClick = { onTerminate("SIGTERM") },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("terminate_sigterm_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.elevatedButtonColors(
                            containerColor = SleekErrorContainer,
                            contentColor = SleekError
                        )
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Terminate Task", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                    }

                    // SIGKILL
                    OutlinedButton(
                        onClick = { onTerminate("SIGKILL") },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("terminate_sigkill_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = SleekError
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SleekError)
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp), tint = SleekError)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Force Kill", color = SleekError, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SleekSurfaceVariant)
                        .border(1.dp, SleekBorder, RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Shield, contentDescription = null, tint = SleekSuccess)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (process.isSelf) "This is the current ProcMaster process and cannot self-terminate." else "System protected process. Signal execution is restricted.",
                            style = MaterialTheme.typography.bodySmall,
                            color = SleekOnBackground
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MetricRow(label: String, value: String, valueColor: Color = SleekOnBackground) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = SleekTextMuted
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            ),
            color = valueColor
        )
    }
}
