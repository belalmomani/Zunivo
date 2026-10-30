package com.zunivo.app.i18n

/** User-selectable app language. SYSTEM follows the device setting. */
enum class AppLanguage(val tag: String?) {
    SYSTEM(null),
    ARABIC("ar"),
    ENGLISH("en");

    companion object {
        fun fromTag(tag: String?): AppLanguage = entries.firstOrNull { it.tag == tag } ?: SYSTEM
    }
}
