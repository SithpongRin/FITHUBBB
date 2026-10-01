package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calculations.FormatUtils
import com.example.localization.AppLanguage
import com.example.localization.StringKey
import com.example.ui.theme.CharcoalBackground
import com.example.ui.theme.LimeAccent
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TextMutedDark
import com.example.ui.theme.TextSecondaryDark
import com.example.ui.theme.WarningAmber
import com.example.ui.viewmodel.FithubViewModel
import com.example.updates.UpdateManager
import com.example.updates.UpdateStatus

@Composable
fun SettingsScreen(
    viewModel: FithubViewModel,
    modifier: Modifier = Modifier
) {
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val appLanguage by viewModel.appLanguage.collectAsState()
    val unitSystem by viewModel.unitSystem.collectAsState()
    val updateStatus by viewModel.updateStatus.collectAsState()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 24.dp, bottom = 100.dp)
    ) {
        item {
            Text(
                text = viewModel.str(StringKey.SETTINGS),
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Preferences, localization and software updates",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }

        // Appearance Section
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = viewModel.str(StringKey.APPEARANCE),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isDarkMode) viewModel.str(StringKey.THEME_DARK) else viewModel.str(StringKey.THEME_LIGHT),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Switch(
                            checked = isDarkMode,
                            onCheckedChange = { viewModel.setDarkMode(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = CharcoalBackground,
                                checkedTrackColor = LimeAccent
                            )
                        )
                    }
                }
            }
        }

        // Daily Step Tracker Section
        item {
            val isStepTrackingEnabled by viewModel.isStepTrackingEnabled.collectAsState()
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = viewModel.str(StringKey.DAILY_STEPS),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Count daily steps automatically in the background using hardware sensor",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isStepTrackingEnabled) viewModel.str(StringKey.STEP_TRACKING_ON) else viewModel.str(StringKey.STEP_TRACKING_OFF),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                        )
                        Switch(
                            checked = isStepTrackingEnabled,
                            onCheckedChange = { viewModel.setStepTrackingEnabled(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = CharcoalBackground,
                                checkedTrackColor = LimeAccent
                            )
                        )
                    }
                }
            }
        }

        // Language Section (Runtime switching English & Khmer)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = viewModel.str(StringKey.LANGUAGE),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FilterChip(
                            selected = appLanguage == AppLanguage.ENGLISH,
                            onClick = { viewModel.setLanguage(AppLanguage.ENGLISH) },
                            label = {
                                Text(
                                    text = "English",
                                    fontWeight = FontWeight.Bold,
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

                        FilterChip(
                            selected = appLanguage == AppLanguage.KHMER,
                            onClick = { viewModel.setLanguage(AppLanguage.KHMER) },
                            label = {
                                Text(
                                    text = "ភាសាខ្មែរ",
                                    fontWeight = FontWeight.Bold,
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
        }

        // Units Section (Metric & Imperial)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = viewModel.str(StringKey.UNITS),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = unitSystem == FormatUtils.UnitSystem.METRIC,
                            onClick = { viewModel.unitSystem.value = FormatUtils.UnitSystem.METRIC },
                            label = {
                                Text(
                                    text = viewModel.str(StringKey.UNIT_METRIC),
                                    maxLines = 1,
                                    softWrap = false
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = LimeAccent,
                                selectedLabelColor = CharcoalBackground
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        FilterChip(
                            selected = unitSystem == FormatUtils.UnitSystem.IMPERIAL,
                            onClick = { viewModel.unitSystem.value = FormatUtils.UnitSystem.IMPERIAL },
                            label = {
                                Text(
                                    text = viewModel.str(StringKey.UNIT_IMPERIAL),
                                    maxLines = 1,
                                    softWrap = false
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = LimeAccent,
                                selectedLabelColor = CharcoalBackground
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        // About & Updates Section
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = viewModel.str(StringKey.ABOUT_UPDATES),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "${viewModel.str(StringKey.CURRENT_VERSION)}: ${UpdateManager.CURRENT_VERSION_NAME} (Build ${UpdateManager.CURRENT_VERSION_CODE})",
                        style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Update Status message & Option 2 actions
                    when (val status = updateStatus) {
                        is UpdateStatus.Checking -> {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = LimeAccent)
                                Text(
                                    text = viewModel.str(StringKey.UPDATE_STATUS_CHECKING),
                                    style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                )
                            }
                        }
                        is UpdateStatus.UpToDate -> {
                            Text(
                                text = viewModel.str(StringKey.UPDATE_STATUS_UP_TO_DATE),
                                style = MaterialTheme.typography.bodyMedium.copy(color = SuccessGreen, fontWeight = FontWeight.Medium)
                            )
                        }
                        is UpdateStatus.Available -> {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "${viewModel.str(StringKey.UPDATE_STATUS_AVAILABLE)}: v${status.version}",
                                    style = MaterialTheme.typography.bodyMedium.copy(color = LimeAccent, fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = if (appLanguage.code == "km") status.notesKm else status.notesEn,
                                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                )
                                Button(
                                    onClick = { viewModel.startDownloadUpdate(status) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = LimeAccent,
                                        contentColor = CharcoalBackground
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Download, contentDescription = "Download")
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (appLanguage.code == "km") "ទាញយក និងដំឡើង APK" else "Download & Install APK",
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        is UpdateStatus.Downloading -> {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "Downloading v${status.version} (${status.progressPercent}%)",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                )
                                LinearProgressIndicator(
                                    progress = { (status.progressPercent / 100f).coerceIn(0f, 1f) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(CircleShape),
                                    color = LimeAccent,
                                    trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                )
                            }
                        }
                        is UpdateStatus.ReadyToInstall -> {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "APK Download Complete (Ready to Install)",
                                    style = MaterialTheme.typography.bodyMedium.copy(color = SuccessGreen, fontWeight = FontWeight.Bold)
                                )
                                Button(
                                    onClick = { viewModel.installDownloadedApk(status.apkFile) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = LimeAccent,
                                        contentColor = CharcoalBackground
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.InstallMobile, contentDescription = "Install")
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (appLanguage.code == "km") "ដំឡើងកំណែថ្មីឥឡូវនេះ" else "Install Update Now",
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        is UpdateStatus.PermissionRequired -> {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "Permission Required to install APK",
                                    style = MaterialTheme.typography.bodyMedium.copy(color = WarningAmber, fontWeight = FontWeight.Bold)
                                )
                                Button(
                                    onClick = { viewModel.requestInstallPermission() },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = WarningAmber,
                                        contentColor = CharcoalBackground
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Allow in Settings", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        else -> {
                            Text(
                                text = "Direct APK auto-update enabled (Option 2)",
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { viewModel.checkForUpdates(false) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = viewModel.str(StringKey.CHECK_FOR_UPDATES),
                                maxLines = 1,
                                softWrap = false
                            )
                        }

                        // Simulation button to demonstrate Option 2 update flow
                        OutlinedButton(
                            onClick = { viewModel.checkForUpdates(true) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (appLanguage.code == "km") "សាកល្បងអាប់ដែត APK (Option 2 Demo)" else "Test APK Update (Option 2 Demo)",
                                color = LimeAccent,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }
        }
    }
}
