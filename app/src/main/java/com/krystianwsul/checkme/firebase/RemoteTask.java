package com.krystianwsul.checkme.firebase;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.krystianwsul.checkme.domainmodel.DomainFactory;
import com.krystianwsul.checkme.domainmodel.MergedSchedule;
import com.krystianwsul.checkme.domainmodel.MergedTask;
import com.krystianwsul.checkme.domainmodel.MergedTaskHierarchy;
import com.krystianwsul.checkme.utils.TaskKey;
import com.krystianwsul.checkme.utils.time.ExactTimeStamp;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class RemoteTask implements MergedTask {
    @NonNull
    private final DomainFactory mDomainFactory;

    @NonNull
    private final String mId;

    @NonNull
    private final RemoteTaskRecord mRemoteTaskRecord;

    @NonNull
    private final Set<String> mTaskOf;

    public RemoteTask(@NonNull DomainFactory domainFactory, @NonNull String id, @NonNull TaskWrapper taskWrapper) {
        Assert.assertTrue(!TextUtils.isEmpty(id));
        Assert.assertTrue(taskWrapper.taskRecord != null);
        Assert.assertTrue(taskWrapper.taskHierarchyRecord == null);

        mDomainFactory = domainFactory;
        mId = id;
        mRemoteTaskRecord = taskWrapper.taskRecord;

        mTaskOf = taskWrapper.taskOf.keySet();
        Assert.assertTrue(!mTaskOf.isEmpty());
    }

    @NonNull
    @Override
    public String getName() {
        return mRemoteTaskRecord.getName();
    }

    @NonNull
    public List<RemoteSchedule> getCurrentSchedules(@NonNull ExactTimeStamp exactTimeStamp) {
        Assert.assertTrue(current(exactTimeStamp));

        return Stream.of(getRemoteSchedules())
                .filter(schedule -> schedule.current(exactTimeStamp))
                .collect(Collectors.toList());
    }

    @NonNull
    private List<RemoteSchedule> getRemoteSchedules() {
        List<RemoteSchedule> remoteSchedules = new ArrayList<>();

        List<RemoteSingleScheduleRecord> singleScheduleRecords = mRemoteTaskRecord.getSingleScheduleRecords();
        for (int i = 0; i < singleScheduleRecords.size(); i++) {
            remoteSchedules.add(new RemoteSingleSchedule(i, singleScheduleRecords.get(i)));
        }

        List<RemoteDailyScheduleRecord> dailyScheduleRecords = mRemoteTaskRecord.getDailyScheduleRecords();
        for (int i = 0; i < dailyScheduleRecords.size(); i++) {
            remoteSchedules.add(new RemoteDailySchedule(i, dailyScheduleRecords.get(i)));
        }

        List<RemoteWeeklyScheduleRecord> weeklyScheduleRecords = mRemoteTaskRecord.getWeeklyScheduleRecords();
        for (int i = 0; i < weeklyScheduleRecords.size(); i++) {
            remoteSchedules.add(new RemoteWeeklySchedule(i, weeklyScheduleRecords.get(i)));
        }

        List<RemoteMonthlyDayScheduleRecord> monthlyDayScheduleRecords = mRemoteTaskRecord.getMonthlyDayScheduleRecords();
        for (int i = 0; i < monthlyDayScheduleRecords.size(); i++) {
            remoteSchedules.add(new RemoteMonthlyDaySchedule(i, monthlyDayScheduleRecords.get(i)));
        }

        List<RemoteMonthlyWeekScheduleRecord> monthlyWeekScheduleRecords = mRemoteTaskRecord.getMonthlyWeekScheduleRecords();
        for (int i = 0; i < monthlyWeekScheduleRecords.size(); i++) {
            remoteSchedules.add(new RemoteMonthlyWeekSchedule(i, monthlyWeekScheduleRecords.get(i)));
        }

        return remoteSchedules;
    }

    @Override
    public boolean current(@NonNull ExactTimeStamp exactTimeStamp) {
        ExactTimeStamp startExactTimeStamp = getStartExactTimeStamp();
        ExactTimeStamp endExactTimeStamp = getEndExactTimeStamp();

        return (startExactTimeStamp.compareTo(exactTimeStamp) <= 0 && (endExactTimeStamp == null || endExactTimeStamp.compareTo(exactTimeStamp) > 0));
    }

    @NonNull
    @Override
    public ExactTimeStamp getStartExactTimeStamp() {
        return new ExactTimeStamp(mRemoteTaskRecord.getStartTime());
    }

    @Nullable
    ExactTimeStamp getEndExactTimeStamp() {
        if (mRemoteTaskRecord.getEndTime() != null)
            return new ExactTimeStamp(mRemoteTaskRecord.getEndTime());
        else
            return null;
    }

    @Nullable
    @Override
    public String getScheduleText(@NonNull Context context, @NonNull ExactTimeStamp exactTimeStamp) {
        Assert.assertTrue(current(exactTimeStamp));

        List<RemoteSchedule> currentSchedules = getCurrentSchedules(exactTimeStamp);

        if (isRootTask(exactTimeStamp)) {
            if (currentSchedules.isEmpty())
                return null;

            Assert.assertTrue(Stream.of(currentSchedules)
                    .allMatch(schedule -> schedule.current(exactTimeStamp)));

            return Stream.of(currentSchedules)
                    .map(schedule -> schedule.getScheduleText(context))
                    .collect(Collectors.joining(", "));
        } else {
            Assert.assertTrue(currentSchedules.isEmpty());
            return null;
        }
    }

    public boolean isRootTask(@NonNull ExactTimeStamp exactTimeStamp) {
        Assert.assertTrue(current(exactTimeStamp));

        return (getParentTask(exactTimeStamp) == null);
    }

    @Nullable
    @Override
    public String getNote() {
        return mRemoteTaskRecord.getNote();
    }

    @NonNull
    @Override
    public TaskKey getTaskKey() {
        return new TaskKey(mId);
    }

    @Nullable
    MergedTask getParentTask(@NonNull ExactTimeStamp exactTimeStamp) {
        Assert.assertTrue(current(exactTimeStamp));

        return mDomainFactory.getParentTask(this, exactTimeStamp);
    }

    @NonNull
    @Override
    public List<MergedTask> getChildTasks(@NonNull ExactTimeStamp exactTimeStamp) {
        Assert.assertTrue(current(exactTimeStamp));

        return mDomainFactory.getChildTasks(this, exactTimeStamp);
    }

    @Override
    public boolean notDeleted(@NonNull ExactTimeStamp exactTimeStamp) {
        ExactTimeStamp endExactTimeStamp = getEndExactTimeStamp();

        return (endExactTimeStamp == null || endExactTimeStamp.compareTo(exactTimeStamp) > 0);
    }

    @NonNull
    public MergedTask getRootTask(@NonNull ExactTimeStamp exactTimeStamp) {
        MergedTask parentTask = getParentTask(exactTimeStamp);
        if (parentTask == null)
            return this;
        else
            return parentTask.getRootTask(exactTimeStamp);
    }

    @Override
    public boolean isVisible(@NonNull ExactTimeStamp now) {
        if (current(now)) {
            MergedTask rootTask = getRootTask(now);

            List<? extends MergedSchedule> schedules = rootTask.getCurrentSchedules(now);

            if (schedules.isEmpty()) {
                return true;
            }

            if (Stream.of(schedules).anyMatch(schedule -> schedule.isVisible(this, now))) {
                return true;
            }
        }

        return false;
    }

    @NonNull
    public Set<String> getTaskOf() {
        return mTaskOf;
    }

    public void setEndExactTimeStamp(@NonNull Map<String, Object> values, @NonNull ExactTimeStamp now) {
        List<RemoteSchedule> schedules = getCurrentSchedules(now);
        if (isRootTask(now)) {
            Assert.assertTrue(Stream.of(schedules)
                    .allMatch(schedule -> schedule.current(now)));

            for (RemoteSchedule schedule : schedules)
                values.put("tasks/" + mId + "/taskRecord/" + schedule.getPath() + "/endTime", now.getLong());
        } else {
            Assert.assertTrue(schedules.isEmpty());
        }

        for (MergedTask childTask : getChildTasks(now)) {
            Assert.assertTrue(childTask != null);
            Assert.assertTrue(childTask instanceof RemoteTask);
            ((RemoteTask) childTask).setEndExactTimeStamp(values, now);
        }

        MergedTaskHierarchy parentTaskHierarchy = mDomainFactory.getParentTaskHierarchy(this, now);
        if (parentTaskHierarchy != null) {
            Assert.assertTrue(parentTaskHierarchy.current(now));
            Assert.assertTrue(parentTaskHierarchy instanceof RemoteTaskHierarchy);
            ((RemoteTaskHierarchy) parentTaskHierarchy).setEndExactTimeStamp(values, now);
        }

        values.put("tasks/" + mId + "/taskRecord/endTime", now.getLong());
    }
}
