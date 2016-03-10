package com.example.krystianwsul.organizator.domainmodel;

import android.content.Context;

import com.example.krystianwsul.organizator.persistencemodel.InstanceRecord;
import com.example.krystianwsul.organizator.utils.InstanceKey;
import com.example.krystianwsul.organizator.utils.time.Date;
import com.example.krystianwsul.organizator.utils.time.DateTime;
import com.example.krystianwsul.organizator.utils.time.ExactTimeStamp;
import com.example.krystianwsul.organizator.utils.time.HourMinute;
import com.example.krystianwsul.organizator.utils.time.NormalTime;
import com.example.krystianwsul.organizator.utils.time.Time;
import com.example.krystianwsul.organizator.utils.time.TimePair;
import com.example.krystianwsul.organizator.utils.time.TimeStamp;

import junit.framework.Assert;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

public class Instance {
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

    public Date getScheduleDate() {
        Assert.assertTrue((mInstanceRecord == null) != (mScheduleDateTime == null));

        if (mInstanceRecord != null)
            return new Date(mInstanceRecord.getScheduleYear(), mInstanceRecord.getScheduleMonth(), mInstanceRecord.getScheduleDay());
        else
            return mScheduleDateTime.getDate();
    }

    public Time getScheduleTime() {
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

    public Time getInstanceTime() {
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

    void setInstanceDateTime(Date date, TimePair timePair) {
        Assert.assertTrue(date != null);
        Assert.assertTrue(timePair != null);
        Assert.assertTrue(isRootInstance());

        if (mInstanceRecord == null)
            createInstanceHierarchy(ExactTimeStamp.getNow());

        mInstanceRecord.setInstanceYear(date.getYear());
        mInstanceRecord.setInstanceMonth(date.getMonth());
        mInstanceRecord.setInstanceDay(date.getDay());

        if (timePair.CustomTimeId != null) {
            Assert.assertTrue(timePair.HourMinute == null);
            mInstanceRecord.setInstanceCustomTimeId(timePair.CustomTimeId);
        } else {
            Assert.assertTrue(timePair.HourMinute != null);

            mInstanceRecord.setInstanceHour(timePair.HourMinute.getHour());
            mInstanceRecord.setInstanceMinute(timePair.HourMinute.getMinute());
        }

        resetNotification();
    }

    public String getDisplayText(Context context) {
        if (isRootInstance()) {
            return getInstanceDateTime().getDisplayText(context);
        } else {
            return null;
        }
    }

    public ArrayList<Instance> getChildInstances() {
        ExactTimeStamp hierarchyExactTimeStamp = getHierarchyExactTimeStamp();

        Task task = mTaskReference.get();
        Assert.assertTrue(task != null);

        DomainFactory domainFactory = mDomainFactoryReference.get();
        Assert.assertTrue(domainFactory != null);

        ArrayList<Instance> childInstances = new ArrayList<>();
        for (Task childTask : task.getChildTasks(hierarchyExactTimeStamp)) {
            Assert.assertTrue(childTask.current(hierarchyExactTimeStamp));

            Instance childInstance = domainFactory.getInstance(childTask, getScheduleDateTime());
            Assert.assertTrue(childInstance != null);

            childInstances.add(childInstance);
        }
        return childInstances;
    }

    private Instance getParentInstance() {
        ExactTimeStamp hierarchyExactTimeStamp = getHierarchyExactTimeStamp();

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

    boolean isRootInstance() {
        Task task = mTaskReference.get();
        Assert.assertTrue(task != null);

        return task.isRootTask(getHierarchyExactTimeStamp());
    }

    public TimeStamp getDone() {
        if (mInstanceRecord == null)
            return null;

        Long done = mInstanceRecord.getDone();
        if (done != null)
            return new TimeStamp(done);
        else
            return null;
    }

    void setDone(boolean done) {
        if (done) {
            ExactTimeStamp now = ExactTimeStamp.getNow();

            if (mInstanceRecord == null) {
                getRootInstance().createInstanceHierarchy(now);
                mInstanceRecord.setDone(now.toTimeStamp().getLong());
            } else {
                mInstanceRecord.setDone(now.toTimeStamp().getLong());
            }

            if (isRootInstance()) {
                Task rootTask = mTaskReference.get();
                Assert.assertTrue(rootTask != null);
                Assert.assertTrue(rootTask.isRootTask(getHierarchyExactTimeStamp()));

                if (rootTask.current(now) && rootTask.onlySingleSchedules()) {
                    SingleSchedule singleSchedule = (SingleSchedule)rootTask.getCurrentSchedule(now);
                    if (singleSchedule.getDateTime().equals(getScheduleDateTime())) {
                        DomainFactory domainFactory = mDomainFactoryReference.get();
                        Assert.assertTrue(domainFactory != null);

                        ArrayList<Instance> allInstances = domainFactory.getExistingInstances(rootTask);
                        ArrayList<Instance> notDoneInstances = new ArrayList<>();
                        for (Instance taskInstance : allInstances)
                            if (taskInstance.getDone() == null)
                                notDoneInstances.add(taskInstance);

                        if (notDoneInstances.isEmpty())
                            rootTask.setEndExactTimeStamp(now.plusOne());
                    }
                }
            }
        } else {
            Assert.assertTrue(mInstanceRecord != null);
            mInstanceRecord.setDone(null);

            resetNotification();
        }
    }

    private Instance getRootInstance() {
        Instance parentInstance = getParentInstance();

        if (parentInstance != null)
            return parentInstance.getRootInstance();
        else
            return this;
    }

    private void createInstanceHierarchy(ExactTimeStamp now) {
        Assert.assertTrue(now != null);

        if (mInstanceRecord == null)
            createInstanceRecord(now);

        for (Instance childInstance : getChildInstances())
            childInstance.createInstanceHierarchy(now);
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

    private ExactTimeStamp getHierarchyExactTimeStamp() {
        if (mInstanceRecord != null)
            return new ExactTimeStamp(mInstanceRecord.getHierarchyTime());
        else
            return getScheduleDateTime().getTimeStamp().toExactTimeStamp();
    }

    @Override
    public String toString() {
        return getName() + " " + getInstanceDateTime();
    }

    boolean getNotified() {
        return (mInstanceRecord != null && mInstanceRecord.getNotified());
    }

    void setNotified(boolean notified) {
        if (mInstanceRecord == null)
            createInstanceHierarchy(ExactTimeStamp.getNow());

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

    void setNotificationShown(boolean notificationShown) {
        if (mInstanceRecord == null)
            createInstanceHierarchy(ExactTimeStamp.getNow());

        Assert.assertTrue(mInstanceRecord != null);

        mInstanceRecord.setNotificationShown(notificationShown);
    }

    private void resetNotification() {
        Assert.assertTrue(mInstanceRecord != null);
        Assert.assertTrue(isRootInstance());

        mInstanceRecord.setNotified(false);
    }

    public Integer getScheduleCustomTimeId() {
        Time scheduleTime = getScheduleTime();
        if (scheduleTime instanceof CustomTime)
            return ((CustomTime) scheduleTime).getId();
        else
            return null;
    }

    public  HourMinute getScheduleHourMinute() {
        Time scheduleTime = getScheduleTime();
        if (scheduleTime instanceof NormalTime)
            return ((NormalTime) scheduleTime).getHourMinute();
        else
            return null;
    }

    Integer getInstanceCustomTimeId() {
        Time instanceTime = getInstanceTime();
        if (instanceTime instanceof CustomTime)
            return ((CustomTime) instanceTime).getId();
        else
            return null;
    }

    HourMinute getInstanceHourMinute() {
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
}
