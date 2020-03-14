package com.krystianwsul.checkme.utils

import com.krystianwsul.common.time.DayOfWeek
import org.junit.Assert
import org.junit.Test
import java.util.*

class KotlinUtilsTest {

    @Test
    fun testEmpty() {
        val input = ArrayList<DayOfWeek>()
        val output = getRanges(input)
        val answer = ArrayList<List<DayOfWeek>>()
        Assert.assertTrue(output == answer)
    }

    @Test
    fun testSingle() {
        Assert.assertTrue(getRanges(listOf(DayOfWeek.WEDNESDAY)) == listOf(listOf(DayOfWeek.WEDNESDAY)))
    }

    @Test
    fun testDoubleAdjacent() {
        val input = listOf(DayOfWeek.TUESDAY, DayOfWeek.MONDAY)
        val output = getRanges(input)
        val answer = listOf(listOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY))
        Assert.assertTrue(output == answer)
    }

    @Test
    fun testDoubleNotAdjacent() {
        Assert.assertTrue(getRanges(listOf(DayOfWeek.SATURDAY, DayOfWeek.TUESDAY)) == listOf(listOf(DayOfWeek.TUESDAY), listOf(DayOfWeek.SATURDAY)))
    }

    @Test
    fun testMissingWednesday() {
        Assert.assertTrue(getRanges(listOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY, DayOfWeek.FRIDAY, DayOfWeek.MONDAY, DayOfWeek.THURSDAY, DayOfWeek.TUESDAY)) == listOf(listOf(DayOfWeek.SUNDAY, DayOfWeek.MONDAY, DayOfWeek.TUESDAY), listOf(DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY)))
    }
}
