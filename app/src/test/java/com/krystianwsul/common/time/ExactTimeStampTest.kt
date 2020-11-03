package com.krystianwsul.common.time

import org.junit.Assert.assertEquals
import org.junit.Test

class ExactTimeStampTest {

    companion object {

        private const val long = 1589803853323
    }

    private fun getExactTimeStamp(offsetHours: Int?): String {
        val offset = offsetHours?.let { (60 * 60 * 1000 * it).toDouble() }
        val exactTimeStamp = ExactTimeStamp.fromOffset(long, offset)

        return exactTimeStamp.toString().substring(8, 13)
    }

    @Test
    fun testNoOffset() {
        assertEquals("14:10", getExactTimeStamp(null))
    }

    @Test
    fun test0() {
        assertEquals("12:10", getExactTimeStamp(0))
    }

    @Test
    fun test1() {
        assertEquals("13:10", getExactTimeStamp(1))
    }

    @Test
    fun test2() {
        assertEquals("14:10", getExactTimeStamp(2))
    }

    @Test
    fun test3() {
        assertEquals("15:10", getExactTimeStamp(3))
    }

    @Test
    fun test4() {
        assertEquals("16:10", getExactTimeStamp(4))
    }
}