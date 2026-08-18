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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Shield
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
import com.example.data.model.ProcessInfo
import com.example.ui.theme.SleekBorder
import com.example.ui.theme.SleekBorderLight
import com.example.ui.theme.SleekError
import com.example.ui.theme.SleekErrorContainer
import com.example.ui.theme.SleekOnBackground
import com.example.ui.theme.SleekOnErrorContainer
import com.example.ui.theme.SleekOnPrimary
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProcessDetailSheet(
    process: ProcessInfo,
    onDismiss: () -> Unit,
    onTerminate: (signal: String) -> Unit,
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

            Spacer(modifier = Modifier.height(18.dp))

            // COMMAND LINE INSPECTION SECTION (Primary User Requirement)
            Text(
                text = "FULL COMMAND LINE & EXECUTABLE",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                ),
                color = SleekPrimary
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

                    Spacer(modifier = Modifier.height(10.dp))

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

            Spacer(modifier = Modifier.height(18.dp))

            // PROCESS METRICS GRID
            Text(
                text = "RESOURCE CONSUMPTION & ATTRIBUTES",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                ),
                color = SleekTextMuted
            )

            Spacer(modifier = Modifier.height(8.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SleekSurfaceVariant)
                    .border(1.dp, SleekBorderLight, RoundedCornerShape(16.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
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

            Spacer(modifier = Modifier.height(20.dp))

            // ACTIONS: One-Click Termination and Signals
            Text(
                text = "PROCESS CONTROL & ACTIONS",
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
                    // SIGTERM Single-Click Termination
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

                    // SIGKILL Force Kill
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

            // Optional: App info in Android Settings if available
            if (process.packageName != null && process.packageName.startsWith("com.")) {
                Spacer(modifier = Modifier.height(10.dp))
                FilledTonalButton(
                    onClick = { openAppInfo(process.packageName) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = SleekSecondaryContainer,
                        contentColor = SleekOnBackground
                    )
                ) {
                    Icon(imageVector = Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Manage in Android App Settings")
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
