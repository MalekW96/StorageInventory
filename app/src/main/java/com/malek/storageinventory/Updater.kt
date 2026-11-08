package com.malek.storageinventory

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.content.FileProvider
import androidx.core.content.pm.PackageInfoCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(
    val versionCode: Long,
    val versionName: String,
    val apkUrl: String,
    val notes: String,
)

/**
 * Checks a small JSON file online for a newer version, downloads the APK and hands
 * it to Android's installer.
 *
 * The JSON file looks like:
 * { "versionCode": 2, "versionName": "1.1", "apkUrl": "https://…/StorageInventory.apk", "notes": "…" }
 */
object Updater {
    /** Where the app looks for update.json unless changed in Settings. */
    const val DEFAULT_UPDATE_URL =
        "https://github.com/MalekW96/StorageInventory/releases/latest/download/update.json"

    private const val PREFS = "settings"
    private const val KEY_URL = "update_url"

    fun updateUrl(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_URL, null)?.takeIf { it.isNotBlank() } ?: DEFAULT_UPDATE_URL

    fun setUpdateUrl(context: Context, url: String?) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_URL, url?.trim()?.takeIf { it.isNotEmpty() })
            .apply()
    }

    fun installedVersion(context: Context): Pair<Long, String> {
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        return PackageInfoCompat.getLongVersionCode(info) to (info.versionName ?: "?")
    }

    private fun open(url: String): HttpURLConnection {
        var current = url
        // Follow redirects by hand (GitHub sends release downloads through a redirect).
        repeat(6) {
            val conn = URL(current).openConnection() as HttpURLConnection
            conn.instanceFollowRedirects = false
            conn.connectTimeout = 15_000
            conn.readTimeout = 30_000
            conn.setRequestProperty("User-Agent", "StorageInventory-Android")
            conn.setRequestProperty("Cache-Control", "no-cache")
            val code = conn.responseCode
            if (code in 300..399) {
                val location = conn.getHeaderField("Location") ?: throw Exception("Bad redirect")
                current = URL(URL(current), location).toString()
                conn.disconnect()
            } else if (code == 200) {
                return conn
            } else {
                conn.disconnect()
                throw Exception(
                    if (code == 404) "No update file found at the update address (HTTP 404)."
                    else "Server answered HTTP $code."
                )
            }
        }
        throw Exception("Too many redirects.")
    }

    suspend fun fetchLatest(context: Context): UpdateInfo = withContext(Dispatchers.IO) {
        val url = updateUrl(context)
        if (url.contains("/OWNER/REPO/")) {
            throw Exception("No update address set yet. Add it under “Update address” below.")
        }
        val conn = open(url)
        val text = conn.inputStream.bufferedReader().use { it.readText() }
        conn.disconnect()
        val json = JSONObject(text)
        UpdateInfo(
            versionCode = json.getLong("versionCode"),
            versionName = json.optString("versionName", json.getLong("versionCode").toString()),
            apkUrl = json.getString("apkUrl"),
            notes = json.optString("notes", ""),
        )
    }

    suspend fun download(context: Context, info: UpdateInfo, onProgress: (Float) -> Unit): File =
        withContext(Dispatchers.IO) {
            val dir = File(context.cacheDir, "updates").apply { mkdirs() }
            dir.listFiles()?.forEach { it.delete() }
            val file = File(dir, "StorageInventory-${info.versionName}.apk")
            val conn = open(info.apkUrl)
            val total = conn.contentLengthLong
            conn.inputStream.use { input ->
                FileOutputStream(file).use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var done = 0L
                    var lastPercent = -1
                    while (true) {
                        val n = input.read(buffer)
                        if (n < 0) break
                        output.write(buffer, 0, n)
                        done += n
                        if (total > 0) {
                            val percent = (done * 100 / total).toInt()
                            if (percent != lastPercent) {
                                lastPercent = percent
                                onProgress(percent / 100f)
                            }
                        }
                    }
                }
            }
            conn.disconnect()
            file
        }

    /** True if Android lets this app install updates; otherwise opens the setting to allow it. */
    fun ensureInstallPermission(context: Context): Boolean {
        if (context.packageManager.canRequestPackageInstalls()) return true
        val intent = Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            Uri.parse("package:${context.packageName}")
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
        }
        return false
    }

    fun install(context: Context, apk: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apk)
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}
