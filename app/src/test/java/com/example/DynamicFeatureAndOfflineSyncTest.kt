package com.example

import com.example.account.SyncStatus
import com.example.account.UserAccount
import com.example.updates.DynamicFeatureManager
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class DynamicFeatureAndOfflineSyncTest {

    @Test
    fun testOfflineSyncStatusModes() {
        val offline = SyncStatus.OfflineMode(localItemsCount = 3, hasInternet = false)
        assertFalse(offline.hasInternet)
        assertEquals(3, offline.localItemsCount)

        val offlineWithNet = SyncStatus.OfflineMode(localItemsCount = 0, hasInternet = true)
        assertTrue(offlineWithNet.hasInternet)

        val synced = SyncStatus.Synced(1700000000000L)
        assertEquals(1700000000000L, synced.timestamp)
    }

    @Test
    fun testUserAccountGuestOffline() {
        val guest = UserAccount.guest()
        assertFalse(guest.isOnline)
        assertFalse(guest.isCloudSynced)
        assertEquals("guest_offline", guest.userId)
        assertNull(guest.lastSyncedAt)
    }

    @Test
    fun testDynamicFeaturePackDataStructures() {
        val pack = com.example.updates.FeaturePack(
            featureCode = 2,
            featureVersion = "1.2.0",
            releaseDate = "2026-10-01",
            titleEn = "Kun Khmer Conditioning",
            titleKm = "លំហាត់គុនខ្មែរ",
            notesEn = "In-app dynamic update without APK reinstall",
            notesKm = "អាប់ដែតមុខងារថ្មីក្នុង App ផ្ទាល់",
            plans = listOf(
                com.example.updates.DynamicPlan(
                    name = "Kun Khmer Striking",
                    daysPerWeek = 4,
                    sessionMinutes = 45,
                    equipment = "BODYWEIGHT",
                    goal = "ENDURANCE",
                    exercises = listOf(
                        com.example.updates.DynamicPlanExercise(
                            name = "Shadow Boxing",
                            sets = 4,
                            reps = 30,
                            weightKg = 0.0,
                            restSeconds = 45
                        )
                    )
                )
            ),
            foods = listOf(
                com.example.updates.DynamicFoodItem(
                    name = "Cambodian Brown Jasmine Rice",
                    servingGrams = 100.0,
                    caloriesPer100g = 111.0,
                    proteinPer100g = 2.6,
                    carbsPer100g = 23.0,
                    fatPer100g = 0.9
                )
            ),
            challenges = listOf(
                com.example.updates.DynamicChallenge(
                    id = "ch_01",
                    titleEn = "100k Steps",
                    titleKm = "១០០,០០០ ជំហាន",
                    descriptionEn = "Monthly Steps",
                    descriptionKm = "ជំហានប្រចាំខែ",
                    targetValue = 100000.0,
                    unit = "steps"
                )
            )
        )

        assertEquals(2, pack.featureCode)
        assertEquals("1.2.0", pack.featureVersion)
        assertEquals(1, pack.plans.size)
        assertEquals("Kun Khmer Striking", pack.plans[0].name)
        assertEquals(1, pack.foods.size)
        assertEquals(111.0, pack.foods[0].caloriesPer100g, 0.001)
        assertEquals(1, pack.challenges.size)
    }

    @Test
    fun testInitialFeatureVersionDefaults() {
        assertEquals("1.0.0", DynamicFeatureManager.INITIAL_FEATURE_VERSION_NAME)
        assertEquals(1, DynamicFeatureManager.INITIAL_FEATURE_VERSION_CODE)
    }
}
