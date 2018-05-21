package com.krystianwsul.checkme.utils

import com.krystianwsul.checkme.utils.time.DayOfWeek
import junit.framework.Assert
import org.junit.Test
import java.util.*

class KotlinUtilsTest {

    @Test
    fun testEmpty() {
        val input = ArrayList<DayOfWeek>()
        val output = KotlinUtils.getRanges(input)
        val answer = ArrayList<List<DayOfWeek>>()
        Assert.assertTrue(output == answer)
    }

    @Test
    fun testSingle() {
        Assert.assertTrue(KotlinUtils.getRanges(listOf(DayOfWeek.WEDNESDAY)) == listOf(listOf(DayOfWeek.WEDNESDAY)))
    }

    @Test
    fun testDoubleAdjacent() {
        val input = Arrays.asList(DayOfWeek.TUESDAY, DayOfWeek.MONDAY)
        val output = KotlinUtils.getRanges(input)
        val answer = listOf(Arrays.asList(DayOfWeek.MONDAY, DayOfWeek.TUESDAY))
        Assert.assertTrue(output == answer)
    }

    @Test
    fun testDoubleNotAdjacent() {
        Assert.assertTrue(KotlinUtils.getRanges(Arrays.asList(DayOfWeek.SATURDAY, DayOfWeek.TUESDAY)) == Arrays.asList(listOf(DayOfWeek.TUESDAY), listOf(DayOfWeek.SATURDAY)))
    }

    @Test
    fun testMissingWednesday() {
        Assert.assertTrue(KotlinUtils.getRanges(Arrays.asList(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY, DayOfWeek.FRIDAY, DayOfWeek.MONDAY, DayOfWeek.THURSDAY, DayOfWeek.TUESDAY)) == Arrays.asList(Arrays.asList(DayOfWeek.SUNDAY, DayOfWeek.MONDAY, DayOfWeek.TUESDAY), Arrays.asList(DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY)))
    }
}
