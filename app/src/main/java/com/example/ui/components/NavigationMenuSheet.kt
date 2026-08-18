package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AltRoute
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NetworkPing
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.SleekBorder
import com.example.ui.theme.SleekBorderLight
import com.example.ui.theme.SleekOnBackground
import com.example.ui.theme.SleekPrimary
import com.example.ui.theme.SleekPrimaryBorder
import com.example.ui.theme.SleekPrimaryContainer
import com.example.ui.theme.SleekSurface
import com.example.ui.theme.SleekSurfaceVariant
import com.example.ui.theme.SleekTextMuted

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavigationMenuSheet(
    currentTabIndex: Int,
    onSelectTab: (Int) -> Unit,
    onShowSpeedTest: () -> Unit,
    onShowPing: () -> Unit,
    onShowTraceroute: () -> Unit,
    onShowKillHistory: () -> Unit,
    onShowSystemInfo: () -> Unit,
    onShowExport: () -> Unit,
    onSpawnTestTask: () -> Unit,
    onTerminateAllBackgroundApps: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SleekSurface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(SleekBorder)
            )
        }
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
                .testTag("navigation_menu_sheet")
        ) {
            // Sheet Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "System Diagnostics Menu",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        ),
                        color = SleekOnBackground
                    )
                    Text(
                        text = "System navigation, network tools & process management",
                        style = MaterialTheme.typography.bodySmall,
                        color = SleekTextMuted
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Menu",
                        tint = SleekTextMuted
                    )
                }
            }

            Text(
                text = "PRIMARY VIEWS",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 1.sp
                ),
                color = SleekTextMuted,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            // Tab 0: Trends (Primary First Tab - Req #2)
            MenuNavigationItem(
                icon = Icons.Default.ShowChart,
                title = "Resource & Network Trends",
                subtitle = "Live CPU, RAM and network traffic trajectory telemetry",
                isSelected = currentTabIndex == 0,
                testTag = "menu_nav_trends",
                onClick = {
                    onSelectTab(0)
                    onDismiss()
                }
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Tab 1: Processes (Second Tab - Req #2)
            MenuNavigationItem(
                icon = Icons.Default.Dashboard,
                title = "Running Processes",
                subtitle = "Tasks, services, background daemons, storage analysis & signals",
                isSelected = currentTabIndex == 1,
                testTag = "menu_nav_processes",
                onClick = {
                    onSelectTab(1)
                    onDismiss()
                }
            )

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = SleekBorder)
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "NETWORK DIAGNOSTIC UTILITIES",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 1.sp
                ),
                color = SleekPrimary,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            // Speed Test Subprocess (Req #7)
            MenuNavigationItem(
                icon = Icons.Default.Speed,
                title = "Network Speed Test Subprocess",
                subtitle = "Measure download/upload bandwidth, ping & jitter at edge locations",
                isSelected = false,
                testTag = "menu_nav_speed_test",
                onClick = {
                    onDismiss()
                    onShowSpeedTest()
                }
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Ping Tool (Req #8)
            MenuNavigationItem(
                icon = Icons.Default.NetworkPing,
                title = "Ping Utility",
                subtitle = "ICMP echo latency, packet loss test & live response console",
                isSelected = false,
                testTag = "menu_nav_ping",
                onClick = {
                    onDismiss()
                    onShowPing()
                }
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Traceroute Tool (Req #9)
            MenuNavigationItem(
                icon = Icons.Default.AltRoute,
                title = "Traceroute Utility",
                subtitle = "Probe multi-hop transit network path and round-trip delays",
                isSelected = false,
                testTag = "menu_nav_traceroute",
                onClick = {
                    onDismiss()
                    onShowTraceroute()
                }
            )

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = SleekBorder)
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "SYSTEM TOOLS & LOGS",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 1.sp
                ),
                color = SleekTextMuted,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            // Kill History
            MenuNavigationItem(
                icon = Icons.Default.History,
                title = "Kill History",
                subtitle = "Audit log of terminated processes and recovered RAM",
                isSelected = false,
                testTag = "menu_nav_history",
                onClick = {
                    onDismiss()
                    onShowKillHistory()
                }
            )

            Spacer(modifier = Modifier.height(6.dp))

            // System Specs
            MenuNavigationItem(
                icon = Icons.Default.Info,
                title = "System Specifications",
                subtitle = "Hardware architecture, CPU cores, OS, and memory specs",
                isSelected = false,
                testTag = "menu_nav_specs",
                onClick = {
                    onDismiss()
                    onShowSystemInfo()
                }
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Spawn Test Task
            MenuNavigationItem(
                icon = Icons.Default.Add,
                title = "Spawn Test Task",
                subtitle = "Simulate custom background CPU & RAM worker thread",
                isSelected = false,
                testTag = "menu_action_spawn_task",
                onClick = {
                    onDismiss()
                    onSpawnTestTask()
                }
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Export Data
            MenuNavigationItem(
                icon = Icons.Default.Download,
                title = "Export Snapshot",
                subtitle = "Export current processes state as JSON or CSV",
                isSelected = false,
                testTag = "menu_action_export",
                onClick = {
                    onDismiss()
                    onShowExport()
                }
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Terminate All Background Apps (Req #5)
            MenuNavigationItem(
                icon = Icons.Default.Close,
                title = "Terminate Suspended / Background Apps",
                subtitle = "Instantly clean all cached, suspended and background processes",
                isSelected = false,
                testTag = "menu_action_terminate_bg",
                onClick = {
                    onDismiss()
                    onTerminateAllBackgroundApps()
                }
            )
        }
    }
}

@Composable
fun MenuNavigationItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isSelected: Boolean,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = if (isSelected) SleekPrimaryContainer else SleekSurfaceVariant
    val borderColor = if (isSelected) SleekPrimaryBorder else SleekBorderLight
    val iconTint = if (isSelected) SleekPrimary else SleekOnBackground

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .border(1.dp, borderColor, RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(if (isSelected) SleekPrimary.copy(alpha = 0.15f) else SleekSurface),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                    fontSize = 14.sp
                ),
                color = SleekOnBackground
            )
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = SleekTextMuted,
                maxLines = 1
            )
        }

        if (isSelected) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(SleekPrimary)
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "ACTIVE",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
            }
        }
    }
}
