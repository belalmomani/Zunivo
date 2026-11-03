package com.zunivo.app.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.core.content.FileProvider
import com.zunivo.app.R
import java.io.File

/**
 * Hands an APK to Android's own installer. The user must allow "install unknown apps" for Zunivo
 * and confirm the install in the system dialog. Nothing is ever installed silently.
 */
internal fun installApk(context: Context, file: File) {
    if (!context.packageManager.canRequestPackageInstalls()) {
        Toast.makeText(context, R.string.store_install_permission, Toast.LENGTH_LONG).show()
        context.startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + context.packageName)))
        return
    }
    try {
        val uri = FileProvider.getUriForFile(context, "com.zunivo.app.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, R.string.msg_export_failed, Toast.LENGTH_SHORT).show()
    } catch (e: IllegalArgumentException) {
        Toast.makeText(context, R.string.msg_export_failed, Toast.LENGTH_SHORT).show()
    }
}
