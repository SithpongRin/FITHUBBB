package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import coil.compose.AsyncImage
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calculations.CalorieCalculator
import com.example.calculations.FormatUtils
import com.example.calculations.ProteinCalculator
import com.example.localization.StringKey
import com.example.ui.components.AuthDialog
import com.example.ui.components.StatCard
import com.example.ui.theme.CharcoalBackground
import com.example.ui.theme.LimeAccent
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningAmber
import com.example.ui.viewmodel.FithubViewModel
import com.example.ui.viewmodel.ScreenTab

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: FithubViewModel,
    modifier: Modifier = Modifier
) {
    val profile by viewModel.profile.collectAsState()
    val unitSystem by viewModel.unitSystem.collectAsState()
    val currentAccount by viewModel.currentAccount.collectAsState()
    val syncStatus by viewModel.syncStatus.collectAsState()

    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showAuthDialog by remember { mutableStateOf(false) }

    // Computations
    val weightKg = profile?.weightKg ?: 70.0
    val heightCm = profile?.heightCm ?: 175.0
    val age = profile?.age ?: 25
    val sex = try {
        CalorieCalculator.Sex.valueOf(profile?.biologicalSex ?: "MALE")
    } catch (_: Throwable) {
        CalorieCalculator.Sex.MALE
    }
    val activity = try {
        CalorieCalculator.ActivityLevel.valueOf(profile?.activityLevel ?: "MODERATE")
    } catch (_: Throwable) {
        CalorieCalculator.ActivityLevel.MODERATE
    }
    val goal = try {
        CalorieCalculator.FitnessGoal.valueOf(profile?.fitnessGoal ?: "GENERAL_FITNESS")
    } catch (_: Throwable) {
        CalorieCalculator.FitnessGoal.GENERAL_FITNESS
    }

    val bmr = CalorieCalculator.calculateBmr(weightKg, heightCm, age, sex)
    val tdee = CalorieCalculator.calculateTdee(bmr, activity)
    val targetCalories = CalorieCalculator.calculateTargetCalories(tdee, goal)
    val proteinRange = ProteinCalculator.calculateProteinRange(weightKg, goal)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 24.dp, bottom = 100.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = viewModel.str(StringKey.NAV_PROFILE),
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Text(
                        text = "Physique parameters & energy balance",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }

                IconButton(
                    onClick = { viewModel.currentTab.value = ScreenTab.SETTINGS },
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .testTag("button_open_settings")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = LimeAccent
                    )
                }
            }
        }

        // Profile Avatar & Name Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(26.dp)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Google Account Profile Picture Avatar
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(LimeAccent),
                        contentAlignment = Alignment.Center
                    ) {
                        val avatarUrl = currentAccount.photoUrl
                        if (!avatarUrl.isNullOrEmpty()) {
                            AsyncImage(
                                model = avatarUrl,
                                contentDescription = "Google Profile Picture",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Avatar",
                                tint = CharcoalBackground,
                                modifier = Modifier.size(44.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = currentAccount.displayName.ifBlank { profile?.fullName ?: "Athlete" },
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )

                    if (currentAccount.email.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Verified Google Account",
                                tint = LimeAccent,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = currentAccount.email,
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "${goal.name.replace("_", " ")} | ${activity.name.replace("_", " ")}",
                        style = MaterialTheme.typography.bodySmall.copy(color = LimeAccent)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { showEditProfileDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("button_edit_profile")
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(viewModel.str(StringKey.SAVE_PROFILE))
                    }
                }
            }
        }

        // Cloud Account & Sync Status Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (currentAccount.isOnline) SuccessGreen.copy(alpha = 0.15f)
                                        else WarningAmber.copy(alpha = 0.15f)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (currentAccount.isOnline) Icons.Default.CloudDone else Icons.Default.CloudOff,
                                    contentDescription = "Cloud Status",
                                    tint = if (currentAccount.isOnline) SuccessGreen else WarningAmber,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Column {
                                Text(
                                    text = if (currentAccount.isOnline) currentAccount.displayName else viewModel.str(StringKey.ACCOUNT_OFFLINE_MODE),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                                Text(
                                    text = if (currentAccount.isOnline) currentAccount.email else "100% Offline Local Storage",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }

                        if (currentAccount.isOnline) {
                            IconButton(
                                onClick = { viewModel.triggerCloudSync() },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surface)
                            ) {
                                if (syncStatus is com.example.account.SyncStatus.Syncing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                        color = LimeAccent
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Sync,
                                        contentDescription = "Sync",
                                        tint = LimeAccent,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (currentAccount.isOnline) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = when (syncStatus) {
                                    is com.example.account.SyncStatus.Syncing -> viewModel.str(StringKey.ACCOUNT_SYNCING)
                                    is com.example.account.SyncStatus.Synced -> "${viewModel.str(StringKey.ACCOUNT_LAST_SYNCED)}: Just now"
                                    else -> viewModel.str(StringKey.ACCOUNT_ONLINE_SYNCED)
                                },
                                style = MaterialTheme.typography.bodySmall.copy(color = SuccessGreen, fontWeight = FontWeight.Medium)
                            )

                            TextButton(
                                onClick = { viewModel.signOutAccount() },
                                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text(viewModel.str(StringKey.ACCOUNT_SIGN_OUT), style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    } else {
                        Text(
                            text = viewModel.str(StringKey.ACCOUNT_OFFLINE_DESC),
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = { showAuthDialog = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = LimeAccent,
                                contentColor = CharcoalBackground
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.AccountCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = viewModel.str(StringKey.ACCOUNT_CONNECT_BTN),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }
        }

        // Biometric Stats Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    title = viewModel.str(StringKey.HEIGHT),
                    value = FormatUtils.formatHeight(heightCm, unitSystem),
                    icon = Icons.Default.Height,
                    testTag = "stat_height",
                    modifier = Modifier.weight(1f)
                )

                StatCard(
                    title = viewModel.str(StringKey.WEIGHT),
                    value = FormatUtils.formatWeight(weightKg, unitSystem),
                    icon = Icons.Default.MonitorWeight,
                    testTag = "stat_weight",
                    modifier = Modifier.weight(1f)
                )

                StatCard(
                    title = viewModel.str(StringKey.AGE),
                    value = "$age yrs",
                    icon = Icons.Default.CalendarToday,
                    testTag = "stat_age",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Calculation Targets (BMR, TDEE, Calorie Target, Protein Range)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Metabolic Calculations (Mifflin-St Jeor)",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(viewModel.str(StringKey.BMR_ESTIMATE), style = MaterialTheme.typography.bodyMedium)
                        Text(FormatUtils.formatCalories(bmr), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(viewModel.str(StringKey.TDEE_ESTIMATE), style = MaterialTheme.typography.bodyMedium)
                        Text(FormatUtils.formatCalories(tdee), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(viewModel.str(StringKey.CALORIE_TARGET), style = MaterialTheme.typography.bodyMedium)
                        Text("$targetCalories kcal", style = MaterialTheme.typography.bodyMedium.copy(color = LimeAccent, fontWeight = FontWeight.Bold))
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Target Protein Range", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "${FormatUtils.formatGrams(proteinRange.minGrams)} - ${FormatUtils.formatGrams(proteinRange.maxGrams)}",
                            style = MaterialTheme.typography.bodyMedium.copy(color = LimeAccent, fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }
    }

    // Edit Profile Dialog (Swipe-down ModalBottomSheet)
    if (showEditProfileDialog) {
        var nameInput by remember { mutableStateOf(profile?.fullName ?: "") }
        var ageVal by remember { mutableIntStateOf(profile?.age ?: 25) }
        var heightVal by remember { mutableIntStateOf((profile?.heightCm ?: 175.0).toInt()) }
        var weightVal by remember { mutableDoubleStateOf(profile?.weightKg ?: 70.0) }
        var sexInput by remember { mutableStateOf(profile?.biologicalSex ?: "MALE") }
        var activityInput by remember { mutableStateOf(profile?.activityLevel ?: "MODERATE") }
        var goalInput by remember { mutableStateOf(profile?.fitnessGoal ?: "GENERAL_FITNESS") }

        ModalBottomSheet(
            onDismissRequest = { showEditProfileDialog = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            dragHandle = {
                BottomSheetDefaults.DragHandle(color = LimeAccent.copy(alpha = 0.6f))
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 36.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Edit Biometrics",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    IconButton(onClick = { showEditProfileDialog = false }) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text(viewModel.str(StringKey.FULL_NAME)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Interactive Biometric Steppers Card (Zero typing required)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Age Stepper
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = viewModel.str(StringKey.AGE),
                                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                )
                                Text(
                                    text = "$ageVal yrs",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = LimeAccent)
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { if (ageVal > 12) ageVal-- },
                                    modifier = Modifier.size(34.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surface)
                                ) {
                                    Icon(Icons.Default.Remove, contentDescription = "Decrease Age", tint = MaterialTheme.colorScheme.onSurface)
                                }
                                IconButton(
                                    onClick = { if (ageVal < 100) ageVal++ },
                                    modifier = Modifier.size(34.dp).clip(CircleShape).background(LimeAccent)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Increase Age", tint = CharcoalBackground)
                                }
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                        // Height Stepper
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = viewModel.str(StringKey.HEIGHT),
                                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                )
                                Text(
                                    text = "$heightVal cm",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = LimeAccent)
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { if (heightVal > 100) heightVal -= 1 },
                                    modifier = Modifier.size(34.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surface)
                                ) {
                                    Icon(Icons.Default.Remove, contentDescription = "Decrease Height", tint = MaterialTheme.colorScheme.onSurface)
                                }
                                Button(
                                    onClick = { if (heightVal > 105) heightVal -= 5 },
                                    contentPadding = PaddingValues(horizontal = 6.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.onSurface),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Text("-5", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                Button(
                                    onClick = { if (heightVal < 235) heightVal += 5 },
                                    contentPadding = PaddingValues(horizontal = 6.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.onSurface),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Text("+5", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                IconButton(
                                    onClick = { if (heightVal < 240) heightVal += 1 },
                                    modifier = Modifier.size(34.dp).clip(CircleShape).background(LimeAccent)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Increase Height", tint = CharcoalBackground)
                                }
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                        // Weight Stepper
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = viewModel.str(StringKey.WEIGHT),
                                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                )
                                Text(
                                    text = String.format(java.util.Locale.US, "%.1f kg", weightVal),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = LimeAccent)
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { if (weightVal > 30.0) weightVal = (Math.round((weightVal - 0.5) * 10.0)) / 10.0 },
                                    modifier = Modifier.size(34.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surface)
                                ) {
                                    Icon(Icons.Default.Remove, contentDescription = "Decrease Weight", tint = MaterialTheme.colorScheme.onSurface)
                                }
                                Button(
                                    onClick = { if (weightVal > 32.0) weightVal = (Math.round((weightVal - 2.0) * 10.0)) / 10.0 },
                                    contentPadding = PaddingValues(horizontal = 6.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.onSurface),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Text("-2", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                Button(
                                    onClick = { if (weightVal < 248.0) weightVal = (Math.round((weightVal + 2.0) * 10.0)) / 10.0 },
                                    contentPadding = PaddingValues(horizontal = 6.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.onSurface),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Text("+2", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                IconButton(
                                    onClick = { if (weightVal < 250.0) weightVal = (Math.round((weightVal + 0.5) * 10.0)) / 10.0 },
                                    modifier = Modifier.size(34.dp).clip(CircleShape).background(LimeAccent)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Increase Weight", tint = CharcoalBackground)
                                }
                            }
                        }
                    }
                }

                Text("Sex", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("MALE", "FEMALE", "OTHER").forEach { s ->
                        FilterChip(
                            selected = sexInput == s,
                            onClick = { sexInput = s },
                            label = { Text(s, maxLines = 1, softWrap = false) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = LimeAccent,
                                selectedLabelColor = CharcoalBackground
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Text("Training Goal", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "GENERAL_FITNESS" to "General",
                        "BUILD_STRENGTH" to "Strength",
                        "WEIGHT_MANAGEMENT" to "Weight"
                    ).forEach { (g, label) ->
                        FilterChip(
                            selected = goalInput == g,
                            onClick = { goalInput = g },
                            label = { Text(label, maxLines = 1, softWrap = false) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = LimeAccent,
                                selectedLabelColor = CharcoalBackground
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { showEditProfileDialog = false },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel", maxLines = 1, softWrap = false)
                    }

                    Button(
                        onClick = {
                            viewModel.updateProfile(
                                name = nameInput.ifBlank { "Athlete" },
                                age = ageVal,
                                sex = sexInput,
                                heightCm = heightVal.toDouble(),
                                weightKg = weightVal,
                                activityLevel = activityInput,
                                goal = goalInput
                            )
                            showEditProfileDialog = false
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

    if (showAuthDialog) {
        AuthDialog(
            viewModel = viewModel,
            onDismiss = { showAuthDialog = false }
        )
    }
}
