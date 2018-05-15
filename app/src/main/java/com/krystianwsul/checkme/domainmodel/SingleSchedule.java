package com.krystianwsul.checkme.domainmodel;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.krystianwsul.checkme.utils.CustomTimeKey;
import com.krystianwsul.checkme.utils.InstanceKey;
import com.krystianwsul.checkme.utils.ScheduleType;
import com.krystianwsul.checkme.utils.time.Date;
import com.krystianwsul.checkme.utils.time.DateTime;
import com.krystianwsul.checkme.utils.time.ExactTimeStamp;
import com.krystianwsul.checkme.utils.time.HourMinute;
import com.krystianwsul.checkme.utils.time.NormalTime;
import com.krystianwsul.checkme.utils.time.Time;
import com.krystianwsul.checkme.utils.time.TimePair;
import com.krystianwsul.checkme.utils.time.TimeStamp;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.List;

public class SingleSchedule extends Schedule {
    @NonNull
    private final SingleScheduleBridge mSingleScheduleBridge;

    public SingleSchedule(@NonNull DomainFactory domainFactory, @NonNull SingleScheduleBridge singleScheduleBridge) {
        super(domainFactory);

        mSingleScheduleBridge = singleScheduleBridge;
    }

    @NonNull
    @Override
    protected ScheduleBridge getScheduleBridge() {
        return mSingleScheduleBridge;
    }

    @NonNull
    @Override
    public String getScheduleText(@NonNull Context context) {
        return getDateTime().getDisplayText(context);
    }

    @NonNull
    private Instance getInstance(@NonNull Task task) {
        InstanceKey instanceKey = new InstanceKey(task.getTaskKey(), getDate(), getTimePair());

        return getDomainFactory().getInstance(instanceKey);
    }

    @Nullable
    @Override
    public TimeStamp getNextAlarm(@NonNull ExactTimeStamp now) {
        TimeStamp timeStamp = getDateTime().getTimeStamp();
        if (timeStamp.toExactTimeStamp().compareTo(now) > 0)
            return timeStamp;
        else
            return null;
    }

    @NonNull
    @Override
    public List<Instance> getInstances(@NonNull Task task, ExactTimeStamp givenStartExactTimeStamp, @NonNull ExactTimeStamp givenExactEndTimeStamp) {
        List<Instance> instances = new ArrayList<>();

        ExactTimeStamp singleScheduleExactTimeStamp = getDateTime().getTimeStamp().toExactTimeStamp();

        if (givenStartExactTimeStamp != null && givenStartExactTimeStamp.compareTo(singleScheduleExactTimeStamp) > 0)
            return instances;

        if (givenExactEndTimeStamp.compareTo(singleScheduleExactTimeStamp) <= 0)
            return instances;

        ExactTimeStamp endExactTimeStamp = getEndExactTimeStamp();
        if (endExactTimeStamp != null && singleScheduleExactTimeStamp.compareTo(endExactTimeStamp) >= 0) // timezone hack
            return instances;

        instances.add(getInstance(task));

        return instances;
    }

    @NonNull
    private Time getTime() {
        CustomTimeKey customTimeKey = mSingleScheduleBridge.getCustomTimeKey();
        if (customTimeKey != null) {
            return getDomainFactory().getCustomTime(customTimeKey);
        } else {
            Integer hour = mSingleScheduleBridge.getHour();
            Integer minute = mSingleScheduleBridge.getMinute();
            Assert.assertTrue(hour != null);
            Assert.assertTrue(minute != null);
            return new NormalTime(hour, minute);
        }
    }

    @NonNull
    public TimePair getTimePair() {
        CustomTimeKey customTimeKey = mSingleScheduleBridge.getCustomTimeKey();
        Integer hour = mSingleScheduleBridge.getHour();
        Integer minute = mSingleScheduleBridge.getMinute();

        if (customTimeKey != null) {
            Assert.assertTrue(hour == null);
            Assert.assertTrue(minute == null);

            return new TimePair(customTimeKey);
        } else {
            Assert.assertTrue(hour != null);
            Assert.assertTrue(minute != null);

            return new TimePair(new HourMinute(hour, minute));
        }
    }

    @NonNull
    public Date getDate() {
        return new Date(mSingleScheduleBridge.getYear(), mSingleScheduleBridge.getMonth(), mSingleScheduleBridge.getDay());
    }

    @NonNull
    private DateTime getDateTime() {
        return new DateTime(getDate(), getTime());
    }

    @Nullable
    @Override
    public CustomTimeKey getCustomTimeKey() {
        return mSingleScheduleBridge.getCustomTimeKey();
    }

    @Override
    public boolean isVisible(@NonNull Task task, @NonNull ExactTimeStamp now) {
        Assert.assertTrue(current(now));

        return getInstance(task).isVisible(now);
    }

    @NonNull
    @Override
    public ScheduleType getScheduleType() {
        return ScheduleType.SINGLE;
    }
}
