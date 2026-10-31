package com.zunivo.app.feature.studio

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

data class ColorShare(val argb: Int, val share: Float)

enum class Simplicity { SIMPLE, MEDIUM, DETAILED }

data class ImageProfile(
    val palette: List<ColorShare>,
    val simplicity: Simplicity,
    val significantColors: Int,
    val edgeRatio: Float,
    val isDark: Boolean,
)

/** Local, on-device colour analysis. No image data leaves the phone. */
object PaletteExtractor {
    private class Bucket { var n = 0L; var r = 0L; var g = 0L; var b = 0L }

    fun extract(pixels: IntArray, maxColors: Int = 6): List<ColorShare> {
        val buckets = HashMap<Int, Bucket>()
        var total = 0
        for (p in pixels) {
            if ((p ushr 24) < 128) continue
            val r = (p shr 16) and 0xFF
            val g = (p shr 8) and 0xFF
            val b = p and 0xFF
            val key = ((r shr 4) shl 8) or ((g shr 4) shl 4) or (b shr 4)
            val bucket = buckets.getOrPut(key) { Bucket() }
            bucket.n++; bucket.r += r; bucket.g += g; bucket.b += b
            total++
        }
        if (total == 0) return emptyList()

        data class Avg(var n: Long, val r: Int, val g: Int, val b: Int)
        val sorted = buckets.values.sortedByDescending { it.n }
            .map { Avg(it.n, (it.r / it.n).toInt(), (it.g / it.n).toInt(), (it.b / it.n).toInt()) }
        val chosen = ArrayList<Avg>()
        for (c in sorted) {
            val near = chosen.firstOrNull { distance(it.r, it.g, it.b, c.r, c.g, c.b) < MERGE_DISTANCE }
            if (near != null) near.n += c.n else chosen.add(c)
        }
        return chosen.sortedByDescending { it.n }.take(maxColors).map {
            ColorShare(0xFF shl 24 or (it.r shl 16) or (it.g shl 8) or it.b, it.n.toFloat() / total)
        }
    }

    private fun distance(r1: Int, g1: Int, b1: Int, r2: Int, g2: Int, b2: Int): Int =
        abs(r1 - r2) + abs(g1 - g2) + abs(b1 - b2)

    private const val MERGE_DISTANCE = 64
}

object ImageAnalyzer {
    fun analyze(pixels: IntArray, width: Int, height: Int): ImageProfile? {
        if (width <= 0 || height <= 0 || pixels.size < width * height) return null
        val palette = PaletteExtractor.extract(pixels, maxColors = 12)
        if (palette.isEmpty()) return null
        val significant = palette.count { it.share >= 0.02f }
        val edges = edgeRatio(pixels, width, height)
        val simplicity = when {
            edges > 0.18f || significant > 8 -> Simplicity.DETAILED
            edges < 0.06f && significant <= 4 -> Simplicity.SIMPLE
            else -> Simplicity.MEDIUM
        }
        val luminance = palette.sumOf { c ->
            val r = (c.argb shr 16) and 0xFF; val g = (c.argb shr 8) and 0xFF; val b = c.argb and 0xFF
            (0.299 * r + 0.587 * g + 0.114 * b) / 255.0 * c.share
        } / palette.sumOf { it.share.toDouble() }
        return ImageProfile(palette.take(6), simplicity, significant, edges, luminance < 0.4)
    }

    private fun edgeRatio(px: IntArray, w: Int, h: Int): Float {
        var edges = 0
        var compared = 0
        for (y in 0 until h) {
            for (x in 0 until w) {
                val p = px[y * w + x]
                if ((p ushr 24) < 128) continue
                if (x + 1 < w) { val q = px[y * w + x + 1]; if ((q ushr 24) >= 128) { compared++; if (diff(p, q) > 96) edges++ } }
                if (y + 1 < h) { val q = px[(y + 1) * w + x]; if ((q ushr 24) >= 128) { compared++; if (diff(p, q) > 96) edges++ } }
            }
        }
        return if (compared == 0) 0f else edges.toFloat() / compared
    }

    private fun diff(a: Int, b: Int): Int =
        abs(((a shr 16) and 0xFF) - ((b shr 16) and 0xFF)) +
            abs(((a shr 8) and 0xFF) - ((b shr 8) and 0xFF)) +
            abs((a and 0xFF) - (b and 0xFF))
}

/** Builds a small harmonious palette (HSL rotations) from the image's own colours. */
object Harmony {
    fun toHsl(argb: Int): FloatArray {
        val r = ((argb shr 16) and 0xFF) / 255f
        val g = ((argb shr 8) and 0xFF) / 255f
        val b = (argb and 0xFF) / 255f
        val mx = max(r, max(g, b)); val mn = min(r, min(g, b))
        val l = (mx + mn) / 2f
        val d = mx - mn
        if (d == 0f) return floatArrayOf(0f, 0f, l)
        val s = d / (1f - abs(2f * l - 1f))
        val h = when (mx) {
            r -> ((g - b) / d) % 6f
            g -> (b - r) / d + 2f
            else -> (r - g) / d + 4f
        } * 60f
        return floatArrayOf(if (h < 0) h + 360f else h, s, l)
    }

    fun fromHsl(h: Float, s: Float, l: Float): Int {
        val c = (1f - abs(2f * l - 1f)) * s
        val hp = ((h % 360f) + 360f) % 360f / 60f
        val x = c * (1f - abs(hp % 2f - 1f))
        val (r1, g1, b1) = when {
            hp < 1f -> Triple(c, x, 0f)
            hp < 2f -> Triple(x, c, 0f)
            hp < 3f -> Triple(0f, c, x)
            hp < 4f -> Triple(0f, x, c)
            hp < 5f -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }
        val m = l - c / 2f
        fun ch(v: Float) = ((v + m) * 255f + 0.5f).toInt().coerceIn(0, 255)
        return (0xFF shl 24) or (ch(r1) shl 16) or (ch(g1) shl 8) or ch(b1)
    }

    fun build(primary: Int, secondary: Int?): List<Int> {
        val (h, s, l) = toHsl(primary).let { Triple(it[0], it[1], it[2]) }
        val accentS = max(s, 0.45f)
        val accentL = l.coerceIn(0.35f, 0.65f)
        val out = listOfNotNull(
            primary,
            secondary,
            fromHsl(h + 180f, accentS, accentL),
            fromHsl(h + 30f, accentS, accentL),
            fromHsl(h - 30f, accentS, accentL),
            fromHsl(h, min(s, 0.15f), if (l > 0.5f) 0.15f else 0.92f),
        )
        return out.distinct()
    }
}
