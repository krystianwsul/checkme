package com.krystianwsul.checkme.firebase;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.krystianwsul.checkme.domainmodel.CustomTime;
import com.krystianwsul.checkme.domainmodel.DomainFactory;
import com.krystianwsul.checkme.domainmodel.MergedInstance;
import com.krystianwsul.checkme.domainmodel.MergedTask;
import com.krystianwsul.checkme.domainmodel.MergedTaskHierarchy;
import com.krystianwsul.checkme.firebase.records.RemoteInstanceRecord;
import com.krystianwsul.checkme.persistencemodel.InstanceShownRecord;
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

public class RemoteInstance implements MergedInstance {
    @NonNull
    private final DomainFactory mDomainFactory;

    @Nullable
    private RemoteInstanceRecord mRemoteInstanceRecord;

    @Nullable
    private String mTaskId;

    @Nullable
    private DateTime mScheduleDateTime;

    @Nullable
    private InstanceShownRecord mInstanceShownRecord;

    public RemoteInstance(@NonNull DomainFactory domainFactory, @NonNull RemoteInstanceRecord remoteInstanceRecord, @Nullable InstanceShownRecord instanceShownRecord) {
        mDomainFactory = domainFactory;
        mRemoteInstanceRecord = remoteInstanceRecord;
        mTaskId = null;
        mScheduleDateTime = null;
        mInstanceShownRecord = instanceShownRecord;
    }

    public RemoteInstance(@NonNull DomainFactory domainFactory, @NonNull String taskId, @NonNull DateTime scheduleDateTime, @Nullable InstanceShownRecord instanceShownRecord) {
        Assert.assertTrue(!TextUtils.isEmpty(taskId));

        mDomainFactory = domainFactory;
        mRemoteInstanceRecord = null;
        mTaskId = taskId;
        mScheduleDateTime = scheduleDateTime;
        mInstanceShownRecord = instanceShownRecord;
    }

    @NonNull
    public InstanceKey getInstanceKey() {
        return new InstanceKey(new TaskKey(getTaskId()), getScheduleDate(), getScheduleCustomTimeId(), getScheduleHourMinute());
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
    private Time getScheduleTime() {
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
    public DateTime getScheduleDateTime() {
        return new DateTime(getScheduleDate(), getScheduleTime());
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
    @Override
    public DateTime getInstanceDateTime() {
        return new DateTime(getInstanceDate(), getInstanceTime());
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
    private Time getInstanceTime() {
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
    @Override
    public RemoteTask getTask() {
        MergedTask task = mDomainFactory.getTask(getTaskKey());
        Assert.assertTrue(task instanceof RemoteTask);

        return (RemoteTask) task;
    }

    @NonNull
    @Override
    public List<MergedInstance> getChildInstances(@NonNull ExactTimeStamp now) {
        ExactTimeStamp hierarchyExactTimeStamp = getHierarchyExactTimeStamp(now);

        MergedTask task = getTask();

        DateTime scheduleDateTime = getScheduleDateTime();

        List<MergedTaskHierarchy> taskHierarchies = mDomainFactory.getChildTaskHierarchies(task);
        HashSet<MergedInstance> childInstances = new HashSet<>();
        for (MergedTaskHierarchy taskHierarchy : taskHierarchies) {
            Assert.assertTrue(taskHierarchy != null);

            MergedTask childTask = taskHierarchy.getChildTask();

            MergedInstance existingChildInstance = mDomainFactory.getExistingInstance(childTask, scheduleDateTime);
            if (existingChildInstance != null) {
                childInstances.add(existingChildInstance);
            } else if (taskHierarchy.notDeleted(hierarchyExactTimeStamp) && taskHierarchy.getChildTask().notDeleted(hierarchyExactTimeStamp)) {
                childInstances.add(mDomainFactory.getInstance(childTask, scheduleDateTime));
            }
        }

        return new ArrayList<>(childInstances);
    }

    @NonNull
    private ExactTimeStamp getHierarchyExactTimeStamp(@NonNull ExactTimeStamp now) {
        ArrayList<ExactTimeStamp> exactTimeStamps = new ArrayList<>();

        exactTimeStamps.add(now);

        MergedTask task = getTask();

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
    public boolean isRootInstance(@NonNull ExactTimeStamp now) {
        return getTask().isRootTask(getHierarchyExactTimeStamp(now));
    }

    @NonNull
    @Override
    public TimePair getInstanceTimePair() {
        return new TimePair(getInstanceCustomTimeId(), getInstanceHourMinute());
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

    @Nullable
    @Override
    public String getDisplayText(@NonNull Context context, @NonNull ExactTimeStamp now) {
        if (isRootInstance(now)) {
            return getInstanceDateTime().getDisplayText(context);
        } else {
            return null;
        }
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

        mInstanceShownRecord = mDomainFactory.createInstanceShownRecord(getTask(), getScheduleDateTime());
    }

    @Override
    public void createInstanceHierarchy(@NonNull ExactTimeStamp now) {
        Assert.assertTrue((mRemoteInstanceRecord == null) != (mScheduleDateTime == null));
        Assert.assertTrue((mTaskId == null) == (mScheduleDateTime == null));

        MergedInstance parentInstance = getParentInstance(now);
        if (parentInstance != null)
            parentInstance.createInstanceHierarchy(now);

        if (mRemoteInstanceRecord == null)
            createInstanceRecord(now);
    }

    @Nullable
    public MergedInstance getParentInstance(@NonNull ExactTimeStamp now) {
        ExactTimeStamp hierarchyExactTimeStamp = getHierarchyExactTimeStamp(now);

        MergedTask task = getTask();

        MergedTask parentTask = task.getParentTask(hierarchyExactTimeStamp);

        if (parentTask == null)
            return null;

        Assert.assertTrue(parentTask.current(hierarchyExactTimeStamp));

        return mDomainFactory.getInstance(parentTask, getScheduleDateTime());
    }

    private void createInstanceRecord(@NonNull ExactTimeStamp now) {
        RemoteTask task = getTask();

        DateTime scheduleDateTime = getScheduleDateTime();

        mTaskId = null;
        mScheduleDateTime = null;

        mRemoteInstanceRecord = mDomainFactory.getRemoteFactory().createRemoteInstanceRecord(task, this, scheduleDateTime, now);
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
    public boolean isVisible(@NonNull ExactTimeStamp now) {
        boolean isVisible = isVisibleHelper(now);

        if (isVisible) {
            MergedTask task = getTask();

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

        MergedInstance parentInstance = getParentInstance(now);
        if (parentInstance == null) {
            ExactTimeStamp done = getDone();
            return (done == null || (done.compareTo(twentyFourHoursAgo) > 0));
        } else {
            return parentInstance.isVisible(now);
        }
    }

    @Override
    public boolean getNotified() {
        return (mInstanceShownRecord != null && mInstanceShownRecord.getNotified());
    }

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
        hash += 12 * 31 * 73 * 24 * 60 * 10000 * getTaskId().hashCode();

        return hash;
    }

    @NonNull
    @Override
    public TimePair getScheduleTimePair() {
        return new TimePair(getScheduleCustomTimeId(), getScheduleHourMinute());
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
}
