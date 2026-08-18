package com.example.ui.components

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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.KillRecord
import com.example.ui.theme.SleekBorder
import com.example.ui.theme.SleekBorderLight
import com.example.ui.theme.SleekError
import com.example.ui.theme.SleekErrorContainer
import com.example.ui.theme.SleekOnBackground
import com.example.ui.theme.SleekOnErrorContainer
import com.example.ui.theme.SleekPrimary
import com.example.ui.theme.SleekSuccess
import com.example.ui.theme.SleekSurface
import com.example.ui.theme.SleekSurfaceVariant
import com.example.ui.theme.SleekTextMuted
import com.example.ui.theme.SleekTextSubtle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun KillHistoryDialog(
    history: List<KillRecord>,
    onDismiss: () -> Unit,
    onClearHistory: () -> Unit
) {
    val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("kill_history_dialog"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SleekSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, SleekBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(SleekErrorContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                tint = SleekError,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Termination Log",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 18.sp
                            ),
                            color = SleekOnBackground
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(SleekSurfaceVariant)
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = SleekOnBackground, modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                val totalFreed = history.sumOf { it.freedMemoryBytes }
                val totalFreedMb = totalFreed.toDouble() / (1024 * 1024)
                Text(
                    text = "${history.size} processes terminated • ~${String.format(Locale.US, "%.1f", totalFreedMb)} MB reclaimed",
                    style = MaterialTheme.typography.bodySmall,
                    color = SleekSuccess
                )

                Spacer(modifier = Modifier.height(14.dp))

                if (history.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No processes terminated yet in this session.",
                            style = MaterialTheme.typography.bodySmall,
                            color = SleekTextMuted
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(history) { record ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(SleekSurfaceVariant)
                                    .border(1.dp, SleekBorderLight, RoundedCornerShape(12.dp))
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = record.processName,
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = SleekOnBackground
                                    )
                                    Text(
                                        text = "PID ${record.pid} • Signal: ${record.signalName}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                        color = SleekTextMuted
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "-${record.formattedFreedMemory}",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = SleekSuccess
                                    )
                                    Text(
                                        text = timeFormat.format(Date(record.timestamp)),
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        color = SleekTextSubtle
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (history.isNotEmpty()) {
                        OutlinedButton(
                            onClick = onClearHistory,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("clear_history_button"),
                            border = androidx.compose.foundation.BorderStroke(1.dp, SleekBorder)
                        ) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp), tint = SleekError)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Clear Log", style = MaterialTheme.typography.labelSmall, color = SleekError)
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SleekBorder)
                    ) {
                        Text("Done", color = SleekTextMuted)
                    }
                }
            }
        }
    }
}
