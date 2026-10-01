package com.example.ui.components

import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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

    // 1. Dynamic Feature Updates (OTA in-app update - concise and short)
    when (val status = featureStatus) {
        is DynamicFeatureStatus.UpdateAvailable -> {
            val pack = status.pack
            AlertDialog(
                onDismissRequest = { viewModel.dismissFeatureUpdate() },
                icon = {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = LimeAccent,
                        modifier = Modifier.size(32.dp)
                    )
                },
                title = {
                    Text(
                        text = if (isKm) "មានការអាប់ដែតថ្មី!" else "New Update Available!",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = if (isKm) "កំណែ៖ v${pack.featureVersion}" else "Version: v${pack.featureVersion}",
                            style = MaterialTheme.typography.labelLarge.copy(
                                color = LimeAccent,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = if (isKm) "អ្វីដែលបានបន្ថែមថ្មី៖" else "What's new:",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                        if (pack.plans.isNotEmpty()) {
                            Text(
                                text = if (isKm) "• ${pack.plans.first().name}" else "• ${pack.plans.first().name}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        if (pack.foods.isNotEmpty()) {
                            Text(
                                text = if (isKm) "• បញ្ជីម្ហូបអាហារសុខភាពខ្មែរ" else "• Healthy Khmer nutrition foods",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        if (pack.challenges.isNotEmpty()) {
                            Text(
                                text = if (isKm) "• កម្មវិធីប្រកួតប្រជែងថ្មីៗ" else "• New challenges",
                                style = MaterialTheme.typography.bodySmall
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
                shape = RoundedCornerShape(20.dp)
            )
        }

        is DynamicFeatureStatus.Applying -> {
            AlertDialog(
                onDismissRequest = { /* Non-dismissible */ },
                properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
                title = {
                    Text(
                        text = if (isKm) "កំពុងអាប់ដែត..." else "Updating...",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        LinearProgressIndicator(
                            progress = { status.progressPercent / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = LimeAccent
                        )
                        Text(
                            text = "${status.progressPercent}%",
                            style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                        modifier = Modifier.size(32.dp)
                    )
                },
                title = {
                    Text(
                        text = if (isKm) "អាប់ដែតរួចរាល់!" else "Update Complete!",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                },
                text = {
                    Text(
                        text = if (isKm)
                            "មុខងារថ្មីត្រូវបានដាក់បញ្ចូលរួចរាល់។ អ្នកអាចប្រើប្រាស់បានភ្លាមៗ!"
                        else
                            "New features added successfully. Ready to use!",
                        style = MaterialTheme.typography.bodyMedium
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
                        Text(text = if (isKm) "យល់ព្រម" else "OK", fontWeight = FontWeight.Bold)
                    }
                },
                shape = RoundedCornerShape(20.dp)
            )
        }

        else -> Unit
    }

    // 2. Core App APK Updates (Concise and short)
    when (val status = apkStatus) {
        is UpdateStatus.Available -> {
            AlertDialog(
                onDismissRequest = { viewModel.dismissUpdate() },
                icon = {
                    Icon(
                        imageVector = Icons.Default.SystemUpdate,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                },
                title = {
                    Text(
                        text = if (isKm) "មានការអាប់ដែតថ្មី!" else "New Update Available!",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = if (isKm) "កំណែ៖ v${status.version}" else "Version: v${status.version}",
                            style = MaterialTheme.typography.labelLarge.copy(
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = if (isKm) "អ្វីដែលបានបន្ថែមថ្មី៖" else "What's new:",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Text(
                            text = if (isKm) "• ${status.notesKm}" else "• ${status.notesEn}",
                            style = MaterialTheme.typography.bodySmall
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
                            text = if (isKm) "អាប់ដែតឥឡូវនេះ" else "Update Now",
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
                shape = RoundedCornerShape(20.dp)
            )
        }

        is UpdateStatus.Downloading -> {
            AlertDialog(
                onDismissRequest = { /* Downloading */ },
                title = {
                    Text(
                        text = if (isKm) "កំពុងទាញយក v${status.version}..." else "Downloading v${status.version}...",
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
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "${status.progressPercent}%",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                        )
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
                        modifier = Modifier.size(32.dp)
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
                        text = if (isKm) "សូមចុចដំឡើងដើម្បីបញ្ចប់ការអាប់ដែត។" else "Tap install to complete the update.",
                        style = MaterialTheme.typography.bodyMedium
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
                shape = RoundedCornerShape(20.dp)
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
                        modifier = Modifier.size(32.dp)
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
                        text = if (isKm) "សូមបើកសិទ្ធិអនុញ្ញាតឱ្យដំឡើងកម្មវិធី។" else "Please allow install permission.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                confirmButton = {
                    Button(
                        onClick = { viewModel.requestInstallPermission() },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = if (isKm) "បើកការកំណត់" else "Settings",
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
                shape = RoundedCornerShape(20.dp)
            )
        }

        else -> Unit
    }
}
