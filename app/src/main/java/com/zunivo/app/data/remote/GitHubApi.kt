package com.zunivo.app.data.remote

import com.zunivo.app.domain.Fetched
import com.zunivo.app.domain.GitHubHosts
import com.zunivo.app.domain.ReleaseInfo
import com.zunivo.app.domain.RepoInfo
import com.zunivo.app.domain.RepoRef
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONException
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URI
import java.net.URL

sealed class GitHubException(message: String) : Exception(message) {
    class NotFound : GitHubException("not found")
    class RateLimited : GitHubException("rate limited")
    class Http(val code: Int) : GitHubException("http $code")
    class Offline : GitHubException("offline")
    class Timeout : GitHubException("timeout")
    class BadData : GitHubException("bad data")
    class Blocked : GitHubException("blocked host")
    class TooLarge : GitHubException("too large")
}

/**
 * Public GitHub REST API client (no token, public repositories only).
 * Only https URLs on GitHub's own hosts are contacted, including every redirect.
 */
class GitHubApi {
    private val api = "https://api.github.com"

    suspend fun repo(ref: RepoRef): RepoInfo = withContext(Dispatchers.IO) {
        val text = getText("$api/repos/${ref.owner}/${ref.repo}")
        try { GitHubParser.parseRepo(text) } catch (e: JSONException) { throw GitHubException.BadData() }
    }

    suspend fun readme(ref: RepoRef): Fetched<String> = optionalText("$api/repos/${ref.owner}/${ref.repo}/readme")

    suspend fun fileText(ref: RepoRef, path: String): Fetched<String> =
        optionalText("$api/repos/${ref.owner}/${ref.repo}/contents/$path")

    suspend fun releases(ref: RepoRef): Fetched<List<ReleaseInfo>> = withContext(Dispatchers.IO) {
        try {
            Fetched.Found(GitHubParser.parseReleases(getText("$api/repos/${ref.owner}/${ref.repo}/releases?per_page=10")))
        } catch (e: GitHubException.NotFound) {
            Fetched.Missing
        } catch (e: GitHubException) {
            Fetched.Failed
        } catch (e: JSONException) {
            Fetched.Failed
        }
    }

    private suspend fun optionalText(url: String): Fetched<String> = withContext(Dispatchers.IO) {
        try {
            val text = GitHubParser.parseFileText(getText(url))
            if (text == null) Fetched.Failed else Fetched.Found(text)
        } catch (e: GitHubException.NotFound) {
            Fetched.Missing
        } catch (e: GitHubException) {
            Fetched.Failed
        } catch (e: JSONException) {
            Fetched.Failed
        }
    }

    /** Streams a file to [dest]. Follows at most 5 redirects, only to GitHub hosts. Deletes partial files on failure. */
    suspend fun download(url: String, dest: File, maxBytes: Long, onProgress: (Long, Long) -> Unit): Long =
        withContext(Dispatchers.IO) {
            var current = url
            for (hop in 0..MAX_REDIRECTS) {
                if (!GitHubHosts.isAllowedUrl(current)) throw GitHubException.Blocked()
                val conn = URL(current).openConnection() as HttpURLConnection
                try {
                    conn.connectTimeout = TIMEOUT_MS
                    conn.readTimeout = TIMEOUT_MS
                    conn.instanceFollowRedirects = false
                    conn.setRequestProperty("User-Agent", USER_AGENT)
                    val code = conn.responseCode
                    if (code in REDIRECTS) {
                        val loc = conn.getHeaderField("Location") ?: throw GitHubException.BadData()
                        current = URI(current).resolve(loc).toString()
                        continue
                    }
                    check(code)
                    val total = conn.contentLengthLong
                    if (total > maxBytes) throw GitHubException.TooLarge()
                    var written = 0L
                    try {
                        conn.inputStream.use { input ->
                            dest.outputStream().use { out ->
                                val buf = ByteArray(16 * 1024)
                                var lastReport = 0L
                                while (true) {
                                    val n = input.read(buf)
                                    if (n < 0) break
                                    written += n
                                    if (written > maxBytes) throw GitHubException.TooLarge()
                                    out.write(buf, 0, n)
                                    if (written - lastReport >= 256 * 1024) { onProgress(written, total); lastReport = written }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        dest.delete()
                        throw e
                    }
                    onProgress(written, total)
                    return@withContext written
                } catch (e: SocketTimeoutException) {
                    dest.delete(); throw GitHubException.Timeout()
                } catch (e: IOException) {
                    dest.delete(); throw GitHubException.Offline()
                } finally {
                    conn.disconnect()
                }
            }
            throw GitHubException.BadData()   // too many redirects
        }

    private fun getText(url: String): String {
        if (!GitHubHosts.isAllowedUrl(url)) throw GitHubException.Blocked()
        val conn = URL(url).openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = TIMEOUT_MS
            conn.readTimeout = TIMEOUT_MS
            conn.instanceFollowRedirects = false
            conn.setRequestProperty("Accept", "application/vnd.github+json")
            conn.setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
            conn.setRequestProperty("User-Agent", USER_AGENT)
            check(conn.responseCode, conn.getHeaderField("x-ratelimit-remaining"))
            if (conn.contentLengthLong > MAX_JSON) throw GitHubException.BadData()
            val bytes = conn.inputStream.use { it.readBytes() }
            if (bytes.size > MAX_JSON) throw GitHubException.BadData()
            return String(bytes, Charsets.UTF_8)
        } catch (e: SocketTimeoutException) {
            throw GitHubException.Timeout()
        } catch (e: IOException) {
            throw GitHubException.Offline()
        } finally {
            conn.disconnect()
        }
    }

    private fun check(code: Int, remaining: String? = null) {
        when {
            code in 200..299 -> Unit
            code == 404 -> throw GitHubException.NotFound()
            (code == 403 || code == 429) && remaining == "0" -> throw GitHubException.RateLimited()
            code == 429 -> throw GitHubException.RateLimited()
            else -> throw GitHubException.Http(code)
        }
    }

    private companion object {
        const val TIMEOUT_MS = 15_000
        const val MAX_JSON = 3_000_000
        const val MAX_REDIRECTS = 5
        const val USER_AGENT = "Zunivo-Android"
        val REDIRECTS = setOf(301, 302, 303, 307, 308)
    }
}
