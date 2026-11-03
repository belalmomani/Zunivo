package com.zunivo.app.domain

data class ReleaseUpdate(
    val tag: String,
    val version: String,
    val notes: String?,
    val htmlUrl: String?,
    val apkName: String,
    val apkUrl: String,
    val apkSize: Long,
    val sha256: String?,          // from GitHub's asset digest, when present
    val checksumUrl: String?,     // optional "<apk>.sha256" asset
)

object VersionCompare {
    private fun core(v: String): String = v.trim().removePrefix("v").removePrefix("V").substringBefore('+')

    private fun numbers(v: String): List<Int> =
        core(v).substringBefore('-').split('.').map { it.toIntOrNull() ?: 0 }

    private fun isPre(v: String): Boolean = core(v).contains('-')

    /** True when [remote] is a newer release than [local]. A final release beats a pre-release of the same number. */
    fun isNewer(remote: String, local: String): Boolean {
        val a = numbers(remote)
        val b = numbers(local)
        for (i in 0 until maxOf(a.size, b.size)) {
            val x = a.getOrElse(i) { 0 }
            val y = b.getOrElse(i) { 0 }
            if (x != y) return x > y
        }
        return isPre(local) && !isPre(remote)
    }

    fun clean(tag: String): String = core(tag)
}

object ChecksumFile {
    private val hex64 = Regex("(?i)\\b[0-9a-f]{64}\\b")

    /** Reads "<hash>  <file>" style files (or a bare hash). Prefers the line that names [fileName]. */
    fun parse(text: String, fileName: String): String? {
        val lines = text.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.toList()
        val named = lines.firstOrNull { it.contains(fileName) }?.let { hex64.find(it)?.value }
        return (named ?: lines.firstNotNullOfOrNull { hex64.find(it)?.value })?.lowercase()
    }
}
