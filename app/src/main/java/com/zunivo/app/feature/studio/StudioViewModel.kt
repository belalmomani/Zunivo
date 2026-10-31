package com.zunivo.app.feature.studio

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.zunivo.app.data.remote.ApiException
import com.zunivo.app.data.remote.IconifyApi
import com.zunivo.app.data.repo.SearchRepository
import com.zunivo.app.domain.HexColor
import com.zunivo.app.domain.IconItem
import com.zunivo.app.domain.LicensePolicy
import com.zunivo.app.feature.search.Category
import com.zunivo.app.feature.search.CategorySeeds
import com.zunivo.app.feature.search.ErrorKind
import com.zunivo.app.feature.search.StyleFilter
import com.zunivo.app.feature.search.applyStyle
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class PaletteMode { IMAGE, HARMONY }

data class Suggestion(val item: IconItem, val category: Category)

data class StudioUiState(
    val analyzing: Boolean = false,
    val analysisFailed: Boolean = false,
    val profile: ImageProfile? = null,
    val paletteMode: PaletteMode = PaletteMode.IMAGE,
    val colorHex: String? = null,
    val categories: Set<Category> = emptySet(),
    val suggestions: List<Suggestion> = emptyList(),
    val excluded: Set<String> = emptySet(),
    val searching: Boolean = false,
    val searchError: ErrorKind? = null,
) {
    fun paletteHex(): List<String> {
        val p = profile ?: return emptyList()
        val base = p.palette.map { it.argb }
        val colors = when (paletteMode) {
            PaletteMode.IMAGE -> base
            PaletteMode.HARMONY -> Harmony.build(base.first(), base.getOrNull(1))
        }
        return colors.map { HexColor.fromArgb(it) }
    }

    /** Icons the user kept. */
    fun included(): List<Suggestion> = suggestions.filter { it.item.ref.id !in excluded }

    /** How many kept icons have a licence that blocks automatic distribution (unknown / unrecognised / non-commercial). */
    fun needsLicenseReview(): Int = included().count { !LicensePolicy.assess(it.item.set?.licenseSpdx).autoDistributionOk }
}

/** Image -> palette (on device) -> icon suggestions from Iconify by category. No AI service is used. */
class StudioViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = SearchRepository(IconifyApi())
    private val _state = MutableStateFlow(StudioUiState())
    val state: StateFlow<StudioUiState> = _state.asStateFlow()

    private val candidates = HashMap<Category, List<IconItem>>()
    private val offsets = HashMap<Category, Int>()
    private var job: Job? = null

    fun analyze(uri: Uri) {
        _state.update { it.copy(analyzing = true, analysisFailed = false) }
        viewModelScope.launch {
            val profile = withContext(Dispatchers.IO) { decodeAndAnalyze(uri) }
            _state.update {
                if (profile == null) it.copy(analyzing = false, analysisFailed = true)
                else it.copy(
                    analyzing = false, profile = profile, paletteMode = PaletteMode.IMAGE,
                    colorHex = HexColor.fromArgb(profile.palette.first().argb),
                )
            }
            if (profile != null && _state.value.categories.isNotEmpty()) suggest()
        }
    }

    fun setPaletteMode(mode: PaletteMode) {
        _state.update { it.copy(paletteMode = mode) }
        val first = _state.value.paletteHex().firstOrNull()
        if (first != null) _state.update { it.copy(colorHex = first) }
    }

    fun setColor(hex: String) = _state.update { it.copy(colorHex = hex) }

    fun toggleCategory(category: Category) {
        _state.update {
            val next = if (category in it.categories) it.categories - category else it.categories + category
            it.copy(categories = if (next.size > MAX_CATEGORIES) it.categories else next)
        }
        suggest()
    }

    fun toggleExclude(id: String) {
        _state.update { it.copy(excluded = if (id in it.excluded) it.excluded - id else it.excluded + id) }
    }

    /** Shows the next group of candidates for every category (alternatives for icons the user rejected). */
    fun shuffle() {
        Category.entries.forEach { c -> offsets[c] = (offsets[c] ?: 0) + GROUP }
        _state.update { it.copy(suggestions = buildSuggestions(), excluded = emptySet()) }
    }

    fun suggest() {
        job?.cancel()
        val wanted = _state.value.categories
        if (wanted.isEmpty()) {
            _state.update { it.copy(suggestions = emptyList(), excluded = emptySet(), searching = false, searchError = null) }
            return
        }
        job = viewModelScope.launch {
            _state.update { it.copy(searching = true, searchError = null) }
            try {
                for (category in wanted) {
                    if (candidates[category] == null) {
                        val seed = CategorySeeds.seeds(category).first()
                        candidates[category] = repo.search(listOf(seed), 64).items
                    }
                }
                _state.update { it.copy(suggestions = buildSuggestions(), excluded = emptySet(), searching = false) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: ApiException) {
                val kind = when (e) {
                    is ApiException.Offline -> ErrorKind.OFFLINE
                    is ApiException.Timeout -> ErrorKind.TIMEOUT
                    is ApiException.Http -> ErrorKind.SERVER
                    is ApiException.BadData -> ErrorKind.BAD_DATA
                }
                _state.update { it.copy(searching = false, searchError = kind) }
            }
        }
    }

    private fun styleHint(): StyleFilter = when (_state.value.profile?.simplicity) {
        Simplicity.SIMPLE -> StyleFilter.OUTLINE
        Simplicity.DETAILED -> StyleFilter.FILLED
        else -> StyleFilter.ALL
    }

    private fun buildSuggestions(): List<Suggestion> {
        val hint = styleHint()
        return _state.value.categories.sortedBy { it.ordinal }.flatMap { category ->
            val all = candidates[category].orEmpty()
            val styled = all.applyStyle(hint).let { if (it.size >= GROUP) it else all }   // relax if too few match
            if (styled.isEmpty()) emptyList<Suggestion>() else {
                val start = (offsets[category] ?: 0) % styled.size
                List(minOf(GROUP, styled.size)) { i -> Suggestion(styled[(start + i) % styled.size], category) }
                    .distinctBy { it.item.ref.id }
            }
        }
    }

    private fun decodeAndAnalyze(uri: Uri): ImageProfile? {
        return try {
            val resolver = getApplication<Application>().contentResolver
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            val w = bounds.outWidth
            val h = bounds.outHeight
            if (w <= 0 || h <= 0 || w.toLong() * h > MAX_SOURCE_PIXELS) return null
            var sample = 1
            while (w / sample > 512 || h / sample > 512) sample *= 2
            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            val decoded = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) } ?: return null
            val scale = minOf(1f, ANALYSIS_SIDE.toFloat() / maxOf(decoded.width, decoded.height))
            val tw = maxOf(1, (decoded.width * scale).toInt())
            val th = maxOf(1, (decoded.height * scale).toInt())
            val small = Bitmap.createScaledBitmap(decoded, tw, th, true)
            val pixels = IntArray(tw * th)
            small.getPixels(pixels, 0, tw, 0, 0, tw, th)
            if (small !== decoded) small.recycle()
            decoded.recycle()
            ImageAnalyzer.analyze(pixels, tw, th)
        } catch (e: Exception) {
            null
        } catch (e: OutOfMemoryError) {
            null
        }
    }

    private companion object {
        const val GROUP = 6
        const val MAX_CATEGORIES = 6
        const val ANALYSIS_SIDE = 96
        const val MAX_SOURCE_PIXELS = 100_000_000L
    }
}
