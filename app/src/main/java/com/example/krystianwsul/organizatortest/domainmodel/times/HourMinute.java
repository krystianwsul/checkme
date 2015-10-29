package com.example.krystianwsul.organizatortest.domainmodel.times;

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
        int comparisonHour = mHour.compareTo(hourMinute.getHour());

        if (comparisonHour != 0)
            return comparisonHour;

        return mMinute.compareTo(hourMinute.getMinute());
    }

    public String toString() {
        return String.format("%02d", mHour) + ":" +String.format("%02d", mMinute);
    }
}
