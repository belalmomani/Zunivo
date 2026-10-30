package com.zunivo.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.zunivo.app.R
import com.zunivo.app.i18n.AppLanguage
import com.zunivo.app.ui.components.ActionCard
import com.zunivo.app.ui.components.ScreenHeader
import com.zunivo.app.ui.components.SectionTitle
import com.zunivo.app.ui.navigation.SubScreen
import com.zunivo.app.ui.navigation.icon
import com.zunivo.app.ui.navigation.titleRes

@Composable
fun SettingsScreen(
    currentLanguage: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
    onOpen: (SubScreen) -> Unit,
    onBack: () -> Unit,
) {
    Column(Modifier.verticalScroll(rememberScrollState())) {
        ScreenHeader(stringResource(R.string.settings_title), onBack)
        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionTitle(stringResource(R.string.settings_language))
            LanguageRow(AppLanguage.SYSTEM, stringResource(R.string.lang_system), currentLanguage, onLanguageChange)
            LanguageRow(AppLanguage.ARABIC, stringResource(R.string.lang_arabic), currentLanguage, onLanguageChange)
            LanguageRow(AppLanguage.ENGLISH, stringResource(R.string.lang_english), currentLanguage, onLanguageChange)
            Text(
                stringResource(R.string.settings_language_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            SectionTitle(stringResource(R.string.menu_about))
            listOf(SubScreen.PRIVACY, SubScreen.HELP, SubScreen.ABOUT).forEach { screen ->
                ActionCard(screen.icon(), stringResource(screen.titleRes()), { onOpen(screen) })
            }
        }
    }
}

@Composable
private fun LanguageRow(
    language: AppLanguage,
    label: String,
    current: AppLanguage,
    onSelect: (AppLanguage) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onSelect(language) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = current == language, onClick = { onSelect(language) })
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}
