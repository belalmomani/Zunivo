package com.zunivo.app.feature.store

import android.app.Application
import android.graphics.BitmapFactory
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.zunivo.app.data.local.DraftStore
import com.zunivo.app.data.remote.GitHubApi
import com.zunivo.app.data.remote.GitHubException
import com.zunivo.app.domain.Decision
import com.zunivo.app.domain.DraftChange
import com.zunivo.app.domain.DraftItem
import com.zunivo.app.domain.DraftMerge
import com.zunivo.app.domain.DraftOps
import com.zunivo.app.domain.FetchStatus
import com.zunivo.app.domain.FileNames
import com.zunivo.app.domain.Fetched
import com.zunivo.app.domain.GitHubHosts
import com.zunivo.app.domain.GitHubUrl
import com.zunivo.app.domain.ImportMode
import com.zunivo.app.domain.ItemKind
import com.zunivo.app.domain.ItemNote
import com.zunivo.app.domain.RepoInfo
import com.zunivo.app.domain.RepoRef
import com.zunivo.app.domain.StoreDraft
import com.zunivo.app.domain.Verification
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest
import java.util.UUID

enum class StoreError { INVALID_URL, NOT_FOUND, RATE_LIMITED, OFFLINE, TIMEOUT, SERVER, BAD_DATA }

enum class StoreMessage {
    APPROVED, DELETED, FILES_DELETED, DOWNLOADED_VERIFIED, DOWNLOADED_UNVERIFIED, DOWNLOAD_FAILED,
    CHECKSUM_MISMATCH, TOO_LARGE, UPDATED, NO_CHANGES,
}

data class ImportPreview(val ref: RepoRef, val info: RepoInfo)
data class DownloadProgress(val bytes: Long, val total: Long)
data class PendingUpdate(val draftId: String, val changes: List<DraftChange>, val fresh: List<DraftItem>, val sizeKb: Long?)

data class StoreUiState(
    val drafts: List<StoreDraft> = emptyList(),
    val loaded: Boolean = false,
    val url: String = "",
    val preview: ImportPreview? = null,
    val loading: Boolean = false,
    val error: StoreError? = null,
    val openedId: String? = null,
    val downloads: Map<String, DownloadProgress> = emptyMap(),
    val pendingUpdate: PendingUpdate? = null,
    val message: StoreMessage? = null,
) {
    fun opened(): StoreDraft? = drafts.firstOrNull { it.id == openedId }
}

class StoreViewModel(app: Application) : AndroidViewModel(app) {
    private val api = GitHubApi()
    private val store = DraftStore(app)
    private val _state = MutableStateFlow(StoreUiState())
    val state: StateFlow<StoreUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val list = withContext(Dispatchers.IO) { store.loadAll() }
            _state.update { it.copy(drafts = list, loaded = true) }
        }
    }

    // ---------- import ----------
    fun setUrl(text: String) = _state.update { it.copy(url = text.take(300), error = null, preview = null) }

    /** Step 1: validate the link and show owner/name/size before anything is downloaded. */
    fun checkUrl() {
        val ref = GitHubUrl.parse(_state.value.url)
        if (ref == null) {
            _state.update { it.copy(error = StoreError.INVALID_URL, preview = null) }
            return
        }
        _state.update { it.copy(loading = true, error = null, preview = null) }
        viewModelScope.launch {
            try {
                val info = api.repo(ref)
                _state.update { it.copy(loading = false, preview = ImportPreview(ref, info)) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: GitHubException) {
                _state.update { it.copy(loading = false, error = errorOf(e)) }
            }
        }
    }

    /** Step 2: build the draft (metadata, or metadata + small files). APKs and archives are never fetched here. */
    fun importNow(mode: ImportMode) {
        val p = _state.value.preview ?: return
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            try {
                val items = fetchItems(p.ref, p.info)
                val id = UUID.randomUUID().toString()
                val withFiles = if (mode == ImportMode.INFO_AND_FILES) withContext(Dispatchers.IO) { downloadImages(id, items) } else items
                val now = System.currentTimeMillis()
                val draft = StoreDraft(
                    id = id, repo = p.ref, createdAt = now, updatedAt = now, approved = false, approvedAt = 0L, mode = mode,
                    sizeKb = p.info.sizeKb, defaultBranch = p.info.defaultBranch,
                    displayName = p.info.name, displayDescription = p.info.description.orEmpty(),
                    categories = emptyList(), coverItemId = null, items = withFiles,
                )
                persist(draft)
                _state.update { it.copy(drafts = listOf(draft) + it.drafts, openedId = id, preview = null, url = "", loading = false) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: GitHubException) {
                _state.update { it.copy(loading = false, error = errorOf(e)) }
            }
        }
    }

    private suspend fun fetchItems(ref: RepoRef, info: RepoInfo): List<DraftItem> {
        val readme: Fetched<String> = api.readme(ref)
        val releases = api.releases(ref)
        val changelog = api.fileText(ref, "CHANGELOG.md")
        return DraftBuilder.items(ref, info, readme, releases, changelog)
    }

    private suspend fun downloadImages(id: String, items: List<DraftItem>): List<DraftItem> {
        val dir = store.filesDir(id)
        var budget = MAX_IMAGES_TOTAL
        var n = 0
        return items.map { item ->
            if (item.kind != ItemKind.IMAGE || item.fetch != FetchStatus.FETCHED || item.value == null) return@map item
            val ext = item.value.substringBefore('?').substringAfterLast('.', "").lowercase().takeIf { it in IMAGE_EXT } ?: "img"
            val name = "img-${++n}.$ext"
            val file = File(dir, name)
            try {
                val bytes = api.download(item.value, file, minOf(MAX_IMAGE, budget)) { _, _ -> }
                val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(file.path, opts)
                if (opts.outWidth <= 0 || opts.outHeight <= 0) {
                    file.delete()
                    item.copy(fetch = FetchStatus.FAILED, note = ItemNote.NOT_AN_IMAGE)
                } else {
                    budget -= bytes
                    item.copy(localFile = name, sizeBytes = bytes)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: GitHubException.TooLarge) {
                item.copy(fetch = FetchStatus.FAILED, note = ItemNote.TOO_LARGE)
            } catch (e: GitHubException) {
                item.copy(fetch = FetchStatus.FAILED, note = ItemNote.DOWNLOAD_FAILED)
            }
        }
    }

    // ---------- navigation ----------
    fun open(id: String) = _state.update { it.copy(openedId = id) }
    fun close() = _state.update { it.copy(openedId = null, pendingUpdate = null) }
    fun consumeMessage() = _state.update { it.copy(message = null) }
    fun clearImport() = _state.update { it.copy(url = "", preview = null, error = null, loading = false) }

    // ---------- review ----------
    private fun edit(id: String, change: (StoreDraft) -> StoreDraft) {
        val old = _state.value.drafts.firstOrNull { it.id == id } ?: return
        val next = change(old)
        if (next === old) return
        _state.update { st -> st.copy(drafts = st.drafts.map { if (it.id == id) next else it }) }
        persist(next)
    }

    private fun now() = System.currentTimeMillis()

    fun decide(id: String, itemId: String, d: Decision) = edit(id) { DraftOps.setDecision(it, itemId, d, now()) }
    fun acceptAll(id: String) = edit(id) { DraftOps.acceptAll(it, now()) }
    fun rejectAll(id: String) = edit(id) { DraftOps.rejectAll(it, now()) }
    fun resetDecisions(id: String) = edit(id) { DraftOps.reset(it, now()) }
    fun moveItem(id: String, itemId: String, delta: Int) = edit(id) { DraftOps.move(it, itemId, delta, now()) }
    fun setCover(id: String, itemId: String?) = edit(id) { DraftOps.setCover(it, itemId, now()) }
    fun setDisplayName(id: String, name: String) = edit(id) { it.copy(displayName = name.trim().take(80), updatedAt = now()) }
    fun setDisplayDescription(id: String, text: String) = edit(id) { it.copy(displayDescription = text.trim().take(1000), updatedAt = now()) }

    fun toggleCategory(id: String, category: String) = edit(id) {
        val next = if (category in it.categories) it.categories - category else it.categories + category
        it.copy(categories = next, updatedAt = now())
    }

    /** Only after the user's explicit confirmation in the UI. */
    fun approve(id: String) {
        val d = _state.value.drafts.firstOrNull { it.id == id } ?: return
        if (!DraftOps.canApprove(d)) return
        edit(id) { DraftOps.approve(it, now()) }
        _state.update { it.copy(message = StoreMessage.APPROVED) }
    }

    // ---------- files ----------
    fun fileOf(draft: StoreDraft, item: DraftItem): File? = item.localFile?.let { store.resolve(draft.id, it) }

    /** Downloads an APK or the source archive. The UI must have shown the warning and size first. */
    fun download(id: String, itemId: String) {
        val draft = _state.value.drafts.firstOrNull { it.id == id } ?: return
        val item = draft.items.firstOrNull { it.id == itemId } ?: return
        val url = item.value ?: return
        if ((item.kind != ItemKind.APK && item.kind != ItemKind.SOURCE_ARCHIVE) || !GitHubHosts.isAllowedUrl(url)) return
        if (_state.value.downloads.containsKey(itemId)) return
        val limit = if (item.kind == ItemKind.APK) MAX_APK else MAX_ARCHIVE
        val fileName = if (item.kind == ItemKind.APK) FileNames.sanitize(url.substringAfterLast('/').substringBefore('?')).let { if (it.endsWith(".apk", true)) it else "$it.apk" }
        else "source-${FileNames.sanitize(draft.repo.repo)}.zip"
        _state.update { it.copy(downloads = it.downloads + (itemId to DownloadProgress(0, item.sizeBytes ?: -1))) }
        viewModelScope.launch {
            try {
                val dir = store.filesDir(id)
                val file = File(dir, fileName)
                withContext(Dispatchers.IO) {
                    api.download(url, file, limit) { done, total ->
                        _state.update { st -> st.copy(downloads = st.downloads + (itemId to DownloadProgress(done, total))) }
                    }
                }
                val hash = withContext(Dispatchers.IO) { sha256(file) }
                val expected = item.expectedSha256
                if (expected != null && !expected.equals(hash, ignoreCase = true)) {
                    file.delete()
                    replaceItem(id, item.copy(note = ItemNote.CHECKSUM_MISMATCH, localFile = null, computedSha256 = hash, verification = null))
                    _state.update { it.copy(message = StoreMessage.CHECKSUM_MISMATCH) }
                } else {
                    val v = if (expected != null) Verification.VERIFIED else Verification.UNVERIFIED
                    replaceItem(id, item.copy(note = null, localFile = fileName, computedSha256 = hash, verification = v))
                    _state.update { it.copy(message = if (v == Verification.VERIFIED) StoreMessage.DOWNLOADED_VERIFIED else StoreMessage.DOWNLOADED_UNVERIFIED) }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: GitHubException.TooLarge) {
                _state.update { it.copy(message = StoreMessage.TOO_LARGE) }
            } catch (e: GitHubException) {
                _state.update { it.copy(message = StoreMessage.DOWNLOAD_FAILED) }
            } finally {
                _state.update { it.copy(downloads = it.downloads - itemId) }
            }
        }
    }

    private fun replaceItem(id: String, item: DraftItem) =
        edit(id) { d -> d.copy(items = d.items.map { if (it.id == item.id) item else it }, updatedAt = now()) }

    /** Frees phone storage but keeps the project data. */
    fun deleteLocalFiles(id: String) {
        viewModelScope.launch { withContext(Dispatchers.IO) { store.deleteFiles(id) } }
        edit(id) { d -> d.copy(items = d.items.map { it.copy(localFile = null, verification = null, computedSha256 = null) }, updatedAt = now()) }
        _state.update { it.copy(message = StoreMessage.FILES_DELETED) }
    }

    fun deleteDraft(id: String) {
        viewModelScope.launch { withContext(Dispatchers.IO) { store.delete(id) } }
        _state.update { st -> st.copy(drafts = st.drafts.filter { it.id != id }, openedId = if (st.openedId == id) null else st.openedId, message = StoreMessage.DELETED) }
    }

    // ---------- update from GitHub ----------
    fun refresh(id: String) {
        val d = _state.value.drafts.firstOrNull { it.id == id } ?: return
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            try {
                val info = api.repo(d.repo)
                val fresh = fetchItems(d.repo, info)
                val changes = DraftMerge.diff(d.items, fresh)
                if (changes.isEmpty()) {
                    _state.update { it.copy(loading = false, message = StoreMessage.NO_CHANGES) }
                } else {
                    _state.update { it.copy(loading = false, pendingUpdate = PendingUpdate(id, changes, fresh, info.sizeKb)) }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: GitHubException) {
                _state.update { it.copy(loading = false, error = errorOf(e)) }
            }
        }
    }

    /** Applies a refresh only after the user has seen the list of changes and confirmed. */
    fun applyUpdate() {
        val u = _state.value.pendingUpdate ?: return
        val d = _state.value.drafts.firstOrNull { it.id == u.draftId } ?: return
        val (merged, obsolete) = DraftMerge.merge(d, u.fresh, u.sizeKb, now())
        viewModelScope.launch { withContext(Dispatchers.IO) { obsolete.forEach { store.deleteOne(d.id, it) } } }
        _state.update { st -> st.copy(drafts = st.drafts.map { if (it.id == d.id) merged else it }, pendingUpdate = null, message = StoreMessage.UPDATED) }
        persist(merged)
    }

    fun cancelUpdate() = _state.update { it.copy(pendingUpdate = null) }

    // ---------- internals ----------
    private fun persist(d: StoreDraft) {
        viewModelScope.launch { withContext(Dispatchers.IO) { store.save(d) } }
    }

    private fun sha256(file: File): String {
        val md = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buf = ByteArray(32 * 1024)
            while (true) {
                val n = input.read(buf)
                if (n < 0) break
                md.update(buf, 0, n)
            }
        }
        return md.digest().joinToString("") { "%02x".format(it) }
    }

    private fun errorOf(e: GitHubException): StoreError = when (e) {
        is GitHubException.NotFound -> StoreError.NOT_FOUND
        is GitHubException.RateLimited -> StoreError.RATE_LIMITED
        is GitHubException.Offline -> StoreError.OFFLINE
        is GitHubException.Timeout -> StoreError.TIMEOUT
        is GitHubException.Http -> StoreError.SERVER
        is GitHubException.BadData, is GitHubException.Blocked, is GitHubException.TooLarge -> StoreError.BAD_DATA
    }

    private companion object {
        const val MAX_IMAGE = 3L * 1024 * 1024
        const val MAX_IMAGES_TOTAL = 10L * 1024 * 1024
        const val MAX_APK = 200L * 1024 * 1024
        const val MAX_ARCHIVE = 100L * 1024 * 1024
        val IMAGE_EXT = setOf("png", "jpg", "jpeg", "gif", "webp")
    }
}
