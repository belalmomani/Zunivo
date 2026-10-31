package com.zunivo.app.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SvgToolsTest {
    private val icon = """<svg xmlns="http://www.w3.org/2000/svg" width="1em" height="1em" viewBox="0 0 24 24"><path fill="#fff" d="M12 2L2 22h20z"/></svg>"""

    @Test fun plainIconIsSafe() = assertTrue(SvgSafety.isSafe(icon))

    @Test fun activeOrExternalContentIsRejected() {
        assertFalse(SvgSafety.isSafe("""<svg viewBox="0 0 1 1"><script>alert(1)</script></svg>"""))
        assertFalse(SvgSafety.isSafe("""<svg viewBox="0 0 1 1" onload="x()"></svg>"""))
        assertFalse(SvgSafety.isSafe("""<svg viewBox="0 0 1 1"><image href="https://evil.example/a.png"/></svg>"""))
        assertFalse(SvgSafety.isSafe("""<svg viewBox="0 0 1 1"><foreignObject/></svg>"""))
        assertFalse(SvgSafety.isSafe("not svg"))
        assertFalse(SvgSafety.isSafe("<svg>"))
    }

    @Test fun composeWrapsWithPaddingBackgroundAndMeta() {
        val out = SvgComposer.compose(icon, 200, 10, "#112233", "Source: X -- by Y")!!
        assertTrue(out.contains("width=\"200\" height=\"200\" viewBox=\"0 0 200 200\""))
        assertTrue(out.contains("<rect width=\"200\" height=\"200\" fill=\"#112233\"/>"))
        assertTrue(out.contains("<svg x=\"20\" y=\"20\" width=\"160\" height=\"160\" viewBox=\"0 0 24 24\">"))
        assertTrue(out.contains("<path fill=\"#fff\""))
        assertTrue(out.contains("Source: X - - by Y"))   // "--" would end the XML comment early
        assertTrue(SvgSafety.isSafe(out))
    }

    @Test fun composeWithoutBackgroundHasNoRect() {
        assertFalse(SvgComposer.compose(icon, 100, 0, null, "m")!!.contains("<rect"))
        assertFalse(SvgComposer.compose(icon, 100, 0, "javascript", "m")!!.contains("<rect"))
    }

    @Test fun composeRejectsBrokenInput() {
        assertNull(SvgComposer.compose("<svg>no viewbox</svg>", 100, 10, null, "m"))
        assertNull(SvgComposer.compose("garbage", 100, 10, null, "m"))
    }
}
