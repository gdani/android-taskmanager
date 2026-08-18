package com.example.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.SleekBorder
import com.example.ui.theme.SleekBorderLight
import com.example.ui.theme.SleekOnBackground
import com.example.ui.theme.SleekOnPrimary
import com.example.ui.theme.SleekOnPrimaryContainer
import com.example.ui.theme.SleekPrimary
import com.example.ui.theme.SleekPrimaryBorder
import com.example.ui.theme.SleekPrimaryContainer
import com.example.ui.theme.SleekSurface
import com.example.ui.theme.SleekSurfaceVariant
import com.example.ui.theme.SleekTextMuted
import com.example.ui.theme.SleekTextSubtle

@Composable
fun SpawnTestTaskDialog(
    onDismiss: () -> Unit,
    onSpawn: (name: String, type: String, targetCpu: Double, memoryMb: Int) -> Unit
) {
    var taskName by remember { mutableStateOf("calc-worker-${(10..99).random()}") }
    var taskType by remember { mutableStateOf("Matrix Cruncher") }
    var cpuTarget by remember { mutableFloatStateOf(12.0f) }
    var memoryTargetMb by remember { mutableFloatStateOf(64.0f) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("spawn_task_dialog"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SleekSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, SleekBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(SleekPrimaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = SleekOnPrimaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Spawn Test",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 18.sp
                            ),
                            color = SleekOnBackground
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(SleekSurfaceVariant)
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = SleekOnBackground, modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Launch a background task to observe live CPU/RAM stats and test single-click termination in real time.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SleekTextMuted
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Task Name
                OutlinedTextField(
                    value = taskName,
                    onValueChange = { taskName = it },
                    label = { Text("Task Identifier / Name") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("task_name_input"),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SleekSurfaceVariant,
                        unfocusedContainerColor = SleekSurfaceVariant,
                        focusedBorderColor = SleekPrimary,
                        unfocusedBorderColor = SleekBorder,
                        focusedTextColor = SleekOnBackground,
                        unfocusedTextColor = SleekOnBackground
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                // CPU Target Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Target CPU Intensity", style = MaterialTheme.typography.labelMedium, color = SleekTextMuted)
                    Text(
                        text = "${cpuTarget.toInt()}%",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        ),
                        color = SleekPrimary
                    )
                }

                Slider(
                    value = cpuTarget,
                    onValueChange = { cpuTarget = it },
                    valueRange = 2f..75f,
                    steps = 14,
                    colors = SliderDefaults.colors(
                        thumbColor = SleekPrimary,
                        activeTrackColor = SleekPrimary,
                        inactiveTrackColor = SleekPrimaryBorder
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Memory Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Memory Buffer", style = MaterialTheme.typography.labelMedium, color = SleekTextMuted)
                    Text(
                        text = "${memoryTargetMb.toInt()} MB",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        ),
                        color = SleekPrimary
                    )
                }

                Slider(
                    value = memoryTargetMb,
                    onValueChange = { memoryTargetMb = it },
                    valueRange = 10f..200f,
                    steps = 18,
                    colors = SliderDefaults.colors(
                        thumbColor = SleekPrimary,
                        activeTrackColor = SleekPrimary,
                        inactiveTrackColor = SleekPrimaryBorder
                    )
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SleekBorder)
                    ) {
                        Text("Cancel", color = SleekTextMuted)
                    }

                    ElevatedButton(
                        onClick = {
                            if (taskName.isNotBlank()) {
                                onSpawn(taskName, taskType, cpuTarget.toDouble(), memoryTargetMb.toInt())
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("confirm_spawn_task_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.elevatedButtonColors(
                            containerColor = SleekPrimary,
                            contentColor = SleekOnPrimary
                        )
                    ) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Launch Task", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                    }
                }
            }
        }
    }
}
