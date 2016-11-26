package com.krystianwsul.checkme.utils.time;

import android.support.v4.util.Pair;

import junit.framework.Assert;

import org.junit.Test;

public class HourMinuteTest {
    @Test
    public void testGetNextHourNormal() {
        Date date = new Date(2016, 1, 1);

        ExactTimeStamp now = new ExactTimeStamp(new Date(2015, 1, 1), new HourMilli(1, 5, 0, 0));

        Pair<Date, HourMinute> result = HourMinute.getNextHour(date, now);

        Assert.assertTrue(result.first.equals(date));
        Assert.assertTrue(result.second.equals(new HourMinute(2, 0)));
    }

    @Test
    public void testGetNextHourAfter23() {
        Date date = new Date(2016, 1, 1);

        ExactTimeStamp now = new ExactTimeStamp(new Date(2015, 1, 1), new HourMilli(23, 5, 0, 0));

        Pair<Date, HourMinute> result = HourMinute.getNextHour(date, now);

        Assert.assertTrue(result.first.equals(new Date(2016, 1, 2)));
        Assert.assertTrue(result.second.equals(new HourMinute(0, 0)));
    }
}