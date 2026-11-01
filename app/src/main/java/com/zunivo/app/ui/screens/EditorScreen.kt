package com.zunivo.app.ui.screens

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Create
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.zunivo.app.R
import com.zunivo.app.domain.EditParams
import com.zunivo.app.domain.HexColor
import com.zunivo.app.domain.IconRef
import com.zunivo.app.domain.LicenseKind
import com.zunivo.app.domain.LicensePolicy
import com.zunivo.app.feature.editor.EditSession
import com.zunivo.app.feature.editor.EditorMessage
import com.zunivo.app.feature.editor.EditorViewModel
import com.zunivo.app.feature.editor.ExportFormat
import com.zunivo.app.feature.editor.ShareRequest
import com.zunivo.app.ui.components.ColorSwatch
import com.zunivo.app.ui.components.EmptyState
import com.zunivo.app.ui.components.ScreenHeader
import com.zunivo.app.ui.components.SectionTitle
import com.zunivo.app.ui.components.parseHexColor
import kotlin.math.roundToInt

private val SWATCHES = listOf(
    "#FFFFFF", "#000000", "#6366F1", "#8B5CF6", "#06B6D4", "#10B981", "#F59E0B", "#F43F5E", "#64748B", "#0B1020",
)

private enum class PreviewContext(val labelRes: Int) {
    LIGHT(R.string.preview_light), DARK(R.string.preview_dark), BUTTON(R.string.preview_button),
    PHONE(R.string.preview_phone), CARD(R.string.preview_card), NAV(R.string.preview_nav),
}

@Composable
fun EditorScreen(onBack: () -> Unit, onGoSearch: () -> Unit, vm: EditorViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val message = state.message
    LaunchedEffect(message) {
        if (message != null) {
            Toast.makeText(context, context.getString(messageRes(message)), Toast.LENGTH_SHORT).show()
            vm.consumeMessage()
        }
    }
    val share = state.share
    LaunchedEffect(share) {
        if (share != null) {
            launchShare(context, share)
            vm.consumeShare()
        }
    }

    val session = state.session
    if (session == null) {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            ScreenHeader(stringResource(R.string.menu_editor), onBack)
            if (state.saved.isEmpty()) {
                EmptyState(
                    Icons.Filled.Create,
                    stringResource(R.string.editor_empty_title),
                    stringResource(R.string.editor_empty_msg),
                )
            }
            Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onGoSearch, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.editor_go_search))
                }
                if (state.saved.isNotEmpty()) {
                    SectionTitle(stringResource(R.string.editor_saved_title))
                    state.saved.forEach { edit ->
                        Surface(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable { vm.reopen(edit) },
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surface,
                        ) {
                            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                IconPreview(edit.icon, edit.params, 44.dp)
                                Column(Modifier.weight(1f)) {
                                    Text(edit.icon.name, style = MaterialTheme.typography.bodyLarge)
                                    Text(edit.setName ?: edit.icon.prefix, style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                IconButton(onClick = { vm.delete(edit.id) }) {
                                    Icon(Icons.Filled.Clear, contentDescription = stringResource(R.string.action_delete))
                                }
                            }
                        }
                    }
                }
            }
        }
    } else {
        BackHandler { vm.close() }
        Workspace(session, state.busy, vm)
    }
}

@Composable
private fun Workspace(session: EditSession, busy: Boolean, vm: EditorViewModel) {
    val params = session.params
    var previewIndex by rememberSaveable { mutableStateOf(0) }
    var compare by rememberSaveable { mutableStateOf(false) }
    val assessment = LicensePolicy.assess(session.set?.licenseSpdx)
    val locked = assessment.modificationOk == false

    Column(
        Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ScreenHeader(session.icon.name, onBack = vm::close)
        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(PreviewContext.entries) { ctx ->
                    FilterChip(
                        selected = previewIndex == ctx.ordinal,
                        onClick = { previewIndex = ctx.ordinal },
                        label = { Text(stringResource(ctx.labelRes)) },
                    )
                }
            }
            PreviewStage(PreviewContext.entries[previewIndex], session.icon, params)

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Switch(checked = compare, onCheckedChange = { compare = it })
                Text(stringResource(R.string.editor_compare))
            }
            if (compare) CompareRow(session.icon, params)

            ColorPicker(
                title = stringResource(R.string.editor_color),
                selected = params.colorHex,
                noneLabel = stringResource(R.string.editor_color_original),
                onSelect = { hex -> vm.update { it.copy(colorHex = hex) } },
            )
            ColorPicker(
                title = stringResource(R.string.editor_background),
                selected = params.bgHex,
                noneLabel = stringResource(R.string.editor_bg_none),
                onSelect = { hex -> vm.update { it.copy(bgHex = hex) } },
            )

            Text(stringResource(R.string.editor_size, params.sizePx), style = MaterialTheme.typography.titleSmall)
            Slider(
                value = params.sizePx.toFloat(),
                onValueChange = { v -> vm.update { it.copy(sizePx = v.roundToInt()) } },
                valueRange = EditParams.MIN_SIZE.toFloat()..EditParams.MAX_SIZE.toFloat(),
            )
            Text(stringResource(R.string.editor_padding, params.paddingPct), style = MaterialTheme.typography.titleSmall)
            Slider(
                value = params.paddingPct.toFloat(),
                onValueChange = { v -> vm.update { it.copy(paddingPct = v.roundToInt()) } },
                valueRange = 0f..EditParams.MAX_PADDING.toFloat(),
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = { vm.update { it.copy(rotation = it.rotation + 90) } }) {
                    Text(stringResource(R.string.editor_rotate))
                }
                FilterChip(
                    selected = params.flipH,
                    onClick = { vm.update { it.copy(flipH = !it.flipH) } },
                    label = { Text(stringResource(R.string.editor_flip_h)) },
                )
                FilterChip(
                    selected = params.flipV,
                    onClick = { vm.update { it.copy(flipV = !it.flipV) } },
                    label = { Text(stringResource(R.string.editor_flip_v)) },
                )
            }

            SectionTitle(stringResource(R.string.detail_license_notes))
            Text(
                stringResource(licenseMessage(assessment.kind)),
                style = MaterialTheme.typography.bodyMedium,
                color = if (assessment.kind == LicenseKind.PERMISSIVE || assessment.kind == LicenseKind.ATTRIBUTION)
                    MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.tertiary,
            )
            if (locked) {
                Text(stringResource(R.string.msg_blocked_nd), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = vm::reset, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.editor_reset))
                }
                Button(onClick = vm::save, enabled = session.dirty && !locked, modifier = Modifier.weight(1f)) {
                    Text(stringResource(if (session.dirty) R.string.editor_save else R.string.editor_saved_state))
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { vm.export(ExportFormat.SVG) }, enabled = !busy && !locked,
                    modifier = Modifier.weight(1f)) { Text(stringResource(R.string.editor_export_svg)) }
                OutlinedButton(onClick = { vm.export(ExportFormat.PNG) }, enabled = !busy && !locked,
                    modifier = Modifier.weight(1f)) { Text(stringResource(R.string.editor_export_png)) }
            }
            if (busy) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator(Modifier.size(24.dp)) }
            }
            Text(stringResource(R.string.editor_export_note), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
internal fun ColorPicker(title: String, selected: String?, noneLabel: String, onSelect: (String?) -> Unit) {
    var custom by rememberSaveable(selected) { mutableStateOf(selected.orEmpty()) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionTitle(title)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            item {
                FilterChip(selected = selected == null, onClick = { onSelect(null) }, label = { Text(noneLabel) })
            }
            items(SWATCHES) { hex -> ColorSwatch(hex, selected == hex, onClick = { onSelect(hex) }) }
        }
        OutlinedTextField(
            value = custom,
            onValueChange = { v ->
                custom = v.take(7)
                HexColor.normalize(v)?.let { onSelect(it) }
            },
            label = { Text(stringResource(R.string.editor_hex_label)) },
            singleLine = true,
            isError = custom.isNotEmpty() && HexColor.normalize(custom) == null,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** The icon with the edit applied, on its own (optional) background and padding. */
@Composable
private fun IconPreview(ref: IconRef, params: EditParams, box: Dp) {
    val bg = parseHexColor(params.bgHex) ?: Color.Transparent
    val requestHeight = (box.value * 3).toInt().coerceIn(48, 512)
    Box(
        Modifier
            .size(box)
            .clip(RoundedCornerShape(box / 6))
            .background(bg)
            .padding(box * params.paddingPct / 100),
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = params.svgUrl(ref, height = requestHeight),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun PreviewStage(context: PreviewContext, ref: IconRef, params: EditParams) {
    val light = Color(0xFFF1F3FF)
    val dark = Color(0xFF0B1020)
    val stageBg = if (context == PreviewContext.LIGHT || context == PreviewContext.CARD) light else dark
    Box(
        Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(20.dp)).background(stageBg),
        contentAlignment = Alignment.Center,
    ) {
        when (context) {
            PreviewContext.LIGHT, PreviewContext.DARK -> IconPreview(ref, params, 112.dp)
            PreviewContext.BUTTON -> Button(onClick = {}) {
                IconPreview(ref, params, 28.dp)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.preview_button_label))
            }
            PreviewContext.PHONE -> Box(
                Modifier.size(width = 110.dp, height = 196.dp).clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF141A30)).border(2.dp, Color(0xFF3A4270), RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.size(width = 50.dp, height = 6.dp).clip(CircleShape).background(Color(0xFF3A4270)))
                    IconPreview(ref, params, 56.dp)
                    Box(Modifier.size(width = 70.dp, height = 8.dp).clip(CircleShape).background(Color(0xFF3A4270)))
                    Box(Modifier.size(width = 54.dp, height = 8.dp).clip(CircleShape).background(Color(0xFF3A4270)))
                }
            }
            PreviewContext.CARD -> Surface(
                shape = RoundedCornerShape(16.dp), color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFD9DDF2)),
                modifier = Modifier.width(150.dp),
            ) {
                Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        Modifier.fillMaxWidth().height(80.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xFFE6E9FA)),
                        contentAlignment = Alignment.Center,
                    ) { IconPreview(ref, params, 52.dp) }
                    Text(stringResource(R.string.preview_product_title), color = Color(0xFF0B1020),
                        style = MaterialTheme.typography.labelLarge)
                    Text(stringResource(R.string.preview_product_price), color = Color(0xFF6366F1),
                        style = MaterialTheme.typography.labelMedium)
                }
            }
            PreviewContext.NAV -> Row(
                Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconPreview(ref, params, 40.dp)
                repeat(3) { Box(Modifier.size(24.dp).clip(CircleShape).background(Color(0xFF3A4270))) }
            }
        }
    }
}

@Composable
private fun CompareRow(ref: IconRef, params: EditParams) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color(0xFFF1F3FF)).padding(16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            IconPreview(ref, EditParams(paddingPct = 0), 80.dp)
            Text(stringResource(R.string.editor_original), color = Color(0xFF0B1020), style = MaterialTheme.typography.labelMedium)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            IconPreview(ref, params, 80.dp)
            Text(stringResource(R.string.editor_edited), color = Color(0xFF0B1020), style = MaterialTheme.typography.labelMedium)
        }
    }
}

private fun messageRes(m: EditorMessage): Int = when (m) {
    EditorMessage.SAVED -> R.string.msg_saved
    EditorMessage.DELETED -> R.string.msg_deleted
    EditorMessage.RESET -> R.string.msg_reset
    EditorMessage.BLOCKED_NO_DERIVATIVES -> R.string.msg_blocked_nd
    EditorMessage.EXPORT_FAILED -> R.string.msg_export_failed
    EditorMessage.EXPORT_UNSAFE -> R.string.msg_export_unsafe
    EditorMessage.OFFLINE -> R.string.msg_offline
}

/** Shares the exported file through the system share sheet (so the user chooses where it is saved). */
internal fun launchShare(context: Context, share: ShareRequest) {
    try {
        val uri = FileProvider.getUriForFile(context, "com.zunivo.app.fileprovider", share.file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = share.mime
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, share.text)
            clipData = ClipData.newRawUri(null, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(send, null))
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, R.string.msg_export_failed, Toast.LENGTH_SHORT).show()
    } catch (e: IllegalArgumentException) {
        Toast.makeText(context, R.string.msg_export_failed, Toast.LENGTH_SHORT).show()
    }
}
