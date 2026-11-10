package com.zunivo.app.domain

data class Session(val accessToken: String, val refreshToken: String, val userId: String, val expiresAtMs: Long) {
    fun expiresSoon(now: Long): Boolean = expiresAtMs - now < 60_000
}

data class DesignerProfile(val id: String, val displayName: String, val bio: String?, val specialties: List<String>)

enum class Visibility { PRIVATE, PUBLIC }

data class DesignerWork(
    val id: String,
    val designerId: String,
    val title: String,
    val description: String?,
    val iconIds: List<String>,
    val visibility: Visibility,
    val updatedAt: String?,
)

enum class ValidationError { NAME, BIO, SPECIALTIES, TITLE, DESCRIPTION, NO_ICONS, TOO_MANY_ICONS }

object DesignerRules {
    const val MAX_WORK_ICONS = 200

    fun cleanSpecialties(raw: String): List<String> =
        raw.split(',', '،', '\n').map { it.trim().take(30) }.filter { it.isNotEmpty() }.distinct().take(8)

    fun validateProfile(name: String, bio: String, specialties: List<String>): ValidationError? = when {
        name.trim().length !in 2..60 -> ValidationError.NAME
        bio.trim().length > 500 -> ValidationError.BIO
        specialties.size > 8 || specialties.any { it.length > 30 } -> ValidationError.SPECIALTIES
        else -> null
    }

    fun validateWork(title: String, description: String, iconIds: List<String>): ValidationError? = when {
        title.trim().length !in 2..80 -> ValidationError.TITLE
        description.trim().length > 1000 -> ValidationError.DESCRIPTION
        iconIds.isEmpty() -> ValidationError.NO_ICONS
        iconIds.size > MAX_WORK_ICONS -> ValidationError.TOO_MANY_ICONS
        else -> null
    }
}

/** What may be published from a local pack: only icons whose license allows redistribution. Others stay local. */
data class PublishPlan(val iconIds: List<String>, val excluded: Int)

object PublishPlanner {
    fun plan(c: IconCollection): PublishPlan {
        val p = PackPlanner.plan(c.copy(colorHex = null))
        return PublishPlan(p.included.map { it.snapshot.icon.id }, p.excluded.size)
    }
}
