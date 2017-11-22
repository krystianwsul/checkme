package com.krystianwsul.checkme.firebase;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.Pair;
import android.text.TextUtils;

import com.krystianwsul.checkme.domainmodel.DomainFactory;
import com.krystianwsul.checkme.domainmodel.Instance;
import com.krystianwsul.checkme.domainmodel.Task;
import com.krystianwsul.checkme.firebase.records.RemoteInstanceRecord;
import com.krystianwsul.checkme.persistencemodel.InstanceShownRecord;
import com.krystianwsul.checkme.utils.CustomTimeKey;
import com.krystianwsul.checkme.utils.TaskKey;
import com.krystianwsul.checkme.utils.time.Date;
import com.krystianwsul.checkme.utils.time.DateTime;
import com.krystianwsul.checkme.utils.time.ExactTimeStamp;
import com.krystianwsul.checkme.utils.time.HourMinute;
import com.krystianwsul.checkme.utils.time.NormalTime;
import com.krystianwsul.checkme.utils.time.Time;
import com.krystianwsul.checkme.utils.time.TimePair;

import junit.framework.Assert;

public class RemoteInstance extends Instance {
    @NonNull
    private final RemoteProject mRemoteProject;

    @Nullable
    private RemoteInstanceRecord mRemoteInstanceRecord;

    @Nullable
    private String mTaskId;

    @Nullable
    private DateTime mScheduleDateTime;

    @Nullable
    private InstanceShownRecord mInstanceShownRecord;

    RemoteInstance(@NonNull DomainFactory domainFactory, @NonNull RemoteProject remoteProject, @NonNull RemoteInstanceRecord remoteInstanceRecord, @Nullable InstanceShownRecord instanceShownRecord, @NonNull ExactTimeStamp now) {
        super(domainFactory);

        mRemoteProject = remoteProject;
        mRemoteInstanceRecord = remoteInstanceRecord;
        mTaskId = null;
        mScheduleDateTime = null;
        mInstanceShownRecord = instanceShownRecord;

        Date date = getInstanceDate();
        ExactTimeStamp instanceTimeStamp = new ExactTimeStamp(date, getInstanceTime().getHourMinute(date.getDayOfWeek()).toHourMilli());
        if (mInstanceShownRecord != null && (mRemoteInstanceRecord.getDone() != null || (instanceTimeStamp.compareTo(now) > 0)))
            mInstanceShownRecord.setNotified(false);
    }

    public RemoteInstance(@NonNull DomainFactory domainFactory, @NonNull RemoteProject remoteProject, @NonNull String taskId, @NonNull DateTime scheduleDateTime, @Nullable InstanceShownRecord instanceShownRecord) {
        super(domainFactory);

        Assert.assertTrue(!TextUtils.isEmpty(taskId));

        mRemoteProject = remoteProject;
        mRemoteInstanceRecord = null;
        mTaskId = taskId;
        mScheduleDateTime = scheduleDateTime;
        mInstanceShownRecord = instanceShownRecord;
    }

    @NonNull
    private String getTaskId() {
        if (mRemoteInstanceRecord != null) {
            Assert.assertTrue(TextUtils.isEmpty(mTaskId));
            Assert.assertTrue(mScheduleDateTime == null);

            return mRemoteInstanceRecord.getTaskId();
        } else {
            Assert.assertTrue(!TextUtils.isEmpty(mTaskId));
            Assert.assertTrue(mScheduleDateTime != null);

            return mTaskId;
        }
    }

    @NonNull
    public Date getScheduleDate() {
        if (mRemoteInstanceRecord != null) {
            Assert.assertTrue(TextUtils.isEmpty(mTaskId));
            Assert.assertTrue(mScheduleDateTime == null);

            return new Date(mRemoteInstanceRecord.getScheduleYear(), mRemoteInstanceRecord.getScheduleMonth(), mRemoteInstanceRecord.getScheduleDay());
        } else {
            Assert.assertTrue(!TextUtils.isEmpty(mTaskId));
            Assert.assertTrue(mScheduleDateTime != null);

            return mScheduleDateTime.getDate();
        }
    }

    @NonNull
    @Override
    protected Time getScheduleTime() {
        if (mRemoteInstanceRecord != null) {
            Assert.assertTrue(TextUtils.isEmpty(mTaskId));
            Assert.assertTrue(mScheduleDateTime == null);

            String customTimeId = mRemoteInstanceRecord.getScheduleCustomTimeId();
            Integer hour = mRemoteInstanceRecord.getScheduleHour();
            Integer minute = mRemoteInstanceRecord.getScheduleMinute();

            Assert.assertTrue((hour == null) == (minute == null));
            Assert.assertTrue((customTimeId == null) != (hour == null));

            if (customTimeId != null) {
                return mRemoteProject.getRemoteCustomTime(customTimeId);
            } else {
                return new NormalTime(hour, minute);
            }
        } else {
            Assert.assertTrue(!TextUtils.isEmpty(mTaskId));
            Assert.assertTrue(mScheduleDateTime != null);

            return mScheduleDateTime.getTime();
        }
    }

    @NonNull
    @Override
    public TaskKey getTaskKey() {
        return getTask().getTaskKey();
    }

    @Nullable
    @Override
    public ExactTimeStamp getDone() {
        if (mRemoteInstanceRecord == null)
            return null;

        Long done = mRemoteInstanceRecord.getDone();
        if (done != null)
            return new ExactTimeStamp(done);
        else
            return null;
    }

    @Override
    public boolean exists() {
        Assert.assertTrue((mRemoteInstanceRecord == null) != (mScheduleDateTime == null));
        Assert.assertTrue((mTaskId == null) == (mScheduleDateTime == null));

        return (mRemoteInstanceRecord != null);
    }

    @NonNull
    public Date getInstanceDate() {
        if (mRemoteInstanceRecord != null) {
            Assert.assertTrue(mTaskId == null);
            Assert.assertTrue(mScheduleDateTime == null);

            if (mRemoteInstanceRecord.getInstanceYear() != null) {
                Assert.assertTrue(mRemoteInstanceRecord.getInstanceMonth() != null);
                Assert.assertTrue(mRemoteInstanceRecord.getInstanceDay() != null);

                return new Date(mRemoteInstanceRecord.getInstanceYear(), mRemoteInstanceRecord.getInstanceMonth(), mRemoteInstanceRecord.getInstanceDay());
            } else {
                Assert.assertTrue(mRemoteInstanceRecord.getInstanceMonth() == null);
                Assert.assertTrue(mRemoteInstanceRecord.getInstanceDay() == null);

                return getScheduleDate();
            }
        } else {
            Assert.assertTrue(mTaskId != null);
            Assert.assertTrue(mScheduleDateTime != null);

            return mScheduleDateTime.getDate();
        }
    }

    @NonNull
    @Override
    protected Time getInstanceTime() {
        if (mRemoteInstanceRecord != null) {
            Assert.assertTrue(mTaskId == null);
            Assert.assertTrue(mScheduleDateTime == null);

            Assert.assertTrue((mRemoteInstanceRecord.getInstanceHour() == null) == (mRemoteInstanceRecord.getInstanceMinute() == null));
            Assert.assertTrue((mRemoteInstanceRecord.getInstanceHour() == null) || (mRemoteInstanceRecord.getInstanceCustomTimeId() == null));

            if (mRemoteInstanceRecord.getInstanceCustomTimeId() != null) {
                return mRemoteProject.getRemoteCustomTime(mRemoteInstanceRecord.getInstanceCustomTimeId());
            } else if (mRemoteInstanceRecord.getInstanceHour() != null) {
                return new NormalTime(mRemoteInstanceRecord.getInstanceHour(), mRemoteInstanceRecord.getInstanceMinute());
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
    public String getName() {
        return getTask().getName();
    }

    @Override
    public void setInstanceDateTime(@NonNull Date date, @NonNull TimePair timePair, @NonNull ExactTimeStamp now) {
        Assert.assertTrue(isRootInstance(now));

        if (mRemoteInstanceRecord == null)
            createInstanceHierarchy(now);

        mRemoteInstanceRecord.setInstanceYear(date.getYear());
        mRemoteInstanceRecord.setInstanceMonth(date.getMonth());
        mRemoteInstanceRecord.setInstanceDay(date.getDay());

        if (timePair.mCustomTimeKey != null) {
            Assert.assertTrue(timePair.mHourMinute == null);
            mRemoteInstanceRecord.setInstanceCustomTimeId(getRemoteFactory().getRemoteCustomTimeId(timePair.mCustomTimeKey, mRemoteProject));
            mRemoteInstanceRecord.setInstanceHour(null);
            mRemoteInstanceRecord.setInstanceMinute(null);
        } else {
            Assert.assertTrue(timePair.mHourMinute != null);

            mRemoteInstanceRecord.setInstanceCustomTimeId(null);
            mRemoteInstanceRecord.setInstanceHour(timePair.mHourMinute.getHour());
            mRemoteInstanceRecord.setInstanceMinute(timePair.mHourMinute.getMinute());
        }

        if (mInstanceShownRecord == null)
            createInstanceShownRecord();

        Assert.assertTrue(mInstanceShownRecord != null);

        mInstanceShownRecord.setNotified(false);
    }

    @NonNull
    private RemoteProjectFactory getRemoteFactory() {
        RemoteProjectFactory remoteProjectFactory = getDomainFactory().getRemoteFactory();
        Assert.assertTrue(remoteProjectFactory != null);

        return remoteProjectFactory;
    }

    private void createInstanceShownRecord() {
        Assert.assertTrue(mInstanceShownRecord == null);

        mInstanceShownRecord = getDomainFactory().getLocalFactory().createInstanceShownRecord(getDomainFactory(), getTaskId(), getScheduleDateTime(), getTask().getRemoteProject().getId());
    }

    @Override
    public void createInstanceHierarchy(@NonNull ExactTimeStamp now) {
        Assert.assertTrue((mRemoteInstanceRecord == null) != (mScheduleDateTime == null));
        Assert.assertTrue((mTaskId == null) == (mScheduleDateTime == null));

        Instance parentInstance = getParentInstance(now);
        if (parentInstance != null)
            parentInstance.createInstanceHierarchy(now);

        if (mRemoteInstanceRecord == null)
            createInstanceRecord(now);
    }

    private void createInstanceRecord(@NonNull ExactTimeStamp now) {
        Task task = getTask();

        DateTime scheduleDateTime = getScheduleDateTime();

        RemoteProjectFactory remoteProjectFactory = getDomainFactory().getRemoteFactory();
        Assert.assertTrue(remoteProjectFactory != null);

        mRemoteInstanceRecord = ((RemoteTask) task).createRemoteInstanceRecord(this, scheduleDateTime, now);

        mTaskId = null;
        mScheduleDateTime = null;
    }

    @Override
    public void setNotificationShown(boolean notificationShown, @NonNull ExactTimeStamp now) {
        if (mInstanceShownRecord == null)
            createInstanceShownRecord();

        Assert.assertTrue(mInstanceShownRecord != null);

        mInstanceShownRecord.setNotificationShown(notificationShown);
    }

    @Override
    public void setDone(boolean done, @NonNull ExactTimeStamp now) {
        if (done) {
            if (mRemoteInstanceRecord == null)
                createInstanceHierarchy(now);

            Assert.assertTrue(mRemoteInstanceRecord != null);

            mRemoteInstanceRecord.setDone(now.getLong());

            if (mInstanceShownRecord != null)
                mInstanceShownRecord.setNotified(false);
        } else {
            Assert.assertTrue(mRemoteInstanceRecord != null);

            mRemoteInstanceRecord.setDone(null);
        }
    }

    @Override
    public void setNotified(@NonNull ExactTimeStamp now) {
        if (mInstanceShownRecord == null)
            createInstanceShownRecord();

        Assert.assertTrue(mInstanceShownRecord != null);

        mInstanceShownRecord.setNotified(true);
    }

    @Override
    public boolean getNotified() {
        return (mInstanceShownRecord != null && mInstanceShownRecord.getNotified());
    }

    @Override
    public void delete() {
        Assert.assertTrue(mRemoteInstanceRecord != null);

        RemoteProjectFactory remoteProjectFactory = getDomainFactory().getRemoteFactory();
        Assert.assertTrue(remoteProjectFactory != null);

        getTask().deleteInstance(this);

        mRemoteInstanceRecord.delete();
    }

    @Override
    public boolean getNotificationShown() {
        return (mInstanceShownRecord != null && mInstanceShownRecord.getNotificationShown());
    }

    @Nullable
    @Override
    public CustomTimeKey getScheduleCustomTimeKey() {
        if (mRemoteInstanceRecord != null) {
            Assert.assertTrue(TextUtils.isEmpty(mTaskId));
            Assert.assertTrue(mScheduleDateTime == null);

            String customTimeId = mRemoteInstanceRecord.getScheduleCustomTimeId();

            if (customTimeId != null) {
                return getDomainFactory().getCustomTimeKey(mRemoteProject.getId(), customTimeId);
            } else {
                return null;
            }
        } else {
            Assert.assertTrue(!TextUtils.isEmpty(mTaskId));
            Assert.assertTrue(mScheduleDateTime != null);

            return mScheduleDateTime.getTime().getTimePair().mCustomTimeKey;
        }
    }

    @Nullable
    @Override
    protected HourMinute getScheduleHourMinute() {
        if (mRemoteInstanceRecord != null) {
            Assert.assertTrue(TextUtils.isEmpty(mTaskId));
            Assert.assertTrue(mScheduleDateTime == null);

            Integer hour = mRemoteInstanceRecord.getScheduleHour();
            Integer minute = mRemoteInstanceRecord.getScheduleMinute();

            if (hour == null) {
                Assert.assertTrue(minute == null);

                return null;
            } else {
                Assert.assertTrue(minute != null);

                return new HourMinute(hour, minute);
            }
        } else {
            Assert.assertTrue(!TextUtils.isEmpty(mTaskId));
            Assert.assertTrue(mScheduleDateTime != null);

            return mScheduleDateTime.getTime().getTimePair().mHourMinute;
        }
    }

    @NonNull
    @Override
    public RemoteTask getTask() {
        return mRemoteProject.getRemoteTaskForce(getTaskId());
    }

    @Override
    public boolean belongsToRemoteProject() {
        return true;
    }

    @Nullable
    @Override
    public RemoteProject getRemoteNullableProject() {
        return getTask().getRemoteProject();
    }

    @NonNull
    @Override
    public RemoteProject getRemoteNonNullProject() {
        return getTask().getRemoteProject();
    }

    @Nullable
    @Override
    public Pair<String, String> getRemoteCustomTimeKey() {
        if (mRemoteInstanceRecord != null) {
            if (TextUtils.isEmpty(mRemoteInstanceRecord.getInstanceCustomTimeId()))
                return null;
            else
                return Pair.create(mRemoteProject.getId(), mRemoteInstanceRecord.getInstanceCustomTimeId());
        } else {
            return null; // scenario already covered by task/schedule relevance
        }
    }
}
