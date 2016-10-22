package com.krystianwsul.checkme.domainmodel;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.krystianwsul.checkme.persistencemodel.InstanceRecord;
import com.krystianwsul.checkme.utils.TaskKey;
import com.krystianwsul.checkme.utils.time.Date;
import com.krystianwsul.checkme.utils.time.DateTime;
import com.krystianwsul.checkme.utils.time.ExactTimeStamp;
import com.krystianwsul.checkme.utils.time.NormalTime;
import com.krystianwsul.checkme.utils.time.Time;
import com.krystianwsul.checkme.utils.time.TimePair;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.Collections;

public class LocalInstance extends Instance {
    @Nullable
    private InstanceRecord mInstanceRecord;

    @Nullable
    private Integer mTaskId;

    @Nullable
    private DateTime mScheduleDateTime;

    public LocalInstance(@NonNull DomainFactory domainFactory, @NonNull InstanceRecord instanceRecord) {
        super(domainFactory);

        mInstanceRecord = instanceRecord;

        mTaskId = null;
        mScheduleDateTime = null;
    }

    LocalInstance(@NonNull DomainFactory domainFactory, int taskId, @NonNull DateTime scheduleDateTime) {
        super(domainFactory);

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
    public String getName() {
        return getTask().getName();
    }

    @NonNull
    @Override
    protected Date getScheduleDate() {
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
    @Override
    protected Time getScheduleTime() {
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
    @Override
    protected Time getInstanceTime() {
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

        Instance parentInstance = getParentInstance(now);
        if (parentInstance != null)
            parentInstance.createInstanceHierarchy(now);

        if (mInstanceRecord == null)
            createInstanceRecord(now);
    }

    private void createInstanceRecord(@NonNull ExactTimeStamp now) {
        LocalTask localTask = (LocalTask) getTask();

        DateTime scheduleDateTime = getScheduleDateTime();

        mTaskId = null;
        mScheduleDateTime = null;

        mInstanceRecord = mDomainFactory.createInstanceRecord(localTask, this, scheduleDateTime, now);
    }

    @NonNull
    protected ExactTimeStamp getHierarchyExactTimeStamp(@NonNull ExactTimeStamp now) {
        ArrayList<ExactTimeStamp> exactTimeStamps = new ArrayList<>();

        exactTimeStamps.add(now);

        Task task = getTask();

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
