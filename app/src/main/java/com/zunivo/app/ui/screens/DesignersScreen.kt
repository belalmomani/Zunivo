package com.zunivo.app.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.zunivo.app.R
import com.zunivo.app.domain.CollectionKind
import com.zunivo.app.domain.DesignerWork
import com.zunivo.app.domain.IconCollection
import com.zunivo.app.domain.IconRef
import com.zunivo.app.domain.PublishPlanner
import com.zunivo.app.domain.Visibility
import com.zunivo.app.feature.designers.DesignersError
import com.zunivo.app.feature.designers.DesignersMessage
import com.zunivo.app.feature.designers.DesignersUiState
import com.zunivo.app.feature.designers.DesignersViewModel
import com.zunivo.app.feature.library.LibraryViewModel
import com.zunivo.app.ui.components.EmptyState
import com.zunivo.app.ui.components.ScreenHeader
import com.zunivo.app.ui.components.SectionTitle
import com.zunivo.app.ui.navigation.SubScreen
import com.zunivo.app.ui.navigation.icon

@Composable
fun DesignersScreen(onBack: () -> Unit, vm: DesignersViewModel = viewModel(), libVm: LibraryViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val message = state.message
    LaunchedEffect(message) {
        if (message != null) {
            Toast.makeText(context, context.getString(messageRes(message)), Toast.LENGTH_SHORT).show()
            vm.consumeMessage()
        }
    }
    val opened = state.works.firstOrNull { it.id == state.openedWorkId }
    when {
        !state.enabled -> Column {
            ScreenHeader(stringResource(R.string.menu_designers), onBack)
            EmptyState(SubScreen.DESIGNERS.icon(), stringResource(R.string.designers_not_connected_title), stringResource(R.string.designers_not_connected_msg))
        }
        opened != null -> {
            BackHandler { vm.openWork(null) }
            WorkDetail(opened, state, vm, libVm)
        }
        else -> Main(state, vm, libVm, onBack)
    }
}

@Composable
private fun Main(state: DesignersUiState, vm: DesignersViewModel, libVm: LibraryViewModel, onBack: () -> Unit) {
    var tab by rememberSaveable { mutableStateOf(0) }
    Column {
        ScreenHeader(stringResource(R.string.menu_designers), onBack)
        Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = tab == 0, onClick = { tab = 0 }, label = { Text(stringResource(R.string.designers_tab_browse)) })
            FilterChip(selected = tab == 1, onClick = { tab = 1; vm.loadMine() }, label = { Text(stringResource(R.string.designers_tab_mine)) })
        }
        if (state.loading) CircularProgressIndicator(Modifier.padding(16.dp).size(24.dp))
        IssueText(state)
        if (tab == 0) BrowseTab(state, vm) else MineTab(state, vm, libVm)
    }
}

@Composable
private fun IssueText(state: DesignersUiState) {
    state.error?.let {
        Text(stringResource(errorRes(it)), color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
    }
    if (state.validation != null) {
        Text(stringResource(R.string.designers_err_invalid), color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
    }
}

// ------------------------------------------------------------------ browse

@Composable
private fun BrowseTab(state: DesignersUiState, vm: DesignersViewModel) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { OutlinedButton(onClick = vm::loadBrowse, enabled = !state.loading) { Text(stringResource(R.string.designers_refresh)) } }
        item { SectionTitle(stringResource(R.string.designers_works_title)) }
        if (state.browseLoaded && state.works.isEmpty()) item { Text(stringResource(R.string.designers_no_works), color = MaterialTheme.colorScheme.onSurfaceVariant) }
        items(state.works, key = { it.id }) { w ->
            Surface(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable { vm.openWork(w.id) },
                shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surface,
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text(w.title, style = MaterialTheme.typography.bodyLarge)
                    Text(stringResource(R.string.designers_by, state.designerName(w.designerId)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        stringResource(R.string.designers_icons_count, w.iconIds.size) + "  -  " + stringResource(R.string.designers_downloads, state.counts[w.id] ?: 0),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        item { SectionTitle(stringResource(R.string.designers_people_title)) }
        if (state.browseLoaded && state.designers.isEmpty()) item { Text(stringResource(R.string.designers_no_people), color = MaterialTheme.colorScheme.onSurfaceVariant) }
        items(state.designers, key = { it.id }) { d ->
            Column {
                Text(d.displayName, style = MaterialTheme.typography.bodyLarge)
                d.bio?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                if (d.specialties.isNotEmpty()) Text(d.specialties.joinToString(", "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary)
            }
        }
    }
}

@Composable
private fun WorkDetail(w: DesignerWork, state: DesignersUiState, vm: DesignersViewModel, libVm: LibraryViewModel) {
    var reporting by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.verticalScroll(rememberScrollState())) {
        ScreenHeader(w.title, onBack = { vm.openWork(null) })
        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.designers_by, state.designerName(w.designerId)), style = MaterialTheme.typography.bodyMedium)
            w.description?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
            Text(stringResource(R.string.designers_downloads, state.counts[w.id] ?: 0), style = MaterialTheme.typography.bodySmall)
            Text(stringResource(R.string.designers_license_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary)
            IconGrid(w.iconIds)
            IssueText(state)
            if (state.loading) CircularProgressIndicator(Modifier.size(24.dp))
            Button(onClick = { vm.addToPacks(w) { items -> libVm.createPackFrom(w.title, items, null) } }, enabled = !state.loading, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.designers_add_to_packs))
            }
            OutlinedButton(onClick = { reporting = true }, enabled = state.session != null, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.designers_report))
            }
            if (state.session == null) Text(stringResource(R.string.designers_report_signin), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    if (reporting) {
        var reason by rememberSaveable { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { reporting = false },
            title = { Text(stringResource(R.string.designers_report_title)) },
            text = { OutlinedTextField(value = reason, onValueChange = { reason = it.take(2000) }, label = { Text(stringResource(R.string.designers_report_hint)) }) },
            confirmButton = { TextButton(onClick = { vm.report(w.id, reason); reporting = false }, enabled = reason.trim().length >= 10) { Text(stringResource(R.string.designers_report_send)) } },
            dismissButton = { TextButton(onClick = { reporting = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

/** Plain rows (the screen already scrolls). Icons are drawn from Iconify by id. */
@Composable
private fun IconGrid(ids: List<String>) {
    ids.mapNotNull { IconRef.parse(it) }.chunked(4).forEach { row ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            row.forEach { ref ->
                Surface(Modifier.weight(1f).aspectRatio(1f), shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surface) {
                    Box(Modifier.padding(14.dp), contentAlignment = Alignment.Center) {
                        AsyncImage(model = ref.svgUrl(height = 64, colorHex = ICON_COLOR), contentDescription = ref.name, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
            repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
        }
    }
}

// ------------------------------------------------------------------ my page

@Composable
private fun MineTab(state: DesignersUiState, vm: DesignersViewModel, libVm: LibraryViewModel) {
    val lib by libVm.state.collectAsStateWithLifecycle()
    var email by rememberSaveable { mutableStateOf(state.email) }
    var code by rememberSaveable { mutableStateOf("") }
    var editing by rememberSaveable { mutableStateOf(false) }
    var pickingPack by rememberSaveable { mutableStateOf(false) }
    var publishing by rememberSaveable { mutableStateOf<String?>(null) }
    var confirmPublic by rememberSaveable { mutableStateOf<String?>(null) }
    var confirmDeletePage by rememberSaveable { mutableStateOf(false) }

    Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        val session = state.session
        if (session == null) {
            SectionTitle(stringResource(R.string.designers_sign_in_title))
            Text(stringResource(R.string.designers_sign_in_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(value = email, onValueChange = { email = it.take(120) }, label = { Text(stringResource(R.string.designers_email)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Button(onClick = { vm.requestCode(email) }, enabled = email.isNotBlank() && !state.loading, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.designers_send_code)) }
            if (state.awaitingCode) {
                OutlinedTextField(value = code, onValueChange = { code = it.filter(Char::isLetterOrDigit).take(10) }, label = { Text(stringResource(R.string.designers_code)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Button(onClick = { vm.verifyCode(code) }, enabled = code.length >= 4 && !state.loading, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.designers_verify)) }
            }
            return@Column
        }

        val profile = state.myProfile
        if (!state.myLoaded) {
            OutlinedButton(onClick = vm::loadMine, enabled = !state.loading) { Text(stringResource(R.string.action_retry)) }
        } else if (profile == null || editing) {
            SectionTitle(stringResource(R.string.designers_profile_title))
            var name by rememberSaveable(profile?.id) { mutableStateOf(profile?.displayName.orEmpty()) }
            var bio by rememberSaveable(profile?.id) { mutableStateOf(profile?.bio.orEmpty()) }
            var specs by rememberSaveable(profile?.id) { mutableStateOf(profile?.specialties?.joinToString(", ").orEmpty()) }
            OutlinedTextField(value = name, onValueChange = { name = it.take(60) }, label = { Text(stringResource(R.string.designers_name)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = bio, onValueChange = { bio = it.take(500) }, label = { Text(stringResource(R.string.designers_bio)) }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = specs, onValueChange = { specs = it.take(300) }, label = { Text(stringResource(R.string.designers_specialties)) }, modifier = Modifier.fillMaxWidth())
            Button(onClick = { vm.saveProfile(name, bio, specs); editing = false }, enabled = name.trim().length >= 2 && !state.loading, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.designers_save_profile))
            }
        } else {
            Text(profile.displayName, style = MaterialTheme.typography.titleLarge)
            profile.bio?.let { Text(it) }
            if (profile.specialties.isNotEmpty()) Text(profile.specialties.joinToString(", "), color = MaterialTheme.colorScheme.tertiary)
            OutlinedButton(onClick = { editing = true }) { Text(stringResource(R.string.designers_edit_profile)) }

            SectionTitle(stringResource(R.string.designers_my_works))
            Text(stringResource(R.string.designers_publish_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onClick = { pickingPack = true }, enabled = !state.loading, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.designers_publish_pack)) }
            state.myWorks.forEach { w ->
                Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(w.title, style = MaterialTheme.typography.bodyLarge)
                                Text(stringResource(R.string.designers_icons_count, w.iconIds.size), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            IconButton(onClick = { vm.deleteWork(w.id) }) { Icon(Icons.Filled.Clear, contentDescription = stringResource(R.string.action_delete)) }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Switch(checked = w.visibility == Visibility.PUBLIC, onCheckedChange = { on -> if (on) confirmPublic = w.id else vm.setVisibility(w, false) })
                            Text(stringResource(if (w.visibility == Visibility.PUBLIC) R.string.designers_is_public else R.string.designers_is_private))
                        }
                    }
                }
            }
            OutlinedButton(onClick = { confirmDeletePage = true }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.designers_delete_page), color = MaterialTheme.colorScheme.error)
            }
        }
        OutlinedButton(onClick = vm::signOut, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.designers_sign_out)) }
    }

    if (pickingPack) {
        val packs = lib.library.collections.filter { it.kind == CollectionKind.PACK && it.entries.isNotEmpty() }
        AlertDialog(
            onDismissRequest = { pickingPack = false },
            title = { Text(stringResource(R.string.designers_choose_pack)) },
            text = {
                LazyColumn {
                    if (packs.isEmpty()) item { Text(stringResource(R.string.designers_no_packs)) }
                    items(packs, key = { it.id }) { c -> TextButton(onClick = { publishing = c.id; pickingPack = false }, modifier = Modifier.fillMaxWidth()) { Text(c.name) } }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { pickingPack = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
    publishing?.let { id ->
        val pack: IconCollection? = lib.library.collections.firstOrNull { it.id == id }
        if (pack != null) {
            val plan = PublishPlanner.plan(pack)
            var title by rememberSaveable(id) { mutableStateOf(pack.name) }
            var desc by rememberSaveable(id) { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { publishing = null },
                title = { Text(stringResource(R.string.designers_publish_pack)) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.designers_publish_summary, plan.iconIds.size, plan.excluded), style = MaterialTheme.typography.bodyMedium)
                        OutlinedTextField(value = title, onValueChange = { title = it.take(80) }, label = { Text(stringResource(R.string.designers_work_title)) }, singleLine = true)
                        OutlinedTextField(value = desc, onValueChange = { desc = it.take(1000) }, label = { Text(stringResource(R.string.designers_work_desc)) })
                    }
                },
                confirmButton = { TextButton(onClick = { vm.publishPack(pack, title, desc); publishing = null }, enabled = plan.iconIds.isNotEmpty() && title.trim().length >= 2) { Text(stringResource(R.string.designers_publish)) } },
                dismissButton = { TextButton(onClick = { publishing = null }) { Text(stringResource(R.string.action_cancel)) } },
            )
        }
    }
    confirmPublic?.let { id ->
        val w = state.myWorks.firstOrNull { it.id == id }
        AlertDialog(
            onDismissRequest = { confirmPublic = null },
            title = { Text(stringResource(R.string.designers_public_confirm_title)) },
            text = { Text(stringResource(R.string.designers_public_confirm_msg)) },
            confirmButton = { TextButton(onClick = { if (w != null) vm.setVisibility(w, true); confirmPublic = null }) { Text(stringResource(R.string.designers_make_public)) } },
            dismissButton = { TextButton(onClick = { confirmPublic = null }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
    if (confirmDeletePage) {
        AlertDialog(
            onDismissRequest = { confirmDeletePage = false },
            title = { Text(stringResource(R.string.designers_delete_page)) },
            text = { Text(stringResource(R.string.designers_delete_page_msg)) },
            confirmButton = { TextButton(onClick = { vm.deleteMyPage(); confirmDeletePage = false }) { Text(stringResource(R.string.action_delete)) } },
            dismissButton = { TextButton(onClick = { confirmDeletePage = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

private fun errorRes(e: DesignersError): Int = when (e) {
    DesignersError.OFFLINE -> R.string.error_offline_msg
    DesignersError.TIMEOUT -> R.string.error_timeout_msg
    DesignersError.UNAUTHORIZED -> R.string.designers_err_unauthorized
    DesignersError.RATE_LIMITED -> R.string.store_err_rate
    DesignersError.REJECTED -> R.string.designers_err_rejected
    DesignersError.SERVER -> R.string.store_err_server
    DesignersError.BAD_DATA -> R.string.error_bad_data_msg
    DesignersError.INVALID_INPUT -> R.string.designers_err_invalid
}

private fun messageRes(m: DesignersMessage): Int = when (m) {
    DesignersMessage.CODE_SENT -> R.string.dm_code_sent
    DesignersMessage.SIGNED_IN -> R.string.dm_signed_in
    DesignersMessage.SIGNED_OUT -> R.string.dm_signed_out
    DesignersMessage.PROFILE_SAVED -> R.string.dm_profile_saved
    DesignersMessage.PAGE_DELETED -> R.string.dm_page_deleted
    DesignersMessage.PUBLISHED -> R.string.dm_published
    DesignersMessage.WORK_UPDATED -> R.string.dm_work_updated
    DesignersMessage.WORK_DELETED -> R.string.msg_deleted
    DesignersMessage.ADDED_TO_PACKS -> R.string.dm_added_to_packs
    DesignersMessage.REPORTED -> R.string.dm_reported
    DesignersMessage.NOTHING_TO_PUBLISH -> R.string.dm_nothing_to_publish
}
