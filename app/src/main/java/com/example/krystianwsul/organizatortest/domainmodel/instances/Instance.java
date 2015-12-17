package com.example.krystianwsul.organizatortest.domainmodel.instances;

import android.content.Context;

import com.example.krystianwsul.organizatortest.domainmodel.dates.Date;
import com.example.krystianwsul.organizatortest.domainmodel.dates.DateTime;
import com.example.krystianwsul.organizatortest.domainmodel.dates.TimeStamp;
import com.example.krystianwsul.organizatortest.domainmodel.tasks.Task;
import com.example.krystianwsul.organizatortest.domainmodel.times.CustomTimeFactory;
import com.example.krystianwsul.organizatortest.domainmodel.times.NormalTime;
import com.example.krystianwsul.organizatortest.domainmodel.times.Time;
import com.example.krystianwsul.organizatortest.persistencemodel.InstanceRecord;
import com.example.krystianwsul.organizatortest.persistencemodel.PersistenceManger;

import junit.framework.Assert;

import java.util.ArrayList;

public class Instance {
    private final Task mTask;

    private InstanceRecord mInstanceRecord;
    private DateTime mScheduleDateTime;

    Instance(Task task, InstanceRecord instanceRecord) {
        Assert.assertTrue(task != null);
        Assert.assertTrue(instanceRecord != null);

        mTask = task;

        mInstanceRecord = instanceRecord;
        mScheduleDateTime = null;
    }

    Instance(Task task, DateTime scheduleDateTime) {
        Assert.assertTrue(task != null);
        Assert.assertTrue(scheduleDateTime != null);

        mTask = task;

        mInstanceRecord = null;
        mScheduleDateTime = scheduleDateTime;
    }

    public Task getTask() {
        return mTask;
    }

    public int getTaskId() {
        return mTask.getId();
    }

    public String getName() {
        return mTask.getName();
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

            if (customTimeId != null)
                return CustomTimeFactory.getInstance().getCustomTime(mInstanceRecord.getScheduleCustomTimeId());
            else
                return new NormalTime(hour, minute);
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
            Assert.assertTrue((mInstanceRecord.getInstanceYear() == null) == (mInstanceRecord.getInstanceMonth() == null) == (mInstanceRecord.getInstanceDay() == null));
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

            if (mInstanceRecord.getInstanceCustomTimeId() != null)
                return CustomTimeFactory.getInstance().getCustomTime(mInstanceRecord.getInstanceCustomTimeId());
            else if (mInstanceRecord.getInstanceHour() != null)
                return new NormalTime(mInstanceRecord.getInstanceHour(), mInstanceRecord.getInstanceMinute());
            else
                return getScheduleTime();
        } else {
            return mScheduleDateTime.getTime();
        }
    }

    public DateTime getInstanceDateTime() {
        return new DateTime(getInstanceDate(), getInstanceTime());
    }

    public String getScheduleText(Context context) {
        if (isRootInstance()) {
            return getInstanceDateTime().getDisplayText(context);
        } else {
            return null;
        }
    }

    public ArrayList<Instance> getChildInstances() {
        TimeStamp hierarchyTimeStamp = getHierarchyTimeStamp();

        ArrayList<Instance> childInstances = new ArrayList<>();
        for (Task childTask : mTask.getChildTasks(hierarchyTimeStamp)) {
            Assert.assertTrue(childTask.current(hierarchyTimeStamp));

            Instance childInstance = InstanceFactory.getInstance().getInstance(childTask, getScheduleDateTime());
            Assert.assertTrue(childInstance != null);

            childInstances.add(childInstance);
        }
        return childInstances;
    }

    public Instance getParentInstance() {
        TimeStamp hierarchyTimeStamp = getHierarchyTimeStamp();

        Task parentTask = mTask.getParentTask(hierarchyTimeStamp);
        if (parentTask == null)
            return null;

        Assert.assertTrue(parentTask.current(hierarchyTimeStamp));

        Instance parentInstance = InstanceFactory.getInstance().getInstance(parentTask, getScheduleDateTime());
        Assert.assertTrue(parentInstance != null);

        return parentInstance;
    }

    public boolean isRootInstance() {
        return mTask.isRootTask(getHierarchyTimeStamp());
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

    public void setDone(boolean done) {
        if (mInstanceRecord == null) {
            if (done) {
                createInstanceRecord();
                getRootInstance().createInstanceHierarchy();
                mInstanceRecord.setDone(TimeStamp.getNow().getLong());
            }
        } else {
            if (done)
                mInstanceRecord.setDone(TimeStamp.getNow().getLong());
            else
                mInstanceRecord.setDone(null);
        }
    }

    private Instance getRootInstance() {
        Instance parentInstance = getParentInstance();

        if (parentInstance != null)
            return parentInstance.getRootInstance();
        else
            return this;
    }

    private void createInstanceHierarchy() {
        createInstanceRecord();

        for (Instance childInstance : getChildInstances())
            childInstance.createInstanceHierarchy();
    }

    private void createInstanceRecord() {
        if (mInstanceRecord != null)
            return;

        InstanceFactory.getInstance().addExistingInstance(this);

        DateTime scheduleDateTime = getScheduleDateTime();

        mScheduleDateTime = null;
        mInstanceRecord = PersistenceManger.getInstance().createInstanceRecord(mTask, scheduleDateTime);
    }

    private TimeStamp getHierarchyTimeStamp() {
        if (mInstanceRecord != null)
            return new TimeStamp(mInstanceRecord.getHierarchyTime());
        else
            return getScheduleDateTime().getTimeStamp();
    }
}
