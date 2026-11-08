package com.malek.storageinventory

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material3.MaterialTheme
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlin.math.max
import kotlin.math.min

/** Saves and loads the photos taken of storages, containers and items. */
object PhotoStore {
    private const val MAX_SIDE = 1600

    private fun dir(context: Context): File =
        File(context.filesDir, "photos").apply { mkdirs() }

    /** The file the camera app writes into before we process it. */
    fun captureFile(context: Context): File = File(dir(context), "capture_tmp.jpg")

    fun captureUri(context: Context): Uri {
        val file = captureFile(context)
        if (file.exists()) file.delete()
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    /**
     * Turns the raw camera file into a saved photo: fixes rotation, shrinks it to a
     * sensible size and stores it under a unique name. Returns its path, or null.
     */
    fun finishCapture(context: Context): String? {
        val tmp = captureFile(context)
        if (!tmp.exists() || tmp.length() == 0L) return null
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(tmp.path, bounds)
            var sample = 1
            while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_SIDE) sample *= 2
            val decoded = BitmapFactory.decodeFile(tmp.path, BitmapFactory.Options().apply { inSampleSize = sample })
                ?: return null

            val degrees = when (
                ExifInterface(tmp.path).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            ) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
            val scale = min(1f, MAX_SIDE.toFloat() / max(decoded.width, decoded.height))
            val output = if (degrees != 0f || scale < 1f) {
                val m = Matrix()
                if (scale < 1f) m.postScale(scale, scale)
                if (degrees != 0f) m.postRotate(degrees)
                Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, m, true)
            } else decoded

            val out = File(dir(context), "${UUID.randomUUID()}.jpg")
            FileOutputStream(out).use { output.compress(Bitmap.CompressFormat.JPEG, 85, it) }
            tmp.delete()
            out.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    /** Loads a photo scaled down so its longest side is about [maxPx]. */
    fun load(path: String, maxPx: Int): Bitmap? {
        val file = File(path)
        if (!file.exists()) return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxPx) sample *= 2
        return BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample })
    }
}

/** Shows a saved photo (loaded off the main thread), or a plain placeholder while loading. */
@Composable
fun PhotoImage(
    path: String,
    maxPx: Int,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
) {
    // Reload when a photo arrives from another device (cloud sync).
    val version = Cloud.photoVersion
    val bitmap by produceState<ImageBitmap?>(initialValue = null, path, maxPx, version) {
        value = withContext(Dispatchers.IO) { PhotoStore.load(path, maxPx)?.asImageBitmap() }
    }
    val bmp = bitmap
    if (bmp != null) {
        Image(bitmap = bmp, contentDescription = null, modifier = modifier, contentScale = contentScale)
    } else {
        Box(modifier.background(MaterialTheme.colorScheme.surfaceVariant))
    }
}
