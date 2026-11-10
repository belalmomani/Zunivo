package com.zunivo.app.data.remote

import com.zunivo.app.domain.DesignerProfile
import com.zunivo.app.domain.DesignerWork
import com.zunivo.app.domain.IconRef
import com.zunivo.app.domain.Session
import com.zunivo.app.domain.Visibility
import org.json.JSONArray
import org.json.JSONObject

/** Pure parsing of Supabase Auth / PostgREST JSON. Throws org.json.JSONException on malformed input. */
object SupabaseParser {
    private fun JSONObject.str(key: String): String? = if (isNull(key)) null else optString(key, "").trim().ifEmpty { null }

    private fun strings(a: JSONArray?): List<String> {
        val out = ArrayList<String>()
        if (a != null) for (i in 0 until a.length()) a.optString(i).trim().takeIf { it.isNotEmpty() }?.let { out.add(it) }
        return out
    }

    fun parseSession(json: String, nowMs: Long): Session? {
        val o = JSONObject(json)
        val access = o.str("access_token") ?: return null
        val refresh = o.str("refresh_token") ?: return null
        val uid = o.optJSONObject("user")?.str("id") ?: return null
        return Session(access, refresh, uid, nowMs + o.optLong("expires_in", 3600L) * 1000)
    }

    private fun profile(o: JSONObject): DesignerProfile? {
        val id = o.str("id") ?: return null
        val name = o.str("display_name") ?: return null
        return DesignerProfile(id, name, o.str("bio"), strings(o.optJSONArray("specialties")))
    }

    fun parseDesigners(json: String): List<DesignerProfile> {
        val a = JSONArray(json)
        return (0 until a.length()).mapNotNull { a.optJSONObject(it)?.let(::profile) }
    }

    private fun work(o: JSONObject): DesignerWork? {
        val id = o.str("id") ?: return null
        val designer = o.str("designer_id") ?: return null
        val title = o.str("title") ?: return null
        // only well-formed Iconify ids survive; they are used to build URLs later
        val ids = strings(o.optJSONArray("icon_ids")).mapNotNull { IconRef.parse(it)?.id }.distinct()
        val vis = if (o.str("visibility") == "public") Visibility.PUBLIC else Visibility.PRIVATE
        return DesignerWork(id, designer, title, o.str("description"), ids, vis, o.str("updated_at"))
    }

    fun parseWorks(json: String): List<DesignerWork> {
        val a = JSONArray(json)
        return (0 until a.length()).mapNotNull { a.optJSONObject(it)?.let(::work) }
    }

    /** `work_download_counts` rows: real counts only; missing work = 0. */
    fun parseCounts(json: String): Map<String, Int> {
        val a = JSONArray(json)
        val out = HashMap<String, Int>()
        for (i in 0 until a.length()) {
            val o = a.optJSONObject(i) ?: continue
            val id = o.str("work_id") ?: continue
            out[id] = o.optInt("downloads", 0).coerceAtLeast(0)
        }
        return out
    }
}
