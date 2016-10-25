package com.krystianwsul.checkme.firebase;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.krystianwsul.checkme.domainmodel.DomainFactory;
import com.krystianwsul.checkme.domainmodel.Instance;
import com.krystianwsul.checkme.domainmodel.Task;
import com.krystianwsul.checkme.firebase.records.RemoteInstanceRecord;
import com.krystianwsul.checkme.persistencemodel.InstanceShownRecord;
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
import java.util.Set;

public class RemoteInstance extends Instance {
    @Nullable
    private RemoteInstanceRecord mRemoteInstanceRecord;

    @Nullable
    private String mTaskId;

    @Nullable
    private DateTime mScheduleDateTime;

    @Nullable
    private InstanceShownRecord mInstanceShownRecord;

    public RemoteInstance(@NonNull DomainFactory domainFactory, @NonNull RemoteInstanceRecord remoteInstanceRecord, @Nullable InstanceShownRecord instanceShownRecord) {
        super(domainFactory);

        mRemoteInstanceRecord = remoteInstanceRecord;
        mTaskId = null;
        mScheduleDateTime = null;
        mInstanceShownRecord = instanceShownRecord;
    }

    public RemoteInstance(@NonNull DomainFactory domainFactory, @NonNull String taskId, @NonNull DateTime scheduleDateTime, @Nullable InstanceShownRecord instanceShownRecord) {
        super(domainFactory);

        Assert.assertTrue(!TextUtils.isEmpty(taskId));

        mRemoteInstanceRecord = null;
        mTaskId = taskId;
        mScheduleDateTime = scheduleDateTime;
        mInstanceShownRecord = instanceShownRecord;
    }

    @NonNull
    public String getTaskId() {
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

            Integer customTimeId = mRemoteInstanceRecord.getScheduleCustomTimeId();
            Integer hour = mRemoteInstanceRecord.getScheduleHour();
            Integer minute = mRemoteInstanceRecord.getScheduleMinute();

            Assert.assertTrue((hour == null) == (minute == null));
            Assert.assertTrue((customTimeId == null) != (hour == null));

            if (customTimeId != null) {
                return mDomainFactory.getCustomTime(customTimeId); // todo customtime
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
        return new TaskKey(getTaskId());
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
                return mDomainFactory.getCustomTime(mRemoteInstanceRecord.getInstanceCustomTimeId());
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

        if (mRemoteInstanceRecord != null)
            exactTimeStamps.add(new ExactTimeStamp(mRemoteInstanceRecord.getHierarchyTime()));

        return Collections.min(exactTimeStamps);
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

        if (timePair.mCustomTimeId != null) {
            Assert.assertTrue(timePair.mHourMinute == null);
            mRemoteInstanceRecord.setInstanceCustomTimeId(timePair.mCustomTimeId);
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

    private void createInstanceShownRecord() {
        Assert.assertTrue(mInstanceShownRecord == null);

        mInstanceShownRecord = mDomainFactory.getLocalFactory().createInstanceShownRecord(((RemoteTask) getTask()).getId(), getScheduleDateTime());
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

        mTaskId = null;
        mScheduleDateTime = null;

        RemoteFactory remoteFactory = mDomainFactory.getRemoteFactory();
        Assert.assertTrue(remoteFactory != null);

        mRemoteInstanceRecord = remoteFactory.createRemoteInstanceRecord((RemoteTask) task, this, scheduleDateTime, now);
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
        } else {
            Assert.assertTrue(mRemoteInstanceRecord != null);

            mRemoteInstanceRecord.setDone(null);

            if (mInstanceShownRecord != null)
                mInstanceShownRecord.setNotified(false);
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
    public void setRelevant() {
        Assert.assertTrue(mRemoteInstanceRecord != null);

        mRemoteInstanceRecord.delete();
    }

    @NonNull
    public String getId() {
        Assert.assertTrue(mRemoteInstanceRecord != null);

        return mRemoteInstanceRecord.getId();
    }

    @Override
    public boolean getNotificationShown() {
        return (mInstanceShownRecord != null && mInstanceShownRecord.getNotificationShown());
    }

    void updateRecordOf(@NonNull Set<String> addedFriends, @NonNull Set<String> removedFriends) {
        Assert.assertTrue(mRemoteInstanceRecord != null);

        mRemoteInstanceRecord.updateRecordOf(addedFriends, removedFriends);
    }
}
