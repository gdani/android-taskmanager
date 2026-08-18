package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.ProcessCategory
import com.example.data.model.ProcessInfo
import com.example.data.model.ProcessState
import com.example.data.provider.ProcessDataProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("ProcMaster", appName)
  }

  @Test
  fun `test process provider extraction and worker spawn`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val scope = CoroutineScope(Dispatchers.Unconfined)
    val provider = ProcessDataProvider(context, scope)

    val procs = provider.fetchRunningProcesses(8L * 1024L * 1024L * 1024L)
    assertTrue("Processes list should not be empty", procs.isNotEmpty())

    // Check that command lines are populated
    val hasCmdlines = procs.any { it.cmdline.isNotBlank() }
    assertTrue("At least one process should have a command line", hasCmdlines)

    // Spawn a custom test task and terminate it
    val workerId = provider.spawnTestTask("unit-test-task", "UnitTest", 5.0, 32)
    val updatedProcs = provider.fetchRunningProcesses(8L * 1024L * 1024L * 1024L)
    val foundWorker = updatedProcs.find { it.workerId == workerId }
    assertNotNull("Spawned test task should appear in running processes", foundWorker)

    val record = provider.terminateProcess(foundWorker!!)
    assertTrue("Worker should be terminated successfully", record.success)
  }

  @Test
  fun `test metric point and trend window data filtering`() {
    val now = System.currentTimeMillis()
    val points = listOf(
      com.example.data.model.MetricPoint(timestampMs = now - 250_000L, cpuPercent = 12.0, memoryPercent = 45.0),
      com.example.data.model.MetricPoint(timestampMs = now - 90_000L, cpuPercent = 25.0, memoryPercent = 48.0),
      com.example.data.model.MetricPoint(timestampMs = now - 30_000L, cpuPercent = 18.0, memoryPercent = 50.0),
      com.example.data.model.MetricPoint(timestampMs = now, cpuPercent = 20.0, memoryPercent = 51.0)
    )

    // 60s window should only include points >= now - 60,000L
    val window60 = points.filter { it.timestampMs >= now - 60_000L }
    assertEquals(2, window60.size)

    // 120s window should include points >= now - 120,000L
    val window120 = points.filter { it.timestampMs >= now - 120_000L }
    assertEquals(3, window120.size)

    // 300s window should include all 4 points
    val window300 = points.filter { it.timestampMs >= now - 300_000L }
    assertEquals(4, window300.size)
  }
}
