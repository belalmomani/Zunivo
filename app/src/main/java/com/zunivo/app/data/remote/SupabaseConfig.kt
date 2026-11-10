package com.zunivo.app.data.remote

import com.zunivo.app.BuildConfig
import java.net.URI

/** Project URL + PUBLIC anon key compiled in from -PSUPABASE_URL / -PSUPABASE_ANON_KEY (or env). Empty = feature off. */
object SupabaseConfig {
    val url: String get() = BuildConfig.SUPABASE_URL.trimEnd('/')
    val anonKey: String get() = BuildConfig.SUPABASE_ANON_KEY
    val host: String? get() = try { URI(url).host } catch (e: Exception) { null }
    val enabled: Boolean get() = url.startsWith("https://") && host != null && anonKey.isNotEmpty()
}
