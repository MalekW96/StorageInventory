package com.malek.storageinventory

import android.content.Context
import android.os.Build
import android.util.Log
import org.json.JSONObject
import java.io.File

/**
 * Records every interaction with the date and time, for troubleshooting.
 * Kept on the phone (last 5,000 entries) and exportable to Excel from Settings.
 */
object ActivityLog {
    private const val MAX_ENTRIES = 5000
    private const val FILE = "activity_log.jsonl"

    data class Entry(val time: Long, val event: String, val details: String, val screen: String, val version: String)

    private var file: File? = null
    private var version: String = "?"
    private val lock = Any()

    /** Name of the screen currently shown; stored with each entry. */
    @Volatile
    var screen: String = "Home"

    fun init(context: Context) {
        file = File(context.filesDir, FILE)
        version = try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "?"
        } catch (e: Exception) {
            "?"
        }
        trim()
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            log("Crash", error.stackTraceToString().take(3000))
            previous?.uncaughtException(thread, error)
        }
        log(
            "App opened",
            "Version $version · Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT}) · ${Build.MANUFACTURER} ${Build.MODEL}"
        )
    }

    fun log(event: String, details: String = "") {
        val f = file ?: return
        val line = JSONObject()
            .put("t", System.currentTimeMillis())
            .put("e", event)
            .put("d", details)
            .put("s", screen)
            .put("v", version)
            .toString()
        synchronized(lock) {
            try {
                f.appendText(line + "\n")
            } catch (e: Exception) {
                Log.e("ActivityLog", "Could not write log", e)
            }
        }
    }

    fun entries(): List<Entry> = synchronized(lock) {
        val f = file ?: return emptyList()
        if (!f.exists()) return emptyList()
        f.readLines().mapNotNull { line ->
            try {
                val o = JSONObject(line)
                Entry(o.getLong("t"), o.optString("e"), o.optString("d"), o.optString("s"), o.optString("v"))
            } catch (e: Exception) {
                null
            }
        }
    }

    fun count(): Int = synchronized(lock) {
        val f = file ?: return 0
        if (!f.exists()) 0 else f.useLines { lines -> lines.count() }
    }

    fun clear() = synchronized(lock) {
        file?.writeText("")
    }

    private fun trim() = synchronized(lock) {
        val f = file ?: return
        if (!f.exists()) return
        val lines = f.readLines()
        if (lines.size > MAX_ENTRIES) f.writeText(lines.takeLast(MAX_ENTRIES).joinToString("\n", postfix = "\n"))
    }
}
