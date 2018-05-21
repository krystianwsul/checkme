package com.krystianwsul.checkme.utils

import com.krystianwsul.checkme.utils.time.Date
import com.krystianwsul.checkme.utils.time.DayOfWeek

import junit.framework.Assert

import org.junit.Test

class UtilsTest {

    @Test
    fun testGetDateInMonth_2016_09_1_true() {
        Assert.assertTrue(Utils.getDateInMonth(2016, 9, 1, true) == Date(2016, 9, 1))
    }

    @Test
    fun testGetDateInMonth_2016_09_30_true() {
        Assert.assertTrue(Utils.getDateInMonth(2016, 9, 30, true) == Date(2016, 9, 30))
    }


    @Test
    fun testGetDateInMonth_2016_09_1_false() {
        Assert.assertTrue(Utils.getDateInMonth(2016, 9, 1, false) == Date(2016, 9, 30))
    }

    @Test
    fun testGetDateInMonth_2016_09_30_false() {
        Assert.assertTrue(Utils.getDateInMonth(2016, 9, 30, false) == Date(2016, 9, 1))
    }

    @Test
    fun testGetDateInMonth_2016_10_1_true() {
        Assert.assertTrue(Utils.getDateInMonth(2016, 10, 1, true) == Date(2016, 10, 1))
    }

    @Test
    fun testGetDateInMonth_2016_10_31_true() {
        Assert.assertTrue(Utils.getDateInMonth(2016, 10, 31, true) == Date(2016, 10, 31))
    }

    @Test
    fun testGetDateInMonth_2016_10_1_false() {
        Assert.assertTrue(Utils.getDateInMonth(2016, 10, 1, false) == Date(2016, 10, 31))
    }

    @Test
    fun testGetDateInMonth_2016_10_31_false() {
        Assert.assertTrue(Utils.getDateInMonth(2016, 10, 31, false) == Date(2016, 10, 1))
    }

    @Test
    fun testGetDateInMonth_2016_09_1_Thursday_true() {
        Assert.assertTrue(Utils.getDateInMonth(2016, 9, 1, DayOfWeek.THURSDAY, true) == Date(2016, 9, 1))
    }

    @Test
    fun testGetDateInMonth_2016_09_1_Friday_true() {
        Assert.assertTrue(Utils.getDateInMonth(2016, 9, 1, DayOfWeek.FRIDAY, true) == Date(2016, 9, 2))
    }

    @Test
    fun testGetDateInMonth_2016_09_1_Tuesday_true() {
        Assert.assertTrue(Utils.getDateInMonth(2016, 9, 1, DayOfWeek.TUESDAY, true) == Date(2016, 9, 6))
    }

    @Test
    fun testGetDateInMonth_2016_09_1_Wednesday_true() {
        Assert.assertTrue(Utils.getDateInMonth(2016, 9, 1, DayOfWeek.WEDNESDAY, true) == Date(2016, 9, 7))
    }

    @Test
    fun testGetDateInMonth_2016_09_2_Thursday_true() {
        Assert.assertTrue(Utils.getDateInMonth(2016, 9, 2, DayOfWeek.THURSDAY, true) == Date(2016, 9, 8))
    }

    @Test
    fun testGetDateInMonth_2016_09_2_Friday_true() {
        Assert.assertTrue(Utils.getDateInMonth(2016, 9, 2, DayOfWeek.FRIDAY, true) == Date(2016, 9, 9))
    }

    @Test
    fun testGetDateInMonth_2016_09_1_Friday_false() {
        Assert.assertTrue(Utils.getDateInMonth(2016, 9, 1, DayOfWeek.FRIDAY, false) == Date(2016, 9, 30))
    }

    @Test
    fun testGetDateInMonth_2016_09_1_Thursday_false() {
        Assert.assertTrue(Utils.getDateInMonth(2016, 9, 1, DayOfWeek.THURSDAY, false) == Date(2016, 9, 29))
    }

    @Test
    fun testGetDateInMonth_2016_09_1_Sunday_false() {
        Assert.assertTrue(Utils.getDateInMonth(2016, 9, 1, DayOfWeek.SUNDAY, false) == Date(2016, 9, 25))
    }

    @Test
    fun testGetDateInMonth_2016_09_1_Saturday_false() {
        Assert.assertTrue(Utils.getDateInMonth(2016, 9, 1, DayOfWeek.SATURDAY, false) == Date(2016, 9, 24))
    }

    @Test
    fun testGetDateInMonth_2016_09_2_Friday_false() {
        Assert.assertTrue(Utils.getDateInMonth(2016, 9, 2, DayOfWeek.FRIDAY, false) == Date(2016, 9, 23))
    }

    @Test
    fun testGetDateInMonth_2016_09_2_Thursday_false() {
        Assert.assertTrue(Utils.getDateInMonth(2016, 9, 2, DayOfWeek.THURSDAY, false) == Date(2016, 9, 22))
    }

    @Test
    fun testGetDateInMonth_2016_10_1_Saturday_true() {
        Assert.assertTrue(Utils.getDateInMonth(2016, 10, 1, DayOfWeek.SATURDAY, true) == Date(2016, 10, 1))
    }

    @Test
    fun testGetDateInMonth_2016_10_1_Sunday_true() {
        Assert.assertTrue(Utils.getDateInMonth(2016, 10, 1, DayOfWeek.SUNDAY, true) == Date(2016, 10, 2))
    }

    @Test
    fun testGetDateInMonth_2016_10_1_Thursday_true() {
        Assert.assertTrue(Utils.getDateInMonth(2016, 10, 1, DayOfWeek.THURSDAY, true) == Date(2016, 10, 6))
    }

    @Test
    fun testGetDateInMonth_2016_10_1_Friday_true() {
        Assert.assertTrue(Utils.getDateInMonth(2016, 10, 1, DayOfWeek.FRIDAY, true) == Date(2016, 10, 7))
    }

    @Test
    fun testGetDateInMonth_2016_10_2_Saturday_true() {
        Assert.assertTrue(Utils.getDateInMonth(2016, 10, 2, DayOfWeek.SATURDAY, true) == Date(2016, 10, 8))
    }

    @Test
    fun testGetDateInMonth_2016_10_2_Sunday_true() {
        Assert.assertTrue(Utils.getDateInMonth(2016, 10, 2, DayOfWeek.SUNDAY, true) == Date(2016, 10, 9))
    }

    @Test
    fun testGetDateInMonth_2016_10_1_Monday_false() {
        Assert.assertTrue(Utils.getDateInMonth(2016, 10, 1, DayOfWeek.MONDAY, false) == Date(2016, 10, 31))
    }

    @Test
    fun testGetDateInMonth_2016_10_1_Sunday_false() {
        Assert.assertTrue(Utils.getDateInMonth(2016, 10, 1, DayOfWeek.SUNDAY, false) == Date(2016, 10, 30))
    }

    @Test
    fun testGetDateInMonth_2016_10_1_Wednesday_false() {
        Assert.assertTrue(Utils.getDateInMonth(2016, 10, 1, DayOfWeek.WEDNESDAY, false) == Date(2016, 10, 26))
    }

    @Test
    fun testGetDateInMonth_2016_10_1_Tuesday_false() {
        Assert.assertTrue(Utils.getDateInMonth(2016, 10, 1, DayOfWeek.TUESDAY, false) == Date(2016, 10, 25))
    }

    @Test
    fun testGetDateInMonth_2016_10_2_Monday_false() {
        Assert.assertTrue(Utils.getDateInMonth(2016, 10, 2, DayOfWeek.MONDAY, false) == Date(2016, 10, 24))
    }

    @Test
    fun testGetDateInMonth_2016_10_2_Sunday_false() {
        Assert.assertTrue(Utils.getDateInMonth(2016, 10, 2, DayOfWeek.SUNDAY, false) == Date(2016, 10, 23))
    }
}