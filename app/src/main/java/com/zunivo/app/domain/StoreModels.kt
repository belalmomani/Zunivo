package com.zunivo.app.domain

import java.net.URI

data class RepoRef(val owner: String, val repo: String) {
    val url: String get() = "https://github.com/$owner/$repo"
}

object GitHubUrl {
    private val OWNER = Regex("^[A-Za-z0-9](?:[A-Za-z0-9-]{0,38})$")
    private val REPO = Regex("^[A-Za-z0-9._-]{1,100}$")

    /** Accepts github.com/owner/repo style links only (https, no credentials, no port). */
    fun parse(raw: String?): RepoRef? {
        var t = raw?.trim() ?: return null
        if (t.isEmpty() || t.length > 300) return null
        if (t.startsWith("github.com/") || t.startsWith("www.github.com/")) t = "https://$t"
        val uri = try { URI(t) } catch (e: Exception) { return null }
        if (uri.scheme != "https" || uri.userInfo != null || uri.port != -1) return null
        val host = uri.host?.lowercase() ?: return null
        if (host != "github.com" && host != "www.github.com") return null
        val parts = (uri.path ?: "").split('/').filter { it.isNotEmpty() }
        if (parts.size < 2) return null
        val owner = parts[0]
        val repo = parts[1].removeSuffix(".git")
        if (!OWNER.matches(owner) || !REPO.matches(repo) || repo == "." || repo == "..") return null
        return RepoRef(owner, repo)
    }
}

/** Only GitHub's own hosts are contacted or followed (including redirects). */
object GitHubHosts {
    fun isAllowed(host: String?): Boolean {
        val h = host?.lowercase() ?: return false
        return h == "github.com" || h == "api.github.com" || h == "codeload.github.com" || h.endsWith(".githubusercontent.com")
    }

    fun isAllowedUrl(url: String): Boolean {
        val uri = try { URI(url) } catch (e: Exception) { return false }
        return uri.scheme == "https" && uri.userInfo == null && isAllowed(uri.host)
    }
}

// ---------- data fetched from GitHub ----------

data class RepoInfo(
    val name: String,
    val description: String?,
    val ownerLogin: String,
    val htmlUrl: String,
    val language: String?,
    val licenseName: String?,
    val licenseSpdx: String?,
    val licenseUrl: String?,
    val sizeKb: Long,
    val defaultBranch: String,
    val homepage: String?,
    val hasWiki: Boolean,
    val archived: Boolean,
    val fork: Boolean,
)

data class AssetInfo(val name: String, val sizeBytes: Long, val url: String, val sha256: String?)

data class ReleaseInfo(
    val tag: String,
    val name: String?,
    val body: String?,
    val htmlUrl: String?,
    val prerelease: Boolean,
    val publishedAt: String?,
    val assets: List<AssetInfo>,
)

sealed interface Fetched<out T> {
    data class Found<T>(val value: T) : Fetched<T>
    data object Missing : Fetched<Nothing>
    data object Failed : Fetched<Nothing>
}

// ---------- draft model ----------

enum class ItemKind(val alwaysShown: Boolean) {
    NAME(false), DESCRIPTION(false), OWNER(true), REPO_URL(true), LANGUAGE(false), LICENSE(true),
    README(false), IMAGE(false), RELEASE(false), CHANGELOG(false), DOCS_LINK(false), APK(false), SOURCE_ARCHIVE(false),
}

enum class FetchStatus { FETCHED, NOT_FOUND, FAILED }
enum class Decision { REVIEW, ACCEPTED, REJECTED }
enum class ItemNote { EXTERNAL_HOST, TOO_LARGE, NOT_AN_IMAGE, CHECKSUM_MISMATCH, DOWNLOAD_FAILED }
enum class Verification { VERIFIED, UNVERIFIED }
enum class ImportMode { INFO_ONLY, INFO_AND_FILES }

data class DraftItem(
    val id: String,
    val kind: ItemKind,
    val label: String,
    val value: String?,
    val fetch: FetchStatus,
    val decision: Decision = Decision.REVIEW,
    val sourceUrl: String? = null,
    val sizeBytes: Long? = null,
    val localFile: String? = null,        // relative to the draft's private file folder
    val expectedSha256: String? = null,   // from GitHub, when it provides one
    val computedSha256: String? = null,
    val verification: Verification? = null,
    val note: ItemNote? = null,
    val changed: Boolean = false,
) {
    /** Items the user can accept/reject. Owner, source and license are always shown and cannot be hidden. */
    val decidable: Boolean get() = !kind.alwaysShown && fetch == FetchStatus.FETCHED
}

data class StoreDraft(
    val id: String,
    val repo: RepoRef,
    val createdAt: Long,
    val updatedAt: Long,
    val approved: Boolean,
    val approvedAt: Long,
    val mode: ImportMode,
    val sizeKb: Long?,
    val defaultBranch: String,
    val displayName: String,          // editable display data; originals stay in `items`
    val displayDescription: String,
    val categories: List<String>,
    val coverItemId: String?,
    val items: List<DraftItem>,
)

data class DraftCounts(val accepted: Int, val rejected: Int, val review: Int)

object DraftOps {
    private fun upd(d: StoreDraft, now: Long, items: List<DraftItem>) = d.copy(items = items, updatedAt = now)

    fun setDecision(d: StoreDraft, itemId: String, decision: Decision, now: Long): StoreDraft =
        upd(d, now, d.items.map { if (it.id == itemId && it.decidable) it.copy(decision = decision) else it })

    fun acceptAll(d: StoreDraft, now: Long) = upd(d, now, d.items.map { if (it.decidable) it.copy(decision = Decision.ACCEPTED) else it })
    fun rejectAll(d: StoreDraft, now: Long) = upd(d, now, d.items.map { if (it.decidable) it.copy(decision = Decision.REJECTED) else it })
    fun reset(d: StoreDraft, now: Long) = upd(d, now, d.items.map { if (it.decidable) it.copy(decision = Decision.REVIEW) else it })

    fun move(d: StoreDraft, itemId: String, delta: Int, now: Long): StoreDraft {
        val i = d.items.indexOfFirst { it.id == itemId }
        val j = i + delta
        if (i < 0 || j !in d.items.indices) return d
        val list = d.items.toMutableList()
        list.add(j, list.removeAt(i))
        return upd(d, now, list)
    }

    fun setCover(d: StoreDraft, itemId: String?, now: Long): StoreDraft {
        val ok = itemId == null || d.items.any { it.id == itemId && it.kind == ItemKind.IMAGE && it.decision == Decision.ACCEPTED }
        return if (ok) d.copy(coverItemId = itemId, updatedAt = now) else d
    }

    /** What an approved project shows: accepted items plus the always-shown source/owner/license. */
    fun visible(d: StoreDraft): List<DraftItem> =
        d.items.filter { it.kind.alwaysShown || (it.decision == Decision.ACCEPTED && it.fetch == FetchStatus.FETCHED) }

    fun counts(d: StoreDraft): DraftCounts {
        val dec = d.items.filter { it.decidable }
        return DraftCounts(dec.count { it.decision == Decision.ACCEPTED }, dec.count { it.decision == Decision.REJECTED }, dec.count { it.decision == Decision.REVIEW })
    }

    fun canApprove(d: StoreDraft): Boolean = counts(d).accepted > 0

    fun approve(d: StoreDraft, now: Long): StoreDraft {
        val cover = d.coverItemId?.takeIf { id -> d.items.any { it.id == id && it.decision == Decision.ACCEPTED } }
        return d.copy(approved = true, approvedAt = now, updatedAt = now, coverItemId = cover)
    }
}

enum class ChangeKind { ADDED, CHANGED, REMOVED }
data class DraftChange(val kind: ChangeKind, val item: DraftItem)

object DraftMerge {
    private fun same(a: DraftItem, b: DraftItem) =
        a.value == b.value && a.sizeBytes == b.sizeBytes && a.fetch == b.fetch && a.expectedSha256 == b.expectedSha256

    fun diff(old: List<DraftItem>, fresh: List<DraftItem>): List<DraftChange> {
        val oldById = old.associateBy { it.id }
        val freshById = fresh.associateBy { it.id }
        val out = ArrayList<DraftChange>()
        fresh.forEach { f ->
            val o = oldById[f.id]
            if (o == null) out.add(DraftChange(ChangeKind.ADDED, f))
            else if (!same(o, f)) out.add(DraftChange(ChangeKind.CHANGED, f))
        }
        old.forEach { o -> if (o.id !in freshById) out.add(DraftChange(ChangeKind.REMOVED, o)) }
        return out
    }

    /** Applies a refresh. Returns the merged draft and the local files that are now obsolete (to delete). */
    fun merge(old: StoreDraft, fresh: List<DraftItem>, sizeKb: Long?, now: Long): Pair<StoreDraft, List<String>> {
        val oldById = old.items.associateBy { it.id }
        val obsolete = ArrayList<String>()
        val merged = fresh.map { f ->
            val o = oldById[f.id]
            when {
                o == null -> f
                same(o, f) -> o
                else -> { o.localFile?.let { obsolete.add(it) }; f.copy(changed = true, decision = Decision.REVIEW) }
            }
        }
        val freshIds = fresh.map { it.id }.toSet()
        old.items.filter { it.id !in freshIds }.forEach { o -> o.localFile?.let { obsolete.add(it) } }
        val cover = old.coverItemId?.takeIf { id -> merged.any { it.id == id && it.decision == Decision.ACCEPTED } }
        return old.copy(items = merged, sizeKb = sizeKb ?: old.sizeKb, coverItemId = cover, updatedAt = now) to obsolete
    }
}
