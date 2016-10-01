package com.krystianwsul.checkme.domainmodel;

import android.content.Context;
import android.support.annotation.NonNull;

import com.krystianwsul.checkme.persistencemodel.ScheduleRecord;
import com.krystianwsul.checkme.persistencemodel.SingleScheduleRecord;
import com.krystianwsul.checkme.utils.time.Date;
import com.krystianwsul.checkme.utils.time.DateTime;
import com.krystianwsul.checkme.utils.time.ExactTimeStamp;
import com.krystianwsul.checkme.utils.time.NormalTime;
import com.krystianwsul.checkme.utils.time.Time;
import com.krystianwsul.checkme.utils.time.TimeStamp;

import junit.framework.Assert;

import java.util.ArrayList;

class SingleSchedule extends Schedule {
    private final SingleScheduleRecord mSingleScheduleRecord;

    SingleSchedule(@NonNull DomainFactory domainFactory, @NonNull ScheduleRecord scheduleRecord, @NonNull SingleScheduleRecord singleScheduleRecord) {
        super(domainFactory, scheduleRecord);

        mSingleScheduleRecord = singleScheduleRecord;
    }

    @Override
    String getTaskText(Context context) {
        Assert.assertTrue(mSingleScheduleRecord != null);

        Instance instance = getDomainFactory().getInstance(getRootTask(), getDateTime());

        return instance.getInstanceDateTime().getDisplayText(context);
    }

    Instance getInstance(Task task) {
        Assert.assertTrue(task != null);

        Instance instance = getDomainFactory().getInstance(task, getDateTime());
        Assert.assertTrue(instance != null);

        return instance;
    }

    @Override
    protected TimeStamp getNextAlarm(ExactTimeStamp now) {
        Assert.assertTrue(mSingleScheduleRecord != null);
        Assert.assertTrue(now != null);

        TimeStamp timeStamp = getDateTime().getTimeStamp();
        if (timeStamp.toExactTimeStamp().compareTo(now) > 0)
            return timeStamp;
        else
            return null;
    }

    @Override
    ArrayList<Instance> getInstances(Task task, ExactTimeStamp givenStartExactTimeStamp, ExactTimeStamp givenExactEndTimeStamp) {
        Assert.assertTrue(task != null);
        Assert.assertTrue(givenExactEndTimeStamp != null);

        ArrayList<Instance> instances = new ArrayList<>();

        ExactTimeStamp singleScheduleExactTimeStamp = getDateTime().getTimeStamp().toExactTimeStamp();

        if (givenStartExactTimeStamp != null && givenStartExactTimeStamp.compareTo(singleScheduleExactTimeStamp) > 0) {
            return instances;
        }

        if (givenExactEndTimeStamp.compareTo(singleScheduleExactTimeStamp) <= 0) {
            return instances;
        }

        instances.add(getInstance(task));

        return instances;
    }

    @NonNull
    public Time getTime() {
        Integer customTimeId = mSingleScheduleRecord.getCustomTimeId();
        if (customTimeId != null) {
            CustomTime customTime = getDomainFactory().getCustomTime(mSingleScheduleRecord.getCustomTimeId());
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

    @NonNull
    Date getDate() {
        return new Date(mSingleScheduleRecord.getYear(), mSingleScheduleRecord.getMonth(), mSingleScheduleRecord.getDay());
    }

    private DateTime getDateTime() {
        return new DateTime(getDate(), getTime());
    }

    @Override
    public Integer getCustomTimeId() {
        return mSingleScheduleRecord.getCustomTimeId();
    }
}
