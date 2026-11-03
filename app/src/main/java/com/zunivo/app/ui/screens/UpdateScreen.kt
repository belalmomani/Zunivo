package com.zunivo.app.ui.screens

import android.text.format.Formatter
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zunivo.app.BuildConfig
import com.zunivo.app.R
import com.zunivo.app.ads.AdsConfig
import com.zunivo.app.feature.update.ChecksumState
import com.zunivo.app.feature.update.UpdateError
import com.zunivo.app.feature.update.UpdateStatus
import com.zunivo.app.feature.update.UpdateViewModel
import com.zunivo.app.ui.components.ScreenHeader
import com.zunivo.app.ui.components.SectionTitle
import com.zunivo.app.ui.installApk

@Composable
fun UpdateScreen(onBack: () -> Unit, vm: UpdateViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    Column(Modifier.verticalScroll(rememberScrollState())) {
        ScreenHeader(stringResource(R.string.menu_update), onBack)
        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.update_installed, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(
                    when {
                        !AdsConfig.enabled -> R.string.update_ads_off
                        AdsConfig.testMode -> R.string.update_ads_test
                        else -> R.string.update_ads_on
                    },
                ),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            SectionTitle(stringResource(R.string.update_changelog_title))
            Text(stringResource(R.string.changelog_text), style = MaterialTheme.typography.bodyMedium)

            if (!BuildConfig.SELF_UPDATE_ENABLED) {
                Text(stringResource(R.string.update_play_note), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.tertiary)
                return@Column
            }

            Text(stringResource(R.string.update_how), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            val busy = state.status == UpdateStatus.CHECKING || state.status == UpdateStatus.DOWNLOADING
            Button(onClick = vm::check, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.update_check)) }
            if (state.status == UpdateStatus.CHECKING) CircularProgressIndicator(Modifier.size(24.dp))

            when (state.status) {
                UpdateStatus.UP_TO_DATE -> Text(stringResource(R.string.update_up_to_date), style = MaterialTheme.typography.bodyLarge)
                UpdateStatus.NO_RELEASE -> Text(stringResource(R.string.update_no_release), style = MaterialTheme.typography.bodyLarge)
                UpdateStatus.ERROR -> {
                    Text(stringResource(errorRes(state.error)), color = MaterialTheme.colorScheme.error)
                    if (state.release != null) OutlinedButton(onClick = vm::download, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.action_retry)) }
                }
                UpdateStatus.AVAILABLE, UpdateStatus.DOWNLOADING, UpdateStatus.READY -> {
                    val r = state.release
                    if (r != null) {
                        SectionTitle(stringResource(R.string.update_available, r.version))
                        if (r.apkSize > 0) Text(stringResource(R.string.store_size_line, Formatter.formatShortFileSize(context, r.apkSize)))
                        r.notes?.let { Text(it.take(3000), style = MaterialTheme.typography.bodyMedium) }
                        if (state.status == UpdateStatus.AVAILABLE) {
                            Text(stringResource(R.string.update_warning), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary)
                            Button(onClick = vm::download, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.update_download)) }
                        }
                        if (state.status == UpdateStatus.DOWNLOADING) {
                            if (state.total > 0) LinearProgressIndicator(progress = { (state.bytes.toFloat() / state.total).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
                            else LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                            Text(Formatter.formatShortFileSize(context, state.bytes), style = MaterialTheme.typography.bodySmall)
                        }
                        val file = state.file
                        if (state.status == UpdateStatus.READY && file != null) {
                            Text(stringResource(R.string.update_signature_ok), style = MaterialTheme.typography.bodyMedium)
                            Text(
                                stringResource(if (state.checksum == ChecksumState.VERIFIED) R.string.update_checksum_ok else R.string.update_checksum_none),
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (state.checksum == ChecksumState.VERIFIED) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.tertiary,
                            )
                            Button(onClick = { installApk(context, file) }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.update_install)) }
                        }
                    }
                }
                else -> Unit
            }
        }
    }
}

private fun errorRes(e: UpdateError?): Int = when (e) {
    UpdateError.OFFLINE -> R.string.error_offline_msg
    UpdateError.TIMEOUT -> R.string.error_timeout_msg
    UpdateError.RATE_LIMITED -> R.string.store_err_rate
    UpdateError.SERVER -> R.string.store_err_server
    UpdateError.BAD_DATA, null -> R.string.error_bad_data_msg
    UpdateError.TOO_LARGE -> R.string.note_too_large
    UpdateError.CHECKSUM_MISMATCH -> R.string.update_err_checksum
    UpdateError.WRONG_PACKAGE -> R.string.update_err_package
    UpdateError.NOT_NEWER -> R.string.update_err_not_newer
    UpdateError.SIGNATURE_MISMATCH -> R.string.update_err_signature
    UpdateError.UNREADABLE -> R.string.update_err_unreadable
}

@Composable
fun LicensesScreen(onBack: () -> Unit) {
    Column(Modifier.verticalScroll(rememberScrollState())) {
        ScreenHeader(stringResource(R.string.menu_licenses), onBack)
        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.licenses_icons_note), style = MaterialTheme.typography.bodyMedium)
            SectionTitle(stringResource(R.string.licenses_libraries))
            Text(stringResource(R.string.licenses_list), style = MaterialTheme.typography.bodyMedium)
        }
    }
}
