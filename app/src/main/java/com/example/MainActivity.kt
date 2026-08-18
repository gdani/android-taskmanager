package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AltRoute
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.NetworkPing
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ProcessCategoryFilter
import com.example.data.model.SystemStats
import com.example.ui.components.ExportDialog
import com.example.ui.components.KillHistoryDialog
import com.example.ui.components.NavigationMenuSheet
import com.example.ui.components.NetworkSpeedTestDialog
import com.example.ui.components.PingUtilityDialog
import com.example.ui.components.ProcessDetailSheet
import com.example.ui.components.ProcessFilterBar
import com.example.ui.components.ProcessTableView
import com.example.ui.components.SpawnTestTaskDialog
import com.example.ui.components.SystemInfoDialog
import com.example.ui.components.SystemMetricsHeader
import com.example.ui.components.TracerouteUtilityDialog
import com.example.ui.components.UsageTrendChart
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.SleekBackground
import com.example.ui.theme.SleekBorder
import com.example.ui.theme.SleekBorderLight
import com.example.ui.theme.SleekError
import com.example.ui.theme.SleekErrorContainer
import com.example.ui.theme.SleekOnBackground
import com.example.ui.theme.SleekOnPrimaryContainer
import com.example.ui.theme.SleekPrimary
import com.example.ui.theme.SleekPrimaryContainer
import com.example.ui.theme.SleekSecondaryContainer
import com.example.ui.theme.SleekSurface
import com.example.ui.theme.SleekSurfaceVariant
import com.example.ui.theme.SleekTextMuted
import com.example.ui.theme.SleekTextSubtle
import com.example.ui.viewmodel.ProcessManagerViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    private val viewModel: ProcessManagerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                ProcessManagerApp(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProcessManagerApp(viewModel: ProcessManagerViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Tab 0: Trends (First/Primary), Tab 1: Processes (Second), Tab 2: History, Tab 3: Specs
    var selectedBottomNavIndex by remember { mutableIntStateOf(0) }
    var showNavigationMenu by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.dismissSnackbar()
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("process_manager_root"),
        containerColor = SleekBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            SleekBottomNavBar(
                selectedIndex = selectedBottomNavIndex,
                onSelectTab = { index ->
                    selectedBottomNavIndex = index
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(SleekBackground)
        ) {
            // Dynamic Views based on active tab
            AnimatedContent(
                targetState = selectedBottomNavIndex,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "tab_content",
                modifier = Modifier.fillMaxSize()
            ) { tabIndex ->
                when (tabIndex) {
                    0 -> {
                        // ==========================================
                        // TAB 0: TRENDS & TELEMETRY (Primary Tab)
                        // Shows top system metrics (CPU, RAM, Network) + Trend graphs
                        // ==========================================
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                        ) {
                            // Top Header with CPU, RAM, Network telemetry, tool triggers and menu
                            SystemMetricsHeader(
                                stats = uiState.systemStats,
                                isPaused = uiState.isAutoRefreshPaused,
                                processes = uiState.processes,
                                onOpenProcessDetail = { viewModel.openProcessDetail(it) },
                                onTogglePause = { viewModel.toggleAutoRefreshPause() },
                                onManualRefresh = { viewModel.refreshNow() },
                                onSpawnTestTask = { viewModel.setShowSpawnTaskDialog(true) },
                                onShowExport = { viewModel.setShowExportDialog(true) },
                                onShowSystemInfo = { selectedBottomNavIndex = 3 },
                                onOpenMenu = { showNavigationMenu = true },
                                onOpenSpeedTest = { viewModel.setShowSpeedTestDialog(true) },
                                onOpenPing = { viewModel.setShowPingDialog(true) },
                                onOpenTraceroute = { viewModel.setShowTracerouteDialog(true) }
                            )

                            // Trend Telemetry Graph (Overall, WiFi, Mobile, Bluetooth, CPU & RAM)
                            UsageTrendChart(
                                metricHistory = uiState.metricHistory,
                                selectedTimeWindowSeconds = uiState.selectedTimeWindowSeconds,
                                onSelectTimeWindow = { viewModel.setSelectedTimeWindow(it) },
                                selectedMetricFilter = uiState.selectedChartMetric,
                                onSelectMetricFilter = { viewModel.setSelectedChartMetric(it) }
                            )

                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }

                    1 -> {
                        // ==========================================
                        // TAB 1: RUNNING PROCESSES (Second Tab)
                        // Shows ONLY processes and filter controls (No Top Header taking up space)
                        // ==========================================
                        Column(modifier = Modifier.fillMaxSize()) {
                            // Minimal Compact Top Bar for Process Tab
                            ProcessTabTopBar(
                                totalProcesses = uiState.processes.size,
                                runningCount = uiState.systemStats.runningProcesses,
                                onOpenMenu = { showNavigationMenu = true },
                                onManualRefresh = { viewModel.refreshNow() },
                                isRefreshing = uiState.isRefreshing
                            )

                            // Filter Bar (Search, System & Background toggles, Sizing button, Categories)
                            ProcessFilterBar(
                                searchQuery = uiState.searchQuery,
                                onSearchChange = { viewModel.onSearchQueryChange(it) },
                                selectedCategory = uiState.selectedCategory,
                                onSelectCategory = { viewModel.onCategorySelected(it) },
                                showSystemProcesses = uiState.showSystemProcesses,
                                onToggleShowSystemProcesses = { viewModel.toggleShowSystemProcesses() },
                                showBackgroundProcesses = uiState.showBackgroundProcesses,
                                onToggleShowBackgroundProcesses = { viewModel.toggleShowBackgroundProcesses() },
                                totalCount = uiState.processes.size,
                                filteredCount = uiState.filteredProcesses.size,
                                isMultiSelectMode = uiState.isMultiSelectMode,
                                selectedPidsCount = uiState.selectedPids.size,
                                onToggleMultiSelect = { viewModel.toggleProcessSelection(-1) },
                                onSelectAllFiltered = { viewModel.selectAllFiltered() },
                                onClearSelection = { viewModel.clearSelection() },
                                onKillSelected = { viewModel.terminateSelectedProcesses() },
                                isCompactView = uiState.isCompactTableView,
                                onToggleTableViewMode = { viewModel.toggleTableViewMode() },
                                refreshIntervalMs = uiState.refreshIntervalMs,
                                onSelectRefreshInterval = { viewModel.setRefreshInterval(it) }
                            )

                            // Sortable & Filterable Process Table
                            Box(modifier = Modifier.weight(1f)) {
                                ProcessTableView(
                                    processes = uiState.filteredProcesses,
                                    sortColumn = uiState.sortColumn,
                                    isSortAscending = uiState.isSortAscending,
                                    onSortColumn = { viewModel.onSortColumn(it) },
                                    onTerminateProcess = { viewModel.terminateProcess(it) },
                                    onOpenDetail = { viewModel.openProcessDetail(it) },
                                    isMultiSelectMode = uiState.isMultiSelectMode,
                                    selectedPids = uiState.selectedPids,
                                    onToggleSelectPid = { viewModel.toggleProcessSelection(it) },
                                    isCompactView = uiState.isCompactTableView,
                                    onClearFilters = {
                                        viewModel.onSearchQueryChange("")
                                        viewModel.onCategorySelected(ProcessCategoryFilter.ALL)
                                    }
                                )
                            }
                        }
                    }

                    2 -> {
                        // ==========================================
                        // TAB 2: TERMINATION AUDIT LOG / HISTORY
                        // ==========================================
                        InlineKillHistoryView(
                            history = uiState.killHistory,
                            onClearHistory = { viewModel.clearKillHistory() }
                        )
                    }

                    3 -> {
                        // ==========================================
                        // TAB 3: HARDWARE & SYSTEM SPECIFICATIONS
                        // ==========================================
                        InlineSystemSpecsView(stats = uiState.systemStats)
                    }
                }
            }
        }

        // Navigation Menu Modal Bottom Sheet
        if (showNavigationMenu) {
            NavigationMenuSheet(
                currentTabIndex = selectedBottomNavIndex,
                onSelectTab = { index ->
                    selectedBottomNavIndex = index
                },
                onShowSpeedTest = { viewModel.setShowSpeedTestDialog(true) },
                onShowPing = { viewModel.setShowPingDialog(true) },
                onShowTraceroute = { viewModel.setShowTracerouteDialog(true) },
                onShowKillHistory = { selectedBottomNavIndex = 2 },
                onShowSystemInfo = { selectedBottomNavIndex = 3 },
                onShowExport = { viewModel.setShowExportDialog(true) },
                onSpawnTestTask = { viewModel.setShowSpawnTaskDialog(true) },
                onDismiss = { showNavigationMenu = false }
            )
        }

        // Full Process Detail, Storage Footprint & Lifecycle Sheet (Req #11, 12, 13, 14)
        uiState.selectedProcessForDetail?.let { selectedProcess ->
            ProcessDetailSheet(
                process = selectedProcess,
                onDismiss = { viewModel.closeProcessDetail() },
                onTerminate = { signal ->
                    viewModel.terminateProcess(selectedProcess, signal)
                    viewModel.closeProcessDetail()
                },
                onRestartApp = { proc ->
                    viewModel.restartProcessApp(proc)
                },
                onClearCache = { proc ->
                    viewModel.clearProcessCache(proc)
                },
                onToggleService = { proc ->
                    viewModel.toggleProcessServiceState(proc)
                }
            )
        }

        // Speed Test Subprocess Dialog (Req #7)
        if (uiState.showSpeedTestDialog) {
            NetworkSpeedTestDialog(
                onDismiss = { viewModel.setShowSpeedTestDialog(false) }
            )
        }

        // Ping Utility Dialog (Req #8)
        if (uiState.showPingDialog) {
            PingUtilityDialog(
                onDismiss = { viewModel.setShowPingDialog(false) }
            )
        }

        // Traceroute Utility Dialog (Req #9)
        if (uiState.showTracerouteDialog) {
            TracerouteUtilityDialog(
                onDismiss = { viewModel.setShowTracerouteDialog(false) }
            )
        }

        // Spawn Test Background Task Dialog
        if (uiState.showSpawnTaskDialog) {
            SpawnTestTaskDialog(
                onDismiss = { viewModel.setShowSpawnTaskDialog(false) },
                onSpawn = { name, type, targetCpu, memoryMb ->
                    viewModel.spawnTestTask(name, type, targetCpu, memoryMb)
                }
            )
        }

        // Kill History / Log Dialog
        if (uiState.showKillHistoryDialog) {
            KillHistoryDialog(
                history = uiState.killHistory,
                onDismiss = { viewModel.setShowKillHistoryDialog(false) },
                onClearHistory = { viewModel.clearKillHistory() }
            )
        }

        // Export Snapshot Dialog (JSON/CSV)
        if (uiState.showExportDialog) {
            ExportDialog(
                onDismiss = { viewModel.setShowExportDialog(false) },
                onGenerateExport = { format -> viewModel.generateExportContent(format) }
            )
        }

        // System Specs Dialog
        if (uiState.showSystemInfoDialog) {
            SystemInfoDialog(
                stats = uiState.systemStats,
                onDismiss = { viewModel.setShowSystemInfoDialog(false) }
            )
        }
    }
}

@Composable
fun ProcessTabTopBar(
    totalProcesses: Int,
    runningCount: Int,
    onOpenMenu: () -> Unit,
    onManualRefresh: () -> Unit,
    isRefreshing: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(SleekSurface)
            .border(1.dp, SleekBorder)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onOpenMenu,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(SleekSurfaceVariant)
                    .testTag("process_tab_menu_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Main Menu",
                    tint = SleekOnBackground,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column {
                Text(
                    text = "Active Processes",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    ),
                    color = SleekOnBackground
                )
                Text(
                    text = "$totalProcesses tasks ($runningCount running)",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = SleekTextMuted
                )
            }
        }

        IconButton(
            onClick = onManualRefresh,
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(SleekSurfaceVariant)
                .testTag("process_tab_refresh_button")
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "Refresh Processes",
                tint = if (isRefreshing) SleekPrimary else SleekOnBackground,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun InlineKillHistoryView(
    history: List<com.example.data.model.KillRecord>,
    onClearHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Termination History",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    ),
                    color = SleekOnBackground
                )
                Text(
                    text = "${history.size} processes terminated",
                    style = MaterialTheme.typography.bodySmall,
                    color = SleekTextMuted
                )
            }

            if (history.isNotEmpty()) {
                OutlinedButton(
                    onClick = onClearHistory,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SleekError)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = "Clear History",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Clear", style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        if (history.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(SleekSurfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = SleekTextMuted,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No termination records",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = SleekOnBackground
                    )
                    Text(
                        text = "Terminated or killed processes will appear here",
                        style = MaterialTheme.typography.bodySmall,
                        color = SleekTextMuted
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(history) { record ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = SleekSurface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SleekBorderLight)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(if (record.success) SleekErrorContainer else SleekSurfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = record.signalName,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 9.sp
                                        ),
                                        color = if (record.success) SleekError else SleekTextMuted
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = record.processName,
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 13.sp
                                        ),
                                        color = SleekOnBackground
                                    )
                                    Text(
                                        text = "PID: ${record.pid} • Freed ~${record.formattedFreedMemory}",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 11.sp
                                        ),
                                        color = SleekTextMuted
                                    )
                                }
                            }

                            Text(
                                text = timeFormat.format(Date(record.timestamp)),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp
                                ),
                                color = SleekTextSubtle
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InlineSystemSpecsView(
    stats: SystemStats,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "System Specifications",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            ),
            color = SleekOnBackground
        )
        Text(
            text = "Hardware architecture and runtime parameters",
            style = MaterialTheme.typography.bodySmall,
            color = SleekTextMuted
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Hardware & CPU Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SleekSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, SleekBorder)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "HARDWARE & CPU",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp,
                        fontSize = 10.sp
                    ),
                    color = SleekPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                SpecsRowItem(label = "Device Model", value = "${android.os.Build.MANUFACTURER.uppercase()} ${android.os.Build.MODEL}")
                SpecsRowDivider()
                SpecsRowItem(label = "CPU Architecture", value = android.os.Build.SUPPORTED_ABIS.joinToString(", "))
                SpecsRowDivider()
                SpecsRowItem(label = "CPU Cores", value = "${stats.coreCount} physical / logical cores")
                SpecsRowDivider()
                SpecsRowItem(label = "Hardware Board", value = android.os.Build.BOARD.ifBlank { "Universal SoC" })
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Memory & Load Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SleekSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, SleekBorder)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "MEMORY & SYSTEM METRICS",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp,
                        fontSize = 10.sp
                    ),
                    color = SleekPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                SpecsRowItem(label = "Total RAM", value = stats.formattedTotalRam)
                SpecsRowDivider()
                SpecsRowItem(label = "RAM Available", value = stats.formattedAvailableRam)
                SpecsRowDivider()
                SpecsRowItem(label = "RAM In Use", value = stats.formattedUsedRam)
                SpecsRowDivider()
                SpecsRowItem(label = "Active Processes", value = "${stats.totalProcesses} total (${stats.runningProcesses} running)")
                SpecsRowDivider()
                SpecsRowItem(label = "Total Threads", value = "${stats.totalThreads} active")
                SpecsRowDivider()
                SpecsRowItem(label = "System Uptime", value = stats.systemUptime)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Operating System & Kernel Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SleekSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, SleekBorder)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "OPERATING SYSTEM & KERNEL",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp,
                        fontSize = 10.sp
                    ),
                    color = SleekPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                SpecsRowItem(label = "Android Version", value = "Android ${android.os.Build.VERSION.RELEASE} (API ${android.os.Build.VERSION.SDK_INT})")
                SpecsRowDivider()
                SpecsRowItem(label = "Kernel Version", value = System.getProperty("os.version") ?: "Linux Kernel")
                SpecsRowDivider()
                SpecsRowItem(label = "Security Patch", value = android.os.Build.VERSION.SECURITY_PATCH ?: "Current")
                SpecsRowDivider()
                SpecsRowItem(label = "Build ID", value = android.os.Build.DISPLAY.ifBlank { android.os.Build.ID })
            }
        }
    }
}

@Composable
fun SpecsRowDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(SleekBorderLight)
    )
}

@Composable
fun SpecsRowItem(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            ),
            color = SleekTextMuted,
            modifier = Modifier.width(135.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp
            ),
            color = SleekOnBackground,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun SleekBottomNavBar(
    selectedIndex: Int,
    onSelectTab: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(SleekSurfaceVariant)
            .border(1.dp, SleekBorder)
            .navigationBarsPadding()
            .padding(vertical = 8.dp, horizontal = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Tab 0: Trends (First)
            SleekNavItem(
                icon = Icons.Default.ShowChart,
                label = "Trends",
                isSelected = selectedIndex == 0,
                onClick = { onSelectTab(0) },
                testTag = "nav_item_trends"
            )
            // Tab 1: Processes (Second)
            SleekNavItem(
                icon = Icons.Default.Dashboard,
                label = "Processes",
                isSelected = selectedIndex == 1,
                onClick = { onSelectTab(1) },
                testTag = "nav_item_processes"
            )
            // Tab 2: History
            SleekNavItem(
                icon = Icons.Default.History,
                label = "History",
                isSelected = selectedIndex == 2,
                onClick = { onSelectTab(2) },
                testTag = "nav_item_analytics"
            )
            // Tab 3: Specs
            SleekNavItem(
                icon = Icons.Default.Info,
                label = "Specs",
                isSelected = selectedIndex == 3,
                onClick = { onSelectTab(3) },
                testTag = "nav_item_settings"
            )
        }
    }
}

@Composable
fun SleekNavItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .testTag(testTag)
            .padding(horizontal = 12.dp, vertical = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .width(60.dp)
                .height(32.dp)
                .clip(CircleShape)
                .background(if (isSelected) SleekSecondaryContainer else Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) SleekOnBackground else SleekTextMuted,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
            ),
            color = if (isSelected) SleekOnBackground else SleekTextMuted
        )
    }
}
