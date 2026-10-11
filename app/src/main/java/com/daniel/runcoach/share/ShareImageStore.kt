package com.daniel.runcoach.share

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** F3/F4: writes the PNG to the share cache or the gallery, and builds the share intent. */
object ShareImageStore {

    private const val DIR = "shared_images"

    /** F3: save into the cache and return a FileProvider Uri for ACTION_SEND. */
    fun saveToCache(context: Context, bitmap: Bitmap): Uri {
        val dir = File(context.cacheDir, DIR).apply { mkdirs() }
        val file = File(dir, "pacernih_${System.currentTimeMillis()}.png")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    fun shareIntent(uri: Uri): Intent =
        Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

    /** F3: delete leftover cache images on app open. */
    fun clearOldCache(context: Context) {
        runCatching { File(context.cacheDir, DIR).listFiles()?.forEach { it.delete() } }
    }

    /** F4: save to Pictures/PacerNih/PacerNih_{yyyyMMdd_HHmm}.png. Returns true on success. */
    fun saveToGallery(context: Context, bitmap: Bitmap): Boolean {
        val name = "PacerNih_" + SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date()) + ".png"
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, name)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/PacerNih")
            }
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return false
        return runCatching {
            resolver.openOutputStream(uri)?.use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            true
        }.getOrDefault(false)
    }
}
