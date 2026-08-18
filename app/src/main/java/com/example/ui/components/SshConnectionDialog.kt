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
import com.example.ui.theme.*
import com.jcraft.jsch.ChannelShell
import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import kotlinx.coroutines.*
import java.io.PrintStream

@Composable
fun SshConnectionDialog(onDismiss: () -> Unit) {
    var host by remember { mutableStateOf("") }; var port by remember { mutableStateOf("22") }; var username by remember { mutableStateOf("") }; var password by remember { mutableStateOf("") }
    var command by remember { mutableStateOf("") }; var connected by remember { mutableStateOf(false) }; var busy by remember { mutableStateOf(false) }
    val lines = remember { mutableStateListOf("SSH terminal — enter credentials and connect.") }; val scope = rememberCoroutineScope(); var session by remember { mutableStateOf<Session?>(null) }; var shell by remember { mutableStateOf<ChannelShell?>(null) }; var output by remember { mutableStateOf<PrintStream?>(null) }
    fun disconnect() { shell?.disconnect(); session?.disconnect(); shell = null; session = null; output = null; connected = false; lines.add("Disconnected.") }
    fun connect() { val p = port.toIntOrNull(); if (host.isBlank() || username.isBlank() || password.isBlank() || p == null) { lines.add("Host, port, username, and password are required."); return }; scope.launch(Dispatchers.IO) { busy = true; try { val s = JSch().getSession(username, host, p).apply { setPassword(password); setConfig("StrictHostKeyChecking", "no"); connect(12_000) }; val ch = s.openChannel("shell") as ChannelShell; ch.setPty(true); ch.setPtyType("xterm"); ch.connect(); val input = ch.inputStream; val out = PrintStream(ch.outputStream, true); withContext(Dispatchers.Main) { session = s; shell = ch; output = out; connected = true; password = ""; lines.add("Connected to $host:$p as $username") }; scope.launch(Dispatchers.IO) { val buffer = ByteArray(2048); var count = input.read(buffer); while (count >= 0) { val text = String(buffer, 0, count); withContext(Dispatchers.Main) { lines.add(text) }; count = input.read(buffer) } } } catch (e: Exception) { withContext(Dispatchers.Main) { lines.add("Connection failed: ${e.localizedMessage ?: "Unknown error"}") } } finally { withContext(Dispatchers.Main) { busy = false } } } }
    fun send() { val text = command.trim(); if (text.isNotBlank() && connected) { output?.println(text); lines.add("$ $text"); command = "" } }
    Dialog(onDismissRequest = { disconnect(); onDismiss() }, properties = DialogProperties(usePlatformDefaultWidth = false)) { Surface(modifier = Modifier.fillMaxWidth(.96f).fillMaxHeight(.9f), shape = RoundedCornerShape(24.dp), color = SleekSurface) { Column(Modifier.padding(18.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("SSH Terminal", style = MaterialTheme.typography.titleLarge, color = SleekOnBackground); TextButton(onClick = { disconnect(); onDismiss() }) { Text("Close") } }
        if (!connected) { OutlinedTextField(host, { host = it }, label = { Text("Hostname or IP") }, modifier = Modifier.fillMaxWidth(), singleLine = true); Row(Modifier.fillMaxWidth()) { OutlinedTextField(username, { username = it }, label = { Text("Username") }, modifier = Modifier.weight(1f)); OutlinedTextField(port, { port = it }, label = { Text("Port") }, modifier = Modifier.width(90.dp)) }; OutlinedTextField(password, { password = it }, label = { Text("Password") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), singleLine = true); FilledTonalButton(onClick = { connect() }, enabled = !busy) { Text(if (busy) "Connecting…" else "Connect") } } else { Text("$username@$host:$port", color = SleekPrimary) }
        Spacer(Modifier.height(8.dp)); Column(Modifier.weight(1f).fillMaxWidth().background(Color(0xFF101510)).padding(10.dp).verticalScroll(rememberScrollState())) { lines.forEach { Text(it, color = Color(0xFFB8F5BE), fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall) } }
        if (connected) { OutlinedTextField(command, { command = it }, label = { Text("Command") }, modifier = Modifier.fillMaxWidth(), singleLine = true); Row { FilledTonalButton(onClick = { send() }) { Text("Send") }; TextButton(onClick = { disconnect() }) { Text("Disconnect") } } }
    } } }
}
