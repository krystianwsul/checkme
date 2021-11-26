package com.krystianwsul.common.time

import org.junit.Assert.assertEquals
import org.junit.Test

class MonthEquivalenceTest {

    @Test
    fun testMonthOrdinal() {
        val date = Date(2021, 11, 26)
        val dateSoy = date.toDateSoy()

        assertEquals(date.month, dateSoy.month1)
    }

    @Test
    fun testConstructor() {
        val date = Date(2021, 11, 26)
        val dateSoy = DateSoy(2021, 11, 26)

        assertEquals(date.toDateSoy(), dateSoy)
    }
}