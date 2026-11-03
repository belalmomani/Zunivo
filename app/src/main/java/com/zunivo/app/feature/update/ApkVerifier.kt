package com.zunivo.app.feature.update

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.pm.PackageInfoCompat
import java.io.File
import java.security.MessageDigest

/** Checks a downloaded APK against the installed app: same package, newer version, same signing certificate. */
object ApkVerifier {
    enum class Result { OK, UNREADABLE, WRONG_PACKAGE, NOT_NEWER, SIGNATURE_MISMATCH }

    @Suppress("DEPRECATION")
    fun verify(context: Context, apk: File): Result {
        val pm = context.packageManager
        val flags = if (Build.VERSION.SDK_INT >= 28) PackageManager.GET_SIGNING_CERTIFICATES else PackageManager.GET_SIGNATURES
        val archive = pm.getPackageArchiveInfo(apk.path, flags) ?: return Result.UNREADABLE
        if (archive.packageName != context.packageName) return Result.WRONG_PACKAGE
        val installed = try {
            pm.getPackageInfo(context.packageName, flags)
        } catch (e: PackageManager.NameNotFoundException) {
            return Result.UNREADABLE
        }
        if (PackageInfoCompat.getLongVersionCode(archive) <= PackageInfoCompat.getLongVersionCode(installed)) return Result.NOT_NEWER
        val a = signers(archive)
        val b = signers(installed)
        return if (a.isNotEmpty() && a == b) Result.OK else Result.SIGNATURE_MISMATCH
    }

    @Suppress("DEPRECATION")
    private fun signers(info: PackageInfo): Set<String> {
        val raw = if (Build.VERSION.SDK_INT >= 28) info.signingInfo?.apkContentsSigners else info.signatures
        val md = MessageDigest.getInstance("SHA-256")
        return raw.orEmpty().map { sig -> md.digest(sig.toByteArray()).joinToString("") { "%02x".format(it) } }.toSet()
    }
}
