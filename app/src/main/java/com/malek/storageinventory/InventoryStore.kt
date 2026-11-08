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
    val nodes = mutableStateListOf<Node>()

    init {
        load()
    }

    fun storages(): List<Node> =
        nodes.filter { it.parentId == null }.sortedBy { it.name.lowercase() }

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
            nodes[index] = nodes[index].copy(qr = qr)
            save()
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
            createdAt = System.currentTimeMillis(),
            photoPaths = photoPaths,
        )
        nodes.add(node)
        save()
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
            nodes[index] = nodes[index].copy(name = newName.trim())
            save()
            ActivityLog.log("Renamed", "“$old” → “${newName.trim()}”")
        }
    }

    /** Adds another photo to an entry. */
    fun addPhoto(id: String, photoPath: String) {
        val index = nodes.indexOfFirst { it.id == id }
        if (index >= 0) {
            val n = nodes[index]
            nodes[index] = n.copy(photoPaths = n.photoPaths + photoPath)
            save()
            ActivityLog.log("Photo added", "“${n.name}” · now ${n.photoPaths.size + 1} photo(s)")
        }
    }

    /** Removes one photo from an entry and deletes the file. */
    fun removePhoto(id: String, photoPath: String) {
        val index = nodes.indexOfFirst { it.id == id }
        if (index >= 0) {
            val n = nodes[index]
            nodes[index] = n.copy(photoPaths = n.photoPaths - photoPath)
            save()
            File(photoPath).delete()
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
        val photos = nodes.filter { it.id in toRemove }.flatMap { it.photoPaths }
        nodes.removeAll { it.id in toRemove }
        save()
        photos.forEach { File(it).delete() }
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
                    )
                )
            }
            nodes.clear()
            nodes.addAll(loaded)
        } catch (e: Exception) {
            Log.e("InventoryStore", "Could not read inventory file", e)
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
