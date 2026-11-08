package com.malek.storageinventory

import android.content.Context
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

enum class NodeType { STORAGE, CONTAINER, ITEM }

/**
 * Everything in the inventory is a node: a top-level storage, a smaller container
 * (which can hold more items/containers), or an item. Each one is tied to one QR code.
 */
data class Node(
    val id: String,
    val qr: String,
    val name: String,
    val type: NodeType,
    val parentId: String?,
    val createdAt: Long,
    /** All photos of this entry, first one is the main photo. */
    val photoPaths: List<String> = emptyList(),
    /** Top-level bins put into long-term storage (shown under “Archived storage”). */
    val archived: Boolean = false,
    /** Last change time (ms); the newest change wins when devices sync. */
    val updatedAt: Long = createdAt,
) {
    /** The main (first) photo. */
    val photoPath: String? get() = photoPaths.firstOrNull()

    val canHoldChildren: Boolean get() = type != NodeType.ITEM

    /** Entries made without a QR code (photo-only items, temporary storages). */
    val hasQr: Boolean get() = qr.isNotBlank()

    /** A storage created without a QR label yet. */
    val isTemporary: Boolean get() = type == NodeType.STORAGE && !hasQr
}

/** Keeps the inventory in memory (observable by Compose) and saves it to a JSON file on the phone. */
class InventoryStore(context: Context) {

    private val file = File(context.filesDir, "inventory.json")
    private val tombFile = File(context.filesDir, "deleted.json")
    val photoDir: File = File(context.filesDir, "photos").apply { mkdirs() }
    val nodes = mutableStateListOf<Node>()

    /** Deleted entry id → deletion time, so deletions reach other devices. */
    val tombstones = HashMap<String, Long>()

    /** Called after every local change (for cloud sync). */
    var onLocalChange: ((Node) -> Unit)? = null
    var onLocalDelete: ((id: String, time: Long, photoPaths: List<String>) -> Unit)? = null
    var onPhotoRemoved: ((photoPath: String) -> Unit)? = null

    private fun now() = System.currentTimeMillis()

    /** Replaces a node, stamps the change time, saves and reports it for sync. */
    private fun change(index: Int, transform: (Node) -> Node): Node {
        val updated = transform(nodes[index]).copy(updatedAt = now())
        nodes[index] = updated
        save()
        onLocalChange?.invoke(updated)
        return updated
    }

    /** Photo id used in the cloud = file name without “.jpg”. */
    fun photoId(path: String): String = File(path).nameWithoutExtension
    fun photoPath(id: String): String = File(photoDir, "$id.jpg").absolutePath

    init {
        load()
    }

    /** All top-level bins (active and archived). */
    fun storages(): List<Node> =
        nodes.filter { it.parentId == null }.sortedBy { it.name.lowercase() }

    /** Top-level bins in one view: active or archived. */
    fun storages(archived: Boolean): List<Node> = storages().filter { it.archived == archived }

    /** True when the entry is (inside) an archived bin. */
    fun isArchived(id: String): Boolean = path(id).firstOrNull()?.archived == true

        /** Moves a bin into (or back out of) the archive. */
    fun setArchived(id: String, archived: Boolean) {
        val index = nodes.indexOfFirst { it.id == id }
        if (index >= 0) {
            change(index) { it.copy(archived = archived) }
            ActivityLog.log(if (archived) "Archived" else "Moved to active", "“${nodes[index].name}”")
        }
    }

    /** Children with containers listed first, then items, each alphabetically. */
    fun children(parentId: String): List<Node> =
        nodes.filter { it.parentId == parentId }
            .sortedWith(compareBy<Node>({ it.type == NodeType.ITEM }, { it.name.lowercase() }))

    fun get(id: String): Node? = nodes.firstOrNull { it.id == id }

    fun findByQr(qr: String): Node? =
        if (qr.isBlank()) null else nodes.firstOrNull { it.qr == qr }

    /** Attaches a QR code to an entry that was created without one. */
    fun setQr(id: String, qr: String) {
        val index = nodes.indexOfFirst { it.id == id }
        if (index >= 0) {
            change(index) { it.copy(qr = qr) }
            ActivityLog.log("QR code assigned", "“${nodes[index].name}” · QR $qr")
        }
    }

    /** Path from the top-level storage down to (and including) this node. */
    fun path(id: String): List<Node> {
        val result = ArrayList<Node>()
        var current = get(id)
        var guard = 0
        while (current != null && guard++ < 100) {
            result.add(0, current)
            current = current.parentId?.let { get(it) }
        }
        return result
    }

    /** Counts everything nested anywhere inside a node: (items, containers). */
    fun countInside(id: String): Pair<Int, Int> {
        var items = 0
        var containers = 0
        val queue = ArrayDeque<String>()
        queue.add(id)
        while (queue.isNotEmpty()) {
            val parent = queue.removeFirst()
            for (child in nodes) {
                if (child.parentId == parent) {
                    if (child.type == NodeType.ITEM) items++ else {
                        containers++
                        queue.add(child.id)
                    }
                }
            }
        }
        return items to containers
    }

    fun add(qr: String, name: String, type: NodeType, parentId: String?, photoPaths: List<String> = emptyList()): Node {
        val node = Node(
            id = UUID.randomUUID().toString(),
            qr = qr,
            name = name.trim(),
            type = type,
            parentId = parentId,
            createdAt = now(),
            photoPaths = photoPaths,
        )
        nodes.add(node)
        save()
        onLocalChange?.invoke(node)
        ActivityLog.log(
            "Added ${typeWord(type)}",
            "“${node.name}”" + (if (qr.isNotBlank()) " · QR $qr" else " · no QR") +
                (parentId?.let { " · in ${path(it).joinToString(" › ") { p -> p.name }}" } ?: "") +
                (if (photoPaths.isEmpty()) " · no photo" else " · ${photoPaths.size} photo(s)")
        )
        return node
    }

    fun rename(id: String, newName: String) {
        val index = nodes.indexOfFirst { it.id == id }
        if (index >= 0) {
            val old = nodes[index].name
            change(index) { it.copy(name = newName.trim()) }
            ActivityLog.log("Renamed", "“$old” → “${newName.trim()}”")
        }
    }

    /** Adds another photo to an entry. */
    fun addPhoto(id: String, photoPath: String) {
        val index = nodes.indexOfFirst { it.id == id }
        if (index >= 0) {
            val n = nodes[index]
            change(index) { it.copy(photoPaths = it.photoPaths + photoPath) }
            ActivityLog.log("Photo added", "“${n.name}” · now ${n.photoPaths.size + 1} photo(s)")
        }
    }

    /** Removes one photo from an entry and deletes the file. */
    fun removePhoto(id: String, photoPath: String) {
        val index = nodes.indexOfFirst { it.id == id }
        if (index >= 0) {
            val n = nodes[index]
            change(index) { it.copy(photoPaths = it.photoPaths - photoPath) }
            File(photoPath).delete()
            onPhotoRemoved?.invoke(photoPath)
            ActivityLog.log("Photo deleted", "“${n.name}” · ${n.photoPaths.size - 1} photo(s) left")
        }
    }

    /** Deletes a node and everything inside it. */
    fun delete(id: String) {
        get(id)?.let { n ->
            val (i, c) = countInside(id)
            ActivityLog.log("Deleted ${typeWord(n.type)}", "“${n.name}” · with $i item(s), $c container(s) inside")
        }
        val toRemove = HashSet<String>()
        val queue = ArrayDeque<String>()
        queue.add(id)
        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            if (toRemove.add(current)) {
                nodes.filter { it.parentId == current }.forEach { queue.add(it.id) }
            }
        }
        val gone = nodes.filter { it.id in toRemove }
        val photos = gone.flatMap { it.photoPaths }
        nodes.removeAll { it.id in toRemove }
        val t = now()
        gone.forEach { tombstones[it.id] = t }
        save()
        saveTombs()
        photos.forEach { File(it).delete() }
        gone.forEach { onLocalDelete?.invoke(it.id, t, it.photoPaths) }
    }

    // ----- Changes coming from other devices (never reported back) -----

    /** Inserts or replaces an entry received from the cloud. */
    fun applyRemote(node: Node) {
        val index = nodes.indexOfFirst { it.id == node.id }
        if (index >= 0) {
            val dropped = nodes[index].photoPaths - node.photoPaths.toSet()
            nodes[index] = node
            dropped.forEach { File(it).delete() }
        } else nodes.add(node)
        if (tombstones.remove(node.id) != null) saveTombs()
        save()
    }

    /** Removes one entry deleted on another device. */
    fun removeRemote(id: String) {
        val n = get(id) ?: return
        nodes.removeAll { it.id == id }
        save()
        n.photoPaths.forEach { File(it).delete() }
    }

    /** Wipes everything on this device (used when signing out with “Remove from device”). */
    fun clearAll() {
        val photos = nodes.flatMap { it.photoPaths }
        nodes.clear()
        tombstones.clear()
        save()
        saveTombs()
        photos.forEach { File(it).delete() }
    }

    private fun saveTombs() {
        try {
            val o = JSONObject()
            tombstones.forEach { (k, v) -> o.put(k, v) }
            tombFile.writeText(o.toString())
        } catch (e: Exception) {
            Log.e("InventoryStore", "Could not save deletions", e)
        }
    }

    private fun typeWord(t: NodeType) = when (t) {
        NodeType.STORAGE -> "storage"
        NodeType.CONTAINER -> "container"
        NodeType.ITEM -> "item"
    }

    private fun load() {
        if (!file.exists()) return
        try {
            val array = JSONObject(file.readText()).getJSONArray("nodes")
            val loaded = ArrayList<Node>(array.length())
            for (i in 0 until array.length()) {
                val o = array.getJSONObject(i)
                loaded.add(
                    Node(
                        id = o.getString("id"),
                        qr = o.getString("qr"),
                        name = o.getString("name"),
                        type = NodeType.valueOf(o.getString("type")),
                        parentId = if (o.isNull("parentId")) null else o.getString("parentId"),
                        createdAt = o.optLong("createdAt", 0L),
                        photoPaths = o.optJSONArray("photos")?.let { arr ->
                            (0 until arr.length()).map { arr.getString(it) }.filter { it.isNotBlank() }
                        } ?: listOfNotNull(
                            // Versions before 1.4.2.1 stored a single photo.
                            if (o.isNull("photoPath")) null else o.optString("photoPath").ifBlank { null }
                        ),
                        archived = o.optBoolean("archived", false),
                        updatedAt = o.optLong("updatedAt", o.optLong("createdAt", 0L)),
                    )
                )
            }
            nodes.clear()
            nodes.addAll(loaded)
        } catch (e: Exception) {
            Log.e("InventoryStore", "Could not read inventory file", e)
        }
        try {
            if (tombFile.exists()) {
                val o = JSONObject(tombFile.readText())
                o.keys().forEach { k -> tombstones[k] = o.optLong(k) }
            }
        } catch (e: Exception) {
            Log.e("InventoryStore", "Could not read deletions", e)
        }
    }

    private fun save() {
        val array = JSONArray()
        for (n in nodes) {
            array.put(
                JSONObject()
                    .put("id", n.id)
                    .put("qr", n.qr)
                    .put("name", n.name)
                    .put("type", n.type.name)
                    .put("parentId", n.parentId ?: JSONObject.NULL)
                    .put("createdAt", n.createdAt)
                    .put("photos", JSONArray(n.photoPaths))
                    .put("photoPath", n.photoPath ?: JSONObject.NULL)
                    .put("archived", n.archived)
                    .put("updatedAt", n.updatedAt)
            )
        }
        val json = JSONObject().put("version", 1).put("nodes", array).toString()
        try {
            // Write to a temp file first so a crash mid-write can't corrupt the inventory.
            val tmp = File(file.parentFile, file.name + ".tmp")
            tmp.writeText(json)
            if (!tmp.renameTo(file)) {
                file.writeText(json)
                tmp.delete()
            }
        } catch (e: Exception) {
            Log.e("InventoryStore", "Could not save inventory file", e)
        }
    }
}
