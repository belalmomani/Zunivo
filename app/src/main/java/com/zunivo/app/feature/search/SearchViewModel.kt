package com.zunivo.app.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zunivo.app.data.remote.ApiException
import com.zunivo.app.data.remote.IconifyApi
import com.zunivo.app.data.repo.SearchRepository
import com.zunivo.app.domain.IconItem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class SearchStatus { IDLE, LOADING, SUCCESS, EMPTY, ERROR }
enum class ErrorKind { OFFLINE, TIMEOUT, SERVER, BAD_DATA }

data class SearchUiState(
    val query: String = "",
    val category: Category? = null,
    val style: StyleFilter = StyleFilter.ALL,
    val status: SearchStatus = SearchStatus.IDLE,
    val items: List<IconItem> = emptyList(),
    val shown: Int = 0,
    val hasMore: Boolean = false,
    val loadingMore: Boolean = false,
    val error: ErrorKind? = null,
    val serverCode: Int = 0,
    val searchedAs: List<String> = emptyList(),
    val untranslated: Boolean = false,
    val selected: IconItem? = null,
)

class SearchViewModel : ViewModel() {
    private val repo = SearchRepository(IconifyApi())
    private val _state = MutableStateFlow(SearchUiState())
    val state: StateFlow<SearchUiState> = _state.asStateFlow()
    private var job: Job? = null

    fun onQueryChange(text: String) {
        _state.update { it.copy(query = text, category = null) }
        schedule(DEBOUNCE_MS)
    }

    fun onCategory(category: Category?) {
        _state.update { it.copy(category = category, query = "") }
        schedule(0)
    }

    fun onStyle(mode: StyleFilter) {
        _state.update { it.copy(style = mode, shown = PAGE) }
    }

    fun retry() = schedule(0)

    fun select(item: IconItem?) {
        _state.update { it.copy(selected = item) }
    }

    fun showMore() {
        val s = _state.value
        val visibleCount = s.items.applyStyle(s.style).size
        if (s.shown < visibleCount) {
            _state.update { it.copy(shown = it.shown + PAGE) }
        } else if (s.hasMore && !s.loadingMore) {
            job?.cancel()
            job = viewModelScope.launch { runSearch(SearchRepository.MAX_LIMIT, append = true) }
        }
    }

    private fun schedule(debounceMs: Long) {
        job?.cancel()
        job = viewModelScope.launch {
            if (debounceMs > 0) delay(debounceMs)
            runSearch(FIRST_LIMIT, append = false)
        }
    }

    private suspend fun runSearch(limit: Int, append: Boolean) {
        val s = _state.value
        val queries = if (s.category != null) CategorySeeds.seeds(s.category) else QueryExpander.expand(s.query)
        if (queries.isEmpty()) {
            val typed = s.query.trim().length >= 2
            _state.update {
                it.copy(
                    status = if (typed) SearchStatus.EMPTY else SearchStatus.IDLE,
                    items = emptyList(), shown = 0, hasMore = false, error = null,
                    searchedAs = emptyList(), untranslated = typed,
                )
            }
            return
        }
        _state.update {
            if (append) it.copy(loadingMore = true)
            else it.copy(status = SearchStatus.LOADING, error = null, searchedAs = queries, untranslated = false)
        }
        try {
            val outcome = repo.search(queries, limit)
            _state.update {
                it.copy(
                    items = outcome.items,
                    shown = if (append) it.shown else minOf(PAGE, outcome.items.size),
                    hasMore = outcome.hasMore && !append,
                    loadingMore = false,
                    status = if (outcome.items.isEmpty()) SearchStatus.EMPTY else SearchStatus.SUCCESS,
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: ApiException) {
            val kind = when (e) {
                is ApiException.Offline -> ErrorKind.OFFLINE
                is ApiException.Timeout -> ErrorKind.TIMEOUT
                is ApiException.Http -> ErrorKind.SERVER
                is ApiException.BadData -> ErrorKind.BAD_DATA
            }
            val code = (e as? ApiException.Http)?.code ?: 0
            _state.update {
                if (append) it.copy(loadingMore = false)   // keep what is already on screen
                else it.copy(status = SearchStatus.ERROR, error = kind, serverCode = code)
            }
        }
    }

    private companion object {
        const val DEBOUNCE_MS = 400L
        const val FIRST_LIMIT = 64
        const val PAGE = 48
    }
}
