package com.audiobookbay.app

import com.audiobookbay.app.ui.screens.downloads.formatRemainingTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DownloadFormattingTest {
    @Test fun unknownEtaIsHidden() {
        assertNull(formatRemainingTime(-1))
        assertNull(formatRemainingTime(0))
    }

    @Test fun subMinuteEtaIsCoarse() {
        assertEquals("<1m", formatRemainingTime(1))
        assertEquals("<1m", formatRemainingTime(59))
    }

    @Test fun minutesAndHoursAreFormatted() {
        assertEquals("1m", formatRemainingTime(60))
        assertEquals("59m", formatRemainingTime(59 * 60 + 59))
        assertEquals("1h 00m", formatRemainingTime(60 * 60))
        assertEquals("2h 05m", formatRemainingTime(2 * 60 * 60 + 5 * 60))
    }
}
