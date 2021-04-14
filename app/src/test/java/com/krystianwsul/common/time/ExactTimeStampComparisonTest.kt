package com.krystianwsul.common.time

import org.junit.Assert.assertEquals
import org.junit.Test

class ExactTimeStampComparisonTest {

    companion object {

        private const val localOffset = 2 * 60 * 60 * 1000.0
    }

    @Test
    fun testLocalToLocal() {
        val date = Date(2021, 4, 14)
        val hourMilli = HourMilli(1, 1, 1, 0)

        val localReference = ExactTimeStamp.Local(date, hourMilli)
        assertEquals(date, localReference.date)
        assertEquals(hourMilli, localReference.hourMilli)
        assertEquals(localOffset, localReference.offset, 0.1)

        val localSame = ExactTimeStamp.Local(date, hourMilli)
        assertEquals(0, localReference.compareTo(localSame))

        val localEarlier = ExactTimeStamp.Local(date, HourMilli(1, 1, 0, 0))
        assertEquals(1, localReference.compareTo(localEarlier))

        val localLater = ExactTimeStamp.Local(date, HourMilli(1, 1, 2, 0))
        assertEquals(-1, localReference.compareTo(localLater))
    }
}