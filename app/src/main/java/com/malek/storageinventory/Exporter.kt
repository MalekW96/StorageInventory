package com.malek.storageinventory

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Builds Excel exports (storage contents, activity log) and opens the share sheet. */
object Exporter {
    private const val MIME = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    private val stamp = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.US)
    private val fileStamp = SimpleDateFormat("yyyyMMdd-HHmm", Locale.US)

    private fun fmt(ms: Long) = if (ms > 0) stamp.format(Date(ms)) else "Unknown"

    private fun typeName(n: Node) = when (n.type) {
        NodeType.STORAGE -> if (n.isTemporary) "Temporary storage" else "Storage"
        NodeType.CONTAINER -> "Container"
        NodeType.ITEM -> "Item"
    }

    private fun countText(items: Int, containers: Int): String {
        val i = if (items == 1) "1 item" else "$items items"
        return if (containers == 0) i else "$i · ${if (containers == 1) "1 container" else "$containers containers"}"
    }

    /** Everything inside [root] (all levels), one row each, in the same order as the app. */
    fun storageSheets(store: InventoryStore, root: Node, appVersion: String): List<XlsxSheet> {
        val rows = ArrayList<List<Any?>>()
        fun walk(parent: Node, level: Int) {
            for (child in store.children(parent.id)) {
                val location = store.path(child.id).dropLast(1).joinToString(" › ") { it.name }
                val contents = if (child.canHoldChildren) {
                    val (i, c) = store.countInside(child.id)
                    countText(i, c)
                } else ""
                rows.add(
                    listOf(
                        rows.size + 1,
                        child.name,
                        typeName(child),
                        location,
                        parent.name,
                        level,
                        if (child.hasQr) child.qr else "(none)",
                        fmt(child.createdAt),
                        if (child.photoPath != null) "Yes" else "No",
                        contents,
                    )
                )
                if (child.canHoldChildren) walk(child, level + 1)
            }
        }
        walk(root, 1)
        val (items, containers) = store.countInside(root.id)
        val contentsSheet = XlsxSheet(
            name = "Contents",
            header = listOf("#", "Name", "Type", "Location", "Directly inside", "Level", "QR code", "Added", "Photo", "Contents"),
            rows = rows,
            widths = listOf(6, 30, 18, 40, 24, 7, 22, 21, 8, 26),
        )
        val summary = XlsxSheet(
            name = "Summary",
            header = listOf("Field", "Value"),
            rows = listOf(
                listOf("Name", root.name),
                listOf("Type", typeName(root)),
                listOf("Location", store.path(root.id).dropLast(1).joinToString(" › ") { it.name }.ifEmpty { "Top level" }),
                listOf("QR code", if (root.hasQr) root.qr else "(none)"),
                listOf("Added", fmt(root.createdAt)),
                listOf("Items inside", items),
                listOf("Containers inside", containers),
                listOf("Exported", fmt(System.currentTimeMillis())),
                listOf("App version", "$appVersion (Android)"),
            ),
            widths = listOf(20, 50),
        )
        return listOf(contentsSheet, summary)
    }

    fun logSheets(appVersion: String): List<XlsxSheet> {
        val entries = ActivityLog.entries()
        val log = XlsxSheet(
            name = "Activity log",
            header = listOf("#", "Date & time", "Event", "Details", "Screen", "App version"),
            rows = entries.mapIndexed { i, e -> listOf(i + 1, fmt(e.time), e.event, e.details, e.screen, e.version) },
            widths = listOf(7, 21, 26, 70, 26, 12),
        )
        val device = XlsxSheet(
            name = "Device",
            header = listOf("Field", "Value"),
            rows = listOf(
                listOf("App version", "$appVersion (Android)"),
                listOf("Android", "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"),
                listOf("Device", "${Build.MANUFACTURER} ${Build.MODEL}"),
                listOf("Entries", entries.size),
                listOf("First entry", entries.firstOrNull()?.let { fmt(it.time) } ?: "-"),
                listOf("Last entry", entries.lastOrNull()?.let { fmt(it.time) } ?: "-"),
                listOf("Exported", fmt(System.currentTimeMillis())),
            ),
            widths = listOf(16, 50),
        )
        return listOf(log, device)
    }

    fun fileName(base: String): String {
        val clean = base.replace(Regex("""[^\p{L}\p{N} _.-]"""), "").trim().replace(' ', '_').ifEmpty { "Export" }
        return "${clean.take(40)}_${fileStamp.format(Date())}.xlsx"
    }

    /** Writes the workbook (call off the main thread) and returns the file. */
    fun write(context: Context, fileName: String, sheets: List<XlsxSheet>): File {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() }
        val file = File(dir, fileName)
        Xlsx.write(file, sheets)
        return file
    }

    /** Opens Android's share sheet (save to Files, Drive, email, WhatsApp…). */
    fun share(context: Context, file: File, title: String) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val send = Intent(Intent.ACTION_SEND)
            .setType(MIME)
            .putExtra(Intent.EXTRA_STREAM, uri)
            .putExtra(Intent.EXTRA_SUBJECT, title)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        val chooser = Intent.createChooser(send, title).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }
}
