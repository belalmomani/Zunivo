package com.zunivo.app.feature.library

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import com.caverock.androidsvg.SVG
import com.zunivo.app.data.remote.ApiException
import com.zunivo.app.data.remote.IconifyApi
import com.zunivo.app.domain.CollectionEntry
import com.zunivo.app.domain.EditParams
import com.zunivo.app.domain.FileNames
import com.zunivo.app.domain.HexColor
import com.zunivo.app.domain.IconCollection
import com.zunivo.app.domain.PackDocs
import com.zunivo.app.domain.PackPlan
import com.zunivo.app.domain.PackPlanner
import com.zunivo.app.domain.Provenance
import com.zunivo.app.domain.SvgComposer
import com.zunivo.app.domain.SvgSafety
import com.zunivo.app.domain.luminance
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Collections
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

/** Builds a real ZIP (svg files, README, LICENSES, colors, preview) and re-opens it to verify it. */
class PackExporter(private val context: Context, private val api: IconifyApi) {

    class Result(val file: File, val included: Int, val skipped: Int)
    class NothingToExport : Exception()
    class NetworkDown : Exception()

    suspend fun export(c: IconCollection): Result {
        val plan = PackPlanner.plan(c)
        if (plan.included.isEmpty()) throw NothingToExport()

        val offline = AtomicBoolean(false)
        val gate = Semaphore(PARALLEL)
        val fetched: List<Pair<CollectionEntry, String?>> = coroutineScope {
            plan.included.map { e -> async { gate.withPermit { e to fetchOne(c, e, offline) } } }.awaitAll()
        }
        val ok = fetched.filter { it.second != null }.map { it.first to it.second!! }
        if (ok.isEmpty()) throw if (offline.get()) NetworkDown() else NothingToExport()
        val failed = fetched.filter { it.second == null }.map { it.first }

        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        cleanOld(dir)
        val file = File(dir, "zunivo-${FileNames.sanitize(c.name)}.zip")
        val docsPlan = PackPlan(ok.map { it.first }, plan.excluded)
        val stamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z", Locale.ROOT).format(Date())

        ZipOutputStream(BufferedOutputStream(FileOutputStream(file))).use { zip ->
            fun add(name: String, bytes: ByteArray) {
                zip.putNextEntry(ZipEntry(name))
                zip.write(bytes)
                zip.closeEntry()
            }
            add("README.md", PackDocs.readme(c, docsPlan, stamp, failed).toByteArray(Charsets.UTF_8))
            add("LICENSES.txt", PackDocs.licenses(docsPlan).toByteArray(Charsets.UTF_8))
            add("colors.json", PackDocs.colorsJson(c).toByteArray(Charsets.UTF_8))
            add("preview.png", previewPng(ok.map { it.second }, c.colorHex))
            ok.forEach { (entry, svg) -> add("svg/${entry.fileName}.svg", svg.toByteArray(Charsets.UTF_8)) }
        }
        verify(file, ok.map { "svg/${it.first.fileName}.svg" })
        return Result(file, ok.size, plan.excluded.size + failed.size)
    }

    private suspend fun fetchOne(c: IconCollection, e: CollectionEntry, offline: AtomicBoolean): String? {
        return try {
            val raw = api.fetchSvg(EditParams(colorHex = c.colorHex).svgUrl(e.snapshot.icon))
            if (!SvgSafety.isSafe(raw)) return null
            SvgComposer.compose(raw, ICON_SIZE, 0, null, Provenance.forIcon(e.snapshot, c.colorHex != null))
        } catch (ex: ApiException) {
            if (ex is ApiException.Offline) offline.set(true)
            null
        }
    }

    private fun previewPng(svgs: List<String>, colorHex: String?): ByteArray {
        val shown = svgs.take(COLS * COLS)
        val rows = (shown.size + COLS - 1) / COLS
        val sheet = Bitmap.createBitmap(COLS * CELL, maxOf(1, rows) * CELL, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(sheet)
        canvas.drawColor(if (colorHex != null && HexColor.luminance(colorHex) > 0.7) Color.parseColor("#0B1020") else Color.parseColor("#F1F3FF"))
        val paint = Paint(Paint.FILTER_BITMAP_FLAG)
        shown.forEachIndexed { i, svgText ->
            try {
                val bmp = Bitmap.createBitmap(ICON_SIZE, ICON_SIZE, Bitmap.Config.ARGB_8888)
                SVG.getFromString(svgText).renderToCanvas(Canvas(bmp))
                val x = (i % COLS) * CELL + 12f
                val y = (i / COLS) * CELL + 12f
                canvas.drawBitmap(bmp, null, RectF(x, y, x + CELL - 24f, y + CELL - 24f), paint)
                bmp.recycle()
            } catch (e: Exception) {
                // one unrenderable icon must not break the archive; it is simply missing from the preview
            }
        }
        val out = ByteArrayOutputStream()
        sheet.compress(Bitmap.CompressFormat.PNG, 100, out)
        sheet.recycle()
        return out.toByteArray()
    }

    /** Re-opens the archive and reads every entry, so a corrupt ZIP is caught before sharing. */
    private fun verify(file: File, svgNames: List<String>) {
        ZipFile(file).use { z ->
            val entries = Collections.list(z.entries())
            val names = entries.map { it.name }.toSet()
            val expected = setOf("README.md", "LICENSES.txt", "colors.json", "preview.png") + svgNames
            if (!names.containsAll(expected)) throw IOException("archive is missing entries")
            if (names.any { it.startsWith("/") || it.contains("..") }) throw IOException("unsafe entry name")
            entries.forEach { e -> z.getInputStream(e).use { it.readBytes() } }
        }
    }

    private fun cleanOld(dir: File) {
        val cutoff = System.currentTimeMillis() - 24L * 60 * 60 * 1000
        dir.listFiles()?.forEach { if (it.lastModified() < cutoff) it.delete() }
    }

    private companion object {
        const val PARALLEL = 4
        const val ICON_SIZE = 256
        const val COLS = 6
        const val CELL = 96
    }
}
