package com.krystianwsul.checkme.domainmodel;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.krystianwsul.checkme.persistencemodel.InstanceRecord;
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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

class Instance implements MergedInstance {
    @NonNull
    private final DomainFactory mDomainFactory;

    @Nullable
    private InstanceRecord mInstanceRecord;

    @Nullable
    private Integer mTaskId;

    @Nullable
    private DateTime mScheduleDateTime;

    Instance(@NonNull DomainFactory domainFactory, @NonNull InstanceRecord instanceRecord) {
        mDomainFactory = domainFactory;

        mInstanceRecord = instanceRecord;

        mTaskId = null;
        mScheduleDateTime = null;
    }

    Instance(@NonNull DomainFactory domainFactory, int taskId, @NonNull DateTime scheduleDateTime) {
        mDomainFactory = domainFactory;

        mInstanceRecord = null;

        mTaskId = taskId;
        mScheduleDateTime = scheduleDateTime;
    }

    int getTaskId() {
        if (mInstanceRecord != null) {
            Assert.assertTrue(mTaskId == null);
            Assert.assertTrue(mScheduleDateTime == null);

            return mInstanceRecord.getTaskId();
        } else {
            Assert.assertTrue(mTaskId != null);
            Assert.assertTrue(mScheduleDateTime != null);

            return mTaskId;
        }
    }

    @NonNull
    @Override
    public TaskKey getTaskKey() {
        return new TaskKey(getTaskId()); // todo firebase
    }

    @NonNull
    @Override
    public LocalTask getTask() {
        Task task = mDomainFactory.getTask(new TaskKey(getTaskId()));
        Assert.assertTrue(task instanceof LocalTask); // todo firebase;

        return (LocalTask) task;
    }

    @NonNull
    @Override
    public String getName() {
        return getTask().getName();
    }

    @NonNull
    private Date getScheduleDate() {
        if (mInstanceRecord != null) {
            Assert.assertTrue(mTaskId == null);
            Assert.assertTrue(mScheduleDateTime == null);

            return new Date(mInstanceRecord.getScheduleYear(), mInstanceRecord.getScheduleMonth(), mInstanceRecord.getScheduleDay());
        } else {
            Assert.assertTrue(mTaskId != null);
            Assert.assertTrue(mScheduleDateTime != null);

            return mScheduleDateTime.getDate();
        }
    }

    @NonNull
    private Time getScheduleTime() {
        if (mInstanceRecord != null) {
            Assert.assertTrue(mTaskId == null);
            Assert.assertTrue(mScheduleDateTime == null);

            Integer customTimeId = mInstanceRecord.getScheduleCustomTimeId();
            Integer hour = mInstanceRecord.getScheduleHour();
            Integer minute = mInstanceRecord.getScheduleMinute();

            Assert.assertTrue((hour == null) == (minute == null));
            Assert.assertTrue((customTimeId == null) != (hour == null));

            if (customTimeId != null) {
                return mDomainFactory.getCustomTime(customTimeId);
            } else {
                return new NormalTime(hour, minute);
            }
        } else {
            Assert.assertTrue(mTaskId != null);
            Assert.assertTrue(mScheduleDateTime != null);

            return mScheduleDateTime.getTime();
        }
    }

    @NonNull
    @Override
    public DateTime getScheduleDateTime() {
        return new DateTime(getScheduleDate(), getScheduleTime());
    }

    @NonNull
    @Override
    public Date getInstanceDate() {
        if (mInstanceRecord != null) {
            Assert.assertTrue(mTaskId == null);
            Assert.assertTrue(mScheduleDateTime == null);

            Assert.assertTrue((mInstanceRecord.getInstanceYear() == null) == (mInstanceRecord.getInstanceMonth() == null));
            Assert.assertTrue((mInstanceRecord.getInstanceYear() == null) == (mInstanceRecord.getInstanceDay() == null));
            if (mInstanceRecord.getInstanceYear() != null)
                return new Date(mInstanceRecord.getInstanceYear(), mInstanceRecord.getInstanceMonth(), mInstanceRecord.getInstanceDay());
            else
                return getScheduleDate();
        } else {
            Assert.assertTrue(mTaskId != null);
            Assert.assertTrue(mScheduleDateTime != null);

            return mScheduleDateTime.getDate();
        }
    }

    @NonNull
    private Time getInstanceTime() {
        if (mInstanceRecord != null) {
            Assert.assertTrue(mTaskId == null);
            Assert.assertTrue(mScheduleDateTime == null);

            Assert.assertTrue((mInstanceRecord.getInstanceHour() == null) == (mInstanceRecord.getInstanceMinute() == null));
            Assert.assertTrue((mInstanceRecord.getInstanceHour() == null) || (mInstanceRecord.getInstanceCustomTimeId() == null));

            if (mInstanceRecord.getInstanceCustomTimeId() != null) {
                return mDomainFactory.getCustomTime(mInstanceRecord.getInstanceCustomTimeId());
            } else if (mInstanceRecord.getInstanceHour() != null) {
                return new NormalTime(mInstanceRecord.getInstanceHour(), mInstanceRecord.getInstanceMinute());
            } else {
                return getScheduleTime();
            }
        } else {
            Assert.assertTrue(mTaskId != null);
            Assert.assertTrue(mScheduleDateTime != null);

            return mScheduleDateTime.getTime();
        }
    }

    @NonNull
    @Override
    public DateTime getInstanceDateTime() {
        return new DateTime(getInstanceDate(), getInstanceTime());
    }

    @Override
    public void setInstanceDateTime(@NonNull Date date, @NonNull TimePair timePair, @NonNull ExactTimeStamp now) {
        Assert.assertTrue(isRootInstance(now));

        if (mInstanceRecord == null)
            createInstanceHierarchy(now);

        mInstanceRecord.setInstanceYear(date.getYear());
        mInstanceRecord.setInstanceMonth(date.getMonth());
        mInstanceRecord.setInstanceDay(date.getDay());

        if (timePair.mCustomTimeId != null) {
            Assert.assertTrue(timePair.mHourMinute == null);
            mInstanceRecord.setInstanceCustomTimeId(timePair.mCustomTimeId);
            mInstanceRecord.setInstanceHour(null);
            mInstanceRecord.setInstanceMinute(null);
        } else {
            Assert.assertTrue(timePair.mHourMinute != null);

            mInstanceRecord.setInstanceCustomTimeId(null);
            mInstanceRecord.setInstanceHour(timePair.mHourMinute.getHour());
            mInstanceRecord.setInstanceMinute(timePair.mHourMinute.getMinute());
        }

        mInstanceRecord.setNotified(false);
    }

    @Nullable
    @Override
    public String getDisplayText(@NonNull Context context, @NonNull ExactTimeStamp now) {
        if (isRootInstance(now)) {
            return getInstanceDateTime().getDisplayText(context);
        } else {
            return null;
        }
    }

    @NonNull
    @Override
    public List<MergedInstance> getChildInstances(@NonNull ExactTimeStamp now) {
        ExactTimeStamp hierarchyExactTimeStamp = getHierarchyExactTimeStamp(now);

        LocalTask localTask = getTask();

        DateTime scheduleDateTime = getScheduleDateTime();

        List<TaskHierarchy> taskHierarchies = mDomainFactory.getChildTaskHierarchies(localTask);
        HashSet<MergedInstance> childInstances = new HashSet<>();
        for (TaskHierarchy taskHierarchy : taskHierarchies) {
            Assert.assertTrue(taskHierarchy != null);

            Assert.assertTrue(taskHierarchy.getChildTask() instanceof LocalTask); // todo firebase
            LocalTask childLocalTask = (LocalTask) taskHierarchy.getChildTask();

            MergedInstance existingChildInstance = mDomainFactory.getExistingInstance(childLocalTask, scheduleDateTime);
            if (existingChildInstance != null) {
                childInstances.add(existingChildInstance);
            } else if (taskHierarchy.notDeleted(hierarchyExactTimeStamp) && taskHierarchy.getChildTask().notDeleted(hierarchyExactTimeStamp)) {
                childInstances.add(mDomainFactory.getInstance(childLocalTask, scheduleDateTime));
            }
        }

        return new ArrayList<>(childInstances);
    }

    @Nullable
    @Override
    public MergedInstance getParentInstance(@NonNull ExactTimeStamp now) {
        ExactTimeStamp hierarchyExactTimeStamp = getHierarchyExactTimeStamp(now);

        LocalTask localTask = getTask();

        Task parentTask = localTask.getParentTask(hierarchyExactTimeStamp);

        if (parentTask == null)
            return null;

        Assert.assertTrue(parentTask.current(hierarchyExactTimeStamp));

        return mDomainFactory.getInstance(parentTask, getScheduleDateTime());
    }

    @Override
    public boolean isRootInstance(@NonNull ExactTimeStamp now) {
        return getTask().isRootTask(getHierarchyExactTimeStamp(now));
    }

    @Nullable
    @Override
    public ExactTimeStamp getDone() {
        if (mInstanceRecord == null)
            return null;

        Long done = mInstanceRecord.getDone();
        if (done != null)
            return new ExactTimeStamp(done);
        else
            return null;
    }

    @Override
    public void setDone(boolean done, @NonNull ExactTimeStamp now) {
        if (done) {
            if (mInstanceRecord == null) {
                createInstanceHierarchy(now);
                mInstanceRecord.setDone(now.getLong());
            } else {
                mInstanceRecord.setDone(now.getLong());
            }
        } else {
            Assert.assertTrue(mInstanceRecord != null);
            mInstanceRecord.setDone(null);
            mInstanceRecord.setNotified(false);
        }
    }

    @Override
    public void createInstanceHierarchy(@NonNull ExactTimeStamp now) {
        Assert.assertTrue((mInstanceRecord == null) != (mScheduleDateTime == null));
        Assert.assertTrue((mTaskId == null) == (mScheduleDateTime == null));

        MergedInstance parentInstance = getParentInstance(now);
        if (parentInstance != null)
            parentInstance.createInstanceHierarchy(now);

        if (mInstanceRecord == null)
            createInstanceRecord(now);
    }

    private void createInstanceRecord(@NonNull ExactTimeStamp now) {
        LocalTask localTask = getTask();

        DateTime scheduleDateTime = getScheduleDateTime();

        mTaskId = null;
        mScheduleDateTime = null;

        mInstanceRecord = mDomainFactory.createInstanceRecord(localTask, this, scheduleDateTime, now);
    }

    @NonNull
    private ExactTimeStamp getHierarchyExactTimeStamp(@NonNull ExactTimeStamp now) {
        ArrayList<ExactTimeStamp> exactTimeStamps = new ArrayList<>();

        exactTimeStamps.add(now);

        LocalTask localTask = getTask();

        ExactTimeStamp taskEndExactTimeStamp = localTask.getEndExactTimeStamp();
        if (taskEndExactTimeStamp != null)
            exactTimeStamps.add(taskEndExactTimeStamp.minusOne());

        ExactTimeStamp done = getDone();
        if (done != null)
            exactTimeStamps.add(done.minusOne());

        if (mInstanceRecord != null)
            exactTimeStamps.add(new ExactTimeStamp(mInstanceRecord.getHierarchyTime()));

        return Collections.min(exactTimeStamps);
    }

    @Override
    public String toString() {
        return getName() + " " + getInstanceDateTime();
    }

    @Override
    public boolean getNotified() {
        return (mInstanceRecord != null && mInstanceRecord.getNotified());
    }

    @Override
    public void setNotified(@NonNull ExactTimeStamp now) {
        if (mInstanceRecord == null)
            createInstanceHierarchy(now);

        Assert.assertTrue(mInstanceRecord != null);
        mInstanceRecord.setNotified(true);
    }

    /*
    I'm going to make some assumptions here:
        1. I won't live past a hundred years
        2. scheduleYear is between 2016 and 2088 (that way the algorithm should be fine during my lifetime)
        3. scheduleCustomTimeId is between 1 and 10,000
        4. hash looping past Integer.MAX_VALUE isn't likely to cause collisions
     */

    @Override
    public int getNotificationId() {
        Date scheduleDate = getScheduleDate();

        Integer scheduleCustomTimeId = getScheduleCustomTimeId();
        HourMinute scheduleHourMinute = getScheduleHourMinute();
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
        hash += 12 * 31 * 73 * 24 * 60 * 10000 * getTaskId();

        return hash;
    }

    boolean getNotificationShown() {
        return (mInstanceRecord != null && mInstanceRecord.getNotificationShown());
    }

    @Override
    public void setNotificationShown(boolean notificationShown, @NonNull ExactTimeStamp now) {
        if (mInstanceRecord == null)
            createInstanceHierarchy(now);

        Assert.assertTrue(mInstanceRecord != null);

        mInstanceRecord.setNotificationShown(notificationShown);
    }

    @Nullable
    private Integer getScheduleCustomTimeId() {
        Time scheduleTime = getScheduleTime();
        if (scheduleTime instanceof CustomTime)
            return ((CustomTime) scheduleTime).getId();
        else
            return null;
    }

    @Nullable
    private HourMinute getScheduleHourMinute() {
        Time scheduleTime = getScheduleTime();
        if (scheduleTime instanceof NormalTime)
            return ((NormalTime) scheduleTime).getHourMinute();
        else
            return null;
    }

    @Nullable
    private Integer getInstanceCustomTimeId() {
        Time instanceTime = getInstanceTime();
        if (instanceTime instanceof CustomTime)
            return ((CustomTime) instanceTime).getId();
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
    public InstanceKey getInstanceKey() {
        return new InstanceKey(new TaskKey(getTaskId()), getScheduleDate(), getScheduleCustomTimeId(), getScheduleHourMinute());
    }

    @NonNull
    @Override
    public TimePair getInstanceTimePair() {
        return new TimePair(getInstanceCustomTimeId(), getInstanceHourMinute());
    }

    @NonNull
    @Override
    public TimePair getScheduleTimePair() {
        return new TimePair(getScheduleCustomTimeId(), getScheduleHourMinute());
    }

    @Override
    public boolean isVisible(@NonNull ExactTimeStamp now) {
        boolean isVisible = isVisibleHelper(now);

        if (isVisible) {
            LocalTask localTask = getTask();

            Date oldestVisible = localTask.getOldestVisible();

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

        MergedInstance parentInstance = getParentInstance(now);
        if (parentInstance == null) {
            ExactTimeStamp done = getDone();
            return (done == null || (done.compareTo(twentyFourHoursAgo) > 0));
        } else {
            return parentInstance.isVisible(now);
        }
    }

    @Override
    public boolean exists() {
        Assert.assertTrue((mInstanceRecord == null) != (mScheduleDateTime == null));
        Assert.assertTrue((mTaskId == null) == (mScheduleDateTime == null));

        return (mInstanceRecord != null);
    }

    public void setRelevant() {
        Assert.assertTrue(mInstanceRecord != null);

        mInstanceRecord.setRelevant(false);
    }
}
