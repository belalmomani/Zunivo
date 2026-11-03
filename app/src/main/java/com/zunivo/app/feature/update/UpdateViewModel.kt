package com.zunivo.app.feature.update

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.zunivo.app.BuildConfig
import com.zunivo.app.data.remote.GitHubApi
import com.zunivo.app.data.remote.GitHubException
import com.zunivo.app.domain.ChecksumFile
import com.zunivo.app.domain.FileNames
import com.zunivo.app.domain.GitHubHosts
import com.zunivo.app.domain.GitHubUrl
import com.zunivo.app.domain.ReleaseUpdate
import com.zunivo.app.domain.VersionCompare
import com.zunivo.app.core.AppInfo
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest

enum class UpdateStatus { IDLE, CHECKING, UP_TO_DATE, NO_RELEASE, AVAILABLE, DOWNLOADING, READY, ERROR }

enum class UpdateError { OFFLINE, TIMEOUT, RATE_LIMITED, SERVER, BAD_DATA, TOO_LARGE, CHECKSUM_MISMATCH, WRONG_PACKAGE, NOT_NEWER, SIGNATURE_MISMATCH, UNREADABLE }

enum class ChecksumState { VERIFIED, NOT_AVAILABLE }

data class UpdateUiState(
    val status: UpdateStatus = UpdateStatus.IDLE,
    val release: ReleaseUpdate? = null,
    val bytes: Long = 0,
    val total: Long = 0,
    val error: UpdateError? = null,
    val checksum: ChecksumState? = null,
    val file: File? = null,
)

class UpdateViewModel(app: Application) : AndroidViewModel(app) {
    private val api = GitHubApi()
    private val _state = MutableStateFlow(UpdateUiState())
    val state: StateFlow<UpdateUiState> = _state.asStateFlow()
    private val repo = GitHubUrl.parse(AppInfo.REPO_URL)!!
    private val dir get() = File(getApplication<Application>().cacheDir, "updates").apply { mkdirs() }

    fun check() {
        if (!BuildConfig.SELF_UPDATE_ENABLED) return
        val s = _state.value.status
        if (s == UpdateStatus.CHECKING || s == UpdateStatus.DOWNLOADING) return
        _state.value = UpdateUiState(status = UpdateStatus.CHECKING)
        viewModelScope.launch {
            try {
                val latest = api.latestRelease(repo)
                _state.value = when {
                    latest == null -> UpdateUiState(status = UpdateStatus.NO_RELEASE)
                    VersionCompare.isNewer(latest.version, BuildConfig.VERSION_NAME) ->
                        UpdateUiState(status = UpdateStatus.AVAILABLE, release = latest, total = latest.apkSize)
                    else -> UpdateUiState(status = UpdateStatus.UP_TO_DATE, release = latest)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: GitHubException) {
                _state.value = UpdateUiState(status = UpdateStatus.ERROR, error = errorOf(e))
            }
        }
    }

    fun download() {
        val r = _state.value.release ?: return
        if (_state.value.status != UpdateStatus.AVAILABLE && _state.value.status != UpdateStatus.ERROR) return
        if (!GitHubHosts.isAllowedUrl(r.apkUrl)) return
        _state.update { it.copy(status = UpdateStatus.DOWNLOADING, bytes = 0, error = null, checksum = null, file = null) }
        viewModelScope.launch {
            val file = File(dir, "zunivo-" + FileNames.sanitize(r.version) + ".apk")
            try {
                withContext(Dispatchers.IO) {
                    dir.listFiles()?.forEach { if (it != file) it.delete() }     // keep only the current download
                    api.download(r.apkUrl, file, MAX_APK) { done, total ->
                        _state.update { st -> st.copy(bytes = done, total = if (total > 0) total else st.total) }
                    }
                }
                val hash = withContext(Dispatchers.IO) { sha256(file) }
                val expected = r.sha256 ?: expectedFromChecksumAsset(r)
                if (expected != null && !expected.equals(hash, ignoreCase = true)) {
                    file.delete()
                    fail(UpdateError.CHECKSUM_MISMATCH)
                    return@launch
                }
                val verdict = ApkVerifier.verify(getApplication<Application>(), file)
                if (verdict != ApkVerifier.Result.OK) {
                    file.delete()
                    fail(
                        when (verdict) {
                            ApkVerifier.Result.WRONG_PACKAGE -> UpdateError.WRONG_PACKAGE
                            ApkVerifier.Result.NOT_NEWER -> UpdateError.NOT_NEWER
                            ApkVerifier.Result.SIGNATURE_MISMATCH -> UpdateError.SIGNATURE_MISMATCH
                            else -> UpdateError.UNREADABLE
                        },
                    )
                    return@launch
                }
                _state.update {
                    it.copy(status = UpdateStatus.READY, file = file,
                        checksum = if (expected != null) ChecksumState.VERIFIED else ChecksumState.NOT_AVAILABLE)
                }
            } catch (e: CancellationException) {
                file.delete()
                throw e
            } catch (e: GitHubException) {
                file.delete()
                fail(errorOf(e))
            }
        }
    }

    private suspend fun expectedFromChecksumAsset(r: ReleaseUpdate): String? {
        val url = r.checksumUrl?.takeIf { GitHubHosts.isAllowedUrl(it) } ?: return null
        val tmp = File(dir, "checksum.txt")
        return try {
            withContext(Dispatchers.IO) {
                api.download(url, tmp, 4_096) { _, _ -> }
                ChecksumFile.parse(tmp.readText(Charsets.UTF_8), r.apkName)
            }
        } catch (e: GitHubException) {
            null
        } finally {
            tmp.delete()
        }
    }

    private fun fail(e: UpdateError) = _state.update { it.copy(status = UpdateStatus.ERROR, error = e, file = null) }

    private fun errorOf(e: GitHubException): UpdateError = when (e) {
        is GitHubException.Offline -> UpdateError.OFFLINE
        is GitHubException.Timeout -> UpdateError.TIMEOUT
        is GitHubException.RateLimited -> UpdateError.RATE_LIMITED
        is GitHubException.Http, is GitHubException.NotFound -> UpdateError.SERVER
        is GitHubException.TooLarge -> UpdateError.TOO_LARGE
        is GitHubException.BadData, is GitHubException.Blocked -> UpdateError.BAD_DATA
    }

    private fun sha256(file: File): String {
        val md = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buf = ByteArray(32 * 1024)
            while (true) {
                val n = input.read(buf)
                if (n < 0) break
                md.update(buf, 0, n)
            }
        }
        return md.digest().joinToString("") { "%02x".format(it) }
    }

    private companion object {
        const val MAX_APK = 200L * 1024 * 1024
    }
}
