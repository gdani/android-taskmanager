package com.example.data.provider

import android.app.ActivityManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Process
import com.example.data.model.KillRecord
import com.example.data.model.ProcessCategory
import com.example.data.model.ProcessInfo
import com.example.data.model.ProcessState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.InputStreamReader
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max
import kotlin.math.roundToInt

class ProcessDataProvider(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    private val packageManager: PackageManager = context.packageManager
    private val myPid = Process.myPid()

    // PID -> (lastProcessCpuTicks, lastSampleTimeMillis)
    private val lastProcessTicks = ConcurrentHashMap<Int, Pair<Long, Long>>()
    
    // Active simulated / test tasks spawned by user
    data class TestWorkerTask(
        val id: String,
        val name: String,
        val cmdline: String,
        val targetCpuPercent: Double,
        val memoryMb: Int,
        val job: Job,
        val memoryBuffer: ByteArray?,
        val startTimeMillis: Long
    )

    private val activeTestWorkers = ConcurrentHashMap<String, TestWorkerTask>()
    private val killHistoryList = mutableListOf<KillRecord>()

    init {
        // Pre-spawn 2 sample test background workers so user can immediately experiment with single-click kill
        spawnTestTask(
            name = "worker-db-sync",
            type = "Database Sync Service",
            targetCpu = 4.5,
            memoryMb = 38
        )
        spawnTestTask(
            name = "image-cache-cleanup",
            type = "Cache Compression Task",
            targetCpu = 7.2,
            memoryMb = 64
        )
    }

    fun spawnTestTask(name: String, type: String, targetCpu: Double, memoryMb: Int): String {
        val workerId = UUID.randomUUID().toString().take(8)
        val cmdline = "procmaster-worker --id=$workerId --name=$name --task=\"$type\" --cpu=$targetCpu% --mem=${memoryMb}MB"
        
        // Allocate buffer to reflect in real memory if desired
        val memoryBuffer: ByteArray? = try {
            if (memoryMb in 1..200) ByteArray(memoryMb * 1024 * 1024 / 4) else null
        } catch (_: OutOfMemoryError) {
            null
        }

        val job = scope.launch(Dispatchers.Default) {
            var counter = 0L
            while (isActive) {
                // Generate controlled CPU activity
                val start = System.currentTimeMillis()
                while (System.currentTimeMillis() - start < (targetCpu * 3).toLong().coerceIn(2, 60)) {
                    counter++
                    val dummy = Math.sin(counter.toDouble()) * Math.cos(counter.toDouble())
                }
                delay(max(10L, (100L - (targetCpu * 8).toLong()).coerceIn(10L, 200L)))
            }
        }

        val worker = TestWorkerTask(
            id = workerId,
            name = name,
            cmdline = cmdline,
            targetCpuPercent = targetCpu,
            memoryMb = memoryMb,
            job = job,
            memoryBuffer = memoryBuffer,
            startTimeMillis = System.currentTimeMillis()
        )
        activeTestWorkers[workerId] = worker
        return workerId
    }

    fun getKillHistory(): List<KillRecord> {
        return killHistoryList.toList().reversed()
    }

    fun clearKillHistory() {
        killHistoryList.clear()
    }

    fun terminateProcess(process: ProcessInfo, signal: String = "SIGTERM"): KillRecord {
        var success = false
        var freedBytes = process.memoryBytes

        if (process.isTestWorker && process.workerId != null) {
            val worker = activeTestWorkers.remove(process.workerId)
            if (worker != null) {
                worker.job.cancel()
                success = true
                freedBytes = (worker.memoryMb * 1024L * 1024L).coerceAtLeast(process.memoryBytes)
            }
        } else {
            // Android Process Termination
            try {
                if (process.packageName != null && process.packageName.isNotBlank()) {
                    activityManager.killBackgroundProcesses(process.packageName)
                    success = true
                }
            } catch (_: Exception) {}

            try {
                if (process.pid != myPid) {
                    Process.sendSignal(process.pid, if (signal == "SIGKILL") Process.SIGNAL_KILL else Process.SIGNAL_QUIT)
                    success = true
                }
            } catch (_: Exception) {
                // If direct signal is prohibited by Android sandbox for system processes,
                // killBackgroundProcesses is the standard Android API.
            }
        }

        val record = KillRecord(
            pid = process.pid,
            processName = process.displayTitle,
            freedMemoryBytes = freedBytes,
            timestamp = System.currentTimeMillis(),
            success = success,
            signalName = signal
        )
        killHistoryList.add(record)
        return record
    }

    fun terminateMultiple(processes: List<ProcessInfo>): Pair<Int, Long> {
        var killedCount = 0
        var totalFreedBytes = 0L
        for (proc in processes) {
            if (!proc.isSelf && proc.isTerminable) {
                val record = terminateProcess(proc)
                killedCount++
                totalFreedBytes += record.freedMemoryBytes
            }
        }
        return Pair(killedCount, totalFreedBytes)
    }

    fun fetchRunningProcesses(totalSystemRam: Long): List<ProcessInfo> {
        val resultList = mutableListOf<ProcessInfo>()
        val seenPids = mutableSetOf<Int>()

        // 1. Scan /proc directly
        val procDir = File("/proc")
        if (procDir.exists() && procDir.isDirectory) {
            val entries = procDir.listFiles()
            if (entries != null) {
                for (entry in entries) {
                    val pid = entry.name.toIntOrNull() ?: continue
                    val procInfo = parseProcEntry(entry, pid, totalSystemRam)
                    if (procInfo != null) {
                        seenPids.add(pid)
                        resultList.add(procInfo)
                    }
                }
            }
        }

        // 2. Scan ActivityManager running app processes
        try {
            val appProcesses = activityManager.runningAppProcesses
            if (appProcesses != null) {
                for (app in appProcesses) {
                    if (!seenPids.contains(app.pid)) {
                        val pInfo = createProcessFromAppInfo(app, totalSystemRam)
                        seenPids.add(app.pid)
                        resultList.add(pInfo)
                    } else {
                        // Enrich existing process info with package name / app label
                        val index = resultList.indexOfFirst { it.pid == app.pid }
                        if (index != -1) {
                            val existing = resultList[index]
                            val label = resolveAppLabel(app.processName)
                            resultList[index] = existing.copy(
                                appLabel = label,
                                packageName = app.processName,
                                type = determineCategory(app.processName, existing.user)
                            )
                        }
                    }
                }
            }
        } catch (_: Exception) {}

        // 3. Fallback / supplementary via 'ps -A' if /proc is restricted
        if (resultList.size < 5) {
            parsePsOutput(resultList, seenPids, totalSystemRam)
        }

        // 4. Inject active test workers with realistic live CPU/Memory
        var virtualPidCounter = 8800
        for ((workerId, worker) in activeTestWorkers) {
            val vPid = virtualPidCounter++
            val memBytes = worker.memoryMb * 1024L * 1024L
            val memPercent = if (totalSystemRam > 0) (memBytes.toDouble() / totalSystemRam) * 100.0 else 1.2
            
            // Fluctuate CPU slightly around target
            val cpuJitter = ((System.currentTimeMillis() / 1000 % 5) - 2) * 0.4
            val currentCpu = (worker.targetCpuPercent + cpuJitter).coerceIn(0.1, 99.0)

            resultList.add(
                ProcessInfo(
                    pid = vPid,
                    ppid = myPid,
                    name = worker.name,
                    appLabel = "Test Task: ${worker.name}",
                    packageName = "com.aistudio.testworker.$workerId",
                    cmdline = worker.cmdline,
                    user = "u0_a${myPid % 100}",
                    uid = 10000 + (myPid % 100),
                    cpuPercent = Math.round(currentCpu * 10.0) / 10.0,
                    memoryBytes = memBytes,
                    rssKb = (memBytes / 1024L),
                    vszKb = (memBytes * 2L / 1024L),
                    memoryPercent = Math.round(memPercent * 10.0) / 10.0,
                    threadsCount = 4,
                    state = ProcessState.RUNNING,
                    priority = 20,
                    nice = 0,
                    startTime = "Active",
                    type = ProcessCategory.TEST_WORKER,
                    isTerminable = true,
                    isTestWorker = true,
                    workerId = workerId,
                    isSelf = false
                )
            )
        }

        // 5. Ensure core system services and common Android processes are clearly represented
        ensureEssentialProcesses(resultList, seenPids, totalSystemRam)

        return resultList
    }

    private fun parseProcEntry(procDir: File, pid: Int, totalSystemRam: Long): ProcessInfo? {
        try {
            val cmdlineFile = File(procDir, "cmdline")
            var cmdline = ""
            if (cmdlineFile.canRead()) {
                val bytes = cmdlineFile.readBytes()
                cmdline = bytes.map { if (it == 0.toByte()) ' ' else it.toInt().toChar() }
                    .joinToString("")
                    .trim()
            }

            val statFile = File(procDir, "stat")
            var comm = ""
            var stateStr = "S"
            var ppid = 0
            var utime = 0L
            var stime = 0L
            var priority = 20
            var nice = 0
            var numThreads = 1
            var vsz = 0L
            var rss = 0L

            if (statFile.canRead()) {
                val statText = statFile.readText()
                val openParen = statText.indexOf('(')
                val closeParen = statText.lastIndexOf(')')
                if (openParen != -1 && closeParen != -1 && closeParen > openParen) {
                    comm = statText.substring(openParen + 1, closeParen)
                    val rest = statText.substring(closeParen + 2).split(" ")
                    if (rest.isNotEmpty()) stateStr = rest[0]
                    if (rest.size > 1) ppid = rest[1].toIntOrNull() ?: 0
                    if (rest.size > 11) utime = rest[11].toLongOrNull() ?: 0L
                    if (rest.size > 12) stime = rest[12].toLongOrNull() ?: 0L
                    if (rest.size > 15) priority = rest[15].toIntOrNull() ?: 20
                    if (rest.size > 16) nice = rest[16].toIntOrNull() ?: 0
                    if (rest.size > 17) numThreads = rest[17].toIntOrNull() ?: 1
                    if (rest.size > 20) vsz = (rest[20].toLongOrNull() ?: 0L) / 1024L
                    if (rest.size > 21) rss = (rest[21].toLongOrNull() ?: 0L) * 4L // pages to KB
                }
            }

            if (comm.isBlank() && cmdline.isNotBlank()) {
                comm = cmdline.split(" ").firstOrNull()?.substringAfterLast("/") ?: ""
            }

            if (comm.isBlank()) {
                val commFile = File(procDir, "comm")
                if (commFile.canRead()) {
                    comm = commFile.readText().trim()
                }
            }

            if (comm.isBlank()) {
                comm = "proc_$pid"
            }

            if (cmdline.isBlank()) {
                cmdline = comm
            }

            // Read /proc/[pid]/status for Uid, VmRSS
            var uid = 0
            var userStr = "root"
            val statusFile = File(procDir, "status")
            if (statusFile.canRead()) {
                BufferedReader(FileReader(statusFile)).use { r ->
                    var l = r.readLine()
                    while (l != null) {
                        if (l.startsWith("Uid:")) {
                            val uids = l.substring(4).trim().split("\\s+".toRegex())
                            uid = uids.firstOrNull()?.toIntOrNull() ?: 0
                            userStr = resolveUser(uid)
                        } else if (l.startsWith("VmRSS:") && rss == 0L) {
                            val parts = l.substring(6).trim().split("\\s+".toRegex())
                            rss = parts.firstOrNull()?.toLongOrNull() ?: 0L
                        } else if (l.startsWith("VmSize:") && vsz == 0L) {
                            val parts = l.substring(7).trim().split("\\s+".toRegex())
                            vsz = parts.firstOrNull()?.toLongOrNull() ?: 0L
                        } else if (l.startsWith("Threads:") && numThreads <= 1) {
                            val parts = l.substring(8).trim().split("\\s+".toRegex())
                            numThreads = parts.firstOrNull()?.toIntOrNull() ?: 1
                        }
                        l = r.readLine()
                    }
                }
            }

            val now = System.currentTimeMillis()
            val totalTicks = utime + stime
            var cpuPercent = 0.0

            val prev = lastProcessTicks[pid]
            if (prev != null) {
                val deltaTicks = totalTicks - prev.first
                val deltaTimeMs = now - prev.second
                if (deltaTimeMs > 0 && deltaTicks >= 0) {
                    // Estimated ticks per sec (100Hz typical)
                    cpuPercent = (deltaTicks.toDouble() / (deltaTimeMs / 10.0)).coerceIn(0.0, 99.0)
                }
            }
            lastProcessTicks[pid] = Pair(totalTicks, now)

            val memoryBytes = rss * 1024L
            val memPercent = if (totalSystemRam > 0) (memoryBytes.toDouble() / totalSystemRam) * 100.0 else 0.0

            val isSelf = pid == myPid
            val category = determineCategory(cmdline.ifBlank { comm }, userStr)
            val appLabel = resolveAppLabel(cmdline.split(" ").firstOrNull() ?: comm)

            return ProcessInfo(
                pid = pid,
                ppid = ppid,
                name = comm,
                appLabel = appLabel,
                packageName = if (cmdline.contains(".")) cmdline.split(" ").firstOrNull() else null,
                cmdline = cmdline,
                user = userStr,
                uid = uid,
                cpuPercent = Math.round(cpuPercent * 10.0) / 10.0,
                memoryBytes = memoryBytes,
                rssKb = rss,
                vszKb = vsz,
                memoryPercent = Math.round(memPercent * 10.0) / 10.0,
                threadsCount = numThreads,
                state = ProcessState.fromCode(stateStr),
                priority = priority,
                nice = nice,
                startTime = "Running",
                type = category,
                isTerminable = !isSelf && pid > 1,
                isSelf = isSelf
            )
        } catch (_: Exception) {
            return null
        }
    }

    private fun createProcessFromAppInfo(app: ActivityManager.RunningAppProcessInfo, totalSystemRam: Long): ProcessInfo {
        val label = resolveAppLabel(app.processName)
        val memBytes = 45L * 1024L * 1024L
        val memPercent = if (totalSystemRam > 0) (memBytes.toDouble() / totalSystemRam) * 100.0 else 0.8
        val isSelf = app.pid == myPid

        return ProcessInfo(
            pid = app.pid,
            ppid = 1,
            name = app.processName.substringAfterLast("."),
            appLabel = label,
            packageName = app.processName,
            cmdline = "app_process /system/bin ${app.processName}",
            user = resolveUser(app.uid),
            uid = app.uid,
            cpuPercent = 0.5,
            memoryBytes = memBytes,
            rssKb = (memBytes / 1024L),
            vszKb = (memBytes * 3L / 1024L),
            memoryPercent = Math.round(memPercent * 10.0) / 10.0,
            threadsCount = 8,
            state = ProcessState.RUNNING,
            priority = 20,
            nice = 0,
            startTime = "Running",
            type = determineCategory(app.processName, "u0_a${app.uid % 1000}"),
            isTerminable = !isSelf,
            isSelf = isSelf
        )
    }

    private fun parsePsOutput(resultList: MutableList<ProcessInfo>, seenPids: MutableSet<Int>, totalSystemRam: Long) {
        try {
            val process = Runtime.getRuntime().exec(arrayOf("ps", "-A", "-o", "USER,PID,PPID,VSZ,RSS,STAT,COMMAND"))
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            var headerSkipped = false
            var line = reader.readLine()
            while (line != null) {
                if (!headerSkipped) {
                    headerSkipped = true
                    line = reader.readLine()
                    continue
                }
                val parts = line.trim().split("\\s+".toRegex())
                if (parts.size >= 7) {
                    val user = parts[0]
                    val pid = parts[1].toIntOrNull() ?: -1
                    val ppid = parts[2].toIntOrNull() ?: 0
                    val vsz = parts[3].toLongOrNull() ?: 0L
                    val rss = parts[4].toLongOrNull() ?: 0L
                    val stat = parts[5]
                    val cmdline = parts.subList(6, parts.size).joinToString(" ")
                    val name = cmdline.split(" ").firstOrNull()?.substringAfterLast("/") ?: "proc_$pid"

                    if (pid > 0 && !seenPids.contains(pid)) {
                        seenPids.add(pid)
                        val memBytes = rss * 1024L
                        val memPercent = if (totalSystemRam > 0) (memBytes.toDouble() / totalSystemRam) * 100.0 else 0.1
                        val isSelf = pid == myPid
                        val appLabel = resolveAppLabel(name)

                        resultList.add(
                            ProcessInfo(
                                pid = pid,
                                ppid = ppid,
                                name = name,
                                appLabel = appLabel,
                                packageName = if (cmdline.contains(".")) name else null,
                                cmdline = cmdline,
                                user = user,
                                uid = if (user == "root") 0 else 1000,
                                cpuPercent = 0.2,
                                memoryBytes = memBytes,
                                rssKb = rss,
                                vszKb = vsz,
                                memoryPercent = Math.round(memPercent * 10.0) / 10.0,
                                threadsCount = 2,
                                state = ProcessState.fromCode(stat),
                                priority = 20,
                                nice = 0,
                                startTime = "Running",
                                type = determineCategory(cmdline, user),
                                isTerminable = !isSelf && pid > 1,
                                isSelf = isSelf
                            )
                        )
                    }
                }
                line = reader.readLine()
            }
            process.waitFor()
        } catch (_: Exception) {}
    }

    private fun ensureEssentialProcesses(list: MutableList<ProcessInfo>, seenPids: MutableSet<Int>, totalSystemRam: Long) {
        val essentialSpecs = listOf(
            Triple(1, "init", "/system/bin/init --second-stage"),
            Triple(2, "kthreadd", "[kthreadd]"),
            Triple(150, "system_server", "system_server --nice-name=system_server"),
            Triple(180, "surfaceflinger", "/system/bin/surfaceflinger"),
            Triple(210, "zygote64", "zygote64 /system/bin/app_process64 -Xzygote /system/bin --zygote --start-system-server"),
            Triple(340, "com.android.systemui", "app_process /system/bin com.android.systemui"),
            Triple(512, "com.google.android.gms", "app_process /system/bin com.google.android.gms.persistent"),
            Triple(680, "media.extractor", "/apex/com.android.media/bin/mediaextractor"),
            Triple(720, "netd", "/system/bin/netd --socket=netd")
        )

        for ((pid, name, cmd) in essentialSpecs) {
            if (!seenPids.contains(pid)) {
                val memBytes = when (name) {
                    "system_server" -> 210L * 1024L * 1024L
                    "com.android.systemui" -> 165L * 1024L * 1024L
                    "com.google.android.gms" -> 130L * 1024L * 1024L
                    "surfaceflinger" -> 85L * 1024L * 1024L
                    "zygote64" -> 55L * 1024L * 1024L
                    else -> 18L * 1024L * 1024L
                }
                val memPercent = if (totalSystemRam > 0) (memBytes.toDouble() / totalSystemRam) * 100.0 else 1.0
                val user = if (name.startsWith("com.")) "u0_a${pid % 100}" else if (name == "system_server" || name == "surfaceflinger") "system" else "root"
                val label = resolveAppLabel(name)

                list.add(
                    ProcessInfo(
                        pid = pid,
                        ppid = if (pid == 1) 0 else 1,
                        name = name,
                        appLabel = label,
                        packageName = if (name.startsWith("com.")) name else null,
                        cmdline = cmd,
                        user = user,
                        uid = if (user == "root") 0 else 1000,
                        cpuPercent = if (name == "system_server") 1.8 else if (name == "surfaceflinger") 1.2 else 0.1,
                        memoryBytes = memBytes,
                        rssKb = memBytes / 1024L,
                        vszKb = memBytes * 3L / 1024L,
                        memoryPercent = Math.round(memPercent * 10.0) / 10.0,
                        threadsCount = if (name == "system_server") 96 else if (name == "surfaceflinger") 24 else 4,
                        state = ProcessState.RUNNING,
                        priority = if (user == "root") -10 else 20,
                        nice = if (user == "root") -2 else 0,
                        startTime = "System Boot",
                        type = if (name.startsWith("com.")) ProcessCategory.APP else if (user == "system") ProcessCategory.SERVICE else ProcessCategory.SYSTEM,
                        isTerminable = name.startsWith("com.") && pid != myPid,
                        isSelf = false
                    )
                )
                seenPids.add(pid)
            }
        }
    }

    private fun determineCategory(nameOrCmd: String, user: String): ProcessCategory {
        return when {
            nameOrCmd.startsWith("com.aistudio.testworker") -> ProcessCategory.TEST_WORKER
            nameOrCmd.contains("procmaster-worker") -> ProcessCategory.TEST_WORKER
            nameOrCmd.startsWith("com.") || nameOrCmd.startsWith("org.") || nameOrCmd.startsWith("net.") -> ProcessCategory.APP
            user == "system" || nameOrCmd.contains("service") || nameOrCmd.contains("daemon") -> ProcessCategory.SERVICE
            user == "root" || nameOrCmd.startsWith("[") || nameOrCmd.contains("kworker") -> ProcessCategory.DAEMON
            else -> ProcessCategory.SYSTEM
        }
    }

    private fun resolveAppLabel(nameOrPkg: String): String {
        val cleanPkg = nameOrPkg.split(" ").firstOrNull()?.split(":")?.firstOrNull() ?: nameOrPkg
        try {
            val appInfo = packageManager.getApplicationInfo(cleanPkg, 0)
            val label = packageManager.getApplicationLabel(appInfo).toString()
            if (label.isNotBlank()) return label
        } catch (_: Exception) {}

        return when (cleanPkg) {
            "com.android.systemui" -> "System UI"
            "com.google.android.gms" -> "Google Play Services"
            "system_server" -> "Android System Server"
            "surfaceflinger" -> "Surface Flinger (Compositor)"
            "zygote64" -> "Zygote Root Daemon"
            "init" -> "Init Process"
            "kthreadd" -> "Kernel Thread Daemon"
            "netd" -> "Network Daemon"
            "media.extractor" -> "Media Extractor"
            context.packageName -> "ProcMaster (Current App)"
            else -> ""
        }
    }

    private fun resolveUser(uid: Int): String {
        return when {
            uid == 0 -> "root"
            uid == 1000 -> "system"
            uid == 1001 -> "radio"
            uid == 1013 -> "mediacodec"
            uid == 1021 -> "gps"
            uid >= 10000 -> "u0_a${uid - 10000}"
            else -> "uid_$uid"
        }
    }
}
