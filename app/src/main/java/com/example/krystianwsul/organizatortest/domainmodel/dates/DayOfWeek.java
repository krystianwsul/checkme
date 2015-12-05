package com.example.krystianwsul.organizatortest.domainmodel.dates;

import junit.framework.Assert;

import java.text.DateFormatSymbols;
import java.util.Calendar;

public enum DayOfWeek {
    SUNDAY,
    MONDAY,
    TUESDAY,
    WEDNESDAY,
    THURSDAY,
    FRIDAY,
    SATURDAY;

    public static DayOfWeek getDayFromCalendar(Calendar calendar) {
        Assert.assertTrue(calendar != null);

        int day = calendar.get(Calendar.DAY_OF_WEEK);
        return values()[day-1];
    }

    public String toString() {
        return DateFormatSymbols.getInstance().getWeekdays()[this.ordinal() + 1];
    }

    public static DayOfWeek today() {
        return Date.today().getDayOfWeek();
    }
}
