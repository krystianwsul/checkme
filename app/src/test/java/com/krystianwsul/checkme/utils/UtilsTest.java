package com.krystianwsul.checkme.utils;

import com.krystianwsul.checkme.utils.time.Date;
import com.krystianwsul.checkme.utils.time.DayOfWeek;

import junit.framework.Assert;

import org.junit.Test;

public class UtilsTest {
    @Test
    public void testGetDateInMonth_2016_09_1_true() {
        Assert.assertTrue(Utils.INSTANCE.getDateInMonth(2016, 9, 1, true).equals(new Date(2016, 9, 1)));
    }

    @Test
    public void testGetDateInMonth_2016_09_30_true() {
        Assert.assertTrue(Utils.INSTANCE.getDateInMonth(2016, 9, 30, true).equals(new Date(2016, 9, 30)));
    }


    @Test
    public void testGetDateInMonth_2016_09_1_false() {
        Assert.assertTrue(Utils.INSTANCE.getDateInMonth(2016, 9, 1, false).equals(new Date(2016, 9, 30)));
    }

    @Test
    public void testGetDateInMonth_2016_09_30_false() {
        Assert.assertTrue(Utils.INSTANCE.getDateInMonth(2016, 9, 30, false).equals(new Date(2016, 9, 1)));
    }

    @Test
    public void testGetDateInMonth_2016_10_1_true() {
        Assert.assertTrue(Utils.INSTANCE.getDateInMonth(2016, 10, 1, true).equals(new Date(2016, 10, 1)));
    }

    @Test
    public void testGetDateInMonth_2016_10_31_true() {
        Assert.assertTrue(Utils.INSTANCE.getDateInMonth(2016, 10, 31, true).equals(new Date(2016, 10, 31)));
    }

    @Test
    public void testGetDateInMonth_2016_10_1_false() {
        Assert.assertTrue(Utils.INSTANCE.getDateInMonth(2016, 10, 1, false).equals(new Date(2016, 10, 31)));
    }

    @Test
    public void testGetDateInMonth_2016_10_31_false() {
        Assert.assertTrue(Utils.INSTANCE.getDateInMonth(2016, 10, 31, false).equals(new Date(2016, 10, 1)));
    }

    @Test
    public void testGetDateInMonth_2016_09_1_Thursday_true() {
        Assert.assertTrue(Utils.INSTANCE.getDateInMonth(2016, 9, 1, DayOfWeek.THURSDAY, true).equals(new Date(2016, 9, 1)));
    }

    @Test
    public void testGetDateInMonth_2016_09_1_Friday_true() {
        Assert.assertTrue(Utils.INSTANCE.getDateInMonth(2016, 9, 1, DayOfWeek.FRIDAY, true).equals(new Date(2016, 9, 2)));
    }

    @Test
    public void testGetDateInMonth_2016_09_1_Tuesday_true() {
        Assert.assertTrue(Utils.INSTANCE.getDateInMonth(2016, 9, 1, DayOfWeek.TUESDAY, true).equals(new Date(2016, 9, 6)));
    }

    @Test
    public void testGetDateInMonth_2016_09_1_Wednesday_true() {
        Assert.assertTrue(Utils.INSTANCE.getDateInMonth(2016, 9, 1, DayOfWeek.WEDNESDAY, true).equals(new Date(2016, 9, 7)));
    }

    @Test
    public void testGetDateInMonth_2016_09_2_Thursday_true() {
        Assert.assertTrue(Utils.INSTANCE.getDateInMonth(2016, 9, 2, DayOfWeek.THURSDAY, true).equals(new Date(2016, 9, 8)));
    }

    @Test
    public void testGetDateInMonth_2016_09_2_Friday_true() {
        Assert.assertTrue(Utils.INSTANCE.getDateInMonth(2016, 9, 2, DayOfWeek.FRIDAY, true).equals(new Date(2016, 9, 9)));
    }

    @Test
    public void testGetDateInMonth_2016_09_1_Friday_false() {
        Assert.assertTrue(Utils.INSTANCE.getDateInMonth(2016, 9, 1, DayOfWeek.FRIDAY, false).equals(new Date(2016, 9, 30)));
    }

    @Test
    public void testGetDateInMonth_2016_09_1_Thursday_false() {
        Assert.assertTrue(Utils.INSTANCE.getDateInMonth(2016, 9, 1, DayOfWeek.THURSDAY, false).equals(new Date(2016, 9, 29)));
    }

    @Test
    public void testGetDateInMonth_2016_09_1_Sunday_false() {
        Assert.assertTrue(Utils.INSTANCE.getDateInMonth(2016, 9, 1, DayOfWeek.SUNDAY, false).equals(new Date(2016, 9, 25)));
    }

    @Test
    public void testGetDateInMonth_2016_09_1_Saturday_false() {
        Assert.assertTrue(Utils.INSTANCE.getDateInMonth(2016, 9, 1, DayOfWeek.SATURDAY, false).equals(new Date(2016, 9, 24)));
    }

    @Test
    public void testGetDateInMonth_2016_09_2_Friday_false() {
        Assert.assertTrue(Utils.INSTANCE.getDateInMonth(2016, 9, 2, DayOfWeek.FRIDAY, false).equals(new Date(2016, 9, 23)));
    }

    @Test
    public void testGetDateInMonth_2016_09_2_Thursday_false() {
        Assert.assertTrue(Utils.INSTANCE.getDateInMonth(2016, 9, 2, DayOfWeek.THURSDAY, false).equals(new Date(2016, 9, 22)));
    }

    @Test
    public void testGetDateInMonth_2016_10_1_Saturday_true() {
        Assert.assertTrue(Utils.INSTANCE.getDateInMonth(2016, 10, 1, DayOfWeek.SATURDAY, true).equals(new Date(2016, 10, 1)));
    }

    @Test
    public void testGetDateInMonth_2016_10_1_Sunday_true() {
        Assert.assertTrue(Utils.INSTANCE.getDateInMonth(2016, 10, 1, DayOfWeek.SUNDAY, true).equals(new Date(2016, 10, 2)));
    }

    @Test
    public void testGetDateInMonth_2016_10_1_Thursday_true() {
        Assert.assertTrue(Utils.INSTANCE.getDateInMonth(2016, 10, 1, DayOfWeek.THURSDAY, true).equals(new Date(2016, 10, 6)));
    }

    @Test
    public void testGetDateInMonth_2016_10_1_Friday_true() {
        Assert.assertTrue(Utils.INSTANCE.getDateInMonth(2016, 10, 1, DayOfWeek.FRIDAY, true).equals(new Date(2016, 10, 7)));
    }

    @Test
    public void testGetDateInMonth_2016_10_2_Saturday_true() {
        Assert.assertTrue(Utils.INSTANCE.getDateInMonth(2016, 10, 2, DayOfWeek.SATURDAY, true).equals(new Date(2016, 10, 8)));
    }

    @Test
    public void testGetDateInMonth_2016_10_2_Sunday_true() {
        Assert.assertTrue(Utils.INSTANCE.getDateInMonth(2016, 10, 2, DayOfWeek.SUNDAY, true).equals(new Date(2016, 10, 9)));
    }

    @Test
    public void testGetDateInMonth_2016_10_1_Monday_false() {
        Assert.assertTrue(Utils.INSTANCE.getDateInMonth(2016, 10, 1, DayOfWeek.MONDAY, false).equals(new Date(2016, 10, 31)));
    }

    @Test
    public void testGetDateInMonth_2016_10_1_Sunday_false() {
        Assert.assertTrue(Utils.INSTANCE.getDateInMonth(2016, 10, 1, DayOfWeek.SUNDAY, false).equals(new Date(2016, 10, 30)));
    }

    @Test
    public void testGetDateInMonth_2016_10_1_Wednesday_false() {
        Assert.assertTrue(Utils.INSTANCE.getDateInMonth(2016, 10, 1, DayOfWeek.WEDNESDAY, false).equals(new Date(2016, 10, 26)));
    }

    @Test
    public void testGetDateInMonth_2016_10_1_Tuesday_false() {
        Assert.assertTrue(Utils.INSTANCE.getDateInMonth(2016, 10, 1, DayOfWeek.TUESDAY, false).equals(new Date(2016, 10, 25)));
    }

    @Test
    public void testGetDateInMonth_2016_10_2_Monday_false() {
        Assert.assertTrue(Utils.INSTANCE.getDateInMonth(2016, 10, 2, DayOfWeek.MONDAY, false).equals(new Date(2016, 10, 24)));
    }

    @Test
    public void testGetDateInMonth_2016_10_2_Sunday_false() {
        Assert.assertTrue(Utils.INSTANCE.getDateInMonth(2016, 10, 2, DayOfWeek.SUNDAY, false).equals(new Date(2016, 10, 23)));
    }
}