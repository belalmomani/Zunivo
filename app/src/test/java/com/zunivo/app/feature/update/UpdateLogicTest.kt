package com.zunivo.app.feature.update

import com.zunivo.app.data.remote.UpdateParser
import com.zunivo.app.domain.ChecksumFile
import com.zunivo.app.domain.VersionCompare
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateLogicTest {
    @Test fun versionComparison() {
        assertTrue(VersionCompare.isNewer("v0.2.0", "0.1.0"))
        assertTrue(VersionCompare.isNewer("1.10.0", "1.9.9"))        // numeric, not text, comparison
        assertTrue(VersionCompare.isNewer("2.0", "1.9.9"))
        assertFalse(VersionCompare.isNewer("0.1.0", "0.1.0"))
        assertFalse(VersionCompare.isNewer("v0.1.0", "0.1.0.0"))
        assertFalse(VersionCompare.isNewer("0.0.9", "0.1.0"))
        assertTrue(VersionCompare.isNewer("1.0.0", "1.0.0-beta"))    // final beats pre-release
        assertFalse(VersionCompare.isNewer("1.0.0-beta", "1.0.0"))
        assertEquals("1.2.3", VersionCompare.clean("v1.2.3+45"))
    }

    @Test fun checksumFileParsing() {
        val h1 = "a".repeat(64); val h2 = "B".repeat(64)
        assertEquals(h1, ChecksumFile.parse("$h1  Zunivo.apk\n", "Zunivo.apk"))
        assertEquals(h1, ChecksumFile.parse(h1, "Zunivo.apk"))
        assertEquals(h2.lowercase(), ChecksumFile.parse("$h1  other.apk\n$h2  Zunivo.apk", "Zunivo.apk"))
        assertNull(ChecksumFile.parse("not a hash", "Zunivo.apk"))
    }

    private val sha = "c".repeat(64)
    private fun release(extra: String = "", prerelease: Boolean = false) = """{
      "tag_name":"v0.2.0","body":"- new things","html_url":"https://github.com/belalmomani/Zunivo/releases/tag/v0.2.0",
      "draft":false,"prerelease":$prerelease,
      "assets":[
        {"name":"Zunivo-v0.2.0.apk","size":1000,"browser_download_url":"https://github.com/belalmomani/Zunivo/releases/download/v0.2.0/Zunivo-v0.2.0.apk","digest":"sha256:$sha"},
        {"name":"Zunivo-v0.2.0.apk.sha256","size":90,"browser_download_url":"https://github.com/belalmomani/Zunivo/releases/download/v0.2.0/Zunivo-v0.2.0.apk.sha256"}
        $extra
      ]}"""

    @Test fun parsesLatestRelease() {
        val u = UpdateParser.parseLatest(release())!!
        assertEquals("0.2.0", u.version)
        assertEquals("Zunivo-v0.2.0.apk", u.apkName)
        assertEquals(sha, u.sha256)
        assertTrue(u.checksumUrl!!.endsWith(".sha256"))
        assertEquals("- new things", u.notes)
    }

    @Test fun releasesWithoutUsableApkOrPrereleasesAreIgnored() {
        assertNull(UpdateParser.parseLatest("""{"tag_name":"v1","draft":false,"prerelease":false,"assets":[{"name":"src.zip","size":5,"browser_download_url":"https://x"}]}"""))
        assertNull(UpdateParser.parseLatest("""{"tag_name":"v1","draft":false,"prerelease":false,"assets":[{"name":"a.apk","size":0,"browser_download_url":"https://x"}]}"""))
        assertNull(UpdateParser.parseLatest(release(prerelease = true)))
        assertNull(UpdateParser.parseLatest("""{"tag_name":"","assets":[]}"""))
    }
}
