package com.krystianwsul.common.time

import org.junit.Assert.assertEquals
import org.junit.Test

class DateTest {

    @Test
    fun testToJson() {
        assertEquals("2020-11-15", Date(2020, 11, 15).toJson())
    }

    @Test
    fun testToString() {
        assertEquals("11/15/20", Date(2020, 11, 15).toString())
    }
}