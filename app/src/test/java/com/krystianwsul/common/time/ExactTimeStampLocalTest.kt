package com.krystianwsul.common.time

import org.junit.Assert.assertEquals
import org.junit.Test

class ExactTimeStampLocalTest {

    companion object {

        private fun getExactTimeStampHourString(): String {
            val exactTimeStamp = getLocalExactTimeStamp()

            return exactTimeStamp.getHourString()
        }
    }

    @Test
    fun testNoOffset() {
        assertEquals("14:10", getExactTimeStampHourString())
    }

    @Test
    fun testDateTimeSoyLocal() {
        val exactTimeStamp = getLocalExactTimeStamp()
        assertEquals("14:10", exactTimeStamp.getHourString())

        val dateTimeSoy = exactTimeStamp.toDateTimeSoy()
        assertEquals("12:10", dateTimeSoy.format("HH:mm"))
    }

    @Test
    fun testDateTimeTzLocal() {
        val exactTimeStamp = getLocalExactTimeStamp()
        assertEquals("14:10", exactTimeStamp.getHourString())

        val dateTimeTz = exactTimeStamp.toDateTimeTz()
        assertEquals("2:10 PM", dateTimeTz.formatTime())
    }
}