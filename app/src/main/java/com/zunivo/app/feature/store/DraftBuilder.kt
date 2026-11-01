package com.zunivo.app.feature.store

import com.zunivo.app.domain.DraftItem
import com.zunivo.app.domain.FetchStatus
import com.zunivo.app.domain.Fetched
import com.zunivo.app.domain.GitHubHosts
import com.zunivo.app.domain.ItemKind
import com.zunivo.app.domain.ItemNote
import com.zunivo.app.domain.ReadmeExtractor
import com.zunivo.app.domain.ReleaseInfo
import com.zunivo.app.domain.RepoInfo
import com.zunivo.app.domain.RepoRef

/** Turns what GitHub returned into reviewable draft items. Never invents data: missing things stay NOT_FOUND. */
object DraftBuilder {
    const val MAX_RELEASES = 5
    const val MAX_IMAGES = 8
    const val README_CAP = 200_000

    private fun found(id: String, kind: ItemKind, label: String, value: String?, url: String? = null, size: Long? = null) =
        DraftItem(id, kind, label, value, if (value.isNullOrBlank()) FetchStatus.NOT_FOUND else FetchStatus.FETCHED, sourceUrl = url, sizeBytes = size)

    fun items(
        ref: RepoRef,
        repo: RepoInfo,
        readme: Fetched<String>,
        releases: Fetched<List<ReleaseInfo>>,
        changelog: Fetched<String>,
    ): List<DraftItem> {
        val out = ArrayList<DraftItem>()
        out += found("NAME", ItemKind.NAME, "Name", repo.name)
        out += found("DESCRIPTION", ItemKind.DESCRIPTION, "Description", repo.description)
        out += found("OWNER", ItemKind.OWNER, "Owner", repo.ownerLogin, "https://github.com/${repo.ownerLogin}")
        out += found("URL", ItemKind.REPO_URL, "Repository", repo.htmlUrl, repo.htmlUrl)
        out += found("LANG", ItemKind.LANGUAGE, "Language", repo.language)
        val licenseText = listOfNotNull(repo.licenseName, repo.licenseSpdx).distinct().joinToString(" / ")
        out += found("LICENSE", ItemKind.LICENSE, "License", licenseText.ifBlank { null }, repo.licenseUrl)

        // README + its images
        when (readme) {
            is Fetched.Found -> {
                out += found("README", ItemKind.README, "README", readme.value.take(README_CAP), "${repo.htmlUrl}#readme")
                ReadmeExtractor.images(readme.value, ref, repo.defaultBranch, MAX_IMAGES).forEach { img ->
                    out += DraftItem(
                        id = "IMG:${img.url}", kind = ItemKind.IMAGE,
                        label = img.alt.ifBlank { img.url.substringBefore('?').substringAfterLast('/') },
                        value = img.url, fetch = if (img.allowedHost) FetchStatus.FETCHED else FetchStatus.FAILED,
                        sourceUrl = img.url, note = if (img.allowedHost) null else ItemNote.EXTERNAL_HOST,
                    )
                }
            }
            Fetched.Missing -> out += DraftItem("README", ItemKind.README, "README", null, FetchStatus.NOT_FOUND)
            Fetched.Failed -> out += DraftItem("README", ItemKind.README, "README", null, FetchStatus.FAILED)
        }

        // releases, release notes and APK assets
        val rel: List<ReleaseInfo> = when (releases) {
            is Fetched.Found -> releases.value.take(MAX_RELEASES)
            else -> emptyList()
        }
        rel.forEach { r ->
            out += found("REL:${r.tag}", ItemKind.RELEASE, r.name ?: r.tag, r.body?.take(4000) ?: "(${r.tag})", r.htmlUrl)
        }
        if (releases is Fetched.Failed) out += DraftItem("REL", ItemKind.RELEASE, "Releases", null, FetchStatus.FAILED)
        if (releases is Fetched.Missing || (releases is Fetched.Found && rel.isEmpty())) {
            out += DraftItem("REL", ItemKind.RELEASE, "Releases", null, FetchStatus.NOT_FOUND)
        }

        when (changelog) {
            is Fetched.Found -> out += found("CHANGELOG", ItemKind.CHANGELOG, "CHANGELOG.md", changelog.value.take(README_CAP), "${repo.htmlUrl}/blob/${repo.defaultBranch}/CHANGELOG.md")
            Fetched.Missing -> out += DraftItem("CHANGELOG", ItemKind.CHANGELOG, "CHANGELOG.md", null, FetchStatus.NOT_FOUND)
            Fetched.Failed -> out += DraftItem("CHANGELOG", ItemKind.CHANGELOG, "CHANGELOG.md", null, FetchStatus.FAILED)
        }

        // documentation links (shown as links; never opened automatically)
        val docs = ArrayList<Pair<String, String>>()
        repo.homepage?.takeIf { it.startsWith("https://") }?.let { docs.add("Homepage" to it) }
        if (repo.hasWiki) docs.add("Wiki" to "${repo.htmlUrl}/wiki")
        if (docs.isEmpty()) out += DraftItem("DOCS", ItemKind.DOCS_LINK, "Documentation", null, FetchStatus.NOT_FOUND)
        docs.forEach { (label, url) -> out += found("DOCS:$url", ItemKind.DOCS_LINK, label, url, url) }

        // APK files published with releases (only genuine .apk assets on GitHub hosts)
        val apks = rel.flatMap { r -> r.assets.filter { it.name.endsWith(".apk", ignoreCase = true) && it.sizeBytes > 0 }.map { r to it } }
        if (apks.isEmpty()) {
            out += DraftItem("APK", ItemKind.APK, "APK", null, FetchStatus.NOT_FOUND)
        } else apks.forEach { (r, a) ->
            val allowed = GitHubHosts.isAllowedUrl(a.url)
            out += DraftItem(
                id = "APK:${r.tag}:${a.name}", kind = ItemKind.APK, label = "${a.name} (${r.tag})", value = a.url,
                fetch = if (allowed) FetchStatus.FETCHED else FetchStatus.FAILED, sourceUrl = a.url, sizeBytes = a.sizeBytes,
                expectedSha256 = a.sha256, note = if (allowed) null else ItemNote.EXTERNAL_HOST,
            )
        }

        // source archive: offered, never downloaded automatically
        out += DraftItem(
            "ARCHIVE", ItemKind.SOURCE_ARCHIVE, "Source archive (${repo.defaultBranch})",
            "https://api.github.com/repos/${ref.owner}/${ref.repo}/zipball/${repo.defaultBranch}", FetchStatus.FETCHED,
            sourceUrl = "${repo.htmlUrl}/archive/refs/heads/${repo.defaultBranch}.zip",
            sizeBytes = if (repo.sizeKb > 0) repo.sizeKb * 1024 else null,
        )
        return out
    }
}
