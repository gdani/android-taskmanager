package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.platform.LocalContext
import com.example.ui.theme.*
import com.jcraft.jsch.ChannelShell
import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import kotlinx.coroutines.*
import java.io.PrintStream

@Composable
fun SshConnectionDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current; val prefs = remember { context.getSharedPreferences("ssh_connection", 0) }
    var host by remember { mutableStateOf(prefs.getString("host", "") ?: "") }; var port by remember { mutableStateOf(prefs.getString("port", "22") ?: "22") }; var username by remember { mutableStateOf(prefs.getString("username", "") ?: "") }; var password by remember { mutableStateOf("") }
    var command by remember { mutableStateOf("") }; var connected by remember { mutableStateOf(false) }; var busy by remember { mutableStateOf(false) }
    var sudoPassword by remember { mutableStateOf("") }; var showSudoPrompt by remember { mutableStateOf(false) }; var rootMode by remember { mutableStateOf(false) }
    val lines = remember { mutableStateListOf("SSH terminal — enter credentials and connect.") }; val scope = rememberCoroutineScope(); var session by remember { mutableStateOf<Session?>(null) }; var shell by remember { mutableStateOf<ChannelShell?>(null) }; var output by remember { mutableStateOf<PrintStream?>(null) }
    fun disconnect() { shell?.disconnect(); session?.disconnect(); shell = null; session = null; output = null; sudoPassword = ""; rootMode = false; connected = false; lines.add("Disconnected.") }
    fun connect() { val p = port.toIntOrNull(); if (host.isBlank() || username.isBlank() || password.isBlank() || p == null) { lines.add("Host, port, username, and password are required."); return }; scope.launch(Dispatchers.IO) { busy = true; try { val s = JSch().getSession(username, host, p).apply { setPassword(password); setConfig("StrictHostKeyChecking", "no"); connect(12_000) }; val ch = s.openChannel("shell") as ChannelShell; ch.setPty(true); ch.setPtyType("xterm"); val input = ch.inputStream; val out = PrintStream(ch.outputStream, true); ch.connect(); withContext(Dispatchers.Main) { prefs.edit().putString("host", host).putString("port", port).putString("username", username).apply(); session = s; shell = ch; output = out; connected = true; password = ""; lines.add("Connected to $host:$p as $username") }; scope.launch(Dispatchers.IO) { val buffer = ByteArray(2048); var count = input.read(buffer); while (count >= 0) { val text = String(buffer, 0, count); withContext(Dispatchers.Main) { lines.add(text) }; count = input.read(buffer) } } } catch (e: Exception) { withContext(Dispatchers.Main) { lines.add("Connection failed: ${e.localizedMessage ?: "Unknown error"}") } } finally { withContext(Dispatchers.Main) { busy = false } } } }
    fun send() {
        val enteredText = command.trim()
        if (rootMode && enteredText == "exit") {
            rootMode = false
            sudoPassword = ""
            command = ""
            lines.add("$ exit")
            lines.add("Exited root context.")
            lines.add("$username@$host:~$ ")
            return
        }
        val text = if (rootMode && !enteredText.startsWith("sudo ")) "sudo -S sh -c '${enteredText.replace("'", "'\\\"'\\\"'")}'" else enteredText
        val activeSession = session
        if (text.isBlank() || !connected || activeSession == null) return
        lines.add("$ $text")
        command = ""
        scope.launch(Dispatchers.IO) {
            try {
                val channel = activeSession.openChannel("exec") as com.jcraft.jsch.ChannelExec
                channel.setCommand(text)
                val stdout = channel.inputStream
                val stderr = channel.errStream
                channel.connect(12_000)
                if (text.startsWith("sudo -S ")) {
                    channel.outputStream.bufferedWriter().use { it.write("$sudoPassword\n"); it.flush() }
                }
                val result = stdout.bufferedReader().readText() + stderr.bufferedReader().readText()
                withContext(Dispatchers.Main) {
                    lines.add(result.ifBlank { "[command completed with no output]" })
                    lines.add(if (rootMode) "root@$host:# " else "$username@$host:~$ ")
                }
                channel.disconnect()
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { lines.add("Command failed: ${e.localizedMessage ?: "Unknown error"}") }
            }
        }
    }
    Dialog(onDismissRequest = { disconnect(); onDismiss() }, properties = DialogProperties(usePlatformDefaultWidth = false)) { Surface(modifier = Modifier.fillMaxWidth(.96f).fillMaxHeight(.9f), shape = RoundedCornerShape(24.dp), color = SleekSurface) { Column(Modifier.padding(18.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("SSH Terminal", style = MaterialTheme.typography.titleLarge, color = SleekOnBackground); TextButton(onClick = { disconnect(); onDismiss() }) { Text("Close") } }
        if (!connected) { OutlinedTextField(host, { host = it }, label = { Text("Hostname or IP") }, modifier = Modifier.fillMaxWidth(), singleLine = true); Row(Modifier.fillMaxWidth()) { OutlinedTextField(username, { username = it }, label = { Text("Username") }, modifier = Modifier.weight(1f)); OutlinedTextField(port, { port = it }, label = { Text("Port") }, modifier = Modifier.width(90.dp)) }; OutlinedTextField(password, { password = it }, label = { Text("Password") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), singleLine = true); FilledTonalButton(onClick = { connect() }, enabled = !busy) { Text(if (busy) "Connecting…" else "Connect") } } else { Text("$username@$host:$port", color = SleekPrimary) }
        val consoleScroll = rememberScrollState(); LaunchedEffect(lines.size) { consoleScroll.scrollTo(consoleScroll.maxValue) }; Spacer(Modifier.height(8.dp)); Column(Modifier.weight(1f).fillMaxWidth().background(Color(0xFF101510)).padding(10.dp).verticalScroll(consoleScroll)) { lines.forEach { Text(it, color = Color(0xFFB8F5BE), fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall) } }
        if (connected) { OutlinedTextField(command, { command = it }, label = { Text("Command") }, modifier = Modifier.fillMaxWidth(), singleLine = true); Row { FilledTonalButton(onClick = { if (command.trim().startsWith("sudo ") && sudoPassword.isBlank()) showSudoPrompt = true else send() }) { Text("Send") }; TextButton(onClick = { disconnect() }) { Text("Disconnect") } } }
    } } }
    if (showSudoPrompt) AlertDialog(onDismissRequest = { showSudoPrompt = false }, title = { Text("Sudo password") }, text = { OutlinedTextField(sudoPassword, { sudoPassword = it }, label = { Text("Password") }, visualTransformation = PasswordVisualTransformation(), singleLine = true) }, confirmButton = { TextButton(onClick = { val becomingRoot = command.trim() == "sudo su" || command.trim() == "sudo -i"; command = "sudo -S ${command.removePrefix("sudo ")}"; rootMode = becomingRoot; showSudoPrompt = false; send() }) { Text("Run") } }, dismissButton = { TextButton(onClick = { showSudoPrompt = false }) { Text("Cancel") } })
}
