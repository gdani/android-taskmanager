package com.example.data.model

import java.util.Locale

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
    BACKGROUND("Background"),
    SERVICES("Services"),
    DAEMONS("Daemons"),
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
    val isSelf: Boolean = false,
    // Storage & Cache breakdown
    val appCodeSizeBytes: Long = 0L,
    val appDataSizeBytes: Long = 0L,
    val appCacheSizeBytes: Long = 0L,
    val appTotalSizeBytes: Long = 0L,
    val isService: Boolean = false,
    val isServiceEnabled: Boolean = true
) {
    val displayTitle: String
        get() = if (appLabel.isNotBlank()) appLabel else name

    val formattedMemory: String
        get() {
            val mb = memoryBytes.toDouble() / (1024 * 1024)
            return if (mb >= 1024) {
                String.format(Locale.US, "%.2f GB", mb / 1024.0)
            } else if (mb >= 1.0) {
                String.format(Locale.US, "%.1f MB", mb)
            } else {
                val kb = memoryBytes / 1024
                "$kb KB"
            }
        }

    val formattedVsz: String
        get() {
            val mb = vszKb.toDouble() / 1024.0
            return if (mb >= 1024) {
                String.format(Locale.US, "%.1f GB", mb / 1024.0)
            } else {
                String.format(Locale.US, "%.1f MB", mb)
            }
        }

    val formattedCodeSize: String
        get() = SystemStats.formatBytes(appCodeSizeBytes)

    val formattedDataSize: String
        get() = SystemStats.formatBytes(appDataSizeBytes)

    val formattedCacheSize: String
        get() = SystemStats.formatBytes(appCacheSizeBytes)

    val formattedTotalSize: String
        get() = SystemStats.formatBytes(appTotalSizeBytes)
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
    val coreCount: Int = 8,
    // Network Metrics Rates (Bytes per sec)
    val totalRxSpeed: Long = 0L,
    val totalTxSpeed: Long = 0L,
    val wifiRxSpeed: Long = 0L,
    val wifiTxSpeed: Long = 0L,
    val mobileRxSpeed: Long = 0L,
    val mobileTxSpeed: Long = 0L,
    val bluetoothRxSpeed: Long = 0L,
    val bluetoothTxSpeed: Long = 0L,
    // Network Cumulative Bytes
    val totalRxBytes: Long = 0L,
    val totalTxBytes: Long = 0L,
    val wifiRxBytes: Long = 0L,
    val wifiTxBytes: Long = 0L,
    val mobileRxBytes: Long = 0L,
    val mobileTxBytes: Long = 0L,
    val bluetoothRxBytes: Long = 0L,
    val bluetoothTxBytes: Long = 0L,
    // Network Info & Addressing
    val localIp: String = "192.168.1.105",
    val hostname: String = "android-device.lan",
    val dnsServer: String = "8.8.8.8, 1.1.1.1",
    val gatewayIp: String = "192.168.1.1",
    val externalIp: String = "142.250.190.46",
    val wifiSignalStrength: String = "85% (-52 dBm)",
    val cellularSignalStrength: String = "4G LTE (4/5 bars)",
    val wifiSsid: String = "Home_WiFi_5G",
    val mobileCarrierName: String = "T-Mobile 4G"
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

    val formattedCachedRam: String
        get() = formatBytes(if (cachedMemoryBytes > 0) cachedMemoryBytes else (totalMemoryBytes - usedMemoryBytes - availableMemoryBytes).coerceAtLeast(0L))

    val formattedTotalRxSpeed: String
        get() = formatSpeed(totalRxSpeed)

    val formattedTotalTxSpeed: String
        get() = formatSpeed(totalTxSpeed)

    val formattedWifiRxSpeed: String
        get() = formatSpeed(wifiRxSpeed)

    val formattedWifiTxSpeed: String
        get() = formatSpeed(wifiTxSpeed)

    val formattedMobileRxSpeed: String
        get() = formatSpeed(mobileRxSpeed)

    val formattedMobileTxSpeed: String
        get() = formatSpeed(mobileTxSpeed)

    val formattedBluetoothRxSpeed: String
        get() = formatSpeed(bluetoothRxSpeed)

    val formattedBluetoothTxSpeed: String
        get() = formatSpeed(bluetoothTxSpeed)

    companion object {
        fun formatBytes(bytes: Long): String {
            if (bytes <= 0) return "0 B"
            val kb = bytes.toDouble() / 1024.0
            val mb = kb / 1024.0
            val gb = mb / 1024.0
            return when {
                gb >= 1.0 -> String.format(Locale.US, "%.2f GB", gb)
                mb >= 1.0 -> String.format(Locale.US, "%.1f MB", mb)
                kb >= 1.0 -> String.format(Locale.US, "%.0f KB", kb)
                else -> "$bytes B"
            }
        }

        fun formatSpeed(bytesPerSec: Long): String {
            if (bytesPerSec <= 0) return "0 B/s"
            val kb = bytesPerSec.toDouble() / 1024.0
            val mb = kb / 1024.0
            return when {
                mb >= 1.0 -> String.format(Locale.US, "%.2f MB/s", mb)
                kb >= 1.0 -> String.format(Locale.US, "%.1f KB/s", kb)
                else -> "$bytesPerSec B/s"
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
