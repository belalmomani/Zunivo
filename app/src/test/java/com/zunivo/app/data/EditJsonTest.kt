package com.zunivo.app.data

import com.zunivo.app.data.local.EditJson
import com.zunivo.app.domain.EditParams
import com.zunivo.app.domain.IconEdit
import com.zunivo.app.domain.IconRef
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EditJsonTest {
    private val edit = IconEdit(
        id = "e1", icon = IconRef("mdi", "home"), setName = "Material Design Icons", authorName = "Pictogrammers",
        licenseTitle = "Apache 2.0", licenseSpdx = "Apache-2.0", licenseUrl = "https://www.apache.org/licenses/LICENSE-2.0",
        params = EditParams(colorHex = "#6366F1", bgHex = null, sizePx = 512, paddingPct = 12, rotation = 180, flipH = true),
        createdAt = 10L, updatedAt = 20L,
    )

    @Test fun roundTripKeepsEverythingIncludingLicense() {
        val back = EditJson.fromJson(EditJson.toJson(listOf(edit)))
        assertEquals(listOf(edit), back)
    }

    @Test fun missingOptionalFieldsStayNull() {
        val e = edit.copy(setName = null, authorName = null, licenseTitle = null, licenseSpdx = null, licenseUrl = null)
        val back = EditJson.fromJson(EditJson.toJson(listOf(e))).single()
        assertNull(back.authorName)
        assertNull(back.licenseSpdx)
    }

    @Test fun invalidEntriesAreSkipped() {
        val json = """{"version":1,"edits":[{"id":"x","icon":"bad id"},{"id":"","icon":"mdi:home"},{"id":"ok","icon":"mdi:home"}]}"""
        assertEquals(listOf("ok"), EditJson.fromJson(json).map { it.id })
    }
}
