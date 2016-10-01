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

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class Task {
    private final WeakReference<DomainFactory> mDomainFactoryReference;

    private final TaskRecord mTaskRecord;
    private final ArrayList<Schedule> mSchedules = new ArrayList<>();

    Task(DomainFactory domainFactory, TaskRecord taskRecord) {
        Assert.assertTrue(domainFactory != null);
        Assert.assertTrue(taskRecord != null);

        mDomainFactoryReference = new WeakReference<>(domainFactory);
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

    public String getName() {
        return mTaskRecord.getName();
    }

    void setName(@NonNull String name, @Nullable String note) {
        Assert.assertTrue(!TextUtils.isEmpty(name));

        mTaskRecord.setName(name);
        mTaskRecord.setNote(note);
    }

    List<Task> getChildTasks(ExactTimeStamp exactTimeStamp) {
        Assert.assertTrue(exactTimeStamp != null);
        Assert.assertTrue(current(exactTimeStamp));

        DomainFactory domainFactory = mDomainFactoryReference.get();
        Assert.assertTrue(domainFactory != null);

        return domainFactory.getChildTasks(this, exactTimeStamp);
    }

    Task getParentTask(ExactTimeStamp exactTimeStamp) {
        Assert.assertTrue(exactTimeStamp != null);
        Assert.assertTrue(current(exactTimeStamp));

        DomainFactory domainFactory = mDomainFactoryReference.get();
        Assert.assertTrue(domainFactory != null);

        return domainFactory.getParentTask(this, exactTimeStamp);
    }

    public int getId() {
        return mTaskRecord.getId();
    }

    boolean isRootTask(ExactTimeStamp exactTimeStamp) {
        Assert.assertTrue(exactTimeStamp != null);
        Assert.assertTrue(current(exactTimeStamp));

        return (getParentTask(exactTimeStamp) == null);
    }

    private ExactTimeStamp getStartExactTimeStamp() {
        return new ExactTimeStamp(mTaskRecord.getStartTime());
    }

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

            if (schedules.isEmpty()) {
                Assert.assertTrue(Stream.of(schedules)
                        .allMatch(schedule -> schedule.current(endExactTimeStamp)));

                Stream.of(schedules)
                        .forEach(schedule -> schedule.setEndExactTimeStamp(endExactTimeStamp));
            }
        } else {
            Assert.assertTrue(getCurrentSchedules(endExactTimeStamp).isEmpty());
        }

        for (Task childTask : getChildTasks(endExactTimeStamp)) {
            Assert.assertTrue(childTask != null);
            childTask.setEndExactTimeStamp(endExactTimeStamp);
        }

        DomainFactory domainFactory = mDomainFactoryReference.get();
        Assert.assertTrue(domainFactory != null);

        domainFactory.setParentHierarchyEndTimeStamp(this, endExactTimeStamp);

        mTaskRecord.setEndTime(endExactTimeStamp.getLong());
    }

    public boolean current(ExactTimeStamp exactTimeStamp) {
        Assert.assertTrue(exactTimeStamp != null);

        ExactTimeStamp startExactTimeStamp = getStartExactTimeStamp();
        ExactTimeStamp endExactTimeStamp = getEndExactTimeStamp();

        return (startExactTimeStamp.compareTo(exactTimeStamp) <= 0 && (endExactTimeStamp == null || endExactTimeStamp.compareTo(exactTimeStamp) > 0));
    }

    boolean notDeleted(ExactTimeStamp exactTimeStamp) {
        Assert.assertTrue(exactTimeStamp != null);

        ExactTimeStamp endExactTimeStamp = getEndExactTimeStamp();

        return (endExactTimeStamp == null || endExactTimeStamp.compareTo(exactTimeStamp) > 0);
    }

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

    List<Instance> getInstances(ExactTimeStamp startExactTimeStamp, ExactTimeStamp endExactTimeStamp, ExactTimeStamp now) {
        Assert.assertTrue(endExactTimeStamp != null);
        Assert.assertTrue(now != null);

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

        DomainFactory domainFactory = mDomainFactoryReference.get();
        Assert.assertTrue(domainFactory != null);

        List<TaskHierarchy> taskHierarchies = domainFactory.getParentTaskHierarchies(this);
        Assert.assertTrue(taskHierarchies != null);

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

    void updateOldestVisible(ExactTimeStamp now) {
        Assert.assertTrue(now != null);

        DomainFactory domainFactory = mDomainFactoryReference.get();
        Assert.assertTrue(domainFactory != null);

        // 24 hack
        List<Instance> instances = domainFactory.getPastInstances(this, now);

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

    private Task getRootTask(ExactTimeStamp exactTimeStamp) {
        Assert.assertTrue(exactTimeStamp != null);

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
