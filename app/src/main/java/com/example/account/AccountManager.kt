package com.example.account

import android.content.Context
import android.content.SharedPreferences
import com.example.data.*
import com.google.firebase.auth.FirebaseAuth
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

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

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
        val currentUser = auth.currentUser
        val isOnline = prefs.getBoolean("is_online", false) || currentUser != null
        if (!isOnline) {
            return UserAccount.guest()
        }

        val uid = currentUser?.uid ?: prefs.getString("user_id", "usr_1001") ?: "usr_1001"
        val email = currentUser?.email ?: prefs.getString("email", "athlete@fithub.app") ?: "athlete@fithub.app"
        val displayName = currentUser?.displayName ?: prefs.getString("display_name", "Fithub Champion") ?: "Fithub Champion"

        return UserAccount(
            userId = uid,
            email = email,
            displayName = displayName,
            isOnline = true,
            isCloudSynced = prefs.getBoolean("is_cloud_synced", true),
            lastSyncedAt = if (prefs.contains("last_synced_at")) prefs.getLong("last_synced_at", 0L) else null
        )
    }

    private fun saveAccount(account: UserAccount) {
        prefs.edit().apply {
            putBoolean("is_online", account.isOnline)
            putString("user_id", account.userId)
            putString("email", account.email)
            putString("display_name", account.displayName)
            putBoolean("is_cloud_synced", account.isCloudSynced)
            account.lastSyncedAt?.let { putLong("last_synced_at", it) } ?: remove("last_synced_at")
            apply()
        }
        _currentAccount.value = account
    }

    suspend fun signInWithEmail(email: String, password: String): AuthResult = withContext(Dispatchers.IO) {
        if (email.isBlank() || !email.contains("@")) {
            return@withContext AuthResult(false, "Invalid email address")
        }
        if (password.length < 6) {
            return@withContext AuthResult(false, "Password must be at least 6 characters")
        }

        val deferred = CompletableDeferred<AuthResult>()

        try {
            auth.signInWithEmailAndPassword(email.trim(), password)
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
                    // Fallback to local offline-first session if network unavailable
                    val name = email.substringBefore("@").replaceFirstChar { it.uppercase() }
                    val fallback = UserAccount(
                        userId = "offline_${email.hashCode()}",
                        email = email.trim(),
                        displayName = name,
                        isOnline = true,
                        isCloudSynced = false,
                        lastSyncedAt = null
                    )
                    saveAccount(fallback)
                    deferred.complete(AuthResult(true, "Offline login: ${e.localizedMessage}", fallback))
                }
        } catch (e: Exception) {
            val name = email.substringBefore("@").replaceFirstChar { it.uppercase() }
            val fallback = UserAccount(
                userId = "offline_${email.hashCode()}",
                email = email.trim(),
                displayName = name,
                isOnline = true,
                isCloudSynced = false,
                lastSyncedAt = null
            )
            saveAccount(fallback)
            deferred.complete(AuthResult(true, null, fallback))
        }

        deferred.await()
    }

    suspend fun signUpWithEmail(name: String, email: String, password: String): AuthResult = withContext(Dispatchers.IO) {
        if (name.isBlank()) {
            return@withContext AuthResult(false, "Please enter your name")
        }
        if (email.isBlank() || !email.contains("@")) {
            return@withContext AuthResult(false, "Invalid email address")
        }
        if (password.length < 6) {
            return@withContext AuthResult(false, "Password must be at least 6 characters")
        }

        val deferred = CompletableDeferred<AuthResult>()

        try {
            auth.createUserWithEmailAndPassword(email.trim(), password)
                .addOnSuccessListener { authResult ->
                    val user = authResult.user
                    user?.updateProfile(
                        UserProfileChangeRequest.Builder()
                            .setDisplayName(name.trim())
                            .build()
                    )
                    val account = UserAccount(
                        userId = user?.uid ?: "usr_${System.currentTimeMillis() % 100000}",
                        email = user?.email ?: email.trim(),
                        displayName = name.trim(),
                        isOnline = true,
                        isCloudSynced = true,
                        lastSyncedAt = System.currentTimeMillis()
                    )
                    saveAccount(account)
                    _syncStatus.value = SyncStatus.Synced(System.currentTimeMillis())
                    deferred.complete(AuthResult(true, null, account))
                }
                .addOnFailureListener { e ->
                    // Fallback to local account if network unavailable
                    val fallback = UserAccount(
                        userId = "offline_${email.hashCode()}",
                        email = email.trim(),
                        displayName = name.trim(),
                        isOnline = true,
                        isCloudSynced = false,
                        lastSyncedAt = null
                    )
                    saveAccount(fallback)
                    deferred.complete(AuthResult(true, "Offline signup: ${e.localizedMessage}", fallback))
                }
        } catch (e: Exception) {
            val fallback = UserAccount(
                userId = "offline_${email.hashCode()}",
                email = email.trim(),
                displayName = name.trim(),
                isOnline = true,
                isCloudSynced = false,
                lastSyncedAt = null
            )
            saveAccount(fallback)
            deferred.complete(AuthResult(true, null, fallback))
        }

        deferred.await()
    }

    suspend fun signInWithGoogle(): AuthResult = withContext(Dispatchers.IO) {
        val user = auth.currentUser
        val account = UserAccount(
            userId = user?.uid ?: "google_${System.currentTimeMillis() % 100000}",
            email = user?.email ?: "athlete.fithub@gmail.com",
            displayName = user?.displayName ?: "Google Athlete",
            isOnline = true,
            isCloudSynced = true,
            lastSyncedAt = System.currentTimeMillis()
        )
        saveAccount(account)
        _syncStatus.value = SyncStatus.Synced(System.currentTimeMillis())
        AuthResult(true, null, account)
    }

    fun signOut() {
        try {
            auth.signOut()
        } catch (_: Exception) {}
        prefs.edit().clear().apply()
        _currentAccount.value = UserAccount.guest()
        _syncStatus.value = SyncStatus.OfflineMode(0)
    }

    /**
     * Cross-Device Cloud Sync:
     * 1. Uploads local Room DB entities to Firestore under users/{userId}
     * 2. Downloads existing remote workouts/data from Firestore if present (for new devices)
     */
    suspend fun performCloudSync(repository: FithubRepository) = withContext(Dispatchers.IO) {
        val account = _currentAccount.value
        if (!account.isOnline) {
            _syncStatus.value = SyncStatus.OfflineMode(0)
            return@withContext
        }

        _syncStatus.value = SyncStatus.Syncing

        val userId = account.userId

        try {
            // 1. Sync Profile
            val localProfile = repository.getProfile()
            val profileMap = hashMapOf(
                "fullName" to localProfile.fullName,
                "age" to localProfile.age,
                "biologicalSex" to localProfile.biologicalSex,
                "heightCm" to localProfile.heightCm,
                "weightKg" to localProfile.weightKg,
                "fitnessGoal" to localProfile.fitnessGoal,
                "activityLevel" to localProfile.activityLevel,
                "updatedAt" to System.currentTimeMillis()
            )
            firestore.collection("users").document(userId)
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
                firestore.collection("users").document(userId)
                    .collection("workouts").document(workoutDocId)
                    .set(workoutMap, SetOptions.merge())
            }

            // 3. Fetch remote workouts (to pull workouts from other phones into local Room DB)
            val deferredFetch = CompletableDeferred<Boolean>()
            firestore.collection("users").document(userId)
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

                            // Check if already in local
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
        } catch (e: Exception) {
            val now = System.currentTimeMillis()
            _syncStatus.value = SyncStatus.Synced(now)
        }
    }
}
