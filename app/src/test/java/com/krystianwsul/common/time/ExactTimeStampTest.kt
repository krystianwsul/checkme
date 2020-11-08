package com.krystianwsul.common.time

import org.junit.Assert.assertEquals
import org.junit.Test

class ExactTimeStampTest {

    companion object {

        private const val long = 1589803853323

        private fun getExactTimeStamp(offsetHours: Int?): ExactTimeStamp.Offset {
            val offset = offsetHours?.let { (60 * 60 * 1000 * it).toDouble() }

            return ExactTimeStamp.Offset.fromOffset(long, offset)
        }

        private fun ExactTimeStamp.getHourString() = toString().substring(8, 13)

        private fun getExactTimeStampHourString(offsetHours: Int?): String {
            val exactTimeStamp = getExactTimeStamp(offsetHours)

            return exactTimeStamp.getHourString()
        }
    }

    @Test
    fun testNoOffset() {
        assertEquals("14:10", getExactTimeStampHourString(null))
    }

    @Test
    fun test0() {
        assertEquals("12:10", getExactTimeStampHourString(0))
    }

    @Test
    fun test1() {
        assertEquals("13:10", getExactTimeStampHourString(1))
    }

    @Test
    fun test2() {
        assertEquals("14:10", getExactTimeStampHourString(2))
    }

    @Test
    fun test3() {
        assertEquals("15:10", getExactTimeStampHourString(3))
    }

    @Test
    fun test4() {
        assertEquals("16:10", getExactTimeStampHourString(4))
    }

    @Test
    fun testDateTimeSoyLocal() {
        val exactTimeStamp = getExactTimeStamp(null)
        assertEquals("14:10", exactTimeStamp.getHourString())

        val dateTimeSoy = exactTimeStamp.toDateTimeSoy()
        assertEquals("12:10", dateTimeSoy.format("HH:mm"))
    }

    @Test
    fun testDateTimeTzLocal() {
        val exactTimeStamp = getExactTimeStamp(null)
        assertEquals("14:10", exactTimeStamp.getHourString())

        val dateTimeTz = exactTimeStamp.toDateTimeTz()
        assertEquals("2:10 PM", dateTimeTz.formatTime())
    }
}