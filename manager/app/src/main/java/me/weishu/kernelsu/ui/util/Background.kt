package me.weishu.kernelsu.ui.util

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.provider.OpenableColumns
import me.weishu.kernelsu.ksuApp
import java.io.File
import java.io.FileOutputStream

object BackgroundUtil {
    private fun file(): File = File(ksuApp.filesDir, "background.jpg")

    fun hasBackground(): Boolean = file().exists()

    fun uriForCoil(): Any = file()

    fun clear(): Boolean = file().delete()

    fun save(bitmap: Bitmap): Boolean {
        return runCatching {
            FileOutputStream(file()).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
            true
        }.getOrDefault(false)
    }
}

fun Uri.displayName(context: Context): String? {
    val cursor = context.contentResolver.query(this, null, null, null, null)
    cursor?.use {
        if (it.moveToFirst()) {
            return it.getString(it.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
        }
    }
    return null
}

