package com.zunivo.app.feature.designers

import com.zunivo.app.data.remote.SupabaseParser
import com.zunivo.app.domain.CollectionEntry
import com.zunivo.app.domain.CollectionKind
import com.zunivo.app.domain.DesignerRules
import com.zunivo.app.domain.IconCollection
import com.zunivo.app.domain.IconRef
import com.zunivo.app.domain.IconSnapshot
import com.zunivo.app.domain.PublishPlanner
import com.zunivo.app.domain.ValidationError
import com.zunivo.app.domain.Visibility
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DesignersLogicTest {
    @Test fun parsesAuthSession() {
        val s = SupabaseParser.parseSession("""{"access_token":"a","refresh_token":"r","expires_in":3600,"user":{"id":"u1"}}""", 1_000L)!!
        assertEquals("u1", s.userId)
        assertEquals(1_000L + 3_600_000L, s.expiresAtMs)
        assertFalse(s.expiresSoon(1_000L))
        assertTrue(s.expiresSoon(1_000L + 3_600_000L - 30_000L))
        assertNull(SupabaseParser.parseSession("""{"access_token":"a"}""", 0))
    }

    @Test fun parsesDesignersWorksAndCounts() {
        val ds = SupabaseParser.parseDesigners("""[{"id":"d1","display_name":"Sara","bio":null,"specialties":["logos"," ","icons"]},{"id":"d2"}]""")
        assertEquals(1, ds.size)
        assertEquals(listOf("logos", "icons"), ds[0].specialties)
        val ws = SupabaseParser.parseWorks("""[{"id":"w1","designer_id":"d1","title":"Pack","icon_ids":["mdi:home","bad id","mdi:home","lucide:sun"],"visibility":"public"},
            {"id":"w2","designer_id":"d1","title":"Hidden","icon_ids":[],"visibility":"anything-else"}]""")
        assertEquals(listOf("mdi:home", "lucide:sun"), ws[0].iconIds)   // malformed and duplicate ids dropped
        assertEquals(Visibility.PUBLIC, ws[0].visibility)
        assertEquals(Visibility.PRIVATE, ws[1].visibility)                // unknown value never becomes public
        assertEquals(mapOf("w1" to 3), SupabaseParser.parseCounts("""[{"work_id":"w1","downloads":3},{"work_id":null}]"""))
    }

    @Test fun profileAndWorkValidation() {
        assertEquals(ValidationError.NAME, DesignerRules.validateProfile("A", "", emptyList()))
        assertEquals(ValidationError.BIO, DesignerRules.validateProfile("Sara", "x".repeat(501), emptyList()))
        assertNull(DesignerRules.validateProfile("Sara", "hi", listOf("logos")))
        assertEquals(listOf("logos", "icons"), DesignerRules.cleanSpecialties("logos، icons,\nlogos , "))
        assertEquals(ValidationError.TITLE, DesignerRules.validateWork("x", "", listOf("mdi:home")))
        assertEquals(ValidationError.NO_ICONS, DesignerRules.validateWork("My pack", "", emptyList()))
        assertEquals(ValidationError.TOO_MANY_ICONS, DesignerRules.validateWork("My pack", "", List(201) { "mdi:i$it" }))
        assertNull(DesignerRules.validateWork("My pack", "", listOf("mdi:home")))
    }

    @Test fun onlyRedistributableIconsArePublished() {
        fun e(name: String, spdx: String?) = CollectionEntry(IconSnapshot(IconRef("s", name), "Set", "A", null, spdx, null), name)
        val pack = IconCollection("p", CollectionKind.PACK, "P", "", listOf(e("ok", "MIT"), e("nc", "CC-BY-NC-4.0"), e("unk", null)), "#FFFFFF", 0, 0)
        val plan = PublishPlanner.plan(pack)
        assertEquals(listOf("s:ok"), plan.iconIds)
        assertEquals(2, plan.excluded)
    }
}
