package com.krystianwsul.common.time

import com.soywiz.klock.days
import com.soywiz.klock.plus
import org.junit.Assert.assertEquals
import org.junit.Test

class DayOfWeekEquivalenceTest {

    @Test
    fun testDaysOfWeekEquivalent() {
        val startDate = Date(2021, 11, 26)
        var currentDateSoy = startDate.toDateSoy()

        repeat(7) {
            val currentDate = Date(currentDateSoy)

            assertEquals(currentDate.dayOfWeek.ordinal, currentDateSoy.dayOfWeekInt)
            assertEquals(currentDate.dayOfWeek.ordinal, currentDateSoy.dayOfWeek.ordinal)
            assertEquals(currentDate.dayOfWeek.toString().lowercase(), currentDateSoy.dayOfWeek.toString().lowercase())

            currentDateSoy += 1.days
        }
    }
}