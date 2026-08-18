package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.SleekBorder
import com.example.ui.theme.SleekError
import com.example.ui.theme.SleekOnBackground
import com.example.ui.theme.SleekPrimary
import com.example.ui.theme.SleekPrimaryContainer
import com.example.ui.theme.SleekSurface
import com.example.ui.theme.SleekSurfaceVariant
import com.example.ui.theme.SleekTextMuted
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

/** A lightweight terminal backed by the device shell under this app's UID (never root). */
@Composable
fun ShellWindowDialog(onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    val output = remember { mutableStateListOf("ProcMaster shell — commands run as this app; root access is unavailable.") }
    var command by remember { mutableStateOf("getprop ro.build.version.release") }
    var isRunning by remember { mutableStateOf(false) }
    var shellJob by remember { mutableStateOf<Job?>(null) }
    val outputScroll = rememberScrollState()

    fun runCommand() {
        val commandToRun = command.trim()
        if (commandToRun.isBlank() || isRunning) return

        output.add("$ $commandToRun")
        isRunning = true
        shellJob = scope.launch(Dispatchers.IO) {
            var process: Process? = null
            try {
                process = ProcessBuilder("/system/bin/sh", "-c", commandToRun)
                    .redirectErrorStream(true)
                    .start()
                val commandOutput = BufferedReader(InputStreamReader(process.inputStream))
                var line = commandOutput.readLine()
                while (line != null) {
                    withContext(Dispatchers.Main) { output.add(line) }
                    line = commandOutput.readLine()
                }
                val exitCode = process.waitFor()
                withContext(Dispatchers.Main) { output.add("[exit $exitCode]") }
            } catch (error: Exception) {
                withContext(Dispatchers.Main) {
                    output.add("[error] ${error.localizedMessage ?: "Unable to execute command"}")
                }
            } finally {
                process?.destroy()
                withContext(Dispatchers.Main) { isRunning = false }
            }
        }
    }

    Dialog(
        onDismissRequest = {
            shellJob?.cancel()
            onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .clip(RoundedCornerShape(24.dp))
                .border(1.dp, SleekBorder, RoundedCornerShape(24.dp))
                .testTag("shell_window_dialog"),
            color = SleekSurface
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
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
                            Icon(Icons.Default.Terminal, null, tint = SleekPrimary)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Shell Window", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = SleekOnBackground)
                            Text("App sandbox shell — no root privileges", style = MaterialTheme.typography.bodySmall, color = SleekTextMuted)
                        }
                    }
                    IconButton(onClick = {
                        shellJob?.cancel()
                        onDismiss()
                    }, modifier = Modifier.testTag("close_shell_window_button")) {
                        Icon(Icons.Default.Close, "Close shell window", tint = SleekTextMuted)
                    }
                }

                Spacer(Modifier.height(14.dp))
                SelectionContainer {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF101510))
                            .padding(12.dp)
                            .verticalScroll(outputScroll)
                            .horizontalScroll(rememberScrollState())
                            .testTag("shell_output")
                    ) {
                        output.forEach { line ->
                            Text(line, color = Color(0xFFB8F5BE), fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = command,
                    onValueChange = { command = it },
                    modifier = Modifier.fillMaxWidth().testTag("shell_command_input"),
                    label = { Text("Command") },
                    singleLine = true,
                    enabled = !isRunning,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SleekPrimary,
                        unfocusedBorderColor = SleekBorder,
                        focusedLabelColor = SleekPrimary
                    )
                )

                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ElevatedButton(
                        onClick = { runCommand() },
                        enabled = !isRunning && command.isNotBlank(),
                        colors = ButtonDefaults.elevatedButtonColors(containerColor = SleekPrimaryContainer, contentColor = SleekPrimary),
                        modifier = Modifier.testTag("run_shell_command_button")
                    ) {
                        Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (isRunning) "Running…" else "Run")
                    }
                    ElevatedButton(
                        onClick = { output.clear() },
                        enabled = !isRunning,
                        colors = ButtonDefaults.elevatedButtonColors(containerColor = SleekSurfaceVariant, contentColor = SleekOnBackground),
                        modifier = Modifier.testTag("clear_shell_output_button")
                    ) {
                        Icon(Icons.Default.DeleteSweep, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Clear")
                    }
                }

                Spacer(Modifier.height(8.dp))
                Text("Commands have the same access as the app and may be unavailable on some devices.", style = MaterialTheme.typography.bodySmall, color = SleekTextMuted)
            }
        }
    }
}
