package com.zunivo.app.data.remote

import com.zunivo.app.domain.DesignerProfile
import com.zunivo.app.domain.DesignerWork
import com.zunivo.app.domain.Session
import com.zunivo.app.domain.Visibility
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.URLEncoder

sealed class SupabaseException(message: String) : Exception(message) {
    class NotConfigured : SupabaseException("not configured")
    class Offline : SupabaseException("offline")
    class Timeout : SupabaseException("timeout")
    class Unauthorized : SupabaseException("unauthorized")
    class RateLimited : SupabaseException("rate limited")
    class Rejected : SupabaseException("rejected")      // 400/409/422: bad code, duplicate, constraint
    class Http(val code: Int) : SupabaseException("http $code")
    class BadData : SupabaseException("bad data")
}

/**
 * Supabase Auth (email one-time code) + PostgREST, using only the public anon key and the user's own access token.
 * Row Level Security on the server decides what each user may read or write (docs/supabase/schema.sql).
 * HttpURLConnection has no PATCH on Android, so updates use an upsert (POST + merge-duplicates).
 */
class SupabaseApi {
    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")

    // ---------------- auth ----------------
    suspend fun sendCode(email: String) {
        call("POST", "/auth/v1/otp", JSONObject().put("email", email).put("create_user", true).toString())
    }

    suspend fun verifyCode(email: String, code: String): Session {
        val text = call("POST", "/auth/v1/verify", JSONObject().put("type", "email").put("email", email).put("token", code).toString())
        return parse { SupabaseParser.parseSession(text, System.currentTimeMillis()) } ?: throw SupabaseException.BadData()
    }

    suspend fun refresh(refreshToken: String): Session {
        val text = call("POST", "/auth/v1/token?grant_type=refresh_token", JSONObject().put("refresh_token", refreshToken).toString())
        return parse { SupabaseParser.parseSession(text, System.currentTimeMillis()) } ?: throw SupabaseException.BadData()
    }

    suspend fun signOut(token: String) {
        try { call("POST", "/auth/v1/logout", "{}", token) } catch (e: SupabaseException) { /* local sign-out happens anyway */ }
    }

    // ---------------- public reads ----------------
    suspend fun designers(): List<DesignerProfile> = parse {
        SupabaseParser.parseDesigners(call("GET", "/rest/v1/designers?select=id,display_name,bio,specialties&order=created_at.desc&limit=50"))
    }

    suspend fun publicWorks(): List<DesignerWork> = parse {
        SupabaseParser.parseWorks(call("GET", "/rest/v1/designer_works?select=$WORK_COLS&visibility=eq.public&order=updated_at.desc&limit=50"))
    }

    suspend fun downloadCounts(): Map<String, Int> = parse {
        SupabaseParser.parseCounts(call("GET", "/rest/v1/work_download_counts?select=work_id,downloads&limit=500"))
    }

    // ---------------- signed-in user ----------------
    suspend fun myProfile(uid: String, token: String): DesignerProfile? = parse {
        SupabaseParser.parseDesigners(call("GET", "/rest/v1/designers?id=eq.${enc(uid)}&select=id,display_name,bio,specialties", token = token)).firstOrNull()
    }

    suspend fun saveProfile(p: DesignerProfile, token: String) {
        val body = JSONObject().put("id", p.id).put("display_name", p.displayName)
            .put("bio", p.bio ?: JSONObject.NULL).put("specialties", JSONArray(p.specialties)).toString()
        call("POST", "/rest/v1/designers", body, token, prefer = "resolution=merge-duplicates,return=minimal")
    }

    suspend fun deleteProfile(uid: String, token: String) {
        call("DELETE", "/rest/v1/designers?id=eq.${enc(uid)}", token = token)   // works and download events cascade
    }

    suspend fun myWorks(uid: String, token: String): List<DesignerWork> = parse {
        SupabaseParser.parseWorks(call("GET", "/rest/v1/designer_works?designer_id=eq.${enc(uid)}&select=$WORK_COLS&order=updated_at.desc", token = token))
    }

    suspend fun saveWork(w: DesignerWork, token: String) {
        val body = JSONObject().put("id", w.id).put("designer_id", w.designerId).put("title", w.title)
            .put("description", w.description ?: JSONObject.NULL).put("icon_ids", JSONArray(w.iconIds))
            .put("visibility", if (w.visibility == Visibility.PUBLIC) "public" else "private")
            .put("updated_at", java.time.Instant.now().toString()).toString()
        call("POST", "/rest/v1/designer_works", body, token, prefer = "resolution=merge-duplicates,return=minimal")
    }

    suspend fun deleteWork(id: String, token: String) {
        call("DELETE", "/rest/v1/designer_works?id=eq.${enc(id)}", token = token)
    }

    suspend fun recordDownload(workId: String, uid: String, token: String) {
        val body = JSONObject().put("work_id", workId).put("user_id", uid).toString()
        call("POST", "/rest/v1/download_events", body, token, prefer = "resolution=ignore-duplicates,return=minimal")
    }

    suspend fun report(workId: String, uid: String, reason: String, token: String) {
        val body = JSONObject().put("work_id", workId).put("reporter_id", uid).put("reason", reason).toString()
        call("POST", "/rest/v1/copyright_reports", body, token, prefer = "return=minimal")
    }

    // ---------------- plumbing ----------------
    private inline fun <T> parse(block: () -> T): T = try { block() } catch (e: JSONException) { throw SupabaseException.BadData() }

    private suspend fun call(method: String, path: String, body: String? = null, token: String? = null, prefer: String? = null): String =
        withContext(Dispatchers.IO) {
            if (!SupabaseConfig.enabled) throw SupabaseException.NotConfigured()
            val url = URL(SupabaseConfig.url + path)
            if (url.protocol != "https" || url.host != SupabaseConfig.host) throw SupabaseException.BadData()
            val conn = url.openConnection() as HttpURLConnection
            try {
                conn.connectTimeout = TIMEOUT_MS
                conn.readTimeout = TIMEOUT_MS
                conn.instanceFollowRedirects = false
                conn.requestMethod = method
                conn.setRequestProperty("apikey", SupabaseConfig.anonKey)
                conn.setRequestProperty("Authorization", "Bearer " + (token ?: SupabaseConfig.anonKey))
                conn.setRequestProperty("Accept", "application/json")
                if (prefer != null) conn.setRequestProperty("Prefer", prefer)
                if (body != null) {
                    conn.doOutput = true
                    conn.setRequestProperty("Content-Type", "application/json")
                    conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
                }
                val code = conn.responseCode
                when {
                    code in 200..299 -> Unit
                    code == 401 || code == 403 -> throw SupabaseException.Unauthorized()
                    code == 429 -> throw SupabaseException.RateLimited()
                    code in 400..422 -> throw SupabaseException.Rejected()
                    else -> throw SupabaseException.Http(code)
                }
                if (code == 204 || conn.contentLengthLong == 0L) return@withContext ""
                if (conn.contentLengthLong > MAX_BYTES) throw SupabaseException.BadData()
                val bytes = conn.inputStream.use { it.readBytes() }
                if (bytes.size > MAX_BYTES) throw SupabaseException.BadData()
                String(bytes, Charsets.UTF_8)
            } catch (e: SocketTimeoutException) {
                throw SupabaseException.Timeout()
            } catch (e: IOException) {
                throw SupabaseException.Offline()
            } finally {
                conn.disconnect()
            }
        }

    private companion object {
        const val TIMEOUT_MS = 15_000
        const val MAX_BYTES = 2_000_000
        const val WORK_COLS = "id,designer_id,title,description,icon_ids,visibility,updated_at"
    }
}
