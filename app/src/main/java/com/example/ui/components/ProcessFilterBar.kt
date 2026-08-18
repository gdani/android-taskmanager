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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ProcessCategoryFilter
import com.example.ui.theme.SleekBorder
import com.example.ui.theme.SleekBorderLight
import com.example.ui.theme.SleekError
import com.example.ui.theme.SleekErrorContainer
import com.example.ui.theme.SleekOnBackground
import com.example.ui.theme.SleekOnPrimary
import com.example.ui.theme.SleekOnPrimaryContainer
import com.example.ui.theme.SleekPrimary
import com.example.ui.theme.SleekPrimaryBorder
import com.example.ui.theme.SleekPrimaryContainer
import com.example.ui.theme.SleekSecondaryContainer
import com.example.ui.theme.SleekSurface
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
    val focusManager = LocalFocusManager.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 2.dp)
    ) {
        // Section Header: Process Count & Current Category Indicator
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Running Processes ($filteredCount${if (filteredCount != totalCount) "/$totalCount" else ""})",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                ),
                color = SleekTextMuted
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(SleekSurfaceVariant)
                        .border(1.dp, SleekBorderLight, RoundedCornerShape(12.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = selectedCategory.label,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = SleekPrimary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(3.dp))

        // Multi-select Batch Action Bar (if active)
        AnimatedVisibility(visible = isMultiSelectMode) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(SleekErrorContainer)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "$selectedPidsCount selected",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp),
                        color = SleekError
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    OutlinedButton(
                        onClick = onSelectAllFiltered,
                        modifier = Modifier
                            .height(28.dp)
                            .testTag("select_all_button"),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SelectAll,
                            contentDescription = "Select All",
                            modifier = Modifier.size(12.dp),
                            tint = SleekError
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("All", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = SleekError)
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    ElevatedButton(
                        onClick = onKillSelected,
                        enabled = selectedPidsCount > 0,
                        modifier = Modifier
                            .height(30.dp)
                            .testTag("kill_selected_batch_button"),
                        colors = ButtonDefaults.elevatedButtonColors(
                            containerColor = SleekError,
                            contentColor = SleekOnPrimary
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = "Kill Selected Tasks",
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Kill ($selectedPidsCount)",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    IconButton(
                        onClick = onClearSelection,
                        modifier = Modifier
                            .size(28.dp)
                            .testTag("cancel_selection_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cancel Selection",
                            tint = SleekError,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // COMPACT Search Bar Row: Expanded wider width with smaller height and compact action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Sleek, Compact Search Box
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(SleekSurface)
                    .border(1.dp, SleekBorder, RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = SleekTextMuted,
                        modifier = Modifier.size(16.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = "Filter name, PID, user...",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                color = SleekTextSubtle
                            )
                        }
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = onSearchChange,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("process_search_input"),
                            singleLine = true,
                            textStyle = TextStyle(
                                color = SleekOnBackground,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Normal
                            ),
                            cursorBrush = SolidColor(SleekPrimary),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() })
                        )
                    }

                    if (searchQuery.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .clickable { onSearchChange("") },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear Search",
                                tint = SleekTextMuted,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            // Compact View Mode Toggle Button (Compact Row vs Card)
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isCompactView) SleekPrimaryContainer else SleekSurface)
                    .border(
                        1.dp,
                        if (isCompactView) SleekPrimaryBorder else SleekBorder,
                        RoundedCornerShape(10.dp)
                    )
                    .clickable { onToggleTableViewMode() }
                    .testTag("toggle_table_view_mode"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isCompactView) Icons.Default.TableRows else Icons.Default.ViewAgenda,
                    contentDescription = if (isCompactView) "Switch to Card View" else "Switch to Compact View",
                    tint = if (isCompactView) SleekPrimary else SleekTextMuted,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Compact Multi-select Toggle Button
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isMultiSelectMode) SleekErrorContainer else SleekSurface)
                    .border(
                        1.dp,
                        if (isMultiSelectMode) SleekError else SleekBorder,
                        RoundedCornerShape(10.dp)
                    )
                    .clickable { onToggleMultiSelect() }
                    .testTag("toggle_multi_select_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = "Multi-select Mode",
                    tint = if (isMultiSelectMode) SleekError else SleekTextMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(5.dp))

        // System & Background Process Toggles Row (Slim Pill Design)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // System Processes Toggle Pill
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (showSystemProcesses) SleekPrimaryContainer else SleekSurfaceVariant)
                    .border(
                        1.dp,
                        if (showSystemProcesses) SleekPrimaryBorder else SleekBorder,
                        RoundedCornerShape(8.dp)
                    )
                    .clickable { onToggleShowSystemProcesses() }
                    .padding(horizontal = 8.dp),
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
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = if (showSystemProcesses) "System: ON" else "System: OFF",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = if (showSystemProcesses) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 10.sp
                        ),
                        color = if (showSystemProcesses) SleekOnPrimaryContainer else SleekTextMuted
                    )
                }
            }

            // Background Processes Toggle Pill
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (showBackgroundProcesses) SleekSecondaryContainer else SleekSurfaceVariant)
                    .border(
                        1.dp,
                        if (showBackgroundProcesses) SleekPrimaryBorder else SleekBorder,
                        RoundedCornerShape(8.dp)
                    )
                    .clickable { onToggleShowBackgroundProcesses() }
                    .padding(horizontal = 8.dp),
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
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = if (showBackgroundProcesses) "Background: ON" else "Background: OFF",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = if (showBackgroundProcesses) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 10.sp
                        ),
                        color = if (showBackgroundProcesses) SleekOnBackground else SleekTextMuted
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(5.dp))

        // Horizontal Filter Chips & Speed Selectors
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
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
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 10.sp
                            )
                        )
                    },
                    leadingIcon = if (selected) {
                        {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(10.dp),
                                tint = SleekPrimary
                            )
                        }
                    } else null,
                    shape = RoundedCornerShape(10.dp),
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
                    modifier = Modifier
                        .height(26.dp)
                        .testTag("filter_chip_${category.name.lowercase()}")
                )
            }

            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(16.dp)
                    .background(SleekBorder)
            )

            // Speed Interval Selector
            val intervals = listOf(1000L to "1s", 2000L to "2s", 5000L to "5s", 0L to "Pause")
            intervals.forEach { (ms, label) ->
                val isSelected = refreshIntervalMs == ms
                FilterChip(
                    selected = isSelected,
                    onClick = { onSelectRefreshInterval(ms) },
                    label = {
                        Text(
                            text = "⚡ $label",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp)
                        )
                    },
                    shape = RoundedCornerShape(10.dp),
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
                    modifier = Modifier
                        .height(26.dp)
                        .testTag("speed_chip_$label")
                )
            }
        }
    }
}
