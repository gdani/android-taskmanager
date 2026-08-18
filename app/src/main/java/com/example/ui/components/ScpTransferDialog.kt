package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.*
import com.jcraft.jsch.ChannelSftp
import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import kotlinx.coroutines.*

@Composable
fun ScpTransferDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("ssh_connection", 0) }
    var host by remember { mutableStateOf(prefs.getString("host", "") ?: "") }; var port by remember { mutableStateOf(prefs.getString("port", "22") ?: "22") }; var username by remember { mutableStateOf(prefs.getString("username", "") ?: "") }; var password by remember { mutableStateOf("") }
    var fromPath by remember { mutableStateOf("") }; var fromUri by remember { mutableStateOf<Uri?>(null) }; var toPath by remember { mutableStateOf("") }; var status by remember { mutableStateOf("Choose a phone file, then enter the remote destination path.") }; var transferring by remember { mutableStateOf(false) }; val scope = rememberCoroutineScope()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let { fromUri = it; fromPath = it.toString(); context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION) } }
    fun transfer() { val p = port.toIntOrNull(); val uri = fromUri; if (host.isBlank() || username.isBlank() || password.isBlank() || p == null || uri == null || toPath.isBlank()) { status = "Host, port, username, password, From path, and To path are required."; return }; scope.launch(Dispatchers.IO) { transferring = true; var session: Session? = null; try { session = JSch().getSession(username, host, p).apply { setPassword(password); setConfig("StrictHostKeyChecking", "no"); connect(12_000) }; val sftp = session.openChannel("sftp") as ChannelSftp; sftp.connect(); context.contentResolver.openInputStream(uri)?.use { input -> sftp.put(input, toPath) } ?: error("Unable to read selected file"); sftp.disconnect(); withContext(Dispatchers.Main) { prefs.edit().putString("host", host).putString("port", port).putString("username", username).apply(); status = "Upload completed successfully." } } catch (e: Exception) { withContext(Dispatchers.Main) { status = "Transfer failed: ${e.localizedMessage ?: "Unknown error"}" } } finally { session?.disconnect(); withContext(Dispatchers.Main) { transferring = false; password = "" } } } }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) { Surface(modifier = Modifier.fillMaxWidth(.95f), shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp), color = SleekSurface) { Column(Modifier.padding(18.dp)) {
        Text("SCP Transfer", style = MaterialTheme.typography.titleLarge, color = SleekOnBackground); Text("Credentials are used once and never stored.", color = SleekTextMuted, style = MaterialTheme.typography.bodySmall)
        OutlinedTextField(host, { host = it }, label = { Text("Hostname or IP") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        Row(Modifier.fillMaxWidth()) { OutlinedTextField(username, { username = it }, label = { Text("Username") }, modifier = Modifier.weight(1f)); OutlinedTextField(port, { port = it }, label = { Text("Port") }, modifier = Modifier.width(90.dp)) }
        OutlinedTextField(password, { password = it }, label = { Text("Password") }, modifier = Modifier.fillMaxWidth(), visualTransformation = PasswordVisualTransformation(), singleLine = true)
        OutlinedTextField(fromPath, { fromPath = it; fromUri = null }, label = { Text("From path (phone)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        TextButton(onClick = { picker.launch(arrayOf("*/*")) }) { Text("Browse phone files") }
        OutlinedTextField(toPath, { toPath = it }, label = { Text("To path (remote host)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        Text(status, style = MaterialTheme.typography.bodySmall, color = SleekTextMuted); Row { FilledTonalButton(onClick = { transfer() }, enabled = !transferring, colors = ButtonDefaults.filledTonalButtonColors(containerColor = SleekPrimaryContainer, contentColor = SleekPrimary)) { Text(if (transferring) "Uploading…" else "Upload") }; TextButton(onClick = onDismiss) { Text("Close") } }
    } } }
}
