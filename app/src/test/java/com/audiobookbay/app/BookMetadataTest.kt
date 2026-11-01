package com.audiobookbay.app
import com.audiobookbay.app.data.remote.AudioBookBayParser
import com.audiobookbay.app.domain.model.*
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test
class BookMetadataTest {
    private fun post(title: String, metadata: String) = """<div class="post"><div class="postTitle"><h2><a href="/abss/$title/">$title</a></h2></div><div class="postInfo">Language: French Keywords: test</div><div class="postContent">$metadata</div></div>"""
    @Test fun `metadata stays within its listing and preserves variable bitrate`() {
        val html = post("one", "<p>Shared by: Élodie</p><p>Posted: 1 Nov 2026<br>Format: M4B / Bitrate: Variable<br>File Size: 2.36 GBs</p>") + post("two", "<p>Format: MP3 / Bitrate: ?</p>")
        val books = AudioBookBayParser().parseAudiobookList(html, "https://audiobookbay.lu")
        assertEquals(2, books.size)
        assertEquals("French", books[0].language)
        assertEquals("Élodie", books[0].uploader)
        assertEquals("1 Nov 2026", books[0].datePosted)
        assertEquals("M4B", books[0].audioFormat)
        assertEquals("Variable", books[0].bitrate)
        assertEquals("2.36 GBs", books[0].formattedSize)
        assertEquals("?", books[1].bitrate)
        assertNull(books[1].uploader)
        assertNull(books[1].formattedSize)
    }
    @Test fun `detail excludes comment metadata`() {
        val html = """<div class="post"><div class="postTitle"><h1>Book</h1></div><div class="postContent"><p>Written by: Author</p></div><div id="comments"><div class="postContent">Language: Fake Format: Fake</div></div></div>"""
        val book = AudioBookBayParser().parseAudiobookDetail(html, "book", "https://audiobookbay.lu")
        assertEquals("", book.language)
        assertEquals("", book.audioFormat)
        assertNull(book.datePosted)
    }
    @Test fun `old cached records decode without invented metadata`() {
        val json = Json { ignoreUnknownKeys = true }
        val summary = json.decodeFromString<Audiobook>("""{"id":"old","title":"Old","author":"Author"}""")
        assertNull(summary.language)
        assertNull(summary.audioFormat)
        val detail = json.decodeFromString<AudiobookDetail>("""{"id":"old","title":"Old","author":"Author"}""")
        assertEquals("", detail.language)
        assertEquals("", detail.audioFormat)
    }
}
