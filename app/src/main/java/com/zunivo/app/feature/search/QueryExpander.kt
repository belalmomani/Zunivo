package com.zunivo.app.feature.search

/**
 * Iconify search is English-only. This turns what the user typed into up to 3 English queries:
 * a small built-in Arabic dictionary plus a few English synonyms. It is intentionally limited
 * (not machine translation); words it does not know are reported as untranslated.
 */
object QueryExpander {
    private val diacritics = Regex("[\\u064B-\\u0652\\u0640]")

    fun normalizeArabic(token: String): String =
        token.replace(diacritics, "")
            .replace('أ', 'ا').replace('إ', 'ا').replace('آ', 'ا')
            .replace('ى', 'ي').replace('ة', 'ه')

    private val arabic: Map<String, String> = mapOf(
        "بيت" to "home", "منزل" to "home", "بيوت" to "home", "بحث" to "search", "مستخدم" to "user",
        "شخص" to "person", "ناس" to "people", "بريد" to "mail", "رسالة" to "message", "هاتف" to "phone",
        "جوال" to "phone", "سيارة" to "car", "حافلة" to "bus", "قطار" to "train", "طائرة" to "plane",
        "سفر" to "travel", "مطعم" to "restaurant", "طعام" to "food", "قهوة" to "coffee", "متجر" to "shop",
        "سلة" to "cart", "تسوق" to "shopping", "مال" to "money", "محفظة" to "wallet", "بنك" to "bank",
        "صحة" to "health", "قلب" to "heart", "طبيب" to "doctor", "مستشفى" to "hospital", "كتاب" to "book",
        "مدرسة" to "school", "تعليم" to "education", "لعبة" to "game", "شجرة" to "tree", "ورقة" to "leaf",
        "زهرة" to "flower", "شمس" to "sun", "قمر" to "moon", "نجمة" to "star", "اعدادات" to "settings",
        "كاميرا" to "camera", "صورة" to "image", "ملف" to "file", "مجلد" to "folder", "حذف" to "delete",
        "تعديل" to "edit", "اضافة" to "add", "تحميل" to "download", "مشاركة" to "share", "اشعار" to "bell",
        "ساعة" to "clock", "تقويم" to "calendar", "خريطة" to "map", "موقع" to "location", "قفل" to "lock",
        "مفتاح" to "key", "سهم" to "arrow", "حاسوب" to "computer", "سحابة" to "cloud", "كود" to "code",
    ).mapKeys { normalizeArabic(it.key) }

    private val english: Map<String, List<String>> = mapOf(
        "home" to listOf("house"), "settings" to listOf("gear"), "delete" to listOf("trash"),
        "search" to listOf("magnify"), "mail" to listOf("email"), "user" to listOf("person"),
        "shop" to listOf("store"), "money" to listOf("cash"), "phone" to listOf("mobile"),
    )

    private fun hasArabic(text: String) = text.any { it in '\u0600'..'\u06FF' }

    private fun translate(token: String): String? {
        if (!hasArabic(token)) return token
        val n = normalizeArabic(token)
        arabic[n]?.let { return it }
        if (n.startsWith("ال") && n.length > 3) return arabic[n.substring(2)]
        return null
    }

    /** Empty list = nothing searchable (too short, or Arabic words with no known translation). */
    fun expand(raw: String): List<String> {
        val q = raw.trim().lowercase()
        if (q.length < 2) return emptyList()
        val tokens = q.split(Regex("\\s+")).filter { it.isNotEmpty() }
        val out = LinkedHashSet<String>()
        if (!hasArabic(q)) {
            out.add(q)
            english[q]?.let { out.addAll(it) }
        } else {
            val translated = tokens.mapNotNull { translate(it) }
            if (translated.isNotEmpty()) {
                out.add(translated.joinToString(" "))
                out.addAll(translated)
            }
        }
        return out.take(3)
    }
}
