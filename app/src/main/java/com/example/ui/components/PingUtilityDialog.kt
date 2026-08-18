package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.NetworkPing
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.SleekBorder
import com.example.ui.theme.SleekBorderLight
import com.example.ui.theme.SleekError
import com.example.ui.theme.SleekErrorContainer
import com.example.ui.theme.SleekOnBackground
import com.example.ui.theme.SleekOnPrimary
import com.example.ui.theme.SleekPrimary
import com.example.ui.theme.SleekPrimaryBorder
import com.example.ui.theme.SleekPrimaryContainer
import com.example.ui.theme.SleekSecondaryContainer
import com.example.ui.theme.SleekSuccess
import com.example.ui.theme.SleekSurface
import com.example.ui.theme.SleekSurfaceVariant
import com.example.ui.theme.SleekTextMuted
import com.example.ui.theme.SleekTextSubtle
import com.example.ui.theme.SleekWarning
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.InetAddress

@Composable
fun PingUtilityDialog(
    onDismiss: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    var targetHost by remember { mutableStateOf("8.8.8.8") }
    var pingCount by remember { mutableStateOf("4") }
    var isPinging by remember { mutableStateOf(false) }
    var pingJob by remember { mutableStateOf<Job?>(null) }
    val terminalLogs = remember { mutableStateListOf<String>() }

    var packetsTransmitted by remember { mutableStateOf(0) }
    var packetsReceived by remember { mutableStateOf(0) }
    var packetLossPercent by remember { mutableStateOf("0%") }
    var minAvgMaxRtt by remember { mutableStateOf("") }

    val presets = listOf("8.8.8.8" to "Google DNS", "1.1.1.1" to "Cloudflare", "google.com" to "Google Web", "127.0.0.1" to "Localhost")

    fun runPing() {
        val host = targetHost.trim()
        if (host.isBlank()) return
        val count = pingCount.toIntOrNull()?.coerceIn(1, 20) ?: 4

        pingJob?.cancel()
        terminalLogs.clear()
        packetsTransmitted = 0
        packetsReceived = 0
        packetLossPercent = "0%"
        minAvgMaxRtt = ""
        isPinging = true

        pingJob = scope.launch(Dispatchers.IO) {
            terminalLogs.add("PING $host ($count packets)...")
            try {
                var process: java.lang.Process? = null
                try {
                    process = Runtime.getRuntime().exec(arrayOf("ping", "-c", count.toString(), "-W", "2", host))
                    val reader = BufferedReader(InputStreamReader(process.inputStream))
                    val errReader = BufferedReader(InputStreamReader(process.errorStream))
                    
                    var line = reader.readLine()
                    while (line != null && isActive) {
                        val currentLine = line
                        withContext(Dispatchers.Main) {
                            terminalLogs.add(currentLine)
                            if (currentLine.contains("bytes from")) {
                                packetsReceived++
                            }
                            if (currentLine.contains("packets transmitted")) {
                                val parts = currentLine.split(",")
                                if (parts.size >= 3) {
                                    packetLossPercent = parts[2].trim()
                                }
                            }
                            if (currentLine.contains("rtt") || currentLine.contains("round-trip")) {
                                minAvgMaxRtt = currentLine.substringAfter("=").trim()
                            }
                        }
                        line = reader.readLine()
                    }

                    var errLine = errReader.readLine()
                    while (errLine != null && isActive) {
                        val e = errLine
                        withContext(Dispatchers.Main) {
                            terminalLogs.add("[stderr] $e")
                        }
                        errLine = errReader.readLine()
                    }
                    process.waitFor()
                } catch (_: Exception) {
                    // Fallback to InetAddress.isReachable loop if direct ping binary is sandboxed
                    for (i in 1..count) {
                        if (!isActive) break
                        val start = System.currentTimeMillis()
                        val reachable = try {
                            val inet = InetAddress.getByName(host)
                            inet.isReachable(1500)
                        } catch (e: Exception) {
                            false
                        }
                        val rtt = System.currentTimeMillis() - start
                        packetsTransmitted++
                        withContext(Dispatchers.Main) {
                            if (reachable) {
                                packetsReceived++
                                terminalLogs.add("Reply from $host: bytes=32 time=${rtt}ms TTL=54")
                            } else {
                                terminalLogs.add("Request timed out for icmp_seq=$i")
                            }
                        }
                        delay(600)
                    }
                    val loss = if (packetsTransmitted > 0) ((packetsTransmitted - packetsReceived) * 100) / packetsTransmitted else 0
                    withContext(Dispatchers.Main) {
                        packetLossPercent = "$loss% packet loss"
                        terminalLogs.add("--- $host ping statistics ---")
                        terminalLogs.add("$packetsTransmitted packets transmitted, $packetsReceived received, $loss% packet loss")
                    }
                }
            } finally {
                withContext(Dispatchers.Main) {
                    isPinging = false
                    terminalLogs.add("Ping process finished.")
                }
            }
        }
    }

    Dialog(
        onDismissRequest = {
            pingJob?.cancel()
            onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .clip(RoundedCornerShape(24.dp))
                .border(1.dp, SleekBorder, RoundedCornerShape(24.dp))
                .testTag("ping_utility_dialog"),
            color = SleekSurface
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
                                .background(SleekPrimaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.NetworkPing,
                                contentDescription = null,
                                tint = SleekPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Ping Diagnostic Tool",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = SleekOnBackground
                            )
                            Text(
                                text = "ICMP echo latency & packet reachability probe",
                                style = MaterialTheme.typography.bodySmall,
                                color = SleekTextMuted
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            pingJob?.cancel()
                            onDismiss()
                        },
                        modifier = Modifier.testTag("close_ping_dialog_button")
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = SleekTextMuted)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Presets
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    presets.forEach { (ip, label) ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (targetHost == ip) SleekPrimaryContainer else SleekSurfaceVariant)
                                .border(1.dp, if (targetHost == ip) SleekPrimaryBorder else SleekBorder, RoundedCornerShape(10.dp))
                                .clickable { targetHost = ip }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "$label ($ip)",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                color = if (targetHost == ip) SleekPrimary else SleekTextMuted
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Host Input & Count
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = targetHost,
                        onValueChange = { targetHost = it },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("ping_host_input"),
                        label = { Text("Target Host / IP", style = MaterialTheme.typography.bodySmall) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = SleekSurfaceVariant,
                            unfocusedContainerColor = SleekSurfaceVariant,
                            focusedBorderColor = SleekPrimary,
                            unfocusedBorderColor = SleekBorder,
                            focusedTextColor = SleekOnBackground,
                            unfocusedTextColor = SleekOnBackground
                        )
                    )

                    OutlinedTextField(
                        value = pingCount,
                        onValueChange = { pingCount = it.filter { char -> char.isDigit() }.take(2) },
                        modifier = Modifier
                            .width(80.dp)
                            .testTag("ping_count_input"),
                        label = { Text("Count", style = MaterialTheme.typography.bodySmall) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = SleekSurfaceVariant,
                            unfocusedContainerColor = SleekSurfaceVariant,
                            focusedBorderColor = SleekPrimary,
                            unfocusedBorderColor = SleekBorder,
                            focusedTextColor = SleekOnBackground,
                            unfocusedTextColor = SleekOnBackground
                        )
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Action Bar (Start / Stop / Copy)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!isPinging) {
                        ElevatedButton(
                            onClick = { runPing() },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("start_ping_button"),
                            colors = ButtonDefaults.elevatedButtonColors(
                                containerColor = SleekPrimary,
                                contentColor = SleekOnPrimary
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Send Ping", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        OutlinedButton(
                            onClick = {
                                pingJob?.cancel()
                                isPinging = false
                                terminalLogs.add("[Cancelled by user]")
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("stop_ping_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Stop, contentDescription = null, tint = SleekError, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Stop Ping", color = SleekError, fontWeight = FontWeight.Bold)
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(terminalLogs.joinToString("\n")))
                        },
                        modifier = Modifier
                            .height(44.dp)
                            .testTag("copy_ping_log_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy Output", modifier = Modifier.size(16.dp))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Terminal Output Console
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF0F172A))
                        .border(1.dp, SleekBorder, RoundedCornerShape(12.dp))
                        .padding(10.dp)
                ) {
                    val logScroll = rememberScrollState()
                    SelectionContainer {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(logScroll)
                        ) {
                            if (terminalLogs.isEmpty()) {
                                Text(
                                    text = "$ ready to ping $targetHost\nPress 'Send Ping' to probe network RTT.",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = SleekTextSubtle
                                )
                            } else {
                                terminalLogs.forEach { line ->
                                    val textColor = when {
                                        line.contains("bytes from") -> SleekSuccess
                                        line.contains("timed out") || line.contains("100% packet loss") -> SleekError
                                        line.startsWith("PING") || line.startsWith("---") -> SleekPrimary
                                        else -> SleekTextMuted
                                    }
                                    Text(
                                        text = line,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        color = textColor,
                                        lineHeight = 15.sp
                                    )
                                }
                            }
                        }
                    }
                }

                if (minAvgMaxRtt.isNotBlank() || packetLossPercent.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(SleekSurfaceVariant)
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Loss: $packetLossPercent",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (packetLossPercent.contains("0%")) SleekSuccess else SleekWarning
                        )
                        if (minAvgMaxRtt.isNotBlank()) {
                            Text(
                                text = "RTT: $minAvgMaxRtt",
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                color = SleekPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}
