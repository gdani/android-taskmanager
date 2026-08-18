package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TableRows
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ProcessCategoryFilter
import com.example.ui.theme.SleekBorder
import com.example.ui.theme.SleekBorderLight
import com.example.ui.theme.SleekError
import com.example.ui.theme.SleekErrorContainer
import com.example.ui.theme.SleekOnBackground
import com.example.ui.theme.SleekOnErrorContainer
import com.example.ui.theme.SleekOnPrimary
import com.example.ui.theme.SleekOnPrimaryContainer
import com.example.ui.theme.SleekPrimary
import com.example.ui.theme.SleekPrimaryBorder
import com.example.ui.theme.SleekPrimaryContainer
import com.example.ui.theme.SleekSecondaryContainer
import com.example.ui.theme.SleekSuccess
import com.example.ui.theme.SleekSuccessContainer
import com.example.ui.theme.SleekSurface
import com.example.ui.theme.SleekSurfaceSelected
import com.example.ui.theme.SleekSurfaceVariant
import com.example.ui.theme.SleekTextMuted
import com.example.ui.theme.SleekTextSubtle

@Composable
fun ProcessFilterBar(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    selectedCategory: ProcessCategoryFilter,
    onSelectCategory: (ProcessCategoryFilter) -> Unit,
    showSystemProcesses: Boolean,
    onToggleShowSystemProcesses: () -> Unit,
    showBackgroundProcesses: Boolean,
    onToggleShowBackgroundProcesses: () -> Unit,
    totalCount: Int,
    filteredCount: Int,
    isMultiSelectMode: Boolean,
    selectedPidsCount: Int,
    onToggleMultiSelect: () -> Unit,
    onSelectAllFiltered: () -> Unit,
    onClearSelection: () -> Unit,
    onKillSelected: () -> Unit,
    isCompactView: Boolean,
    onToggleTableViewMode: () -> Unit,
    refreshIntervalMs: Long,
    onSelectRefreshInterval: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        // Section Header: Running Processes (N) + Filter info
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Running Processes ($filteredCount${if (filteredCount != totalCount) " of $totalCount" else ""})",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp
                ),
                color = SleekTextMuted
            )

            // Multi-Select trigger or count badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(SleekSurfaceVariant)
                        .border(1.dp, SleekBorder, RoundedCornerShape(20.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Filter: ${selectedCategory.label}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = SleekTextMuted
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Multi-select Batch Action Bar (if active)
        AnimatedVisibility(visible = isMultiSelectMode) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(SleekErrorContainer)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "$selectedPidsCount selected",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = SleekError
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedButton(
                        onClick = onSelectAllFiltered,
                        modifier = Modifier
                            .height(32.dp)
                            .testTag("select_all_button"),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SelectAll,
                            contentDescription = "Select All",
                            modifier = Modifier.size(14.dp),
                            tint = SleekError
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("All", style = MaterialTheme.typography.labelSmall, color = SleekError)
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    ElevatedButton(
                        onClick = onKillSelected,
                        enabled = selectedPidsCount > 0,
                        modifier = Modifier
                            .height(36.dp)
                            .testTag("kill_selected_batch_button"),
                        colors = ButtonDefaults.elevatedButtonColors(
                            containerColor = SleekError,
                            contentColor = SleekOnPrimary
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = "Kill Selected Tasks",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Kill ($selectedPidsCount)",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    IconButton(
                        onClick = onClearSelection,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("cancel_selection_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cancel Selection",
                            tint = SleekError
                        )
                    }
                }
            }
        }

        // Search Bar with Table Layout / Multi-select Toggles
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                modifier = Modifier
                    .weight(1f)
                    .testTag("process_search_input"),
                placeholder = {
                    Text(
                        text = "Filter by name, PID, user, cmd...",
                        style = MaterialTheme.typography.bodySmall,
                        color = SleekTextSubtle
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = SleekTextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchChange("") }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear Search",
                                tint = SleekTextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = SleekSurface,
                    unfocusedContainerColor = SleekSurface,
                    focusedBorderColor = SleekPrimary,
                    unfocusedBorderColor = SleekBorder,
                    focusedTextColor = SleekOnBackground,
                    unfocusedTextColor = SleekOnBackground
                )
            )

            // Table View Toggle (Card vs Compact rows)
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(SleekSurface)
                    .border(1.dp, SleekBorder, RoundedCornerShape(16.dp))
                    .clickable { onToggleTableViewMode() }
                    .testTag("toggle_table_view_mode"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isCompactView) Icons.Default.ViewAgenda else Icons.Default.TableRows,
                    contentDescription = if (isCompactView) "Switch to Card View" else "Switch to Compact Table View",
                    tint = if (isCompactView) SleekPrimary else SleekTextMuted,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Multi-select toggle button
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isMultiSelectMode) SleekErrorContainer else SleekSurface)
                    .border(1.dp, if (isMultiSelectMode) SleekError else SleekBorder, RoundedCornerShape(16.dp))
                    .clickable { onToggleMultiSelect() }
                    .testTag("toggle_multi_select_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = "Multi-select Mode",
                    tint = if (isMultiSelectMode) SleekError else SleekTextMuted,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // System & Background Process Toggles Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // System Processes Toggle Pill
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (showSystemProcesses) SleekPrimaryContainer else SleekSurfaceVariant)
                    .border(
                        1.dp,
                        if (showSystemProcesses) SleekPrimaryBorder else SleekBorder,
                        RoundedCornerShape(12.dp)
                    )
                    .clickable { onToggleShowSystemProcesses() }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                    .testTag("toggle_system_processes_switch"),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "System Processes Toggle",
                        tint = if (showSystemProcesses) SleekPrimary else SleekTextMuted,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (showSystemProcesses) "System: ON" else "System: OFF",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = if (showSystemProcesses) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 11.sp
                        ),
                        color = if (showSystemProcesses) SleekOnPrimaryContainer else SleekTextMuted
                    )
                }
            }

            // Background Processes Toggle Pill
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (showBackgroundProcesses) SleekSecondaryContainer else SleekSurfaceVariant)
                    .border(
                        1.dp,
                        if (showBackgroundProcesses) SleekPrimaryBorder else SleekBorder,
                        RoundedCornerShape(12.dp)
                    )
                    .clickable { onToggleShowBackgroundProcesses() }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                    .testTag("toggle_background_processes_switch"),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Layers,
                        contentDescription = "Background Processes Toggle",
                        tint = if (showBackgroundProcesses) SleekPrimary else SleekTextMuted,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (showBackgroundProcesses) "Background: ON" else "Background: OFF",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = if (showBackgroundProcesses) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 11.sp
                        ),
                        color = if (showBackgroundProcesses) SleekOnBackground else SleekTextMuted
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Horizontal Filter Chips & Refresh Speed Pills
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category Chips
            ProcessCategoryFilter.values().forEach { category ->
                val selected = selectedCategory == category
                FilterChip(
                    selected = selected,
                    onClick = { onSelectCategory(category) },
                    label = {
                        Text(
                            text = category.label,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                            )
                        )
                    },
                    leadingIcon = if (selected) {
                        {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = SleekPrimary
                            )
                        }
                    } else null,
                    shape = RoundedCornerShape(12.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = SleekPrimaryContainer,
                        selectedLabelColor = SleekOnPrimaryContainer,
                        containerColor = SleekSurfaceVariant,
                        labelColor = SleekTextMuted
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = selected,
                        borderColor = if (selected) SleekPrimaryBorder else SleekBorder
                    ),
                    modifier = Modifier.testTag("filter_chip_${category.name.lowercase()}")
                )
            }

            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(20.dp)
                    .background(SleekBorder)
            )

            // Speed Interval Selector (1s, 2s, 5s, Pause)
            val intervals = listOf(1000L to "1s", 2000L to "2s", 5000L to "5s", 0L to "Pause")
            intervals.forEach { (ms, label) ->
                val isSelected = refreshIntervalMs == ms
                FilterChip(
                    selected = isSelected,
                    onClick = { onSelectRefreshInterval(ms) },
                    label = {
                        Text(
                            text = "⚡ $label",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp)
                        )
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = SleekSecondaryContainer,
                        selectedLabelColor = SleekOnBackground,
                        containerColor = SleekSurfaceVariant,
                        labelColor = SleekTextMuted
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = if (isSelected) SleekPrimaryBorder else SleekBorder
                    ),
                    modifier = Modifier.testTag("speed_chip_$label")
                )
            }
        }
    }
}
