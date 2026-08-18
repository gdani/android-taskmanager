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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShowChart
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
import com.example.ui.components.ProcessDetailSheet
import com.example.ui.components.ProcessFilterBar
import com.example.ui.components.ProcessTableView
import com.example.ui.components.SpawnTestTaskDialog
import com.example.ui.components.SystemInfoDialog
import com.example.ui.components.SystemMetricsHeader
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
import com.example.ui.theme.SleekSuccess
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
            // Live System Monitor Card (CPU/RAM/Quick Actions)
            SystemMetricsHeader(
                stats = uiState.systemStats,
                isPaused = uiState.isAutoRefreshPaused,
                isRefreshing = uiState.isRefreshing,
                onTogglePause = { viewModel.toggleAutoRefreshPause() },
                onManualRefresh = { viewModel.refreshNow() },
                onKillAllBackground = { viewModel.terminateAllBackgroundApps() },
                onSpawnTestTask = { viewModel.setShowSpawnTaskDialog(true) },
                onShowKillHistory = { selectedBottomNavIndex = 2 },
                onShowExport = { viewModel.setShowExportDialog(true) },
                onShowSystemInfo = { selectedBottomNavIndex = 3 },
                onOpenMenu = { showNavigationMenu = true }
            )

            // Dynamic Tab Views
            AnimatedContent(
                targetState = selectedBottomNavIndex,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "tab_content",
                modifier = Modifier.weight(1f)
            ) { tabIndex ->
                when (tabIndex) {
                    0 -> {
                        // TAB 0: PROCESSES ONLY (No Trend Chart)
                        Column(modifier = Modifier.fillMaxSize()) {
                            // Filter Bar (Search, System & Background toggles, Category chips)
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

                            // Clean, Filterable, Sortable Process Table
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

                    1 -> {
                        // TAB 1: TRENDS ONLY (Everything else, NO processes at all)
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(vertical = 4.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            UsageTrendChart(
                                metricHistory = uiState.metricHistory,
                                selectedTimeWindowSeconds = uiState.selectedTimeWindowSeconds,
                                onSelectTimeWindow = { viewModel.setSelectedTimeWindow(it) },
                                selectedMetricFilter = uiState.selectedChartMetric,
                                onSelectMetricFilter = { viewModel.setSelectedChartMetric(it) }
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Trend Analytics Breakdown Card
                            TrendAnalyticsCard(
                                stats = uiState.systemStats,
                                onClearHistory = { viewModel.clearKillHistory() },
                                onSpawnWorker = { viewModel.setShowSpawnTaskDialog(true) }
                            )
                        }
                    }

                    2 -> {
                        // TAB 2: TERMINATION HISTORY
                        InlineKillHistoryView(
                            history = uiState.killHistory,
                            onClearHistory = { viewModel.clearKillHistory() }
                        )
                    }

                    3 -> {
                        // TAB 3: SYSTEM SPECS
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
                onShowExport = { viewModel.setShowExportDialog(true) },
                onSpawnTestTask = { viewModel.setShowSpawnTaskDialog(true) },
                onDismiss = { showNavigationMenu = false }
            )
        }

        // Full Command Line Inspector Bottom Sheet
        uiState.selectedProcessForDetail?.let { selectedProcess ->
            ProcessDetailSheet(
                process = selectedProcess,
                onDismiss = { viewModel.closeProcessDetail() },
                onTerminate = { signal ->
                    viewModel.terminateProcess(selectedProcess, signal)
                    viewModel.closeProcessDetail()
                }
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
fun TrendAnalyticsCard(
    stats: SystemStats,
    onClearHistory: () -> Unit,
    onSpawnWorker: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = SleekSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, SleekBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "SYSTEM LOAD ANALYSIS",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 1.sp
                ),
                color = SleekTextMuted
            )
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // CPU Summary Pill
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(SleekPrimaryContainer)
                        .padding(12.dp)
                ) {
                    Column {
                        Text(
                            text = "CPU Load",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = SleekOnPrimaryContainer.copy(alpha = 0.7f)
                        )
                        Text(
                            text = String.format(Locale.US, "%.1f%%", stats.totalCpuUsagePercent),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            ),
                            color = SleekOnPrimaryContainer
                        )
                        Text(
                            text = "${stats.coreCount} active CPU cores",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = SleekOnPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }

                // RAM Summary Pill
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(SleekSecondaryContainer)
                        .padding(12.dp)
                ) {
                    Column {
                        Text(
                            text = "RAM In Use",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = SleekOnBackground.copy(alpha = 0.7f)
                        )
                        Text(
                            text = stats.formattedUsedRam,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            ),
                            color = SleekOnBackground
                        )
                        Text(
                            text = "${String.format(Locale.US, "%.1f", stats.memoryUsagePercent)}% of ${stats.formattedTotalRam}",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = SleekTextMuted
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    onClick = onSpawnWorker,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("⚡ Test Worker", style = MaterialTheme.typography.labelMedium)
                }

                OutlinedButton(
                    onClick = onClearHistory,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Clear Logs", style = MaterialTheme.typography.labelMedium)
                }
            }
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

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SleekSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, SleekBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                SpecsRowItem(label = "Device Model", value = "${android.os.Build.MANUFACTURER.uppercase()} ${android.os.Build.MODEL}")
                SpecsRowItem(label = "Android Version", value = "Android ${android.os.Build.VERSION.RELEASE} (SDK ${android.os.Build.VERSION.SDK_INT})")
                SpecsRowItem(label = "CPU Architecture", value = android.os.Build.SUPPORTED_ABIS.joinToString(", "))
                SpecsRowItem(label = "CPU Cores", value = "${stats.coreCount} physical / logical cores")
                SpecsRowItem(label = "Total RAM", value = stats.formattedTotalRam)
                SpecsRowItem(label = "Active Processes", value = "${stats.totalProcesses} running")
                SpecsRowItem(label = "RAM Available", value = stats.formattedAvailableRam)
                SpecsRowItem(label = "Kernel Version", value = System.getProperty("os.version") ?: "Linux Kernel")
            }
        }
    }
}

@Composable
fun SpecsRowItem(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = SleekTextMuted
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace
            ),
            color = SleekOnBackground
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
            SleekNavItem(
                icon = Icons.Default.Dashboard,
                label = "Processes",
                isSelected = selectedIndex == 0,
                onClick = { onSelectTab(0) },
                testTag = "nav_item_processes"
            )
            SleekNavItem(
                icon = Icons.Default.ShowChart,
                label = "Trends",
                isSelected = selectedIndex == 1,
                onClick = { onSelectTab(1) },
                testTag = "nav_item_trends"
            )
            SleekNavItem(
                icon = Icons.Default.History,
                label = "History",
                isSelected = selectedIndex == 2,
                onClick = { onSelectTab(2) },
                testTag = "nav_item_analytics"
            )
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
