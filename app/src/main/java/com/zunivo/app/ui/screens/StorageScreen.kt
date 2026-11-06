package com.zunivo.app.ui.screens

import android.app.ActivityManager
import android.content.Context
import android.text.format.Formatter
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.SingletonImageLoader
import com.zunivo.app.R
import com.zunivo.app.ui.components.ScreenHeader
import com.zunivo.app.ui.components.SectionTitle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

private data class StorageSizes(val images: Long, val temp: Long, val data: Long)

private fun dirSize(f: File): Long = if (!f.exists()) 0 else if (f.isFile) f.length() else (f.listFiles()?.sumOf { dirSize(it) } ?: 0)

private fun measure(context: Context): StorageSizes = StorageSizes(
    images = SingletonImageLoader.get(context).diskCache?.size ?: 0,
    temp = dirSize(File(context.cacheDir, "exports")) + dirSize(File(context.cacheDir, "updates")),
    data = dirSize(context.filesDir),
)

@Composable
fun StorageScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var tick by remember { mutableStateOf(0) }
    var sizes by remember { mutableStateOf<List<Long>?>(null) }
    var confirmWipe by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(tick) {
        val s = withContext(Dispatchers.IO) { measure(context) }
        sizes = listOf(s.images, s.temp, s.data)
    }

    fun size(i: Int): String = sizes?.get(i)?.let { Formatter.formatShortFileSize(context, it) } ?: context.getString(R.string.storage_measuring)

    Column(Modifier.verticalScroll(rememberScrollState())) {
        ScreenHeader(stringResource(R.string.menu_storage), onBack)
        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.storage_intro), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(stringResource(R.string.storage_images, size(0)))
            Text(stringResource(R.string.storage_temp, size(1)))
            Text(stringResource(R.string.storage_data, size(2)))
            Button(
                onClick = {
                    val loader = SingletonImageLoader.get(context)
                    loader.diskCache?.clear()
                    loader.memoryCache?.clear()
                    File(context.cacheDir, "exports").deleteRecursively()
                    File(context.cacheDir, "updates").deleteRecursively()
                    Toast.makeText(context, R.string.storage_cache_cleared, Toast.LENGTH_SHORT).show()
                    tick++
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.storage_clear_cache)) }

            SectionTitle(stringResource(R.string.storage_danger))
            Text(stringResource(R.string.storage_delete_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedButton(onClick = { confirmWipe = true }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.storage_delete_all), color = MaterialTheme.colorScheme.error)
            }
        }
    }

    if (confirmWipe) {
        AlertDialog(
            onDismissRequest = { confirmWipe = false },
            title = { Text(stringResource(R.string.storage_delete_title)) },
            text = { Text(stringResource(R.string.storage_delete_msg)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmWipe = false
                    // Android's own "clear app data": removes everything and restarts the app from scratch.
                    (context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).clearApplicationUserData()
                }) { Text(stringResource(R.string.storage_delete_confirm), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmWipe = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}
