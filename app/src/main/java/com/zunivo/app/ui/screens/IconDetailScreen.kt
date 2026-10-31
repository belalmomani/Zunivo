package com.zunivo.app.ui.screens

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.zunivo.app.R
import com.zunivo.app.domain.IconItem
import com.zunivo.app.domain.LicenseKind
import com.zunivo.app.domain.LicensePolicy
import com.zunivo.app.ui.components.ScreenHeader
import com.zunivo.app.ui.components.SectionTitle

@Composable
fun IconDetailScreen(item: IconItem, onBack: () -> Unit, onOpenEditor: (IconItem) -> Unit) {
    val context = LocalContext.current
    val set = item.set
    val assessment = LicensePolicy.assess(set?.licenseSpdx)
    val unknown = stringResource(R.string.detail_unknown)
    val licenseText = set?.licenseTitle ?: set?.licenseSpdx
    val attribution = if (set?.authorName != null && licenseText != null) {
        stringResource(R.string.attribution_template, item.ref.name, set.name, set.authorName, licenseText)
    } else null

    Column(Modifier.verticalScroll(rememberScrollState())) {
        ScreenHeader(item.ref.name, onBack)
        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
            ) {
                Box(Modifier.padding(32.dp), contentAlignment = Alignment.Center) {
                    AsyncImage(
                        model = item.ref.svgUrl(height = 160, colorHex = ICON_COLOR),
                        contentDescription = item.ref.name,
                        modifier = Modifier.size(120.dp),
                    )
                }
            }

            InfoRow(stringResource(R.string.detail_name), item.ref.id)
            InfoRow(stringResource(R.string.detail_set), set?.let { "${it.name} (${it.prefix})" } ?: item.ref.prefix)
            InfoRow(stringResource(R.string.detail_source), stringResource(R.string.detail_source_value))
            InfoRow(stringResource(R.string.detail_author), set?.authorName ?: unknown)
            InfoRow(stringResource(R.string.detail_license), licenseText ?: unknown)

            SectionTitle(stringResource(R.string.detail_license_notes))
            Text(stringResource(licenseMessage(assessment.kind)), style = MaterialTheme.typography.bodyMedium,
                color = if (assessment.kind == LicenseKind.PERMISSIVE || assessment.kind == LicenseKind.ATTRIBUTION)
                    MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.tertiary)
            if (assessment.noticeRequired && assessment.kind != LicenseKind.UNKNOWN) {
                Text(stringResource(R.string.license_notice_required), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Button(onClick = { onOpenEditor(item) }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.action_edit_export))
            }
            OutlinedButton(onClick = { copy(context, item.ref.id) }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.detail_copy_name))
            }
            if (attribution != null) {
                OutlinedButton(onClick = { copy(context, attribution) }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.detail_copy_attribution))
                }
            }
            val licenseUrl = set?.licenseUrl
            if (licenseUrl != null) {
                OutlinedButton(onClick = { openHttps(context, licenseUrl) }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.detail_open_license))
                }
            }
            val authorUrl = set?.authorUrl
            if (authorUrl != null) {
                OutlinedButton(onClick = { openHttps(context, authorUrl) }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.detail_open_author))
                }
            }

            SectionTitle(stringResource(R.string.detail_next_title))
            DisabledAction(R.string.action_add_favorite, 5)
            DisabledAction(R.string.action_add_folder, 5)
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun DisabledAction(labelRes: Int, phase: Int) {
    OutlinedButton(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(labelRes) + " - " + stringResource(R.string.badge_phase, phase))
    }
}

internal fun licenseMessage(kind: LicenseKind): Int = when (kind) {
    LicenseKind.PERMISSIVE -> R.string.license_permissive
    LicenseKind.ATTRIBUTION -> R.string.license_attribution
    LicenseKind.SHARE_ALIKE -> R.string.license_sharealike
    LicenseKind.NON_COMMERCIAL -> R.string.license_noncommercial
    LicenseKind.NO_DERIVATIVES -> R.string.license_noderivs
    LicenseKind.REVIEW -> R.string.license_review
    LicenseKind.UNKNOWN -> R.string.license_unknown
}

private fun copy(context: Context, text: String) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText("Zunivo", text))
    Toast.makeText(context, R.string.detail_copied, Toast.LENGTH_SHORT).show()
}

/** Only https links from the API are opened, and only via the system browser. */
private fun openHttps(context: Context, url: String) {
    if (!url.startsWith("https://")) return
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, R.string.detail_no_browser, Toast.LENGTH_SHORT).show()
    }
}
