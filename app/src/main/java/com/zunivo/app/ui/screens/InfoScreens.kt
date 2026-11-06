package com.zunivo.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.zunivo.app.R
import com.zunivo.app.core.AppInfo
import com.zunivo.app.ui.components.ScreenHeader
import com.zunivo.app.ui.components.ZunivoLogo

@Composable
fun AboutScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val version = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: "?"
    }
    Column(Modifier.verticalScroll(rememberScrollState())) {
        ScreenHeader(stringResource(R.string.about_title), onBack)
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ZunivoLogo(logoSize = 80.dp)
            Text(AppInfo.NAME_EN, style = MaterialTheme.typography.headlineMedium)
            Text(AppInfo.NAME_AR, style = MaterialTheme.typography.titleLarge)
            Text(stringResource(R.string.about_version, version.orEmpty()))
            Text(stringResource(R.string.about_package, AppInfo.PACKAGE_NAME))
            Text(stringResource(R.string.about_repo, AppInfo.REPO_URL))
            Text(
                stringResource(R.string.about_license_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun PrivacyScreen(onBack: () -> Unit) {
    val sections = listOf(
        R.string.privacy_s1_title to R.string.privacy_s1_body,
        R.string.privacy_s2_title to R.string.privacy_s2_body,
        R.string.privacy_s3_title to R.string.privacy_s3_body,
        R.string.privacy_s4_title to R.string.privacy_s4_body,
        R.string.privacy_s5_title to R.string.privacy_s5_body,
        R.string.privacy_s6_title to R.string.privacy_s6_body,
    )
    Column(Modifier.verticalScroll(rememberScrollState())) {
        ScreenHeader(stringResource(R.string.privacy_title), onBack)
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.privacy_draft), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.tertiary)
            sections.forEach { (title, body) ->
                Text(stringResource(title), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(body), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
fun HelpScreen(onBack: () -> Unit) {
    TextPage(stringResource(R.string.help_title), null, stringResource(R.string.help_body), onBack)
}

@Composable
private fun TextPage(title: String, badge: String?, body: String, onBack: () -> Unit) {
    Column(Modifier.verticalScroll(rememberScrollState())) {
        ScreenHeader(title, onBack)
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (badge != null) {
                Text(badge, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.tertiary)
            }
            Text(body, style = MaterialTheme.typography.bodyLarge)
        }
    }
}
