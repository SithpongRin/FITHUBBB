package com.example.updates

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

object ApkInstaller {

    /**
     * Checks if the app has permission to install unknown apps (Android 8.0+)
     */
    fun canInstallApks(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    /**
     * Opens system settings to allow installing unknown apps
     */
    fun requestInstallPermission(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    /**
     * Downloads an APK from the given URL and reports live progress.
     */
    suspend fun downloadApk(
        context: Context,
        apkUrl: String,
        onProgress: (percent: Int, currentBytes: Long, totalBytes: Long) -> Unit
    ): File = withContext(Dispatchers.IO) {
        val updatesDir = File(context.cacheDir, "updates").apply { mkdirs() }
        val outputFile = File(updatesDir, "fithub_update.apk")
        if (outputFile.exists()) {
            outputFile.delete()
        }

        // If it's a simulated URL or demo, simulate realistic downloading progress
        if (!apkUrl.startsWith("http://") && !apkUrl.startsWith("https://")) {
            val fakeTotal = 18_450_000L
            for (percent in 0..100 step 5) {
                kotlinx.coroutines.delay(80)
                onProgress(percent, (fakeTotal * percent) / 100, fakeTotal)
            }
            outputFile.writeText("SIMULATED_APK_PAYLOAD")
            return@withContext outputFile
        }

        var currentUrl = apkUrl
        var connection: HttpURLConnection
        var redirectCount = 0

        while (true) {
            val url = URL(currentUrl)
            connection = url.openConnection() as HttpURLConnection
            connection.instanceFollowRedirects = true
            connection.connectTimeout = 15000
            connection.readTimeout = 30000
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android) FithubApp/1.0.5")
            connection.connect()

            val code = connection.responseCode
            if (code == HttpURLConnection.HTTP_MOVED_TEMP || code == HttpURLConnection.HTTP_MOVED_PERM || code == HttpURLConnection.HTTP_SEE_OTHER || code == 307 || code == 308) {
                val newUrl = connection.getHeaderField("Location")
                connection.disconnect()
                if (newUrl != null && redirectCount < 5) {
                    currentUrl = newUrl
                    redirectCount++
                    continue
                }
            }

            if (code != HttpURLConnection.HTTP_OK) {
                connection.disconnect()
                throw Exception("Server returned HTTP $code: ${connection.responseMessage}")
            }
            break
        }

        val fileLength = connection.contentLength.toLong()
        val inputStream = connection.inputStream
        val outputStream = FileOutputStream(outputFile)

        val buffer = ByteArray(8192)
        var totalRead = 0L
        var bytesRead: Int

        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
            outputStream.write(buffer, 0, bytesRead)
            totalRead += bytesRead
            if (fileLength > 0) {
                val percent = ((totalRead * 100) / fileLength).toInt()
                onProgress(percent, totalRead, fileLength)
            }
        }

        outputStream.flush()
        outputStream.close()
        inputStream.close()
        connection.disconnect()

        outputFile
    }

    /**
     * Prompts the Android OS PackageInstaller to install the downloaded APK.
     */
    fun installApk(context: Context, apkFile: File) {
        val authority = "${context.packageName}.fileprovider"
        val apkUri: Uri = FileProvider.getUriForFile(context, authority, apkFile)

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
