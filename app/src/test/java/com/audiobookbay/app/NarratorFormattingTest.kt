package com.audiobookbay.app

import com.audiobookbay.app.ui.screens.detail.splitNarrators
import org.junit.Assert.assertEquals
import org.junit.Test

class NarratorFormattingTest {
    @Test fun separatesNamesAndPreservesUnicode() {
        assertEquals(listOf("Jos\u00e9", "Jane Doe", "\u674e\u660e", "John Smith"),
            splitNarrators(" Jos\u00e9, Jane Doe;\u674e\u660e\r\nJohn Smith ; ,"))
    }
    @Test fun preservesSingleAndAmbiguousNames() {
        assertEquals(listOf("Jane and John / Ensemble"), splitNarrators("Jane and John / Ensemble"))
    }
    @Test fun hidesMissingOrBlankNarrators() {
        listOf(null, "", "  ", ";,\n").forEach { assertEquals(emptyList<String>(), splitNarrators(it)) }
    }
}
