package com.example.krystianwsul.organizatortest.domainmodel.times;

import com.example.krystianwsul.organizatortest.persistencemodel.PersistenceManger;
import com.example.krystianwsul.organizatortest.persistencemodel.TimeRecord;
import com.example.krystianwsul.organizatortest.domainmodel.dates.DayOfWeek;

import junit.framework.Assert;

import java.util.HashMap;

/**
 * Created by Krystian on 10/12/2015.
 */
public class CustomTime implements Time {
    private final TimeRecord mTimeRecord;

    private static HashMap<Integer, CustomTime> sCustomTimes = new HashMap<>();

    public static CustomTime getCustomTime(int timeRecordId) {
        if (sCustomTimes.containsKey(timeRecordId)) {
            return sCustomTimes.get(timeRecordId);
        } else {
            CustomTime customTime = new CustomTime(timeRecordId);
            sCustomTimes.put(timeRecordId, customTime);
            return customTime;
        }
    }

    private CustomTime(int timeRecordId) {
        mTimeRecord = PersistenceManger.getInstance().getTimeRecord(timeRecordId);
        Assert.assertTrue(mTimeRecord != null);
    }

    public String getName() {
        return mTimeRecord.getName();
    }

    public HourMinute getTimeByDay(DayOfWeek day) {
        switch (day) {
            case SUNDAY:
                if (mTimeRecord.getSundayHour() != null)
                    return new HourMinute(mTimeRecord.getSundayHour(), mTimeRecord.getSundayMinute());
                else
                    return null;
            case MONDAY:
                if (mTimeRecord.getMondayHour() != null)
                    return new HourMinute(mTimeRecord.getMondayHour(), mTimeRecord.getMondayMinute());
                else
                    return null;
            case TUESDAY:
                if (mTimeRecord.getTuesdayHour() != null)
                    return new HourMinute(mTimeRecord.getTuesdayHour(), mTimeRecord.getTuesdayMinute());
                else
                    return null;
            case WEDNESDAY:
                if (mTimeRecord.getWednesdayHour() != null)
                    return new HourMinute(mTimeRecord.getWednesdayHour(), mTimeRecord.getWednesdayMinute());
                else
                    return null;
            case THURSDAY:
                if (mTimeRecord.getThursdayHour() != null)
                    return new HourMinute(mTimeRecord.getThursdayHour(), mTimeRecord.getThursdayMinute());
                else
                    return null;
            case FRIDAY:
                if (mTimeRecord.getFridayHour() != null)
                    return new HourMinute(mTimeRecord.getFridayHour(), mTimeRecord.getFridayMinute());
                else
                    return null;
            case SATURDAY:
                if (mTimeRecord.getSaturdayHour() != null)
                    return new HourMinute(mTimeRecord.getSaturdayHour(), mTimeRecord.getSaturdayMinute());
                else
                    return null;
            default:
                throw new IllegalArgumentException("invalid day: " + day);
        }
    }

    public String toString() {
        return getName();
    }
}
