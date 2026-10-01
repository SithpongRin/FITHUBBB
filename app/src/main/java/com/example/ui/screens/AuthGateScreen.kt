package com.example.ui.screens

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.example.localization.AppLanguage
import com.example.ui.theme.CharcoalBackground
import com.example.ui.theme.LimeAccent
import com.example.ui.viewmodel.FithubViewModel
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch

@Composable
fun AuthGateScreen(
    viewModel: FithubViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val appLanguage by viewModel.appLanguage.collectAsState()
    val isKm = appLanguage == AppLanguage.KHMER

    var isSignUp by remember { mutableStateOf(true) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var infoMessage by remember { mutableStateOf<String?>(null) }
    var showGoogleNoticeDialog by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // High-contrast Dual Language Switcher in top right corner
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(4.dp)
                        .testTag("auth_language_switcher"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isKm) LimeAccent else Color.Transparent,
                        onClick = { viewModel.setLanguage(AppLanguage.KHMER) }
                    ) {
                        Text(
                            text = "ខ្មែរ",
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isKm) CharcoalBackground else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (!isKm) LimeAccent else Color.Transparent,
                        onClick = { viewModel.setLanguage(AppLanguage.ENGLISH) }
                    ) {
                        Text(
                            text = "EN",
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (!isKm) CharcoalBackground else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }

            // App Brand Logo & Hero Header
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(LimeAccent, LimeAccent.copy(alpha = 0.75f))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.FitnessCenter,
                    contentDescription = "Fithub Logo",
                    tint = CharcoalBackground,
                    modifier = Modifier.size(38.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "FITHUB",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
            )

            Text(
                text = if (isKm) "កម្មវិធីតាមដានសុខភាព និងការហាត់ប្រាណឆ្លាតវៃ" else "Smart Fitness & Health Tracker",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Auth Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                        RoundedCornerShape(24.dp)
                    ),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Sign In vs Sign Up Tabs
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSignUp) LimeAccent else Color.Transparent)
                                .clickable {
                                    isSignUp = true
                                    errorMessage = null
                                    infoMessage = null
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isKm) "ចុះឈ្មោះ" else "Sign Up",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSignUp) CharcoalBackground else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (!isSignUp) LimeAccent else Color.Transparent)
                                .clickable {
                                    isSignUp = false
                                    errorMessage = null
                                    infoMessage = null
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isKm) "ចូលប្រើប្រាស់" else "Sign In",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (!isSignUp) CharcoalBackground else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Name Input (for sign up)
                    AnimatedVisibility(visible = isSignUp) {
                        Column {
                            OutlinedTextField(
                                value = name,
                                onValueChange = {
                                    name = it
                                    errorMessage = null
                                },
                                label = { Text(if (isKm) "ឈ្មោះពេញរបស់អ្នក" else "Full Name") },
                                placeholder = { Text(if (isKm) "ឧ. សិទ្ធិពង្ស" else "e.g. Sithpong") },
                                leadingIcon = {
                                    Icon(Icons.Default.Person, contentDescription = "Name", tint = LimeAccent)
                                },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("auth_name_input"),
                                shape = RoundedCornerShape(14.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }

                    // Email Input
                    OutlinedTextField(
                        value = email,
                        onValueChange = {
                            email = it
                            errorMessage = null
                        },
                        label = { Text(if (isKm) "អាសយដ្ឋាន Email" else "Email Address") },
                        placeholder = { Text("name@example.com") },
                        leadingIcon = {
                            Icon(Icons.Default.Email, contentDescription = "Email", tint = LimeAccent)
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auth_email_input"),
                        shape = RoundedCornerShape(14.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Password Input
                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            errorMessage = null
                        },
                        label = { Text(if (isKm) "ពាក្យសម្ងាត់" else "Password") },
                        placeholder = { Text(if (isKm) "យ៉ាងតិច ៦ តួអក្សរ" else "At least 6 characters") },
                        leadingIcon = {
                            Icon(Icons.Default.Lock, contentDescription = "Password", tint = LimeAccent)
                        },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auth_password_input"),
                        shape = RoundedCornerShape(14.dp)
                    )

                    // Error Message
                    if (errorMessage != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = errorMessage ?: "",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.error),
                            textAlign = TextAlign.Center
                        )
                    }

                    // Info Message
                    if (infoMessage != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = infoMessage ?: "",
                            style = MaterialTheme.typography.bodySmall.copy(color = LimeAccent),
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Submit Button
                    Button(
                        onClick = {
                            errorMessage = null
                            infoMessage = null

                            if (isSignUp) {
                                if (name.isBlank()) {
                                    errorMessage = if (isKm) "សូមបញ្ចូលឈ្មោះរបស់អ្នក" else "Please enter your name"
                                    return@Button
                                }
                                if (email.isBlank() || !email.contains("@")) {
                                    errorMessage = if (isKm) "សូមបញ្ចូលអ៊ីមែលឱ្យបានត្រឹមត្រូវ" else "Please enter a valid email address"
                                    return@Button
                                }
                                if (password.length < 6) {
                                    errorMessage = if (isKm) "ពាក្យសម្ងាត់ត្រូវមានយ៉ាងតិច ៦ តួអក្សរ" else "Password must be at least 6 characters"
                                    return@Button
                                }
                                isLoading = true
                                viewModel.signUpWithEmail(name, email, password) { success, err ->
                                    isLoading = false
                                    if (!success) {
                                        errorMessage = err
                                    }
                                }
                            } else {
                                if (email.isBlank() || !email.contains("@")) {
                                    errorMessage = if (isKm) "សូមបញ្ចូលអ៊ីមែលរបស់អ្នក" else "Please enter your email"
                                    return@Button
                                }
                                if (password.isBlank()) {
                                    errorMessage = if (isKm) "សូមបញ្ចូលពាក្យសម្ងាត់" else "Please enter your password"
                                    return@Button
                                }
                                isLoading = true
                                viewModel.signInWithEmail(email, password) { success, err ->
                                    isLoading = false
                                    if (!success) {
                                        errorMessage = err
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("auth_submit_btn"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LimeAccent,
                            contentColor = CharcoalBackground
                        ),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = CharcoalBackground,
                                strokeWidth = 2.5.dp
                            )
                        } else {
                            Text(
                                text = if (isSignUp) {
                                    if (isKm) "បង្កើតគណនី និងចាប់ផ្តើម" else "Create Account"
                                } else {
                                    if (isKm) "ចូលប្រើប្រាស់" else "Sign In"
                                },
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                        )
                        Text(
                            text = if (isKm) " ឬ " else " OR ",
                            style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Authentic Google Sign In with CredentialManager
                    OutlinedButton(
                        onClick = {
                            coroutineScope.launch {
                                isLoading = true
                                errorMessage = null
                                infoMessage = null
                                try {
                                    val credentialManager = CredentialManager.create(context)
                                    val googleIdOption = GetGoogleIdOption.Builder()
                                        .setFilterByAuthorizedAccounts(false)
                                        .setServerClientId("fithub-google-auth.apps.googleusercontent.com")
                                        .setAutoSelectEnabled(false)
                                        .build()

                                    val request = GetCredentialRequest.Builder()
                                        .addCredentialOption(googleIdOption)
                                        .build()

                                    val result = credentialManager.getCredential(context = context, request = request)
                                    val credential = result.credential
                                    if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                                        val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                                        viewModel.signInWithGoogle(
                                            name = googleIdTokenCredential.displayName,
                                            email = googleIdTokenCredential.id,
                                            photoUrl = googleIdTokenCredential.profilePictureUri?.toString()
                                        ) { _, _ -> }
                                    }
                                } catch (e: Exception) {
                                    // Show clear, polite dialog instead of scary red inline error
                                    showGoogleNoticeDialog = true
                                } finally {
                                    isLoading = false
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        enabled = !isLoading
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = "Google",
                            tint = LimeAccent,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (isKm) "បន្តជាមួយ Google" else "Continue with Google",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Continue as Guest Button (100% Offline Mode)
                    TextButton(
                        onClick = {
                            viewModel.continueAsGuest()
                        }
                    ) {
                        Text(
                            text = if (isKm) "ចូលប្រើជាភ្ញៀវ (ដំណើរការ Offline)" else "Continue as Guest (Offline Mode)",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Informative Google Notice Dialog
        if (showGoogleNoticeDialog) {
            AlertDialog(
                onDismissRequest = { showGoogleNoticeDialog = false },
                icon = {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = null,
                        tint = LimeAccent,
                        modifier = Modifier.size(36.dp)
                    )
                },
                title = {
                    Text(
                        text = "Google Sign-In",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                },
                text = {
                    Text(
                        text = if (isKm)
                            "ដើម្បីផ្ទៀងផ្ទាត់ជាមួយ Google Account ដោយសុវត្ថិភាព តម្រូវឱ្យភ្ជាប់គម្រោង Google Cloud Console OAuth Client ID ពី Developer។\n\nសម្រាប់ពេលនេះ សូមចុះឈ្មោះ ឬចូលប្រើប្រាស់ជាមួយ Email និង Password ផ្ទាល់ខាងលើ ដើម្បីចាប់ផ្តើមភ្លាមៗ!"
                        else
                            "Google Sign-In requires developer Google Cloud OAuth setup.\n\nPlease sign up or sign in directly with Email and Password above to get started immediately!",
                        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp)
                    )
                },
                confirmButton = {
                    Button(
                        onClick = { showGoogleNoticeDialog = false },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LimeAccent,
                            contentColor = CharcoalBackground
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(text = if (isKm) "យល់ព្រម" else "Got It", fontWeight = FontWeight.Bold)
                    }
                },
                shape = RoundedCornerShape(20.dp)
            )
        }
    }
}
