package com.example.data.model

enum class ProcessState(val code: String, val displayName: String) {
    RUNNING("R", "Running"),
    SLEEPING("S", "Sleeping"),
    DISK_SLEEP("D", "Disk Sleep"),
    ZOMBIE("Z", "Zombie"),
    STOPPED("T", "Stopped"),
    PAGING("W", "Paging"),
    IDLE("I", "Idle"),
    UNKNOWN("?", "Unknown");

    companion object {
        fun fromCode(code: String): ProcessState {
            return when (code.trim().uppercase().take(1)) {
                "R" -> RUNNING
                "S" -> SLEEPING
                "D" -> DISK_SLEEP
                "Z" -> ZOMBIE
                "T" -> STOPPED
                "W" -> PAGING
                "I" -> IDLE
                else -> UNKNOWN
            }
        }
    }
}

enum class ProcessCategory(val label: String) {
    APP("App"),
    SYSTEM("System"),
    SERVICE("Service"),
    DAEMON("Daemon"),
    TEST_WORKER("Test Task")
}

enum class ProcessCategoryFilter(val label: String) {
    ALL("All"),
    USER_APPS("User Apps"),
    SYSTEM("System"),
    SERVICES("Services"),
    HIGH_CPU("High CPU (>2%)"),
    HIGH_RAM("High RAM (>50MB)"),
    TEST_WORKERS("Test Tasks")
}

enum class ProcessSortColumn(val label: String) {
    PID("PID"),
    NAME("Name"),
    CPU("CPU %"),
    MEMORY("Memory"),
    USER("User"),
    STATE("State"),
    THREADS("Threads")
}

data class ProcessInfo(
    val pid: Int,
    val ppid: Int = 0,
    val name: String,
    val appLabel: String = "",
    val packageName: String? = null,
    val cmdline: String = "",
    val user: String = "u0_a0",
    val uid: Int = 1000,
    val cpuPercent: Double = 0.0,
    val memoryBytes: Long = 0L,
    val rssKb: Long = 0L,
    val vszKb: Long = 0L,
    val memoryPercent: Double = 0.0,
    val threadsCount: Int = 1,
    val state: ProcessState = ProcessState.RUNNING,
    val priority: Int = 20,
    val nice: Int = 0,
    val startTime: String = "",
    val type: ProcessCategory = ProcessCategory.APP,
    val isTerminable: Boolean = true,
    val isTestWorker: Boolean = false,
    val workerId: String? = null,
    val isSelf: Boolean = false
) {
    val displayTitle: String
        get() = if (appLabel.isNotBlank()) appLabel else name

    val formattedMemory: String
        get() {
            val mb = memoryBytes.toDouble() / (1024 * 1024)
            return if (mb >= 1024) {
                String.format("%.2f GB", mb / 1024.0)
            } else if (mb >= 1.0) {
                String.format("%.1f MB", mb)
            } else {
                val kb = memoryBytes / 1024
                "$kb KB"
            }
        }

    val formattedVsz: String
        get() {
            val mb = vszKb.toDouble() / 1024.0
            return if (mb >= 1024) {
                String.format("%.1f GB", mb / 1024.0)
            } else {
                String.format("%.1f MB", mb)
            }
        }
}

data class SystemStats(
    val totalCpuUsagePercent: Double = 0.0,
    val cpuCores: List<Double> = emptyList(),
    val totalMemoryBytes: Long = 0L,
    val usedMemoryBytes: Long = 0L,
    val availableMemoryBytes: Long = 0L,
    val cachedMemoryBytes: Long = 0L,
    val swapTotalBytes: Long = 0L,
    val swapUsedBytes: Long = 0L,
    val totalProcesses: Int = 0,
    val runningProcesses: Int = 0,
    val sleepingProcesses: Int = 0,
    val totalThreads: Int = 0,
    val systemUptime: String = "",
    val osVersion: String = "",
    val deviceModel: String = "",
    val coreCount: Int = 8
) {
    val memoryUsagePercent: Double
        get() = if (totalMemoryBytes > 0) {
            (usedMemoryBytes.toDouble() / totalMemoryBytes) * 100.0
        } else 0.0

    val formattedTotalRam: String
        get() = formatBytes(totalMemoryBytes)

    val formattedUsedRam: String
        get() = formatBytes(usedMemoryBytes)

    val formattedAvailableRam: String
        get() = formatBytes(availableMemoryBytes)

    companion object {
        fun formatBytes(bytes: Long): String {
            val mb = bytes.toDouble() / (1024 * 1024)
            return if (mb >= 1024) {
                String.format("%.2f GB", mb / 1024.0)
            } else {
                String.format("%.0f MB", mb)
            }
        }
    }
}

data class KillRecord(
    val id: String = java.util.UUID.randomUUID().toString(),
    val pid: Int,
    val processName: String,
    val freedMemoryBytes: Long,
    val timestamp: Long = System.currentTimeMillis(),
    val success: Boolean = true,
    val signalName: String = "SIGTERM"
) {
    val formattedFreedMemory: String
        get() = SystemStats.formatBytes(freedMemoryBytes)
}
