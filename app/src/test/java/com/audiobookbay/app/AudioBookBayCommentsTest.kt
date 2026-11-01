package com.audiobookbay.app

import com.audiobookbay.app.data.remote.AudioBookBayParser
import org.junit.Test
import org.junit.Assert.*

class AudioBookBayCommentsTest {
    private fun parse(comments: String) = AudioBookBayParser().parseAudiobookDetail("<div class='postTitle'><h1>Book</h1></div>$comments", "book", "https://example.com")

    @Test fun `extracts author date and decoded paragraphs without nested replies`() {
        val book = parse("""<ol class="commentlist"><li id="comment-1"><cite>Alice</cite><small>November 1</small><p>Thanks &amp; cheers!</p><p>Second paragraph.</p><ul class="children"><li id="comment-2"><cite>Bob</cite><p>Reply</p></li></ul></li></ol>""")
        assertEquals(2, book.comments.size)
        assertEquals("Alice", book.comments[0].author)
        assertEquals("November 1", book.comments[0].date)
        assertEquals("Thanks & cheers!\n\nSecond paragraph.", book.comments[0].text)
        assertEquals("Reply", book.comments[1].text)
    }

    @Test fun `missing comments and comment forms do not create entries`() {
        assertTrue(parse("<form id='commentform'><textarea>Write here</textarea></form>").comments.isEmpty())
    }
}
