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

    suspend fun signInWithEmail(email: String, password: String): AuthResult = withContext(Dispatchers.IO) {
        val cleanEmail = email.trim().lowercase()
        if (cleanEmail.isBlank() || !cleanEmail.contains("@")) {
            return@withContext AuthResult(false, "សូមបញ្ចូលអ៊ីមែលឱ្យបានត្រឹមត្រូវ")
        }
        if (password.length < 6) {
            return@withContext AuthResult(false, "ពាក្យសម្ងាត់ត្រូវមានយ៉ាងតិច ៦ តួអក្សរ")
        }

        val storedHash = prefs.getString("user_pass_$cleanEmail", null)
        val storedName = prefs.getString("user_name_$cleanEmail", null)

        // 1. Verify against local credentials if registered locally
        if (storedHash != null) {
            if (storedHash == hashPassword(password)) {
                val account = UserAccount(
                    userId = "usr_${cleanEmail.hashCode().toString().replace("-", "")}",
                    email = cleanEmail,
                    displayName = storedName ?: cleanEmail.substringBefore("@").replaceFirstChar { it.uppercase() },
                    isOnline = networkMonitor.isCurrentlyOnline(),
                    isCloudSynced = false,
                    lastSyncedAt = System.currentTimeMillis()
                )
                saveAccount(account)
                _syncStatus.value = if (networkMonitor.isCurrentlyOnline()) SyncStatus.Synced(System.currentTimeMillis()) else SyncStatus.OfflineMode(0)
                return@withContext AuthResult(true, null, account)
            } else {
                return@withContext AuthResult(false, "ពាក្យសម្ងាត់មិនត្រឹមត្រូវទេ សូមពិនិត្យមើលឡើងវិញ")
            }
        }

        // 2. Fallback to Firebase Auth if local credentials not found
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
                        // Save credentials locally as well for offline fallback
                        prefs.edit()
                            .putString("user_pass_$cleanEmail", hashPassword(password))
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

        // 3. If no local record and no Firebase configured
        AuthResult(false, "រកមិនឃើញគណនីអ៊ីមែលនេះទេ សូមចុច 'ចុះឈ្មោះ' ជាមុនសិន")
    }

    suspend fun signUpWithEmail(name: String, email: String, password: String): AuthResult = withContext(Dispatchers.IO) {
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

        // Check if email already registered locally
        val existingPass = prefs.getString("user_pass_$cleanEmail", null)
        if (existingPass != null) {
            return@withContext AuthResult(false, "អ៊ីមែលនេះមានគណនីរួចហើយ សូមជ្រើសរើស 'ចូលប្រើប្រាស់'")
        }

        val hashedPassword = hashPassword(password)
        val uid = "usr_${cleanEmail.hashCode().toString().replace("-", "")}"

        // Save local credentials permanently (100% Offline-First)
        prefs.edit()
            .putString("user_pass_$cleanEmail", hashedPassword)
            .putString("user_name_$cleanEmail", cleanName)
            .apply()

        val localAccount = UserAccount(
            userId = uid,
            email = cleanEmail,
            displayName = cleanName,
            isOnline = networkMonitor.isCurrentlyOnline(),
            isCloudSynced = false,
            lastSyncedAt = System.currentTimeMillis()
        )
        saveAccount(localAccount)

        // Try syncing with Firebase Auth in the background if configured
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

    val networkMonitor = com.example.network.NetworkMonitor(context)

    suspend fun performCloudSync(repository: FithubRepository) = withContext(Dispatchers.IO) {
        val account = _currentAccount.value
        val isNetworkAvailable = networkMonitor.isCurrentlyOnline()

        if (!account.isOnline) {
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

        val currentFirestore = firestore
        if (currentFirestore == null) {
            val now = System.currentTimeMillis()
            _syncStatus.value = SyncStatus.Synced(now)
            return@withContext
        }

        _syncStatus.value = SyncStatus.Syncing

        val userId = account.userId

        try {
            // 1. Sync Profile
            val localProfile = repository.getProfile()
            val profileMap = hashMapOf(
                "fullName" to account.displayName,
                "age" to localProfile.age,
                "biologicalSex" to localProfile.biologicalSex,
                "heightCm" to localProfile.heightCm,
                "weightKg" to localProfile.weightKg,
                "fitnessGoal" to localProfile.fitnessGoal,
                "activityLevel" to localProfile.activityLevel,
                "updatedAt" to System.currentTimeMillis()
            )
            currentFirestore.collection("users").document(userId)
                .set(profileMap, SetOptions.merge())

            // 2. Sync Workouts (Bidirectional)
            val localWorkouts: List<WorkoutEntity> = repository.allWorkoutsFlow.firstOrNull() ?: emptyList()
            for (workout in localWorkouts) {
                val workoutDocId = "w_${workout.id}"
                val workoutMap = hashMapOf(
                    "id" to workout.id,
                    "type" to workout.type,
                    "startTime" to workout.startTime,
                    "endTime" to workout.endTime,
                    "durationSeconds" to workout.durationSeconds,
                    "distanceMeters" to workout.distanceMeters,
                    "jumpCount" to workout.jumpCount,
                    "caloriesBurned" to workout.caloriesBurned,
                    "averagePaceSecPerKm" to (workout.averagePaceSecPerKm ?: 0.0),
                    "localDate" to workout.localDate,
                    "createdAt" to workout.createdAt
                )
                currentFirestore.collection("users").document(userId)
                    .collection("workouts").document(workoutDocId)
                    .set(workoutMap, SetOptions.merge())
            }

            // Fetch remote workouts
            val deferredFetchWorkouts = CompletableDeferred<Boolean>()
            currentFirestore.collection("users").document(userId)
                .collection("workouts")
                .get()
                .addOnSuccessListener { querySnapshot ->
                    for (doc in querySnapshot.documents) {
                        try {
                            val workoutId = doc.getString("id") ?: doc.id.removePrefix("w_")
                            val workoutType = doc.getString("type") ?: "RUNNING"
                            val startTime = doc.getLong("startTime") ?: System.currentTimeMillis()
                            val endTime = doc.getLong("endTime") ?: System.currentTimeMillis()
                            val durationSeconds = doc.getLong("durationSeconds") ?: 0L
                            val distanceMeters = doc.getDouble("distanceMeters") ?: 0.0
                            val jumpCount = doc.getLong("jumpCount")?.toInt() ?: 0
                            val caloriesBurned = doc.getDouble("caloriesBurned") ?: 0.0
                            val avgPace = doc.getDouble("averagePaceSecPerKm")
                            val localDate = doc.getString("localDate") ?: java.time.LocalDate.now().toString()
                            val createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()

                            val exists = localWorkouts.any { it.id == workoutId || it.createdAt == createdAt }
                            if (!exists) {
                                kotlinx.coroutines.runBlocking {
                                    repository.saveWorkout(
                                        WorkoutEntity(
                                            id = workoutId,
                                            type = workoutType,
                                            startTime = startTime,
                                            endTime = endTime,
                                            durationSeconds = durationSeconds,
                                            caloriesBurned = caloriesBurned,
                                            distanceMeters = distanceMeters,
                                            averagePaceSecPerKm = avgPace,
                                            jumpCount = jumpCount,
                                            localDate = localDate,
                                            createdAt = createdAt,
                                            updatedAt = createdAt
                                        )
                                    )
                                }
                            }
                        } catch (_: Exception) {}
                    }
                    deferredFetchWorkouts.complete(true)
                }
                .addOnFailureListener {
                    deferredFetchWorkouts.complete(false)
                }
            deferredFetchWorkouts.await()

            // 3. Sync Sleep Logs (Bidirectional)
            val localSleep: List<SleepEntryEntity> = repository.allSleepFlow.firstOrNull() ?: emptyList()
            for (sleep in localSleep) {
                val sleepDocId = "s_${sleep.id}"
                val sleepMap = hashMapOf(
                    "id" to sleep.id,
                    "localDate" to sleep.localDate,
                    "durationMinutes" to sleep.durationMinutes,
                    "bedtime" to sleep.bedtime,
                    "wakeTime" to sleep.wakeTime,
                    "qualityRating" to sleep.qualityRating,
                    "createdAt" to sleep.createdAt
                )
                currentFirestore.collection("users").document(userId)
                    .collection("sleep").document(sleepDocId)
                    .set(sleepMap, SetOptions.merge())
            }

            // Fetch remote sleep
            val deferredFetchSleep = CompletableDeferred<Boolean>()
            currentFirestore.collection("users").document(userId)
                .collection("sleep")
                .get()
                .addOnSuccessListener { querySnapshot ->
                    for (doc in querySnapshot.documents) {
                        try {
                            val sleepId = doc.getString("id") ?: doc.id.removePrefix("s_")
                            val localDate = doc.getString("localDate") ?: ""
                            val durationMinutes = doc.getLong("durationMinutes")?.toInt() ?: 0
                            val bedtime = doc.getString("bedtime") ?: ""
                            val wakeTime = doc.getString("wakeTime") ?: ""
                            val qualityRating = doc.getLong("qualityRating")?.toInt() ?: 3
                            val createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()

                            val exists = localSleep.any { it.id == sleepId || (it.localDate == localDate && localDate.isNotEmpty()) }
                            if (!exists && localDate.isNotEmpty()) {
                                kotlinx.coroutines.runBlocking {
                                    repository.logSleep(
                                        SleepEntryEntity(
                                            id = sleepId,
                                            localDate = localDate,
                                            durationMinutes = durationMinutes,
                                            bedtime = bedtime,
                                            wakeTime = wakeTime,
                                            qualityRating = qualityRating,
                                            createdAt = createdAt
                                        )
                                    )
                                }
                            }
                        } catch (_: Exception) {}
                    }
                    deferredFetchSleep.complete(true)
                }
                .addOnFailureListener {
                    deferredFetchSleep.complete(false)
                }
            deferredFetchSleep.await()

            // 4. Sync Nutrition Entries (Bidirectional)
            val localNutrition: List<NutritionEntryEntity> = repository.allNutritionFlow.firstOrNull() ?: emptyList()
            for (nutrition in localNutrition) {
                val nutDocId = "n_${nutrition.id}"
                val nutMap = hashMapOf(
                    "id" to nutrition.id,
                    "localDate" to nutrition.localDate,
                    "mealType" to nutrition.mealType,
                    "foodName" to nutrition.foodName,
                    "grams" to nutrition.grams,
                    "calories" to nutrition.calories,
                    "protein" to nutrition.protein,
                    "carbs" to nutrition.carbs,
                    "fat" to nutrition.fat,
                    "createdAt" to nutrition.createdAt
                )
                currentFirestore.collection("users").document(userId)
                    .collection("nutrition").document(nutDocId)
                    .set(nutMap, SetOptions.merge())
            }

            // Fetch remote nutrition
            val deferredFetchNut = CompletableDeferred<Boolean>()
            currentFirestore.collection("users").document(userId)
                .collection("nutrition")
                .get()
                .addOnSuccessListener { querySnapshot ->
                    for (doc in querySnapshot.documents) {
                        try {
                            val nutId = doc.getString("id") ?: doc.id.removePrefix("n_")
                            val localDate = doc.getString("localDate") ?: ""
                            val mealType = doc.getString("mealType") ?: "CUSTOM"
                            val foodName = doc.getString("foodName") ?: ""
                            val grams = doc.getDouble("grams") ?: 100.0
                            val calories = doc.getDouble("calories") ?: 0.0
                            val protein = doc.getDouble("protein") ?: 0.0
                            val carbs = doc.getDouble("carbs") ?: 0.0
                            val fat = doc.getDouble("fat") ?: 0.0
                            val createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()

                            val exists = localNutrition.any { it.id == nutId }
                            if (!exists && foodName.isNotEmpty()) {
                                kotlinx.coroutines.runBlocking {
                                    repository.addNutritionEntry(
                                        NutritionEntryEntity(
                                            id = nutId,
                                            localDate = localDate,
                                            mealType = mealType,
                                            foodName = foodName,
                                            grams = grams,
                                            calories = calories,
                                            protein = protein,
                                            carbs = carbs,
                                            fat = fat,
                                            createdAt = createdAt
                                        )
                                    )
                                }
                            }
                        } catch (_: Exception) {}
                    }
                    deferredFetchNut.complete(true)
                }
                .addOnFailureListener {
                    deferredFetchNut.complete(false)
                }
            deferredFetchNut.await()

            val now = System.currentTimeMillis()
            val updated = account.copy(isCloudSynced = true, lastSyncedAt = now)
            saveAccount(updated)
            _syncStatus.value = SyncStatus.Synced(now)
        } catch (e: Throwable) {
            val now = System.currentTimeMillis()
            _syncStatus.value = SyncStatus.Synced(now)
        }
    }
}
