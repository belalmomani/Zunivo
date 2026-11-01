package com.zunivo.app.domain

import java.util.Locale

enum class ExcludeReason { LICENSE_UNKNOWN, LICENSE_NOT_RECOGNIZED, NON_COMMERCIAL, NO_DERIVATIVES, MODIFICATION_NOT_ALLOWED }

data class PackPlan(
    val included: List<CollectionEntry>,
    val excluded: List<Pair<CollectionEntry, ExcludeReason>>,
)

/** Decides which entries may go into an exported archive. Unknown or restrictive licences are never exported. */
object PackPlanner {
    fun plan(c: IconCollection): PackPlan {
        val included = ArrayList<CollectionEntry>()
        val excluded = ArrayList<Pair<CollectionEntry, ExcludeReason>>()
        for (e in c.entries) {
            val a = LicensePolicy.assess(e.snapshot.licenseSpdx)
            val reason = when {
                a.kind == LicenseKind.UNKNOWN -> ExcludeReason.LICENSE_UNKNOWN
                a.kind == LicenseKind.REVIEW -> ExcludeReason.LICENSE_NOT_RECOGNIZED
                a.kind == LicenseKind.NON_COMMERCIAL -> ExcludeReason.NON_COMMERCIAL
                a.kind == LicenseKind.NO_DERIVATIVES -> ExcludeReason.NO_DERIVATIVES
                c.colorHex != null && a.modificationOk != true -> ExcludeReason.MODIFICATION_NOT_ALLOWED
                !a.autoDistributionOk -> ExcludeReason.LICENSE_NOT_RECOGNIZED
                else -> null
            }
            if (reason == null) included.add(e) else excluded.add(e to reason)
        }
        return PackPlan(included, excluded)
    }
}

/** Text files that travel inside every exported archive. */
object PackDocs {
    private fun licenseLabel(s: IconSnapshot): String =
        listOfNotNull(s.licenseTitle, s.licenseSpdx).distinct().joinToString(" / ").ifBlank { "not provided" }

    fun readme(c: IconCollection, plan: PackPlan, createdAt: String, failed: List<CollectionEntry>): String = buildString {
        appendLine("# ${c.name}")
        appendLine()
        if (c.description.isNotBlank()) { appendLine(c.description); appendLine() }
        appendLine("Created with Zunivo on $createdAt.")
        appendLine("Icons come from Iconify (https://api.iconify.design). Each icon keeps the license of its original set.")
        appendLine()
        appendLine("## Included icons (${plan.included.size})")
        plan.included.forEach { e ->
            val s = e.snapshot
            appendLine("- svg/${e.fileName}.svg - ${s.icon.id} - set: ${s.setName ?: s.icon.prefix} - author: ${s.authorName ?: "not provided"} - license: ${licenseLabel(s)}")
        }
        if (plan.excluded.isNotEmpty()) {
            appendLine()
            appendLine("## Not included (license does not allow automatic redistribution)")
            plan.excluded.forEach { (e, r) -> appendLine("- ${e.snapshot.icon.id}: ${r.name.lowercase(Locale.ROOT).replace('_', ' ')}") }
        }
        if (failed.isNotEmpty()) {
            appendLine()
            appendLine("## Could not be downloaded")
            failed.forEach { appendLine("- ${it.snapshot.icon.id}") }
        }
        appendLine()
        appendLine("## Using these icons")
        appendLine("- Keep the license notice and give credit to the authors listed in LICENSES.txt where the license requires it.")
        appendLine("- Some files may be recolored by the pack owner; the original authors did not make those changes.")
        appendLine("- This file is information, not legal advice. Check each license before commercial use or redistribution.")
        appendLine()
        appendLine("## ملاحظة")
        appendLine("كل أيقونة تحتفظ بترخيص مجموعتها الأصلية. راجع LICENSES.txt واحتفظ بالنسب إلى المؤلفين حيث يلزم.")
    }

    fun licenses(plan: PackPlan): String = buildString {
        val bySet = plan.included.groupBy { it.snapshot.icon.prefix }
        bySet.forEach { (prefix, entries) ->
            val s = entries.first().snapshot
            appendLine("Set: ${s.setName ?: prefix} ($prefix)")
            appendLine("Author: ${s.authorName ?: "not provided"}")
            appendLine("License: ${licenseLabel(s)}")
            s.licenseUrl?.let { appendLine("License URL: $it") }
            appendLine("Icons: " + entries.joinToString(", ") { it.snapshot.icon.name })
            appendLine()
        }
        if (bySet.isEmpty()) appendLine("No icons were included.")
    }

    fun colorsJson(c: IconCollection): String {
        val color = c.colorHex?.let { "\"$it\"" } ?: "null"
        return "{\n  \"packColor\": $color,\n  \"note\": \"null means each icon keeps its own colors\"\n}\n"
    }
}
