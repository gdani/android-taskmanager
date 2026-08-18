package com.example.ui.components

import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileManagerDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    var hasAccess by remember { mutableStateOf(Environment.isExternalStorageManager()) }
    var directory by remember { mutableStateOf(Environment.getExternalStorageDirectory()) }
    var filter by remember { mutableStateOf("*") }
    var newFolderName by remember { mutableStateOf("") }
    var showNewFolderDialog by remember { mutableStateOf(false) }
    var showLocationMenu by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<File?>(null) }
    var refresh by remember { mutableIntStateOf(0) }
    var isWorking by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf<File?>(null) }
    val scope = rememberCoroutineScope()
    val accessLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { hasAccess = Environment.isExternalStorageManager(); refresh++ }
    val files = remember(directory, filter, refresh) {
        val regex = runCatching { Regex(filter.replace(".", "\\.").replace("*", ".*").replace("?", "."), RegexOption.IGNORE_CASE) }.getOrElse { Regex(".*") }
        directory.listFiles()?.filter { regex.matches(it.name) }?.sortedWith(compareByDescending<File> { it.isDirectory }.thenBy { it.name.lowercase() }).orEmpty()
    }
    val locations = remember(refresh) {
        listOf("/sdcard", "/storage/emulated/0", "/system", "/vendor", "/product", "/data")
            .map(::File)
            .distinctBy { it.absolutePath }
            .filter { it.exists() && it.canRead() }
    }
    fun copy(source: File, target: File): Boolean = runCatching {
        val destination = File(target, source.name)
        if (source.canonicalFile == destination.canonicalFile || target.canonicalPath.startsWith(source.canonicalPath + File.separator)) return false
        if (source.isDirectory) { destination.mkdirs(); source.listFiles()?.forEach { copy(it, destination) } }
        else source.inputStream().use { input -> destination.outputStream().use { input.copyTo(it) } }; true
    }.getOrDefault(false)
    fun delete(target: File): Boolean = if (target.isDirectory) target.listFiles()?.all { delete(it) } != false && target.delete() else target.delete()

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.fillMaxWidth(.96f).fillMaxHeight(.9f).clip(RoundedCornerShape(24.dp)).border(1.dp, SleekBorder, RoundedCornerShape(24.dp)), color = SleekSurface) {
            Column(Modifier.padding(18.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column { Text("File Manager", style = MaterialTheme.typography.titleLarge, color = SleekOnBackground); Text("Shared storage explorer", style = MaterialTheme.typography.bodySmall, color = SleekTextMuted) }
                    FilledTonalButton(onClick = onDismiss, shape = RoundedCornerShape(10.dp), colors = ButtonDefaults.filledTonalButtonColors(containerColor = SleekSurfaceVariant, contentColor = SleekOnBackground)) { Text("Close") }
                }
                Spacer(Modifier.height(14.dp))
                if (!hasAccess) {
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(SleekSurfaceVariant).border(1.dp, SleekPrimaryBorder, RoundedCornerShape(16.dp)).padding(16.dp)) {
                        Text("Storage access required", style = MaterialTheme.typography.titleSmall, color = SleekOnBackground)
                        Spacer(Modifier.height(4.dp)); Text("Grant All files access to browse and manage shared storage.", color = SleekTextMuted)
                        Spacer(Modifier.height(10.dp))
                        ElevatedButton(onClick = { accessLauncher.launch(Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:${context.packageName}"))) }, colors = ButtonDefaults.elevatedButtonColors(containerColor = SleekPrimaryContainer, contentColor = SleekPrimary)) { Text("Grant access in Settings") }
                    }
                } else {
                    OutlinedTextField(filter, { filter = it }, label = { Text("Filter (*, ?, e.g. *.pdf)") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SleekPrimary, focusedLabelColor = SleekPrimary))
                    Spacer(Modifier.height(8.dp))
                    ExposedDropdownMenuBox(expanded = showLocationMenu, onExpandedChange = { showLocationMenu = it }) {
                        OutlinedTextField(directory.absolutePath, {}, readOnly = true, label = { Text("Location with read access") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(showLocationMenu) }, modifier = Modifier.menuAnchor().fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                        ExposedDropdownMenu(expanded = showLocationMenu, onDismissRequest = { showLocationMenu = false }) { locations.forEach { location -> DropdownMenuItem(text = { Text(location.absolutePath, fontFamily = FontFamily.Monospace) }, onClick = { directory = location; selected = null; showLocationMenu = false }) } }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { directory.parentFile?.let { directory = it } }, modifier = Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)).background(SleekSurfaceVariant)) { Icon(Icons.Default.ArrowUpward, "Up", tint = SleekOnBackground) }
                        IconButton(onClick = { showNewFolderDialog = true }, modifier = Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)).background(SleekPrimaryContainer)) { Icon(Icons.Default.CreateNewFolder, "Create folder", tint = SleekPrimary) }
                        selected?.let { file ->
                            IconButton(onClick = { scope.launch(Dispatchers.IO) { isWorking = true; val success = copy(file, directory); withContext(Dispatchers.Main) { isWorking = false; if (success) refresh++ } } }, enabled = !isWorking, modifier = Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)).background(SleekSurfaceVariant)) { Icon(Icons.Default.ContentCopy, "Copy here", tint = SleekOnBackground) }
                            IconButton(onClick = { scope.launch(Dispatchers.IO) { isWorking = true; val success = copy(file, directory) && delete(file); withContext(Dispatchers.Main) { isWorking = false; if (success) { selected = null; refresh++ } } } }, enabled = !isWorking, modifier = Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)).background(SleekSurfaceVariant)) { Icon(Icons.Default.DriveFileMove, "Move here", tint = SleekOnBackground) }
                            IconButton(onClick = { confirmDelete = file }, enabled = !isWorking, modifier = Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)).background(SleekErrorContainer)) { Icon(Icons.Default.Delete, "Del", tint = SleekError) }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    LazyColumn(Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(SleekSurfaceVariant).padding(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) { items(files, key = { it.absolutePath }) { file ->
                        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(if (selected == file) SleekPrimaryContainer else SleekSurface).border(1.dp, if (selected == file) SleekPrimaryBorder else SleekBorderLight, RoundedCornerShape(10.dp)).clickable { selected = file }.padding(horizontal = 12.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(if (file.isDirectory) "📁 ${file.name}" else "📄 ${file.name}", color = SleekOnBackground); Text(if (file.isDirectory) "Folder" else "${file.length()} bytes", style = MaterialTheme.typography.bodySmall, color = SleekTextMuted) }; if (file.isDirectory) TextButton(onClick = { directory = file }) { Text("Open") } }
                    } }
                    selected?.let { Text("Selected: ${it.name} — navigate to a folder, then use Copy here or Move here.", color = SleekTextMuted) }
                }
            }
        }
    }
    if (showNewFolderDialog) AlertDialog(onDismissRequest = { showNewFolderDialog = false }, title = { Text("Create folder") }, text = { OutlinedTextField(newFolderName, { newFolderName = it }, label = { Text("Folder name") }, singleLine = true) }, confirmButton = { TextButton(onClick = { val name = newFolderName.trim(); if (name.isNotBlank() && name !in setOf(".", "..") && !name.contains(File.separator) && File(directory, name).mkdir()) { newFolderName = ""; showNewFolderDialog = false; refresh++ } }) { Text("Create") } }, dismissButton = { TextButton(onClick = { showNewFolderDialog = false }) { Text("Cancel") } })
    confirmDelete?.let { target -> AlertDialog(onDismissRequest = { confirmDelete = null }, title = { Text("Delete ${target.name}?") }, text = { Text("This permanently deletes the selected file or folder and its contents.") }, confirmButton = { TextButton(onClick = { delete(target); if (selected == target) selected = null; confirmDelete = null; refresh++ }) { Text("Delete") } }, dismissButton = { TextButton(onClick = { confirmDelete = null }) { Text("Cancel") } }) }
}
