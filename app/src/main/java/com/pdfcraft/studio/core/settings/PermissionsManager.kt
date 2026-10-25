package com.pdfcraft.studio.core.settings

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

object StorageAccess {

    /** API 33+ has no broad PDF scan permission; MediaStore + SAF picker are used instead. */
    fun isGranted(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= 33) {
            true
        } else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun needsRuntimeReadPermission(): Boolean = Build.VERSION.SDK_INT < 33
}
