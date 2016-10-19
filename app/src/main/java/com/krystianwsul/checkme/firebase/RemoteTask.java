package com.krystianwsul.checkme.firebase;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.annimon.stream.Collectors;
import com.annimon.stream.Optional;
import com.annimon.stream.Stream;
import com.krystianwsul.checkme.domainmodel.DomainFactory;
import com.krystianwsul.checkme.domainmodel.MergedInstance;
import com.krystianwsul.checkme.domainmodel.MergedSchedule;
import com.krystianwsul.checkme.domainmodel.MergedTask;
import com.krystianwsul.checkme.domainmodel.MergedTaskHierarchy;
import com.krystianwsul.checkme.firebase.records.RemoteTaskRecord;
import com.krystianwsul.checkme.utils.TaskKey;
import com.krystianwsul.checkme.utils.time.Date;
import com.krystianwsul.checkme.utils.time.ExactTimeStamp;
import com.krystianwsul.checkme.utils.time.HourMilli;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public class RemoteTask implements MergedTask {
    @NonNull
    private final DomainFactory mDomainFactory;

    @NonNull
    private final RemoteTaskRecord mRemoteTaskRecord;

    RemoteTask(@NonNull DomainFactory domainFactory, @NonNull RemoteTaskRecord remoteTaskRecord) {
        mDomainFactory = domainFactory;
        mRemoteTaskRecord = remoteTaskRecord;
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
    private Collection<RemoteSchedule> getRemoteSchedules() {
        if (mDomainFactory.getRemoteFactory().mRemoteSchedules.containsKey(mRemoteTaskRecord.getId()))
            return mDomainFactory.getRemoteFactory().mRemoteSchedules.get(mRemoteTaskRecord.getId());
        else
            return new ArrayList<>();
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
    @Override
    public ExactTimeStamp getEndExactTimeStamp() {
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
        return new TaskKey(mRemoteTaskRecord.getId());
    }

    @Nullable
    public MergedTask getParentTask(@NonNull ExactTimeStamp exactTimeStamp) {
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
    Set<String> getRecordOf() {
        return mRemoteTaskRecord.getRecordOf();
    }

    public void setEndExactTimeStamp(@NonNull ExactTimeStamp now) {
        Assert.assertTrue(current(now));

        List<RemoteSchedule> schedules = getCurrentSchedules(now);
        if (isRootTask(now)) {
            Assert.assertTrue(Stream.of(schedules)
                    .allMatch(schedule -> schedule.current(now)));

            for (RemoteSchedule schedule : schedules)
                schedule.setEndExactTimeStamp(now);
        } else {
            Assert.assertTrue(schedules.isEmpty());
        }

        for (MergedTask childTask : getChildTasks(now)) {
            Assert.assertTrue(childTask != null);
            Assert.assertTrue(childTask instanceof RemoteTask);
            childTask.setEndExactTimeStamp(now);
        }

        MergedTaskHierarchy parentTaskHierarchy = mDomainFactory.getParentTaskHierarchy(this, now);
        if (parentTaskHierarchy != null) {
            Assert.assertTrue(parentTaskHierarchy.current(now));

            parentTaskHierarchy.setEndExactTimeStamp(now);
        }

        mRemoteTaskRecord.setEndTime(now.getLong());
    }

    @NonNull
    public String getId() {
        return mRemoteTaskRecord.getId();
    }

    @Override
    public void createChildTask(@NonNull ExactTimeStamp now, @NonNull String name, @Nullable String note) {
        mDomainFactory.getRemoteFactory().createChildTask(mDomainFactory, this, now, name, note);
    }

    @Nullable
    @Override
    public Date getOldestVisible() {
        if (mRemoteTaskRecord.getOldestVisibleYear() != null) {
            Assert.assertTrue(mRemoteTaskRecord.getOldestVisibleMonth() != null);
            Assert.assertTrue(mRemoteTaskRecord.getOldestVisibleDay() != null);

            return new Date(mRemoteTaskRecord.getOldestVisibleYear(), mRemoteTaskRecord.getOldestVisibleMonth(), mRemoteTaskRecord.getOldestVisibleDay());
        } else {
            Assert.assertTrue(mRemoteTaskRecord.getOldestVisibleMonth() == null);
            Assert.assertTrue(mRemoteTaskRecord.getOldestVisibleDay() == null);

            return null;
        }
    }

    public void updateOldestVisible(@NonNull ExactTimeStamp now) {
        // 24 hack
        List<MergedInstance> instances = mDomainFactory.getPastInstances(this, now);

        Optional<MergedInstance> optional = Stream.of(instances)
                .filter(instance -> instance.isVisible(now))
                .min((lhs, rhs) -> lhs.getScheduleDateTime().compareTo(rhs.getScheduleDateTime()));

        Date oldestVisible;

        if (optional.isPresent()) {
            oldestVisible = optional.get().getScheduleDateTime().getDate();

            if (oldestVisible.compareTo(now.getDate()) > 0)
                oldestVisible = now.getDate();
        } else {
            oldestVisible = now.getDate();
        }

        mRemoteTaskRecord.setOldestVisibleYear(oldestVisible.getYear());
        mRemoteTaskRecord.setOldestVisibleMonth(oldestVisible.getMonth());
        mRemoteTaskRecord.setOldestVisibleDay(oldestVisible.getDay());
    }

    @NonNull
    @Override
    public List<MergedInstance> getInstances(@Nullable ExactTimeStamp startExactTimeStamp, @NonNull ExactTimeStamp endExactTimeStamp, @NonNull ExactTimeStamp now) {
        if (startExactTimeStamp == null) { // 24 hack
            Date oldestVisible = getOldestVisible();
            if (oldestVisible != null) {
                HourMilli zero = new HourMilli(0, 0, 0, 0);
                startExactTimeStamp = new ExactTimeStamp(oldestVisible, zero);
            }
        }

        List<MergedInstance> instances = new ArrayList<>();
        for (RemoteSchedule schedule : getRemoteSchedules())
            instances.addAll(schedule.getInstances(this, startExactTimeStamp, endExactTimeStamp));

        List<MergedTaskHierarchy> taskHierarchies = mDomainFactory.getParentTaskHierarchies(this);

        ExactTimeStamp finalStartExactTimeStamp = startExactTimeStamp;

        instances.addAll(Stream.of(taskHierarchies)
                .map(MergedTaskHierarchy::getParentTask)
                .map(task -> task.getInstances(finalStartExactTimeStamp, endExactTimeStamp, now))
                .flatMap(Stream::of)
                .map(instance -> instance.getChildInstances(now))
                .flatMap(Stream::of)
                .filter(instance -> instance.getTaskKey().equals(getTaskKey()))
                .collect(Collectors.toList()));

        return instances;
    }

    @Override
    public void setRelevant() {
        mRemoteTaskRecord.delete();
    }

    @Override
    public void setName(@NonNull String name, @Nullable String note) {
        Assert.assertTrue(!TextUtils.isEmpty(name));

        mRemoteTaskRecord.setName(name);
        mRemoteTaskRecord.setNote(note);
    }
}
