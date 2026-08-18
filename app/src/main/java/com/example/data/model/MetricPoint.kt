package com.example.data.model

data class MetricPoint(
    val timestampMs: Long = System.currentTimeMillis(),
    val cpuPercent: Double = 0.0,
    val memoryPercent: Double = 0.0,
    val memoryUsedBytes: Long = 0L,
    val memoryTotalBytes: Long = 0L,
    // Network Metrics (Bytes/sec throughput)
    val totalRxSpeedBytesPerSec: Long = 0L,
    val totalTxSpeedBytesPerSec: Long = 0L,
    val wifiRxSpeedBytesPerSec: Long = 0L,
    val wifiTxSpeedBytesPerSec: Long = 0L,
    val mobileRxSpeedBytesPerSec: Long = 0L,
    val mobileTxSpeedBytesPerSec: Long = 0L,
    val bluetoothRxSpeedBytesPerSec: Long = 0L,
    val bluetoothTxSpeedBytesPerSec: Long = 0L,
    val cumulativeRxBytes: Long = 0L,
    val cumulativeTxBytes: Long = 0L
)

enum class ChartMetricFilter(val label: String) {
    BOTH("CPU & RAM"),
    CPU_ONLY("CPU Only"),
    MEMORY_ONLY("RAM Only"),
    NETWORK_TOTAL("All Network"),
    NETWORK_WIFI("Wi-Fi"),
    NETWORK_MOBILE("Mobile Data"),
    NETWORK_BT("Bluetooth")
}
