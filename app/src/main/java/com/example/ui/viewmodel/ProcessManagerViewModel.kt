package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.ChartMetricFilter
import com.example.data.model.KillRecord
import com.example.data.model.MetricPoint
import com.example.data.model.ProcessCategory
import com.example.data.model.ProcessCategoryFilter
import com.example.data.model.ProcessInfo
import com.example.data.model.ProcessSortColumn
import com.example.data.model.ProcessState
import com.example.data.model.SystemStats
import com.example.data.provider.ProcessDataProvider
import com.example.data.provider.SystemMetricsProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ProcessManagerUiState(
    val processes: List<ProcessInfo> = emptyList(),
    val filteredProcesses: List<ProcessInfo> = emptyList(),
    val systemStats: SystemStats = SystemStats(),
    val metricHistory: List<MetricPoint> = emptyList(),
    val selectedTimeWindowSeconds: Int = 60,
    val selectedChartMetric: ChartMetricFilter = ChartMetricFilter.BOTH,
    val isChartExpanded: Boolean = true,
    val searchQuery: String = "",
    val selectedCategory: ProcessCategoryFilter = ProcessCategoryFilter.ALL,
    val sortColumn: ProcessSortColumn = ProcessSortColumn.CPU,
    val isSortAscending: Boolean = false,
    val refreshIntervalMs: Long = 2000L,
    val isAutoRefreshPaused: Boolean = false,
    val isRefreshing: Boolean = false,
    val selectedProcessForDetail: ProcessInfo? = null,
    val selectedPids: Set<Int> = emptySet(),
    val isMultiSelectMode: Boolean = false,
    val isCompactTableView: Boolean = false,
    val killHistory: List<KillRecord> = emptyList(),
    val showSpawnTaskDialog: Boolean = false,
    val showKillHistoryDialog: Boolean = false,
    val showExportDialog: Boolean = false,
    val showSystemInfoDialog: Boolean = false,
    val snackbarMessage: String? = null
)

class ProcessManagerViewModel(application: Application) : AndroidViewModel(application) {

    private val metricsProvider = SystemMetricsProvider(application)
    private val dataProvider = ProcessDataProvider(application, viewModelScope)

    private val _uiState = MutableStateFlow(ProcessManagerUiState())
    val uiState: StateFlow<ProcessManagerUiState> = _uiState.asStateFlow()

    private val rawMetricHistory = mutableListOf<MetricPoint>()
    private var refreshJob: Job? = null

    init {
        seedInitialMetricHistory()
        startAutoRefresh()
    }

    private fun seedInitialMetricHistory() {
        val now = System.currentTimeMillis()
        val stats = metricsProvider.getSystemStats()
        val baseCpu = if (stats.totalCpuUsagePercent > 0) stats.totalCpuUsagePercent else 14.0
        val baseRam = if (stats.memoryUsagePercent > 0) stats.memoryUsagePercent else 52.0
        val totalRam = if (stats.totalMemoryBytes > 0) stats.totalMemoryBytes else (6L * 1024L * 1024L * 1024L)

        val seedPoints = mutableListOf<MetricPoint>()
        for (i in 150 downTo 0) {
            val pointTime = now - (i * 2000L)
            val sineCpu = kotlin.math.sin(i * 0.15) * 4.0 + kotlin.math.cos(i * 0.05) * 3.0
            val noiseCpu = ((-15..15).random() / 10.0)
            val cpuVal = (baseCpu + sineCpu + noiseCpu).coerceIn(2.0, 95.0)

            val sineRam = kotlin.math.sin(i * 0.08) * 1.5
            val ramPercentVal = (baseRam + sineRam).coerceIn(10.0, 95.0)
            val ramUsed = ((ramPercentVal / 100.0) * totalRam).toLong()

            seedPoints.add(
                MetricPoint(
                    timestampMs = pointTime,
                    cpuPercent = cpuVal,
                    memoryPercent = ramPercentVal,
                    memoryUsedBytes = ramUsed,
                    memoryTotalBytes = totalRam
                )
            )
        }
        synchronized(rawMetricHistory) {
            rawMetricHistory.clear()
            rawMetricHistory.addAll(seedPoints)
        }
        _uiState.update { it.copy(metricHistory = seedPoints, systemStats = stats) }
    }

    private fun startAutoRefresh() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                if (!_uiState.value.isAutoRefreshPaused) {
                    loadDataInternal()
                }
                val interval = _uiState.value.refreshIntervalMs
                if (interval <= 0) {
                    delay(2000L)
                } else {
                    delay(interval)
                }
            }
        }
    }

    fun refreshNow() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isRefreshing = true) }
            loadDataInternal()
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    private suspend fun loadDataInternal() {
        val stats = metricsProvider.getSystemStats()
        val rawList = dataProvider.fetchRunningProcesses(stats.totalMemoryBytes)
        val history = dataProvider.getKillHistory()

        val runningCount = rawList.count { it.state == ProcessState.RUNNING }
        val sleepingCount = rawList.count { it.state == ProcessState.SLEEPING || it.state == ProcessState.DISK_SLEEP }
        val threadCount = rawList.sumOf { it.threadsCount }

        val updatedStats = stats.copy(
            totalProcesses = rawList.size,
            runningProcesses = runningCount,
            sleepingProcesses = sleepingCount,
            totalThreads = threadCount
        )

        val newPoint = MetricPoint(
            timestampMs = System.currentTimeMillis(),
            cpuPercent = updatedStats.totalCpuUsagePercent,
            memoryPercent = updatedStats.memoryUsagePercent,
            memoryUsedBytes = updatedStats.usedMemoryBytes,
            memoryTotalBytes = updatedStats.totalMemoryBytes
        )

        val historySnapshot = synchronized(rawMetricHistory) {
            rawMetricHistory.add(newPoint)
            val cutoff = System.currentTimeMillis() - 360_000L
            while (rawMetricHistory.isNotEmpty() && rawMetricHistory.first().timestampMs < cutoff) {
                rawMetricHistory.removeAt(0)
            }
            rawMetricHistory.toList()
        }

        withContext(Dispatchers.Main) {
            _uiState.update { currentState ->
                val filtered = applyFilterAndSort(
                    rawList,
                    currentState.searchQuery,
                    currentState.selectedCategory,
                    currentState.sortColumn,
                    currentState.isSortAscending
                )
                
                // If detail sheet is open, keep selected process reference fresh
                val updatedSelected = currentState.selectedProcessForDetail?.let { currentSel ->
                    rawList.find { it.pid == currentSel.pid }
                }

                currentState.copy(
                    processes = rawList,
                    filteredProcesses = filtered,
                    systemStats = updatedStats,
                    metricHistory = historySnapshot,
                    killHistory = history,
                    selectedProcessForDetail = updatedSelected
                )
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { state ->
            val filtered = applyFilterAndSort(
                state.processes,
                query,
                state.selectedCategory,
                state.sortColumn,
                state.isSortAscending
            )
            state.copy(searchQuery = query, filteredProcesses = filtered)
        }
    }

    fun onCategorySelected(category: ProcessCategoryFilter) {
        _uiState.update { state ->
            val filtered = applyFilterAndSort(
                state.processes,
                state.searchQuery,
                category,
                state.sortColumn,
                state.isSortAscending
            )
            state.copy(selectedCategory = category, filteredProcesses = filtered)
        }
    }

    fun onSortColumn(column: ProcessSortColumn) {
        _uiState.update { state ->
            val newAscending = if (state.sortColumn == column) !state.isSortAscending else false
            val filtered = applyFilterAndSort(
                state.processes,
                state.searchQuery,
                state.selectedCategory,
                column,
                newAscending
            )
            state.copy(sortColumn = column, isSortAscending = newAscending, filteredProcesses = filtered)
        }
    }

    fun setRefreshInterval(intervalMs: Long) {
        _uiState.update { state ->
            state.copy(
                refreshIntervalMs = intervalMs,
                isAutoRefreshPaused = intervalMs == 0L
            )
        }
        startAutoRefresh()
    }

    fun toggleAutoRefreshPause() {
        _uiState.update { state ->
            val newPaused = !state.isAutoRefreshPaused
            state.copy(isAutoRefreshPaused = newPaused)
        }
    }

    fun terminateProcess(process: ProcessInfo, signal: String = "SIGTERM") {
        viewModelScope.launch(Dispatchers.IO) {
            val record = dataProvider.terminateProcess(process, signal)
            val msg = "Terminated ${process.displayTitle} (PID ${process.pid}). Freed ${record.formattedFreedMemory} RAM."
            
            withContext(Dispatchers.Main) {
                _uiState.update { state ->
                    // Remove killed process locally for instant snappy feedback
                    val updatedProcesses = state.processes.filter { it.pid != process.pid }
                    val updatedFiltered = applyFilterAndSort(
                        updatedProcesses,
                        state.searchQuery,
                        state.selectedCategory,
                        state.sortColumn,
                        state.isSortAscending
                    )
                    state.copy(
                        processes = updatedProcesses,
                        filteredProcesses = updatedFiltered,
                        killHistory = dataProvider.getKillHistory(),
                        selectedProcessForDetail = if (state.selectedProcessForDetail?.pid == process.pid) null else state.selectedProcessForDetail,
                        snackbarMessage = msg
                    )
                }
            }
        }
    }

    fun terminateSelectedProcesses() {
        val selectedPids = _uiState.value.selectedPids
        if (selectedPids.isEmpty()) return

        viewModelScope.launch(Dispatchers.IO) {
            val procsToKill = _uiState.value.processes.filter { selectedPids.contains(it.pid) }
            val (count, freedBytes) = dataProvider.terminateMultiple(procsToKill)
            val formattedFreed = SystemStats.formatBytes(freedBytes)
            val msg = "Killed $count selected processes. Freed ~$formattedFreed RAM."

            withContext(Dispatchers.Main) {
                _uiState.update { state ->
                    val updatedProcesses = state.processes.filter { !selectedPids.contains(it.pid) }
                    val updatedFiltered = applyFilterAndSort(
                        updatedProcesses,
                        state.searchQuery,
                        state.selectedCategory,
                        state.sortColumn,
                        state.isSortAscending
                    )
                    state.copy(
                        processes = updatedProcesses,
                        filteredProcesses = updatedFiltered,
                        selectedPids = emptySet(),
                        isMultiSelectMode = false,
                        killHistory = dataProvider.getKillHistory(),
                        snackbarMessage = msg
                    )
                }
            }
        }
    }

    fun terminateAllBackgroundApps() {
        viewModelScope.launch(Dispatchers.IO) {
            val bgProcs = _uiState.value.processes.filter { 
                !it.isSelf && it.isTerminable && (it.type == ProcessCategory.APP || it.type == ProcessCategory.TEST_WORKER)
            }
            val (count, freedBytes) = dataProvider.terminateMultiple(bgProcs)
            val formattedFreed = SystemStats.formatBytes(freedBytes)
            val msg = "Cleaned $count background tasks. Freed ~$formattedFreed RAM."

            withContext(Dispatchers.Main) {
                _uiState.update { state ->
                    val killedPids = bgProcs.map { it.pid }.toSet()
                    val updatedProcesses = state.processes.filter { !killedPids.contains(it.pid) }
                    val updatedFiltered = applyFilterAndSort(
                        updatedProcesses,
                        state.searchQuery,
                        state.selectedCategory,
                        state.sortColumn,
                        state.isSortAscending
                    )
                    state.copy(
                        processes = updatedProcesses,
                        filteredProcesses = updatedFiltered,
                        killHistory = dataProvider.getKillHistory(),
                        snackbarMessage = msg
                    )
                }
            }
        }
    }

    fun spawnTestTask(name: String, type: String, targetCpu: Double, memoryMb: Int) {
        val workerId = dataProvider.spawnTestTask(name, type, targetCpu, memoryMb)
        _uiState.update { 
            it.copy(
                showSpawnTaskDialog = false,
                snackbarMessage = "Spawned test worker '$name' (~$memoryMb MB, ${targetCpu}% CPU)"
            ) 
        }
        refreshNow()
    }

    fun toggleProcessSelection(pid: Int) {
        _uiState.update { state ->
            val updated = state.selectedPids.toMutableSet()
            if (updated.contains(pid)) {
                updated.remove(pid)
            } else {
                updated.add(pid)
            }
            state.copy(
                selectedPids = updated,
                isMultiSelectMode = updated.isNotEmpty()
            )
        }
    }

    fun selectAllFiltered() {
        _uiState.update { state ->
            val terminablePids = state.filteredProcesses
                .filter { !it.isSelf && it.isTerminable }
                .map { it.pid }
                .toSet()
            state.copy(
                selectedPids = terminablePids,
                isMultiSelectMode = terminablePids.isNotEmpty()
            )
        }
    }

    fun clearSelection() {
        _uiState.update { state ->
            state.copy(selectedPids = emptySet(), isMultiSelectMode = false)
        }
    }

    fun toggleTableViewMode() {
        _uiState.update { state ->
            state.copy(isCompactTableView = !state.isCompactTableView)
        }
    }

    fun setSelectedTimeWindow(seconds: Int) {
        _uiState.update { it.copy(selectedTimeWindowSeconds = seconds) }
    }

    fun setSelectedChartMetric(filter: ChartMetricFilter) {
        _uiState.update { it.copy(selectedChartMetric = filter) }
    }

    fun toggleChartExpanded() {
        _uiState.update { it.copy(isChartExpanded = !it.isChartExpanded) }
    }

    fun openProcessDetail(process: ProcessInfo) {
        _uiState.update { it.copy(selectedProcessForDetail = process) }
    }

    fun closeProcessDetail() {
        _uiState.update { it.copy(selectedProcessForDetail = null) }
    }

    fun setShowSpawnTaskDialog(show: Boolean) {
        _uiState.update { it.copy(showSpawnTaskDialog = show) }
    }

    fun setShowKillHistoryDialog(show: Boolean) {
        _uiState.update { it.copy(showKillHistoryDialog = show) }
    }

    fun setShowExportDialog(show: Boolean) {
        _uiState.update { it.copy(showExportDialog = show) }
    }

    fun setShowSystemInfoDialog(show: Boolean) {
        _uiState.update { it.copy(showSystemInfoDialog = show) }
    }

    fun clearKillHistory() {
        dataProvider.clearKillHistory()
        _uiState.update { it.copy(killHistory = emptyList(), snackbarMessage = "Kill history cleared") }
    }

    fun dismissSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    fun generateExportContent(format: String): String {
        val processes = _uiState.value.filteredProcesses
        val stats = _uiState.value.systemStats
        val timestamp = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).format(java.util.Date())

        return if (format.uppercase() == "CSV") {
            buildString {
                appendLine("# ProcMaster Snapshot Export - $timestamp")
                appendLine("# CPU: ${String.format("%.1f", stats.totalCpuUsagePercent)}% | RAM: ${stats.formattedUsedRam}/${stats.formattedTotalRam}")
                appendLine("PID,PPID,NAME,APP_LABEL,USER,CPU_PERCENT,MEMORY_MB,STATE,THREADS,COMMAND_LINE")
                for (p in processes) {
                    val memMb = String.format("%.2f", p.memoryBytes.toDouble() / (1024 * 1024))
                    val cleanCmd = p.cmdline.replace("\"", "\"\"")
                    appendLine("${p.pid},${p.ppid},\"${p.name}\",\"${p.appLabel}\",\"${p.user}\",${p.cpuPercent},$memMb,${p.state.code},${p.threadsCount},\"$cleanCmd\"")
                }
            }
        } else {
            // JSON format
            buildString {
                appendLine("{")
                appendLine("  \"export_timestamp\": \"$timestamp\",")
                appendLine("  \"system\": {")
                appendLine("    \"cpu_percent\": ${stats.totalCpuUsagePercent},")
                appendLine("    \"ram_total_bytes\": ${stats.totalMemoryBytes},")
                appendLine("    \"ram_used_bytes\": ${stats.usedMemoryBytes},")
                appendLine("    \"process_count\": ${stats.totalProcesses}")
                appendLine("  },")
                appendLine("  \"processes\": [")
                processes.forEachIndexed { index, p ->
                    val isLast = index == processes.size - 1
                    val memMb = p.memoryBytes.toDouble() / (1024 * 1024)
                    val escapedCmd = p.cmdline.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ")
                    val escapedName = p.name.replace("\"", "\\\"")
                    val escapedLabel = p.appLabel.replace("\"", "\\\"")
                    appendLine("    {")
                    appendLine("      \"pid\": ${p.pid},")
                    appendLine("      \"ppid\": ${p.ppid},")
                    appendLine("      \"name\": \"$escapedName\",")
                    appendLine("      \"app_label\": \"$escapedLabel\",")
                    appendLine("      \"user\": \"${p.user}\",")
                    appendLine("      \"cpu_percent\": ${p.cpuPercent},")
                    appendLine("      \"memory_mb\": ${String.format(java.util.Locale.US, "%.2f", memMb)},")
                    appendLine("      \"state\": \"${p.state.name}\",")
                    appendLine("      \"threads\": ${p.threadsCount},")
                    appendLine("      \"command_line\": \"$escapedCmd\"")
                    appendLine("    }${if (isLast) "" else ","}")
                }
                appendLine("  ]")
                appendLine("}")
            }
        }
    }

    private fun applyFilterAndSort(
        list: List<ProcessInfo>,
        searchQuery: String,
        category: ProcessCategoryFilter,
        sortColumn: ProcessSortColumn,
        ascending: Boolean
    ): List<ProcessInfo> {
        val q = searchQuery.trim().lowercase()

        // 1. Search Query Filter
        var filtered = if (q.isBlank()) {
            list
        } else {
            list.filter {
                it.name.lowercase().contains(q) ||
                it.appLabel.lowercase().contains(q) ||
                it.pid.toString().contains(q) ||
                it.user.lowercase().contains(q) ||
                it.cmdline.lowercase().contains(q) ||
                (it.packageName != null && it.packageName.lowercase().contains(q))
            }
        }

        // 2. Category Filter
        filtered = when (category) {
            ProcessCategoryFilter.ALL -> filtered
            ProcessCategoryFilter.USER_APPS -> filtered.filter { it.type == ProcessCategory.APP }
            ProcessCategoryFilter.SYSTEM -> filtered.filter { it.type == ProcessCategory.SYSTEM }
            ProcessCategoryFilter.SERVICES -> filtered.filter { it.type == ProcessCategory.SERVICE }
            ProcessCategoryFilter.HIGH_CPU -> filtered.filter { it.cpuPercent >= 2.0 }
            ProcessCategoryFilter.HIGH_RAM -> filtered.filter { it.memoryBytes >= 50L * 1024L * 1024L }
            ProcessCategoryFilter.TEST_WORKERS -> filtered.filter { it.type == ProcessCategory.TEST_WORKER }
        }

        // 3. Sorting
        val comparator: Comparator<ProcessInfo> = when (sortColumn) {
            ProcessSortColumn.PID -> compareBy { it.pid }
            ProcessSortColumn.NAME -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.displayTitle }
            ProcessSortColumn.CPU -> compareBy { it.cpuPercent }
            ProcessSortColumn.MEMORY -> compareBy { it.memoryBytes }
            ProcessSortColumn.USER -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.user }
            ProcessSortColumn.STATE -> compareBy { it.state.name }
            ProcessSortColumn.THREADS -> compareBy { it.threadsCount }
        }

        return if (ascending) {
            filtered.sortedWith(comparator)
        } else {
            filtered.sortedWith(comparator.reversed())
        }
    }
}
