package com.krystianwsul.checkme.domainmodel;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.krystianwsul.checkme.utils.InstanceKey;
import com.krystianwsul.checkme.utils.TaskKey;
import com.krystianwsul.checkme.utils.time.Date;
import com.krystianwsul.checkme.utils.time.DateTime;
import com.krystianwsul.checkme.utils.time.ExactTimeStamp;
import com.krystianwsul.checkme.utils.time.HourMinute;
import com.krystianwsul.checkme.utils.time.NormalTime;
import com.krystianwsul.checkme.utils.time.Time;
import com.krystianwsul.checkme.utils.time.TimePair;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;

public abstract class Instance {
    @NonNull
    protected final DomainFactory mDomainFactory;

    protected Instance(@NonNull DomainFactory domainFactory) {
        mDomainFactory = domainFactory;
    }

    @NonNull
    public InstanceKey getInstanceKey() {
        return new InstanceKey(getTaskKey(), getScheduleDate(), getScheduleCustomTimeId(), getScheduleHourMinute());
    }

    @NonNull
    protected abstract Date getScheduleDate();

    @Nullable
    public Integer getScheduleCustomTimeId() {
        Time scheduleTime = getScheduleTime();
        if (scheduleTime instanceof CustomTime)
            return ((CustomTime) scheduleTime).getId();
        else
            return null;
    }

    @Nullable
    public HourMinute getScheduleHourMinute() {
        Time scheduleTime = getScheduleTime();
        if (scheduleTime instanceof NormalTime)
            return ((NormalTime) scheduleTime).getHourMinute();
        else
            return null;
    }

    @NonNull
    protected abstract Time getScheduleTime();

    @NonNull
    public DateTime getScheduleDateTime() {
        return new DateTime(getScheduleDate(), getScheduleTime());
    }

    @NonNull
    public abstract TaskKey getTaskKey();

    @Nullable
    public abstract ExactTimeStamp getDone();

    public abstract boolean exists();

    @NonNull
    public DateTime getInstanceDateTime() {
        return new DateTime(getInstanceDate(), getInstanceTime());
    }

    @NonNull
    protected abstract Time getInstanceTime();

    @NonNull
    public List<Instance> getChildInstances(@NonNull ExactTimeStamp now) {
        ExactTimeStamp hierarchyExactTimeStamp = getHierarchyExactTimeStamp(now);

        Task task = getTask();

        DateTime scheduleDateTime = getScheduleDateTime();

        List<TaskHierarchy> taskHierarchies = mDomainFactory.getChildTaskHierarchies(task);
        HashSet<Instance> childInstances = new HashSet<>();
        for (TaskHierarchy taskHierarchy : taskHierarchies) {
            Assert.assertTrue(taskHierarchy != null);

            Task childTask = taskHierarchy.getChildTask();

            Instance existingChildInstance = mDomainFactory.getExistingInstance(childTask, scheduleDateTime);
            if (existingChildInstance != null) {
                childInstances.add(existingChildInstance);
            } else if (taskHierarchy.notDeleted(hierarchyExactTimeStamp) && taskHierarchy.getChildTask().notDeleted(hierarchyExactTimeStamp)) {
                childInstances.add(mDomainFactory.getInstance(childTask, scheduleDateTime));
            }
        }

        return new ArrayList<>(childInstances);
    }

    @NonNull
    protected abstract ExactTimeStamp getHierarchyExactTimeStamp(@NonNull ExactTimeStamp now);

    @NonNull
    public abstract String getName();

    public boolean isRootInstance(@NonNull ExactTimeStamp now) {
        return getTask().isRootTask(getHierarchyExactTimeStamp(now));
    }

    @NonNull
    public Task getTask() {
        return mDomainFactory.getTask(getTaskKey());
    }

    @NonNull
    public TimePair getInstanceTimePair() {
        return new TimePair(getInstanceCustomTimeId(), getInstanceHourMinute());
    }

    @Nullable
    protected Integer getInstanceCustomTimeId() {
        Time instanceTime = getInstanceTime();
        if (instanceTime instanceof CustomTime)
            return ((CustomTime) instanceTime).getId();
        else
            return null;
    }

    @Nullable
    protected HourMinute getInstanceHourMinute() {
        Time instanceTime = getInstanceTime();
        if (instanceTime instanceof NormalTime)
            return ((NormalTime) instanceTime).getHourMinute();
        else
            return null;
    }

    @NonNull
    public abstract Date getInstanceDate();

    @Nullable
    public String getDisplayText(@NonNull Context context, @NonNull ExactTimeStamp now) {
        if (isRootInstance(now)) {
            return getInstanceDateTime().getDisplayText(context);
        } else {
            return null;
        }
    }

    public abstract void setInstanceDateTime(@NonNull Date date, @NonNull TimePair timePair, @NonNull ExactTimeStamp now);

    public abstract void createInstanceHierarchy(@NonNull ExactTimeStamp now);

    public abstract void setNotificationShown(boolean notificationShown, @NonNull ExactTimeStamp now);

    public abstract void setDone(boolean done, @NonNull ExactTimeStamp now);

    public abstract void setNotified(@NonNull ExactTimeStamp now);

    public boolean isVisible(@NonNull ExactTimeStamp now) {
        boolean isVisible = isVisibleHelper(now);

        if (isVisible) {
            Task task = getTask();

            Date oldestVisible = task.getOldestVisible();

            // zone hack
            if (!(oldestVisible == null || oldestVisible.compareTo(getScheduleDateTime().getDate()) <= 0)) {
                Log.e("asdf", getName() + " oldest: " + oldestVisible + ", schedule: " + getScheduleDateTime() + ", instance: " + getInstanceDateTime() + ", exists: " + exists());
            }

            //Assert.assertTrue(oldestVisible == null || oldestVisible.compareTo(getScheduleDateTime().getTimeStamp().getDate()) <= 0 || exists());
        }

        return isVisible;
    }

    private boolean isVisibleHelper(@NonNull ExactTimeStamp now) {
        Calendar calendar = now.getCalendar();
        calendar.add(Calendar.DAY_OF_YEAR, -1); // 24 hack
        ExactTimeStamp twentyFourHoursAgo = new ExactTimeStamp(calendar);

        Instance parentInstance = getParentInstance(now);
        if (parentInstance == null) {
            ExactTimeStamp done = getDone();
            return (done == null || (done.compareTo(twentyFourHoursAgo) > 0));
        } else {
            return parentInstance.isVisible(now);
        }
    }

    public abstract boolean getNotified();

    /*
    I'm going to make some assumptions here:
        1. I won't live past a hundred years
        2. scheduleYear is between 2016 and 2088 (that way the algorithm should be fine during my lifetime)
        3. scheduleCustomTimeId is between 1 and 10,000
        4. hash looping past Integer.MAX_VALUE isn't likely to cause collisions
     */

    public int getNotificationId() {
        return getNotificationId(getScheduleDate(), getScheduleCustomTimeId(), getScheduleHourMinute(), getTaskKey());
    }

    public static int getNotificationId(@NonNull Date scheduleDate, @Nullable Integer scheduleCustomTimeId, @Nullable HourMinute scheduleHourMinute, @NonNull TaskKey taskKey) {
        Assert.assertTrue((scheduleCustomTimeId == null) != (scheduleHourMinute == null));

        int hash = scheduleDate.getMonth();
        hash += 12 * scheduleDate.getDay();
        hash += 12 * 31 * (scheduleDate.getYear() - 2015);

        if (scheduleCustomTimeId == null) {
            hash += 12 * 31 * 73 * (scheduleHourMinute.getHour() + 1);
            hash += 12 * 31 * 73 * 24 * (scheduleHourMinute.getMinute() + 1);
        } else {
            hash += 12 * 31 * 73 * 24 * 60 * scheduleCustomTimeId;
        }

        //noinspection NumericOverflow
        hash += 12 * 31 * 73 * 24 * 60 * 10000 * taskKey.hashCode();

        return hash;
    }

    @Nullable
    public Instance getParentInstance(@NonNull ExactTimeStamp now) {
        ExactTimeStamp hierarchyExactTimeStamp = getHierarchyExactTimeStamp(now);

        Task task = getTask();

        Task parentTask = task.getParentTask(hierarchyExactTimeStamp);

        if (parentTask == null)
            return null;

        Assert.assertTrue(parentTask.current(hierarchyExactTimeStamp));

        return mDomainFactory.getInstance(parentTask, getScheduleDateTime());
    }

    @NonNull
    public TimePair getScheduleTimePair() {
        return new TimePair(getScheduleCustomTimeId(), getScheduleHourMinute());
    }

    public abstract void setRelevant();

    public abstract boolean getNotificationShown();

    @Override
    public String toString() {
        return super.toString() + " " + getName() + " " + getScheduleDateTime().toString() + " " + getInstanceDateTime().toString();
    }
}
