package com.krystianwsul.common.time

import org.junit.Assert.assertEquals
import org.junit.Test

class DateTimeTest {

    companion object {

        private const val long = 1589803853323
    }

    private fun getDateTime(offsetHours: Int?): String {
        val offset = offsetHours?.let { (60 * 60 * 1000 * it).toDouble() }
        val dateTime = DateTime.fromOffset(long, offset)

        return dateTime.toString().substring(8, 13)
    }

    @Test
    fun testNoOffset() {
        assertEquals("2:10 ", getDateTime(null))
    }

    @Test
    fun test0() {
        assertEquals("12:10", getDateTime(0))
    }

    @Test
    fun test1() {
        assertEquals("1:10 ", getDateTime(1))
    }

    @Test
    fun test2() {
        assertEquals("2:10 ", getDateTime(2))
    }

    @Test
    fun test3() {
        assertEquals("3:10 ", getDateTime(3))
    }

    @Test
    fun test4() {
        assertEquals("4:10 ", getDateTime(4))
    }
}