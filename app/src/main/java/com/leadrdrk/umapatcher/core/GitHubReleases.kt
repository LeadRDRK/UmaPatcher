package com.leadrdrk.umapatcher.core

import android.net.Uri
import java.net.URL
import com.leadrdrk.umapatcher.utils.fetchJson as utilsFetchJson

class GitHubReleases(
    private val repoPath: String
) {
    private val releasesApi = URL("https://api.github.com/repos/$repoPath/releases")
    private val latestApi = URL("https://api.github.com/repos/$repoPath/releases/latest")

    private fun fetchJson(url: URL): HashMap<*, *> =
        utilsFetchJson(url, "application/vnd.github+json")

    fun fetchReleases() = fetchJson(releasesApi)
    fun fetchLatest() = fetchJson(latestApi)

    fun getReleaseUrl(tagName: String) =
        "https://github.com/$repoPath/releases/tag/${Uri.encode(tagName)}"
}