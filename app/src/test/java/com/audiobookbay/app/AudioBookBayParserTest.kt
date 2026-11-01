package com.audiobookbay.app

import com.audiobookbay.app.data.remote.AudioBookBayParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioBookBayParserTest {

    private val parser = AudioBookBayParser()

    @Test
    fun parseAudiobookList_extractsPostsCorrectly() {
        val sampleHtml = """
            <html>
            <body>
                <div class="post">
                    <div class="postTitle">
                        <h2><a href="https://audiobookbay.lu/audio-books/the-hobbit-j-r-r-tolkien/">The Hobbit – J. R. R. Tolkien</a></h2>
                    </div>
                    <div class="postContent">
                        <img src="https://audiobookbay.lu/covers/hobbit.jpg" />
                        <p>Format: MP3. Bitrate: 64 kbps. Author: J. R. R. Tolkien. Narrator: Rob Inglis.</p>
                    </div>
                    <div class="postInfo">
                        <a rel="category tag">Fantasy</a>
                    </div>
                </div>
            </body>
            </html>
        """.trimIndent()

        val books = parser.parseAudiobookList(sampleHtml, "https://audiobookbay.lu")

        assertEquals(1, books.size)
        val book = books.first()
        assertEquals("The Hobbit – J. R. R. Tolkien", book.title)
        assertEquals("the-hobbit-j-r-r-tolkien", book.id)
        assertEquals("Fantasy", book.category)
        assertTrue(book.coverImageUrl?.contains("hobbit.jpg") == true)
    }

    @Test
    fun parseAudiobookDetail_extractsInfoHashAndConstructsMagnet() {
        val sampleHtml = """
            <html>
            <body>
                <div class="postTitle">
                    <h1>The Hobbit – J. R. R. Tolkien</h1>
                </div>
                <div class="postContent">
                    <img src="https://audiobookbay.lu/covers/hobbit.jpg" />
                    <p>In a hole in the ground there lived a hobbit.</p>
                    <table>
                        <tr>
                            <td>Tracker:</td>
                            <td>udp://tracker.openbittorrent.com:80</td>
                        </tr>
                        <tr>
                            <td>Info Hash:</td>
                            <td>4a8a35e2528b97d27e2832810a0e28f3521b38e4</td>
                        </tr>
                    </table>
                </div>
            </body>
            </html>
        """.trimIndent()

        val detail = parser.parseAudiobookDetail(sampleHtml, "the-hobbit", "https://audiobookbay.lu")

        assertEquals("the-hobbit", detail.id)
        assertNotNull(detail.torrentInfo)
        assertEquals("4a8a35e2528b97d27e2832810a0e28f3521b38e4", detail.torrentInfo?.infoHash)
        assertTrue(detail.torrentInfo?.magnetUri?.startsWith("magnet:?xt=urn:btih:4a8a35e2528b97d27e2832810a0e28f3521b38e4") == true)
    }

    @Test
    fun inlineMetadata_doesNotMergeAuthorNarratorAndFormat() {
        val detail = parser.parseAudiobookDetail("""
            <div class="postTitle"><h1>A Public Domain Story - Example Writer</h1></div>
            <div class="postContent"><p>Written by Example Writer Read by Example Reader Format: MP3 Bitrate: 96 Kbps Unabridged</p></div>
        """.trimIndent(), "story", "https://example.org")
        assertEquals("Example Writer", detail.author)
        assertEquals("Example Reader", detail.narrator)
        assertEquals("MP3", detail.audioFormat)
        assertEquals("96 Kbps", detail.bitrate)
    }

    @Test(expected = IllegalArgumentException::class)
    fun challengePage_isNotAcceptedAsBookDetail() {
        parser.parseAudiobookDetail("<h1>Just a moment</h1>", "story", "https://example.org")
    }

    @Test
    fun listHandlesRelativeLinksAndPlainCategoryText() {
        val books = parser.parseAudiobookList("""
            <article class="post">
                <div class="postTitle"><h2><a href="/abss/story/">Story - Example Writer</a></h2></div>
                <div class="postInfo">Category: Classic Language: English</div>
                <div class="postContent"><img src="/cover.jpg" /></div>
            </article>
        """.trimIndent(), "https://example.org")
        assertEquals("https://example.org/abss/story/", books.single().detailUrl)
        assertEquals("https://example.org/cover.jpg", books.single().coverImageUrl)
        assertEquals("Classic", books.single().category)
        assertEquals("Example Writer", books.single().author)
    }
}
