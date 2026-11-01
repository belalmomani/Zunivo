package com.zunivo.app.ui.screens

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.text.format.Formatter
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.zunivo.app.R
import com.zunivo.app.domain.ChangeKind
import com.zunivo.app.domain.Decision
import com.zunivo.app.domain.DraftItem
import com.zunivo.app.domain.DraftOps
import com.zunivo.app.domain.FetchStatus
import com.zunivo.app.domain.ImportMode
import com.zunivo.app.domain.ItemKind
import com.zunivo.app.domain.ItemNote
import com.zunivo.app.domain.StoreDraft
import com.zunivo.app.domain.Verification
import com.zunivo.app.feature.search.Category
import com.zunivo.app.feature.store.StoreError
import com.zunivo.app.feature.store.StoreMessage
import com.zunivo.app.feature.store.StoreUiState
import com.zunivo.app.feature.store.StoreViewModel
import com.zunivo.app.ui.components.EmptyState
import com.zunivo.app.ui.components.ScreenHeader
import com.zunivo.app.ui.components.SectionTitle

@Composable
fun StoreScreen(onBack: () -> Unit, vm: StoreViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val message = state.message
    LaunchedEffect(message) {
        if (message != null) {
            Toast.makeText(context, context.getString(storeMessageRes(message)), Toast.LENGTH_LONG).show()
            vm.consumeMessage()
        }
    }
    val opened = state.opened()
    if (opened != null) {
        BackHandler { vm.close() }
        DraftScreen(opened, state, vm)
    } else {
        StoreHome(state, vm, onBack)
    }
}

// ---------------------------------------------------------------- home + import

@Composable
private fun StoreHome(state: StoreUiState, vm: StoreViewModel, onBack: () -> Unit) {
    val approved = state.drafts.filter { it.approved }
    val drafts = state.drafts.filter { !it.approved }
    LazyColumn(contentPadding = PaddingValues(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { ScreenHeader(stringResource(R.string.menu_store), onBack) }
        item {
            Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionTitle(stringResource(R.string.store_import_title))
                OutlinedTextField(
                    value = state.url, onValueChange = vm::setUrl, singleLine = true, modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.store_url_label)) },
                    placeholder = { Text("https://github.com/owner/repo") },
                    isError = state.error == StoreError.INVALID_URL,
                )
                state.error?.let { Text(stringResource(storeErrorRes(it)), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                Button(onClick = vm::checkUrl, enabled = state.url.isNotBlank() && !state.loading, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.store_check))
                }
                if (state.loading) CircularProgressIndicator(Modifier.size(24.dp))
                state.preview?.let { p ->
                    val context = LocalContext.current
                    Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(p.info.name, style = MaterialTheme.typography.titleMedium)
                            Text(stringResource(R.string.store_owner_line, p.info.ownerLogin), style = MaterialTheme.typography.bodyMedium)
                            Text(
                                stringResource(R.string.store_size_line, Formatter.formatShortFileSize(context, p.info.sizeKb * 1024)),
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                stringResource(R.string.store_license_line, p.info.licenseName ?: stringResource(R.string.detail_unknown)),
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(stringResource(R.string.store_mode_note), style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Button(onClick = { vm.importNow(ImportMode.INFO_ONLY) }, enabled = !state.loading, modifier = Modifier.fillMaxWidth()) {
                                Text(stringResource(R.string.store_mode_info))
                            }
                            OutlinedButton(onClick = { vm.importNow(ImportMode.INFO_AND_FILES) }, enabled = !state.loading, modifier = Modifier.fillMaxWidth()) {
                                Text(stringResource(R.string.store_mode_files))
                            }
                        }
                    }
                }
            }
        }
        if (state.drafts.isEmpty() && state.loaded) {
            item { EmptyState(Icons.Filled.ShoppingCart, stringResource(R.string.store_empty_title), stringResource(R.string.store_empty_msg)) }
        }
        if (drafts.isNotEmpty()) {
            item { SectionTitle(stringResource(R.string.store_drafts_title), Modifier.padding(horizontal = 16.dp)) }
            items(drafts, key = { it.id }) { DraftRow(it) { vm.open(it.id) } }
        }
        if (approved.isNotEmpty()) {
            item { SectionTitle(stringResource(R.string.store_projects_title), Modifier.padding(horizontal = 16.dp)) }
            items(approved, key = { it.id }) { DraftRow(it) { vm.open(it.id) } }
        }
    }
}

@Composable
private fun DraftRow(d: StoreDraft, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surface,
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(d.displayName.ifBlank { d.repo.repo }, style = MaterialTheme.typography.bodyLarge)
            Text("${d.repo.owner}/${d.repo.repo}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            val c = DraftOps.counts(d)
            Text(stringResource(R.string.store_counts, c.accepted, c.rejected, c.review), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ---------------------------------------------------------------- draft review / approved project

@Composable
private fun DraftScreen(d: StoreDraft, state: StoreUiState, vm: StoreViewModel) {
    val context = LocalContext.current
    var confirmApprove by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    var openLink by rememberSaveable { mutableStateOf<String?>(null) }
    var warnDownload by rememberSaveable { mutableStateOf<String?>(null) }
    val counts = DraftOps.counts(d)
    val shown = if (d.approved) DraftOps.visible(d) else d.items

    LazyColumn(contentPadding = PaddingValues(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { ScreenHeader(d.displayName.ifBlank { d.repo.repo }, onBack = vm::close) }
        item {
            Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(if (d.approved) R.string.store_status_approved else R.string.store_status_draft),
                    style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.tertiary)
                Text("${d.repo.owner}/${d.repo.repo}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                state.error?.let { Text(stringResource(storeErrorRes(it)), color = MaterialTheme.colorScheme.error) }
                if (state.loading) CircularProgressIndicator(Modifier.size(24.dp))

                // display data (editable) is kept apart from the original data in the items below
                DisplayFields(d, vm)

                if (!d.approved) {
                    Text(stringResource(R.string.store_counts, counts.accepted, counts.rejected, counts.review), style = MaterialTheme.typography.bodyMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { vm.acceptAll(d.id) }, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.store_accept_all)) }
                        OutlinedButton(onClick = { vm.rejectAll(d.id) }, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.store_reject_all)) }
                    }
                    OutlinedButton(onClick = { vm.resetDecisions(d.id) }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.store_reset)) }
                    Button(onClick = { confirmApprove = true }, enabled = DraftOps.canApprove(d), modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.store_approve))
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { vm.refresh(d.id) }, enabled = !state.loading, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.store_refresh)) }
                        OutlinedButton(onClick = { vm.deleteLocalFiles(d.id) }, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.store_delete_files)) }
                    }
                }
                OutlinedButton(onClick = { confirmDelete = true }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.store_delete_project)) }
                if (d.items.none { it.kind == ItemKind.APK && it.fetch == FetchStatus.FETCHED }) {
                    Text(stringResource(R.string.store_no_apk), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        items(shown, key = { it.id }) { item ->
            ItemCard(d, item, state, vm, onOpenLink = { openLink = it }, onDownload = { warnDownload = it }, context = context)
        }
    }

    if (confirmApprove) {
        AlertDialog(
            onDismissRequest = { confirmApprove = false },
            title = { Text(stringResource(R.string.store_approve)) },
            text = { Text(stringResource(R.string.store_approve_msg, counts.accepted, counts.review)) },
            confirmButton = { TextButton(onClick = { vm.approve(d.id); confirmApprove = false }) { Text(stringResource(R.string.store_approve)) } },
            dismissButton = { TextButton(onClick = { confirmApprove = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.delete_title, d.displayName.ifBlank { d.repo.repo })) },
            text = { Text(stringResource(R.string.delete_msg)) },
            confirmButton = { TextButton(onClick = { vm.deleteDraft(d.id); confirmDelete = false }) { Text(stringResource(R.string.action_delete)) } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
    openLink?.let { url ->
        AlertDialog(
            onDismissRequest = { openLink = null },
            title = { Text(stringResource(R.string.store_open_link_title)) },
            text = { Text(stringResource(R.string.store_open_link_msg, url)) },
            confirmButton = { TextButton(onClick = { openExternal(context, url); openLink = null }) { Text(stringResource(R.string.store_open_link)) } },
            dismissButton = { TextButton(onClick = { openLink = null }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
    warnDownload?.let { itemId ->
        val item = d.items.firstOrNull { it.id == itemId }
        if (item != null) {
            AlertDialog(
                onDismissRequest = { warnDownload = null },
                title = { Text(stringResource(if (item.kind == ItemKind.APK) R.string.store_apk_warning_title else R.string.store_archive_warning_title)) },
                text = {
                    Text(
                        stringResource(if (item.kind == ItemKind.APK) R.string.store_apk_warning_msg else R.string.store_archive_warning_msg,
                            item.sizeBytes?.let { Formatter.formatShortFileSize(context, it) } ?: stringResource(R.string.detail_unknown)),
                    )
                },
                confirmButton = { TextButton(onClick = { vm.download(d.id, itemId); warnDownload = null }) { Text(stringResource(R.string.store_download)) } },
                dismissButton = { TextButton(onClick = { warnDownload = null }) { Text(stringResource(R.string.action_cancel)) } },
            )
        }
    }
    state.pendingUpdate?.takeIf { it.draftId == d.id }?.let { u ->
        AlertDialog(
            onDismissRequest = vm::cancelUpdate,
            title = { Text(stringResource(R.string.store_update_title)) },
            text = {
                LazyColumn {
                    item { Text(stringResource(R.string.store_update_msg), style = MaterialTheme.typography.bodySmall) }
                    items(u.changes, key = { it.kind.name + it.item.id }) { c ->
                        Text(stringResource(changeRes(c.kind)) + ": " + c.item.label, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            },
            confirmButton = { TextButton(onClick = vm::applyUpdate) { Text(stringResource(R.string.store_update_apply)) } },
            dismissButton = { TextButton(onClick = vm::cancelUpdate) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

@Composable
private fun DisplayFields(d: StoreDraft, vm: StoreViewModel) {
    var name by rememberSaveable(d.id) { mutableStateOf(d.displayName) }
    var desc by rememberSaveable(d.id) { mutableStateOf(d.displayDescription) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionTitle(stringResource(R.string.store_display_title))
        OutlinedTextField(value = name, onValueChange = { name = it.take(80); vm.setDisplayName(d.id, name) },
            label = { Text(stringResource(R.string.store_display_name)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = desc, onValueChange = { desc = it.take(1000); vm.setDisplayDescription(d.id, desc) },
            label = { Text(stringResource(R.string.store_display_desc)) }, modifier = Modifier.fillMaxWidth())
        Text(stringResource(R.string.store_original_locked), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(Category.entries) { c ->
                FilterChip(selected = c.name in d.categories, onClick = { vm.toggleCategory(d.id, c.name) }, label = { Text(stringResource(c.labelRes)) })
            }
        }
        Text(stringResource(R.string.store_visibility_private), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ItemCard(
    d: StoreDraft, item: DraftItem, state: StoreUiState, vm: StoreViewModel,
    onOpenLink: (String) -> Unit, onDownload: (String) -> Unit, context: Context,
) {
    val progress = state.downloads[item.id]
    Surface(
        modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
        shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surface,
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(kindRes(item.kind)) + (if (item.kind == ItemKind.IMAGE || item.kind == ItemKind.RELEASE || item.kind == ItemKind.APK || item.kind == ItemKind.DOCS_LINK) " - " + item.label else ""),
                        style = MaterialTheme.typography.labelLarge)
                    Text(stringResource(fetchRes(item.fetch)), style = MaterialTheme.typography.bodySmall,
                        color = if (item.fetch == FetchStatus.FETCHED) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.tertiary)
                }
                if (!d.approved) {
                    IconButton(onClick = { vm.moveItem(d.id, item.id, -1) }) { Icon(Icons.Filled.KeyboardArrowUp, contentDescription = stringResource(R.string.entry_move_up)) }
                    IconButton(onClick = { vm.moveItem(d.id, item.id, 1) }) { Icon(Icons.Filled.KeyboardArrowDown, contentDescription = stringResource(R.string.entry_move_down)) }
                }
            }
            item.note?.let { Text(stringResource(noteRes(it)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }

            // preview of the content
            when {
                item.kind == ItemKind.APK && item.fetch == FetchStatus.NOT_FOUND ->
                    Text(stringResource(R.string.store_no_apk), style = MaterialTheme.typography.bodyMedium)
                item.kind == ItemKind.IMAGE && item.fetch == FetchStatus.FETCHED -> {
                    val local = vm.fileOf(d, item)
                    AsyncImage(
                        model = local ?: item.value,
                        contentDescription = item.label,
                        modifier = Modifier.fillMaxWidth().size(160.dp),
                    )
                }
                item.kind == ItemKind.DOCS_LINK && item.value != null ->
                    TextButton(onClick = { onOpenLink(item.value) }) { Text(item.value) }
                item.kind == ItemKind.APK || item.kind == ItemKind.SOURCE_ARCHIVE -> Unit
                item.value != null -> Text(item.value.take(PREVIEW_CHARS), style = MaterialTheme.typography.bodyMedium, maxLines = 8)
            }
            item.sourceUrl?.takeIf { item.kind != ItemKind.DOCS_LINK && it.startsWith("https://") }?.let { url ->
                TextButton(onClick = { onOpenLink(url) }) { Text(stringResource(R.string.store_source_link)) }
            }

            if (item.kind == ItemKind.APK || item.kind == ItemKind.SOURCE_ARCHIVE) FileActions(d, item, progress, vm, onDownload, context)

            if (!d.approved && item.decidable) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    DecisionChip(R.string.store_accept, item.decision == Decision.ACCEPTED) { vm.decide(d.id, item.id, Decision.ACCEPTED) }
                    DecisionChip(R.string.store_reject, item.decision == Decision.REJECTED) { vm.decide(d.id, item.id, Decision.REJECTED) }
                    DecisionChip(R.string.store_keep_review, item.decision == Decision.REVIEW) { vm.decide(d.id, item.id, Decision.REVIEW) }
                }
                if (item.kind == ItemKind.IMAGE && item.decision == Decision.ACCEPTED) {
                    OutlinedButton(onClick = { vm.setCover(d.id, if (d.coverItemId == item.id) null else item.id) }) {
                        Text(stringResource(if (d.coverItemId == item.id) R.string.store_cover_remove else R.string.store_cover_set))
                    }
                }
            } else if (item.kind.alwaysShown) {
                Text(stringResource(R.string.store_always_shown), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else if (d.approved && d.coverItemId == item.id) {
                Text(stringResource(R.string.store_is_cover), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary)
            }
        }
    }
}

@Composable
private fun DecisionChip(labelRes: Int, selected: Boolean, onClick: () -> Unit) {
    FilterChip(selected = selected, onClick = onClick, label = { Text(stringResource(labelRes)) })
}

@Composable
private fun FileActions(
    d: StoreDraft, item: DraftItem, progress: com.zunivo.app.feature.store.DownloadProgress?,
    vm: StoreViewModel, onDownload: (String) -> Unit, context: Context,
) {
    val file = vm.fileOf(d, item)
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        item.sizeBytes?.let { Text(stringResource(R.string.store_size_line, Formatter.formatShortFileSize(context, it)), style = MaterialTheme.typography.bodySmall) }
        if (item.expectedSha256 != null) Text("SHA-256: " + item.expectedSha256.take(16) + "...", style = MaterialTheme.typography.bodySmall)
        if (progress != null) {
            if (progress.total > 0) LinearProgressIndicator(progress = { (progress.bytes.toFloat() / progress.total).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
            else LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
        if (file != null) {
            Text(
                stringResource(if (item.verification == Verification.VERIFIED) R.string.store_verified else R.string.store_unverified),
                style = MaterialTheme.typography.bodySmall,
                color = if (item.verification == Verification.VERIFIED) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.tertiary,
            )
            item.computedSha256?.let { Text("SHA-256: " + it.take(16) + "...", style = MaterialTheme.typography.bodySmall) }
            if (item.kind == ItemKind.APK) {
                Button(onClick = { installApk(context, file) }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.store_install)) }
            }
        } else if (item.fetch == FetchStatus.FETCHED && item.note != ItemNote.EXTERNAL_HOST) {
            OutlinedButton(onClick = { onDownload(item.id) }, enabled = progress == null, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.store_download))
            }
        }
    }
}

// ---------------------------------------------------------------- helpers

private const val PREVIEW_CHARS = 600

private fun kindRes(k: ItemKind): Int = when (k) {
    ItemKind.NAME -> R.string.kind_name
    ItemKind.DESCRIPTION -> R.string.kind_description
    ItemKind.OWNER -> R.string.kind_owner
    ItemKind.REPO_URL -> R.string.kind_repo_url
    ItemKind.LANGUAGE -> R.string.kind_language
    ItemKind.LICENSE -> R.string.kind_license
    ItemKind.README -> R.string.kind_readme
    ItemKind.IMAGE -> R.string.kind_image
    ItemKind.RELEASE -> R.string.kind_release
    ItemKind.CHANGELOG -> R.string.kind_changelog
    ItemKind.DOCS_LINK -> R.string.kind_docs
    ItemKind.APK -> R.string.kind_apk
    ItemKind.SOURCE_ARCHIVE -> R.string.kind_archive
}

private fun fetchRes(s: FetchStatus): Int = when (s) {
    FetchStatus.FETCHED -> R.string.status_fetched
    FetchStatus.NOT_FOUND -> R.string.status_not_found
    FetchStatus.FAILED -> R.string.status_failed
}

private fun noteRes(n: ItemNote): Int = when (n) {
    ItemNote.EXTERNAL_HOST -> R.string.note_external_host
    ItemNote.TOO_LARGE -> R.string.note_too_large
    ItemNote.NOT_AN_IMAGE -> R.string.note_not_image
    ItemNote.CHECKSUM_MISMATCH -> R.string.note_checksum
    ItemNote.DOWNLOAD_FAILED -> R.string.note_download_failed
}

private fun changeRes(k: ChangeKind): Int = when (k) {
    ChangeKind.ADDED -> R.string.change_added
    ChangeKind.CHANGED -> R.string.change_changed
    ChangeKind.REMOVED -> R.string.change_removed
}

private fun storeErrorRes(e: StoreError): Int = when (e) {
    StoreError.INVALID_URL -> R.string.store_err_invalid
    StoreError.NOT_FOUND -> R.string.store_err_not_found
    StoreError.RATE_LIMITED -> R.string.store_err_rate
    StoreError.OFFLINE -> R.string.error_offline_msg
    StoreError.TIMEOUT -> R.string.error_timeout_msg
    StoreError.SERVER -> R.string.store_err_server
    StoreError.BAD_DATA -> R.string.error_bad_data_msg
}

private fun storeMessageRes(m: StoreMessage): Int = when (m) {
    StoreMessage.APPROVED -> R.string.store_msg_approved
    StoreMessage.DELETED -> R.string.msg_deleted
    StoreMessage.FILES_DELETED -> R.string.store_msg_files_deleted
    StoreMessage.DOWNLOADED_VERIFIED -> R.string.store_msg_verified
    StoreMessage.DOWNLOADED_UNVERIFIED -> R.string.store_msg_unverified
    StoreMessage.DOWNLOAD_FAILED -> R.string.note_download_failed
    StoreMessage.CHECKSUM_MISMATCH -> R.string.note_checksum
    StoreMessage.TOO_LARGE -> R.string.note_too_large
    StoreMessage.UPDATED -> R.string.store_msg_updated
    StoreMessage.NO_CHANGES -> R.string.store_msg_no_changes
}

/** Opens a link in the browser only after the user confirmed the exact address in a dialog. */
private fun openExternal(context: Context, url: String) {
    if (!url.startsWith("https://")) return
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, R.string.msg_export_failed, Toast.LENGTH_SHORT).show()
    }
}

/** Hands the APK to Android's own installer. The user must allow "install unknown apps" for Zunivo and confirm. */
private fun installApk(context: Context, file: java.io.File) {
    if (!context.packageManager.canRequestPackageInstalls()) {
        Toast.makeText(context, R.string.store_install_permission, Toast.LENGTH_LONG).show()
        context.startActivity(
            Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + context.packageName)),
        )
        return
    }
    try {
        val uri = FileProvider.getUriForFile(context, "com.zunivo.app.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, R.string.msg_export_failed, Toast.LENGTH_SHORT).show()
    } catch (e: IllegalArgumentException) {
        Toast.makeText(context, R.string.msg_export_failed, Toast.LENGTH_SHORT).show()
    }
}
