package com.zunivo.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zunivo.app.R
import com.zunivo.app.domain.PrefsOps
import com.zunivo.app.feature.library.LibraryViewModel
import com.zunivo.app.feature.prefs.PrefsViewModel
import com.zunivo.app.feature.search.Category
import com.zunivo.app.ui.components.ColorSwatch
import com.zunivo.app.ui.components.ScreenHeader
import com.zunivo.app.ui.components.SectionTitle

/** Everything Zunivo remembers about how you use it. It stays on this phone and you can edit or erase it. */
@Composable
fun PreferencesScreen(onBack: () -> Unit, prefsVm: PrefsViewModel = viewModel(), libVm: LibraryViewModel = viewModel()) {
    val prefs by prefsVm.prefs.collectAsStateWithLifecycle()
    val topCategories = PrefsOps.topCategories(prefs).mapNotNull { name -> Category.entries.firstOrNull { it.name == name } }
    // stringResource is @Composable, so resolve the labels here (map is inline) and not inside joinToString.
    val topCategoryLabels = topCategories.map { stringResource(it.labelRes) }
    val colors = PrefsOps.topColors(prefs)

    Column(Modifier.verticalScroll(rememberScrollState())) {
        ScreenHeader(stringResource(R.string.menu_preferences), onBack)
        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.prefs_intro), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            SwitchRow(R.string.prefs_learn, R.string.prefs_learn_desc, prefs.learnFromUsage) { prefsVm.setLearning(it) }
            SwitchRow(R.string.prefs_history, R.string.prefs_history_desc, prefs.keepSearchHistory) { prefsVm.setHistory(it) }

            SectionTitle(stringResource(R.string.prefs_learned_title))
            Text(stringResource(R.string.prefs_top_categories), style = MaterialTheme.typography.labelLarge)
            Text(
                if (topCategories.isEmpty()) stringResource(R.string.prefs_nothing_yet)
                else topCategoryLabels.joinToString(", "),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(stringResource(R.string.prefs_recent_colors), style = MaterialTheme.typography.labelLarge)
            if (colors.isEmpty()) {
                Text(stringResource(R.string.prefs_nothing_yet), style = MaterialTheme.typography.bodyMedium)
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    colors.forEach { hex -> ColorSwatch(hex, selected = false, onClick = {}) }
                }
            }
            OutlinedButton(onClick = { prefsVm.clearLearned(); libVm.clearRecents() }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.prefs_clear_learned))
            }

            if (prefs.keepSearchHistory) {
                SectionTitle(stringResource(R.string.prefs_history_title))
                if (prefs.searchHistory.isEmpty()) Text(stringResource(R.string.prefs_nothing_yet), style = MaterialTheme.typography.bodyMedium)
                prefs.searchHistory.forEach { q ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(q, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                        IconButton(onClick = { prefsVm.removeHistory(q) }) {
                            Icon(Icons.Filled.Clear, contentDescription = stringResource(R.string.entry_remove))
                        }
                    }
                }
                OutlinedButton(onClick = prefsVm::clearHistory, enabled = prefs.searchHistory.isNotEmpty(), modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.prefs_clear_history))
                }
            }
        }
    }
}

@Composable
private fun SwitchRow(titleRes: Int, descRes: Int, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(titleRes), style = MaterialTheme.typography.bodyLarge)
            Text(stringResource(descRes), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
