package com.krystianwsul.checkme.utils.time;

import android.support.annotation.NonNull;
import android.text.TextUtils;

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

    @NonNull
    public static DayOfWeek getDayFromCalendar(@NonNull Calendar calendar) {
        int day = calendar.get(Calendar.DAY_OF_WEEK);

        DayOfWeek dayOfWeek = values()[day - 1];
        Assert.assertTrue(dayOfWeek != null);

        return dayOfWeek;
    }

    @NonNull
    public String toString() {
        String weekDay = DateFormatSymbols.getInstance().getWeekdays()[this.ordinal() + 1];
        Assert.assertTrue(!TextUtils.isEmpty(weekDay));

        return weekDay;
    }
}
