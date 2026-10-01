package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.CharcoalBackground
import com.example.ui.theme.LimeAccent
import com.example.ui.viewmodel.FithubViewModel
import com.example.updates.DynamicFeatureStatus
import com.example.updates.UpdateStatus

@Composable
fun InAppUpdateDialogs(viewModel: FithubViewModel) {
    val isKm = viewModel.appLanguage.collectAsState().value.code == "km"
    val featureStatus by viewModel.dynamicFeatureStatus.collectAsState()
    val apkStatus by viewModel.updateStatus.collectAsState()

    // 1. Dynamic Feature Updates (OTA - Without APK Reinstall)
    when (val status = featureStatus) {
        is DynamicFeatureStatus.UpdateAvailable -> {
            val pack = status.pack
            AlertDialog(
                onDismissRequest = { viewModel.dismissFeatureUpdate() },
                icon = {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(LimeAccent.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = LimeAccent,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                },
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (isKm) "មានមុខងារថ្មីក្នុង App!" else "New In-App Features Available!",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                lineHeight = 30.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = LimeAccent.copy(alpha = 0.18f)
                        ) {
                            Text(
                                text = if (isKm) "អាប់ដែតក្នុង App ផ្ទាល់ (មិនបាច់ Install APK)" else "Instant In-App Update (No APK Reinstall)",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = LimeAccent,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = if (isKm) pack.titleKm else pack.titleEn,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Text(
                            text = if (isKm) pack.notesKm else pack.notesEn,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 22.sp
                            )
                        )
                        if (pack.plans.isNotEmpty()) {
                            Text(
                                text = if (isKm) "+ គម្រោងហាត់ថ្មី៖ ${pack.plans.joinToString { it.name }}" else "+ New Plans: ${pack.plans.joinToString { it.name }}",
                                style = MaterialTheme.typography.labelMedium.copy(color = LimeAccent)
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { viewModel.applyFeatureUpdate(pack) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LimeAccent,
                            contentColor = CharcoalBackground
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = if (isKm) "អាប់ដែតឥឡូវនេះ" else "Update Now",
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.dismissFeatureUpdate() }) {
                        Text(
                            text = if (isKm) "ពេលក្រោយ" else "Later",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                shape = RoundedCornerShape(24.dp)
            )
        }

        is DynamicFeatureStatus.Applying -> {
            AlertDialog(
                onDismissRequest = { /* Non-dismissible while applying */ },
                properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
                title = {
                    Text(
                        text = if (isKm) "កំពុងអាប់ដែតមុខងារ..." else "Applying In-App Update...",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        LinearProgressIndicator(
                            progress = { status.progressPercent / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = LimeAccent
                        )
                        Text(
                            text = "${status.stepDescription} (${status.progressPercent}%)",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }
                },
                confirmButton = {},
                shape = RoundedCornerShape(20.dp)
            )
        }

        is DynamicFeatureStatus.Applied -> {
            AlertDialog(
                onDismissRequest = { viewModel.dismissFeatureUpdate() },
                icon = {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = LimeAccent,
                        modifier = Modifier.size(36.dp)
                    )
                },
                title = {
                    Text(
                        text = if (isKm) "អាប់ដែតជោគជ័យ!" else "Update Complete!",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                },
                text = {
                    Text(
                        text = if (isKm)
                            "មុខងារថ្មីត្រូវបានបញ្ចូលទៅក្នុង App ដោយជោគជ័យ។ លោកអ្នកអាចប្រើប្រាស់មុខងារទាំងអស់នេះបានភ្លាមៗ ទោះបីគ្មានអ៊ីនធឺណិត (Offline)។"
                        else
                            "Features applied directly into local storage. Fully usable offline immediately without APK reinstall.",
                        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp)
                    )
                },
                confirmButton = {
                    Button(
                        onClick = { viewModel.dismissFeatureUpdate() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LimeAccent,
                            contentColor = CharcoalBackground
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = if (isKm) "យល់ព្រម" else "Got It",
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                shape = RoundedCornerShape(24.dp)
            )
        }

        else -> Unit
    }

    // 2. Core App APK Updates (When a new native build is released)
    when (val status = apkStatus) {
        is UpdateStatus.Available -> {
            AlertDialog(
                onDismissRequest = { viewModel.dismissUpdate() },
                icon = {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SystemUpdate,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                },
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (isKm) "មានកំណែកម្មវិធីថ្មី!" else "New App Version Available!",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                lineHeight = 30.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "v${status.version}",
                            style = MaterialTheme.typography.labelLarge.copy(
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = if (isKm) status.notesKm else status.notesEn,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 22.sp
                            )
                        )
                        val sizeMb = String.format("%.1f MB", status.downloadSizeBytes / (1024f * 1024f))
                        Text(
                            text = if (isKm) "ទំហំឯកសារ៖ $sizeMb" else "Size: $sizeMb",
                            style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { viewModel.startDownloadUpdate(status) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = if (isKm) "ទាញយក និងដំឡើង" else "Download & Install",
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.dismissUpdate() }) {
                        Text(
                            text = if (isKm) "ពេលក្រោយ" else "Later",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                shape = RoundedCornerShape(24.dp)
            )
        }

        is UpdateStatus.Downloading -> {
            AlertDialog(
                onDismissRequest = { /* Download in progress */ },
                title = {
                    Text(
                        text = if (isKm) "កំពុងទាញយកកំណែ v${status.version}..." else "Downloading v${status.version}...",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        LinearProgressIndicator(
                            progress = { status.progressPercent / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${status.progressPercent}%",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            val downloadedMb = String.format("%.1f", status.downloadedBytes / (1024f * 1024f))
                            val totalMb = String.format("%.1f MB", status.totalBytes / (1024f * 1024f))
                            Text(
                                text = "$downloadedMb / $totalMb",
                                style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { viewModel.dismissUpdate() }) {
                        Text(
                            text = if (isKm) "បោះបង់" else "Cancel",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                },
                shape = RoundedCornerShape(20.dp)
            )
        }

        is UpdateStatus.ReadyToInstall -> {
            AlertDialog(
                onDismissRequest = { viewModel.dismissUpdate() },
                icon = {
                    Icon(
                        imageVector = Icons.Default.CloudDownload,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                },
                title = {
                    Text(
                        text = if (isKm) "រួចរាល់សម្រាប់ការដំឡើង" else "Ready to Install",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                },
                text = {
                    Text(
                        text = if (isKm)
                            "ឯកសារកំណែ v${status.version} ត្រូវបានទាញយករួចរាល់។ សូមចុចដំឡើងដើម្បីបញ្ចប់។"
                        else
                            "Package v${status.version} downloaded successfully. Tap install to complete the update.",
                        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp)
                    )
                },
                confirmButton = {
                    Button(
                        onClick = { viewModel.installDownloadedApk(status.apkFile) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = if (isKm) "ដំឡើងឥឡូវនេះ" else "Install Now",
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.dismissUpdate() }) {
                        Text(
                            text = if (isKm) "ពេលក្រោយ" else "Later",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                shape = RoundedCornerShape(24.dp)
            )
        }

        is UpdateStatus.PermissionRequired -> {
            AlertDialog(
                onDismissRequest = { viewModel.dismissUpdate() },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(36.dp)
                    )
                },
                title = {
                    Text(
                        text = if (isKm) "ត្រូវការការអនុញ្ញាត" else "Permission Required",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                },
                text = {
                    Text(
                        text = if (isKm)
                            "ដើម្បីដំឡើងកំណែកម្មវិធីថ្មីដោយផ្ទាល់ពីក្នុង App សូមបើកសិទ្ធិ «អនុញ្ញាតឱ្យដំឡើងកម្មវិធីមិនស្គាល់ប្រភព» (Install unknown apps)។"
                        else
                            "To install update directly from the app, please enable 'Install unknown apps' permission.",
                        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp)
                    )
                },
                confirmButton = {
                    Button(
                        onClick = { viewModel.requestInstallPermission() },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = if (isKm) "បើកការអនុញ្ញាត" else "Open Settings",
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.dismissUpdate() }) {
                        Text(
                            text = if (isKm) "បោះបង់" else "Cancel",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                shape = RoundedCornerShape(24.dp)
            )
        }

        else -> Unit
    }
}
