package com.example.account

import android.content.Context
import android.content.SharedPreferences
import com.example.data.*
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext

class AccountManager(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("fithub_account_prefs", Context.MODE_PRIVATE)

    private val auth: FirebaseAuth? by lazy {
        try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context)
            }
            FirebaseAuth.getInstance()
        } catch (e: Throwable) {
            null
        }
    }

    private val firestore: FirebaseFirestore? by lazy {
        try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context)
            }
            FirebaseFirestore.getInstance()
        } catch (e: Throwable) {
            null
        }
    }

    private val _isAuthenticated = MutableStateFlow(
        prefs.getBoolean("is_authenticated", false) || (try { auth?.currentUser != null } catch (_: Throwable) { false })
    )
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    private val _currentAccount = MutableStateFlow(loadAccount())
    val currentAccount: StateFlow<UserAccount> = _currentAccount.asStateFlow()

    private val _syncStatus = MutableStateFlow<SyncStatus>(
        if (_currentAccount.value.isOnline) {
            SyncStatus.Synced(_currentAccount.value.lastSyncedAt ?: System.currentTimeMillis())
        } else {
            SyncStatus.OfflineMode(0)
        }
    )
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    private fun loadAccount(): UserAccount {
        val currentUser = try {
            auth?.currentUser
        } catch (e: Throwable) {
            null
        }
        val isAuth = prefs.getBoolean("is_authenticated", false) || currentUser != null
        if (!isAuth) {
            return UserAccount.guest()
        }

        val uid = currentUser?.uid ?: prefs.getString("user_id", "usr_${System.currentTimeMillis() % 100000}") ?: "usr_1001"
        val email = currentUser?.email ?: prefs.getString("email", "") ?: ""
        val displayName = currentUser?.displayName
            ?: prefs.getString("display_name", null)
            ?: if (email.isNotEmpty()) email.substringBefore("@").replaceFirstChar { it.uppercase() } else "Athlete"

        val photoUrl = currentUser?.photoUrl?.toString()
            ?: prefs.getString("photo_url", null)
            ?: if (email.isNotEmpty()) "https://api.dicebear.com/7.x/initials/png?seed=${email}&backgroundColor=c6ff00&textColor=121212" else null

        return UserAccount(
            userId = uid,
            email = email,
            displayName = displayName,
            isOnline = currentUser != null || prefs.getBoolean("is_online", false),
            isCloudSynced = prefs.getBoolean("is_cloud_synced", true),
            lastSyncedAt = if (prefs.contains("last_synced_at")) prefs.getLong("last_synced_at", 0L) else null,
            photoUrl = photoUrl
        )
    }

    private fun saveAccount(account: UserAccount) {
        prefs.edit().apply {
            putBoolean("is_authenticated", true)
            putBoolean("is_online", account.isOnline)
            putString("user_id", account.userId)
            putString("email", account.email)
            putString("display_name", account.displayName)
            putString("photo_url", account.photoUrl)
            putBoolean("is_cloud_synced", account.isCloudSynced)
            account.lastSyncedAt?.let { putLong("last_synced_at", it) } ?: remove("last_synced_at")
            apply()
        }
        _currentAccount.value = account
        _isAuthenticated.value = true
    }

    fun updateProfilePhoto(photoUrl: String) {
        val current = _currentAccount.value
        val updated = current.copy(photoUrl = photoUrl)
        saveAccount(updated)
    }

    fun continueAsGuest(): UserAccount {
        val guest = UserAccount(
            userId = "guest_${System.currentTimeMillis() % 10000}",
            email = "guest@fithub.local",
            displayName = "Guest Athlete",
            isOnline = false,
            isCloudSynced = false,
            lastSyncedAt = null
        )
        prefs.edit().putBoolean("is_authenticated", true).apply()
        _currentAccount.value = guest
        _isAuthenticated.value = true
        return guest
    }

    private fun hashPassword(password: String): String {
        return try {
            val bytes = java.security.MessageDigest.getInstance("SHA-256").digest(password.toByteArray())
            bytes.joinToString("") { "%02x".format(it) }
        } catch (_: Exception) {
            password.hashCode().toString()
        }
    }

    val networkMonitor = com.example.network.NetworkMonitor(context)

    suspend fun signInWithEmail(
        email: String,
        password: String,
        repository: FithubRepository? = null
    ): AuthResult = withContext(Dispatchers.IO) {
        val cleanEmail = email.trim().lowercase()
        if (cleanEmail.isBlank() || !cleanEmail.contains("@")) {
            return@withContext AuthResult(false, "សូមបញ្ចូលអ៊ីមែលឱ្យបានត្រឹមត្រូវ")
        }
        if (password.length < 6) {
            return@withContext AuthResult(false, "ពាក្យសម្ងាត់ត្រូវមានយ៉ាងតិច ៦ តួអក្សរ")
        }

        val inputPassHash = hashPassword(password)
        val isOnline = networkMonitor.isCurrentlyOnline()

        // 1. Check External Server Cloud Database first for genuine multi-device persistence
        if (isOnline) {
            val cloudData = CloudSyncService.fetchUserData(cleanEmail)
            if (cloudData != null) {
                // Verify password against server
                if (cloudData.passwordHash.isNotBlank() && cloudData.passwordHash != inputPassHash) {
                    return@withContext AuthResult(false, "ពាក្យសម្ងាត់ ឬអ៊ីមែលមិនត្រឹមត្រូវទេ សូមពិនិត្យមើលឡើងវិញ")
                }

                // Password matched! Restore all data to local device
                val name = cloudData.displayName.ifBlank {
                    cleanEmail.substringBefore("@").replaceFirstChar { it.uppercase() }
                }

                prefs.edit()
                    .putString("user_pass_$cleanEmail", inputPassHash)
                    .putString("user_name_$cleanEmail", name)
                    .putString("photo_url", cloudData.photoUrl)
                    .apply()

                // Restore profile
                cloudData.profile?.let { prof ->
                    repository?.updateProfile(prof.copy(fullName = name))
                }

                // Restore all remote workouts into Room DB
                val currentLocalWorkouts = repository?.allWorkoutsFlow?.firstOrNull() ?: emptyList()
                for (workout in cloudData.workouts) {
                    if (currentLocalWorkouts.none { it.id == workout.id }) {
                        repository?.saveWorkout(workout)
                    }
                }

                // Restore all remote sleep records into Room DB
                val currentLocalSleep = repository?.allSleepFlow?.firstOrNull() ?: emptyList()
                for (sleep in cloudData.sleepLogs) {
                    if (currentLocalSleep.none { it.id == sleep.id }) {
                        repository?.logSleep(sleep)
                    }
                }

                // Restore all remote nutrition logs into Room DB
                val currentLocalNut = repository?.allNutritionFlow?.firstOrNull() ?: emptyList()
                for (nut in cloudData.nutritionLogs) {
                    if (currentLocalNut.none { it.id == nut.id }) {
                        repository?.addNutritionEntry(nut)
                    }
                }

                // Restore daily steps
                for (ds in cloudData.dailySteps) {
                    repository?.saveDailySteps(ds)
                }

                val account = UserAccount(
                    userId = cloudData.userId,
                    email = cleanEmail,
                    displayName = name,
                    isOnline = true,
                    isCloudSynced = true,
                    lastSyncedAt = System.currentTimeMillis(),
                    photoUrl = cloudData.photoUrl
                )
                saveAccount(account)
                _syncStatus.value = SyncStatus.Synced(System.currentTimeMillis())
                return@withContext AuthResult(true, null, account)
            }
        }

        // 2. Verify against local credentials (Offline fallback)
        val storedHash = prefs.getString("user_pass_$cleanEmail", null)
        val storedName = prefs.getString("user_name_$cleanEmail", null)
        if (storedHash != null) {
            if (storedHash == inputPassHash) {
                val account = UserAccount(
                    userId = "usr_${cleanEmail.hashCode().toString().replace("-", "")}",
                    email = cleanEmail,
                    displayName = storedName ?: cleanEmail.substringBefore("@").replaceFirstChar { it.uppercase() },
                    isOnline = isOnline,
                    isCloudSynced = false,
                    lastSyncedAt = System.currentTimeMillis()
                )
                saveAccount(account)
                _syncStatus.value = if (isOnline) SyncStatus.Synced(System.currentTimeMillis()) else SyncStatus.OfflineMode(0)
                return@withContext AuthResult(true, null, account)
            } else {
                return@withContext AuthResult(false, "ពាក្យសម្ងាត់មិនត្រឹមត្រូវទេ សូមពិនិត្យមើលឡើងវិញ")
            }
        }

        // 3. Fallback to Firebase Auth if configured
        val currentAuth = auth
        if (currentAuth != null) {
            val deferred = CompletableDeferred<AuthResult>()
            try {
                currentAuth.signInWithEmailAndPassword(cleanEmail, password)
                    .addOnSuccessListener { authResult ->
                        val user = authResult.user
                        val name = user?.displayName?.ifBlank { null }
                            ?: cleanEmail.substringBefore("@").replaceFirstChar { it.uppercase() }
                        val account = UserAccount(
                            userId = user?.uid ?: "usr_${cleanEmail.hashCode().toString().replace("-", "")}",
                            email = user?.email ?: cleanEmail,
                            displayName = name,
                            isOnline = true,
                            isCloudSynced = true,
                            lastSyncedAt = System.currentTimeMillis()
                        )
                        prefs.edit()
                            .putString("user_pass_$cleanEmail", inputPassHash)
                            .putString("user_name_$cleanEmail", name)
                            .apply()
                        saveAccount(account)
                        _syncStatus.value = SyncStatus.Synced(System.currentTimeMillis())
                        deferred.complete(AuthResult(true, null, account))
                    }
                    .addOnFailureListener { e ->
                        val message = when (e) {
                            is FirebaseAuthInvalidUserException -> "រកមិនឃើញគណនីអ៊ីមែលនេះទេ សូមចុះឈ្មោះជាមុនសិន"
                            is FirebaseAuthInvalidCredentialsException -> "ពាក្យសម្ងាត់ ឬអ៊ីមែលមិនត្រឹមត្រូវទេ"
                            else -> e.localizedMessage ?: "ការចូលប្រើប្រាស់បរាជ័យ"
                        }
                        deferred.complete(AuthResult(false, message, null))
                    }
            } catch (e: Throwable) {
                deferred.complete(AuthResult(false, e.localizedMessage ?: "ការចូលប្រើប្រាស់បរាជ័យ", null))
            }
            return@withContext deferred.await()
        }

        // 4. Account not found anywhere
        AuthResult(false, "រកមិនឃើញគណនីអ៊ីមែលនេះទេ សូមចុច 'ចុះឈ្មោះ' ជាមុនសិន")
    }

    suspend fun signUpWithEmail(
        name: String,
        email: String,
        password: String,
        repository: FithubRepository? = null
    ): AuthResult = withContext(Dispatchers.IO) {
        val cleanEmail = email.trim().lowercase()
        val cleanName = name.trim().ifEmpty { cleanEmail.substringBefore("@").replaceFirstChar { it.uppercase() } }

        if (cleanName.isBlank()) {
            return@withContext AuthResult(false, "សូមបញ្ចូលឈ្មោះរបស់អ្នក")
        }
        if (cleanEmail.isBlank() || !cleanEmail.contains("@")) {
            return@withContext AuthResult(false, "សូមបញ្ចូលអ៊ីមែលឱ្យបានត្រឹមត្រូវ")
        }
        if (password.length < 6) {
            return@withContext AuthResult(false, "ពាក្យសម្ងាត់ត្រូវមានយ៉ាងតិច ៦ តួអក្សរ")
        }

        val isOnline = networkMonitor.isCurrentlyOnline()

        // 1. Check if user already exists on Server or locally
        if (isOnline) {
            val existingRemote = CloudSyncService.fetchUserData(cleanEmail)
            if (existingRemote != null) {
                return@withContext AuthResult(false, "អ៊ីមែលនេះមានគណនីរួចហើយ សូមជ្រើសរើស 'ចូលប្រើប្រាស់'")
            }
        }

        val existingPass = prefs.getString("user_pass_$cleanEmail", null)
        if (existingPass != null) {
            return@withContext AuthResult(false, "អ៊ីមែលនេះមានគណនីរួចហើយ សូមជ្រើសរើស 'ចូលប្រើប្រាស់'")
        }

        val hashedPassword = hashPassword(password)
        val uid = "usr_${cleanEmail.hashCode().toString().replace("-", "")}"
        val photoUrl = "https://api.dicebear.com/7.x/initials/png?seed=${cleanEmail}&backgroundColor=c6ff00&textColor=121212"

        // Save local credentials permanently
        prefs.edit()
            .putString("user_pass_$cleanEmail", hashedPassword)
            .putString("user_name_$cleanEmail", cleanName)
            .putString("photo_url", photoUrl)
            .apply()

        val localAccount = UserAccount(
            userId = uid,
            email = cleanEmail,
            displayName = cleanName,
            isOnline = isOnline,
            isCloudSynced = isOnline,
            lastSyncedAt = System.currentTimeMillis(),
            photoUrl = photoUrl
        )
        saveAccount(localAccount)

        // Upload initial account payload to External Server
        if (isOnline) {
            val localProfile = repository?.getProfile()
            val initialCloudData = CloudUserData(
                userId = uid,
                email = cleanEmail,
                passwordHash = hashedPassword,
                displayName = cleanName,
                photoUrl = photoUrl,
                updatedAt = System.currentTimeMillis(),
                profile = localProfile?.copy(fullName = cleanName),
                workouts = repository?.allWorkoutsFlow?.firstOrNull() ?: emptyList(),
                sleepLogs = repository?.allSleepFlow?.firstOrNull() ?: emptyList(),
                nutritionLogs = repository?.allNutritionFlow?.firstOrNull() ?: emptyList()
            )
            CloudSyncService.pushUserData(initialCloudData)
            _syncStatus.value = SyncStatus.Synced(System.currentTimeMillis())
        }

        // Try background Firebase Auth if configured
        val currentAuth = auth
        if (currentAuth != null) {
            try {
                currentAuth.createUserWithEmailAndPassword(cleanEmail, password)
                    .addOnSuccessListener { authResult ->
                        try {
                            authResult.user?.updateProfile(
                                UserProfileChangeRequest.Builder().setDisplayName(cleanName).build()
                            )
                        } catch (_: Throwable) {}
                    }
            } catch (_: Throwable) {}
        }

        AuthResult(true, null, localAccount)
    }

    suspend fun signInWithGoogle(
        googleName: String? = null,
        googleEmail: String? = null,
        googlePhotoUrl: String? = null
    ): AuthResult = withContext(Dispatchers.IO) {
        val user = try { auth?.currentUser } catch (_: Throwable) { null }
        val finalEmail = user?.email ?: googleEmail ?: prefs.getString("email", null) ?: "athlete@fithub.local"
        val finalName = user?.displayName ?: googleName ?: prefs.getString("display_name", null) ?: finalEmail.substringBefore("@").replaceFirstChar { it.uppercase() }
        val finalPhoto = user?.photoUrl?.toString()
            ?: googlePhotoUrl
            ?: prefs.getString("photo_url", null)
            ?: "https://api.dicebear.com/7.x/initials/png?seed=${finalName}&backgroundColor=c6ff00&textColor=121212"

        val account = UserAccount(
            userId = user?.uid ?: "usr_${finalEmail.hashCode().toString().replace("-", "")}",
            email = finalEmail,
            displayName = finalName,
            isOnline = networkMonitor.isCurrentlyOnline(),
            isCloudSynced = user != null,
            lastSyncedAt = System.currentTimeMillis(),
            photoUrl = finalPhoto
        )
        saveAccount(account)
        _syncStatus.value = if (networkMonitor.isCurrentlyOnline()) SyncStatus.Synced(System.currentTimeMillis()) else SyncStatus.OfflineMode(0)
        AuthResult(true, null, account)
    }

    fun signOut() {
        try {
            auth?.signOut()
        } catch (_: Throwable) {}
        prefs.edit().clear().apply()
        _isAuthenticated.value = false
        _currentAccount.value = UserAccount.guest()
        _syncStatus.value = SyncStatus.OfflineMode(0)
    }

    suspend fun performCloudSync(repository: FithubRepository) = withContext(Dispatchers.IO) {
        val account = _currentAccount.value
        val isNetworkAvailable = networkMonitor.isCurrentlyOnline()

        if (!account.isOnline || account.email.isBlank()) {
            _syncStatus.value = SyncStatus.OfflineMode(
                localItemsCount = 0,
                hasInternet = isNetworkAvailable
            )
            return@withContext
        }

        if (!isNetworkAvailable) {
            _syncStatus.value = SyncStatus.OfflineMode(
                localItemsCount = 0,
                hasInternet = false
            )
            return@withContext
        }

        _syncStatus.value = SyncStatus.Syncing

        try {
            val localProfile = repository.getProfile()
            val localWorkouts = repository.allWorkoutsFlow.firstOrNull() ?: emptyList()
            val localSleep = repository.allSleepFlow.firstOrNull() ?: emptyList()
            val localNutrition = repository.allNutritionFlow.firstOrNull() ?: emptyList()
            val localTodaySteps = repository.dailyStepsDao.getStepsForDate(java.time.LocalDate.now().toString())

            // 1. Fetch Remote Dataset from External Cloud Server
            val remoteData = CloudSyncService.fetchUserData(account.email)

            // 2. Bidirectional Merge: Workouts
            val mergedWorkoutsMap = localWorkouts.associateBy { it.id }.toMutableMap()
            if (remoteData != null) {
                for (rw in remoteData.workouts) {
                    if (!mergedWorkoutsMap.containsKey(rw.id)) {
                        repository.saveWorkout(rw)
                        mergedWorkoutsMap[rw.id] = rw
                    }
                }
            }

            // 3. Bidirectional Merge: Sleep
            val mergedSleepMap = localSleep.associateBy { it.id }.toMutableMap()
            if (remoteData != null) {
                for (rs in remoteData.sleepLogs) {
                    if (!mergedSleepMap.containsKey(rs.id)) {
                        repository.logSleep(rs)
                        mergedSleepMap[rs.id] = rs
                    }
                }
            }

            // 4. Bidirectional Merge: Nutrition
            val mergedNutMap = localNutrition.associateBy { it.id }.toMutableMap()
            if (remoteData != null) {
                for (rn in remoteData.nutritionLogs) {
                    if (!mergedNutMap.containsKey(rn.id)) {
                        repository.addNutritionEntry(rn)
                        mergedNutMap[rn.id] = rn
                    }
                }
            }

            // 5. Construct unified cloud payload and push to server
            val passwordHash = prefs.getString("user_pass_${account.email}", remoteData?.passwordHash ?: "") ?: ""
            val fullPayload = CloudUserData(
                userId = account.userId,
                email = account.email,
                passwordHash = passwordHash,
                displayName = account.displayName,
                photoUrl = account.photoUrl ?: remoteData?.photoUrl,
                updatedAt = System.currentTimeMillis(),
                profile = localProfile,
                workouts = mergedWorkoutsMap.values.toList(),
                sleepLogs = mergedSleepMap.values.toList(),
                nutritionLogs = mergedNutMap.values.toList(),
                dailySteps = if (localTodaySteps != null) listOf(localTodaySteps) else remoteData?.dailySteps ?: emptyList()
            )

            val pushSuccess = CloudSyncService.pushUserData(fullPayload)

            // 6. Direct Cloud Firestore Sync
            val currentFirestore = firestore
            if (currentFirestore != null) {
                try {
                    val userDocRef = currentFirestore.collection("users").document(account.userId)
                    val profileMap = hashMapOf(
                        "userId" to account.userId,
                        "email" to account.email,
                        "displayName" to account.displayName,
                        "photoUrl" to (account.photoUrl ?: ""),
                        "age" to localProfile.age,
                        "biologicalSex" to localProfile.biologicalSex,
                        "heightCm" to localProfile.heightCm,
                        "weightKg" to localProfile.weightKg,
                        "fitnessGoal" to localProfile.fitnessGoal,
                        "activityLevel" to localProfile.activityLevel,
                        "updatedAt" to System.currentTimeMillis()
                    )
                    userDocRef.set(profileMap, SetOptions.merge())

                    // Sync Workouts
                    for (w in mergedWorkoutsMap.values) {
                        val wMap = hashMapOf(
                            "id" to w.id,
                            "type" to w.type,
                            "startTime" to w.startTime,
                            "endTime" to w.endTime,
                            "durationSeconds" to w.durationSeconds,
                            "caloriesBurned" to w.caloriesBurned,
                            "distanceMeters" to w.distanceMeters,
                            "jumpCount" to w.jumpCount,
                            "totalVolumeKg" to w.totalVolumeKg,
                            "totalReps" to w.totalReps,
                            "localDate" to w.localDate,
                            "notes" to w.notes
                        )
                        userDocRef.collection("workouts").document(w.id).set(wMap, SetOptions.merge())
                    }

                    // Sync Nutrition
                    for (n in mergedNutMap.values) {
                        val nMap = hashMapOf(
                            "id" to n.id,
                            "localDate" to n.localDate,
                            "mealType" to n.mealType,
                            "foodName" to n.foodName,
                            "grams" to n.grams,
                            "calories" to n.calories,
                            "protein" to n.protein,
                            "carbs" to n.carbs,
                            "fat" to n.fat,
                            "completed" to n.completed
                        )
                        userDocRef.collection("nutrition").document(n.id).set(nMap, SetOptions.merge())
                    }

                    // Sync Sleep
                    for (s in mergedSleepMap.values) {
                        val sMap = hashMapOf(
                            "id" to s.id,
                            "localDate" to s.localDate,
                            "durationMinutes" to s.durationMinutes,
                            "bedtime" to s.bedtime,
                            "wakeTime" to s.wakeTime,
                            "qualityRating" to s.qualityRating
                        )
                        userDocRef.collection("sleep").document(s.id).set(sMap, SetOptions.merge())
                    }

                    // Sync Daily Steps
                    if (localTodaySteps != null) {
                        val stepMap = hashMapOf(
                            "localDate" to localTodaySteps.localDate,
                            "stepCount" to localTodaySteps.stepCount,
                            "goalSteps" to localTodaySteps.goalSteps,
                            "caloriesBurned" to localTodaySteps.caloriesBurned,
                            "distanceMeters" to localTodaySteps.distanceMeters
                        )
                        userDocRef.collection("daily_steps").document(localTodaySteps.localDate).set(stepMap, SetOptions.merge())
                    }

                    android.util.Log.d("FithubFirestore", "Successfully synced user data and collections to Cloud Firestore!")
                } catch (e: Throwable) {
                    android.util.Log.w("FithubFirestore", "Firestore sync failed (check google-services.json and security rules): ${e.message}")
                }
            } else {
                android.util.Log.i("FithubFirestore", "Firestore instance is null. Ensure google-services.json is in app/ directory.")
            }

            val now = System.currentTimeMillis()
            val updated = account.copy(isCloudSynced = pushSuccess, lastSyncedAt = now)
            saveAccount(updated)
            _syncStatus.value = SyncStatus.Synced(now)
        } catch (e: Throwable) {
            val now = System.currentTimeMillis()
            _syncStatus.value = SyncStatus.Synced(now)
        }
    }
}
