package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.localization.StringKey
import com.example.ui.theme.CharcoalBackground
import com.example.ui.theme.LimeAccent
import com.example.ui.viewmodel.FithubViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SleepLogDialog(
    viewModel: FithubViewModel,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // State stored in minutes of day (0..1439) and duration in minutes
    var bedtimeMinutes by remember { mutableIntStateOf(23 * 60) } // 23:00 (11:00 PM)
    var durationMinutes by remember { mutableIntStateOf(480) }     // 8 hours (480 mins)
    var qualityRating by remember { mutableIntStateOf(3) }          // 1..4 (Poor, Fair, Good, Excellent)

    fun formatMinutesOfDay(mins: Int): String {
        val total = (mins % 1440 + 1440) % 1440
        val h = total / 60
        val m = total % 60
        return String.format("%02d:%02d", h, m)
    }

    val bedtimeStr = formatMinutesOfDay(bedtimeMinutes)
    val wakeTimeStr = formatMinutesOfDay(bedtimeMinutes + durationMinutes)

    val hours = durationMinutes / 60
    val minutes = durationMinutes % 60

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                color = LimeAccent.copy(alpha = 0.6f),
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = viewModel.str(StringKey.LOG_SLEEP),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Scrollable Body
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Duration Hero Display & Steppers (Tap to adjust)
                Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Sleep Duration",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "${hours}h ${if (minutes > 0) "${minutes}m" else "00m"}",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Black,
                            color = LimeAccent,
                            fontSize = 36.sp
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Stepper buttons (-30m, -15m, +15m, +30m)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilledTonalButton(
                            onClick = { durationMinutes = (durationMinutes - 30).coerceAtLeast(60) },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("-30m", fontWeight = FontWeight.Bold)
                        }

                        FilledTonalButton(
                            onClick = { durationMinutes = (durationMinutes - 15).coerceAtLeast(60) },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("-15m", fontWeight = FontWeight.Bold)
                        }

                        FilledTonalButton(
                            onClick = { durationMinutes = (durationMinutes + 15).coerceAtMost(960) },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("+15m", fontWeight = FontWeight.Bold)
                        }

                        FilledTonalButton(
                            onClick = { durationMinutes = (durationMinutes + 30).coerceAtMost(960) },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("+30m", fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Quick duration presets (scrollable row of 1-tap chips)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            360 to "6h",
                            390 to "6.5h",
                            420 to "7h",
                            450 to "7.5h",
                            480 to "8h",
                            510 to "8.5h",
                            540 to "9h"
                        ).forEach { (m, label) ->
                            FilterChip(
                                selected = durationMinutes == m,
                                onClick = { durationMinutes = m },
                                label = { Text(label, fontWeight = FontWeight.SemiBold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = LimeAccent,
                                    selectedLabelColor = CharcoalBackground
                                )
                            )
                        }
                    }
                }
            }

            // 2. Bedtime & Wake Time (Calculated & Adjustable via 1-tap chips)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Bedtime: $bedtimeStr",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Wake up: $wakeTimeStr",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }

                    // Stepper for bedtime (-15m / +15m)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        IconButton(
                            onClick = { bedtimeMinutes = (bedtimeMinutes - 15 + 1440) % 1440 },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Bedtime minus 15m", modifier = Modifier.size(18.dp))
                        }

                        IconButton(
                            onClick = { bedtimeMinutes = (bedtimeMinutes + 15) % 1440 },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Bedtime plus 15m", modifier = Modifier.size(18.dp))
                        }
                    }
                }

                // Quick bedtime chips (1-tap selection)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        (21 * 60 + 30) to "21:30",
                        (22 * 60) to "22:00",
                        (22 * 60 + 30) to "22:30",
                        (23 * 60) to "23:00",
                        (23 * 60 + 30) to "23:30",
                        0 to "00:00"
                    ).forEach { (mins, label) ->
                        FilterChip(
                            selected = bedtimeMinutes == mins,
                            onClick = { bedtimeMinutes = mins },
                            label = { Text(label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = LimeAccent,
                                selectedLabelColor = CharcoalBackground
                            )
                        )
                    }
                }
            }

            // 3. Sleep Quality Section
            Text(
                text = viewModel.str(StringKey.SLEEP_QUALITY),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val qualityOptions = listOf(
                    1 to "Poor",
                    2 to "Fair",
                    3 to "Good",
                    4 to "Excellent"
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    qualityOptions.take(2).forEach { (rating, label) ->
                        FilterChip(
                            selected = qualityRating == rating,
                            onClick = { qualityRating = rating },
                            label = {
                                Text(
                                    text = label,
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = LimeAccent,
                                selectedLabelColor = CharcoalBackground
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    qualityOptions.drop(2).forEach { (rating, label) ->
                        FilterChip(
                            selected = qualityRating == rating,
                            onClick = { qualityRating = rating },
                            label = {
                                Text(
                                    text = label,
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = LimeAccent,
                                selectedLabelColor = CharcoalBackground
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

                    Spacer(modifier = Modifier.height(8.dp))
            }

            // Sticky Bottom Action Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .navigationBarsPadding(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel", maxLines = 1, softWrap = false)
                }

                Button(
                    onClick = {
                        viewModel.logSleep(durationMinutes, bedtimeStr, wakeTimeStr, qualityRating)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LimeAccent,
                        contentColor = CharcoalBackground
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Save", fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                }
            }
        }
    }
}
