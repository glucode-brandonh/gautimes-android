package com.glucode.gautimes.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class DateUtilsTest {

    @Test
    fun `formatIsoTime adds 2 hours correctly`() {
        // Morning time
        assertEquals("10:00", DateUtils.formatIsoTime("2026-05-28T08:00:00Z"))
        
        // Afternoon time
        assertEquals("17:30", DateUtils.formatIsoTime("2026-05-28T15:30:00Z"))
        
        // Time with offset
        assertEquals("12:00", DateUtils.formatIsoTime("2026-05-28T10:00:00+00:00"))
    }

    @Test
    fun `formatIsoTime handles midnight rollover`() {
        // Late night
        assertEquals("00:45", DateUtils.formatIsoTime("2026-05-28T22:45:00Z"))
        
        // Very late night
        assertEquals("01:15", DateUtils.formatIsoTime("2026-05-28T23:15:00Z"))
    }

    @Test
    fun `formatIsoTime returns 00-00 on invalid input`() {
        assertEquals("00:00", DateUtils.formatIsoTime("invalid-date"))
        assertEquals("00:00", DateUtils.formatIsoTime(""))
    }
}
