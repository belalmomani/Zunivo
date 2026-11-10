package com.zunivo.app.data.remote

import com.zunivo.app.domain.IconifyEndpoints
import com.zunivo.app.domain.SearchPage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONException
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.URLEncoder

sealed class ApiException(message: String) : Exception(message) {
    class Offline : ApiException("offline")
    class Timeout : ApiException("timeout")
    class Http(val code: Int) : ApiException("http $code")
    class BadData : ApiException("bad data")
}

/** Minimal HTTPS client for the public Iconify API (documented at iconify/website docs/api). */
class IconifyApi(private val baseUrl: String = IconifyEndpoints.BASE_URL) {

    suspend fun search(query: String, limit: Int): SearchPage = withContext(Dispatchers.IO) {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val text = get("$baseUrl/search?query=$encoded&limit=$limit", "application/json", MAX_JSON_BYTES)
        try {
            IconifyParser.parseSearch(text)
        } catch (e: JSONException) {
            throw ApiException.BadData()
        }
    }

    /** Author/license info for the given set prefixes (each prefix must be a safe Iconify identifier). */
    suspend fun collections(prefixes: List<String>): Map<String, com.zunivo.app.domain.IconSetInfo> = withContext(Dispatchers.IO) {
        val safe = prefixes.distinct().filter { it.matches(Regex("[A-Za-z0-9][A-Za-z0-9_-]*")) }.take(40)
        if (safe.isEmpty()) return@withContext emptyMap()
        val text = get("$baseUrl/collections?prefixes=" + URLEncoder.encode(safe.joinToString(","), "UTF-8"), "application/json", MAX_JSON_BYTES)
        try {
            IconifyParser.parseCollections(text)
        } catch (e: JSONException) {
            throw ApiException.BadData()
        }
    }

    /** Downloads one icon as SVG text. Only URLs under the Iconify base URL are accepted. */
    suspend fun fetchSvg(url: String): String = withContext(Dispatchers.IO) {
        if (!url.startsWith("$baseUrl/")) throw ApiException.BadData()
        get(url, "image/svg+xml", MAX_SVG_BYTES)
    }

    private fun get(url: String, accept: String, maxBytes: Int): String {
        val conn = URL(url).openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = TIMEOUT_MS
            conn.readTimeout = TIMEOUT_MS
            conn.instanceFollowRedirects = false   // never follow a redirect to another host
            conn.requestMethod = "GET"
            conn.setRequestProperty("Accept", accept)
            val code = conn.responseCode
            if (code !in 200..299) throw ApiException.Http(code)
            if (conn.contentLengthLong > maxBytes) throw ApiException.BadData()
            val bytes = conn.inputStream.use { it.readBytes() }
            if (bytes.size > maxBytes) throw ApiException.BadData()
            return String(bytes, Charsets.UTF_8)
        } catch (e: SocketTimeoutException) {
            throw ApiException.Timeout()
        } catch (e: IOException) {
            throw ApiException.Offline()
        } finally {
            conn.disconnect()
        }
    }

    private companion object {
        const val TIMEOUT_MS = 10_000
        const val MAX_JSON_BYTES = 2_000_000
        const val MAX_SVG_BYTES = 500_000
    }
}
