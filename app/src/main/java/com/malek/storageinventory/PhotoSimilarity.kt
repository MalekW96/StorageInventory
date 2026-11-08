package com.malek.storageinventory

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import kotlin.math.sqrt

/**
 * Finds stored photos that look like a new one, entirely on the phone.
 *
 * Each photo is described by two things:
 *  - what Google's on-device image-labelling AI (ML Kit) sees in it ("Tool", "Cup", "Shoe"…),
 *    with a confidence for each label;
 *  - a small colour histogram (which colours it's made of).
 * Two photos are similar when they share labels and colours. Descriptions of stored photos
 * are cached in a file so each photo is only analysed once.
 */
object PhotoSimilarity {
    private const val CACHE_FILE = "photo_features.json"
    private const val HUE_BINS = 12
    private const val SAT_BINS = 3
    private const val GRAY_BINS = 4
    private const val BINS = HUE_BINS * SAT_BINS + GRAY_BINS

    class Features(val labels: Map<Int, Float>, val hist: FloatArray)

    private val labeler by lazy {
        ImageLabeling.getClient(ImageLabelerOptions.Builder().setConfidenceThreshold(0.15f).build())
    }

    private var cache: MutableMap<String, Features>? = null
    private val lock = Any()

    private fun cache(context: Context): MutableMap<String, Features> = synchronized(lock) {
        cache ?: loadCache(context).also { cache = it }
    }

    private fun loadCache(context: Context): MutableMap<String, Features> {
        val result = HashMap<String, Features>()
        val file = File(context.filesDir, CACHE_FILE)
        if (!file.exists()) return result
        try {
            val root = JSONObject(file.readText())
            for (path in root.keys()) {
                if (!File(path).exists()) continue
                val o = root.getJSONObject(path)
                val labelsJson = o.getJSONObject("labels")
                val labels = HashMap<Int, Float>()
                for (k in labelsJson.keys()) labels[k.toInt()] = labelsJson.getDouble(k).toFloat()
                val histJson = o.getJSONArray("hist")
                val hist = FloatArray(histJson.length()) { histJson.getDouble(it).toFloat() }
                if (hist.size == BINS) result[path] = Features(labels, hist)
            }
        } catch (e: Exception) {
            Log.e("PhotoSimilarity", "Could not read feature cache", e)
        }
        return result
    }

    private fun saveCache(context: Context) {
        val snapshot = synchronized(lock) { HashMap(cache ?: return) }
        val root = JSONObject()
        for ((path, f) in snapshot) {
            val labels = JSONObject()
            for ((k, v) in f.labels) labels.put(k.toString(), v.toDouble())
            val hist = JSONArray()
            for (v in f.hist) hist.put(v.toDouble())
            root.put(path, JSONObject().put("labels", labels).put("hist", hist))
        }
        try {
            File(context.filesDir, CACHE_FILE).writeText(root.toString())
        } catch (e: Exception) {
            Log.e("PhotoSimilarity", "Could not save feature cache", e)
        }
    }

    /** Analyses one photo. Must run off the main thread. */
    private fun analyse(path: String): Features? {
        val bitmap = PhotoStore.load(path, 480) ?: return null
        val labels = try {
            Tasks.await(labeler.process(InputImage.fromBitmap(bitmap, 0)))
                .associate { it.index to it.confidence }
        } catch (e: Exception) {
            Log.e("PhotoSimilarity", "Labelling failed", e)
            emptyMap()
        }
        return Features(labels, histogram(bitmap))
    }

    private fun histogram(bitmap: Bitmap): FloatArray {
        val small = Bitmap.createScaledBitmap(bitmap, 48, 48, true)
        val hist = FloatArray(BINS)
        val hsv = FloatArray(3)
        for (y in 0 until small.height) {
            for (x in 0 until small.width) {
                Color.colorToHSV(small.getPixel(x, y), hsv)
                val index = if (hsv[1] < 0.18f || hsv[2] < 0.15f) {
                    HUE_BINS * SAT_BINS + (hsv[2] * GRAY_BINS).toInt().coerceIn(0, GRAY_BINS - 1)
                } else {
                    val h = (hsv[0] / 360f * HUE_BINS).toInt().coerceIn(0, HUE_BINS - 1)
                    val s = ((hsv[1] - 0.18f) / 0.82f * SAT_BINS).toInt().coerceIn(0, SAT_BINS - 1)
                    h * SAT_BINS + s
                }
                hist[index] += 1f
            }
        }
        val total = hist.sum().coerceAtLeast(1f)
        for (i in hist.indices) hist[i] /= total
        return hist
    }

    private fun cosine(a: Map<Int, Float>, b: Map<Int, Float>): Float {
        if (a.isEmpty() || b.isEmpty()) return 0f
        var dot = 0f
        for ((k, v) in a) b[k]?.let { dot += v * it }
        val na = sqrt(a.values.sumOf { (it * it).toDouble() }).toFloat()
        val nb = sqrt(b.values.sumOf { (it * it).toDouble() }).toFloat()
        return if (na == 0f || nb == 0f) 0f else dot / (na * nb)
    }

    private fun histOverlap(a: FloatArray, b: FloatArray): Float {
        var sum = 0f
        for (i in a.indices) sum += minOf(a[i], b[i])
        return sum
    }

    private fun similarity(a: Features, b: Features): Float {
        val colour = histOverlap(a.hist, b.hist)
        return if (a.labels.isEmpty() || b.labels.isEmpty()) colour * 0.6f
        else 0.65f * cosine(a.labels, b.labels) + 0.35f * colour
    }

    /**
     * Compares the photo at [queryPath] with the photos of [candidates] and returns the
     * closest matches, best first, with a 0–1 similarity score.
     */
    suspend fun findSimilar(
        context: Context,
        queryPath: String,
        candidates: List<Node>,
        onProgress: (done: Int, total: Int) -> Unit,
    ): List<Pair<Node, Float>> = withContext(Dispatchers.Default) {
        val query = analyse(queryPath) ?: throw Exception("Couldn't read the photo.")
        val cached = cache(context)
        val withPhotos = candidates.filter { it.photoPaths.isNotEmpty() }
        val total = withPhotos.sumOf { it.photoPaths.size }
        var done = 0
        var changed = false
        val scored = ArrayList<Pair<Node, Float>>()
        for (node in withPhotos) {
            // A node matches as well as its best-matching photo.
            var best = -1f
            for (path in node.photoPaths) {
                withContext(Dispatchers.Main) { onProgress(done, total) }
                val features = synchronized(lock) { cached[path] } ?: analyse(path)?.also {
                    synchronized(lock) { cached[path] = it }
                    changed = true
                }
                if (features != null) best = maxOf(best, similarity(query, features))
                done++
            }
            if (best >= 0f) scored.add(node to best)
        }
        if (changed) saveCache(context)
        withContext(Dispatchers.Main) { onProgress(total, total) }
        scored.filter { it.second >= 0.2f }.sortedByDescending { it.second }.take(15)
    }
}
