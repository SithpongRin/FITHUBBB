package com.example.updates

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

sealed class UpdateStatus {
    object Idle : UpdateStatus()
    object Checking : UpdateStatus()
    object UpToDate : UpdateStatus()
    data class Available(
        val version: String,
        val versionCode: Int,
        val notesEn: String,
        val notesKm: String,
        val apkUrl: String = "",
        val downloadSizeBytes: Long = 18450000L
    ) : UpdateStatus()
    data class Downloading(
        val progressPercent: Int,
        val downloadedBytes: Long,
        val totalBytes: Long,
        val version: String
    ) : UpdateStatus()
    data class ReadyToInstall(
        val apkFile: File,
        val version: String
    ) : UpdateStatus()
    object PermissionRequired : UpdateStatus()
    data class Error(val message: String) : UpdateStatus()
}

object UpdateManager {

    const val CURRENT_VERSION_NAME = "1.1.3"
    const val CURRENT_VERSION_CODE = 14

    private val _updateStatus = MutableStateFlow<UpdateStatus>(UpdateStatus.Idle)
    val updateStatus: StateFlow<UpdateStatus> = _updateStatus.asStateFlow()

    // Primary endpoint: jsDelivr fast CDN / raw githubusercontent; Fallback: GitHub API
    var customUpdateUrl: String = "https://cdn.jsdelivr.net/gh/SithpongRin/FITHUBBB@main/version.json"
    private const val GITHUB_RAW_URL = "https://raw.githubusercontent.com/SithpongRin/FITHUBBB/main/version.json"
    private const val GITHUB_API_URL = "https://api.github.com/repos/SithpongRin/FITHUBBB/contents/version.json"

    suspend fun checkForUpdates(
        context: Context? = null,
        forceSimulateAvailable: Boolean = false
    ) = withContext(Dispatchers.IO) {
        val currentStatus = _updateStatus.value
        if (!forceSimulateAvailable && (currentStatus is UpdateStatus.Available || currentStatus is UpdateStatus.Downloading || currentStatus is UpdateStatus.ReadyToInstall)) {
            return@withContext
        }

        _updateStatus.value = UpdateStatus.Checking
        kotlinx.coroutines.delay(400)

        if (forceSimulateAvailable) {
            _updateStatus.value = UpdateStatus.Available(
                version = "1.0.5",
                versionCode = 6,
                notesEn = "Option 2 Direct In-App APK Updater with automatic download, live progress and package installer.",
                notesKm = "មុខងារអាប់ដែត APK ស្វ័យប្រវត្តិក្នង App ផ្ទាល់៖ ទាញយកលឿន និងដំឡើងជាន់គ្នាដោយមិនបាត់ទិន្នន័យ។",
                apkUrl = "demo_apk_stream",
                downloadSizeBytes = 28_841_252L
            )
            return@withContext
        }

        // Try fast jsDelivr CDN first, then raw GitHub endpoints, then fallback to GitHub API
        val candidateUrls = listOf(
            customUpdateUrl,
            GITHUB_RAW_URL,
            "https://raw.githubusercontent.com/SithpongRin/FITHUBBB/refs/heads/main/version.json"
        )

        var fetchedJson: JSONObject? = null
        var lastError = ""

        for (endpoint in candidateUrls) {
            try {
                val jsonStr = fetchJsonString(endpoint)
                if (!jsonStr.isNullOrBlank()) {
                    fetchedJson = JSONObject(jsonStr)
                    break
                }
            } catch (e: Exception) {
                lastError = e.localizedMessage ?: "Network error"
            }
        }

        // Fallback: GitHub Contents API with base64 decoding
        if (fetchedJson == null) {
            try {
                val apiResponse = fetchJsonString(GITHUB_API_URL)
                if (!apiResponse.isNullOrBlank()) {
                    val root = JSONObject(apiResponse)
                    val encoded = root.optString("content", "").replace("\n", "").trim()
                    if (encoded.isNotEmpty()) {
                        val decodedBytes = android.util.Base64.decode(encoded, android.util.Base64.DEFAULT)
                        fetchedJson = JSONObject(String(decodedBytes, Charsets.UTF_8))
                    }
                }
            } catch (e: Exception) {
                lastError = e.localizedMessage ?: lastError
            }
        }

        if (fetchedJson != null) {
            val remoteVersionCode = fetchedJson.optInt("versionCode", 1)
            val remoteVersionName = fetchedJson.optString("versionName", "1.0.0")
            val notesEn = fetchedJson.optString("releaseNotesEn", "New update available.")
            val notesKm = fetchedJson.optString("releaseNotesKm", "មានកំណែថ្មី។")
            val apkUrl = fetchedJson.optString("apkUrl", "")
            val size = fetchedJson.optLong("fileSizeBytes", 28_841_252L)

            val currentInstalledCode = try {
                com.example.BuildConfig.VERSION_CODE
            } catch (_: Throwable) {
                CURRENT_VERSION_CODE
            }

            if (remoteVersionCode > currentInstalledCode) {
                _updateStatus.value = UpdateStatus.Available(
                    version = remoteVersionName,
                    versionCode = remoteVersionCode,
                    notesEn = notesEn,
                    notesKm = notesKm,
                    apkUrl = apkUrl,
                    downloadSizeBytes = size
                )
            } else {
                _updateStatus.value = UpdateStatus.UpToDate
            }
            return@withContext
        }

        _updateStatus.value = UpdateStatus.Error(
            if (lastError.isNotBlank()) "កំហុសបណ្តាញ៖ $lastError"
            else "មិនអាចទាញយកព័ត៌មានអាប់ដែតបានទេ។ សូមពិនិត្យការតភ្ជាប់អ៊ីនធឺណិត។"
        )
    }

    private fun fetchJsonString(urlStr: String): String? {
        val checkUrl = if (urlStr.contains("?")) "$urlStr&_t=${System.currentTimeMillis()}" else "$urlStr?_t=${System.currentTimeMillis()}"
        var currentUrl = checkUrl
        var redirectCount = 0

        while (redirectCount < 4) {
            val url = URL(currentUrl)
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 10000
            conn.readTimeout = 10000
            conn.useCaches = false
            conn.instanceFollowRedirects = true
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android) FithubApp/1.1.0")
            conn.setRequestProperty("Accept", "application/json, text/plain, */*")
            conn.setRequestProperty("Connection", "close")
            conn.setRequestProperty("Accept-Encoding", "identity")

            val code = conn.responseCode
            if (code == HttpURLConnection.HTTP_MOVED_TEMP || code == HttpURLConnection.HTTP_MOVED_PERM || code == HttpURLConnection.HTTP_SEE_OTHER || code == 307 || code == 308) {
                val redirectUrl = conn.getHeaderField("Location")
                conn.disconnect()
                if (redirectUrl != null) {
                    currentUrl = redirectUrl
                    redirectCount++
                    continue
                }
            }

            if (code == HttpURLConnection.HTTP_OK) {
                val content = conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                conn.disconnect()
                return content
            } else {
                conn.disconnect()
                return null
            }
        }
        return null
    }

    suspend fun startDownload(context: Context, available: UpdateStatus.Available) {
        if (!ApkInstaller.canInstallApks(context)) {
            _updateStatus.value = UpdateStatus.PermissionRequired
            return
        }

        _updateStatus.value = UpdateStatus.Downloading(
            progressPercent = 0,
            downloadedBytes = 0L,
            totalBytes = available.downloadSizeBytes,
            version = available.version
        )

        try {
            val file = ApkInstaller.downloadApk(context, available.apkUrl) { percent, current, total ->
                _updateStatus.value = UpdateStatus.Downloading(
                    progressPercent = percent,
                    downloadedBytes = current,
                    totalBytes = total,
                    version = available.version
                )
            }
            _updateStatus.value = UpdateStatus.ReadyToInstall(file, available.version)
        } catch (e: Exception) {
            _updateStatus.value = UpdateStatus.Error(e.message ?: "Failed to download update APK")
        }
    }

    fun installApk(context: Context, file: File) {
        if (!ApkInstaller.canInstallApks(context)) {
            _updateStatus.value = UpdateStatus.PermissionRequired
            ApkInstaller.requestInstallPermission(context)
            return
        }
        ApkInstaller.installApk(context, file)
    }

    fun dismissUpdate() {
        _updateStatus.value = UpdateStatus.Idle
    }
}
