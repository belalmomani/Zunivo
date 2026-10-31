package com.zunivo.app.feature.editor

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.caverock.androidsvg.SVG
import com.zunivo.app.data.local.EditStore
import com.zunivo.app.data.remote.ApiException
import com.zunivo.app.data.remote.IconifyApi
import com.zunivo.app.domain.EditParams
import com.zunivo.app.domain.IconEdit
import com.zunivo.app.domain.IconItem
import com.zunivo.app.domain.IconRef
import com.zunivo.app.domain.IconSetInfo
import com.zunivo.app.domain.LicensePolicy
import com.zunivo.app.domain.SvgComposer
import com.zunivo.app.domain.SvgSafety
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

enum class ExportFormat { SVG, PNG }

enum class EditorMessage { SAVED, DELETED, RESET, BLOCKED_NO_DERIVATIVES, EXPORT_FAILED, EXPORT_UNSAFE, OFFLINE }

data class EditSession(
    val editId: String?,
    val icon: IconRef,
    val set: IconSetInfo?,
    val params: EditParams,
    val savedParams: EditParams?,
    val createdAt: Long = 0L,
) {
    val dirty: Boolean get() = savedParams == null || savedParams != params
}

data class ShareRequest(val file: File, val mime: String, val text: String)

data class EditorUiState(
    val session: EditSession? = null,
    val saved: List<IconEdit> = emptyList(),
    val busy: Boolean = false,
    val message: EditorMessage? = null,
    val share: ShareRequest? = null,
)

class EditorViewModel(app: Application) : AndroidViewModel(app) {
    private val store = EditStore(app)
    private val api = IconifyApi()
    private val _state = MutableStateFlow(EditorUiState())
    val state: StateFlow<EditorUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val list = withContext(Dispatchers.IO) { store.load() }
            _state.update { it.copy(saved = list.sortedByDescending { e -> e.updatedAt }) }
        }
    }

    fun open(item: IconItem) {
        _state.update {
            it.copy(session = EditSession(null, item.ref, item.set, EditParams(), null), message = null)
        }
    }

    fun reopen(edit: IconEdit) {
        val set = IconSetInfo(
            prefix = edit.icon.prefix, name = edit.setName ?: edit.icon.prefix,
            authorName = edit.authorName, authorUrl = null,
            licenseTitle = edit.licenseTitle, licenseSpdx = edit.licenseSpdx, licenseUrl = edit.licenseUrl,
            palette = null,
        )
        _state.update {
            it.copy(
                session = EditSession(edit.id, edit.icon, set, edit.params, edit.params, edit.createdAt),
                message = null,
            )
        }
    }

    fun close() = _state.update { it.copy(session = null) }

    fun update(change: (EditParams) -> EditParams) {
        _state.update { st ->
            val s = st.session ?: return@update st
            st.copy(session = s.copy(params = change(s.params).sanitized()))
        }
    }

    fun reset() {
        _state.update { st ->
            val s = st.session ?: return@update st
            st.copy(session = s.copy(params = EditParams()), message = EditorMessage.RESET)
        }
    }

    fun save() {
        val st = _state.value
        val s = st.session ?: return
        if (blocksModification(s)) {
            _state.update { it.copy(message = EditorMessage.BLOCKED_NO_DERIVATIVES) }
            return
        }
        val now = System.currentTimeMillis()
        val edit = IconEdit(
            id = s.editId ?: UUID.randomUUID().toString(),
            icon = s.icon,
            setName = s.set?.name, authorName = s.set?.authorName,
            licenseTitle = s.set?.licenseTitle, licenseSpdx = s.set?.licenseSpdx, licenseUrl = s.set?.licenseUrl,
            params = s.params, createdAt = if (s.createdAt > 0) s.createdAt else now, updatedAt = now,
        )
        val list = (listOf(edit) + st.saved.filter { it.id != edit.id })
        _state.update {
            it.copy(
                saved = list, message = EditorMessage.SAVED,
                session = s.copy(editId = edit.id, savedParams = s.params, createdAt = edit.createdAt),
            )
        }
        viewModelScope.launch(Dispatchers.IO) { store.save(list) }
    }

    fun delete(id: String) {
        val list = _state.value.saved.filter { it.id != id }
        _state.update { st ->
            st.copy(
                saved = list, message = EditorMessage.DELETED,
                session = st.session?.let { s -> if (s.editId == id) s.copy(editId = null, savedParams = null) else s },
            )
        }
        viewModelScope.launch(Dispatchers.IO) { store.save(list) }
    }

    fun export(format: ExportFormat) {
        val s = _state.value.session ?: return
        if (blocksModification(s)) {
            _state.update { it.copy(message = EditorMessage.BLOCKED_NO_DERIVATIVES) }
            return
        }
        _state.update { it.copy(busy = true, message = null) }
        viewModelScope.launch {
            try {
                val request = withContext(Dispatchers.IO) { buildExport(s, format) }
                _state.update { it.copy(busy = false, share = request) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: ExportException) {
                _state.update { it.copy(busy = false, message = e.reason) }
            } catch (e: ApiException) {
                _state.update { it.copy(busy = false, message = EditorMessage.OFFLINE) }
            } catch (e: Exception) {
                _state.update { it.copy(busy = false, message = EditorMessage.EXPORT_FAILED) }
            }
        }
    }

    fun consumeShare() = _state.update { it.copy(share = null) }
    fun consumeMessage() = _state.update { it.copy(message = null) }

    private class ExportException(val reason: EditorMessage) : Exception(reason.name)

    /** A no-derivatives licence forbids modified copies; an untouched icon may still be exported. */
    private fun blocksModification(s: EditSession): Boolean =
        LicensePolicy.assess(s.set?.licenseSpdx).modificationOk == false && !s.params.isDefault()

    private suspend fun buildExport(s: EditSession, format: ExportFormat): ShareRequest {
        val params = s.params.sanitized()
        val raw = api.fetchSvg(params.svgUrl(s.icon))
        if (!SvgSafety.isSafe(raw)) throw ExportException(EditorMessage.EXPORT_UNSAFE)

        val meta = provenance(s)
        val composed = SvgComposer.compose(raw, params.sizePx, params.paddingPct, params.bgHex, meta)
            ?: throw ExportException(EditorMessage.EXPORT_FAILED)

        val dir = File(getApplication<Application>().cacheDir, "exports").apply { mkdirs() }
        cleanOld(dir)
        val base = "zunivo-${s.icon.prefix}-${s.icon.name}"
        val credit = creditLine(s)
        return when (format) {
            ExportFormat.SVG -> {
                val f = File(dir, "$base.svg")
                f.writeText(composed, Charsets.UTF_8)
                ShareRequest(f, "image/svg+xml", credit)
            }
            ExportFormat.PNG -> {
                val svg = SVG.getFromString(composed)
                val bmp = Bitmap.createBitmap(params.sizePx, params.sizePx, Bitmap.Config.ARGB_8888)
                svg.renderToCanvas(Canvas(bmp))
                val f = File(dir, "$base.png")
                f.outputStream().use { out -> bmp.compress(Bitmap.CompressFormat.PNG, 100, out) }
                bmp.recycle()
                ShareRequest(f, "image/png", credit)
            }
        }
    }

    private fun provenance(s: EditSession): String {
        val set = s.set
        val license = listOfNotNull(set?.licenseTitle, set?.licenseSpdx).distinct().joinToString(" / ")
        return "Source: Iconify (api.iconify.design); icon ${s.icon.id}; set ${set?.name ?: s.icon.prefix}; " +
            "original author: ${set?.authorName ?: "unknown"}; " +
            "license: ${license.ifBlank { "unknown - verify before redistribution" }}" +
            (set?.licenseUrl?.let { " ($it)" } ?: "") + "; " +
            "adapted copy (colour/padding/background) created by the user with Zunivo - the original author did not make these changes."
    }

    private fun creditLine(s: EditSession): String {
        val set = s.set
        val license = set?.licenseTitle ?: set?.licenseSpdx
        return if (set?.authorName != null && license != null) {
            "Icon ${s.icon.name} from the ${set.name} set by ${set.authorName}, licensed under $license. Adapted with Zunivo."
        } else {
            "Icon ${s.icon.id} from Iconify. Author or license not provided - verify before redistributing. Adapted with Zunivo."
        }
    }

    private fun cleanOld(dir: File) {
        val cutoff = System.currentTimeMillis() - 24L * 60 * 60 * 1000
        dir.listFiles()?.forEach { if (it.lastModified() < cutoff) it.delete() }
    }
}
