package com.example.data.model

data class MetricPoint(
    val timestampMs: Long = System.currentTimeMillis(),
    val cpuPercent: Double = 0.0,
    val memoryPercent: Double = 0.0,
    val memoryUsedBytes: Long = 0L,
    val memoryTotalBytes: Long = 0L
)

enum class ChartMetricFilter(val label: String) {
    BOTH("CPU & Memory"),
    CPU_ONLY("CPU Only"),
    MEMORY_ONLY("Memory Only")
}
