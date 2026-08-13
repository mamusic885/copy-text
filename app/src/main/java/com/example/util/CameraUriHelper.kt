package com.example.util

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

object CameraUriHelper {

    fun createTempImageUri(context: Context): Uri {
        val tempDir = File(context.cacheDir, "images").apply {
            if (!exists()) mkdirs()
        }
        val tempFile = File.createTempFile(
            "copy_text_${System.currentTimeMillis()}_",
            ".jpg",
            tempDir
        )
        val authority = "${context.packageName}.fileprovider"
        return FileProvider.getUriForFile(context, authority, tempFile)
    }
}
