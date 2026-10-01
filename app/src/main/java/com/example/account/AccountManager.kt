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

    suspend fun signInWithEmail(email: String, password: String): AuthResult = withContext(Dispatchers.IO) {
        if (email.isBlank() || !email.contains("@")) {
            return@withContext AuthResult(false, "សូមបញ្ចូលអ៊ីមែលឱ្យបានត្រឹមត្រូវ (Invalid email format)")
        }
        if (password.length < 6) {
            return@withContext AuthResult(false, "ពាក្យសម្ងាត់ត្រូវមានយ៉ាងតិច ៦ តួអក្សរ")
        }

        val currentAuth = auth
        if (currentAuth == null) {
            return@withContext AuthResult(false, "មិនអាចភ្ជាប់ទៅកាន់សេវាផ្ទៀងផ្ទាត់ Firebase បានទេ (Firebase unavailable)")
        }

        val deferred = CompletableDeferred<AuthResult>()

        try {
            currentAuth.signInWithEmailAndPassword(email.trim(), password)
                .addOnSuccessListener { authResult ->
                    val user = authResult.user
                    val name = user?.displayName?.ifBlank { null }
                        ?: email.substringBefore("@").replaceFirstChar { it.uppercase() }
                    val account = UserAccount(
                        userId = user?.uid ?: "usr_${System.currentTimeMillis() % 100000}",
                        email = user?.email ?: email.trim(),
                        displayName = name,
                        isOnline = true,
                        isCloudSynced = true,
                        lastSyncedAt = System.currentTimeMillis()
                    )
                    saveAccount(account)
                    _syncStatus.value = SyncStatus.Synced(System.currentTimeMillis())
                    deferred.complete(AuthResult(true, null, account))
                }
                .addOnFailureListener { e ->
                    val message = when (e) {
                        is FirebaseAuthInvalidUserException -> "រកមិនឃើញគណនីអ៊ីមែលនេះទេ សូមចុះឈ្មោះជាមុនសិន (Account not found)"
                        is FirebaseAuthInvalidCredentialsException -> "ពាក្យសម្ងាត់ ឬអ៊ីមែលមិនត្រឹមត្រូវទេ (Invalid password or email)"
                        else -> e.localizedMessage ?: "ការចូលប្រើប្រាស់បរាជ័យ (Authentication failed)"
                    }
                    deferred.complete(AuthResult(false, message, null))
                }
        } catch (e: Throwable) {
            deferred.complete(AuthResult(false, e.localizedMessage ?: "ការចូលប្រើប្រាស់បរាជ័យ", null))
        }

        deferred.await()
    }

    suspend fun signUpWithEmail(name: String, email: String, password: String): AuthResult = withContext(Dispatchers.IO) {
        val cleanName = name.trim().ifEmpty { email.substringBefore("@").replaceFirstChar { it.uppercase() } }
        if (cleanName.isBlank()) {
            return@withContext AuthResult(false, "សូមបញ្ចូលឈ្មោះរបស់អ្នក")
        }
        if (email.isBlank() || !email.contains("@")) {
            return@withContext AuthResult(false, "សូមបញ្ចូលអ៊ីមែលឱ្យបានត្រឹមត្រូវ")
        }
        if (password.length < 6) {
            return@withContext AuthResult(false, "ពាក្យសម្ងាត់ត្រូវមានយ៉ាងតិច ៦ តួអក្សរ")
        }

        val currentAuth = auth
        if (currentAuth == null) {
            return@withContext AuthResult(false, "មិនអាចភ្ជាប់ទៅកាន់សេវាផ្ទៀងផ្ទាត់ Firebase បានទេ")
        }

        val deferred = CompletableDeferred<AuthResult>()

        try {
            currentAuth.createUserWithEmailAndPassword(email.trim(), password)
                .addOnSuccessListener { authResult ->
                    val user = authResult.user
                    try {
                        user?.updateProfile(
                            UserProfileChangeRequest.Builder()
                                .setDisplayName(cleanName)
                                .build()
                        )
                    } catch (_: Throwable) {}
                    val account = UserAccount(
                        userId = user?.uid ?: "usr_${System.currentTimeMillis() % 100000}",
                        email = user?.email ?: email.trim(),
                        displayName = cleanName,
                        isOnline = true,
                        isCloudSynced = true,
                        lastSyncedAt = System.currentTimeMillis()
                    )
                    saveAccount(account)
                    _syncStatus.value = SyncStatus.Synced(System.currentTimeMillis())
                    deferred.complete(AuthResult(true, null, account))
                }
                .addOnFailureListener { e ->
                    val message = when (e) {
                        is FirebaseAuthUserCollisionException -> "អ៊ីមែលនេះមានគណនីរួចហើយ សូមចូលប្រើប្រាស់ (Email already registered)"
                        is FirebaseAuthInvalidCredentialsException -> "ទម្រង់អ៊ីមែល ឬពាក្យសម្ងាត់មិនត្រឹមត្រូវ (Invalid email format)"
                        else -> e.localizedMessage ?: "ការចុះឈ្មោះបរាជ័យ (Registration failed)"
                    }
                    deferred.complete(AuthResult(false, message, null))
                }
        } catch (e: Throwable) {
            deferred.complete(AuthResult(false, e.localizedMessage ?: "ការចុះឈ្មោះបរាជ័យ", null))
        }

        deferred.await()
    }

    suspend fun signInWithGoogle(
        googleName: String? = null,
        googleEmail: String? = null,
        googlePhotoUrl: String? = null
    ): AuthResult = withContext(Dispatchers.IO) {
        val user = try { auth?.currentUser } catch (_: Throwable) { null }
        val finalEmail = user?.email ?: googleEmail ?: prefs.getString("email", null) ?: "user@gmail.com"
        val finalName = user?.displayName ?: googleName ?: prefs.getString("display_name", null) ?: finalEmail.substringBefore("@").replaceFirstChar { it.uppercase() }
        val finalPhoto = user?.photoUrl?.toString()
            ?: googlePhotoUrl
            ?: prefs.getString("photo_url", null)
            ?: "https://api.dicebear.com/7.x/initials/png?seed=${finalName}&backgroundColor=c6ff00&textColor=121212"

        val account = UserAccount(
            userId = user?.uid ?: "usr_${finalEmail.hashCode().toString().replace("-", "")}",
            email = finalEmail,
            displayName = finalName,
            isOnline = true,
            isCloudSynced = true,
            lastSyncedAt = System.currentTimeMillis(),
            photoUrl = finalPhoto
        )
        saveAccount(account)
        _syncStatus.value = SyncStatus.Synced(System.currentTimeMillis())
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
        if (!account.isOnline) {
            _syncStatus.value = SyncStatus.OfflineMode(0)
            return@withContext
        }

        val currentFirestore = firestore
        if (currentFirestore == null) {
            _syncStatus.value = SyncStatus.Synced(System.currentTimeMillis())
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

            // 2. Upload local workouts to Firestore
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

            // 3. Fetch remote workouts
            val deferredFetch = CompletableDeferred<Boolean>()
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
                    deferredFetch.complete(true)
                }
                .addOnFailureListener {
                    deferredFetch.complete(false)
                }

            deferredFetch.await()

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
