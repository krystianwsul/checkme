package com.krystianwsul.checkme.domainmodel;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.annimon.stream.Collectors;
import com.annimon.stream.Optional;
import com.annimon.stream.Stream;
import com.krystianwsul.checkme.persistencemodel.TaskRecord;
import com.krystianwsul.checkme.utils.ScheduleType;
import com.krystianwsul.checkme.utils.time.Date;
import com.krystianwsul.checkme.utils.time.ExactTimeStamp;
import com.krystianwsul.checkme.utils.time.HourMilli;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.List;

public class Task {
    @NonNull
    private final DomainFactory mDomainFactory;

    @NonNull
    private final TaskRecord mTaskRecord;

    @NonNull
    private final ArrayList<Schedule> mSchedules = new ArrayList<>();

    Task(@NonNull DomainFactory domainFactory, @NonNull TaskRecord taskRecord) {
        mDomainFactory = domainFactory;
        mTaskRecord = taskRecord;
    }

    void addSchedules(@NonNull List<Schedule> schedules) {
        mSchedules.addAll(schedules);
    }

    @Nullable
    String getScheduleText(@NonNull Context context, @NonNull ExactTimeStamp exactTimeStamp) {
        Assert.assertTrue(current(exactTimeStamp));

        List<Schedule> currentSchedules = getCurrentSchedules(exactTimeStamp);

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

    @NonNull
    List<Schedule> getCurrentSchedules(@NonNull ExactTimeStamp exactTimeStamp) {
        Assert.assertTrue(current(exactTimeStamp));

        return Stream.of(mSchedules)
                .filter(schedule -> schedule.current(exactTimeStamp))
                .collect(Collectors.toList());
    }

    @NonNull
    public String getName() {
        return mTaskRecord.getName();
    }

    void setName(@NonNull String name, @Nullable String note) {
        Assert.assertTrue(!TextUtils.isEmpty(name));

        mTaskRecord.setName(name);
        mTaskRecord.setNote(note);
    }

    @NonNull
    List<Task> getChildTasks(@NonNull ExactTimeStamp exactTimeStamp) {
        Assert.assertTrue(current(exactTimeStamp));

        return mDomainFactory.getChildTasks(this, exactTimeStamp);
    }

    @Nullable
    Task getParentTask(@NonNull ExactTimeStamp exactTimeStamp) {
        Assert.assertTrue(current(exactTimeStamp));

        return mDomainFactory.getParentTask(this, exactTimeStamp);
    }

    public int getId() {
        return mTaskRecord.getId();
    }

    boolean isRootTask(@NonNull ExactTimeStamp exactTimeStamp) {
        Assert.assertTrue(current(exactTimeStamp));

        return (getParentTask(exactTimeStamp) == null);
    }

    @NonNull
    private ExactTimeStamp getStartExactTimeStamp() {
        return new ExactTimeStamp(mTaskRecord.getStartTime());
    }

    @Nullable
    ExactTimeStamp getEndExactTimeStamp() {
        if (mTaskRecord.getEndTime() != null)
            return new ExactTimeStamp(mTaskRecord.getEndTime());
        else
            return null;
    }

    void setEndExactTimeStamp(@NonNull ExactTimeStamp endExactTimeStamp) {
        Assert.assertTrue(current(endExactTimeStamp));

        if (isRootTask(endExactTimeStamp)) {
            List<Schedule> schedules = getCurrentSchedules(endExactTimeStamp);

            Assert.assertTrue(Stream.of(schedules)
                    .allMatch(schedule -> schedule.current(endExactTimeStamp)));

            Stream.of(schedules)
                    .forEach(schedule -> schedule.setEndExactTimeStamp(endExactTimeStamp));
        } else {
            Assert.assertTrue(getCurrentSchedules(endExactTimeStamp).isEmpty());
        }

        for (Task childTask : getChildTasks(endExactTimeStamp)) {
            Assert.assertTrue(childTask != null);
            childTask.setEndExactTimeStamp(endExactTimeStamp);
        }

        mDomainFactory.setParentHierarchyEndTimeStamp(this, endExactTimeStamp);

        mTaskRecord.setEndTime(endExactTimeStamp.getLong());
    }

    public boolean current(@NonNull ExactTimeStamp exactTimeStamp) {
        ExactTimeStamp startExactTimeStamp = getStartExactTimeStamp();
        ExactTimeStamp endExactTimeStamp = getEndExactTimeStamp();

        return (startExactTimeStamp.compareTo(exactTimeStamp) <= 0 && (endExactTimeStamp == null || endExactTimeStamp.compareTo(exactTimeStamp) > 0));
    }

    boolean notDeleted(@NonNull ExactTimeStamp exactTimeStamp) {
        ExactTimeStamp endExactTimeStamp = getEndExactTimeStamp();

        return (endExactTimeStamp == null || endExactTimeStamp.compareTo(exactTimeStamp) > 0);
    }

    @Nullable
    Date getOldestVisible() {
        if (mTaskRecord.getOldestVisibleYear() != null) {
            Assert.assertTrue(mTaskRecord.getOldestVisibleMonth() != null);
            Assert.assertTrue(mTaskRecord.getOldestVisibleDay() != null);

            return new Date(mTaskRecord.getOldestVisibleYear(), mTaskRecord.getOldestVisibleMonth(), mTaskRecord.getOldestVisibleDay());
        } else {
            Assert.assertTrue(mTaskRecord.getOldestVisibleMonth() == null);
            Assert.assertTrue(mTaskRecord.getOldestVisibleDay() == null);

            return null;
        }
    }

    @NonNull
    List<Instance> getInstances(@Nullable ExactTimeStamp startExactTimeStamp, @NonNull ExactTimeStamp endExactTimeStamp, @NonNull ExactTimeStamp now) {
        if (startExactTimeStamp == null) { // 24 hack
            Date oldestVisible = getOldestVisible();
            if (oldestVisible != null) {
                HourMilli zero = new HourMilli(0, 0, 0, 0);
                startExactTimeStamp = new ExactTimeStamp(oldestVisible, zero);
            }
        }

        List<Instance> instances = new ArrayList<>();
        for (Schedule schedule : mSchedules)
            instances.addAll(schedule.getInstances(this, startExactTimeStamp, endExactTimeStamp));

        List<TaskHierarchy> taskHierarchies = mDomainFactory.getParentTaskHierarchies(this);

        ExactTimeStamp finalStartExactTimeStamp = startExactTimeStamp;

        instances.addAll(Stream.of(taskHierarchies)
                .map(TaskHierarchy::getParentTask)
                .map(task -> task.getInstances(finalStartExactTimeStamp, endExactTimeStamp, now))
                .flatMap(Stream::of)
                .map(instance -> instance.getChildInstances(now))
                .flatMap(Stream::of)
                .filter(instance -> instance.getTaskId() == getId())
                .collect(Collectors.toList()));

        return instances;
    }

    void updateOldestVisible(@NonNull ExactTimeStamp now) {
        // 24 hack
        List<Instance> instances = mDomainFactory.getPastInstances(this, now);

        Optional<Instance> optional = Stream.of(instances)
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

        mTaskRecord.setOldestVisibleYear(oldestVisible.getYear());
        mTaskRecord.setOldestVisibleMonth(oldestVisible.getMonth());
        mTaskRecord.setOldestVisibleDay(oldestVisible.getDay());
    }

    @NonNull
    private Task getRootTask(@NonNull ExactTimeStamp exactTimeStamp) {
        Task parentTask = getParentTask(exactTimeStamp);
        if (parentTask == null)
            return this;
        else
            return parentTask.getRootTask(exactTimeStamp);
    }

    boolean isVisible(@NonNull ExactTimeStamp now) {
        if (current(now)) {
            Task rootTask = getRootTask(now);

            List<Schedule> schedules = rootTask.getCurrentSchedules(now);

            if (schedules.isEmpty()) {
                return true;
            }

            if (Stream.of(schedules).anyMatch(schedule -> schedule.getType() != ScheduleType.SINGLE)) {
                return true;
            }

            if (Stream.of(schedules)
                    .map(schedule -> (SingleSchedule) schedule)
                    .anyMatch(schedule -> schedule.getInstance(this).isVisible(now))) {
                return true;
            }
        }

        return false;
    }

    void setRelevant() {
        mTaskRecord.setRelevant(false);
    }

    @Nullable
    String getNote() {
        return mTaskRecord.getNote();
    }
}
