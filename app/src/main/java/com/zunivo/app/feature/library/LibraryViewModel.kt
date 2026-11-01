package com.zunivo.app.feature.library

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.zunivo.app.data.local.LibraryStore
import com.zunivo.app.data.remote.ApiException
import com.zunivo.app.data.remote.IconifyApi
import com.zunivo.app.data.repo.SearchRepository
import com.zunivo.app.domain.CollectionKind
import com.zunivo.app.domain.HexColor
import com.zunivo.app.domain.IconCollection
import com.zunivo.app.domain.IconItem
import com.zunivo.app.domain.IconRef
import com.zunivo.app.domain.IconSnapshot
import com.zunivo.app.domain.Library
import com.zunivo.app.domain.LibraryOps
import com.zunivo.app.feature.editor.ShareRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.UUID

enum class LibraryMessage {
    CREATED, RENAMED, DUPLICATED, DELETED, ADDED, ALREADY_THERE, FULL, FAVORITE_ADDED, FAVORITE_REMOVED,
    EXPORTED, EXPORT_EMPTY, EXPORT_FAILED, OFFLINE, STARTER_FAILED,
}

data class LibraryUiState(
    val library: Library = Library(),
    val loaded: Boolean = false,
    val openedId: String? = null,
    val busy: Boolean = false,
    val message: LibraryMessage? = null,
    val args: List<Int> = emptyList(),
    val share: ShareRequest? = null,
) {
    fun collection(id: String?): IconCollection? = library.collections.firstOrNull { it.id == id }
    fun isFavorite(ref: IconRef): Boolean = library.favorites.any { it.icon == ref }
}

class LibraryViewModel(app: Application) : AndroidViewModel(app) {
    private val store = LibraryStore(app)
    private val api = IconifyApi()
    private val searchRepo = SearchRepository(api)
    private val exporter = PackExporter(app, api)
    private val saveLock = Mutex()
    private val _state = MutableStateFlow(LibraryUiState())
    val state: StateFlow<LibraryUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val lib = withContext(Dispatchers.IO) { store.load() }
            _state.update { it.copy(library = lib, loaded = true) }
        }
    }

    // ---- favourites ----
    fun toggleFavorite(item: IconItem) {
        val had = _state.value.isFavorite(item.ref)
        mutate { LibraryOps.toggleFavorite(it, IconSnapshot.of(item)) }
        say(if (had) LibraryMessage.FAVORITE_REMOVED else LibraryMessage.FAVORITE_ADDED)
    }

    // ---- collections ----
    fun createCollection(kind: CollectionKind, name: String, open: Boolean = false): String {
        val id = UUID.randomUUID().toString()
        mutate { LibraryOps.create(it, id, kind, name, "", System.currentTimeMillis()) }
        if (open) _state.update { it.copy(openedId = id) }
        say(LibraryMessage.CREATED)
        return id
    }

    fun createPackFrom(name: String, items: List<IconItem>, colorHex: String?): String {
        val id = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        mutate { lib ->
            var next = LibraryOps.create(lib, id, CollectionKind.PACK, name, "", now)
            next = LibraryOps.update(next, id) { c ->
                var cur = c.copy(colorHex = HexColor.normalize(colorHex))
                items.forEach { cur = LibraryOps.addEntry(cur, IconSnapshot.of(it), now) }
                cur
            }
            next
        }
        _state.update { it.copy(openedId = id) }
        say(LibraryMessage.CREATED)
        return id
    }

    fun addToCollection(id: String, item: IconItem) {
        val c = _state.value.collection(id) ?: return
        val updated = LibraryOps.addEntry(c, IconSnapshot.of(item), System.currentTimeMillis())
        if (updated === c) {
            val full = c.entries.size >= LibraryOps.MAX_ENTRIES && c.entries.none { it.snapshot.icon == item.ref }
            say(if (full) LibraryMessage.FULL else LibraryMessage.ALREADY_THERE, if (full) listOf(LibraryOps.MAX_ENTRIES) else emptyList())
        } else {
            mutate { LibraryOps.update(it, id) { _ -> updated } }
            say(LibraryMessage.ADDED)
        }
    }

    fun rename(id: String, name: String) {
        val clean = LibraryOps.cleanName(name)
        if (clean.isEmpty()) return
        mutate { LibraryOps.update(it, id) { c -> c.copy(name = clean, updatedAt = System.currentTimeMillis()) } }
        say(LibraryMessage.RENAMED)
    }

    fun duplicate(id: String, suffix: String) {
        val c = _state.value.collection(id) ?: return
        val copy = LibraryOps.duplicate(c, UUID.randomUUID().toString(), suffix, System.currentTimeMillis())
        mutate { it.copy(collections = listOf(copy) + it.collections) }
        say(LibraryMessage.DUPLICATED)
    }

    fun delete(id: String) {
        mutate { it.copy(collections = it.collections.filter { c -> c.id != id }) }
        _state.update { if (it.openedId == id) it.copy(openedId = null) else it }
        say(LibraryMessage.DELETED)
    }

    fun removeEntry(id: String, index: Int) =
        mutate { LibraryOps.update(it, id) { c -> LibraryOps.removeEntry(c, index, System.currentTimeMillis()) } }

    fun moveEntry(id: String, index: Int, delta: Int) =
        mutate { LibraryOps.update(it, id) { c -> LibraryOps.moveEntry(c, index, delta, System.currentTimeMillis()) } }

    fun renameEntryFile(id: String, index: Int, name: String) =
        mutate { LibraryOps.update(it, id) { c -> LibraryOps.renameEntryFile(c, index, name, System.currentTimeMillis()) } }

    fun setColor(id: String, hex: String?) =
        mutate { LibraryOps.update(it, id) { c -> c.copy(colorHex = HexColor.normalize(hex), updatedAt = System.currentTimeMillis()) } }

    fun open(id: String) = _state.update { it.copy(openedId = id) }
    fun closeOpened() = _state.update { it.copy(openedId = null) }
    fun consumeMessage() = _state.update { it.copy(message = null, args = emptyList()) }
    fun consumeShare() = _state.update { it.copy(share = null) }

    // ---- ready-made packs ----
    fun createStarterPack(starter: StarterPack, name: String) {
        if (_state.value.busy) return
        _state.update { it.copy(busy = true) }
        viewModelScope.launch {
            try {
                val items = searchRepo.search(starter.seeds.take(2), 64).items.take(STARTER_SIZE)
                if (items.isEmpty()) {
                    _state.update { it.copy(busy = false) }
                    say(LibraryMessage.STARTER_FAILED)
                } else {
                    createPackFrom(name, items, null)
                    _state.update { it.copy(busy = false) }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: ApiException) {
                _state.update { it.copy(busy = false) }
                say(if (e is ApiException.Offline) LibraryMessage.OFFLINE else LibraryMessage.STARTER_FAILED)
            }
        }
    }

    // ---- export ----
    fun export(id: String) {
        val c = _state.value.collection(id) ?: return
        if (_state.value.busy) return
        _state.update { it.copy(busy = true) }
        viewModelScope.launch {
            try {
                val r = withContext(Dispatchers.IO) { exporter.export(c) }
                val text = "Icon pack \"${c.name}\" created with Zunivo. Each icon keeps its original license; see LICENSES.txt inside the archive."
                _state.update {
                    it.copy(busy = false, share = ShareRequest(r.file, "application/zip", text),
                        message = LibraryMessage.EXPORTED, args = listOf(r.included, r.skipped))
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: PackExporter.NothingToExport) {
                _state.update { it.copy(busy = false, message = LibraryMessage.EXPORT_EMPTY) }
            } catch (e: PackExporter.NetworkDown) {
                _state.update { it.copy(busy = false, message = LibraryMessage.OFFLINE) }
            } catch (e: Exception) {
                _state.update { it.copy(busy = false, message = LibraryMessage.EXPORT_FAILED) }
            }
        }
    }

    // ---- internals ----
    private fun say(message: LibraryMessage, args: List<Int> = emptyList()) {
        _state.update { it.copy(message = message, args = args) }
    }

    private fun mutate(change: (Library) -> Library) {
        val next = change(_state.value.library)
        _state.update { it.copy(library = next) }
        viewModelScope.launch {
            saveLock.withLock { withContext(Dispatchers.IO) { store.save(next) } }
        }
    }

    private companion object {
        const val STARTER_SIZE = 24
    }
}
