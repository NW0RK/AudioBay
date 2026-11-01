package com.audiobookbay.app.data.remote

import com.audiobookbay.app.domain.model.Audiobook
import com.audiobookbay.app.domain.model.AudiobookDetail
import com.audiobookbay.app.domain.model.AudiobookSource
import com.audiobookbay.app.domain.model.TorrentInfo
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import timber.log.Timber

/**
 * HTML parser for AudioBookBay.lu public pages.
 *
 * NOTE FOR CODEX:
 * AudioBookBay does not have an official API and frequently changes domains or updates anti-bot policies.
 * Detail pages expose the 40-character hex infohash in an info table even when not logged in.
 * Standard magnet link can be constructed as:
 * `magnet:?xt=urn:btih:<infoHash>&dn=<encodedTitle>&tr=udp%3A%2F%2Ftracker.openbittorrent.com%3A80`
 */
class AudioBookBayParser {

    /**
     * Parses a page containing list of audiobooks (home, category, search results).
     */
    fun parseAudiobookList(html: String, baseUrl: String): List<Audiobook> {
        val document: Document = Jsoup.parse(html, baseUrl)
        val audiobooks = mutableListOf<Audiobook>()

        // AudioBookBay typically uses .post or article elements for listings
        val postElements = document.select("div.post")
        for (post in postElements) {
            try {
                val titleElement = post.selectFirst(".postTitle h2 a, .postTitle a, h2 a") ?: continue
                val title = titleElement.text().trim()
                val detailUrl = titleElement.absUrl("href")
                val id = extractIdFromUrl(detailUrl)

                val coverImg = post.selectFirst(".postContent img")?.absUrl("src")

                // Extract metadata text
                val contentText = post.select(".postContent").text()
                val category = post.select(".postInfo a[rel='category tag']").firstOrNull()?.text()

                audiobooks.add(
                    Audiobook(
                        id = id,
                        title = title,
                        author = extractAuthor(contentText, title),
                        narrator = extractNarrator(contentText),
                        coverImageUrl = coverImg,
                        category = category,
                        detailUrl = detailUrl,
                        source = AudiobookSource.AUDIOBOOKBAY,
                    ),
                )
            } catch (e: Exception) {
                Timber.w(e, "Failed to parse audiobook post element")
            }
        }
        return audiobooks
    }

    /**
     * Parses an audiobook detail page to extract description, infohash, format, and files.
     */
    fun parseAudiobookDetail(html: String, idOrSlug: String, baseUrl: String): AudiobookDetail {
        val document: Document = Jsoup.parse(html, baseUrl)

        val title = document.selectFirst(".postTitle h1, h1")?.text()?.trim() ?: idOrSlug
        val coverUrl = document.selectFirst(".postContent img")?.absUrl("src")
        val description = document.select(".postContent p").text().trim()

        // Extract Info Hash from table
        val infoHash = extractInfoHash(document)

        val magnetUri = if (!infoHash.isNullOrBlank()) {
            buildMagnetUri(infoHash, title)
        } else {
            null
        }

        val torrentInfo = if (!infoHash.isNullOrBlank() && !magnetUri.isNullOrBlank()) {
            TorrentInfo(
                infoHash = infoHash,
                magnetUri = magnetUri,
                trackerUrls = listOf(
                    "udp://tracker.openbittorrent.com:80/announce",
                    "udp://tracker.opentrackr.org:1337/announce",
                    "udp://tracker.coppersurfer.tk:6969/announce",
                ),
            )
        } else {
            null
        }

        return AudiobookDetail(
            id = idOrSlug,
            title = title,
            author = extractAuthor(description, title),
            narrator = extractNarrator(description),
            description = description,
            coverImageUrl = coverUrl,
            torrentInfo = torrentInfo,
            source = AudiobookSource.AUDIOBOOKBAY,
        )
    }

    private fun extractInfoHash(document: Document): String? {
        val rows = document.select("table tr")
        for (row in rows) {
            val thOrTd = row.select("td, th").map { it.text().trim() }
            if (thOrTd.size >= 2) {
                val label = thOrTd[0].lowercase()
                if (label.contains("info hash") || label.contains("infohash")) {
                    val hash = thOrTd[1].trim()
                    if (hash.matches(Regex("^[a-fA-F0-9]{40}$"))) {
                        return hash.lowercase()
                    }
                }
            }
        }
        // Fallback: search regex in raw html
        val regex = Regex("(?i)info[\\s_]?hash[:\\s]*([a-fA-F0-9]{40})")
        val match = regex.find(document.html())
        return match?.groupValues?.get(1)?.lowercase()
    }

    private fun buildMagnetUri(infoHash: String, title: String): String {
        val encodedTitle = java.net.URLEncoder.encode(title, "UTF-8")
        val trackers = listOf(
            "udp%3A%2F%2Ftracker.openbittorrent.com%3A80",
            "udp%3A%2F%2Ftracker.opentrackr.org%3A1337%2Fannounce",
            "udp%3A%2F%2Fopen.stealth.si%3A80%2Fannounce",
        )
        val trackerParams = trackers.joinToString("") { "&tr=$it" }
        return "magnet:?xt=urn:btih:$infoHash&dn=$encodedTitle$trackerParams"
    }

    private fun extractIdFromUrl(url: String): String {
        return url.trimEnd('/').substringAfterLast('/')
    }

    private fun extractAuthor(text: String, titleFallback: String): String {
        val authorMatch = Regex("(?i)(?:author|written by)[:\\s]+([^\\n,\\.<]+)").find(text)
        if (authorMatch != null) {
            return authorMatch.groupValues[1].trim()
        }
        // Check if title has "by Author"
        val byMatch = Regex("(?i)by\\s+([^\\–\\-\\[\\(]+)").find(titleFallback)
        return byMatch?.groupValues?.get(1)?.trim() ?: "Unknown Author"
    }

    private fun extractNarrator(text: String): String? {
        val match = Regex("(?i)(?:narrator|read by)[:\\s]+([^\\n,\\.<]+)").find(text)
        return match?.groupValues?.get(1)?.trim()
    }
}
