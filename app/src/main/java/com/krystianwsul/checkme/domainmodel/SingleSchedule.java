package com.krystianwsul.checkme.domainmodel;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.krystianwsul.checkme.utils.time.Date;
import com.krystianwsul.checkme.utils.time.DateTime;
import com.krystianwsul.checkme.utils.time.ExactTimeStamp;
import com.krystianwsul.checkme.utils.time.NormalTime;
import com.krystianwsul.checkme.utils.time.Time;
import com.krystianwsul.checkme.utils.time.TimeStamp;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.List;

class SingleSchedule extends Schedule {
    @NonNull
    private final SingleScheduleBridge mSingleScheduleBridge;

    SingleSchedule(@NonNull DomainFactory domainFactory, @NonNull SingleScheduleBridge singleScheduleBridge) {
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
    MergedInstance getInstance(@NonNull Task task) {
        return mDomainFactory.getInstance(task, getDateTime());
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
    public List<MergedInstance> getInstances(@NonNull Task task, ExactTimeStamp givenStartExactTimeStamp, @NonNull ExactTimeStamp givenExactEndTimeStamp) {
        List<MergedInstance> instances = new ArrayList<>();

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
        Integer customTimeId = mSingleScheduleBridge.getCustomTimeId();
        if (customTimeId != null) {
            return mDomainFactory.getCustomTime(mSingleScheduleBridge.getCustomTimeId());
        } else {
            Integer hour = mSingleScheduleBridge.getHour();
            Integer minute = mSingleScheduleBridge.getMinute();
            Assert.assertTrue(hour != null);
            Assert.assertTrue(minute != null);
            return new NormalTime(hour, minute);
        }
    }

    @NonNull
    Date getDate() {
        return new Date(mSingleScheduleBridge.getYear(), mSingleScheduleBridge.getMonth(), mSingleScheduleBridge.getDay());
    }

    @NonNull
    private DateTime getDateTime() {
        return new DateTime(getDate(), getTime());
    }

    @Nullable
    @Override
    public Integer getCustomTimeId() {
        return mSingleScheduleBridge.getCustomTimeId();
    }

    @Override
    public boolean isVisible(@NonNull Task task, @NonNull ExactTimeStamp now) {
        Assert.assertTrue(current(now));

        return getInstance(task).isVisible(now);
    }
}
