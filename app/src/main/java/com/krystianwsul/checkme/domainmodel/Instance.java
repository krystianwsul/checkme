package com.krystianwsul.checkme.domainmodel;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.krystianwsul.checkme.utils.CustomTimeKey;
import com.krystianwsul.checkme.utils.InstanceKey;
import com.krystianwsul.checkme.utils.ScheduleKey;
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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class Instance {
    @NonNull
    protected final DomainFactory mDomainFactory;

    protected Instance(@NonNull DomainFactory domainFactory) {
        mDomainFactory = domainFactory;
    }

    @NonNull
    public InstanceKey getInstanceKey() {
        return new InstanceKey(getTaskKey(), getScheduleKey());
    }

    @NonNull
    public ScheduleKey getScheduleKey() {
        return new ScheduleKey(getScheduleDate(), new TimePair(getScheduleCustomTimeKey(), getScheduleHourMinute()));
    }

    @NonNull
    protected abstract Date getScheduleDate();

    @Nullable
    protected abstract CustomTimeKey getScheduleCustomTimeKey();

    @Nullable
    protected abstract HourMinute getScheduleHourMinute();

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
    DateTime getInstanceDateTime() {
        return new DateTime(getInstanceDate(), getInstanceTime());
    }

    @NonNull
    protected abstract Time getInstanceTime();

    @NonNull
    List<Instance> getChildInstances(@NonNull ExactTimeStamp now) {
        ExactTimeStamp hierarchyExactTimeStamp = getHierarchyExactTimeStamp(now);

        Task task = getTask();

        DateTime scheduleDateTime = getScheduleDateTime();

        Set<? extends TaskHierarchy> taskHierarchies = mDomainFactory.getTaskHierarchiesByParentTaskKey(task.getTaskKey());
        HashSet<Instance> childInstances = new HashSet<>();
        for (TaskHierarchy taskHierarchy : taskHierarchies) {
            Assert.assertTrue(taskHierarchy != null);

            TaskKey childTaskKey = taskHierarchy.getChildTaskKey();

            if (taskHierarchy.notDeleted(hierarchyExactTimeStamp) && taskHierarchy.getChildTask().notDeleted(hierarchyExactTimeStamp)) {
                Instance childInstance = mDomainFactory.getInstance(childTaskKey, scheduleDateTime);

                Instance parentInstance = childInstance.getParentInstance(now);
                if (parentInstance != null && parentInstance.getInstanceKey().equals(getInstanceKey()))
                    childInstances.add(childInstance);
            }
        }

        return new ArrayList<>(childInstances);
    }

    @NonNull
    private ExactTimeStamp getHierarchyExactTimeStamp(@NonNull ExactTimeStamp now) {
        ArrayList<ExactTimeStamp> exactTimeStamps = new ArrayList<>();

        exactTimeStamps.add(now);

        Task task = getTask();

        ExactTimeStamp taskEndExactTimeStamp = task.getEndExactTimeStamp();
        if (taskEndExactTimeStamp != null)
            exactTimeStamps.add(taskEndExactTimeStamp.minusOne());

        ExactTimeStamp done = getDone();
        if (done != null)
            exactTimeStamps.add(done.minusOne());

        exactTimeStamps.add(getScheduleDateTime().getTimeStamp().toExactTimeStamp());

        return Collections.min(exactTimeStamps);
    }

    @NonNull
    public abstract String getName();

    protected boolean isRootInstance(@NonNull ExactTimeStamp now) {
        return (getParentInstance(now) == null);
    }

    @NonNull
    public Task getTask() {
        return mDomainFactory.getTaskForce(getTaskKey());
    }

    @NonNull
    public TimePair getInstanceTimePair() {
        return new TimePair(getInstanceCustomTimeKey(), getInstanceHourMinute());
    }

    @Nullable
    CustomTimeKey getInstanceCustomTimeKey() {
        Time instanceTime = getInstanceTime();
        if (instanceTime instanceof CustomTime)
            return ((CustomTime) instanceTime).getCustomTimeKey();
        else
            return null;
    }

    @Nullable
    private HourMinute getInstanceHourMinute() {
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

    boolean isVisible(@NonNull ExactTimeStamp now) {
        boolean isVisible = isVisibleHelper(now);

        if (isVisible) {
            Task task = getTask();

            Date oldestVisible = task.getOldestVisible();
            Date date = getScheduleDateTime().getDate();

            if (oldestVisible != null && date.compareTo(oldestVisible) < 0) {
                if (exists()) {
                    task.correctOldestVisible(date); // po pierwsze bo syf straszny, po drugie dlatego że edycja z root na child może dodać instances w przeszłości
                } else {
                    return false;
                }
            }
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

    int getNotificationId() {
        return getNotificationId(getScheduleDate(), getScheduleCustomTimeKey(), getScheduleHourMinute(), getTaskKey());
    }

    static int getNotificationId(@NonNull Date scheduleDate, @Nullable CustomTimeKey scheduleCustomTimeKey, @Nullable HourMinute scheduleHourMinute, @NonNull TaskKey taskKey) {
        Assert.assertTrue((scheduleCustomTimeKey == null) != (scheduleHourMinute == null));

        int hash = scheduleDate.getMonth();
        hash += 12 * scheduleDate.getDay();
        hash += 12 * 31 * (scheduleDate.getYear() - 2015);

        if (scheduleCustomTimeKey == null) {
            hash += 12 * 31 * 73 * (scheduleHourMinute.getHour() + 1);
            hash += 12 * 31 * 73 * 24 * (scheduleHourMinute.getMinute() + 1);
        } else {
            hash += 12 * 31 * 73 * 24 * 60 * scheduleCustomTimeKey.hashCode();
        }

        //noinspection NumericOverflow
        hash += 12 * 31 * 73 * 24 * 60 * 10000 * taskKey.hashCode();

        return hash;
    }

    @Nullable
    protected Instance getParentInstance(@NonNull ExactTimeStamp now) {
        ExactTimeStamp hierarchyExactTimeStamp = getHierarchyExactTimeStamp(now);

        Task task = getTask();

        Task parentTask = task.getParentTask(hierarchyExactTimeStamp);

        if (parentTask == null)
            return null;

        Assert.assertTrue(parentTask.current(hierarchyExactTimeStamp));

        return mDomainFactory.getInstance(parentTask.getTaskKey(), getScheduleDateTime());
    }

    @NonNull
    public TimePair getScheduleTimePair() {
        return new TimePair(getScheduleCustomTimeKey(), getScheduleHourMinute());
    }

    public abstract void delete();

    public abstract boolean getNotificationShown();

    @Override
    public String toString() {
        return super.toString() + " " + getName() + " " + getScheduleDateTime().toString() + " " + getInstanceDateTime().toString();
    }
}
