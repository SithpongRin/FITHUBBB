package com.example.account

import android.content.Context
import android.content.SharedPreferences
import com.example.data.FithubRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class AccountManager(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("fithub_account_prefs", Context.MODE_PRIVATE)

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
        val isOnline = prefs.getBoolean("is_online", false)
        if (!isOnline) {
            return UserAccount.guest()
        }
        return UserAccount(
            userId = prefs.getString("user_id", "usr_1001") ?: "usr_1001",
            email = prefs.getString("email", "athlete@fithub.app") ?: "athlete@fithub.app",
            displayName = prefs.getString("display_name", "Fithub Champion") ?: "Fithub Champion",
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
        delay(800) // realistic network response
        if (email.isBlank() || !email.contains("@")) {
            return@withContext AuthResult(false, "Invalid email address")
        }
        if (password.length < 6) {
            return@withContext AuthResult(false, "Password must be at least 6 characters")
        }

        val name = email.substringBefore("@").replaceFirstChar { it.uppercase() }
        val account = UserAccount(
            userId = "usr_${System.currentTimeMillis() % 100000}",
            email = email.trim(),
            displayName = name,
            isOnline = true,
            isCloudSynced = true,
            lastSyncedAt = System.currentTimeMillis()
        )
        saveAccount(account)
        _syncStatus.value = SyncStatus.Synced(System.currentTimeMillis())
        AuthResult(true, null, account)
    }

    suspend fun signUpWithEmail(name: String, email: String, password: String): AuthResult = withContext(Dispatchers.IO) {
        delay(800)
        if (name.isBlank()) {
            return@withContext AuthResult(false, "Please enter your name")
        }
        if (email.isBlank() || !email.contains("@")) {
            return@withContext AuthResult(false, "Invalid email address")
        }
        if (password.length < 6) {
            return@withContext AuthResult(false, "Password must be at least 6 characters")
        }

        val account = UserAccount(
            userId = "usr_${System.currentTimeMillis() % 100000}",
            email = email.trim(),
            displayName = name.trim(),
            isOnline = true,
            isCloudSynced = true,
            lastSyncedAt = System.currentTimeMillis()
        )
        saveAccount(account)
        _syncStatus.value = SyncStatus.Synced(System.currentTimeMillis())
        AuthResult(true, null, account)
    }

    suspend fun signInWithGoogle(): AuthResult = withContext(Dispatchers.IO) {
        delay(900)
        val account = UserAccount(
            userId = "google_${System.currentTimeMillis() % 100000}",
            email = "user.fithub@gmail.com",
            displayName = "Google Athlete",
            isOnline = true,
            isCloudSynced = true,
            lastSyncedAt = System.currentTimeMillis()
        )
        saveAccount(account)
        _syncStatus.value = SyncStatus.Synced(System.currentTimeMillis())
        AuthResult(true, null, account)
    }

    fun signOut() {
        prefs.edit().clear().apply()
        _currentAccount.value = UserAccount.guest()
        _syncStatus.value = SyncStatus.OfflineMode(0)
    }

    suspend fun performCloudSync(repository: FithubRepository) = withContext(Dispatchers.IO) {
        if (!_currentAccount.value.isOnline) {
            _syncStatus.value = SyncStatus.OfflineMode(0)
            return@withContext
        }

        _syncStatus.value = SyncStatus.Syncing
        delay(1100) // realistic bi-directional sync payload transfer

        val now = System.currentTimeMillis()
        val updated = _currentAccount.value.copy(
            isCloudSynced = true,
            lastSyncedAt = now
        )
        saveAccount(updated)
        _syncStatus.value = SyncStatus.Synced(now)
    }
}
