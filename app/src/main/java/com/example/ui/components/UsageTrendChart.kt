package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ChartMetricFilter
import com.example.data.model.MetricPoint
import com.example.data.model.SystemStats
import com.example.ui.theme.SleekBorder
import com.example.ui.theme.SleekBorderLight
import com.example.ui.theme.SleekMemory
import com.example.ui.theme.SleekOnBackground
import com.example.ui.theme.SleekOnPrimaryContainer
import com.example.ui.theme.SleekPrimary
import com.example.ui.theme.SleekPrimaryBorder
import com.example.ui.theme.SleekPrimaryContainer
import com.example.ui.theme.SleekSecondaryContainer
import com.example.ui.theme.SleekSuccess
import com.example.ui.theme.SleekSurface
import com.example.ui.theme.SleekSurfaceVariant
import com.example.ui.theme.SleekTextMuted
import com.example.ui.theme.SleekTextSubtle
import com.example.ui.theme.SleekWarning
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max

@Composable
fun UsageTrendChart(
    metricHistory: List<MetricPoint>,
    selectedTimeWindowSeconds: Int,
    onSelectTimeWindow: (Int) -> Unit,
    selectedMetricFilter: ChartMetricFilter,
    onSelectMetricFilter: (ChartMetricFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    var scrubbedPoint by remember { mutableStateOf<MetricPoint?>(null) }
    var scrubbedXRatio by remember { mutableStateOf<Float?>(null) }
    val textMeasurer = rememberTextMeasurer()

    val infiniteTransition = rememberInfiniteTransition(label = "pulse_chart")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha_chart"
    )

    val now = System.currentTimeMillis()
    val windowMs = selectedTimeWindowSeconds * 1000L
    val startTimeMs = now - windowMs

    val windowPoints = remember(metricHistory, selectedTimeWindowSeconds, now) {
        val points = metricHistory.filter { it.timestampMs >= startTimeMs }
        if (points.isNotEmpty()) points else metricHistory.takeLast(10)
    }

    val isNetworkChart = selectedMetricFilter in listOf(
        ChartMetricFilter.NETWORK_TOTAL,
        ChartMetricFilter.NETWORK_WIFI,
        ChartMetricFilter.NETWORK_MOBILE,
        ChartMetricFilter.NETWORK_BT
    )

    // Summary calculations
    val avgCpu = if (windowPoints.isNotEmpty()) windowPoints.map { it.cpuPercent }.average() else 0.0
    val maxCpu = if (windowPoints.isNotEmpty()) windowPoints.maxOf { it.cpuPercent } else 0.0
    val avgRamBytes = if (windowPoints.isNotEmpty()) (windowPoints.map { it.memoryUsedBytes }.average()).toLong() else 0L
    val maxRamBytes = if (windowPoints.isNotEmpty()) windowPoints.maxOf { it.memoryUsedBytes } else 0L

    // Network throughput bounds
    val maxRxSpeed = if (windowPoints.isNotEmpty()) {
        when (selectedMetricFilter) {
            ChartMetricFilter.NETWORK_WIFI -> windowPoints.maxOf { it.wifiRxSpeedBytesPerSec }
            ChartMetricFilter.NETWORK_MOBILE -> windowPoints.maxOf { it.mobileRxSpeedBytesPerSec }
            ChartMetricFilter.NETWORK_BT -> windowPoints.maxOf { it.bluetoothRxSpeedBytesPerSec }
            else -> windowPoints.maxOf { it.totalRxSpeedBytesPerSec }
        }
    } else 1024L
    val maxTxSpeed = if (windowPoints.isNotEmpty()) {
        when (selectedMetricFilter) {
            ChartMetricFilter.NETWORK_WIFI -> windowPoints.maxOf { it.wifiTxSpeedBytesPerSec }
            ChartMetricFilter.NETWORK_MOBILE -> windowPoints.maxOf { it.mobileTxSpeedBytesPerSec }
            ChartMetricFilter.NETWORK_BT -> windowPoints.maxOf { it.bluetoothTxSpeedBytesPerSec }
            else -> windowPoints.maxOf { it.totalTxSpeedBytesPerSec }
        }
    } else 1024L
    val networkMaxScaleBytes = max(10 * 1024L, max(maxRxSpeed, maxTxSpeed) * 12 / 10)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(SleekSurfaceVariant)
            .border(1.dp, SleekBorder, RoundedCornerShape(18.dp))
            .padding(14.dp)
            .testTag("usage_trend_chart")
    ) {
        // Top Header: Title & Time Window Selector
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(SleekPrimaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ShowChart,
                        contentDescription = "Usage Trends",
                        tint = SleekOnPrimaryContainer,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (isNetworkChart) "Network Traffic History" else "Resource Trends",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            ),
                            color = SleekOnBackground
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(SleekPrimary.copy(alpha = pulseAlpha))
                        )
                    }
                    Text(
                        text = if (isNetworkChart) "Real-time Rx/Tx network bandwidth graph" else "Live CPU & RAM trajectory analysis",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = SleekTextMuted
                    )
                }
            }

            // Time Window Selector (1m / 2m / 5m)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                val windows = listOf(60 to "1m", 120 to "2m", 300 to "5m")
                windows.forEach { (sec, label) ->
                    val isSelected = selectedTimeWindowSeconds == sec
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) SleekPrimary else SleekSurface)
                            .border(1.dp, if (isSelected) SleekPrimary else SleekBorder, RoundedCornerShape(8.dp))
                            .clickable { onSelectTimeWindow(sec) }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .testTag("time_window_${sec}s"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            ),
                            color = if (isSelected) Color.White else SleekTextMuted
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Horizontal Metric Selector Tabs (CPU & RAM, CPU, RAM, All Network, Wi-Fi, Mobile, BT) (Req #3)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ChartMetricFilter.values().forEach { filter ->
                val isSelected = selectedMetricFilter == filter
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) SleekPrimaryContainer else SleekSurface)
                        .border(1.dp, if (isSelected) SleekPrimaryBorder else SleekBorderLight, RoundedCornerShape(12.dp))
                        .clickable { onSelectMetricFilter(filter) }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                        .testTag("chart_metric_toggle_${filter.name.lowercase()}")
                ) {
                    Text(
                        text = filter.label,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        ),
                        color = if (isSelected) SleekPrimary else SleekTextMuted
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Legend & Quick Stats Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isNetworkChart) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(SleekSuccess))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Download (Rx)", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold), color = SleekSuccess)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(SleekPrimary))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Upload (Tx)", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold), color = SleekPrimary)
                    }
                }
                Text(
                    text = "Peak: ${SystemStats.formatSpeed(max(maxRxSpeed, maxTxSpeed))}",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontSize = 10.sp),
                    color = SleekTextMuted
                )
            } else {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (selectedMetricFilter != ChartMetricFilter.MEMORY_ONLY) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(SleekPrimary))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("CPU", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold), color = SleekPrimary)
                        }
                    }
                    if (selectedMetricFilter != ChartMetricFilter.CPU_ONLY) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(SleekMemory))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("RAM", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold), color = SleekMemory)
                        }
                    }
                }

                Text(
                    text = if (selectedMetricFilter == ChartMetricFilter.CPU_ONLY) "Peak: ${String.format(Locale.US, "%.1f", maxCpu)}%" else "RAM Peak: ${SystemStats.formatBytes(maxRamBytes)}",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontSize = 10.sp),
                    color = SleekTextMuted
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Tooltip or Summary Card
        if (scrubbedPoint != null) {
            val p = scrubbedPoint!!
            val diffSec = ((now - p.timestampMs) / 1000).coerceAtLeast(0)
            val timeStr = if (diffSec == 0L) "Now" else "-${diffSec}s ago"
            val clockTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(p.timestampMs))

            val rxSpeed = when (selectedMetricFilter) {
                ChartMetricFilter.NETWORK_WIFI -> p.wifiRxSpeedBytesPerSec
                ChartMetricFilter.NETWORK_MOBILE -> p.mobileRxSpeedBytesPerSec
                ChartMetricFilter.NETWORK_BT -> p.bluetoothRxSpeedBytesPerSec
                else -> p.totalRxSpeedBytesPerSec
            }
            val txSpeed = when (selectedMetricFilter) {
                ChartMetricFilter.NETWORK_WIFI -> p.wifiTxSpeedBytesPerSec
                ChartMetricFilter.NETWORK_MOBILE -> p.mobileTxSpeedBytesPerSec
                ChartMetricFilter.NETWORK_BT -> p.bluetoothTxSpeedBytesPerSec
                else -> p.totalTxSpeedBytesPerSec
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(SleekPrimaryContainer)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                    .testTag("chart_scrubber_tooltip"),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "⏱ $timeStr ($clockTime)",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    ),
                    color = SleekOnPrimaryContainer
                )

                if (isNetworkChart) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "↓ ${SystemStats.formatSpeed(rxSpeed)}",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 11.sp),
                            color = SleekSuccess
                        )
                        Text(
                            text = "↑ ${SystemStats.formatSpeed(txSpeed)}",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 11.sp),
                            color = SleekPrimary
                        )
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (selectedMetricFilter != ChartMetricFilter.MEMORY_ONLY) {
                            Text(
                                text = "CPU: ${String.format(Locale.US, "%.1f", p.cpuPercent)}%",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                ),
                                color = SleekPrimary
                            )
                        }
                        if (selectedMetricFilter != ChartMetricFilter.CPU_ONLY) {
                            Text(
                                text = "RAM: ${SystemStats.formatBytes(p.memoryUsedBytes)}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                ),
                                color = SleekMemory
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // High-Performance Interactive Chart Canvas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(SleekSurface)
                .border(1.dp, SleekBorderLight, RoundedCornerShape(12.dp))
                .padding(8.dp)
                .pointerInput(windowPoints, startTimeMs, now) {
                    detectTapGestures(
                        onPress = { offset ->
                            val leftMargin = 38.dp.toPx()
                            val availableWidth = (size.width - leftMargin - 4.dp.toPx()).coerceAtLeast(1f)
                            val ratio = ((offset.x - leftMargin) / availableWidth).coerceIn(0f, 1f)
                            val targetTime = startTimeMs + (ratio * (now - startTimeMs)).toLong()
                            scrubbedPoint = windowPoints.minByOrNull { abs(it.timestampMs - targetTime) }
                            scrubbedXRatio = ratio
                            tryAwaitRelease()
                            scrubbedPoint = null
                            scrubbedXRatio = null
                        }
                    )
                }
                .pointerInput(windowPoints, startTimeMs, now) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val leftMargin = 38.dp.toPx()
                            val availableWidth = (size.width - leftMargin - 4.dp.toPx()).coerceAtLeast(1f)
                            val ratio = ((offset.x - leftMargin) / availableWidth).coerceIn(0f, 1f)
                            val targetTime = startTimeMs + (ratio * (now - startTimeMs)).toLong()
                            scrubbedPoint = windowPoints.minByOrNull { abs(it.timestampMs - targetTime) }
                            scrubbedXRatio = ratio
                        },
                        onDragEnd = {
                            scrubbedPoint = null
                            scrubbedXRatio = null
                        },
                        onDragCancel = {
                            scrubbedPoint = null
                            scrubbedXRatio = null
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            val leftMargin = 38.dp.toPx()
                            val availableWidth = (size.width - leftMargin - 4.dp.toPx()).coerceAtLeast(1f)
                            val ratio = ((change.position.x - leftMargin) / availableWidth).coerceIn(0f, 1f)
                            val targetTime = startTimeMs + (ratio * (now - startTimeMs)).toLong()
                            scrubbedPoint = windowPoints.minByOrNull { abs(it.timestampMs - targetTime) }
                            scrubbedXRatio = ratio
                        }
                    )
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val canvasWidth = size.width
                val canvasHeight = size.height
                val leftPadding = 38.dp.toPx()
                val rightPadding = 6.dp.toPx()
                val topPadding = 6.dp.toPx()
                val bottomPadding = 18.dp.toPx()

                val plotWidth = canvasWidth - leftPadding - rightPadding
                val plotHeight = canvasHeight - topPadding - bottomPadding
                val plotTop = topPadding
                val plotBottom = topPadding + plotHeight
                val plotLeft = leftPadding

                if (plotWidth <= 0 || plotHeight <= 0) return@Canvas

                val gridColor = Color(0xFFE2DDE8)
                val axisTextStyle = TextStyle(
                    color = SleekTextMuted,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace
                )

                // 1. Draw Grid Lines and Y-Axis Labels
                val ySteps = listOf(0, 25, 50, 75, 100)
                ySteps.forEach { stepPercent ->
                    val y = plotBottom - (stepPercent / 100f) * plotHeight
                    drawLine(
                        color = gridColor,
                        start = Offset(plotLeft, y),
                        end = Offset(plotLeft + plotWidth, y),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = if (stepPercent in 1..99) PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f) else null
                    )

                    val label = if (isNetworkChart) {
                        val bytesVal = (networkMaxScaleBytes * (stepPercent / 100.0)).toLong()
                        SystemStats.formatSpeed(bytesVal)
                    } else {
                        "$stepPercent%"
                    }

                    val labelY = (y - 5.dp.toPx()).coerceIn(0f, (canvasHeight - 12.dp.toPx()).coerceAtLeast(0f))
                    drawText(
                        textMeasurer = textMeasurer,
                        text = label,
                        topLeft = Offset(0f, labelY),
                        style = axisTextStyle
                    )
                }

                // 2. Draw X-Axis Time Ticks
                val timeTicksCount = 4
                for (i in 0..timeTicksCount) {
                    val ratio = i.toFloat() / timeTicksCount
                    val x = plotLeft + (ratio * plotWidth)
                    val secAgo = selectedTimeWindowSeconds - (ratio * selectedTimeWindowSeconds).toInt()
                    val label = if (secAgo == 0) "Now" else "-${secAgo}s"

                    val labelX = (x - (if (secAgo == 0) 22.dp.toPx() else 10.dp.toPx())).coerceIn(plotLeft - 4.dp.toPx(), (canvasWidth - 26.dp.toPx()).coerceAtLeast(0f))
                    val labelY = (plotBottom + 2.dp.toPx()).coerceIn(0f, (canvasHeight - 12.dp.toPx()).coerceAtLeast(0f))

                    drawText(
                        textMeasurer = textMeasurer,
                        text = label,
                        topLeft = Offset(labelX, labelY),
                        style = axisTextStyle
                    )
                }

                if (windowPoints.isEmpty()) return@Canvas

                fun calculateCoords(point: MetricPoint, valueFraction: Double): Offset {
                    val timeRatio = ((point.timestampMs - startTimeMs).toFloat() / windowMs.toFloat()).coerceIn(0f, 1f)
                    val x = plotLeft + (timeRatio * plotWidth)
                    val y = plotBottom - (valueFraction.toFloat().coerceIn(0f, 1f) * plotHeight)
                    return Offset(x, y)
                }

                if (isNetworkChart) {
                    // Draw Download (Rx) curve
                    val rxCoords = windowPoints.map { p ->
                        val bytes = when (selectedMetricFilter) {
                            ChartMetricFilter.NETWORK_WIFI -> p.wifiRxSpeedBytesPerSec
                            ChartMetricFilter.NETWORK_MOBILE -> p.mobileRxSpeedBytesPerSec
                            ChartMetricFilter.NETWORK_BT -> p.bluetoothRxSpeedBytesPerSec
                            else -> p.totalRxSpeedBytesPerSec
                        }
                        val frac = bytes.toDouble() / networkMaxScaleBytes.toDouble()
                        calculateCoords(p, frac)
                    }

                    // Draw Upload (Tx) curve
                    val txCoords = windowPoints.map { p ->
                        val bytes = when (selectedMetricFilter) {
                            ChartMetricFilter.NETWORK_WIFI -> p.wifiTxSpeedBytesPerSec
                            ChartMetricFilter.NETWORK_MOBILE -> p.mobileTxSpeedBytesPerSec
                            ChartMetricFilter.NETWORK_BT -> p.bluetoothTxSpeedBytesPerSec
                            else -> p.totalTxSpeedBytesPerSec
                        }
                        val frac = bytes.toDouble() / networkMaxScaleBytes.toDouble()
                        calculateCoords(p, frac)
                    }

                    // Render Rx path & gradient
                    if (rxCoords.size >= 2) {
                        val rxPath = Path()
                        val rxFillPath = Path()
                        rxPath.moveTo(rxCoords.first().x, rxCoords.first().y)
                        rxFillPath.moveTo(rxCoords.first().x, plotBottom)
                        rxFillPath.lineTo(rxCoords.first().x, rxCoords.first().y)

                        for (i in 1 until rxCoords.size) {
                            val prev = rxCoords[i - 1]
                            val curr = rxCoords[i]
                            val cpx1 = (prev.x + curr.x) / 2f
                            val cpy1 = prev.y
                            val cpx2 = (prev.x + curr.x) / 2f
                            val cpy2 = curr.y
                            rxPath.cubicTo(cpx1, cpy1, cpx2, cpy2, curr.x, curr.y)
                            rxFillPath.cubicTo(cpx1, cpy1, cpx2, cpy2, curr.x, curr.y)
                        }
                        rxFillPath.lineTo(rxCoords.last().x, plotBottom)
                        rxFillPath.close()

                        drawPath(
                            path = rxFillPath,
                            brush = Brush.verticalGradient(
                                listOf(SleekSuccess.copy(alpha = 0.22f), SleekSuccess.copy(alpha = 0.0f)),
                                startY = plotTop,
                                endY = plotBottom
                            )
                        )
                        drawPath(
                            path = rxPath,
                            color = SleekSuccess,
                            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )
                    }

                    // Render Tx path
                    if (txCoords.size >= 2) {
                        val txPath = Path()
                        txPath.moveTo(txCoords.first().x, txCoords.first().y)
                        for (i in 1 until txCoords.size) {
                            val prev = txCoords[i - 1]
                            val curr = txCoords[i]
                            val cpx1 = (prev.x + curr.x) / 2f
                            val cpy1 = prev.y
                            val cpx2 = (prev.x + curr.x) / 2f
                            val cpy2 = curr.y
                            txPath.cubicTo(cpx1, cpy1, cpx2, cpy2, curr.x, curr.y)
                        }
                        drawPath(
                            path = txPath,
                            color = SleekPrimary,
                            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )
                    }
                } else {
                    // CPU & RAM charts
                    if (selectedMetricFilter != ChartMetricFilter.MEMORY_ONLY) {
                        val cpuCoords = windowPoints.map { calculateCoords(it, it.cpuPercent / 100.0) }
                        if (cpuCoords.size >= 2) {
                            val cpuPath = Path()
                            val cpuFillPath = Path()
                            cpuPath.moveTo(cpuCoords.first().x, cpuCoords.first().y)
                            cpuFillPath.moveTo(cpuCoords.first().x, plotBottom)
                            cpuFillPath.lineTo(cpuCoords.first().x, cpuCoords.first().y)

                            for (i in 1 until cpuCoords.size) {
                                val prev = cpuCoords[i - 1]
                                val curr = cpuCoords[i]
                                val cpx1 = (prev.x + curr.x) / 2f
                                val cpy1 = prev.y
                                val cpx2 = (prev.x + curr.x) / 2f
                                val cpy2 = curr.y
                                cpuPath.cubicTo(cpx1, cpy1, cpx2, cpy2, curr.x, curr.y)
                                cpuFillPath.cubicTo(cpx1, cpy1, cpx2, cpy2, curr.x, curr.y)
                            }
                            cpuFillPath.lineTo(cpuCoords.last().x, plotBottom)
                            cpuFillPath.close()

                            drawPath(
                                path = cpuFillPath,
                                brush = Brush.verticalGradient(
                                    listOf(SleekPrimary.copy(alpha = 0.22f), SleekPrimary.copy(alpha = 0.0f)),
                                    startY = plotTop,
                                    endY = plotBottom
                                )
                            )
                            drawPath(
                                path = cpuPath,
                                color = SleekPrimary,
                                style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                            )
                        }
                    }

                    if (selectedMetricFilter != ChartMetricFilter.CPU_ONLY) {
                        val memCoords = windowPoints.map { calculateCoords(it, it.memoryPercent / 100.0) }
                        if (memCoords.size >= 2) {
                            val memPath = Path()
                            val memFillPath = Path()
                            memPath.moveTo(memCoords.first().x, memCoords.first().y)
                            memFillPath.moveTo(memCoords.first().x, plotBottom)
                            memFillPath.lineTo(memCoords.first().x, memCoords.first().y)

                            for (i in 1 until memCoords.size) {
                                val prev = memCoords[i - 1]
                                val curr = memCoords[i]
                                val cpx1 = (prev.x + curr.x) / 2f
                                val cpy1 = prev.y
                                val cpx2 = (prev.x + curr.x) / 2f
                                val cpy2 = curr.y
                                memPath.cubicTo(cpx1, cpy1, cpx2, cpy2, curr.x, curr.y)
                                memFillPath.cubicTo(cpx1, cpy1, cpx2, cpy2, curr.x, curr.y)
                            }
                            memFillPath.lineTo(memCoords.last().x, plotBottom)
                            memFillPath.close()

                            drawPath(
                                path = memFillPath,
                                brush = Brush.verticalGradient(
                                    listOf(SleekMemory.copy(alpha = 0.18f), SleekMemory.copy(alpha = 0.0f)),
                                    startY = plotTop,
                                    endY = plotBottom
                                )
                            )
                            drawPath(
                                path = memPath,
                                color = SleekMemory,
                                style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                            )
                        }
                    }
                }

                // Draw Scrubber Line
                scrubbedPoint?.let { sp ->
                    val timeRatio = ((sp.timestampMs - startTimeMs).toFloat() / windowMs.toFloat()).coerceIn(0f, 1f)
                    val scrubX = plotLeft + (timeRatio * plotWidth)

                    drawLine(
                        color = SleekOnBackground.copy(alpha = 0.6f),
                        start = Offset(scrubX, plotTop),
                        end = Offset(scrubX, plotBottom),
                        strokeWidth = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
                    )
                }
            }
        }
    }
}
