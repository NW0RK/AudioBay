package com.audiobookbay.app.data.remote

import com.audiobookbay.app.domain.model.AudiobookComment
import com.audiobookbay.app.domain.model.Audiobook
import com.audiobookbay.app.domain.model.AudiobookDetail
import com.audiobookbay.app.domain.model.AudiobookSource
import com.audiobookbay.app.domain.model.TorrentInfo
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
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
        val postElements = document.select("div.post, article.post")
        for (post in postElements) {
            try {
                val titleElement = post.selectFirst(".postTitle h2 a, .postTitle a, h2 a") ?: continue
                val title = titleElement.text().trim()
                val detailUrl = titleElement.absUrl("href")
                val id = extractIdFromUrl(detailUrl)
                if (title.isBlank() || id.isBlank()) continue

                val coverImg = post.selectFirst(".postContent img")?.absUrl("src")

                // Extract metadata text
                val contentText = post.select(".postContent").text()
                val category = post.select(".postInfo a[rel='category tag']").firstOrNull()?.text()
                    ?: Regex("Category:\\s*(.*?)(?:Language:|$)", RegexOption.IGNORE_CASE)
                        .find(post.select(".postInfo").text())?.groupValues?.get(1)?.trim()

                val metadata = parseMetadata(post)
                audiobooks.add(
                    Audiobook(
                        id = id,
                        title = title,
                        author = extractAuthor(contentText, title),
                        narrator = extractNarrator(contentText),
                        coverImageUrl = coverImg,
                        category = category,
                        language = metadata["Language"], datePosted = metadata["Posted"],
                        uploader = metadata["Shared by"], audioFormat = metadata["Format"],
                        bitrate = metadata["Bitrate"], formattedSize = metadata["File Size"],
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

        val title = document.selectFirst(".postTitle h1, article h1")?.text()?.trim()
            ?.takeIf { it.isNotBlank() } ?: throw IllegalArgumentException("Unrecognized audiobook detail page")
        val coverUrl = document.selectFirst(".postContent img")?.absUrl("src")
        val metadataText = document.select(".postContent").text()
        val description = document.select(".postContent p")
            .filterNot { it.text().matches(Regex("(?is)^(?:Shared by|Written by|Read by|Format:|Bitrate:|Unabridged|Abridged).*")) }
            .joinToString("\n\n") { it.text().trim() }.trim()

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

        val metadata = parseMetadata(document.selectFirst("div.post, article.post") ?: document.body())
        return AudiobookDetail(
            id = idOrSlug,
            title = title,
            author = extractAuthor(metadataText, title),
            narrator = extractNarrator(metadataText),
            description = description,
            comments = parseComments(document),
            coverImageUrl = coverUrl,
            torrentInfo = torrentInfo,
            language = metadata["Language"].orEmpty(), datePosted = metadata["Posted"],
            uploader = metadata["Shared by"], formattedSize = metadata["File Size"].orEmpty(),
            audioFormat = metadata["Format"].orEmpty(), bitrate = metadata["Bitrate"],
            source = AudiobookSource.AUDIOBOOKBAY,
        )
    }

    private fun parseMetadata(post: org.jsoup.nodes.Element): Map<String, String> {
        val copy = post.clone()
        copy.select(".commentlist, .comment-list, .commentZone, #comments, script, style").remove()
        val text = copy.select(".postInfo, .postContent").joinToString("\n") { it.wholeText() }
        val labels = "Language|Keywords|Posted|Format|Bitrate|File Size|Shared by|Written by|Read by|Category"
        return Regex("(?i)($labels):\\s*(.*?)(?=(?:$labels):|\\b(?:Unabridged|Abridged)\\b|[\\r\\n]|$)")
            .findAll(text).mapNotNull { match ->
                val key = labels.split("|").first { it.equals(match.groupValues[1], true) }
                val value = match.groupValues[2].trim().trimEnd('/').trim()
                value.takeIf { it.isNotBlank() }?.let { key to it }
            }.toMap()
    }

    private fun parseComments(document: Document): List<AudiobookComment> =
        document.select(".commentlist li[id^=comment-], .comment-list li[id^=comment-], .commentZone [id^=comment-]")
            .distinctBy { it.id() }
            .mapNotNull { element ->
                // Remove nested replies before reading this comment's content.
                val own = element.clone().apply { select(".children, ul, ol, script, style, .reply, form").remove() }
                val author = own.selectFirst(".comment-author cite, .comment-author .fn, cite, .commentAuthor")?.text().orEmpty()
                val date = own.selectFirst(".comment-meta, .commentmetadata, .commentDate, time, small")?.text().orEmpty()
                val body = own.selectFirst(".comment-content, .comment-body .comment-text, .commentText")
                val text = if (body != null) body.select("p").takeIf { it.isNotEmpty() }?.joinToString("\n\n") { it.text() } ?: body.text()
                    else own.select("p").joinToString("\n\n") { it.text() }
                if (text.isBlank()) null else AudiobookComment(element.id(), author, date, text)
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
        val author = metadataValue(text, "author|written by")
        if (author != null) return author
        val byMatch = Regex("(?i)\\bby\\s+(.+)$").find(titleFallback)
        return byMatch?.groupValues?.get(1)?.trim()
            ?: titleFallback.substringAfterLast(" - ", "").takeIf { it.isNotBlank() }
            ?: "Unknown Author"
    }

    private fun extractNarrator(text: String): String? = metadataValue(text, "narrator|narrated by|read by")

    private fun metadataValue(text: String, labels: String): String? =
        Regex("(?i)(?:$labels)[:\\s]+(.+?)(?=\\s+(?:read by|narrated by|narrator|format|bitrate|language|unabridged|abridged)\\b|[\\n]|$)")
            .find(text)?.groupValues?.get(1)?.trim()?.trimEnd('.')
}
