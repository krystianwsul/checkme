package com.krystianwsul.common.time

import org.junit.Assert.assertEquals
import org.junit.Test

class ExactTimeStampConversionTest {

    @Test
    fun testNoOffset() {
        val localExactTimeStamp = getLocalExactTimeStamp()
        assertEquals("14:10", localExactTimeStamp.getHourString())

        val offsetExactTimeStamp = getOffsetExactTimeStamp(null)
        assertEquals("14:10", offsetExactTimeStamp.getHourString())

        assertEquals(offsetExactTimeStamp, localExactTimeStamp.toOffset(offsetExactTimeStamp))
    }

    @Test
    fun testSameOffset() {
        val localExactTimeStamp = getLocalExactTimeStamp()
        assertEquals("14:10", localExactTimeStamp.getHourString())

        val offsetExactTimeStamp = getOffsetExactTimeStamp(2)
        assertEquals("14:10", offsetExactTimeStamp.getHourString())

        assertEquals(offsetExactTimeStamp, localExactTimeStamp.toOffset(offsetExactTimeStamp))
    }

    @Test
    fun testOffset4() {
        val localExactTimeStamp = getLocalExactTimeStamp()
        assertEquals("14:10", localExactTimeStamp.getHourString())

        val offsetExactTimeStamp = getOffsetExactTimeStamp(4)
        assertEquals("16:10", offsetExactTimeStamp.getHourString())

        assertEquals(offsetExactTimeStamp, localExactTimeStamp.toOffset(offsetExactTimeStamp))
    }
}