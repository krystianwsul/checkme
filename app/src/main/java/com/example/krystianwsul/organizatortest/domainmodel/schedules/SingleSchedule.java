package com.example.krystianwsul.organizatortest.domainmodel.schedules;

import android.content.Context;

import com.example.krystianwsul.organizatortest.domainmodel.instances.Instance;
import com.example.krystianwsul.organizatortest.domainmodel.repetitions.SingleRepetitionFactory;
import com.example.krystianwsul.organizatortest.domainmodel.tasks.RootTask;
import com.example.krystianwsul.organizatortest.domainmodel.times.CustomTime;
import com.example.krystianwsul.organizatortest.domainmodel.times.CustomTimeFactory;
import com.example.krystianwsul.organizatortest.domainmodel.times.NormalTime;
import com.example.krystianwsul.organizatortest.persistencemodel.SingleScheduleRecord;
import com.example.krystianwsul.organizatortest.domainmodel.dates.Date;
import com.example.krystianwsul.organizatortest.domainmodel.dates.DateTime;
import com.example.krystianwsul.organizatortest.domainmodel.dates.TimeStamp;
import com.example.krystianwsul.organizatortest.domainmodel.times.Time;

import junit.framework.Assert;

import java.util.ArrayList;

/**
 * Created by Krystian on 10/17/2015.
 */
public class SingleSchedule extends Schedule {
    protected final SingleScheduleRecord mSingleScheduleRecord;

    protected SingleSchedule(SingleScheduleRecord singleScheduleRecord, RootTask rootTask) {
        super(rootTask);

        Assert.assertTrue(singleScheduleRecord != null);

        mSingleScheduleRecord = singleScheduleRecord;
    }

    public DateTime getDateTime() {
        return new DateTime(getDate(), getTime());
    }

    public Date getDate() {
        return new Date(mSingleScheduleRecord.getYear(), mSingleScheduleRecord.getMonth(), mSingleScheduleRecord.getDay());
    }

    public Time getTime() {
        Integer customTimeId = mSingleScheduleRecord.getCustomTimeId();
        if (customTimeId != null) {
            CustomTime customTime = CustomTimeFactory.getCustomTime(mSingleScheduleRecord.getCustomTimeId());
            Assert.assertTrue(customTime != null);
            return customTime;
        } else {
            Integer hour = mSingleScheduleRecord.getHour();
            Integer minute = mSingleScheduleRecord.getMinute();
            Assert.assertTrue(hour != null);
            Assert.assertTrue(minute != null);
            return new NormalTime(hour, minute);
        }
    }

    public ArrayList<Instance> getInstances(TimeStamp givenStartTimeStamp, TimeStamp givenEndTimeStamp) {
        Assert.assertTrue(givenEndTimeStamp != null);

        ArrayList<Instance> instances = new ArrayList<>();

        DateTime dateTime = getDateTime();

        TimeStamp timeStamp = new TimeStamp(dateTime.getDate(), dateTime.getTime().getHourMinute(dateTime.getDate().getDayOfWeek()));

        if (givenStartTimeStamp != null && (givenStartTimeStamp.compareTo(timeStamp) >= 0))
            return instances;

        if (givenEndTimeStamp.compareTo(timeStamp) < 0)
            return instances;

        instances.add(SingleRepetitionFactory.getInstance().getSingleRepetition(this).getInstance(mRootTask));

        return instances;
    }

    public String getTaskText(Context context) {
        return getDateTime().getDisplayText(context);
    }

    public TimeStamp getEndTimeStamp() {
        return null;
    }
}
