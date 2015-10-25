package com.example.krystianwsul.organizatortest.timing.schedules;

import com.example.krystianwsul.organizatortest.timing.DateTime;
import com.example.krystianwsul.organizatortest.timing.TimeStamp;

import junit.framework.Assert;

import java.util.ArrayList;

/**
 * Created by Krystian on 10/17/2015.
 */
public class SingleSchedule implements Schedule {
    private final DateTime mDateTime;

    public SingleSchedule(DateTime dateTime) {
        Assert.assertTrue(dateTime != null);
        mDateTime = dateTime;
    }

    public ArrayList<DateTime> getDateTimes(TimeStamp givenStartTimeStamp, TimeStamp givenEndTimeStamp) {
        Assert.assertTrue(givenEndTimeStamp != null);

        ArrayList<DateTime> dateTimes = new ArrayList<>();

        TimeStamp timeStamp = new TimeStamp(mDateTime.getDate(), mDateTime.getTime().getTimeByDay(mDateTime.getDate().getDayOfWeek()));

        if (givenStartTimeStamp != null && (givenStartTimeStamp.compareTo(timeStamp) > 0))
            return dateTimes;

        if (givenEndTimeStamp.compareTo(timeStamp) < 0)
            return dateTimes;

        dateTimes.add(mDateTime);

        return dateTimes;
    }
}
