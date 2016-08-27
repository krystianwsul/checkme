package com.krystianwsul.checkme.domainmodel;

import android.content.Context;
import android.util.Log;

import com.annimon.stream.Stream;
import com.krystianwsul.checkme.persistencemodel.InstanceRecord;
import com.krystianwsul.checkme.utils.InstanceKey;
import com.krystianwsul.checkme.utils.time.Date;
import com.krystianwsul.checkme.utils.time.DateTime;
import com.krystianwsul.checkme.utils.time.ExactTimeStamp;
import com.krystianwsul.checkme.utils.time.HourMinute;
import com.krystianwsul.checkme.utils.time.NormalTime;
import com.krystianwsul.checkme.utils.time.Time;
import com.krystianwsul.checkme.utils.time.TimePair;

import junit.framework.Assert;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

class Instance {
    private final WeakReference<DomainFactory> mDomainFactoryReference;

    private final WeakReference<Task> mTaskReference;

    private InstanceRecord mInstanceRecord;
    private DateTime mScheduleDateTime;

    Instance(DomainFactory domainFactory, Task task, InstanceRecord instanceRecord) {
        Assert.assertTrue(domainFactory != null);
        Assert.assertTrue(task != null);
        Assert.assertTrue(instanceRecord != null);

        mDomainFactoryReference = new WeakReference<>(domainFactory);

        mTaskReference = new WeakReference<>(task);

        mInstanceRecord = instanceRecord;
        mScheduleDateTime = null;
    }

    Instance(DomainFactory domainFactory, Task task, DateTime scheduleDateTime) {
        Assert.assertTrue(domainFactory != null);
        Assert.assertTrue(task != null);
        Assert.assertTrue(scheduleDateTime != null);

        mDomainFactoryReference = new WeakReference<>(domainFactory);

        mTaskReference = new WeakReference<>(task);

        mInstanceRecord = null;
        mScheduleDateTime = scheduleDateTime;
    }

    public int getTaskId() {
        Task task = mTaskReference.get();
        Assert.assertTrue(task != null);

        return task.getId();
    }

    public String getName() {
        Task task = mTaskReference.get();
        Assert.assertTrue(task != null);

        return task.getName();
    }

    private Date getScheduleDate() {
        Assert.assertTrue((mInstanceRecord == null) != (mScheduleDateTime == null));

        if (mInstanceRecord != null)
            return new Date(mInstanceRecord.getScheduleYear(), mInstanceRecord.getScheduleMonth(), mInstanceRecord.getScheduleDay());
        else
            return mScheduleDateTime.getDate();
    }

    private Time getScheduleTime() {
        Assert.assertTrue((mInstanceRecord == null) != (mScheduleDateTime == null));

        if (mInstanceRecord != null) {
            Integer customTimeId = mInstanceRecord.getScheduleCustomTimeId();
            Integer hour = mInstanceRecord.getScheduleHour();
            Integer minute = mInstanceRecord.getScheduleMinute();

            Assert.assertTrue((hour == null) == (minute == null));
            Assert.assertTrue((customTimeId == null) != (hour == null));

            if (customTimeId != null) {
                DomainFactory domainFactory = mDomainFactoryReference.get();
                Assert.assertTrue(domainFactory != null);

                return domainFactory.getCustomTime(mInstanceRecord.getScheduleCustomTimeId());
            } else {
                return new NormalTime(hour, minute);
            }
        } else {
            return mScheduleDateTime.getTime();
        }
    }

    public DateTime getScheduleDateTime() {
        return new DateTime(getScheduleDate(), getScheduleTime());
    }

    public Date getInstanceDate() {
        Assert.assertTrue((mInstanceRecord == null) != (mScheduleDateTime == null));

        if (mInstanceRecord != null) {
            Assert.assertTrue((mInstanceRecord.getInstanceYear() == null) == (mInstanceRecord.getInstanceMonth() == null));
            Assert.assertTrue((mInstanceRecord.getInstanceYear() == null) == (mInstanceRecord.getInstanceDay() == null));
            if (mInstanceRecord.getInstanceYear() != null)
                return new Date(mInstanceRecord.getInstanceYear(), mInstanceRecord.getInstanceMonth(), mInstanceRecord.getInstanceDay());
            else
                return getScheduleDate();
        } else {
            return mScheduleDateTime.getDate();
        }
    }

    private Time getInstanceTime() {
        Assert.assertTrue((mInstanceRecord == null) != (mScheduleDateTime == null));

        if (mInstanceRecord != null) {
            Assert.assertTrue((mInstanceRecord.getInstanceHour() == null) == (mInstanceRecord.getInstanceMinute() == null));
            Assert.assertTrue((mInstanceRecord.getInstanceHour() == null) || (mInstanceRecord.getInstanceCustomTimeId() == null));

            if (mInstanceRecord.getInstanceCustomTimeId() != null) {
                DomainFactory domainFactory = mDomainFactoryReference.get();
                Assert.assertTrue(domainFactory != null);

                return domainFactory.getCustomTime(mInstanceRecord.getInstanceCustomTimeId());
            } else if (mInstanceRecord.getInstanceHour() != null) {
                return new NormalTime(mInstanceRecord.getInstanceHour(), mInstanceRecord.getInstanceMinute());
            } else {
                return getScheduleTime();
            }
        } else {
            return mScheduleDateTime.getTime();
        }
    }

    public DateTime getInstanceDateTime() {
        return new DateTime(getInstanceDate(), getInstanceTime());
    }

    void setInstanceDateTime(Date date, TimePair timePair, ExactTimeStamp now) {
        Assert.assertTrue(date != null);
        Assert.assertTrue(timePair != null);
        Assert.assertTrue(now != null);
        Assert.assertTrue(isRootInstance(now));

        if (mInstanceRecord == null)
            createInstanceHierarchy(now);

        mInstanceRecord.setInstanceYear(date.getYear());
        mInstanceRecord.setInstanceMonth(date.getMonth());
        mInstanceRecord.setInstanceDay(date.getDay());

        if (timePair.CustomTimeId != null) {
            Assert.assertTrue(timePair.HourMinute == null);
            mInstanceRecord.setInstanceCustomTimeId(timePair.CustomTimeId);
            mInstanceRecord.setInstanceHour(null);
            mInstanceRecord.setInstanceMinute(null);
        } else {
            Assert.assertTrue(timePair.HourMinute != null);

            mInstanceRecord.setInstanceCustomTimeId(null);
            mInstanceRecord.setInstanceHour(timePair.HourMinute.getHour());
            mInstanceRecord.setInstanceMinute(timePair.HourMinute.getMinute());
        }

        mInstanceRecord.setNotified(false);
    }

    public String getDisplayText(Context context, ExactTimeStamp now) {
        Assert.assertTrue(now != null);

        if (isRootInstance(now)) {
            return getInstanceDateTime().getDisplayText(context);
        } else {
            return null;
        }
    }

    public ArrayList<Instance> getChildInstances(ExactTimeStamp now) {
        Assert.assertTrue(now != null);

        ExactTimeStamp hierarchyExactTimeStamp = getHierarchyExactTimeStamp(now);
        Assert.assertTrue(hierarchyExactTimeStamp != null);

        Task task = mTaskReference.get();
        Assert.assertTrue(task != null);

        DomainFactory domainFactory = mDomainFactoryReference.get();
        Assert.assertTrue(domainFactory != null);

        DateTime scheduleDateTime = getScheduleDateTime();
        Assert.assertTrue(scheduleDateTime != null);

        List<TaskHierarchy> taskHierarchies = domainFactory.getChildTaskHierarchies(task);
        HashSet<Instance> childInstances = new HashSet<>();
        for (TaskHierarchy taskHierarchy : taskHierarchies) {
            Assert.assertTrue(taskHierarchy != null);

            Task childTask = taskHierarchy.getChildTask();
            Assert.assertTrue(childTask != null);

            Instance existingChildInstance = domainFactory.getExistingInstance(childTask, scheduleDateTime);
            if (existingChildInstance != null) {
                childInstances.add(existingChildInstance);
            } else if (taskHierarchy.notDeleted(hierarchyExactTimeStamp) && taskHierarchy.getChildTask().notDeleted(hierarchyExactTimeStamp)) {
                childInstances.add(domainFactory.getInstance(childTask, scheduleDateTime));
            }
        }

        return new ArrayList<>(childInstances);
    }

    private Instance getParentInstance(ExactTimeStamp now) {
        Assert.assertTrue(now != null);

        ExactTimeStamp hierarchyExactTimeStamp = getHierarchyExactTimeStamp(now);
        Assert.assertTrue(hierarchyExactTimeStamp != null);

        Task task = mTaskReference.get();
        Assert.assertTrue(task != null);

        DomainFactory domainFactory = mDomainFactoryReference.get();
        Assert.assertTrue(domainFactory != null);

        Task parentTask = task.getParentTask(hierarchyExactTimeStamp);

        if (parentTask == null)
            return null;

        Assert.assertTrue(parentTask.current(hierarchyExactTimeStamp));

        Instance parentInstance = domainFactory.getInstance(parentTask, getScheduleDateTime());
        Assert.assertTrue(parentInstance != null);

        return parentInstance;
    }

    boolean isRootInstance(ExactTimeStamp now) {
        Assert.assertTrue(now != null);

        Task task = mTaskReference.get();
        Assert.assertTrue(task != null);

        return task.isRootTask(getHierarchyExactTimeStamp(now));
    }

    public ExactTimeStamp getDone() {
        if (mInstanceRecord == null)
            return null;

        Long done = mInstanceRecord.getDone();
        if (done != null)
            return new ExactTimeStamp(done);
        else
            return null;
    }

    void setDone(boolean done, ExactTimeStamp now) {
        Assert.assertTrue(now != null);

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

    private void createInstanceHierarchy(ExactTimeStamp now) {
        Assert.assertTrue(now != null);
        Assert.assertTrue((mInstanceRecord == null) != (mScheduleDateTime == null));

        Instance parentInstance = getParentInstance(now);
        if (parentInstance != null)
            parentInstance.createInstanceHierarchy(now);

        if (mInstanceRecord == null)
            createInstanceRecord(now);
    }

    private void createInstanceRecord(ExactTimeStamp now) {
        Assert.assertTrue(now != null);

        Assert.assertTrue(mInstanceRecord == null);
        Assert.assertTrue(mScheduleDateTime != null);

        Task task = mTaskReference.get();
        Assert.assertTrue(task != null);

        DomainFactory domainFactory = mDomainFactoryReference.get();
        Assert.assertTrue(domainFactory != null);

        DateTime scheduleDateTime = getScheduleDateTime();

        mScheduleDateTime = null;
        mInstanceRecord = domainFactory.createInstanceRecord(task, this, scheduleDateTime, now);
    }

    private ExactTimeStamp getHierarchyExactTimeStamp(ExactTimeStamp now) {
        ArrayList<ExactTimeStamp> exactTimeStamps = new ArrayList<>();

        exactTimeStamps.add(now);

        Task task = mTaskReference.get();
        Assert.assertTrue(task != null);

        ExactTimeStamp taskEndExactTimeStamp = task.getEndExactTimeStamp();
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

    boolean getNotified() {
        return (mInstanceRecord != null && mInstanceRecord.getNotified());
    }

    void setNotified(boolean notified, ExactTimeStamp now) {
        Assert.assertTrue(now != null);

        if (mInstanceRecord == null)
            createInstanceHierarchy(now);

        Assert.assertTrue(mInstanceRecord != null);
        mInstanceRecord.setNotified(notified);
    }

    /*
    I'm going to make some assumptions here:
        1. I won't live past a hundred years
        2. scheduleYear is between 2016 and 2088 (that way the algorithm should be fine during my lifetime)
        3. scheduleCustomTimeId is between 1 and 10,000
        4. hash looping past Integer.MAX_VALUE isn't likely to cause collisions
     */

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

    void setNotificationShown(boolean notificationShown, ExactTimeStamp now) {
        Assert.assertTrue(now != null);

        if (mInstanceRecord == null)
            createInstanceHierarchy(now);

        Assert.assertTrue(mInstanceRecord != null);

        mInstanceRecord.setNotificationShown(notificationShown);
    }

    private Integer getScheduleCustomTimeId() {
        Time scheduleTime = getScheduleTime();
        if (scheduleTime instanceof CustomTime)
            return ((CustomTime) scheduleTime).getId();
        else
            return null;
    }

    private HourMinute getScheduleHourMinute() {
        Time scheduleTime = getScheduleTime();
        if (scheduleTime instanceof NormalTime)
            return ((NormalTime) scheduleTime).getHourMinute();
        else
            return null;
    }

    private Integer getInstanceCustomTimeId() {
        Time instanceTime = getInstanceTime();
        if (instanceTime instanceof CustomTime)
            return ((CustomTime) instanceTime).getId();
        else
            return null;
    }

    private HourMinute getInstanceHourMinute() {
        Time instanceTime = getInstanceTime();
        if (instanceTime instanceof NormalTime)
            return ((NormalTime) instanceTime).getHourMinute();
        else
            return null;
    }

    public InstanceKey getInstanceKey() {
        return new InstanceKey(getTaskId(), getScheduleDate(), getScheduleCustomTimeId(), getScheduleHourMinute());
    }

    public TimePair getInstanceTimePair() {
        return new TimePair(getInstanceCustomTimeId(), getInstanceHourMinute());
    }

    boolean isVisible(ExactTimeStamp now) {
        Assert.assertTrue(now != null);

        boolean isVisible = isVisibleHelper(now);

        if (isVisible) {
            Task task = mTaskReference.get();
            Assert.assertTrue(task != null);

            Date oldestVisible = task.getOldestVisible();

            // zone hack
            if (!(oldestVisible == null || oldestVisible.compareTo(getScheduleDateTime().getDate()) <= 0)) {
                Log.e("asdf", getName() + " oldest: " + oldestVisible + ", schedule: " + getScheduleDateTime() + ", instance: " + getInstanceDateTime() + ", exists: " + exists());
            }

            Assert.assertTrue(oldestVisible == null || oldestVisible.compareTo(getScheduleDateTime().getTimeStamp().getDate()) <= 0 || exists());
        }

        return isVisible;
    }

    private boolean isVisibleHelper(ExactTimeStamp now) {
        Assert.assertTrue(now != null);

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

    boolean isRelevant(ExactTimeStamp now) {
        Assert.assertTrue(now != null);

        if (isVisible(now))
            return true;

        if (Stream.of(getChildInstances(now))
                .anyMatch(instance -> instance.isRelevant(now)))
            return true;

        Task task = mTaskReference.get();
        Assert.assertTrue(task != null);

        Date oldestVisible = task.getOldestVisible();

        if (oldestVisible == null)
            return true;

        //noinspection RedundantIfStatement
        if (getScheduleDateTime().getDate().compareTo(oldestVisible) >= 0)
            return true;

        return false;
    }

    boolean exists() {
        Assert.assertTrue((mInstanceRecord == null) != (mScheduleDateTime == null));
        return (mInstanceRecord != null);
    }

    public void setRelevant() {
        mInstanceRecord.setRelevant(false);
    }

    @SuppressWarnings("RedundantIfStatement")
    public boolean usesCustomTime(CustomTime customTime) {
        Assert.assertTrue(customTime != null);
        Assert.assertTrue(exists());

        Integer scheduleCustomTimeId = getScheduleCustomTimeId();
        if ((scheduleCustomTimeId != null) && (customTime.getId() == scheduleCustomTimeId))
            return true;

        Integer instanceCustomTimeId = getInstanceCustomTimeId();
        if ((instanceCustomTimeId != null) && (customTime.getId() == instanceCustomTimeId))
            return true;

        return false;
    }
}
