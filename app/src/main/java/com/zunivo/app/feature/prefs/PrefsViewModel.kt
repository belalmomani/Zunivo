package com.zunivo.app.feature.prefs

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.zunivo.app.data.local.PrefsStore
import com.zunivo.app.domain.PrefsOps
import com.zunivo.app.domain.UserPrefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Local-only preferences: review, change and clear everything from the Preferences screen. */
class PrefsViewModel(app: Application) : AndroidViewModel(app) {
    private val store = PrefsStore(app)
    private val lock = Mutex()
    private val _prefs = MutableStateFlow(UserPrefs())
    val prefs: StateFlow<UserPrefs> = _prefs.asStateFlow()

    init {
        viewModelScope.launch {
            val loaded = withContext(Dispatchers.IO) { store.load() }
            if (_prefs.value == UserPrefs()) _prefs.value = loaded
        }
    }

    private fun change(f: (UserPrefs) -> UserPrefs) {
        val next = f(_prefs.value)
        if (next == _prefs.value) return
        _prefs.value = next
        viewModelScope.launch { lock.withLock { withContext(Dispatchers.IO) { store.save(next) } } }
    }

    fun recordCategory(name: String) = change { PrefsOps.recordCategory(it, name) }
    fun recordColor(hex: String?) = change { PrefsOps.recordColor(it, hex) }
    fun recordSearch(query: String) = change { PrefsOps.recordSearch(it, query) }
    fun setLearning(on: Boolean) = change { PrefsOps.setLearning(it, on) }
    fun setHistory(on: Boolean) = change { PrefsOps.setHistory(it, on) }
    fun removeHistory(query: String) = change { PrefsOps.removeHistory(it, query) }
    fun clearHistory() = change { PrefsOps.clearHistory(it) }
    fun clearLearned() = change { PrefsOps.clearLearned(it) }
}
