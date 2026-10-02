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

    const val CURRENT_VERSION_NAME = "1.0.3"
    const val CURRENT_VERSION_CODE = 4

    private val _updateStatus = MutableStateFlow<UpdateStatus>(UpdateStatus.Idle)
    val updateStatus: StateFlow<UpdateStatus> = _updateStatus.asStateFlow()

    // Configurable endpoint for checking updates from GitHub raw JSON
    var customUpdateUrl: String = "https://raw.githubusercontent.com/SithpongRin/FITHUBBB/main/version.json"

    suspend fun checkForUpdates(
        context: Context? = null,
        forceSimulateAvailable: Boolean = false
    ) = withContext(Dispatchers.IO) {
        _updateStatus.value = UpdateStatus.Checking
        kotlinx.coroutines.delay(1000)

        if (forceSimulateAvailable) {
            _updateStatus.value = UpdateStatus.Available(
                version = "1.1.0",
                versionCode = 2,
                notesEn = "Option 2 Direct In-App APK Updater with automatic download, live progress and package installer.",
                notesKm = "មុខងារអាប់ដែត APK ស្វ័យប្រវត្តិក្នង App ផ្ទាល់ (ករណីទី ២)៖ ទាញយកលឿន និងដំឡើងជាន់គ្នាដោយមិនបាត់ទិន្នន័យ។",
                apkUrl = "demo_apk_stream",
                downloadSizeBytes = 18_450_000L
            )
            return@withContext
        }

        if (customUpdateUrl.isNotBlank() && customUpdateUrl.startsWith("http")) {
            try {
                var checkUrl = if (customUpdateUrl.contains("?")) "$customUpdateUrl&_t=${System.currentTimeMillis()}" else "$customUpdateUrl?_t=${System.currentTimeMillis()}"
                var conn: HttpURLConnection
                var redirectCount = 0

                while (true) {
                    val url = URL(checkUrl)
                    conn = url.openConnection() as HttpURLConnection
                    conn.connectTimeout = 10000
                    conn.readTimeout = 10000
                    conn.useCaches = false
                    conn.instanceFollowRedirects = true

                    val code = conn.responseCode
                    if (code == HttpURLConnection.HTTP_MOVED_TEMP || code == HttpURLConnection.HTTP_MOVED_PERM || code == HttpURLConnection.HTTP_SEE_OTHER || code == 307 || code == 308) {
                        val redirectUrl = conn.getHeaderField("Location")
                        conn.disconnect()
                        if (redirectUrl != null && redirectCount < 4) {
                            checkUrl = redirectUrl
                            redirectCount++
                            continue
                        }
                    }

                    if (code == HttpURLConnection.HTTP_OK) {
                        val reader = BufferedReader(InputStreamReader(conn.inputStream))
                        val jsonStr = reader.readText()
                        reader.close()
                        conn.disconnect()
                        val json = JSONObject(jsonStr)

                        val remoteVersionCode = json.optInt("versionCode", 1)
                        val remoteVersionName = json.optString("versionName", "1.0.0")
                        val notesEn = json.optString("releaseNotesEn", "New update available.")
                        val notesKm = json.optString("releaseNotesKm", "មានកំណែថ្មី។")
                        val apkUrl = json.optString("apkUrl", "")
                        val size = json.optLong("fileSizeBytes", 18_450_000L)

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
                    } else {
                        conn.disconnect()
                        _updateStatus.value = UpdateStatus.Error(
                            if (code == 404)
                                "HTTP 404: មិនអាចទាញយក version.json បានទេ។ សូមប្រាកដថា GitHub Repository FITHUBBB ត្រូវបានកំណត់ជា Public។"
                            else
                                "HTTP Error $code ពេលពិនិត្យកំណែថ្មី។"
                        )
                        return@withContext
                    }
                }
            } catch (e: Exception) {
                _updateStatus.value = UpdateStatus.Error("កំហុសបណ្តាញ៖ ${e.localizedMessage ?: "មិនអាចភ្ជាប់ទៅកាន់ GitHub បានទេ"}")
                return@withContext
            }
        }

        _updateStatus.value = UpdateStatus.UpToDate
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
