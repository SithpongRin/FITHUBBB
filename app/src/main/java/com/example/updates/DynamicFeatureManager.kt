package com.example.updates

import android.content.Context
import com.example.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

data class DynamicPlanExercise(
    val name: String,
    val sets: Int,
    val reps: Int,
    val weightKg: Double,
    val restSeconds: Int,
    val dayIndex: Int = 0,
    val orderIndex: Int = 0,
    val isBodyweight: Boolean = true
)

data class DynamicPlan(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val daysPerWeek: Int,
    val sessionMinutes: Int,
    val equipment: String, // FULL_GYM, DUMBBELLS_ONLY, BODYWEIGHT
    val goal: String, // STRENGTH, ENDURANCE, GENERAL_FITNESS
    val exercises: List<DynamicPlanExercise>
)

data class DynamicFoodItem(
    val name: String,
    val servingGrams: Double,
    val caloriesPer100g: Double,
    val proteinPer100g: Double,
    val carbsPer100g: Double,
    val fatPer100g: Double
)

data class DynamicChallenge(
    val id: String,
    val titleEn: String,
    val titleKm: String,
    val descriptionEn: String,
    val descriptionKm: String,
    val targetValue: Double,
    val unit: String
)

data class FeaturePack(
    val featureCode: Int,
    val featureVersion: String,
    val releaseDate: String,
    val titleEn: String,
    val titleKm: String,
    val notesEn: String,
    val notesKm: String,
    val plans: List<DynamicPlan>,
    val foods: List<DynamicFoodItem>,
    val challenges: List<DynamicChallenge>
)

sealed class DynamicFeatureStatus {
    object Idle : DynamicFeatureStatus()
    object Checking : DynamicFeatureStatus()
    data class UpToDate(val currentVersion: String) : DynamicFeatureStatus()
    data class UpdateAvailable(val pack: FeaturePack) : DynamicFeatureStatus()
    data class Applying(val progressPercent: Int, val stepDescription: String) : DynamicFeatureStatus()
    data class Applied(val pack: FeaturePack, val plansAdded: Int, val foodsAdded: Int) : DynamicFeatureStatus()
    data class Error(val message: String) : DynamicFeatureStatus()
}

object DynamicFeatureManager {

    private const val PREFS_NAME = "fithub_dynamic_features"
    private const val KEY_FEATURE_VERSION_CODE = "installed_feature_code"
    private const val KEY_FEATURE_VERSION_NAME = "installed_feature_name"

    const val INITIAL_FEATURE_VERSION_NAME = "1.0.0"
    const val INITIAL_FEATURE_VERSION_CODE = 1

    private val _featureStatus = MutableStateFlow<DynamicFeatureStatus>(DynamicFeatureStatus.Idle)
    val featureStatus: StateFlow<DynamicFeatureStatus> = _featureStatus.asStateFlow()

    // Configurable endpoint for checking dynamic features (Over-The-Air without APK)
    var customFeatureUrl: String = "https://raw.githubusercontent.com/SithpongRin/FITHUBBB/main/features.json"

    fun getInstalledFeatureVersion(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_FEATURE_VERSION_NAME, INITIAL_FEATURE_VERSION_NAME) ?: INITIAL_FEATURE_VERSION_NAME
    }

    fun getInstalledFeatureCode(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_FEATURE_VERSION_CODE, INITIAL_FEATURE_VERSION_CODE)
    }

    suspend fun checkForFeatureUpdates(
        context: Context,
        forceSimulateAvailable: Boolean = false
    ) = withContext(Dispatchers.IO) {
        _featureStatus.value = DynamicFeatureStatus.Checking
        delay(900)

        val currentCode = getInstalledFeatureCode(context)

        if (forceSimulateAvailable) {
            val samplePack = getSampleOtaFeaturePack(currentCode + 1)
            _featureStatus.value = DynamicFeatureStatus.UpdateAvailable(samplePack)
            return@withContext
        }

        // Try downloading remote features.json if network is reachable
        if (customFeatureUrl.isNotBlank() && customFeatureUrl.startsWith("http")) {
            try {
                val checkUrl = if (customFeatureUrl.contains("?")) "$customFeatureUrl&_t=${System.currentTimeMillis()}" else "$customFeatureUrl?_t=${System.currentTimeMillis()}"
                val url = URL(checkUrl)
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 8000
                conn.readTimeout = 8000
                conn.useCaches = false
                if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                    val reader = BufferedReader(InputStreamReader(conn.inputStream))
                    val jsonStr = reader.readText()
                    reader.close()
                    val json = JSONObject(jsonStr)

                    val remoteCode = json.optInt("featureCode", 1)
                    val remoteVersion = json.optString("featureVersion", "1.0.0")

                    if (remoteCode > currentCode) {
                        val pack = parseFeaturePack(json)
                        _featureStatus.value = DynamicFeatureStatus.UpdateAvailable(pack)
                        return@withContext
                    }
                }
            } catch (_: Exception) {
                // Network unavailable or server not yet deployed: fallback to sample pack if uninstalled
            }
        }

        // If current installed code is at initial version, offer the Kun Khmer & HIIT feature pack
        if (currentCode < 2) {
            val samplePack = getSampleOtaFeaturePack(2)
            _featureStatus.value = DynamicFeatureStatus.UpdateAvailable(samplePack)
        } else {
            _featureStatus.value = DynamicFeatureStatus.UpToDate(getInstalledFeatureVersion(context))
        }
    }

    /**
     * Applies the feature update directly into Room Database.
     * Zero APK installation, zero permissions, runs 100% in-app immediately!
     */
    suspend fun applyFeatureUpdate(
        context: Context,
        repository: FithubRepository,
        pack: FeaturePack
    ) = withContext(Dispatchers.IO) {
        try {
            _featureStatus.value = DynamicFeatureStatus.Applying(
                progressPercent = 20,
                stepDescription = "ទាញយកកញ្ចប់មុខងារថ្មី (Downloading Feature Payload)..."
            )
            delay(400)

            _featureStatus.value = DynamicFeatureStatus.Applying(
                progressPercent = 50,
                stepDescription = "បញ្ចូលគម្រោងហាត់ និងលំហាត់ថ្មី (Installing Dynamic Workout Plans)..."
            )

            var plansCount = 0
            for (plan in pack.plans) {
                val planEntity = WorkoutPlanEntity(
                    id = plan.id,
                    name = plan.name,
                    daysPerWeek = plan.daysPerWeek,
                    sessionMinutes = plan.sessionMinutes,
                    equipment = plan.equipment,
                    goal = plan.goal,
                    createdAt = System.currentTimeMillis()
                )

                val exercises = plan.exercises.mapIndexed { idx, ex ->
                    PlanExerciseEntity(
                        id = UUID.randomUUID().toString(),
                        planId = plan.id,
                        dayIndex = ex.dayIndex,
                        orderIndex = ex.orderIndex.takeIf { it > 0 } ?: idx,
                        name = ex.name,
                        sets = ex.sets,
                        reps = ex.reps,
                        weightKg = ex.weightKg,
                        restSeconds = ex.restSeconds,
                        isBodyweight = ex.isBodyweight
                    )
                }

                repository.savePlanWithExercises(planEntity, exercises)
                plansCount++
            }

            _featureStatus.value = DynamicFeatureStatus.Applying(
                progressPercent = 80,
                stepDescription = "បន្ថែមទិន្នន័យអាហារូបត្ថម្ភថ្មី (Updating Nutrition Database)..."
            )
            delay(300)

            var foodsCount = 0
            for (food in pack.foods) {
                val foodEntity = FoodEntity(
                    id = UUID.randomUUID().toString(),
                    name = food.name,
                    servingGrams = food.servingGrams,
                    caloriesPer100g = food.caloriesPer100g,
                    proteinPer100g = food.proteinPer100g,
                    carbsPer100g = food.carbsPer100g,
                    fatPer100g = food.fatPer100g,
                    isCustom = false
                )
                repository.addCustomFood(foodEntity)
                foodsCount++
            }

            // Save new feature version to preferences
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putInt(KEY_FEATURE_VERSION_CODE, pack.featureCode)
                .putString(KEY_FEATURE_VERSION_NAME, pack.featureVersion)
                .apply()

            _featureStatus.value = DynamicFeatureStatus.Applying(
                progressPercent = 100,
                stepDescription = "បានដំឡើងមុខងារថ្មីដោយជោគជ័យ! (Complete)"
            )
            delay(300)

            _featureStatus.value = DynamicFeatureStatus.Applied(
                pack = pack,
                plansAdded = plansCount,
                foodsAdded = foodsCount
            )
        } catch (e: Exception) {
            _featureStatus.value = DynamicFeatureStatus.Error(
                e.message ?: "Failed to apply dynamic in-app feature update"
            )
        }
    }

    fun dismissFeatureUpdate() {
        _featureStatus.value = DynamicFeatureStatus.Idle
    }

    private fun parseFeaturePack(json: JSONObject): FeaturePack {
        val featureCode = json.optInt("featureCode", 2)
        val featureVersion = json.optString("featureVersion", "1.1.0")
        val releaseDate = json.optString("releaseDate", "2026-10-01")
        val titleEn = json.optString("titleEn", "New Fitness Programs")
        val titleKm = json.optString("titleKm", "កម្មវិធីហាត់ប្រាណថ្មីៗ")
        val notesEn = json.optString("descriptionEn", "Over-the-air in-app feature update.")
        val notesKm = json.optString("descriptionKm", "អាប់ដែតមុខងារថ្មីក្នុង App ផ្ទាល់។")

        val plansList = mutableListOf<DynamicPlan>()
        val plansArray = json.optJSONArray("newPlans") ?: JSONArray()
        for (i in 0 until plansArray.length()) {
            val p = plansArray.getJSONObject(i)
            val pId = p.optString("id", UUID.randomUUID().toString())
            val pName = p.optString("name", "New Routine")
            val pDays = p.optInt("daysPerWeek", 3)
            val pMin = p.optInt("sessionMinutes", 30)
            val pEquip = p.optString("equipment", "BODYWEIGHT")
            val pGoal = p.optString("goal", "GENERAL_FITNESS")

            val exList = mutableListOf<DynamicPlanExercise>()
            val exArray = p.optJSONArray("exercises") ?: JSONArray()
            for (j in 0 until exArray.length()) {
                val ex = exArray.getJSONObject(j)
                exList.add(
                    DynamicPlanExercise(
                        name = ex.optString("name", "Exercise"),
                        sets = ex.optInt("sets", 3),
                        reps = ex.optInt("reps", 10),
                        weightKg = ex.optDouble("weightKg", 0.0),
                        restSeconds = ex.optInt("restSeconds", 60),
                        dayIndex = ex.optInt("dayIndex", 0),
                        orderIndex = ex.optInt("orderIndex", j),
                        isBodyweight = ex.optBoolean("isBodyweight", true)
                    )
                )
            }
            plansList.add(DynamicPlan(pId, pName, pDays, pMin, pEquip, pGoal, exList))
        }

        val foodsList = mutableListOf<DynamicFoodItem>()
        val foodsArray = json.optJSONArray("newFoods") ?: JSONArray()
        for (i in 0 until foodsArray.length()) {
            val f = foodsArray.getJSONObject(i)
            foodsList.add(
                DynamicFoodItem(
                    name = f.optString("name", "Healthy Food"),
                    servingGrams = f.optDouble("servingGrams", 100.0),
                    caloriesPer100g = f.optDouble("caloriesPer100g", 100.0),
                    proteinPer100g = f.optDouble("proteinPer100g", 10.0),
                    carbsPer100g = f.optDouble("carbsPer100g", 15.0),
                    fatPer100g = f.optDouble("fatPer100g", 2.0)
                )
            )
        }

        val challengeList = mutableListOf<DynamicChallenge>()
        val chArray = json.optJSONArray("newChallenges") ?: JSONArray()
        for (i in 0 until chArray.length()) {
            val c = chArray.getJSONObject(i)
            challengeList.add(
                DynamicChallenge(
                    id = c.optString("id", "ch_$i"),
                    titleEn = c.optString("titleEn", "Challenge"),
                    titleKm = c.optString("titleKm", "ការប្រកួតប្រជែង"),
                    descriptionEn = c.optString("descriptionEn", ""),
                    descriptionKm = c.optString("descriptionKm", ""),
                    targetValue = c.optDouble("targetValue", 100.0),
                    unit = c.optString("unit", "units")
                )
            )
        }

        return FeaturePack(
            featureCode = featureCode,
            featureVersion = featureVersion,
            releaseDate = releaseDate,
            titleEn = titleEn,
            titleKm = titleKm,
            notesEn = notesEn,
            notesKm = notesKm,
            plans = plansList,
            foods = foodsList,
            challenges = challengeList
        )
    }

    private fun getSampleOtaFeaturePack(newCode: Int): FeaturePack {
        return FeaturePack(
            featureCode = newCode,
            featureVersion = "1.2.0",
            releaseDate = "2026-10-01",
            titleEn = "Kun Khmer Conditioning & Metabolic HIIT Shredder",
            titleKm = "លំហាត់គុនខ្មែរ & កម្មវិធីដុតខ្លាញ់ HIIT កម្រិតខ្ពស់",
            notesEn = "Over-the-air in-app feature update with authentic Kun Khmer endurance routines, Cambodian healthy whole foods, and high-intensity calisthenics.",
            notesKm = "អាប់ដែតមុខងារថ្មីក្នុង App ផ្ទាល់៖ កម្មវិធីហាត់ក្បាច់គុនខ្មែរ (Kun Khmer) បញ្ជីអាហារសុខភាពខ្មែរ និងលំហាត់កម្លាំងថ្មីៗ ដោយមិនចាំបាច់ install APK ឡើងវិញ។",
            plans = listOf(
                DynamicPlan(
                    id = "plan_kun_khmer_ota",
                    name = "Kun Khmer Conditioning (លំហាត់គុនខ្មែរ)",
                    daysPerWeek = 4,
                    sessionMinutes = 45,
                    equipment = "BODYWEIGHT",
                    goal = "ENDURANCE",
                    exercises = listOf(
                        DynamicPlanExercise(name = "Shadow Boxing & Knee Drives", sets = 4, reps = 30, weightKg = 0.0, restSeconds = 45, dayIndex = 0, orderIndex = 0),
                        DynamicPlanExercise(name = "Explosive Jump Tuck Squats", sets = 4, reps = 15, weightKg = 0.0, restSeconds = 60, dayIndex = 0, orderIndex = 1),
                        DynamicPlanExercise(name = "Heavy Bag / High Kick Conditioning", sets = 5, reps = 20, weightKg = 0.0, restSeconds = 45, dayIndex = 0, orderIndex = 2),
                        DynamicPlanExercise(name = "Plank Knee Tucks & Core Burner", sets = 3, reps = 20, weightKg = 0.0, restSeconds = 30, dayIndex = 0, orderIndex = 3)
                    )
                ),
                DynamicPlan(
                    id = "plan_hiit_shred_ota",
                    name = "HIIT Metabolic Shredder (ដុតជាតិខ្លាញ់លឿន)",
                    daysPerWeek = 3,
                    sessionMinutes = 35,
                    equipment = "BODYWEIGHT",
                    goal = "GENERAL_FITNESS",
                    exercises = listOf(
                        DynamicPlanExercise(name = "Burpee Broad Jumps", sets = 4, reps = 12, weightKg = 0.0, restSeconds = 45, dayIndex = 0, orderIndex = 0),
                        DynamicPlanExercise(name = "Mountain Climber Sprints", sets = 4, reps = 40, weightKg = 0.0, restSeconds = 30, dayIndex = 0, orderIndex = 1),
                        DynamicPlanExercise(name = "Speed Jumping Rope", sets = 4, reps = 100, weightKg = 0.0, restSeconds = 45, dayIndex = 0, orderIndex = 2)
                    )
                )
            ),
            foods = listOf(
                DynamicFoodItem(name = "Cambodian Brown Jasmine Rice (បាយសំរូប)", servingGrams = 100.0, caloriesPer100g = 111.0, proteinPer100g = 2.6, carbsPer100g = 23.0, fatPer100g = 0.9),
                DynamicFoodItem(name = "Grilled Snakehead Fish (ត្រីរ៉ស់អាំង)", servingGrams = 100.0, caloriesPer100g = 98.0, proteinPer100g = 20.5, carbsPer100g = 0.0, fatPer100g = 1.2),
                DynamicFoodItem(name = "Steamed Bok Choy (ស្ពៃបូកគោចំហុយ)", servingGrams = 100.0, caloriesPer100g = 13.0, proteinPer100g = 1.5, carbsPer100g = 2.2, fatPer100g = 0.2),
                DynamicFoodItem(name = "Boiled Morning Glory / Water Spinach (ត្រកួនស្រុះ)", servingGrams = 100.0, caloriesPer100g = 19.0, proteinPer100g = 2.6, carbsPer100g = 3.1, fatPer100g = 0.2)
            ),
            challenges = listOf(
                DynamicChallenge(
                    id = "ch_100k_steps",
                    titleEn = "100,000 Monthly Steps",
                    titleKm = "ប្រកួតដើរ ១០០,០០០ ជំហានប្រចាំខែ",
                    descriptionEn = "Walk 100k steps this month for cardiovascular conditioning.",
                    descriptionKm = "ដើរឱ្យបាន ១០០,០០០ ជំហានក្នុងខែនេះ ដើម្បីសុខភាពបេះដូងរឹងមាំ។",
                    targetValue = 100000.0,
                    unit = "steps"
                ),
                DynamicChallenge(
                    id = "ch_50k_run",
                    titleEn = "50km Cumulative Sprint",
                    titleKm = "ប្រកួតរត់សរុប ៥០ គីឡូម៉ែត្រ",
                    descriptionEn = "Hit 50km total running distance.",
                    descriptionKm = "រត់ឱ្យបានចម្ងាយសរុប ៥០ គីឡូម៉ែត្រ។",
                    targetValue = 50.0,
                    unit = "km"
                )
            )
        )
    }
}
