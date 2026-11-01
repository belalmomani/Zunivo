package com.zunivo.app.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zunivo.app.R
import com.zunivo.app.domain.CollectionKind
import com.zunivo.app.feature.library.LibraryViewModel
import com.zunivo.app.domain.IconItem
import com.zunivo.app.domain.LicenseKind
import com.zunivo.app.domain.LicensePolicy
import com.zunivo.app.ui.components.ScreenHeader
import com.zunivo.app.ui.components.SectionTitle

@Composable
fun IconDetailScreen(
    item: IconItem,
    onBack: () -> Unit,
    onOpenEditor: (IconItem) -> Unit,
    libVm: LibraryViewModel = viewModel(),
) {
    val lib by libVm.state.collectAsStateWithLifecycle()
    var addTo by rememberSaveable { mutableStateOf<String?>(null) }   // null | "FOLDER" | "PACK"
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
            AddDialogHost(addTo, item, libVm) { addTo = null }
            SectionTitle(stringResource(R.string.detail_next_title))
            OutlinedButton(onClick = { libVm.toggleFavorite(item) }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(if (lib.isFavorite(item.ref)) R.string.action_remove_favorite else R.string.action_add_favorite))
            }
            OutlinedButton(onClick = { addTo = CollectionKind.FOLDER.name }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.action_add_folder))
            }
            OutlinedButton(onClick = { addTo = CollectionKind.PACK.name }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.action_add_pack))
            }
        }
    }
}

@Composable
private fun AddDialogHost(kind: String?, item: IconItem, libVm: LibraryViewModel, onClose: () -> Unit) {
    val lib by libVm.state.collectAsStateWithLifecycle()
    val k = kind?.let { runCatching { CollectionKind.valueOf(it) }.getOrNull() } ?: return
    AddToCollectionDialog(
        kind = k,
        collections = lib.library.collections,
        onPick = { id -> libVm.addToCollection(id, item); onClose() },
        onCreate = { name ->
            val id = libVm.createCollection(k, name)
            libVm.addToCollection(id, item)
            onClose()
        },
        onDismiss = onClose,
    )
}

@Composable
private fun InfoRow(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge)
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

