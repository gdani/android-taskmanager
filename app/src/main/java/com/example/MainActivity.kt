package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ProcessCategoryFilter
import com.example.ui.components.ExportDialog
import com.example.ui.components.KillHistoryDialog
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
import com.example.ui.theme.SleekOnBackground
import com.example.ui.theme.SleekPrimary
import com.example.ui.theme.SleekSecondaryContainer
import com.example.ui.theme.SleekSurfaceVariant
import com.example.ui.theme.SleekTextMuted
import com.example.ui.viewmodel.ProcessManagerViewModel
import androidx.compose.material.icons.filled.ShowChart

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
                    when (index) {
                        0 -> {
                            // When clicking Processes, trends chart collapses to the background
                            viewModel.setChartExpanded(false)
                        }
                        1 -> {
                            // When clicking Trends, trends chart expands prominently
                            viewModel.setChartExpanded(true)
                        }
                        2 -> viewModel.setShowKillHistoryDialog(true)
                        3 -> viewModel.setShowSystemInfoDialog(true)
                    }
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
            // Live System Monitor Card (CPU/RAM/Actions)
            SystemMetricsHeader(
                stats = uiState.systemStats,
                isPaused = uiState.isAutoRefreshPaused,
                isRefreshing = uiState.isRefreshing,
                onTogglePause = { viewModel.toggleAutoRefreshPause() },
                onManualRefresh = { viewModel.refreshNow() },
                onKillAllBackground = { viewModel.terminateAllBackgroundApps() },
                onSpawnTestTask = { viewModel.setShowSpawnTaskDialog(true) },
                onShowKillHistory = { viewModel.setShowKillHistoryDialog(true) },
                onShowExport = { viewModel.setShowExportDialog(true) },
                onShowSystemInfo = { viewModel.setShowSystemInfoDialog(true) }
            )

            // Live-updating Line Chart (CPU & Memory Trends over 60s/120s/300s)
            UsageTrendChart(
                metricHistory = uiState.metricHistory,
                selectedTimeWindowSeconds = uiState.selectedTimeWindowSeconds,
                onSelectTimeWindow = { viewModel.setSelectedTimeWindow(it) },
                selectedMetricFilter = uiState.selectedChartMetric,
                onSelectMetricFilter = { viewModel.setSelectedChartMetric(it) },
                isExpanded = uiState.isChartExpanded,
                onToggleExpand = { viewModel.toggleChartExpanded() }
            )

            // Search, Multi-Filter, System/Background Toggles, and Mode Bar
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
                icon = Icons.Default.Analytics,
                label = "History",
                isSelected = selectedIndex == 2,
                onClick = { onSelectTab(2) },
                testTag = "nav_item_analytics"
            )
            SleekNavItem(
                icon = Icons.Default.Settings,
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
                .background(if (isSelected) SleekSecondaryContainer else androidx.compose.ui.graphics.Color.Transparent),
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
