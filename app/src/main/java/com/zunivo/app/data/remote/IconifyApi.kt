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
        val url = URL("$baseUrl/search?query=$encoded&limit=$limit")
        val conn = url.openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = TIMEOUT_MS
            conn.readTimeout = TIMEOUT_MS
            conn.requestMethod = "GET"
            conn.setRequestProperty("Accept", "application/json")
            val code = conn.responseCode
            if (code !in 200..299) throw ApiException.Http(code)
            if (conn.contentLengthLong > MAX_BYTES) throw ApiException.BadData()
            val text = conn.inputStream.use { it.readBytes() }
            if (text.size > MAX_BYTES) throw ApiException.BadData()
            IconifyParser.parseSearch(String(text, Charsets.UTF_8))
        } catch (e: SocketTimeoutException) {
            throw ApiException.Timeout()
        } catch (e: JSONException) {
            throw ApiException.BadData()
        } catch (e: IOException) {
            throw ApiException.Offline()
        } finally {
            conn.disconnect()
        }
    }

    private companion object {
        const val TIMEOUT_MS = 10_000
        const val MAX_BYTES = 2_000_000
    }
}
