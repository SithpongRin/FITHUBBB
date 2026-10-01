package com.example.account

data class UserAccount(
    val userId: String,
    val email: String,
    val displayName: String,
    val isOnline: Boolean,
    val isCloudSynced: Boolean,
    val lastSyncedAt: Long?,
    val photoUrl: String? = null
) {
    companion object {
        fun guest(): UserAccount = UserAccount(
            userId = "guest_offline",
            email = "offline@fithub.local",
            displayName = "Offline Athlete",
            isOnline = false,
            isCloudSynced = false,
            lastSyncedAt = null
        )
    }
}

sealed class SyncStatus {
    object Idle : SyncStatus()
    object Syncing : SyncStatus()
    data class Synced(val timestamp: Long) : SyncStatus()
    data class OfflineMode(val localItemsCount: Int) : SyncStatus()
    data class Error(val message: String) : SyncStatus()
}

data class AuthResult(
    val isSuccess: Boolean,
    val errorMessage: String? = null,
    val account: UserAccount? = null
)
