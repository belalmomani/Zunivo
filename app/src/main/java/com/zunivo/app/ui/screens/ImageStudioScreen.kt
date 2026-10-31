package com.zunivo.app.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.zunivo.app.R
import com.zunivo.app.domain.HexColor
import com.zunivo.app.feature.search.Category
import com.zunivo.app.feature.search.ErrorKind
import com.zunivo.app.feature.studio.PaletteMode
import com.zunivo.app.feature.studio.Simplicity
import com.zunivo.app.feature.studio.StudioUiState
import com.zunivo.app.feature.studio.StudioViewModel
import com.zunivo.app.ui.components.ColorSwatch
import com.zunivo.app.ui.components.ScreenHeader
import com.zunivo.app.ui.components.SectionTitle

@Composable
fun ImageStudioScreen(onBack: () -> Unit, vm: StudioViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) vm.analyze(uri)
    }
    Column(Modifier.verticalScroll(rememberScrollState())) {
        ScreenHeader(stringResource(R.string.menu_studio), onBack)
        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                stringResource(R.string.studio_privacy),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(
                onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.studio_pick)) }

            if (state.analyzing) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CircularProgressIndicator(Modifier.size(20.dp))
                    Text(stringResource(R.string.studio_analyzing))
                }
            }
            if (state.analysisFailed) {
                Text(stringResource(R.string.studio_failed), color = MaterialTheme.colorScheme.error)
            }
            if (state.profile != null) ProfileSection(state, vm)
        }
    }
}

@Composable
private fun ProfileSection(state: StudioUiState, vm: StudioViewModel) {
    val profile = state.profile ?: return
    val simplicityLabel = stringResource(
        when (profile.simplicity) {
            Simplicity.SIMPLE -> R.string.studio_simple
            Simplicity.MEDIUM -> R.string.studio_medium
            Simplicity.DETAILED -> R.string.studio_detailed
        },
    )
    SectionTitle(stringResource(R.string.studio_colors))
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        profile.palette.forEachIndexed { index, share ->
            val hex = HexColor.fromArgb(share.argb)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                ColorSwatch(hex, selected = false, onClick = { vm.setColor(hex) }, diameter = 40.dp)
                Text(
                    when (index) {
                        0 -> stringResource(R.string.studio_primary)
                        1 -> stringResource(R.string.studio_secondary)
                        else -> ""
                    },
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
    Text(stringResource(R.string.studio_simplicity_line, simplicityLabel), style = MaterialTheme.typography.bodyMedium)
    Text(
        stringResource(if (profile.isDark) R.string.studio_tone_dark else R.string.studio_tone_light),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    SectionTitle(stringResource(R.string.studio_palette))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(
            selected = state.paletteMode == PaletteMode.IMAGE,
            onClick = { vm.setPaletteMode(PaletteMode.IMAGE) },
            label = { Text(stringResource(R.string.studio_palette_image)) },
        )
        FilterChip(
            selected = state.paletteMode == PaletteMode.HARMONY,
            onClick = { vm.setPaletteMode(PaletteMode.HARMONY) },
            label = { Text(stringResource(R.string.studio_palette_harmony)) },
        )
    }
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(state.paletteHex()) { hex -> ColorSwatch(hex, state.colorHex == hex, onClick = { vm.setColor(hex) }) }
    }

    SectionTitle(stringResource(R.string.studio_categories))
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(Category.entries) { category ->
            FilterChip(
                selected = category in state.categories,
                onClick = { vm.toggleCategory(category) },
                label = { Text(stringResource(category.labelRes)) },
            )
        }
    }

    SectionTitle(stringResource(R.string.studio_suggestions))
    when {
        state.categories.isEmpty() -> Text(
            stringResource(R.string.studio_no_suggestions),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        state.searching -> CircularProgressIndicator(Modifier.size(24.dp))
        state.searchError != null -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                stringResource(
                    when (state.searchError) {
                        ErrorKind.OFFLINE -> R.string.error_offline_msg
                        ErrorKind.TIMEOUT -> R.string.error_timeout_msg
                        else -> R.string.error_bad_data_msg
                    },
                ),
                color = MaterialTheme.colorScheme.error,
            )
            OutlinedButton(onClick = vm::suggest) { Text(stringResource(R.string.action_retry)) }
        }
        else -> {
            Text(
                stringResource(R.string.studio_tap_exclude),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            // Plain rows (not a lazy grid): this screen already scrolls.
            state.suggestions.chunked(4).forEach { rowItems ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    rowItems.forEach { s ->
                        val excluded = s.item.ref.id in state.excluded
                        Surface(
                            modifier = Modifier.weight(1f).aspectRatio(1f).clip(RoundedCornerShape(12.dp))
                                .clickable { vm.toggleExclude(s.item.ref.id) },
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface,
                        ) {
                            Box(Modifier.padding(14.dp), contentAlignment = Alignment.Center) {
                                AsyncImage(
                                    model = s.item.ref.svgUrl(height = 64, colorHex = state.colorHex),
                                    contentDescription = s.item.ref.name,
                                    modifier = Modifier.fillMaxWidth().alpha(if (excluded) 0.25f else 1f),
                                )
                            }
                        }
                    }
                    repeat(4 - rowItems.size) { Spacer(Modifier.weight(1f)) }
                }
            }
            val kept = state.included().size
            Text(
                stringResource(R.string.studio_summary, kept, state.needsLicenseReview()),
                style = MaterialTheme.typography.bodyMedium,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = vm::shuffle, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.studio_shuffle))
                }
                OutlinedButton(onClick = {}, enabled = false, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.studio_create_pack) + " - " + stringResource(R.string.badge_phase, 5))
                }
            }
        }
    }
    Spacer(Modifier.height(16.dp))
}
