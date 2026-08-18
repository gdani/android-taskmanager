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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
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
    var fromPath by remember { mutableStateOf("local:") }; var fromUri by remember { mutableStateOf<Uri?>(null) }; var toPath by remember { mutableStateOf("remote:") }; var toUri by remember { mutableStateOf<Uri?>(null) }; var browseFrom by remember { mutableStateOf(true) }; var showBrowseChoice by remember { mutableStateOf(false) }; var status by remember { mutableStateOf("Use local: or remote: prefixes for each endpoint.") }; var transferring by remember { mutableStateOf(false) }; val scope = rememberCoroutineScope()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let { if (browseFrom) { fromUri = it; fromPath = "local:$it" } else { toUri = it; toPath = "local:$it" } } }
    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri -> uri?.let { if (browseFrom) { fromUri = it; fromPath = "local:$it" } else { toUri = it; toPath = "local:$it" }; runCatching { context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION) } } }
    fun local(path: String) = path.removePrefix("local:").removePrefix("remote:")
    fun transfer() { val fromLocal = fromPath.startsWith("local:"); val toLocal = toPath.startsWith("local:"); if (fromLocal && toLocal) { val source = fromUri ?: Uri.parse(local(fromPath)); val target = toUri ?: Uri.parse(local(toPath)); scope.launch(Dispatchers.IO) { context.contentResolver.openInputStream(source)?.use { i -> context.contentResolver.openOutputStream(target)?.use { i.copyTo(it) } }; withContext(Dispatchers.Main) { status = "Local copy completed." } }; return }; val p = port.toIntOrNull(); if (host.isBlank() || username.isBlank() || password.isBlank() || p == null || fromLocal == toLocal) { status = "One endpoint must be local: and the other remote:, with valid SSH details."; return }; scope.launch(Dispatchers.IO) { transferring = true; var session: Session? = null; try { session = JSch().getSession(username, host, p).apply { setPassword(password); setConfig("StrictHostKeyChecking", "no"); connect(12_000) }; val sftp = session.openChannel("sftp") as ChannelSftp; sftp.connect(); if (fromLocal) context.contentResolver.openInputStream(fromUri ?: Uri.parse(local(fromPath)))?.use { sftp.put(it, local(toPath)) } else context.contentResolver.openOutputStream(toUri ?: Uri.parse(local(toPath)))?.use { sftp.get(local(fromPath), it) }; sftp.disconnect(); withContext(Dispatchers.Main) { prefs.edit().putString("host", host).putString("port", port).putString("username", username).apply(); status = "Transfer completed successfully." } } catch (e: Exception) { withContext(Dispatchers.Main) { status = "Transfer failed: ${e.localizedMessage ?: "Unknown error"}" } } finally { session?.disconnect(); withContext(Dispatchers.Main) { transferring = false; password = "" } } } }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) { Surface(modifier = Modifier.fillMaxWidth(.95f), shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp), color = SleekSurface) { Column(Modifier.padding(18.dp)) {
        Text("SCP Transfer", style = MaterialTheme.typography.titleLarge, color = SleekOnBackground); Text("Credentials are used once and never stored.", color = SleekTextMuted, style = MaterialTheme.typography.bodySmall)
        OutlinedTextField(host, { host = it }, label = { Text("Hostname or IP") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        Row(Modifier.fillMaxWidth()) { OutlinedTextField(username, { username = it }, label = { Text("Username") }, modifier = Modifier.weight(1f)); OutlinedTextField(port, { port = it }, label = { Text("Port") }, modifier = Modifier.width(90.dp)) }
        OutlinedTextField(password, { password = it }, label = { Text("Password") }, modifier = Modifier.fillMaxWidth(), visualTransformation = PasswordVisualTransformation(), singleLine = true)
        Row { OutlinedTextField(fromPath, { fromPath = it; fromUri = null }, label = { Text("From path (local: or remote:)") }, modifier = Modifier.weight(1f), singleLine = true); IconButton(onClick = { browseFrom = true; showBrowseChoice = true }) { Icon(Icons.Default.FolderOpen, "Browse from path") } }
        Row { OutlinedTextField(toPath, { toPath = it; toUri = null }, label = { Text("To path (local: or remote:)") }, modifier = Modifier.weight(1f), singleLine = true); IconButton(onClick = { browseFrom = false; showBrowseChoice = true }) { Icon(Icons.Default.FolderOpen, "Browse to path") } }
        Text(status, style = MaterialTheme.typography.bodySmall, color = SleekTextMuted); Row { FilledTonalButton(onClick = { transfer() }, enabled = !transferring, colors = ButtonDefaults.filledTonalButtonColors(containerColor = SleekPrimaryContainer, contentColor = SleekPrimary)) { Text(if (transferring) "Uploading…" else "Upload") }; TextButton(onClick = onDismiss) { Text("Close") } }
    } } }
    if (showBrowseChoice) AlertDialog(onDismissRequest = { showBrowseChoice = false }, title = { Text("Select local endpoint") }, text = { Text("Choose either a specific file or an entire folder.") }, confirmButton = { TextButton(onClick = { showBrowseChoice = false; picker.launch(arrayOf("*/*")) }) { Text("File") } }, dismissButton = { TextButton(onClick = { showBrowseChoice = false; folderPicker.launch(null) }) { Text("Folder") } })
}
