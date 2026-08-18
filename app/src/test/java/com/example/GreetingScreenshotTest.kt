package com.example

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.data.model.ProcessCategory
import com.example.data.model.ProcessCategoryFilter
import com.example.data.model.ProcessInfo
import com.example.data.model.ProcessSortColumn
import com.example.data.model.ProcessState
import com.example.data.model.SystemStats
import com.example.ui.components.DetailedProcessCard
import com.example.ui.components.ProcessFilterBar
import com.example.ui.components.SystemMetricsHeader
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.Slate950
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [34])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun greeting_screenshot() {
    composeTestRule.setContent {
      MyApplicationTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = Slate950) {
          SystemMetricsHeader(
            stats = SystemStats(
              totalCpuUsagePercent = 24.5,
              cpuCores = listOf(15.0, 32.0, 10.0, 45.0, 12.0, 18.0, 25.0, 30.0),
              totalMemoryBytes = 8L * 1024L * 1024L * 1024L,
              usedMemoryBytes = 3600L * 1024L * 1024L,
              availableMemoryBytes = 4400L * 1024L * 1024L,
              totalProcesses = 142,
              runningProcesses = 8,
              totalThreads = 480,
              systemUptime = "12h 45m",
              coreCount = 8
            ),
            isPaused = false,
            isRefreshing = false,
            onTogglePause = {},
            onManualRefresh = {},
            onKillAllBackground = {},
            onSpawnTestTask = {},
            onShowKillHistory = {},
            onShowExport = {},
            onShowSystemInfo = {}
          )
        }
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
  }
}
