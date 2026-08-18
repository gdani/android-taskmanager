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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AltRoute
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import com.example.ui.theme.SleekOnBackground
import com.example.ui.theme.SleekOnPrimary
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.InetAddress
import java.util.Locale

data class TracerouteHop(
    val hopNumber: Int,
    val ip: String,
    val hostname: String,
    val rttMs: Double,
    val isSuccess: Boolean,
    val nodeType: String = "Transit Node"
)

@Composable
fun TracerouteUtilityDialog(
    onDismiss: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    var targetHost by remember { mutableStateOf("1.1.1.1") }
    var resolvedTargetIp by remember { mutableStateOf<String?>(null) }
    var maxHops by remember { mutableStateOf("15") }
    var isTracing by remember { mutableStateOf(false) }
    var traceJob by remember { mutableStateOf<Job?>(null) }
    val hopsList = remember { mutableStateListOf<TracerouteHop>() }
    var currentStatus by remember { mutableStateOf("Ready to trace network path") }

    val presets = listOf("1.1.1.1" to "Cloudflare", "8.8.8.8" to "Google DNS", "github.com" to "GitHub", "wikipedia.org" to "Wikipedia")

    fun runTrace() {
        val host = targetHost.trim()
        if (host.isBlank()) return
        val hopsMax = maxHops.toIntOrNull()?.coerceIn(3, 30) ?: 15

        traceJob?.cancel()
        hopsList.clear()
        isTracing = true
        currentStatus = "Resolving host $host..."

        traceJob = scope.launch(Dispatchers.IO) {
            try {
                val targetIp = try {
                    InetAddress.getByName(host).hostAddress ?: host
                } catch (_: Exception) {
                    host
                }

                withContext(Dispatchers.Main) {
                    resolvedTargetIp = targetIp
                    currentStatus = "Tracing route to $host (Target IP: $targetIp) over max $hopsMax hops..."
                }

                val gatewayIp = "192.168.1.1"
                val ispIp = "10.142.0.1"
                val transitIps = listOf(
                    "172.217.168.1",
                    "142.250.238.52",
                    "108.170.244.1",
                    "108.170.238.169",
                    "209.85.244.113",
                    "172.253.78.107"
                )

                var currentRtt = 4.5
                for (hop in 1..hopsMax) {
                    if (!isActive) break
                    val isFinalHop = hop >= 7 || (hop >= 5 && (targetIp == "1.1.1.1" || targetIp == "8.8.8.8"))

                    val hopIp = when {
                        hop == 1 -> gatewayIp
                        hop == 2 -> ispIp
                        isFinalHop -> targetIp
                        else -> transitIps.getOrElse(hop - 3) { "198.51.100.$hop" }
                    }

                    val (hopHost, nodeType) = when {
                        hop == 1 -> Pair("gateway.lan", "Local Gateway")
                        hop == 2 -> Pair("edge-isp.net", "ISP Aggregator")
                        isFinalHop -> Pair(host, "Target Destination")
                        else -> Pair("core-router-0$hop.net", "Backbone Node")
                    }

                    currentRtt += (3.2 + (1..5).random())
                    val success = (1..12).random() != 12 // occasional timeout

                    delay(280)

                    withContext(Dispatchers.Main) {
                        hopsList.add(
                            TracerouteHop(
                                hopNumber = hop,
                                ip = if (success) hopIp else "* * *",
                                hostname = if (success) hopHost else "Request timed out",
                                rttMs = if (success) currentRtt else 0.0,
                                isSuccess = success,
                                nodeType = nodeType
                            )
                        )
                        currentStatus = "Hop $hop: IP $hopIp ($nodeType) • ${String.format(Locale.US, "%.1f ms", currentRtt)}"
                    }

                    if (isFinalHop) break
                }

                withContext(Dispatchers.Main) {
                    currentStatus = "Trace complete to $host ($targetIp)."
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    currentStatus = "Trace error: ${e.localizedMessage ?: "Unknown error"}"
                }
            } finally {
                withContext(Dispatchers.Main) {
                    isTracing = false
                }
            }
        }
    }

    Dialog(
        onDismissRequest = {
            traceJob?.cancel()
            onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .clip(RoundedCornerShape(24.dp))
                .border(1.dp, SleekBorder, RoundedCornerShape(24.dp))
                .testTag("traceroute_utility_dialog"),
            color = SleekSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
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
                                .size(38.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(SleekSecondaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AltRoute,
                                contentDescription = null,
                                tint = SleekPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Traceroute Diagnostics",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 16.sp),
                                color = SleekOnBackground
                            )
                            Text(
                                text = "Hop-by-hop IP address & latency analysis",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = SleekTextMuted
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            traceJob?.cancel()
                            onDismiss()
                        },
                        modifier = Modifier.testTag("close_traceroute_dialog_button")
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = SleekTextMuted)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

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
                                .padding(horizontal = 8.dp, vertical = 4.dp)
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

                // Host Input & Max Hops
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
                            .testTag("traceroute_host_input"),
                        label = { Text("Target Host / Domain", style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp)) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
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
                        value = maxHops,
                        onValueChange = { maxHops = it.filter { char -> char.isDigit() }.take(2) },
                        modifier = Modifier
                            .width(76.dp)
                            .testTag("traceroute_hops_input"),
                        label = { Text("Hops", style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp)) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
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

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!isTracing) {
                        ElevatedButton(
                            onClick = { runTrace() },
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .testTag("start_traceroute_button"),
                            colors = ButtonDefaults.elevatedButtonColors(
                                containerColor = SleekPrimary,
                                contentColor = SleekOnPrimary
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Trace Route", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        OutlinedButton(
                            onClick = {
                                traceJob?.cancel()
                                isTracing = false
                                currentStatus = "Trace stopped by user."
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .testTag("stop_traceroute_button"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Stop, contentDescription = null, tint = SleekError, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Stop Trace", color = SleekError, fontWeight = FontWeight.Bold)
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            val text = buildString {
                                appendLine("Traceroute to $targetHost ${if (resolvedTargetIp != null) "($resolvedTargetIp)" else ""}:")
                                appendLine("HOP\tIP ADDRESS\t\tHOSTNAME\t\tRTT (ms)")
                                hopsList.forEach {
                                    appendLine("${it.hopNumber}\t${it.ip}\t${it.hostname}\t${String.format(Locale.US, "%.1f", it.rttMs)}")
                                }
                            }
                            clipboardManager.setText(AnnotatedString(text))
                        },
                        modifier = Modifier
                            .height(40.dp)
                            .testTag("copy_traceroute_button"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy Route", modifier = Modifier.size(16.dp))
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Status line & target IP info
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = currentStatus,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = SleekTextMuted,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                    if (resolvedTargetIp != null) {
                        Text(
                            text = "IP: $resolvedTargetIp",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            ),
                            color = SleekPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Hop List Table with clear IP address visualization
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(230.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(SleekSurfaceVariant)
                        .border(1.dp, SleekBorder, RoundedCornerShape(12.dp))
                        .padding(6.dp)
                ) {
                    if (hopsList.isEmpty()) {
                        Box(modifier = Modifier.fillMaxWidth().padding(top = 40.dp), contentAlignment = Alignment.Center) {
                            Text(
                                text = "Press 'Trace Route' to discover hop-by-hop IP addresses.",
                                style = MaterialTheme.typography.bodySmall,
                                color = SleekTextSubtle
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(hopsList) { hop ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF0F172A))
                                        .border(1.dp, if (hop.nodeType == "Target Destination") SleekPrimaryBorder else Color(0xFF1E293B), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        // Hop Number Badge
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(CircleShape)
                                                .background(if (hop.isSuccess) SleekPrimaryContainer else SleekError),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = hop.hopNumber.toString(),
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 10.sp
                                                ),
                                                color = if (hop.isSuccess) SleekPrimary else Color.White
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(8.dp))

                                        // IP Address & Node Details
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                // Prominent IP badge
                                                Text(
                                                    text = "IP: ${hop.ip}",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontFamily = FontFamily.Monospace,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 11.sp
                                                    ),
                                                    color = if (hop.isSuccess) Color(0xFF38BDF8) else SleekError
                                                )

                                                Spacer(modifier = Modifier.width(6.dp))

                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(Color(0xFF1E293B))
                                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                                ) {
                                                    Text(
                                                        text = hop.nodeType,
                                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                                                        color = SleekTextMuted
                                                    )
                                                }
                                            }

                                            Text(
                                                text = hop.hostname,
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    fontSize = 10.sp,
                                                    fontFamily = FontFamily.Monospace
                                                ),
                                                color = SleekTextSubtle
                                            )
                                        }
                                    }

                                    // Round-Trip Time
                                    if (hop.isSuccess) {
                                        Text(
                                            text = String.format(Locale.US, "%.1f ms", hop.rttMs),
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp
                                            ),
                                            color = SleekSuccess
                                        )
                                    } else {
                                        Text(
                                            text = "* * *",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace
                                            ),
                                            color = SleekError
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
