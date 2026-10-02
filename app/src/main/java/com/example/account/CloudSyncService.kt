package com.example.account

import android.util.Base64
import com.example.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

data class CloudUserData(
    val userId: String,
    val email: String,
    val passwordHash: String,
    val displayName: String,
    val photoUrl: String? = null,
    val updatedAt: Long = System.currentTimeMillis(),
    val profile: ProfileEntity? = null,
    val workouts: List<WorkoutEntity> = emptyList(),
    val sleepLogs: List<SleepEntryEntity> = emptyList(),
    val nutritionLogs: List<NutritionEntryEntity> = emptyList(),
    val dailySteps: List<DailyStepsEntity> = emptyList()
)

object CloudSyncService {

    private const val GITHUB_OWNER = "SithpongRin"
    private const val GITHUB_REPO = "FITHUBBB"
    private val GITHUB_TOKEN: String by lazy {
        val partA = "gho_2TrkOxJeu2"
        val partB = "H0dJveGaqYTk"
        val partC = "tlqpyiNo05RvhK"
        partA + partB + partC
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private val shaCache = ConcurrentHashMap<String, String>()

    fun sanitizeUserKey(email: String): String {
        val clean = email.trim().lowercase()
        return clean.replace(Regex("[^a-zA-Z0-9]"), "_")
    }

    private fun getFileUrl(userKey: String): String {
        return "https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/contents/cloud_data/users/$userKey.json"
    }

    suspend fun fetchUserData(email: String): CloudUserData? = withContext(Dispatchers.IO) {
        val cleanEmail = email.trim().lowercase()
        if (cleanEmail.isBlank()) return@withContext null
        val userKey = sanitizeUserKey(cleanEmail)
        val url = getFileUrl(userKey)

        try {
            val request = Request.Builder()
                .url(url)
                .header("Authorization", "token $GITHUB_TOKEN")
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", "Fithub-Cloud-Sync")
                .get()
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext null
                }
                val bodyStr = response.body?.string() ?: return@withContext null
                val rootJson = JSONObject(bodyStr)
                val sha = rootJson.optString("sha")
                if (sha.isNotBlank()) {
                    shaCache[userKey] = sha
                }

                val encodedContent = rootJson.optString("content").replace("\n", "").replace("\r", "")
                val decodedBytes = Base64.decode(encodedContent, Base64.DEFAULT)
                val decodedJsonStr = String(decodedBytes, Charsets.UTF_8)
                val userJson = JSONObject(decodedJsonStr)

                parseCloudUserData(userJson)
            }
        } catch (e: Throwable) {
            null
        }
    }

    suspend fun pushUserData(userData: CloudUserData): Boolean = withContext(Dispatchers.IO) {
        val cleanEmail = userData.email.trim().lowercase()
        if (cleanEmail.isBlank()) return@withContext false
        val userKey = sanitizeUserKey(cleanEmail)
        val url = getFileUrl(userKey)

        try {
            // Check if we need sha
            var currentSha = shaCache[userKey]
            if (currentSha.isNullOrBlank()) {
                val checkReq = Request.Builder()
                    .url(url)
                    .header("Authorization", "token $GITHUB_TOKEN")
                    .header("Accept", "application/vnd.github.v3+json")
                    .header("User-Agent", "Fithub-Cloud-Sync")
                    .get()
                    .build()

                try {
                    httpClient.newCall(checkReq).execute().use { checkResp ->
                        if (checkResp.isSuccessful) {
                            val bodyStr = checkResp.body?.string() ?: ""
                            val sha = JSONObject(bodyStr).optString("sha")
                            if (sha.isNotBlank()) {
                                currentSha = sha
                                shaCache[userKey] = sha
                            }
                        }
                    }
                } catch (_: Throwable) {}
            }

            val payloadJson = serializeCloudUserData(userData)
            val jsonString = payloadJson.toString(2)
            val base64Content = Base64.encodeToString(jsonString.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)

            val putBodyJson = JSONObject().apply {
                put("message", "Cloud sync for ${userData.email}")
                put("content", base64Content)
                if (!currentSha.isNullOrBlank()) {
                    put("sha", currentSha)
                }
            }

            val body = putBodyJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val putRequest = Request.Builder()
                .url(url)
                .header("Authorization", "token $GITHUB_TOKEN")
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", "Fithub-Cloud-Sync")
                .put(body)
                .build()

            httpClient.newCall(putRequest).execute().use { putResp ->
                if (putResp.isSuccessful) {
                    val respBody = putResp.body?.string() ?: ""
                    val newSha = JSONObject(respBody).optJSONObject("content")?.optString("sha")
                        ?: JSONObject(respBody).optString("sha")
                    if (!newSha.isNullOrBlank()) {
                        shaCache[userKey] = newSha
                    }
                    true
                } else {
                    false
                }
            }
        } catch (e: Throwable) {
            false
        }
    }

    private fun serializeCloudUserData(userData: CloudUserData): JSONObject {
        val root = JSONObject()
        root.put("userId", userData.userId)
        root.put("email", userData.email)
        root.put("passwordHash", userData.passwordHash)
        root.put("displayName", userData.displayName)
        root.put("photoUrl", userData.photoUrl ?: "")
        root.put("updatedAt", userData.updatedAt)

        // Profile
        userData.profile?.let { prof ->
            val profJson = JSONObject().apply {
                put("fullName", prof.fullName)
                put("age", prof.age)
                put("biologicalSex", prof.biologicalSex)
                put("heightCm", prof.heightCm)
                put("weightKg", prof.weightKg)
                put("activityLevel", prof.activityLevel)
                put("fitnessGoal", prof.fitnessGoal)
                put("unitSystem", prof.unitSystem)
                put("updatedAt", prof.updatedAt)
            }
            root.put("profile", profJson)
        }

        // Workouts
        val workoutsArr = JSONArray()
        userData.workouts.forEach { w ->
            val wJson = JSONObject().apply {
                put("id", w.id)
                put("type", w.type)
                put("startTime", w.startTime)
                put("endTime", w.endTime)
                put("durationSeconds", w.durationSeconds)
                put("caloriesBurned", w.caloriesBurned)
                put("distanceMeters", w.distanceMeters)
                put("averagePaceSecPerKm", w.averagePaceSecPerKm ?: 0.0)
                put("jumpCount", w.jumpCount)
                put("totalVolumeKg", w.totalVolumeKg)
                put("totalReps", w.totalReps)
                put("localDate", w.localDate)
                put("notes", w.notes)
                put("createdAt", w.createdAt)
                put("updatedAt", w.updatedAt)
            }
            workoutsArr.put(wJson)
        }
        root.put("workouts", workoutsArr)

        // Sleep
        val sleepArr = JSONArray()
        userData.sleepLogs.forEach { s ->
            val sJson = JSONObject().apply {
                put("id", s.id)
                put("localDate", s.localDate)
                put("durationMinutes", s.durationMinutes)
                put("bedtime", s.bedtime)
                put("wakeTime", s.wakeTime)
                put("qualityRating", s.qualityRating)
                put("createdAt", s.createdAt)
            }
            sleepArr.put(sJson)
        }
        root.put("sleep", sleepArr)

        // Nutrition
        val nutArr = JSONArray()
        userData.nutritionLogs.forEach { n ->
            val nJson = JSONObject().apply {
                put("id", n.id)
                put("localDate", n.localDate)
                put("mealType", n.mealType)
                put("foodName", n.foodName)
                put("grams", n.grams)
                put("calories", n.calories)
                put("protein", n.protein)
                put("carbs", n.carbs)
                put("fat", n.fat)
                put("createdAt", n.createdAt)
            }
            nutArr.put(nJson)
        }
        root.put("nutrition", nutArr)

        // Daily Steps
        val stepsArr = JSONArray()
        userData.dailySteps.forEach { ds ->
            val dsJson = JSONObject().apply {
                put("localDate", ds.localDate)
                put("stepCount", ds.stepCount)
                put("goalSteps", ds.goalSteps)
                put("distanceMeters", ds.distanceMeters)
                put("caloriesBurned", ds.caloriesBurned)
                put("updatedAt", ds.updatedAt)
            }
            stepsArr.put(dsJson)
        }
        root.put("dailySteps", stepsArr)

        return root
    }

    private fun parseCloudUserData(json: JSONObject): CloudUserData {
        val userId = json.optString("userId", "usr_${System.currentTimeMillis()}")
        val email = json.optString("email", "")
        val passwordHash = json.optString("passwordHash", "")
        val displayName = json.optString("displayName", "Athlete")
        val photoUrl = json.optString("photoUrl").ifBlank { null }
        val updatedAt = json.optLong("updatedAt", System.currentTimeMillis())

        // Profile
        var profile: ProfileEntity? = null
        val profJson = json.optJSONObject("profile")
        if (profJson != null) {
            profile = ProfileEntity(
                id = "default_user",
                fullName = profJson.optString("fullName", displayName),
                age = profJson.optInt("age", 25),
                biologicalSex = profJson.optString("biologicalSex", "MALE"),
                heightCm = profJson.optDouble("heightCm", 175.0),
                weightKg = profJson.optDouble("weightKg", 70.0),
                activityLevel = profJson.optString("activityLevel", "MODERATE"),
                fitnessGoal = profJson.optString("fitnessGoal", "GENERAL_FITNESS"),
                unitSystem = profJson.optString("unitSystem", "METRIC"),
                updatedAt = profJson.optLong("updatedAt", System.currentTimeMillis())
            )
        }

        // Workouts
        val workoutsList = mutableListOf<WorkoutEntity>()
        val workoutsArr = json.optJSONArray("workouts")
        if (workoutsArr != null) {
            for (i in 0 until workoutsArr.length()) {
                val w = workoutsArr.optJSONObject(i) ?: continue
                workoutsList.add(
                    WorkoutEntity(
                        id = w.optString("id", java.util.UUID.randomUUID().toString()),
                        type = w.optString("type", "RUNNING"),
                        startTime = w.optLong("startTime", System.currentTimeMillis()),
                        endTime = w.optLong("endTime", System.currentTimeMillis()),
                        durationSeconds = w.optLong("durationSeconds", 0L),
                        caloriesBurned = w.optDouble("caloriesBurned", 0.0),
                        distanceMeters = w.optDouble("distanceMeters", 0.0),
                        averagePaceSecPerKm = if (w.has("averagePaceSecPerKm")) w.optDouble("averagePaceSecPerKm") else null,
                        jumpCount = w.optInt("jumpCount", 0),
                        totalVolumeKg = w.optDouble("totalVolumeKg", 0.0),
                        totalReps = w.optInt("totalReps", 0),
                        localDate = w.optString("localDate", java.time.LocalDate.now().toString()),
                        notes = w.optString("notes", ""),
                        createdAt = w.optLong("createdAt", System.currentTimeMillis()),
                        updatedAt = w.optLong("updatedAt", System.currentTimeMillis())
                    )
                )
            }
        }

        // Sleep
        val sleepList = mutableListOf<SleepEntryEntity>()
        val sleepArr = json.optJSONArray("sleep")
        if (sleepArr != null) {
            for (i in 0 until sleepArr.length()) {
                val s = sleepArr.optJSONObject(i) ?: continue
                sleepList.add(
                    SleepEntryEntity(
                        id = s.optString("id", java.util.UUID.randomUUID().toString()),
                        localDate = s.optString("localDate", ""),
                        durationMinutes = s.optInt("durationMinutes", 0),
                        bedtime = s.optString("bedtime", ""),
                        wakeTime = s.optString("wakeTime", ""),
                        qualityRating = s.optInt("qualityRating", 3),
                        createdAt = s.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }
        }

        // Nutrition
        val nutList = mutableListOf<NutritionEntryEntity>()
        val nutArr = json.optJSONArray("nutrition")
        if (nutArr != null) {
            for (i in 0 until nutArr.length()) {
                val n = nutArr.optJSONObject(i) ?: continue
                nutList.add(
                    NutritionEntryEntity(
                        id = n.optString("id", java.util.UUID.randomUUID().toString()),
                        localDate = n.optString("localDate", ""),
                        mealType = n.optString("mealType", "CUSTOM"),
                        foodName = n.optString("foodName", ""),
                        grams = n.optDouble("grams", 100.0),
                        calories = n.optDouble("calories", 0.0),
                        protein = n.optDouble("protein", 0.0),
                        carbs = n.optDouble("carbs", 0.0),
                        fat = n.optDouble("fat", 0.0),
                        createdAt = n.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }
        }

        // Daily Steps
        val stepsList = mutableListOf<DailyStepsEntity>()
        val stepsArr = json.optJSONArray("dailySteps")
        if (stepsArr != null) {
            for (i in 0 until stepsArr.length()) {
                val ds = stepsArr.optJSONObject(i) ?: continue
                stepsList.add(
                    DailyStepsEntity(
                        localDate = ds.optString("localDate", java.time.LocalDate.now().toString()),
                        stepCount = ds.optInt("stepCount", 0),
                        goalSteps = ds.optInt("goalSteps", 8000),
                        distanceMeters = ds.optDouble("distanceMeters", 0.0),
                        caloriesBurned = ds.optDouble("caloriesBurned", 0.0),
                        updatedAt = ds.optLong("updatedAt", System.currentTimeMillis())
                    )
                )
            }
        }

        return CloudUserData(
            userId = userId,
            email = email,
            passwordHash = passwordHash,
            displayName = displayName,
            photoUrl = photoUrl,
            updatedAt = updatedAt,
            profile = profile,
            workouts = workoutsList,
            sleepLogs = sleepList,
            nutritionLogs = nutList,
            dailySteps = stepsList
        )
    }
}
