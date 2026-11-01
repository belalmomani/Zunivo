package com.zunivo.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.zunivo.app.R
import com.zunivo.app.domain.CollectionKind
import com.zunivo.app.domain.EditParams
import com.zunivo.app.domain.ExcludeReason
import com.zunivo.app.domain.IconCollection
import com.zunivo.app.domain.LibraryOps
import com.zunivo.app.domain.PackPlanner
import com.zunivo.app.feature.library.LibraryUiState
import com.zunivo.app.feature.library.LibraryViewModel
import com.zunivo.app.feature.library.StarterPack
import com.zunivo.app.ui.components.EmptyState
import com.zunivo.app.ui.components.ScreenHeader
import com.zunivo.app.ui.components.SectionTitle

/** Folders (kind = FOLDER) and packs (kind = PACK) share one screen; packs add ready-made starters. */
@Composable
fun CollectionsScreen(kind: CollectionKind, onBack: () -> Unit, libVm: LibraryViewModel = viewModel()) {
    val state by libVm.state.collectAsStateWithLifecycle()
    val opened = state.collection(state.openedId)?.takeIf { it.kind == kind }
    if (opened != null) {
        BackHandler { libVm.closeOpened() }
        CollectionDetail(opened, state.busy, libVm)
    } else {
        CollectionList(kind, onBack, libVm, state)
    }
}

@Composable
private fun CollectionList(kind: CollectionKind, onBack: () -> Unit, libVm: LibraryViewModel, state: LibraryUiState) {
    var creating by rememberSaveable { mutableStateOf(false) }
    var starter by rememberSaveable { mutableStateOf<String?>(null) }
    val mine = state.library.collections.filter { it.kind == kind }
    val title = if (kind == CollectionKind.FOLDER) R.string.menu_folders else R.string.menu_packs

    LazyColumn(contentPadding = PaddingValues(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { ScreenHeader(stringResource(title), onBack) }
        item {
            Button(onClick = { creating = true }, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Text(stringResource(if (kind == CollectionKind.FOLDER) R.string.new_folder else R.string.new_pack))
            }
        }
        if (mine.isEmpty()) {
            item {
                EmptyState(
                    Icons.Filled.List,
                    stringResource(if (kind == CollectionKind.FOLDER) R.string.folders_empty_title else R.string.packs_empty_title),
                    stringResource(if (kind == CollectionKind.FOLDER) R.string.folders_empty_msg else R.string.packs_empty_msg),
                )
            }
        }
        items(mine, key = { it.id }) { c ->
            Surface(
                modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth().clip(RoundedCornerShape(14.dp))
                    .clickable { libVm.open(c.id) },
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface,
            ) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    val first = c.entries.firstOrNull()
                    if (first != null) {
                        AsyncImage(
                            model = first.snapshot.icon.let { EditParams(colorHex = c.colorHex ?: ICON_COLOR).svgUrl(it, 64) },
                            contentDescription = null, modifier = Modifier.size(40.dp),
                        )
                    } else {
                        Icon(Icons.Filled.List, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(40.dp))
                    }
                    Column(Modifier.weight(1f)) {
                        Text(c.name, style = MaterialTheme.typography.bodyLarge)
                        Text(stringResource(R.string.entries_count, c.entries.size), style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        if (kind == CollectionKind.PACK) {
            item {
                Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionTitle(stringResource(R.string.starter_title))
                    Text(stringResource(R.string.starter_note), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (state.busy) CircularProgressIndicator(Modifier.size(24.dp))
                }
            }
            items(StarterPack.entries.chunked(2)) { pair ->
                Row(Modifier.padding(horizontal = 16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    pair.forEach { sp ->
                        OutlinedButton(onClick = { starter = sp.name }, enabled = !state.busy, modifier = Modifier.weight(1f)) {
                            Text(stringResource(sp.nameRes))
                        }
                    }
                    if (pair.size == 1) Column(Modifier.weight(1f)) {}
                }
            }
        }
    }

    if (creating) {
        NameDialog(
            title = stringResource(if (kind == CollectionKind.FOLDER) R.string.new_folder else R.string.new_pack),
            initial = "",
            onConfirm = { name -> libVm.createCollection(kind, name, open = true); creating = false },
            onDismiss = { creating = false },
        )
    }
    val chosen = starter?.let { runCatching { StarterPack.valueOf(it) }.getOrNull() }
    if (chosen != null) {
        val name = stringResource(chosen.nameRes)
        NameDialog(
            title = stringResource(R.string.new_pack),
            initial = name,
            onConfirm = { n -> libVm.createStarterPack(chosen, n); starter = null },
            onDismiss = { starter = null },
        )
    }
}

@Composable
private fun CollectionDetail(c: IconCollection, busy: Boolean, libVm: LibraryViewModel) {
    var renaming by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    var renameEntry by rememberSaveable { mutableStateOf(-1) }
    val plan = PackPlanner.plan(c)
    val copySuffix = stringResource(R.string.copy_suffix)

    LazyColumn(contentPadding = PaddingValues(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { ScreenHeader(c.name, onBack = libVm::closeOpened) }
        item {
            Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { renaming = true }, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.action_rename)) }
                    OutlinedButton(onClick = { libVm.duplicate(c.id, copySuffix) }, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.action_duplicate)) }
                    OutlinedButton(onClick = { confirmDelete = true }, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.action_delete)) }
                }
                Button(onClick = { libVm.export(c.id) }, enabled = c.entries.isNotEmpty() && !busy, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.action_export_zip))
                }
                if (busy) CircularProgressIndicator(Modifier.size(24.dp))
                if (c.entries.isNotEmpty()) {
                    Text(
                        stringResource(R.string.pack_summary, plan.included.size, plan.excluded.size),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (plan.excluded.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.tertiary,
                    )
                    Text(stringResource(R.string.pack_share_note), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (c.kind == CollectionKind.PACK) {
                    ColorPicker(
                        title = stringResource(R.string.pack_color),
                        selected = c.colorHex,
                        noneLabel = stringResource(R.string.pack_color_original),
                        onSelect = { hex -> libVm.setColor(c.id, hex) },
                    )
                }
            }
        }
        if (c.entries.isEmpty()) {
            item {
                EmptyState(Icons.Filled.List, stringResource(R.string.collection_empty_title), stringResource(R.string.collection_empty_msg))
            }
        }
        itemsIndexed(c.entries, key = { _, e -> e.snapshot.icon.id }) { index, e ->
            val excluded = plan.excluded.firstOrNull { it.first.snapshot.icon == e.snapshot.icon }?.second
            Surface(
                modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface,
            ) {
                Column(Modifier.padding(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        AsyncImage(
                            model = EditParams(colorHex = c.colorHex ?: ICON_COLOR).svgUrl(e.snapshot.icon, 72),
                            contentDescription = e.snapshot.icon.name, modifier = Modifier.size(40.dp),
                        )
                        Column(Modifier.weight(1f)) {
                            Text(e.fileName + ".svg", style = MaterialTheme.typography.bodyMedium)
                            Text(e.snapshot.icon.id, style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { libVm.moveEntry(c.id, index, -1) }, enabled = index > 0) {
                            Icon(Icons.Filled.KeyboardArrowUp, contentDescription = stringResource(R.string.entry_move_up))
                        }
                        IconButton(onClick = { libVm.moveEntry(c.id, index, 1) }, enabled = index < c.entries.size - 1) {
                            Icon(Icons.Filled.KeyboardArrowDown, contentDescription = stringResource(R.string.entry_move_down))
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (excluded != null) {
                            Text(stringResource(R.string.pack_entry_excluded, stringResource(excludeText(excluded))),
                                modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.tertiary)
                        } else {
                            Column(Modifier.weight(1f)) {}
                        }
                        IconButton(onClick = { renameEntry = index }) {
                            Icon(Icons.Filled.Create, contentDescription = stringResource(R.string.entry_rename))
                        }
                        IconButton(onClick = { libVm.removeEntry(c.id, index) }) {
                            Icon(Icons.Filled.Clear, contentDescription = stringResource(R.string.entry_remove))
                        }
                    }
                }
            }
        }
    }

    if (renaming) {
        NameDialog(stringResource(R.string.action_rename), c.name,
            onConfirm = { n -> libVm.rename(c.id, n); renaming = false }, onDismiss = { renaming = false })
    }
    if (renameEntry in c.entries.indices) {
        NameDialog(stringResource(R.string.entry_rename), c.entries[renameEntry].fileName,
            onConfirm = { n -> libVm.renameEntryFile(c.id, renameEntry, n); renameEntry = -1 }, onDismiss = { renameEntry = -1 })
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.delete_title, c.name)) },
            text = { Text(stringResource(R.string.delete_msg)) },
            confirmButton = { TextButton(onClick = { libVm.delete(c.id); confirmDelete = false }) { Text(stringResource(R.string.action_delete)) } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

private fun excludeText(r: ExcludeReason): Int = when (r) {
    ExcludeReason.LICENSE_UNKNOWN -> R.string.exclude_unknown
    ExcludeReason.LICENSE_NOT_RECOGNIZED -> R.string.exclude_review
    ExcludeReason.NON_COMMERCIAL -> R.string.exclude_nc
    ExcludeReason.NO_DERIVATIVES -> R.string.exclude_nd
    ExcludeReason.MODIFICATION_NOT_ALLOWED -> R.string.exclude_modification
}

@Composable
fun NameDialog(title: String, initial: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var text by rememberSaveable { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it.take(LibraryOps.MAX_NAME) },
                label = { Text(stringResource(R.string.collection_name_label)) },
                singleLine = true,
            )
        },
        confirmButton = { TextButton(onClick = { onConfirm(text) }, enabled = text.isNotBlank()) { Text(stringResource(R.string.action_save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

/** Lets the user pick an existing folder/pack or create a new one, then reports the choice. */
@Composable
fun AddToCollectionDialog(
    kind: CollectionKind,
    collections: List<IconCollection>,
    onPick: (String) -> Unit,
    onCreate: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var creating by rememberSaveable { mutableStateOf(false) }
    if (creating) {
        NameDialog(
            stringResource(if (kind == CollectionKind.FOLDER) R.string.new_folder else R.string.new_pack), "",
            onConfirm = { n -> onCreate(n) }, onDismiss = { creating = false },
        )
        return
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (kind == CollectionKind.FOLDER) R.string.dialog_add_folder_title else R.string.dialog_add_pack_title)) },
        text = {
            LazyColumn {
                items(collections.filter { it.kind == kind }, key = { it.id }) { c ->
                    TextButton(onClick = { onPick(c.id) }, modifier = Modifier.fillMaxWidth()) {
                        Text(c.name + "  (" + c.entries.size + ")")
                    }
                }
                item {
                    TextButton(onClick = { creating = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(if (kind == CollectionKind.FOLDER) R.string.new_folder else R.string.new_pack))
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}
