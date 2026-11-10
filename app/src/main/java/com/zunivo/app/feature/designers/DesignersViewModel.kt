package com.zunivo.app.feature.designers

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.zunivo.app.data.local.SecureStore
import com.zunivo.app.data.remote.ApiException
import com.zunivo.app.data.remote.IconifyApi
import com.zunivo.app.data.remote.SupabaseApi
import com.zunivo.app.data.remote.SupabaseConfig
import com.zunivo.app.data.remote.SupabaseException
import com.zunivo.app.domain.DesignerProfile
import com.zunivo.app.domain.DesignerRules
import com.zunivo.app.domain.DesignerWork
import com.zunivo.app.domain.IconCollection
import com.zunivo.app.domain.IconItem
import com.zunivo.app.domain.IconRef
import com.zunivo.app.domain.PublishPlanner
import com.zunivo.app.domain.Session
import com.zunivo.app.domain.ValidationError
import com.zunivo.app.domain.Visibility
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.UUID

enum class DesignersError { OFFLINE, TIMEOUT, UNAUTHORIZED, RATE_LIMITED, REJECTED, SERVER, BAD_DATA, INVALID_INPUT }

enum class DesignersMessage { CODE_SENT, SIGNED_IN, SIGNED_OUT, PROFILE_SAVED, PAGE_DELETED, PUBLISHED, WORK_UPDATED, WORK_DELETED, ADDED_TO_PACKS, REPORTED, NOTHING_TO_PUBLISH }

data class DesignersUiState(
    val enabled: Boolean = false,
    val loading: Boolean = false,
    val error: DesignersError? = null,
    val validation: ValidationError? = null,
    val message: DesignersMessage? = null,
    val designers: List<DesignerProfile> = emptyList(),
    val works: List<DesignerWork> = emptyList(),
    val counts: Map<String, Int> = emptyMap(),
    val browseLoaded: Boolean = false,
    val session: Session? = null,
    val awaitingCode: Boolean = false,
    val email: String = "",
    val myProfile: DesignerProfile? = null,
    val myLoaded: Boolean = false,
    val myWorks: List<DesignerWork> = emptyList(),
    val openedWorkId: String? = null,
) {
    fun designerName(id: String): String = designers.firstOrNull { it.id == id }?.displayName ?: ""
}

class DesignersViewModel(app: Application) : AndroidViewModel(app) {
    private val api = SupabaseApi()
    private val iconify = IconifyApi()
    private val secure = SecureStore(app)
    private val _state = MutableStateFlow(DesignersUiState(enabled = SupabaseConfig.enabled))
    val state: StateFlow<DesignersUiState> = _state.asStateFlow()

    init {
        if (SupabaseConfig.enabled) {
            viewModelScope.launch {
                val saved = withContext(Dispatchers.IO) { secure.read() }?.let { readSession(it) }
                if (saved != null) _state.update { it.copy(session = saved) }
                loadBrowse()
            }
        }
    }

    // ---------------- browse (public data) ----------------
    fun loadBrowse() = act {
        val ds = api.designers()
        val ws = api.publicWorks()
        val counts = try { api.downloadCounts() } catch (e: SupabaseException) { emptyMap() }   // counts are optional
        _state.update { it.copy(designers = ds, works = ws, counts = counts, browseLoaded = true) }
    }

    fun openWork(id: String?) = _state.update { it.copy(openedWorkId = id) }

    // ---------------- sign in (email one-time code) ----------------
    fun requestCode(email: String) {
        val e = email.trim()
        if (!e.matches(Regex("[^@\\s]+@[^@\\s]+\\.[^@\\s]+"))) { invalid(ValidationError.NAME); return }
        act {
            api.sendCode(e)
            _state.update { it.copy(awaitingCode = true, email = e, message = DesignersMessage.CODE_SENT) }
        }
    }

    fun verifyCode(code: String) {
        val c = code.trim()
        if (c.length !in 4..10) { invalid(ValidationError.NAME); return }
        act {
            val s = api.verifyCode(_state.value.email, c)
            withContext(Dispatchers.IO) { secure.write(writeSession(s)) }
            _state.update { it.copy(session = s, awaitingCode = false, message = DesignersMessage.SIGNED_IN) }
            loadMineInternal(s)
        }
    }

    fun signOut() {
        val s = _state.value.session
        viewModelScope.launch {
            if (s != null) api.signOut(s.accessToken)
            withContext(Dispatchers.IO) { secure.clear() }
            _state.update { it.copy(session = null, myProfile = null, myWorks = emptyList(), myLoaded = false, awaitingCode = false, message = DesignersMessage.SIGNED_OUT) }
        }
    }

    // ---------------- my page ----------------
    fun loadMine() = act { _state.value.session?.let { loadMineInternal(fresh()) } }

    private suspend fun loadMineInternal(s: Session) {
        val p = api.myProfile(s.userId, s.accessToken)
        val ws = if (p != null) api.myWorks(s.userId, s.accessToken) else emptyList()
        _state.update { it.copy(myProfile = p, myWorks = ws, myLoaded = true) }
    }

    fun saveProfile(name: String, bio: String, specialtiesRaw: String) {
        val specs = DesignerRules.cleanSpecialties(specialtiesRaw)
        DesignerRules.validateProfile(name, bio, specs)?.let { invalid(it); return }
        act {
            val s = fresh()
            val p = DesignerProfile(s.userId, name.trim(), bio.trim().ifEmpty { null }, specs)
            api.saveProfile(p, s.accessToken)
            _state.update { it.copy(myProfile = p, message = DesignersMessage.PROFILE_SAVED) }
            loadBrowse()
        }
    }

    /** Deletes the public profile; works and download events cascade on the server. The sign-in account itself is kept. */
    fun deleteMyPage() = act {
        val s = fresh()
        api.deleteProfile(s.userId, s.accessToken)
        _state.update { it.copy(myProfile = null, myWorks = emptyList(), message = DesignersMessage.PAGE_DELETED) }
        loadBrowse()
    }

    /** Publishes only icons whose license allows redistribution. New works start PRIVATE. */
    fun publishPack(pack: IconCollection, title: String, description: String) {
        val plan = PublishPlanner.plan(pack)
        if (plan.iconIds.isEmpty()) { _state.update { it.copy(message = DesignersMessage.NOTHING_TO_PUBLISH) }; return }
        DesignerRules.validateWork(title, description, plan.iconIds)?.let { invalid(it); return }
        act {
            val s = fresh()
            val w = DesignerWork(UUID.randomUUID().toString(), s.userId, title.trim(), description.trim().ifEmpty { null }, plan.iconIds, Visibility.PRIVATE, null)
            api.saveWork(w, s.accessToken)
            _state.update { it.copy(message = DesignersMessage.PUBLISHED) }
            loadMineInternal(s)
        }
    }

    fun setVisibility(work: DesignerWork, public: Boolean) = act {
        val s = fresh()
        api.saveWork(work.copy(visibility = if (public) Visibility.PUBLIC else Visibility.PRIVATE), s.accessToken)
        _state.update { it.copy(message = DesignersMessage.WORK_UPDATED) }
        loadMineInternal(s)
        loadBrowse()
    }

    fun deleteWork(id: String) = act {
        val s = fresh()
        api.deleteWork(id, s.accessToken)
        _state.update { it.copy(message = DesignersMessage.WORK_DELETED) }
        loadMineInternal(s)
        loadBrowse()
    }

    // ---------------- using someone's public work ----------------
    /**
     * Looks up each set's author and license from Iconify (not from the publisher), then hands the icons over.
     * If signed in, a real download event is recorded.
     */
    fun addToPacks(work: DesignerWork, onItems: (List<IconItem>) -> Unit) = act {
        val refs = work.iconIds.mapNotNull { IconRef.parse(it) }
        val sets = iconify.collections(refs.map { it.prefix })
        onItems(refs.map { IconItem(it, sets[it.prefix]) })
        _state.value.session?.let { s0 ->
            try {
                val s = fresh()
                api.recordDownload(work.id, s.userId, s.accessToken)
                api.downloadCounts().let { c -> _state.update { it.copy(counts = c) } }
            } catch (e: SupabaseException) { /* the download itself already worked; the counter is best effort */ }
        }
        _state.update { it.copy(message = DesignersMessage.ADDED_TO_PACKS) }
    }

    fun report(workId: String, reason: String) {
        if (reason.trim().length !in 10..2000) { invalid(ValidationError.DESCRIPTION); return }
        act {
            val s = fresh()
            api.report(workId, s.userId, reason.trim(), s.accessToken)
            _state.update { it.copy(message = DesignersMessage.REPORTED) }
        }
    }

    fun consumeMessage() = _state.update { it.copy(message = null) }
    fun clearIssues() = _state.update { it.copy(error = null, validation = null) }

    // ---------------- internals ----------------
    private fun invalid(v: ValidationError) = _state.update { it.copy(validation = v, error = null) }

    /** Runs a network action with loading/error handling. */
    private fun act(block: suspend () -> Unit) {
        _state.update { it.copy(loading = true, error = null, validation = null) }
        viewModelScope.launch {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: SupabaseException) {
                _state.update { it.copy(error = errorOf(e)) }
            } catch (e: ApiException) {
                _state.update { it.copy(error = if (e is ApiException.Offline) DesignersError.OFFLINE else DesignersError.SERVER) }
            } finally {
                _state.update { it.copy(loading = false) }
            }
        }
    }

    /** The current session, refreshed first when it is about to expire. */
    private suspend fun fresh(): Session {
        val s = _state.value.session ?: throw SupabaseException.Unauthorized()
        if (!s.expiresSoon(System.currentTimeMillis())) return s
        return try {
            val n = api.refresh(s.refreshToken)
            withContext(Dispatchers.IO) { secure.write(writeSession(n)) }
            _state.update { it.copy(session = n) }
            n
        } catch (e: SupabaseException.Rejected) {
            withContext(Dispatchers.IO) { secure.clear() }
            _state.update { it.copy(session = null) }
            throw SupabaseException.Unauthorized()
        }
    }

    private fun errorOf(e: SupabaseException): DesignersError = when (e) {
        is SupabaseException.Offline -> DesignersError.OFFLINE
        is SupabaseException.Timeout -> DesignersError.TIMEOUT
        is SupabaseException.Unauthorized -> DesignersError.UNAUTHORIZED
        is SupabaseException.RateLimited -> DesignersError.RATE_LIMITED
        is SupabaseException.Rejected -> DesignersError.REJECTED
        is SupabaseException.Http, is SupabaseException.NotConfigured -> DesignersError.SERVER
        is SupabaseException.BadData -> DesignersError.BAD_DATA
    }

    private fun writeSession(s: Session): String =
        JSONObject().put("a", s.accessToken).put("r", s.refreshToken).put("u", s.userId).put("e", s.expiresAtMs).toString()

    private fun readSession(text: String): Session? = try {
        val o = JSONObject(text)
        Session(o.getString("a"), o.getString("r"), o.getString("u"), o.getLong("e"))
    } catch (e: Exception) { null }
}
