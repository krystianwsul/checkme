package com.example.krystianwsul.organizatortest.domainmodel.times;

import com.example.krystianwsul.organizatortest.domainmodel.dates.Date;
import com.example.krystianwsul.organizatortest.domainmodel.dates.DateTime;
import com.example.krystianwsul.organizatortest.domainmodel.dates.TimeStamp;

import junit.framework.Assert;

import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * Created by Krystian on 10/13/2015.
 */
public class HourMinute implements Comparable<HourMinute> {
    private Integer mHour;
    private Integer mMinute;

    public HourMinute(int hour, int minute) {
        mHour = hour;
        mMinute = minute;
    }

    public HourMinute(Calendar calendar) {
        mHour = calendar.get(Calendar.HOUR_OF_DAY);
        mMinute = calendar.get(Calendar.MINUTE);
    }

    public Integer getHour() {
        return mHour;
    }

    public Integer getMinute() {
        return mMinute;
    }

    public int compareTo(HourMinute hourMinute) {
        Assert.assertTrue(hourMinute != null);

        int comparisonHour = mHour.compareTo(hourMinute.getHour());

        if (comparisonHour != 0)
            return comparisonHour;

        return mMinute.compareTo(hourMinute.getMinute());
    }

    public boolean equals(HourMinute hourMinute) {
        Assert.assertTrue(hourMinute != null);
        return (compareTo(hourMinute) == 0);
    }

    public String toString() {
        Date date = TimeStamp.getNow().getDate();
        java.util.Date javaDate = new java.util.Date(date.getYear(), date.getMonth(), date.getDay(), mHour, mMinute);
        return SimpleDateFormat.getTimeInstance(SimpleDateFormat.SHORT).format(javaDate);
    }
}
