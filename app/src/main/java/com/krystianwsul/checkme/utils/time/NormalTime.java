package com.krystianwsul.checkme.utils.time;

import android.support.annotation.NonNull;

public class NormalTime implements Time {
    @NonNull
    private final HourMinute mHourMinute;

    public NormalTime(@NonNull HourMinute hourMinute) {
        mHourMinute = hourMinute;
    }

    public NormalTime(int hour, int minute) {
        mHourMinute = new HourMinute(hour, minute);
    }

    @NonNull
    public HourMinute getHourMinute(@NonNull DayOfWeek dayOfWeek) {
        return mHourMinute;
    }

    @NonNull
    public HourMinute getHourMinute() {
        return mHourMinute;
    }

    @NonNull
    public String toString() {
        return mHourMinute.toString();
    }

    @NonNull
    @Override
    public TimePair getTimePair() {
        return new TimePair(null, mHourMinute);
    }
}
