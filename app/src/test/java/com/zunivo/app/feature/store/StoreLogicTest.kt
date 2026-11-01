package com.zunivo.app.feature.store

import com.zunivo.app.data.local.DraftJson
import com.zunivo.app.data.remote.GitHubParser
import com.zunivo.app.domain.AssetInfo
import com.zunivo.app.domain.ChangeKind
import com.zunivo.app.domain.Decision
import com.zunivo.app.domain.DraftItem
import com.zunivo.app.domain.DraftMerge
import com.zunivo.app.domain.DraftOps
import com.zunivo.app.domain.FetchStatus
import com.zunivo.app.domain.Fetched
import com.zunivo.app.domain.GitHubHosts
import com.zunivo.app.domain.GitHubUrl
import com.zunivo.app.domain.ImportMode
import com.zunivo.app.domain.ItemKind
import com.zunivo.app.domain.ItemNote
import com.zunivo.app.domain.ReadmeExtractor
import com.zunivo.app.domain.ReleaseInfo
import com.zunivo.app.domain.RepoInfo
import com.zunivo.app.domain.RepoRef
import com.zunivo.app.domain.StoreDraft
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StoreLogicTest {
    private val ref = RepoRef("octo", "demo")
    private val info = RepoInfo("demo", "A demo", "octo", "https://github.com/octo/demo", "Kotlin", "MIT License", "MIT",
        "https://api.github.com/licenses/mit", 2048, "main", "https://demo.example", true, false, false)

    // ---- URL / hosts ----
    @Test fun parsesCommonGitHubLinks() {
        assertEquals(ref, GitHubUrl.parse("https://github.com/octo/demo"))
        assertEquals(ref, GitHubUrl.parse("  https://github.com/octo/demo.git "))
        assertEquals(ref, GitHubUrl.parse("https://www.github.com/octo/demo/tree/main/app"))
        assertEquals(ref, GitHubUrl.parse("github.com/octo/demo"))
    }

    @Test fun rejectsEverythingElse() {
        listOf(
            null, "", "octo/demo", "http://github.com/octo/demo", "https://gitlab.com/octo/demo",
            "https://github.com/octo", "https://evil.com/github.com/octo/demo", "https://user:pw@github.com/octo/demo",
            "https://github.com:444/octo/demo", "https://github.com/octo/..", "https://github.com/-bad/demo",
            "https://github.com/octo/de mo",
        ).forEach { assertNull(it.toString(), GitHubUrl.parse(it)) }
    }

    @Test fun onlyGitHubHostsAreAllowed() {
        assertTrue(GitHubHosts.isAllowedUrl("https://api.github.com/repos/a/b"))
        assertTrue(GitHubHosts.isAllowedUrl("https://objects.githubusercontent.com/x"))
        assertTrue(GitHubHosts.isAllowedUrl("https://codeload.github.com/a/b/zip/main"))
        assertFalse(GitHubHosts.isAllowedUrl("http://github.com/a"))
        assertFalse(GitHubHosts.isAllowedUrl("https://github.com.evil.com/a"))
        assertFalse(GitHubHosts.isAllowedUrl("https://evilgithubusercontent.com/a"))
        assertFalse(GitHubHosts.isAllowedUrl("https://example.com/a.apk"))
    }

    // ---- README images ----
    @Test fun extractsRelativeAndAbsoluteImagesAndSkipsBadges() {
        val md = """
            ![logo](docs/logo.png)
            [![build](https://img.shields.io/badge/a-b-green.svg)](https://x.example)
            <img src="./shots/one.JPG" width="100">
            ![ext](https://example.com/pic.png)
            ![vector](assets/art.svg)
            ![dup](docs/logo.png)
            ![att](https://github.com/user-attachments/assets/1234-abcd)
            ![bad](../../etc/passwd.png)
            ![http](http://insecure.example/a.png)
        """.trimIndent()
        val imgs = ReadmeExtractor.images(md, ref, "main")
        assertEquals(
            listOf(
                "https://raw.githubusercontent.com/octo/demo/main/docs/logo.png",
                "https://raw.githubusercontent.com/octo/demo/main/shots/one.JPG",
                "https://example.com/pic.png",
                "https://github.com/user-attachments/assets/1234-abcd",
            ),
            imgs.map { it.url },
        )
        assertEquals(listOf(true, true, false, true), imgs.map { it.allowedHost })
    }

    // ---- parsers (fixtures follow docs.github.com/rest shapes; hand-written) ----
    @Test fun parsesRepoReadmeAndReleases() {
        val repo = GitHubParser.parseRepo("""{"name":"demo","description":null,"owner":{"login":"octo"},"html_url":"https://github.com/octo/demo",
            "language":"Kotlin","license":{"name":"MIT License","spdx_id":"NOASSERTION","url":null},"size":12,"default_branch":"dev","homepage":"","has_wiki":true}""")
        assertNull(repo.description); assertNull(repo.licenseSpdx); assertNull(repo.homepage)
        assertEquals("dev", repo.defaultBranch)

        assertEquals("# Hello", GitHubParser.parseFileText("""{"encoding":"base64","content":"IyBI\nZWxsbw=="}"""))
        assertNull(GitHubParser.parseFileText("""{"encoding":"none","content":""}"""))

        val sha = "a".repeat(64)
        val rels = GitHubParser.parseReleases("""[
          {"tag_name":"v2","name":"Two","body":"notes","draft":false,"prerelease":false,"html_url":"https://github.com/octo/demo/releases/tag/v2",
           "assets":[{"name":"app.apk","size":10,"browser_download_url":"https://github.com/octo/demo/releases/download/v2/app.apk","digest":"sha256:$sha"},
                     {"name":"notes.txt","size":1,"browser_download_url":"https://github.com/x"}]},
          {"tag_name":"v1-draft","draft":true}]""")
        assertEquals(listOf("v2"), rels.map { it.tag })
        assertEquals(sha, rels[0].assets[0].sha256)
        assertNull(rels[0].assets[1].sha256)
    }

    // ---- builder ----
    private fun release(tag: String, vararg assets: AssetInfo) = ReleaseInfo(tag, null, "notes", null, false, null, assets.toList())

    @Test fun builderKeepsMissingThingsMissing() {
        val noLicense = info.copy(licenseName = null, licenseSpdx = null, description = null, homepage = null, hasWiki = false, language = null)
        val items = DraftBuilder.items(ref, noLicense, Fetched.Missing, Fetched.Found(emptyList()), Fetched.Missing)
        fun status(id: String) = items.first { it.id == id }.fetch
        assertEquals(FetchStatus.NOT_FOUND, status("LICENSE"))
        assertEquals(FetchStatus.NOT_FOUND, status("DESCRIPTION"))
        assertEquals(FetchStatus.NOT_FOUND, status("README"))
        assertEquals(FetchStatus.NOT_FOUND, status("APK"))          // never invents an APK
        assertEquals(FetchStatus.NOT_FOUND, status("DOCS"))
        assertEquals(FetchStatus.FETCHED, status("ARCHIVE"))        // offered, not downloaded
        assertTrue(items.none { it.localFile != null })
    }

    @Test fun builderListsApksImagesAndFlagsExternalHosts() {
        val apk = AssetInfo("app.apk", 5000, "https://github.com/octo/demo/releases/download/v1/app.apk", "b".repeat(64))
        val evil = AssetInfo("x.apk", 5000, "https://evil.example/x.apk", null)
        val txt = AssetInfo("notes.txt", 5, "https://github.com/octo/demo/releases/download/v1/notes.txt", null)
        val items = DraftBuilder.items(
            ref, info, Fetched.Found("![a](a.png) ![b](https://example.com/b.png)"),
            Fetched.Found(listOf(release("v1", apk, evil, txt))), Fetched.Found("# changes"),
        )
        val apks = items.filter { it.kind == ItemKind.APK }
        assertEquals(2, apks.size)
        assertEquals(FetchStatus.FETCHED, apks.first { it.label.startsWith("app.apk") }.fetch)
        assertEquals(ItemNote.EXTERNAL_HOST, apks.first { it.label.startsWith("x.apk") }.note)
        val imgs = items.filter { it.kind == ItemKind.IMAGE }
        assertEquals(listOf(FetchStatus.FETCHED, FetchStatus.FAILED), imgs.map { it.fetch })
        assertTrue(items.any { it.id == "DOCS:https://demo.example" })
        assertTrue(items.any { it.id == "DOCS:https://github.com/octo/demo/wiki" })
    }

    // ---- review operations ----
    private fun draft(items: List<DraftItem>) = StoreDraft("d1", ref, 1, 1, false, 0, ImportMode.INFO_ONLY, 2, "main", "demo", "A demo", emptyList(), null, items)
    private fun sample() = draft(DraftBuilder.items(ref, info, Fetched.Found("![a](a.png)"), Fetched.Found(emptyList()), Fetched.Missing))

    @Test fun acceptAllRejectAllAndPerItemOverrides() {
        var d = DraftOps.acceptAll(sample(), 2)
        assertTrue(DraftOps.canApprove(d))
        d = DraftOps.setDecision(d, "IMG:https://raw.githubusercontent.com/octo/demo/main/a.png", Decision.REJECTED, 3)  // change after accept-all
        assertEquals(Decision.REJECTED, d.items.first { it.kind == ItemKind.IMAGE }.decision)
        assertEquals(Decision.ACCEPTED, d.items.first { it.id == "README" || it.id == "NAME" }.decision)
        d = DraftOps.rejectAll(d, 4)
        assertFalse(DraftOps.canApprove(d))
        d = DraftOps.setDecision(d, "DESCRIPTION", Decision.ACCEPTED, 5)       // accept only the description
        assertTrue(DraftOps.canApprove(d))
        assertEquals(0, DraftOps.counts(DraftOps.reset(d, 6)).accepted)
    }

    @Test fun sourceOwnerAndLicenseCannotBeHiddenOrRejected() {
        val d = DraftOps.rejectAll(sample(), 2)
        val shown = DraftOps.visible(d).map { it.id }
        assertTrue("OWNER" in shown && "URL" in shown && "LICENSE" in shown)
        assertEquals(Decision.REVIEW, DraftOps.setDecision(d, "OWNER", Decision.REJECTED, 3).items.first { it.id == "OWNER" }.decision)
        assertFalse("README" in shown)
    }

    @Test fun approvalKeepsOriginalsSeparateFromDisplayEdits() {
        val d = DraftOps.acceptAll(sample(), 2).copy(displayName = "My Demo", displayDescription = "mine")
        val a = DraftOps.approve(d, 9)
        assertTrue(a.approved)
        assertEquals("demo", a.items.first { it.id == "NAME" }.value)       // original untouched
        assertEquals("My Demo", a.displayName)
    }

    @Test fun coverMustBeAnAcceptedImage() {
        val d = sample()
        val imgId = d.items.first { it.kind == ItemKind.IMAGE }.id
        assertNull(DraftOps.setCover(d, imgId, 2).coverItemId)              // not accepted yet
        val acc = DraftOps.setDecision(d, imgId, Decision.ACCEPTED, 2)
        assertEquals(imgId, DraftOps.setCover(acc, imgId, 3).coverItemId)
    }

    // ---- persistence + update merge ----
    @Test fun draftJsonRoundTrip() {
        val d = DraftOps.acceptAll(sample(), 2).copy(categories = listOf("TECH"), coverItemId = null)
        assertEquals(d, DraftJson.fromJson(DraftJson.toJson(d)))
    }

    @Test fun refreshShowsChangesAndKeepsDecisionsForUnchangedItems() {
        val old = DraftOps.acceptAll(sample(), 2)
        val newInfo = info.copy(description = "A better demo")
        val fresh = DraftBuilder.items(ref, newInfo, Fetched.Found("![a](a.png)"),
            Fetched.Found(listOf(release("v9"))), Fetched.Missing)
        val changes = DraftMerge.diff(old.items, fresh)
        assertTrue(changes.any { it.kind == ChangeKind.CHANGED && it.item.id == "DESCRIPTION" })
        assertTrue(changes.any { it.kind == ChangeKind.ADDED && it.item.id == "REL:v9" })
        assertFalse(changes.any { it.item.id == "NAME" })                  // unchanged: not listed

        val (merged, obsolete) = DraftMerge.merge(old, fresh, 4096, 10)
        assertEquals(Decision.REVIEW, merged.items.first { it.id == "DESCRIPTION" }.decision)   // changed -> needs review again
        assertTrue(merged.items.first { it.id == "DESCRIPTION" }.changed)
        assertEquals(Decision.ACCEPTED, merged.items.first { it.id == "NAME" }.decision)         // kept
        assertEquals(4096L, merged.sizeKb)
        assertNotNull(obsolete)
    }
}
