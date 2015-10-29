package com.example.krystianwsul.organizatortest.domainmodel.schedules;

import android.content.Context;

import com.example.krystianwsul.organizatortest.persistencemodel.PersistenceManger;
import com.example.krystianwsul.organizatortest.persistencemodel.SingleScheduleRecord;
import com.example.krystianwsul.organizatortest.domainmodel.dates.Date;
import com.example.krystianwsul.organizatortest.domainmodel.dates.DateTime;
import com.example.krystianwsul.organizatortest.domainmodel.dates.TimeStamp;
import com.example.krystianwsul.organizatortest.domainmodel.times.Time;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by Krystian on 10/17/2015.
 */
public abstract class SingleSchedule implements Schedule {
    protected final SingleScheduleRecord mSingleScheduleRecord;

    private final static HashMap<Integer, SingleSchedule> sSingleSchedules = new HashMap<>();

    public static SingleSchedule getSingleSchedule(int singleScheduleId) {
        if (sSingleSchedules.containsKey(singleScheduleId)) {
            return sSingleSchedules.get(singleScheduleId);
        } else {
            SingleSchedule singleSchedule = createSingleSchedule(singleScheduleId);
            sSingleSchedules.put(singleScheduleId, singleSchedule);
            return singleSchedule;
        }
    }

    private static SingleSchedule createSingleSchedule(int singleScheduleId) {
        SingleScheduleRecord singleScheduleRecord = PersistenceManger.getInstance().getSingleScheduleRecord(singleScheduleId);
        if (singleScheduleRecord.getTimeRecordId() == null)
            return new SingleNormalSchedule(singleScheduleId);
        else
            return new SingleCustomSchedule(singleScheduleId);
    }

    protected SingleSchedule(int singleScheduleRecordId) {
        mSingleScheduleRecord = PersistenceManger.getInstance().getSingleScheduleRecord(singleScheduleRecordId);
        Assert.assertTrue(mSingleScheduleRecord != null);
    }

    private DateTime getDateTime() {
        return new DateTime(getDate(), getTime());
    }

    private Date getDate() {
        return new Date(mSingleScheduleRecord.getYear(), mSingleScheduleRecord.getMonth(), mSingleScheduleRecord.getDay());
    }

    protected abstract Time getTime();

    public ArrayList<DateTime> getDateTimes(TimeStamp givenStartTimeStamp, TimeStamp givenEndTimeStamp) {
        Assert.assertTrue(givenEndTimeStamp != null);

        ArrayList<DateTime> dateTimes = new ArrayList<>();

        DateTime dateTime = getDateTime();

        TimeStamp timeStamp = new TimeStamp(dateTime.getDate(), dateTime.getTime().getTimeByDay(dateTime.getDate().getDayOfWeek()));

        if (givenStartTimeStamp != null && (givenStartTimeStamp.compareTo(timeStamp) > 0))
            return dateTimes;

        if (givenEndTimeStamp.compareTo(timeStamp) < 0)
            return dateTimes;

        dateTimes.add(dateTime);

        return dateTimes;
    }

    public String getTaskText(Context context) {
        return getDateTime().getDisplayText(context);
    }
}
