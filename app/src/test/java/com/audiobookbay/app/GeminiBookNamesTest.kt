package com.audiobookbay.app

import com.audiobookbay.app.data.remote.GeminiBookNames
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test

class GeminiBookNamesTest {
    private fun response(value: JsonObject, finish: String = "STOP") = buildJsonObject {
        putJsonArray("candidates") { addJsonObject {
            put("finishReason", finish)
            putJsonObject("content") { putJsonArray("parts") { addJsonObject { put("text", value.toString()) } } }
        } }
    }.toString()

    @Test fun unicodeInitialsAndMultipleAuthorsSurviveProtocol() {
        val result = GeminiBookNames.parse(response(buildJsonObject {
            put("title", "  1Q84  "); put("author", "Haruki Murakami, J. R. R. Tolkien, José Saramago")
        }))
        assertEquals("1Q84", result.title)
        assertEquals("Haruki Murakami, J. R. R. Tolkien, José Saramago", result.author)
    }

    @Test fun unresolvedAndBlankFieldsRemainNull() {
        val result = GeminiBookNames.parse(response(buildJsonObject { put("title", JsonNull); put("author", "  ") }))
        assertNull(result.title); assertNull(result.author)
    }

    @Test fun requestUsesStructuredOutputAndTreatsNamesAsData() {
        val raw = "DUNE: A Subtitle [Series 1] - Frank Herbert [MP3 64kbps]"
        val request = Json.parseToJsonElement(GeminiBookNames.request(raw, "FRANK HERBERT")).jsonObject
        val config = request.getValue("generationConfig").jsonObject
        assertEquals("application/json", config.getValue("responseMimeType").jsonPrimitive.content)
        val text = request.getValue("contents").jsonArray.first().jsonObject.getValue("parts")
            .jsonArray.first().jsonObject.getValue("text").jsonPrimitive.content
        assertEquals(raw, Json.parseToJsonElement(text).jsonObject.getValue("title").jsonPrimitive.content)
        assertEquals(setOf("title", "author"), Json.parseToJsonElement(text).jsonObject.keys)
    }

    @Test fun truncatedAndInvalidResponsesAreRejected() {
        val good = buildJsonObject { put("title", "Dune"); put("author", "Frank Herbert") }
        for (body in listOf("{}", "not json", response(good, "MAX_TOKENS"),
            response(buildJsonObject { put("title", 42); put("author", JsonNull) }),
            response(buildJsonObject { put("title", "Book") }))) {
            assertTrue(runCatching { GeminiBookNames.parse(body) }.isFailure)
        }
    }
}
