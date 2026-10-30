package com.zunivo.app.data

import com.zunivo.app.data.remote.IconifyParser
import org.json.JSONException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Fixture follows the documented /search response shape; it is hand-written, not a live response. */
class IconifyParserTest {
    private val sample = """
    {
      "icons": ["mdi:home", "mdi-light:home", "bad id", "x:y/z"],
      "total": 2, "limit": 64, "start": 0,
      "collections": {
        "mdi": {
          "name": "Material Design Icons",
          "author": {"name": "Pictogrammers", "url": "https://github.com/Templarian/MaterialDesign"},
          "license": {"title": "Apache 2.0", "spdx": "Apache-2.0", "url": "https://www.apache.org/licenses/LICENSE-2.0"},
          "palette": false
        },
        "mdi-light": {"name": "Material Design Light"}
      },
      "request": {"query": "home"}
    }
    """.trimIndent()

    @Test fun parsesIconsAndSkipsInvalidIds() {
        val page = IconifyParser.parseSearch(sample)
        assertEquals(listOf("mdi:home", "mdi-light:home"), page.icons.map { it.id })
        assertEquals(64, page.limit)
    }

    @Test fun parsesSetInfoAndKeepsMissingFieldsNull() {
        val page = IconifyParser.parseSearch(sample)
        val mdi = page.sets.getValue("mdi")
        assertEquals("Pictogrammers", mdi.authorName)
        assertEquals("Apache-2.0", mdi.licenseSpdx)
        assertEquals(false, mdi.palette)
        val light = page.sets.getValue("mdi-light")
        assertNull(light.authorName)
        assertNull(light.licenseSpdx)
        assertNull(light.palette)
    }

    @Test fun emptyResultIsValid() {
        val page = IconifyParser.parseSearch("""{"icons":[],"total":0,"limit":64,"start":0,"collections":{}}""")
        assertTrue(page.icons.isEmpty())
    }

    @Test(expected = JSONException::class) fun malformedJsonThrows() {
        IconifyParser.parseSearch("not json")
    }
}
