package com.krystianwsul.checkme.utils.time

import com.krystianwsul.common.time.Date
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.time.HourMilli
import com.krystianwsul.common.time.HourMinute
import org.junit.Assert

import org.junit.Test

class HourMinuteTest {

    @Test
    fun testGetNextHourNormal() {
        val date = Date(2016, 1, 1)

        val now = ExactTimeStamp.Local(Date(2015, 1, 1), HourMilli(1, 5, 0, 0))

        val (first, second) = HourMinute.getNextHour(date, now)

        Assert.assertTrue(first == date)
        Assert.assertTrue(second == HourMinute(2, 0))
    }

    @Test
    fun testGetNextHourAfter23() {
        val date = Date(2016, 1, 1)

        val now = ExactTimeStamp.Local(Date(2015, 1, 1), HourMilli(23, 5, 0, 0))

        val (first, second) = HourMinute.getNextHour(date, now)

        Assert.assertTrue(first == Date(2016, 1, 2))
        Assert.assertTrue(second == HourMinute(0, 0))
    }
}